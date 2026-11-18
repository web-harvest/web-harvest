package org.webharvest.plugin.db;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.CorePluginRegistry;
import org.webharvest.definition.CorePluginsRegistrar;
import org.webharvest.plugin.core.HttpPlugin;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;

public class DatabasePluginRegistrarTest {

    private final CorePluginRegistry registry = CorePluginRegistry.getInstance();

    @BeforeMethod
    public void registerDefaultCorePlugins() {
        CorePluginsRegistrar.reset();
        CorePluginsRegistrar.registerCorePlugins();
    }

    @AfterMethod
    public void restoreDefaultCorePlugins() {
        CorePluginsRegistrar.reset();
        CorePluginsRegistrar.registerCorePlugins();
    }

    @Test
    public void registersDatabaseElementsOnlyWhenExplicitlyInvoked() {
        assertFalse(registry.isPluginRegistered("connection"));
        assertFalse(registry.isPluginRegistered("database"));
        assertSame(registry.getPluginClass("http"), HttpPlugin.class);

        DatabasePluginRegistrar.register(registry);

        assertSame(registry.getPluginClass("connection"), ConnectionPlugin.class);
        assertSame(registry.getPluginClass("database"), DatabasePlugin.class);
        assertSame(registry.getPluginClass("http"), HttpPlugin.class);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void rejectsNullRegistry() {
        DatabasePluginRegistrar.register(null);
    }
}
