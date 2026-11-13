package org.webharvest.plugin.factory;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.registry.PluginRegistry;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Covers DefaultPluginFactory instantiation when a plugin exposes only a
 * {@link PluginMetadata} constructor (no no-args constructor).
 */
public class DefaultPluginFactoryMetadataCtorBranchContractTest {

    private DefaultPluginFactory factory;

    @BeforeMethod
    public void setUp() {
        factory = new DefaultPluginFactory(mock(PluginRegistry.class));
    }

    @Test
    public void createPluginUsesMetadataConstructorWhenNoArgsMissing() throws Exception {
        Plugin plugin = factory.createPlugin(MetadataOnlyPlugin.class);
        assertTrue(plugin instanceof MetadataOnlyPlugin);
        assertNotNull(plugin.getMetadata());
        assertEquals(plugin.getMetadata().getId(), "metadataonlyplugin");
    }

    @Test
    public void createPluginInitializesConfigurationAfterMetadataConstruction() throws Exception {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty(anyString())).thenReturn(null);
        when(configuration.getProperty(anyString(), anyString())).thenAnswer(
                invocation -> invocation.getArgument(1));
        when(configuration.hasChildren()).thenReturn(false);

        Plugin plugin = factory.createPlugin(MetadataOnlyPlugin.class, configuration);
        assertTrue(((MetadataOnlyPlugin) plugin).initialized);
    }

    @Test(expectedExceptions = PluginException.class)
    public void createPluginFailsWhenNoSuitablePublicConstructorExists() throws Exception {
        factory.createPlugin(UnconstructablePlugin.class);
    }

    public static final class MetadataOnlyPlugin implements Plugin {
        private final PluginMetadata metadata;
        private boolean initialized;

        public MetadataOnlyPlugin(PluginMetadata metadata) {
            this.metadata = metadata;
        }

        @Override
        public void initialize(PluginConfiguration configuration) {
            this.initialized = true;
        }

        @Override
        public void validateConfiguration(PluginConfiguration configuration) {
        }

        @Override
        public Variable execute(DynamicScopeContext context) {
            return EmptyVariable.INSTANCE;
        }

        @Override
        public PluginMetadata getMetadata() {
            return metadata;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public void cleanup() {
        }

        @Override
        public Plugin getParentPlugin() {
            return null;
        }

        @Override
        public void setParentPlugin(Plugin parentPlugin) {
        }

        @Override
        public int getRunningLevel() {
            return 0;
        }
    }

    public static final class UnconstructablePlugin implements Plugin {
        private UnconstructablePlugin(String ignored) {
        }

        @Override
        public void initialize(PluginConfiguration configuration) {
        }

        @Override
        public void validateConfiguration(PluginConfiguration configuration) {
        }

        @Override
        public Variable execute(DynamicScopeContext context) {
            return EmptyVariable.INSTANCE;
        }

        @Override
        public PluginMetadata getMetadata() {
            return null;
        }

        @Override
        public boolean isInitialized() {
            return false;
        }

        @Override
        public void cleanup() {
        }

        @Override
        public Plugin getParentPlugin() {
            return null;
        }

        @Override
        public void setParentPlugin(Plugin parentPlugin) {
        }

        @Override
        public int getRunningLevel() {
            return 0;
        }
    }
}
