/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.integration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.Harvest;
import org.webharvest.Harvester;
import org.webharvest.HarvestLoadCallback;
import org.webharvest.definition.BufferConfigSource;
import org.webharvest.definition.ConfigSource;
import org.webharvest.definition.CorePluginRegistry;
import org.webharvest.definition.CorePluginsRegistrar;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.plugin.scanner.PluginPackageScanner;
import org.webharvest.plugin.zip.ZipEntryPlugin;
import org.webharvest.plugin.zip.ZipPlugin;
import org.webharvest.plugin.zip.ZipPluginRegistrar;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Runtime contracts for the optional ZIP extension.
 *
 * @author WebHarvest Team
 * @since 2.2.0
 */
public class ZipPluginIntegrationTest {

    private Injector injector;
    private Harvest harvest;

    @BeforeMethod
    public void setUp() {
        CorePluginsRegistrar.reset();
        injector = Guice.createInjector(new ScraperModule(System.getProperty("user.dir")));
        ZipPluginRegistrar.register(CorePluginRegistry.getInstance());
        assertTrue(CorePluginRegistry.getInstance().isPluginRegistered("zip"));
        assertTrue(CorePluginRegistry.getInstance().isPluginRegistered("zip-entry"));
        harvest = injector.getInstance(Harvest.class);
    }

    @AfterMethod
    public void tearDown() {
        CorePluginsRegistrar.reset();
        CorePluginsRegistrar.registerCorePlugins();
        injector = null;
        harvest = null;
    }

    /**
     * Verifies that the generic package scanner discovers the real optional ZIP module
     * only when explicitly invoked, without adding it to the default Core registry.
     */
    @Test(groups = {"integration", "zip"})
    public void discoversOptionalZipPluginsThroughGenericPackageScanner() {
        CorePluginsRegistrar.reset();
        CorePluginsRegistrar.registerCorePlugins();

        assertFalse(CorePluginRegistry.getInstance().isPluginRegistered("zip"),
                "Optional ZIP plugins must not be part of the default Core registry");
        assertFalse(CorePluginRegistry.getInstance().isPluginRegistered("zip-entry"),
                "Optional ZIP plugins must not be part of the default Core registry");

        new PluginPackageScanner(CorePluginRegistry.getInstance())
                .scanPackages("org.webharvest.plugin.zip");

        assertSame(CorePluginRegistry.getInstance().getPluginClass("zip"), ZipPlugin.class);
        assertSame(CorePluginRegistry.getInstance().getPluginClass("zip-entry"), ZipEntryPlugin.class);
    }

    /** Verifies real archive contents and nested ZIP scope restoration. */
    @Test(groups = {"integration", "zip"})
    public void testZipPluginExampleCreatesEntriesAndNestedArchive() throws Exception {
        String xml;
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("integration-tests/zip_plugin_example.xml")) {
            assertNotNull(input, "ZIP example XML should be available on the test classpath");
            xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        DynamicScopeContext context = executeXml(xml);
        Variable archive = context.getVar("archive");
        assertNotNull(archive, "ZIP plugin should return a binary archive value");

        Map<String, byte[]> outerEntries = readZip(archive.toBinary("UTF-8"));
        assertEquals(outerEntries.size(), 4, "Outer archive should contain exactly four entries");
        assertEquals(asUtf8(outerEntries.get("before.txt")), "before");
        assertEquals(asUtf8(outerEntries.get("after.txt")), "after");
        byte[] latin1Content = outerEntries.get("latin1.txt");
        assertNotNull(latin1Content, "Charset-specific entry should be present");
        assertEquals(latin1Content.length, 4);
        assertEquals(latin1Content[3] & 0xFF, 0xE9, "Entry charset attribute should control encoding");
        assertTrue(outerEntries.containsKey("inner.zip"), "Nested archive should be written as an outer entry");

        Map<String, byte[]> innerEntries = readZip(outerEntries.get("inner.zip"));
        assertEquals(innerEntries.size(), 1, "Nested archive should contain exactly one entry");
        assertEquals(asUtf8(innerEntries.get("inner.txt")), "inside");
    }

    private DynamicScopeContext executeXml(String xml) throws Exception {
        ConfigSource configSource = new BufferConfigSource(xml);
        HarvestLoadCallback loadCallback = new HarvestLoadCallback() { };
        Harvester harvester = harvest.getHarvester(configSource, loadCallback);

        final DynamicScopeContext[] resultContext = new DynamicScopeContext[1];
        harvester.execute(new Harvester.ContextInitCallback() {
            @Override
            public void onSuccess(DynamicScopeContext context) {
                resultContext[0] = context;
            }
        });

        assertNotNull(resultContext[0], "Execution context should be available after harvesting");
        return resultContext[0];
    }

    private Map<String, byte[]> readZip(byte[] archiveBytes) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zipInput = new ZipInputStream(
                new ByteArrayInputStream(archiveBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                ByteArrayOutputStream entryBytes = new ByteArrayOutputStream();
                zipInput.transferTo(entryBytes);
                entries.put(entry.getName(), entryBytes.toByteArray());
                zipInput.closeEntry();
            }
        }
        return entries;
    }

    private String asUtf8(byte[] bytes) {
        assertNotNull(bytes, "Expected ZIP entry content");
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
