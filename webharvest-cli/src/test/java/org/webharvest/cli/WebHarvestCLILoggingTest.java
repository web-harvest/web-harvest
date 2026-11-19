/*
 * Copyright (c) 2006-2026 the original author or authors.
 */
package org.webharvest.cli;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.ioc.DebugFileLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Behavioral tests for the CLI logging flags and debug-file output.
 *
 * <p>The current implementation keeps its logging setup methods private.
 * Reflection lets these tests exercise the real CLI configuration path
 * without widening the production API solely for tests.</p>
 */
public class WebHarvestCLILoggingTest {

    static {
        System.setProperty("log4j1.compatibility", "true");
    }

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
        Configurator.setRootLevel(Level.WARN);

        invokeLoggingMethod("parseLoggingSettings", new Class<?>[]{Map.class},
                Collections.<String, String>emptyMap());

        assertEquals(rootLoggerConfig().getLevel(), Level.WARN);
    }

    @Test
    public void appliesExplicitLogLevelToRootLogger() throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("loglevel", "ERROR");

        invokeLoggingMethod("parseLoggingSettings", new Class<?>[]{Map.class}, params);

        assertEquals(rootLoggerConfig().getLevel(), Level.ERROR);
    }

    @Test
    public void loadsCustomPropertiesAndTakesPrecedenceOverLogLevel() throws Exception {
        Path outputFile = workingDirectory.resolve("custom-properties-output.log");
        Path propertiesFile = workingDirectory.resolve("custom-log4j.properties");
        String outputPath = outputFile.toAbsolutePath().toString().replace("\\", "\\\\");
        String properties = "log4j.rootLogger=ERROR, file\n"
                + "log4j.appender.file=org.apache.log4j.FileAppender\n"
                + "log4j.appender.file.File=" + outputPath + "\n"
                + "log4j.appender.file.Append=false\n"
                + "log4j.appender.file.layout=org.apache.log4j.PatternLayout\n"
                + "log4j.appender.file.layout.ConversionPattern=%p %c - %m%n\n"
                + "log4j.logger.org.webharvest.cli.loggingtest=DEBUG\n";
        Files.write(propertiesFile, properties.getBytes(StandardCharsets.UTF_8));

        Map<String, String> params = new HashMap<String, String>();
        params.put("logpropsfile", propertiesFile.toAbsolutePath().toString());
        params.put("loglevel", "TRACE");

        invokeLoggingMethod("parseLoggingSettings", new Class<?>[]{Map.class}, params);

        assertEquals(rootLoggerConfig().getLevel(), Level.ERROR);
        assertEquals(loggerConfig("org.webharvest.cli.loggingtest").getLevel(), Level.DEBUG);

        Logger customLogger = LoggerFactory.getLogger("org.webharvest.cli.loggingtest");
        customLogger.debug("webharvest-custom-properties-compatibility-marker");

        String output = new String(Files.readAllBytes(outputFile), StandardCharsets.UTF_8);
        assertTrue(output.contains("webharvest-custom-properties-compatibility-marker"),
                "The legacy properties FileAppender should write SLF4J events");
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

        assertEquals(loggerConfig(DebugFileLogger.NAME).getLevel(), Level.TRACE);
        Logger debugLogger = LoggerFactory.getLogger(DebugFileLogger.NAME);
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

    private static LoggerConfig rootLoggerConfig() {
        return loggerContext().getConfiguration().getRootLogger();
    }

    private static LoggerConfig loggerConfig(String name) {
        return loggerContext().getConfiguration().getLoggerConfig(name);
    }

    private static LoggerContext loggerContext() {
        return (LoggerContext) LogManager.getContext(false);
    }

    private static void invokeLoggingMethod(String name, Class<?>[] parameterTypes,
            Object... arguments) throws Exception {
        Method method = WebHarvestCLI.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        method.invoke(null, arguments);
    }

    private static void restoreDefaultConfiguration() {
        Configurator.reconfigure();
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
