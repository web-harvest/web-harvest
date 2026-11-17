package org.webharvest.plugin.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.Config;
import org.webharvest.definition.ConfigFactory;
import org.webharvest.definition.ConfigSourceFactory;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.plugin.DefaultPluginBodyExecutor;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.exceptions.IncludeExecutionException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

public class IncludePluginTest {

    private static final String NAMESPACE = "http://org.webharvest/schema/2.1/core";

    private Injector injector;
    private ConfigFactory configFactory;
    private ConfigSourceFactory configSourceFactory;
    private ScraperContext context;
    private Path directory;

    @BeforeMethod
    public void setUp() throws IOException {
        directory = Files.createTempDirectory("webharvest-include-test");
        injector = Guice.createInjector(new ScraperModule(directory.toString()));
        configFactory = injector.getInstance(ConfigFactory.class);
        configSourceFactory = injector.getInstance(ConfigSourceFactory.class);
        context = injector.getInstance(ScraperContext.class);
    }

    @Test
    public void shouldExecuteIncludedConfigurationAndShareVariables() throws Exception {
        write("main.xml", config("<include path=\"included.xml\"/>"
                + "<def var=\"afterInclude\" value=\"done\"/>"));
        write("included.xml", config("<def var=\"fromInclude\" value=\"loaded\"/>"));

        Config main = load("main.xml");
        context.setConfig(main);

        Variable result = execute(main);

        assertNotNull(result);
        assertEquals(context.getVar("fromInclude").toString(), "loaded");
        assertEquals(context.getVar("afterInclude").toString(), "done");
        assertSame(context.getConfig(), main, "The parent config must be restored after include");
    }

    @Test
    public void shouldResolveNestedIncludesRelativeToEachIncludingFile() throws Exception {
        write("main.xml", config("<include path=\"sub/first.xml\"/>"));
        write("sub/first.xml", config("<include path=\"nested/second.xml\"/>"));
        write("sub/nested/second.xml", config("<def var=\"nestedValue\" value=\"resolved\"/>"));

        Config main = load("main.xml");
        context.setConfig(main);

        execute(main);

        assertEquals(context.getVar("nestedValue").toString(), "resolved");
        assertSame(context.getConfig(), main, "Nested includes must restore each parent config");
    }

    @Test
    public void shouldAbortContainingConfigurationAndRestoreParentOnMissingInclude() throws Exception {
        write("main.xml", config("<include path=\"missing.xml\"/>"
                + "<def var=\"mustNotRun\" value=\"incorrect\"/>"));

        Config main = load("main.xml");
        context.setConfig(main);

        try {
            execute(main);
            fail("A missing include must fail the containing configuration");
        } catch (RuntimeException failure) {
            assertTrue(hasIncludeFailure(failure), "Failure must retain the IncludeExecutionException marker");
        }

        assertNull(context.getVar("mustNotRun"), "Later siblings must not execute after a fatal include");
        assertSame(context.getConfig(), main, "Parent config must be restored after include failure");
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void shouldValidateRequiredPath() throws Exception {
        IncludePlugin plugin = new IncludePlugin();
        DefaultPluginConfiguration configuration =
                new DefaultPluginConfiguration("include", NAMESPACE, "include", Collections.emptyMap());

        plugin.validateConfiguration(configuration);
    }

    @Test
    public void shouldExposeIncludeMetadataAndSchema() {
        IncludePlugin plugin = new IncludePlugin();
        assertEquals(plugin.getMetadata().getId(), "include");
        assertEquals(plugin.getMetadata().getElementName(), "include");
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertEquals(schema.getRequiredAttributes(), new String[]{"path"});
    }

    private Config load(String name) {
        Config config = configFactory.create(configSourceFactory.create(directory.resolve(name).toFile()));
        config.reload();
        return config;
    }

    private Variable execute(Config config) throws InterruptedException {
        return new DefaultPluginBodyExecutor().executeChildren(
                config.getConfigurationTree().getConfiguration().getChildren(), context);
    }

    private void write(String name, String xml) throws IOException {
        Path file = directory.resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, xml.getBytes(StandardCharsets.UTF_8));
    }

    private String config(String children) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<config xmlns=\"" + NAMESPACE + "\">" + children + "</config>";
    }

    private boolean hasIncludeFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof IncludeExecutionException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
