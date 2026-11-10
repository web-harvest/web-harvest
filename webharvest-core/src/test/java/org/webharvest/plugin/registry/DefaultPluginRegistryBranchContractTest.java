package org.webharvest.plugin.registry;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;

/**
 * Additional registration, lookup, search, and statistics branches for {@link DefaultPluginRegistry}.
 */
public class DefaultPluginRegistryBranchContractTest {

    private DefaultPluginRegistry registry;

    @BeforeMethod
    public void setUp() {
        registry = new DefaultPluginRegistry();
    }

    private Plugin plugin(String id, String name, String description, String category, String namespace, String element) {
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        when(plugin.getMetadata()).thenReturn(metadata);
        when(metadata.getId()).thenReturn(id);
        when(metadata.getName()).thenReturn(name);
        when(metadata.getDescription()).thenReturn(description);
        when(metadata.getCategory()).thenReturn(category);
        when(metadata.getNamespace()).thenReturn(namespace);
        when(metadata.getElementName()).thenReturn(element);
        return plugin;
    }

    @Test
    public void unregisterRemovesNamespaceElementMappingAndPrunesCategory() throws Exception {
        Plugin first = plugin("p1", "One", "First plugin", "tools", "http://ns", "one");
        Plugin second = plugin("p2", "Two", "Second plugin", "tools", "http://ns", "two");
        registry.registerPlugin(first);
        registry.registerPlugin(second);

        assertTrue(registry.isPluginRegistered("http://ns", "one"));
        registry.unregisterPlugin("p1");
        assertFalse(registry.isPluginRegistered("http://ns", "one"));
        assertTrue(registry.isPluginRegistered("http://ns", "two"));

        List<Plugin> tools = registry.getPluginsByCategory("tools");
        assertEquals(tools.size(), 1);

        registry.unregisterPlugin("p2");
        assertTrue(registry.getPluginsByCategory("tools").isEmpty());
        assertTrue(registry.getPluginsByNamespace("http://ns").isEmpty());
    }

    @Test
    public void searchMatchesIdWhenNameAndDescriptionAreNull() throws Exception {
        Plugin sparse = plugin("fetch-data", null, null, null, null, null);
        registry.registerPlugin(sparse);

        List<Plugin> hits = registry.searchPlugins("fetch");
        assertEquals(hits.size(), 1);
        assertEquals(hits.get(0), sparse);
    }

    @Test
    public void statisticsReflectMultipleCategoriesAndNamespaces() throws Exception {
        registry.registerPlugin(plugin("a", "A", "alpha", "cat-a", "http://a", "a"));
        registry.registerPlugin(plugin("b", "B", "beta", "cat-b", "http://b", "b"));

        Map<String, Object> stats = registry.getPluginStatistics();
        assertEquals(stats.get("totalPlugins"), 2);
        assertEquals(stats.get("categories"), 2);
        assertEquals(stats.get("namespaces"), 2);

        @SuppressWarnings("unchecked")
        Map<String, Integer> byCategory = (Map<String, Integer>) stats.get("pluginsByCategory");
        assertEquals(byCategory.get("cat-a"), Integer.valueOf(1));
        assertEquals(byCategory.get("cat-b"), Integer.valueOf(1));
    }

    @Test(expectedExceptions = PluginException.class)
    public void registerClassRejectsAbstractPluginType() throws Exception {
        registry.registerPlugin(AbstractStubPlugin.class);
    }

    private abstract static class AbstractStubPlugin implements Plugin {
    }
}
