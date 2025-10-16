package org.webharvest.server.api;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.webharvest.definition.Config;
import org.webharvest.definition.ConfigFactory;
import org.webharvest.definition.ConfigSource;
import org.webharvest.definition.BufferConfigSource;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.webharvest.ioc.ScraperModule;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Unit test for ConfigServlet to ensure it uses v2.2 API correctly.
 */
public class ConfigServletTest {

    private ConfigFactory configFactory;

    @BeforeClass
    public void setUp() {
        Injector injector = Guice.createInjector(new ScraperModule("."));
        configFactory = injector.getInstance(ConfigFactory.class);
    }

    @Test(description = "Test that simple configuration can be validated without OutOfScopeException")
    public void testValidateSimpleConfig() {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                           "<config>\n" +
                           "    <set-var name=\"test\">Hello WebHarvest!</set-var>\n" +
                           "    <log message=\"Test: ${test}\"/>\n" +
                           "</config>";

        // This is what ConfigServlet.validateConfig() does
        ConfigSource configSource = new BufferConfigSource(xmlContent);
        Config config = configFactory.create(configSource);
        
        assertNotNull(config, "Config should be created successfully");
    }

    @Test(description = "Test that HTTP example can be validated")
    public void testValidateHttpExample() {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                           "<config>\n" +
                           "    <http url=\"https://httpbin.org/json\" method=\"GET\"/>\n" +
                           "    <log message=\"Response: ${http.body}\"/>\n" +
                           "</config>";

        ConfigSource configSource = new BufferConfigSource(xmlContent);
        Config config = configFactory.create(configSource);
        
        assertNotNull(config, "HTTP config should be created successfully");
    }

    @Test(description = "Test that XPath example can be validated")
    public void testValidateXPathExample() {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                           "<config>\n" +
                           "    <http url=\"https://example.com\" method=\"GET\"/>\n" +
                           "    <xpath expression=\"//h1\" result=\"heading\"/>\n" +
                           "    <log message=\"Heading: ${heading}\"/>\n" +
                           "</config>";

        ConfigSource configSource = new BufferConfigSource(xmlContent);
        Config config = configFactory.create(configSource);
        
        assertNotNull(config, "XPath config should be created successfully");
    }

    @Test(description = "Test that JSON example can be validated")
    public void testValidateJsonExample() {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                           "<config>\n" +
                           "    <http url=\"https://httpbin.org/json\" method=\"GET\"/>\n" +
                           "    <json-to-xml>\n" +
                           "        <xpath expression=\"/json/slideshow/title\" result=\"title\"/>\n" +
                           "    </json-to-xml>\n" +
                           "    <log message=\"Title: ${title}\"/>\n" +
                           "</config>";

        ConfigSource configSource = new BufferConfigSource(xmlContent);
        Config config = configFactory.create(configSource);
        
        assertNotNull(config, "JSON config should be created successfully");
    }

    @Test(description = "Test that config.reload() works without throwing exception")
    public void testConfigReload() {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                           "<config>\n" +
                           "    <set-var name=\"test\">Value</set-var>\n" +
                           "    <log message=\"${test}\"/>\n" +
                           "</config>";

        ConfigSource configSource = new BufferConfigSource(xmlContent);
        Config config = configFactory.create(configSource);
        
        // This is what ExecutionManager does - reload() must work
        config.reload();
        
        assertNotNull(config, "Config should be valid after reload");
        assertNotNull(config.getConfigurationTree(), "Configuration tree should be loaded after reload");
    }

    @Test(description = "Test that config.reload() is required before getConfigurationTree()")
    public void testConfigReloadRequired() {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                           "<config>\n" +
                           "    <log message=\"Test\"/>\n" +
                           "</config>";

        ConfigSource configSource = new BufferConfigSource(xmlContent);
        Config config = configFactory.create(configSource);
        
        // Without reload(), getConfigurationTree() should throw exception
        try {
            config.getConfigurationTree();
            throw new AssertionError("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Call reload() first"));
        }
    }
}

