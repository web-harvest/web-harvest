package org.webharvest.plugin;

import org.testng.annotations.Test;
import org.webharvest.plugin.services.HttpServiceException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.testng.Assert.*;

public class PluginInterfacesTest {

    @Test
    public void testDefaultPluginConfigurationStoresIdentityAndTypedProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("name", "value");
        properties.put("enabled", "true");
        properties.put("count", "42");
        properties.put("total", "9000");
        properties.put("ratio", "1.5");
        properties.put("invalid", "not-a-number");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "plugin-id", "urn:webharvest:test", "test", properties);

        assertEquals(configuration.getPluginId(), "plugin-id");
        assertEquals(configuration.getNamespace(), "urn:webharvest:test");
        assertEquals(configuration.getElementName(), "test");
        assertTrue(configuration.hasProperty("name"));
        assertEquals(configuration.getProperty("name"), "value");
        assertEquals(configuration.getProperty("missing"), null);
        assertEquals(configuration.getProperty("missing", "fallback"), "fallback");
        assertTrue(configuration.getBooleanProperty("enabled", false));
        assertEquals(configuration.getIntProperty("count", 0), 42);
        assertEquals(configuration.getLongProperty("total", 0L), 9000L);
        assertEquals(configuration.getDoubleProperty("ratio", 0.0), 1.5);
        assertEquals(configuration.getIntProperty("invalid", 7), 7);
        assertEquals(configuration.getLongProperty("missing", 11L), 11L);
        assertEquals(configuration.getDoubleProperty("missing", 2.5), 2.5);
        assertFalse(configuration.hasProperty("missing"));
    }

    @Test
    public void testDefaultPluginConfigurationReturnsDefensivePropertyCopies() {
        Map<String, String> initialProperties = new HashMap<>();
        initialProperties.put("name", "original");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "plugin-id", "urn:webharvest:test", "test", initialProperties);

        initialProperties.put("name", "changed-input");
        assertEquals(configuration.getProperty("name"), "original");

        Map<String, String> propertyCopy = configuration.getAllProperties();
        propertyCopy.put("name", "changed-map");
        assertEquals(configuration.getProperty("name"), "original");

        Properties propertiesCopy = configuration.getProperties();
        propertiesCopy.setProperty("name", "changed-properties");
        assertEquals(configuration.getProperty("name"), "original");
    }

    @Test
    public void testHttpServiceExceptionPreservesMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");
        HttpServiceException exception = new HttpServiceException("HTTP service error", cause);

        assertEquals(exception.getMessage(), "HTTP service error");
        assertSame(exception.getCause(), cause);
    }
}
