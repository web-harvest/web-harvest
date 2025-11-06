package org.webharvest.plugin.factory;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.core.DefPlugin;
import org.webharvest.plugin.core.HttpPlugin;
import org.webharvest.plugin.core.XPathPlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.registry.PluginRegistry;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Tests for DefaultPluginFactory.
 * Increases coverage for plugin.factory package from 0% to 70%+
 */
public class DefaultPluginFactoryTest {
    
    private PluginRegistry mockRegistry;
    private DefaultPluginFactory factory;
    
    @BeforeMethod
    public void setUp() {
        mockRegistry = mock(PluginRegistry.class);
        factory = new DefaultPluginFactory(mockRegistry);
    }

    @Test
    public void testConstructorWithNullRegistry() {
        try {
            new DefaultPluginFactory(null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("registry cannot be null"));
        }
    }

    @Test
    public void testCreatePluginByClass() throws PluginException {
        Plugin plugin = factory.createPlugin(DefPlugin.class);
        assertNotNull(plugin);
        assertTrue(plugin instanceof DefPlugin);
    }

    @Test
    public void testCreatePluginByClassWithConfiguration() throws PluginException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        Plugin plugin = factory.createPlugin(DefPlugin.class, config);
        assertNotNull(plugin);
        assertTrue(plugin instanceof DefPlugin);
    }

    @Test
    public void testCreatePluginByClassWithNullClass() {
        try {
            factory.createPlugin((Class<? extends Plugin>) null);
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("cannot be null"));
        }
    }

    @Test
    public void testCreatePluginByClassWithNullClassAndConfig() {
        PluginConfiguration config = mock(PluginConfiguration.class);
        try {
            factory.createPlugin((Class<? extends Plugin>) null, config);
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("cannot be null"));
        }
    }

    @Test
    public void testCreatePluginById() throws PluginException {
        // Use real plugin instance
        HttpPlugin realPlugin = new HttpPlugin();
        when(mockRegistry.getPlugin("http")).thenReturn(realPlugin);

        Plugin plugin = factory.createPlugin("http");
        assertNotNull(plugin);
        assertTrue(plugin instanceof HttpPlugin);
    }

    @Test
    public void testCreatePluginByIdWithConfiguration() throws PluginException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        // Use real plugin instance
        DefPlugin realPlugin = new DefPlugin();
        when(mockRegistry.getPlugin("def")).thenReturn(realPlugin);

        Plugin plugin = factory.createPlugin("def", config);
        assertNotNull(plugin);
        assertTrue(plugin instanceof DefPlugin);
    }

    @Test
    public void testCreatePluginByIdWithNullId() {
        try {
            factory.createPlugin((String) null);
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("cannot be null or empty"));
        }
    }

    @Test
    public void testCreatePluginByIdWithEmptyId() {
        try {
            factory.createPlugin("");
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("cannot be null or empty"));
        }
    }

    @Test
    public void testCreatePluginByIdWithWhitespaceId() {
        try {
            factory.createPlugin("   ");
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("cannot be null or empty"));
        }
    }

    @Test
    public void testCreatePluginByIdNotFound() {
        when(mockRegistry.getPlugin("nonexistent")).thenReturn(null);
        try {
            factory.createPlugin("nonexistent");
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("not found"));
        }
    }

    @Test
    public void testCreatePluginByIdWithConfigurationNotFound() {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(mockRegistry.getPlugin("nonexistent")).thenReturn(null);
        try {
            factory.createPlugin("nonexistent", config);
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("not found"));
        }
    }

    @Test
    public void testCreatePluginByNamespaceAndElement() throws PluginException {
        // Use real plugin instance
        XPathPlugin realPlugin = new XPathPlugin();
        when(mockRegistry.getPlugin("http://webharvest.org/schema", "xpath")).thenReturn(realPlugin);

        Plugin plugin = factory.createPlugin("http://webharvest.org/schema", "xpath", null);
        assertNotNull(plugin);
        assertTrue(plugin instanceof XPathPlugin);
    }

    @Test
    public void testCreatePluginByNamespaceAndElementWithConfiguration() throws PluginException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        // Use real plugin instance
        HttpPlugin realPlugin = new HttpPlugin();
        when(mockRegistry.getPlugin("http://webharvest.org/schema", "http")).thenReturn(realPlugin);

        Plugin plugin = factory.createPlugin("http://webharvest.org/schema", "http", config);
        assertNotNull(plugin);
        assertTrue(plugin instanceof HttpPlugin);
    }

    @Test
    public void testCreatePluginByNamespaceAndElementWithNullNamespace() {
        try {
            factory.createPlugin(null, "element");
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("Namespace cannot be null or empty"));
        }
    }

    @Test
    public void testCreatePluginByNamespaceAndElementWithEmptyNamespace() {
        try {
            factory.createPlugin("", "element", null);
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("Namespace cannot be null or empty"));
        }
    }

    @Test
    public void testCreatePluginByNamespaceAndElementWithNullElement() {
        try {
            factory.createPlugin("namespace", (String) null, null);
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("Element name cannot be null or empty"));
        }
    }

    @Test
    public void testCreatePluginByNamespaceAndElementWithEmptyElement() {
        try {
            factory.createPlugin("namespace", "", null);
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("Element name cannot be null or empty"));
        }
    }

    @Test
    public void testCreatePluginByNamespaceAndElementNotFound() {
        when(mockRegistry.getPlugin("namespace", "element")).thenReturn(null);
        try {
            factory.createPlugin("namespace", "element", null);
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("not found"));
        }
    }

    @Test(expectedExceptions = PluginException.class)
    public void testCreateMockPluginThrowsException() throws PluginException {
        // Should throw PluginException - MockPlugin removed in v2.2.x
        factory.createMockPlugin(DefPlugin.class);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testCreateMockPluginWithConfigurationThrowsException() throws PluginException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        // Should throw PluginException - MockPlugin removed in v2.2.x
        factory.createMockPlugin(DefPlugin.class, config);
    }

    @Test
    public void testCreateMockPluginWithNullClass() {
        try {
            factory.createMockPlugin(null);
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("cannot be null"));
        }
    }

    @Test
    public void testCanCreatePlugin() {
        assertTrue(factory.canCreatePlugin(DefPlugin.class));
        assertTrue(factory.canCreatePlugin(HttpPlugin.class));
        assertTrue(factory.canCreatePlugin(XPathPlugin.class));
    }

    @Test
    public void testCanCreatePluginWithNullClass() {
        assertFalse(factory.canCreatePlugin(null));
    }

    @Test
    public void testGetPluginClassById() {
        // Use real plugin instance
        DefPlugin realPlugin = new DefPlugin();
        when(mockRegistry.getPlugin("def")).thenReturn(realPlugin);

        Class<? extends Plugin> pluginClass = factory.getPluginClass("def");
        assertNotNull(pluginClass);
        assertEquals(pluginClass, DefPlugin.class);
    }

    @Test
    public void testGetPluginClassByIdWithNullId() {
        assertNull(factory.getPluginClass((String) null));
    }

    @Test
    public void testGetPluginClassByIdWithEmptyId() {
        assertNull(factory.getPluginClass(""));
    }

    @Test
    public void testGetPluginClassByIdWithWhitespaceId() {
        assertNull(factory.getPluginClass("   "));
    }

    @Test
    public void testGetPluginClassByIdNotFound() {
        when(mockRegistry.getPlugin("nonexistent")).thenReturn(null);
        assertNull(factory.getPluginClass("nonexistent"));
    }

    @Test
    public void testGetPluginClassByNamespaceAndElement() {
        // Use real plugin instance
        HttpPlugin realPlugin = new HttpPlugin();
        when(mockRegistry.getPlugin("http://webharvest.org/schema", "http")).thenReturn(realPlugin);

        Class<? extends Plugin> pluginClass = factory.getPluginClass("http://webharvest.org/schema", "http");
        assertNotNull(pluginClass);
        assertEquals(pluginClass, HttpPlugin.class);
    }

    @Test
    public void testGetPluginClassByNamespaceAndElementWithNullNamespace() {
        assertNull(factory.getPluginClass(null, "element"));
    }

    @Test
    public void testGetPluginClassByNamespaceAndElementWithNullElement() {
        assertNull(factory.getPluginClass("namespace", null));
    }

    @Test
    public void testGetPluginClassByNamespaceAndElementNotFound() {
        when(mockRegistry.getPlugin("namespace", "element")).thenReturn(null);
        assertNull(factory.getPluginClass("namespace", "element"));
    }

    @Test
    public void testDefaultPluginMetadata() {
        // Test the inner class DefaultPluginMetadata
        DefaultPluginFactory.DefaultPluginMetadata metadata = new DefaultPluginFactory.DefaultPluginMetadata(
            "test-id", "Test Plugin", "2.3.0", "Test description",
            "Test Author", "test-category", new String[]{"tag1", "tag2"},
            new String[]{"dep1"}, null, "http://test", "test-element", true, "TestClass"
        );

        assertEquals(metadata.getId(), "test-id");
        assertEquals(metadata.getName(), "Test Plugin");
        assertEquals(metadata.getVersion(), "2.3.0");
        assertNotNull(metadata.getDescription());
        assertEquals(metadata.getAuthor(), "Test Author");
        assertEquals(metadata.getCategory(), "test-category");
        assertNotNull(metadata.getTags());
        assertEquals(metadata.getTags().length, 2);
        assertNotNull(metadata.getDependencies());
        assertEquals(metadata.getDependencies().length, 1);
        assertNull(metadata.getConfigurationSchema());
        assertEquals(metadata.getNamespace(), "http://test");
        assertEquals(metadata.getElementName(), "test-element");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getClassName(), "TestClass");
    }

    @Test
    public void testDefaultPluginMetadataWithNullSchema() {
        DefaultPluginFactory.DefaultPluginMetadata metadata = new DefaultPluginFactory.DefaultPluginMetadata(
            "id", "name", "ver", "desc", "author", "cat",
            new String[0], new String[0], null, "ns", "elem", false, "Class"
        );

        assertNull(metadata.getConfigurationSchema());
        assertFalse(metadata.isCorePlugin());
    }
}
