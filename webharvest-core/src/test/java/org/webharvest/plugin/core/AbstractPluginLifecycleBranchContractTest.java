package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Lifecycle contracts on {@link org.webharvest.plugin.AbstractPlugin}:
 * parent/running-level, cleanup idempotence, and double-initialize rejection.
 */
public class AbstractPluginLifecycleBranchContractTest {

    @Test
    public void setParentPluginAdjustsRunningLevelAndCleanupResetsState() throws Exception {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty("language")).thenReturn(null);
        when(configuration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(configuration.getProperty("id", "")).thenReturn("");
        when(configuration.getProperty("_textContent")).thenReturn("x");
        when(configuration.hasChildren()).thenReturn(false);

        TemplatePlugin parent = new TemplatePlugin();
        parent.initialize(configuration);
        assertEquals(parent.getRunningLevel(), 1);

        PluginConfiguration childConfig = mock(PluginConfiguration.class);
        when(childConfig.getProperty("language")).thenReturn(null);
        when(childConfig.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(childConfig.getProperty("id", "")).thenReturn("");
        when(childConfig.getProperty("_textContent")).thenReturn("y");
        when(childConfig.hasChildren()).thenReturn(false);

        TemplatePlugin child = new TemplatePlugin();
        child.setParentPlugin(parent);
        assertEquals(child.getRunningLevel(), parent.getRunningLevel() + 1);
        assertSame(child.getParentPlugin(), parent);

        child.setParentPlugin(null);
        assertEquals(child.getRunningLevel(), 1);

        child.initialize(childConfig);
        assertTrue(child.isInitialized());
        child.cleanup();
        assertFalse(child.isInitialized());
        child.cleanup(); // idempotent when already cleaned
        assertFalse(child.isInitialized());
    }

    @Test(expectedExceptions = Exception.class)
    public void initializeRejectsSecondCall() throws Exception {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty("language")).thenReturn(null);
        when(configuration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(configuration.getProperty("id", "")).thenReturn("");
        when(configuration.hasChildren()).thenReturn(false);

        TemplatePlugin plugin = new TemplatePlugin();
        plugin.initialize(configuration);
        plugin.initialize(configuration);
    }

    @Test
    public void executeWithNullContextFailsFast() throws Exception {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty("language")).thenReturn(null);
        when(configuration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(configuration.getProperty("id", "")).thenReturn("");
        when(configuration.getProperty("_textContent")).thenReturn("ok");
        when(configuration.hasChildren()).thenReturn(false);

        TemplatePlugin plugin = new TemplatePlugin();
        plugin.initialize(configuration);
        try {
            plugin.execute((DynamicScopeContext) null);
            fail("Expected IllegalArgumentException for null context");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("context"));
        }
    }
}
