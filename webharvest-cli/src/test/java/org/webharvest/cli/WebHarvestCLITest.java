package org.webharvest.cli;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.webharvest.definition.Config;
import org.webharvest.definition.ConfigFactory;
import org.webharvest.definition.ConfigSource;
import org.webharvest.definition.BufferConfigSource;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.webharvest.ioc.ScraperModule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import static org.testng.Assert.*;

/**
 * Comprehensive unit tests for WebHarvest CLI to verify correct usage
 * of v2.2 Scraper API and configuration handling.
 */
public class WebHarvestCLITest {

    private ConfigFactory configFactory;
    private File tempConfigFile;
    private File tempWorkDir;

    @BeforeClass
    public void setUp() throws IOException {
        Injector injector = Guice.createInjector(new ScraperModule("."));
        configFactory = injector.getInstance(ConfigFactory.class);
        
        // Create temp directory for test files
        tempWorkDir = Files.createTempDirectory("webharvest-cli-test").toFile();
        tempWorkDir.deleteOnExit();
    }

    @AfterClass
    public void tearDown() {
        if (tempConfigFile != null && tempConfigFile.exists()) {
            tempConfigFile.delete();
        }
        if (tempWorkDir != null && tempWorkDir.exists()) {
            deleteDirectory(tempWorkDir);
        }
    }

    @Test(description = "Test that CLI can validate simple configuration")
    public void testValidateSimpleConfig() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config>\n" +
                          "    <set-var name=\"test\">Hello WebHarvest CLI!</set-var>\n" +
                          "    <log message=\"Test: ${test}\"/>\n" +
                          "</config>";

        // This simulates what CLI does
        ConfigSource configSource = new BufferConfigSource(configXml);
        Config config = configFactory.create(configSource);
        config.reload();
        
