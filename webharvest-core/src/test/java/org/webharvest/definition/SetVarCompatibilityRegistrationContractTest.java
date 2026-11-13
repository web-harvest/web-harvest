package org.webharvest.definition;

import static org.testng.Assert.*;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.core.SetVarPlugin;

/**
 * Compatibility contract: legacy {@code set-var} remains registered beside modern aliases.
 */
public class SetVarCompatibilityRegistrationContractTest {

    @BeforeMethod
    public void setUp() {
        CorePluginsRegistrar.reset();
        CorePluginsRegistrar.registerCorePlugins();
    }

    @AfterMethod
    public void tearDown() {
        CorePluginsRegistrar.reset();
    }

    @Test
    public void registrarKeepsSetVarCompatibilityAliasMapped() {
        Class<?> mapped = CorePluginRegistry.getInstance().getPluginClass("set-var");
        assertEquals(mapped, SetVarPlugin.class,
                "Legacy set-var compatibility alias must remain registered");

        PluginMetadata metadata = new SetVarPlugin().getMetadata();
        assertEquals(metadata.getElementName(), "set-var");
        assertEquals(metadata.getId(), "set-var");
    }
}
