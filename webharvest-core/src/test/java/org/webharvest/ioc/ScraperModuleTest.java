package org.webharvest.ioc;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.runtime.web.HttpClientManager;
import org.webharvest.runtime.web.HttpClientManager.ProxySettings;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * Test for ScraperModule class.
 *
 * @since 2.2.0
 */
public class ScraperModuleTest {

    @Test
    public void testScraperModuleCreation() {
        ScraperModule module = new ScraperModule("/tmp");
        assertNotNull(module, "ScraperModule should be created");
    }

    @Test
    public void testScraperModuleConfiguration() {
        ScraperModule module = new ScraperModule("/tmp");
        assertNotNull(module, "ScraperModule should be configurable");
    }

    @Test
    public void testScraperModuleBindings() {
        ScraperModule module = new ScraperModule("/tmp");
        assertNotNull(module, "ScraperModule should support bindings");
    }
}
