package org.webharvest.plugin.scanner;

import static org.testng.Assert.*;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.CorePluginRegistry;

/**
 * Contracts for Bug #40 package scanning configuration and empty/noop inputs.
 */
public class PluginPackageScannerBranchContractTest {

    @AfterMethod
    public void clearSystemProperty() {
        System.clearProperty(PluginPackageScanner.PLUGIN_PACKAGES_PROPERTY);
    }

    @Test
    public void scanPackagesWithNoArgumentsIsNoOp() {
        CorePluginRegistry registry = CorePluginRegistry.getInstance();
        PluginPackageScanner scanner = new PluginPackageScanner(registry);
        scanner.scanPackages();
        assertNotNull(scanner);
    }

    @Test
    public void scanSystemPropertyPackagesIgnoresBlankProperty() {
        System.clearProperty(PluginPackageScanner.PLUGIN_PACKAGES_PROPERTY);
        CorePluginRegistry registry = CorePluginRegistry.getInstance();
        PluginPackageScanner scanner = new PluginPackageScanner(registry);
        scanner.scanSystemPropertyPackages();
    }

    @Test
    public void scanSystemPropertyPackagesAcceptsCommaSeparatedList() {
        System.setProperty(PluginPackageScanner.PLUGIN_PACKAGES_PROPERTY,
                "org.webharvest.plugin.core, ,org.webharvest.does.not.exist.plugins");
        CorePluginRegistry registry = CorePluginRegistry.getInstance();
        PluginPackageScanner scanner = new PluginPackageScanner(registry);
        // Must not throw even when one package is missing from the classpath layout.
        scanner.scanSystemPropertyPackages();
    }

    @Test
    public void constructorCopiesConfiguredPackages() {
        PluginPackageScanner scanner = new PluginPackageScanner(
                CorePluginRegistry.getInstance(),
                "org.webharvest.plugin.core");
        scanner.scanPackages("org.webharvest.plugin.core");
        assertEquals(PluginPackageScanner.PLUGIN_PACKAGES_PROPERTY, "webharvest.plugin.packages");
    }
}
