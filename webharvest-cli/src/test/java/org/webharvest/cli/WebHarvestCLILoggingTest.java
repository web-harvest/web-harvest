/*
 * Copyright (c) 2006-2026 the original author or authors.
 */
package org.webharvest.cli;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.ioc.DebugFileLogger;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Behavioral tests for the CLI logging flags and debug-file output.
 *
 * <p>The current implementation keeps its logging setup methods private.
 * Reflection lets these tests exercise the real CLI configuration path
 * without widening the production API solely for tests.</p>
 *
 * <p>These assertions intentionally describe externally observable behavior.
 * The backend-specific logger inspection can be migrated alongside the
 * implementation when the logging provider changes.</p>
 */
public class WebHarvestCLILoggingTest {

    private Path workingDirectory;

    @BeforeMethod
    public void setUp() throws Exception {
        workingDirectory = Files.createTempDirectory("webharvest-cli-logging");
        restoreDefaultConfiguration();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        restoreDefaultConfiguration();
        if (workingDirectory != null) {
            deleteRecursively(workingDirectory.toFile());
        }
    }

    @Test
    public void preservesConfiguredRootLevelWhenNoLoggingOptionIsProvided() throws Exception {
        Logger.getRootLogger().setLevel(Level.WARN);

        invokeLoggingMethod("parseLoggingSettings", new Class<?>[]{Map.class},
                Collections.<String, String>emptyMap());

        assertEquals(Logger.getRootLogger().getLevel(), Level.WARN);
    }

    @Test
    public void appliesExplicitLogLevelToRootLogger() throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("loglevel", "ERROR");

        invokeLoggingMethod("parseLoggingSettings", new Class<?>[]{Map.class}, params);

        assertEquals(Logger.getRootLogger().getLevel(), Level.ERROR);
    }

    @Test
    public void loadsCustomPropertiesAndNamedCategoryLevel() throws Exception {
        Path propertiesFile = workingDirectory.resolve("custom-log4j.properties");
        String properties = "log4j.rootLogger=ERROR, console\n"
                + "log4j.appender.console=org.apache.log4j.ConsoleAppender\n"
                + "log4j.appender.console.Target=System.err\n"
                + "log4j.appender.console.layout=org.apache.log4j.PatternLayout\n"
                + "log4j.appender.console.layout.ConversionPattern=%p %c - %m%n\n"
                + "log4j.logger.org.webharvest.cli.loggingtest=DEBUG\n";
        Files.write(propertiesFile, properties.getBytes(StandardCharsets.UTF_8));

        Map<String, String> params = new HashMap<String, String>();
        params.put("logpropsfile", propertiesFile.toAbsolutePath().toString());

        invokeLoggingMethod("parseLoggingSettings", new Class<?>[]{Map.class}, params);

        assertEquals(Logger.getRootLogger().getLevel(), Level.ERROR);
        assertEquals(Logger.getLogger("org.webharvest.cli.loggingtest").getLevel(), Level.DEBUG);
    }

    @Test
    public void debugModeWritesTraceMessagesToWorkDirectoryDebugFile() throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("debug", "true");

        invokeLoggingMethod("parseDebugModeSettings",
                new Class<?>[]{Map.class, String.class},
                params, workingDirectory.toAbsolutePath().toString());

        File debugFile = workingDirectory.resolve("_DEBUG").toFile();
        assertTrue(debugFile.isFile(), "Debug mode should create <workdir>/_DEBUG");

        Logger debugLogger = Logger.getLogger(DebugFileLogger.NAME);
        assertEquals(debugLogger.getLevel(), Level.TRACE);
        debugLogger.trace("webharvest-debug-compatibility-marker");

        String output = new String(Files.readAllBytes(debugFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(output.contains("webharvest-debug-compatibility-marker"),
                "The debug file should contain emitted TRACE messages");
    }

    @Test
    public void disabledDebugModeDoesNotCreateDebugFile() throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("debug", "false");

        invokeLoggingMethod("parseDebugModeSettings",
                new Class<?>[]{Map.class, String.class},
                params, workingDirectory.toAbsolutePath().toString());

        assertFalse(workingDirectory.resolve("_DEBUG").toFile().exists());
    }

    private static void invokeLoggingMethod(String name, Class<?>[] parameterTypes,
            Object... arguments) throws Exception {
        Method method = WebHarvestCLI.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        method.invoke(null, arguments);
    }

    private static void restoreDefaultConfiguration() {
        LogManager.resetConfiguration();
        PropertyConfigurator.configure(new File("log4j.properties").getAbsolutePath());
    }

    private static void deleteRecursively(File file) throws Exception {
        if (!file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        Files.deleteIfExists(file.toPath());
    }
}
