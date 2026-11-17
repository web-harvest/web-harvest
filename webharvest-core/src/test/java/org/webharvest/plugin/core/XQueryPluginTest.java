package org.webharvest.plugin.core;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Metadata contract for XQueryPlugin.
 *
 * <p>Execution scenarios are covered by the XML runtime integration suite.
 * Parameter, arithmetic and empty-result edge cases remain tracked in Issue #98
 * and are deliberately not represented by placeholder unit tests.</p>
 */
public class XQueryPluginTest {

    @Test(description = "Test XQuery plugin metadata")
    public void testXQueryPluginMetadata() {
        XQueryPlugin plugin = new XQueryPlugin();
        assertNotNull(plugin.getMetadata(), "Plugin should have metadata");
        assertEquals(plugin.getMetadata().getName(), "XQuery",
                "Plugin name should be 'XQuery Plugin'");
    }
}
