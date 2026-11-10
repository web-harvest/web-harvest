package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Filesystem and schema validation contracts for {@link FilePlugin} execute paths.
 */
public class FilePluginBranchContractTest {

    private Path workRoot;
    private ScraperContext context;

    @BeforeMethod
    public void setUp() throws Exception {
        workRoot = Files.createTempDirectory("wh-file-contract-");
        context = new ScraperContext();
        context.setCharset("UTF-8");
    }

    private static void bindWorkingDir(FilePlugin plugin, Path dir) throws Exception {
        Field field = FilePlugin.class.getDeclaredField("workingDir");
        field.setAccessible(true);
        field.set(plugin, dir.toString());
    }

    private FilePlugin configuredPlugin(String action, String relativePath) throws Exception {
        return configuredPlugin(action, relativePath, Collections.emptyMap());
    }

    private FilePlugin configuredPlugin(
            String action, String relativePath, java.util.Map<String, String> extra) throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "file", "http://org.webharvest/schema/2.1/core", "file");
        configuration.setProperty("path", relativePath);
        configuration.setProperty("action", action);
        for (java.util.Map.Entry<String, String> entry : extra.entrySet()) {
            configuration.setProperty(entry.getKey(), entry.getValue());
        }
        FilePlugin plugin = new FilePlugin();
        bindWorkingDir(plugin, workRoot);
        plugin.initialize(configuration);
        return plugin;
    }

    @Test
    public void schemaRejectsNullConfigurationAndInvalidAction() {
        PluginConfigurationSchema schema = new FilePlugin().getConfigurationSchema();
        try {
            schema.validate(null);
            fail("Null configuration must throw");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Configuration cannot be null");
        }

        PluginConfiguration missingPath = mock(PluginConfiguration.class);
        when(missingPath.getProperty("path")).thenReturn(null);
        try {
            schema.validate(missingPath);
            fail("Missing path must throw");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Path attribute is required");
        }

        PluginConfiguration badAction = mock(PluginConfiguration.class);
        when(badAction.getProperty("path")).thenReturn("x.txt");
        when(badAction.getProperty("action", "read")).thenReturn("chmod");
        try {
            schema.validate(badAction);
            fail("Unknown action must throw");
        } catch (PluginValidationException e) {
            assertTrue(e.getMessage().contains("Invalid action"));
        }

        PluginConfiguration badType = mock(PluginConfiguration.class);
        when(badType.getProperty("path")).thenReturn("x.bin");
        when(badType.getProperty("action", "read")).thenReturn("read");
        when(badType.getProperty("type", "text")).thenReturn("xml");
        try {
            schema.validate(badType);
            fail("Unknown type must throw");
        } catch (PluginValidationException e) {
            assertTrue(e.getMessage().contains("Invalid type"));
        }
    }

    @Test
    public void readTextReturnsContentAndMissingFileIsEmpty() throws Exception {
        Path file = workRoot.resolve("read-me.txt");
        Files.writeString(file, "hello file", StandardCharsets.UTF_8);

        FilePlugin plugin = configuredPlugin("read", "read-me.txt");
        Variable content = plugin.execute(context);
        assertEquals(content.toString(), "hello file");

        FilePlugin missing = configuredPlugin("read", "nope.txt");
        assertSame(missing.execute(context), EmptyVariable.INSTANCE);
    }

    @Test
    public void writeTextUsesTextContentAndAppendPreservesPriorBytes() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "file", "http://org.webharvest/schema/2.1/core", "file");
        configuration.setProperty("path", "out/note.txt");
        configuration.setProperty("action", "write");
        configuration.setProperty("_textContent", "line one");

        FilePlugin writePlugin = new FilePlugin();
        bindWorkingDir(writePlugin, workRoot);
        writePlugin.initialize(configuration);
        assertSame(writePlugin.execute(context), EmptyVariable.INSTANCE);
        assertEquals(Files.readString(workRoot.resolve("out/note.txt")), "line one");

        DefaultPluginConfiguration appendConfig = new DefaultPluginConfiguration(
                "file", "http://org.webharvest/schema/2.1/core", "file");
        appendConfig.setProperty("path", "out/note.txt");
        appendConfig.setProperty("action", "append");
        appendConfig.setProperty("_textContent", "\nline two");

        FilePlugin appendPlugin = new FilePlugin();
        bindWorkingDir(appendPlugin, workRoot);
        appendPlugin.initialize(appendConfig);
        appendPlugin.execute(context);
        assertEquals(Files.readString(workRoot.resolve("out/note.txt")), "line one\nline two");
    }

    @Test
    public void readBinaryActionReturnsEmptyPlaceholder() throws Exception {
        Path file = workRoot.resolve("data.bin");
        Files.write(file, new byte[] {0x01, 0x02});

        FilePlugin plugin = configuredPlugin("read", "data.bin", java.util.Map.of("type", "binary"));
        assertSame(plugin.execute(context), EmptyVariable.INSTANCE);
    }

    @Test
    public void listHonorsFilterRecursiveAndDirectoryFlags() throws Exception {
        Files.createDirectories(workRoot.resolve("tree/sub"));
        Files.writeString(workRoot.resolve("tree/a.txt"), "a");
        Files.writeString(workRoot.resolve("tree/b.log"), "b");
        Files.writeString(workRoot.resolve("tree/sub/c.txt"), "c");

        FilePlugin listTxt = configuredPlugin("list", "tree", java.util.Map.of(
                "listfilter", "*.txt",
                "listrecursive", "true",
                "listdirs", "false",
                "listfiles", "true"));
        Variable listed = listTxt.execute(context);
        String names = listed.toString();
        assertTrue(names.contains("a.txt"));
        assertTrue(names.contains("c.txt"));
        assertFalse(names.contains("b.log"));

        FilePlugin dirsOnly = configuredPlugin("list", "tree", java.util.Map.of(
                "listfilter", "*",
                "listfiles", "false",
                "listdirs", "true",
                "listrecursive", "false"));
        Variable dirList = dirsOnly.execute(context);
        assertTrue(dirList.toString().contains("sub/"));
    }

    @Test
    public void listOnFilePathReturnsEmpty() throws Exception {
        Path onlyFile = workRoot.resolve("solo.txt");
        Files.writeString(onlyFile, "x");
        FilePlugin plugin = configuredPlugin("list", "solo.txt");
        assertSame(plugin.execute(context), EmptyVariable.INSTANCE);
    }

    @Test
    public void appendBinaryConcatenatesListMembers() {
        FilePlugin plugin = new FilePlugin();
        ListVariable body = new ListVariable(Arrays.asList(
                new NodeVariable(new byte[] {1, 2}),
                new NodeVariable(new byte[] {3})));
        NodeVariable merged = plugin.appendBinary(body);
        assertEquals(merged.toBinary(), new byte[] {1, 2, 3});
        assertEquals(plugin.appendBinary(null).toString(), "");
    }

    @Test
    public void unknownActionAtRuntimeYieldsEmptyVariable() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "file", "http://org.webharvest/schema/2.1/core", "file");
        configuration.setProperty("path", "noop.txt");
        configuration.setProperty("action", "read");

        FilePlugin plugin = new FilePlugin();
        bindWorkingDir(plugin, workRoot);
        plugin.initialize(configuration);
        configuration.setProperty("action", "snapshot");
        assertSame(plugin.execute(context), EmptyVariable.INSTANCE);
    }
}
