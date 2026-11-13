package org.webharvest.plugin.registry;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;

/**
 * Residual unregister edge contracts: null/blank IDs, missing plugins, and
 * plugins whose metadata omits namespace/category mappings.
 */
public class DefaultPluginRegistryUnregisterResidualBranchContractTest {

    private DefaultPluginRegistry registry;

    @BeforeMethod
    public void setUp() {
        registry = new DefaultPluginRegistry();
    }

    @Test(expectedExceptions = PluginException.class)
    public void unregisterRejectsNullPluginId() throws Exception {
        registry.unregisterPlugin(null);
    }

    @Test(expectedExceptions = PluginException.class)
    public void unregisterRejectsBlankPluginId() throws Exception {
        registry.unregisterPlugin("   ");
    }

    @Test(expectedExceptions = PluginException.class)
    public void unregisterRejectsUnknownPluginId() throws Exception {
        registry.unregisterPlugin("missing-plugin");
    }

    @Test
    public void unregisterSucceedsWhenMetadataOmitsNamespaceAndCategory() throws Exception {
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        when(plugin.getMetadata()).thenReturn(metadata);
        when(metadata.getId()).thenReturn("sparse");
        when(metadata.getName()).thenReturn("Sparse");
        when(metadata.getDescription()).thenReturn("no ns");
        when(metadata.getCategory()).thenReturn(null);
        when(metadata.getNamespace()).thenReturn(null);
        when(metadata.getElementName()).thenReturn(null);

        registry.registerPlugin(plugin);
        registry.unregisterPlugin("sparse");
        assertNull(registry.getPlugin("sparse"));
    }

    @Test
    public void unregisterSucceedsWhenMetadataItselfIsNull() throws Exception {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getMetadata()).thenReturn(null);
        // Registration requires metadata in DefaultPluginRegistry; simulate via
        // a plugin that registers with metadata then clears lookup by direct unregister
        // after re-stubbing is not possible. Instead register a minimal metadata plugin
        // and verify a second sparse unregister path via category-null above.
        PluginMetadata metadata = mock(PluginMetadata.class);
        when(plugin.getMetadata()).thenReturn(metadata);
        when(metadata.getId()).thenReturn("nullable-meta");
        when(metadata.getName()).thenReturn("N");
        when(metadata.getDescription()).thenReturn("d");
        when(metadata.getCategory()).thenReturn("");
        when(metadata.getNamespace()).thenReturn("");
        when(metadata.getElementName()).thenReturn("el");

        registry.registerPlugin(plugin);
        // After registration, force getMetadata to return null during unregister.
        when(plugin.getMetadata()).thenReturn(null);
        registry.unregisterPlugin("nullable-meta");
        assertNull(registry.getPlugin("nullable-meta"));
    }
}
