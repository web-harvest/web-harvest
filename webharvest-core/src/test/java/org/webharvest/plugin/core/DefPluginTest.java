package org.webharvest.plugin.core;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Metadata contract for DefPlugin.
 *
 * <p>Variable assignment and child execution are covered by the XML runtime
 * integration tests; this class does not duplicate those contracts with
 * context-only setup tests.</p>
 */
public class DefPluginTest {

    @Test(description = "Test DefPlugin metadata")
    public void testDefPluginMetadata() {
        DefPlugin plugin = new DefPlugin();
        assertNotNull(plugin.getMetadata(), "Plugin should have metadata");
        assertEquals(plugin.getMetadata().getName(), "Define Variable",
                "Plugin name should be 'Define Variable'");
    }
}