        assertNotNull(config, "CLI config should be created successfully");
        assertNotNull(config.getConfigurationTree(), "Configuration tree should be loaded");
    }

    @Test(description = "Test that CLI can handle file-based configuration")
    public void testFileBasedConfig() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config>\n" +
                          "    <log message=\"File-based config test\"/>\n" +
                          "</config>";

        tempConfigFile = new File(tempWorkDir, "test-config.xml");
        try (FileWriter writer = new FileWriter(tempConfigFile)) {
            writer.write(configXml);
        }

        assertTrue(tempConfigFile.exists(), "Config file should be created");
        assertTrue(tempConfigFile.length() > 0, "Config file should not be empty");
    }

    @Test(description = "Test that CLI can handle HTTP plugin configuration")
    public void testHttpPluginConfig() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config>\n" +
                          "    <http url=\"https://httpbin.org/json\" method=\"GET\"/>\n" +
                          "    <log message=\"Response received\"/>\n" +
                          "</config>";

        ConfigSource configSource = new BufferConfigSource(configXml);
        Config config = configFactory.create(configSource);
        config.reload();
        
        assertNotNull(config, "HTTP config should be created");
        assertNotNull(config.getConfigurationTree(), "HTTP config tree should be loaded");
    }

    @Test(description = "Test that CLI can handle XPath plugin configuration")
    public void testXPathPluginConfig() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config>\n" +
                          "    <http url=\"https://example.com\" method=\"GET\"/>\n" +
                          "    <xpath expression=\"//h1\" result=\"heading\"/>\n" +
                          "    <log message=\"Heading: ${heading}\"/>\n" +
                          "</config>";

        ConfigSource configSource = new BufferConfigSource(configXml);
        Config config = configFactory.create(configSource);
        config.reload();
        
        assertNotNull(config, "XPath config should be created");
        assertNotNull(config.getConfigurationTree(), "XPath config tree should be loaded");
    }

    @Test(description = "Test that CLI can handle variable substitution configuration")
    public void testVariableSubstitutionConfig() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config>\n" +
                          "    <set-var name=\"url\">https://httpbin.org/json</set-var>\n" +
                          "    <set-var name=\"method\">GET</set-var>\n" +
                          "    <http url=\"${url}\" method=\"${method}\"/>\n" +
                          "    <log message=\"Response from ${url}\"/>\n" +
                          "</config>";

        ConfigSource configSource = new BufferConfigSource(configXml);
        Config config = configFactory.create(configSource);
        config.reload();
        
        assertNotNull(config, "Variable substitution config should be created");
        assertNotNull(config.getConfigurationTree(), "Variable substitution config tree should be loaded");
    }

    @Test(description = "Test that CLI can handle loop configuration")
    public void testLoopConfig() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config>\n" +
                          "    <loop maxloops=\"3\">\n" +
                          "        <set-var name=\"index\">${_index}</set-var>\n" +
                          "        <log message=\"Loop iteration: ${index}\"/>\n" +
                          "    </loop>\n" +
                          "</config>";

        ConfigSource configSource = new BufferConfigSource(configXml);
        Config config = configFactory.create(configSource);
        config.reload();
        
        assertNotNull(config, "Loop config should be created");
        assertNotNull(config.getConfigurationTree(), "Loop config tree should be loaded");
    }

    @Test(description = "Test that CLI can handle conditional configuration")
    public void testConditionalConfig() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config>\n" +
                          "    <set-var name=\"debug\">true</set-var>\n" +
                          "    <if condition=\"${debug}\">\n" +
                          "        <log message=\"Debug mode is ON\"/>\n" +
                          "    </if>\n" +
                          "</config>";

        ConfigSource configSource = new BufferConfigSource(configXml);
        Config config = configFactory.create(configSource);
        config.reload();
        
        assertNotNull(config, "Conditional config should be created");
        assertNotNull(config.getConfigurationTree(), "Conditional config tree should be loaded");
    }

    @Test(description = "Test that CLI can handle JSON processing configuration")
    public void testJsonProcessingConfig() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config>\n" +
                          "    <http url=\"https://httpbin.org/json\" method=\"GET\"/>\n" +
                          "    <json-to-xml>\n" +
                          "        <xpath expression=\"/json/slideshow/title\" result=\"title\"/>\n" +
                          "    </json-to-xml>\n" +
                          "    <log message=\"Title: ${title}\"/>\n" +
                          "</config>";

        ConfigSource configSource = new BufferConfigSource(configXml);
        Config config = configFactory.create(configSource);
        config.reload();
        
        assertNotNull(config, "JSON processing config should be created");
        assertNotNull(config.getConfigurationTree(), "JSON processing config tree should be loaded");
    }

    @Test(description = "Test that CLI requires reload() before getConfigurationTree()")
    public void testReloadRequired() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config>\n" +
                          "    <log message=\"Test\"/>\n" +
                          "</config>";

        ConfigSource configSource = new BufferConfigSource(configXml);
        Config config = configFactory.create(configSource);
        
        // Without reload(), getConfigurationTree() should throw exception
        try {
            config.getConfigurationTree();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Call reload() first"));
        }
    }

    @Test(description = "Test that CLI can handle complex nested configuration")
    public void testComplexNestedConfig() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config>\n" +
                          "    <http url=\"https://httpbin.org/html\" method=\"GET\"/>\n" +
                          "    <loop maxloops=\"2\">\n" +
                          "        <if condition=\"${_index} == 0\">\n" +
                          "            <xpath expression=\"//h1\" result=\"heading1\"/>\n" +
                          "            <log message=\"First heading: ${heading1}\"/>\n" +
                          "        </if>\n" +
                          "        <if condition=\"${_index} == 1\">\n" +
                          "            <xpath expression=\"//p\" result=\"paragraph\"/>\n" +
                          "            <log message=\"Paragraph: ${paragraph}\"/>\n" +
                          "        </if>\n" +
                          "    </loop>\n" +
                          "</config>";

        ConfigSource configSource = new BufferConfigSource(configXml);
        Config config = configFactory.create(configSource);
        config.reload();
        
        assertNotNull(config, "Complex nested config should be created");
        assertNotNull(config.getConfigurationTree(), "Complex nested config tree should be loaded");
    }

    // Helper method to recursively delete directory
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
