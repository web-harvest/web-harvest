package org.webharvest.plugin.registry;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.definition.IElementDef;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Test for DefaultPluginRegistry.
 */
public class DefaultPluginRegistryTest {

    private DefaultPluginRegistry registry;
    private Plugin mockPlugin;
    private PluginMetadata mockMetadata;

    @BeforeMethod
    public void setUp() {
        registry = new DefaultPluginRegistry();
        mockPlugin = mock(Plugin.class);
        mockMetadata = mock(PluginMetadata.class);
        
        when(mockPlugin.getMetadata()).thenReturn(mockMetadata);
        when(mockMetadata.getId()).thenReturn("test-plugin");
        when(mockMetadata.getName()).thenReturn("Test Plugin");
        when(mockMetadata.getDescription()).thenReturn("A test plugin");
        when(mockMetadata.getNamespace()).thenReturn("http://test.namespace");
        when(mockMetadata.getElementName()).thenReturn("test-element");
        when(mockMetadata.getCategory()).thenReturn("test-category");
    }

    @Test
    public void testRegisterPlugin() throws PluginException {
        registry.registerPlugin((Plugin) mockPlugin);
        
        assertEquals(registry.getPluginCount(), 1);
        assertTrue(registry.isPluginRegistered("test-plugin"));
        assertEquals(registry.getPlugin("test-plugin"), mockPlugin);
        assertEquals(registry.getPluginMetadata("test-plugin"), mockMetadata);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testRegisterPluginWithNullPlugin() throws PluginException {
        registry.registerPlugin((Plugin) null);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testRegisterPluginWithNullMetadata() throws PluginException {
        when(mockPlugin.getMetadata()).thenReturn(null);
        registry.registerPlugin((Plugin) mockPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testRegisterPluginWithNullId() throws PluginException {
        when(mockMetadata.getId()).thenReturn(null);
        registry.registerPlugin((Plugin) mockPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testRegisterPluginWithEmptyId() throws PluginException {
        when(mockMetadata.getId()).thenReturn("   ");
        registry.registerPlugin((Plugin) mockPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testRegisterPluginDuplicate() throws PluginException {
        registry.registerPlugin((Plugin) mockPlugin);
        registry.registerPlugin((Plugin) mockPlugin);
    }

    @Test
    public void testRegisterPluginWithNamespaceAndElement() throws PluginException {
        registry.registerPlugin((Plugin) mockPlugin);
        
        assertTrue(registry.isPluginRegistered("http://test.namespace", "test-element"));
        assertEquals(registry.getPlugin("http://test.namespace", "test-element"), mockPlugin);
    }

    @Test
    public void testRegisterPluginWithCategory() throws PluginException {
        registry.registerPlugin((Plugin) mockPlugin);
        
        List<Plugin> categoryPlugins = registry.getPluginsByCategory("test-category");
        assertEquals(categoryPlugins.size(), 1);
        assertEquals(categoryPlugins.get(0), mockPlugin);
    }

    @Test
    public void testRegisterPluginWithNamespace() throws PluginException {
        registry.registerPlugin((Plugin) mockPlugin);
        
        List<Plugin> namespacePlugins = registry.getPluginsByNamespace("http://test.namespace");
        assertEquals(namespacePlugins.size(), 1);
        assertEquals(namespacePlugins.get(0), mockPlugin);
    }

    @Test
    public void testRegisterPluginWithNullNamespace() throws PluginException {
        when(mockMetadata.getNamespace()).thenReturn(null);
        registry.registerPlugin((Plugin) mockPlugin);
        
        assertFalse(registry.isPluginRegistered("http://test.namespace", "test-element"));
        assertTrue(registry.isPluginRegistered("test-plugin"));
    }

    @Test
    public void testRegisterPluginWithNullElementName() throws PluginException {
        when(mockMetadata.getElementName()).thenReturn(null);
        registry.registerPlugin((Plugin) mockPlugin);
        
        assertFalse(registry.isPluginRegistered("http://test.namespace", "test-element"));
        assertTrue(registry.isPluginRegistered("test-plugin"));
    }

    @Test
    public void testRegisterPluginWithNullCategory() throws PluginException {
        when(mockMetadata.getCategory()).thenReturn(null);
        registry.registerPlugin((Plugin) mockPlugin);
        
        List<Plugin> categoryPlugins = registry.getPluginsByCategory("test-category");
        assertTrue(categoryPlugins.isEmpty());
        assertTrue(registry.isPluginRegistered("test-plugin"));
    }

    @Test
    public void testRegisterPluginClass() throws PluginException {
        Plugin testPlugin = new TestPlugin();
        registry.registerPlugin(TestPlugin.class);
        
        assertEquals(registry.getPluginCount(), 1);
        assertTrue(registry.isPluginRegistered("test-plugin-class"));
    }

    @Test(expectedExceptions = PluginException.class)
    public void testRegisterPluginClassWithNull() throws PluginException {
        registry.registerPlugin((Class<? extends Plugin>) null);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testRegisterPluginClassInstantiationError() throws PluginException {
        registry.registerPlugin(AbstractTestPlugin.class);
    }

    @Test
    public void testUnregisterPlugin() throws PluginException {
        registry.registerPlugin((Plugin) mockPlugin);
        assertEquals(registry.getPluginCount(), 1);
        
        registry.unregisterPlugin("test-plugin");
        assertEquals(registry.getPluginCount(), 0);
        assertFalse(registry.isPluginRegistered("test-plugin"));
        assertNull(registry.getPlugin("test-plugin"));
        assertNull(registry.getPluginMetadata("test-plugin"));
    }

    @Test(expectedExceptions = PluginException.class)
    public void testUnregisterPluginWithNullId() throws PluginException {
        registry.unregisterPlugin(null);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testUnregisterPluginWithEmptyId() throws PluginException {
        registry.unregisterPlugin("   ");
    }

    @Test(expectedExceptions = PluginException.class)
    public void testUnregisterPluginNotFound() throws PluginException {
        registry.unregisterPlugin("non-existent-plugin");
    }

    @Test
    public void testUnregisterPluginRemovesFromCategory() throws PluginException {
        registry.registerPlugin((Plugin) mockPlugin);
        List<Plugin> categoryPlugins = registry.getPluginsByCategory("test-category");
        assertEquals(categoryPlugins.size(), 1);
        
        registry.unregisterPlugin("test-plugin");
        categoryPlugins = registry.getPluginsByCategory("test-category");
        assertTrue(categoryPlugins.isEmpty());
    }

    @Test
    public void testUnregisterPluginRemovesFromNamespace() throws PluginException {
        registry.registerPlugin((Plugin) mockPlugin);
        List<Plugin> namespacePlugins = registry.getPluginsByNamespace("http://test.namespace");
        assertEquals(namespacePlugins.size(), 1);
        
        registry.unregisterPlugin("test-plugin");
        namespacePlugins = registry.getPluginsByNamespace("http://test.namespace");
        assertTrue(namespacePlugins.isEmpty());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testGetPluginWithNullId() {
        registry.getPlugin(null);
    }

    @Test
    public void testGetPluginWithNamespaceAndElement() throws PluginException {
        registry.registerPlugin((Plugin) mockPlugin);
        
        assertEquals(registry.getPlugin("http://test.namespace", "test-element"), mockPlugin);
        assertNull(registry.getPlugin("http://test.namespace", "non-existent"));
        assertNull(registry.getPlugin("http://non-existent", "test-element"));
    }

    @Test
    public void testGetPluginWithNullNamespace() {
        assertNull(registry.getPlugin(null, "test-element"));
    }

    @Test
    public void testGetPluginWithNullElementName() {
        assertNull(registry.getPlugin("http://test.namespace", null));
    }

    @Test
    public void testGetAllPlugins() throws PluginException {
        assertTrue(registry.getAllPlugins().isEmpty());
        
        registry.registerPlugin((Plugin) mockPlugin);
        Collection<Plugin> allPlugins = registry.getAllPlugins();
        assertEquals(allPlugins.size(), 1);
        assertTrue(allPlugins.contains(mockPlugin));
    }

    @Test
    public void testGetPluginsByCategoryWithNullCategory() {
        List<Plugin> plugins = registry.getPluginsByCategory(null);
        assertTrue(plugins.isEmpty());
    }

    @Test
    public void testGetPluginsByCategoryWithNonExistentCategory() {
        List<Plugin> plugins = registry.getPluginsByCategory("non-existent");
        assertTrue(plugins.isEmpty());
    }

    @Test
    public void testGetPluginsByNamespaceWithNullNamespace() {
        List<Plugin> plugins = registry.getPluginsByNamespace(null);
        assertTrue(plugins.isEmpty());
    }

    @Test
    public void testGetPluginsByNamespaceWithNonExistentNamespace() {
        List<Plugin> plugins = registry.getPluginsByNamespace("http://non-existent");
        assertTrue(plugins.isEmpty());
    }

    @Test
    public void testGetPluginMetadataWithNonExistentPlugin() {
        assertNull(registry.getPluginMetadata("non-existent"));
    }

    @Test
    public void testGetAllPluginMetadata() throws PluginException {
        assertTrue(registry.getAllPluginMetadata().isEmpty());
        
        registry.registerPlugin((Plugin) mockPlugin);
        Collection<PluginMetadata> allMetadata = registry.getAllPluginMetadata();
        assertEquals(allMetadata.size(), 1);
        assertTrue(allMetadata.contains(mockMetadata));
    }

    @Test
    public void testSearchPlugins() throws PluginException {
        assertTrue(registry.searchPlugins("test").isEmpty());
        
        registry.registerPlugin((Plugin) mockPlugin);
        
        List<Plugin> results = registry.searchPlugins("test");
        assertEquals(results.size(), 1);
        assertEquals(results.get(0), mockPlugin);
        
        results = registry.searchPlugins("Plugin");
        assertEquals(results.size(), 1);
        
        results = registry.searchPlugins("test");
        assertEquals(results.size(), 1);
        
        results = registry.searchPlugins("category");
        assertEquals(results.size(), 1);
        
        results = registry.searchPlugins("non-existent");
        assertTrue(results.isEmpty());
    }

    @Test
    public void testSearchPluginsWithNullQuery() {
        List<Plugin> results = registry.searchPlugins(null);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testSearchPluginsWithEmptyQuery() {
        List<Plugin> results = registry.searchPlugins("   ");
        assertTrue(results.isEmpty());
    }

    @Test(expectedExceptions = PluginException.class)
    public void testSearchPluginsWithNullMetadata() throws PluginException {
        when(mockPlugin.getMetadata()).thenReturn(null);
        registry.registerPlugin((Plugin) mockPlugin);
    }

    @Test
    public void testIsPluginRegisteredWithNullId() {
        assertFalse(registry.isPluginRegistered(null));
    }

    @Test
    public void testIsPluginRegisteredWithNamespaceAndElement() throws PluginException {
        assertFalse(registry.isPluginRegistered("http://test.namespace", "test-element"));
        
        registry.registerPlugin((Plugin) mockPlugin);
        assertTrue(registry.isPluginRegistered("http://test.namespace", "test-element"));
        assertFalse(registry.isPluginRegistered("http://test.namespace", "non-existent"));
        assertFalse(registry.isPluginRegistered("http://non-existent", "test-element"));
    }

    @Test
    public void testIsPluginRegisteredWithNullNamespace() {
        assertFalse(registry.isPluginRegistered(null, "test-element"));
    }

    @Test
    public void testIsPluginRegisteredWithNullElementName() {
        assertFalse(registry.isPluginRegistered("http://test.namespace", null));
    }

    @Test
    public void testGetPluginCount() throws PluginException {
        assertEquals(registry.getPluginCount(), 0);
        
        registry.registerPlugin((Plugin) mockPlugin);
        assertEquals(registry.getPluginCount(), 1);
        
        Plugin anotherPlugin = mock(Plugin.class);
        PluginMetadata anotherMetadata = mock(PluginMetadata.class);
        when(anotherPlugin.getMetadata()).thenReturn(anotherMetadata);
        when(anotherMetadata.getId()).thenReturn("another-plugin");
        when(anotherMetadata.getName()).thenReturn("Another Plugin");
        when(anotherMetadata.getDescription()).thenReturn("Another test plugin");
        when(anotherMetadata.getNamespace()).thenReturn("http://another.namespace");
        when(anotherMetadata.getElementName()).thenReturn("another-element");
        when(anotherMetadata.getCategory()).thenReturn("another-category");
        
        registry.registerPlugin(anotherPlugin);
        assertEquals(registry.getPluginCount(), 2);
    }

    @Test
    public void testGetPluginStatistics() throws PluginException {
        Map<String, Object> stats = registry.getPluginStatistics();
        assertEquals(stats.get("totalPlugins"), 0);
        assertEquals(stats.get("categories"), 0);
        assertEquals(stats.get("namespaces"), 0);
        
        registry.registerPlugin((Plugin) mockPlugin);
        stats = registry.getPluginStatistics();
        assertEquals(stats.get("totalPlugins"), 1);
        assertEquals(stats.get("categories"), 1);
        assertEquals(stats.get("namespaces"), 1);
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> categoryCounts = (Map<String, Integer>) stats.get("pluginsByCategory");
        assertEquals(categoryCounts.get("test-category"), Integer.valueOf(1));
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> namespaceCounts = (Map<String, Integer>) stats.get("pluginsByNamespace");
        assertEquals(namespaceCounts.get("http://test.namespace"), Integer.valueOf(1));
    }

    @Test
    public void testClear() throws PluginException {
        registry.registerPlugin((Plugin) mockPlugin);
        assertEquals(registry.getPluginCount(), 1);
        
        registry.clear();
        assertEquals(registry.getPluginCount(), 0);
        assertTrue(registry.getAllPlugins().isEmpty());
        assertTrue(registry.getAllPluginMetadata().isEmpty());
        assertTrue(registry.getPluginsByCategory("test-category").isEmpty());
        assertTrue(registry.getPluginsByNamespace("http://test.namespace").isEmpty());
    }

    @Test
    public void testRefresh() throws PluginException {
        // Default implementation does nothing, so just test it doesn't throw
        registry.refresh();
    }

    // Test plugin classes
    public static class TestPlugin implements Plugin {
        @Override
        public PluginMetadata getMetadata() {
            PluginMetadata metadata = mock(PluginMetadata.class);
            when(metadata.getId()).thenReturn("test-plugin-class");
            when(metadata.getName()).thenReturn("Test Plugin Class");
            when(metadata.getDescription()).thenReturn("A test plugin class");
            when(metadata.getNamespace()).thenReturn("http://test.namespace");
            when(metadata.getElementName()).thenReturn("test-element");
            when(metadata.getCategory()).thenReturn("test-category");
            return metadata;
        }

        public PluginConfigurationSchema getConfigurationSchema() {
            return mock(PluginConfigurationSchema.class);
        }

        @Override
        public int getRunningLevel() {
            return 0;
        }

        @Override
        public Variable execute(DynamicScopeContext context) throws InterruptedException {
            return mock(Variable.class);
        }

        @Override
        public void initialize(PluginConfiguration config) throws PluginException {
            // No-op
        }

        @Override
        public void validateConfiguration(PluginConfiguration config) throws PluginValidationException {
            // No-op
        }

        @Override
        public boolean isInitialized() {
            return true;
        }

        @Override
        public void cleanup() {
            // No-op
        }

        @Override
        public IElementDef getElementDef() {
            return null;
        }

        @Override
        public void setElementDef(IElementDef elementDef) {
            // No-op
        }

        @Override
        public Plugin getParentPlugin() {
            return null;
        }

        @Override
        public void setParentPlugin(Plugin parentPlugin) {
            // No-op
        }
    }

    private static abstract class AbstractTestPlugin implements Plugin {
        // Abstract class that cannot be instantiated
    }
}
