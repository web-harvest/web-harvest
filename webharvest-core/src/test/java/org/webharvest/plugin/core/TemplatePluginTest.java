package org.webharvest.plugin.core;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Metadata contract for TemplatePlugin.
 *
 * <p>Template execution scenarios must be covered by XML runtime tests. The
 * former context-only and unconditional-success tests did not execute the
 * plugin and are intentionally not retained as behavior evidence.</p>
 */
public class TemplatePluginTest {

    @Test(description = "Test template plugin metadata")
    public void testTemplatePluginMetadata() {
        TemplatePlugin plugin = new TemplatePlugin();
        assertNotNull(plugin.getMetadata(), "Plugin should have metadata");
        assertEquals(plugin.getMetadata().getName(), "Template",
                "Plugin name should be 'Template Plugin'");
    }
}
