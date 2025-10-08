package org.webharvest.plugin;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Properties;

import static org.testng.Assert.*;

/**
 * Unit tests for DefaultPluginConfiguration.
 *
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class DefaultPluginConfigurationTest {

    private DefaultPluginConfiguration config;

    @BeforeMethod
    public void setUp() {
        config = new DefaultPluginConfiguration("test-plugin", "http://test.namespace", "test-element");
    }

    @Test
    public void testConstructorWithAllParameters() {
        DefaultPluginConfiguration testConfig = new DefaultPluginConfiguration("plugin-id", "namespace", "element-name");
        
        assertEquals(testConfig.getPluginId(), "plugin-id");
        assertEquals(testConfig.getNamespace(), "namespace");
        assertEquals(testConfig.getElementName(), "element-name");
    }

    @Test
    public void testConstructorWithIdAndNamespace() {
        DefaultPluginConfiguration testConfig = new DefaultPluginConfiguration("plugin-id", "namespace", null);
        
        assertEquals(testConfig.getPluginId(), "plugin-id");
        assertEquals(testConfig.getNamespace(), "namespace");
        assertNull(testConfig.getElementName());
    }

    @Test
    public void testConstructorWithIdOnly() {
        DefaultPluginConfiguration testConfig = new DefaultPluginConfiguration("plugin-id", null, null);
        
        assertEquals(testConfig.getPluginId(), "plugin-id");
        assertNull(testConfig.getNamespace());
        assertNull(testConfig.getElementName());
    }

    @Test
    public void testSetAndGetProperty() {
        config.setProperty("key1", "value1");
        config.setProperty("key2", "value2");
        
        assertEquals(config.getProperty("key1"), "value1");
        assertEquals(config.getProperty("key2"), "value2");
    }

    @Test
    public void testGetPropertyWithDefaultValue() {
        config.setProperty("existing", "value");
        
        assertEquals(config.getProperty("existing", "default"), "value");
        assertEquals(config.getProperty("nonexistent", "default"), "default");
    }

    @Test
    public void testGetPropertyWithNullKey() {
        assertNull(config.getProperty(null));
        assertEquals(config.getProperty(null, "default"), "default");
    }

    @Test
    public void testSetPropertyWithNullKey() {
        config.setProperty(null, "value");
        assertEquals(config.getProperty(null), "value");
    }

    @Test
    public void testSetPropertyWithNullValue() {
        config.setProperty("key", null);
        assertNull(config.getProperty("key"));
    }

    @Test
    public void testGetAllProperties() {
        config.setProperty("key1", "value1");
        config.setProperty("key2", "value2");
        
        Map<String, String> props = config.getAllProperties();
        assertNotNull(props);
        assertEquals(props.get("key1"), "value1");
        assertEquals(props.get("key2"), "value2");
    }

    @Test
    public void testGetAllPropertiesReturnsNewInstance() {
        config.setProperty("key", "value");
        
        Map<String, String> props1 = config.getAllProperties();
        Map<String, String> props2 = config.getAllProperties();
        
        assertNotSame(props1, props2);
        assertEquals(props1.get("key"), "value");
        assertEquals(props2.get("key"), "value");
    }

    @Test
    public void testGetProperties() {
        config.setProperty("key1", "value1");
        config.setProperty("key2", "value2");
        
        Properties props = config.getProperties();
        assertNotNull(props);
        assertEquals(props.getProperty("key1"), "value1");
        assertEquals(props.getProperty("key2"), "value2");
    }

    @Test
    public void testGetPropertiesReturnsNewInstance() {
        config.setProperty("key", "value");
        
        Properties props1 = config.getProperties();
        Properties props2 = config.getProperties();
        
        assertNotSame(props1, props2);
        assertEquals(props1.getProperty("key"), "value");
        assertEquals(props2.getProperty("key"), "value");
    }

    @Test
    public void testClear() {
        config.setProperty("key1", "value1");
        config.setProperty("key2", "value2");
        
        config.clear();
        
        assertNull(config.getProperty("key1"));
        assertNull(config.getProperty("key2"));
        assertEquals(config.size(), 0);
    }

    @Test
    public void testClearWhenEmpty() {
        config.clear();
        
        assertEquals(config.size(), 0);
    }

    @Test
    public void testHasProperty() {
        config.setProperty("key", "value");
        
        assertTrue(config.hasProperty("key"));
        assertFalse(config.hasProperty("nonexistent"));
    }

    @Test
    public void testHasPropertyWithNull() {
        config.setProperty(null, "value");
        
        assertTrue(config.hasProperty(null));
    }

    @Test
    public void testRemoveProperty() {
        config.setProperty("key", "value");
        
        assertTrue(config.hasProperty("key"));
        String removed = config.removeProperty("key");
        assertEquals(removed, "value");
        assertFalse(config.hasProperty("key"));
        assertNull(config.getProperty("key"));
    }

    @Test
    public void testRemovePropertyWithNull() {
        config.setProperty(null, "value");
        
        assertTrue(config.hasProperty(null));
        String removed = config.removeProperty(null);
        assertEquals(removed, "value");
        assertFalse(config.hasProperty(null));
    }

    @Test
    public void testRemovePropertyNonExistent() {
        String removed = config.removeProperty("nonexistent");
        assertNull(removed);
    }

    @Test
    public void testSize() {
        assertEquals(config.size(), 0);
        
        config.setProperty("key1", "value1");
        assertEquals(config.size(), 1);
        
        config.setProperty("key2", "value2");
        assertEquals(config.size(), 2);
        
        config.removeProperty("key1");
        assertEquals(config.size(), 1);
        
        config.clear();
        assertEquals(config.size(), 0);
    }

    @Test
    public void testIsEmpty() {
        assertTrue(config.isEmpty());
        
        config.setProperty("key", "value");
        assertFalse(config.isEmpty());
        
        config.clear();
        assertTrue(config.isEmpty());
    }

    @Test
    public void testToString() {
        config.setProperty("key1", "value1");
        config.setProperty("key2", "value2");
        
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("key1"));
        assertTrue(str.contains("key2"));
        assertTrue(str.contains("value1"));
        assertTrue(str.contains("value2"));
    }

    @Test
    public void testToStringWithNoProperties() {
        String str = config.toString();
        assertNotNull(str);
    }

    @Test
    public void testBooleanProperty() {
        config.setProperty("bool", true);
        
        assertTrue(config.getBooleanProperty("bool", false));
        assertTrue(config.getBooleanProperty("bool", true));
        assertFalse(config.getBooleanProperty("nonexistent", false));
        assertTrue(config.getBooleanProperty("nonexistent", true));
    }

    @Test
    public void testIntProperty() {
        config.setProperty("int", 42);
        
        assertEquals(config.getIntProperty("int", 0), 42);
        assertEquals(config.getIntProperty("int", 100), 42);
        assertEquals(config.getIntProperty("nonexistent", 0), 0);
        assertEquals(config.getIntProperty("nonexistent", 100), 100);
    }

    @Test
    public void testLongProperty() {
        config.setProperty("long", 123456789L);
        
        assertEquals(config.getLongProperty("long", 0L), 123456789L);
        assertEquals(config.getLongProperty("long", 100L), 123456789L);
        assertEquals(config.getLongProperty("nonexistent", 0L), 0L);
        assertEquals(config.getLongProperty("nonexistent", 100L), 100L);
    }

    @Test
    public void testDoubleProperty() {
        config.setProperty("double", 3.14);
        
        assertEquals(config.getDoubleProperty("double", 0.0), 3.14, 0.001);
        assertEquals(config.getDoubleProperty("double", 100.0), 3.14, 0.001);
        assertEquals(config.getDoubleProperty("nonexistent", 0.0), 0.0, 0.001);
        assertEquals(config.getDoubleProperty("nonexistent", 100.0), 100.0, 0.001);
    }

    @Test
    public void testSetPropertyWithBoolean() {
        config.setProperty("bool", true);
        assertEquals(config.getProperty("bool"), "true");
        
        config.setProperty("bool", false);
        assertEquals(config.getProperty("bool"), "false");
    }

    @Test
    public void testSetPropertyWithInt() {
        config.setProperty("int", 42);
        assertEquals(config.getProperty("int"), "42");
        
        config.setProperty("int", -1);
        assertEquals(config.getProperty("int"), "-1");
    }

    @Test
    public void testSetPropertyWithLong() {
        config.setProperty("long", 123456789L);
        assertEquals(config.getProperty("long"), "123456789");
        
        config.setProperty("long", -1L);
        assertEquals(config.getProperty("long"), "-1");
    }

    @Test
    public void testSetPropertyWithDouble() {
        config.setProperty("double", 3.14);
        assertEquals(config.getProperty("double"), "3.14");
        
        config.setProperty("double", -1.0);
        assertEquals(config.getProperty("double"), "-1.0");
    }

    @Test
    public void testConstructorWithProperties() {
        Properties props = new Properties();
        props.setProperty("prop1", "value1");
        props.setProperty("prop2", "value2");
        
        DefaultPluginConfiguration testConfig = new DefaultPluginConfiguration("plugin-id", "namespace", "element", props);
        
        assertEquals(testConfig.getPluginId(), "plugin-id");
        assertEquals(testConfig.getNamespace(), "namespace");
        assertEquals(testConfig.getElementName(), "element");
        assertEquals(testConfig.getProperty("prop1"), "value1");
        assertEquals(testConfig.getProperty("prop2"), "value2");
    }

    @Test
    public void testConstructorWithMap() {
        Map<String, String> map = new java.util.HashMap<>();
        map.put("map1", "value1");
        map.put("map2", "value2");
        
        DefaultPluginConfiguration testConfig = new DefaultPluginConfiguration("plugin-id", "namespace", "element", map);
        
        assertEquals(testConfig.getPluginId(), "plugin-id");
        assertEquals(testConfig.getNamespace(), "namespace");
        assertEquals(testConfig.getElementName(), "element");
        assertEquals(testConfig.getProperty("map1"), "value1");
        assertEquals(testConfig.getProperty("map2"), "value2");
    }
}