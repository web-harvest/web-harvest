package org.webharvest.plugin.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;

import org.testng.annotations.Test;
import org.webharvest.definition.CorePluginsRegistrar;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.ScraperContext;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/** Regression tests for interruption propagation through file plugin body handling. */
public class FilePluginInterruptionTest {

    private static final String CORE_NAMESPACE = "http://org.webharvest/schema/2.1/core";

    @Test
    public void interruptedNestedBodyIsNotConvertedToEmptyFileWriteSuccess() throws Exception {
        CorePluginsRegistrar.registerCorePlugins();

        Path output = Paths.get("target", "test-output",
                "file-interrupted-" + UUID.randomUUID() + ".txt");
        DefaultPluginConfiguration file = new DefaultPluginConfiguration("file", CORE_NAMESPACE, "file");
        file.setProperty("path", output.toString());
        file.setProperty("action", "write");
        file.setProperty("type", "text");
        file.setProperty("charset", "UTF-8");

        DefaultPluginConfiguration sleep = new DefaultPluginConfiguration("sleep", CORE_NAMESPACE, "sleep");
        sleep.setProperty("milliseconds", "5000");
        file.setChildren(Collections.<PluginConfiguration>singletonList(sleep));

        FilePlugin plugin = new FilePlugin();
        plugin.initialize(file);
        Thread.currentThread().interrupt();
        try {
            plugin.execute(new ScraperContext());
            fail("An interrupted child plugin must not become an empty/successful file write");
        } catch (InterruptedException expected) {
            assertTrue(Thread.currentThread().isInterrupted(),
                    "The interrupt flag must be restored after plugin cleanup");
        } finally {
            Thread.interrupted();
            Files.deleteIfExists(output);
        }

        assertFalse(Files.exists(output), "The interrupted file operation must not create its target");
    }
}
