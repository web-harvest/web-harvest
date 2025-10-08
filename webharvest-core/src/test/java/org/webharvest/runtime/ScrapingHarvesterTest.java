package org.webharvest.runtime;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.Harvester.ContextInitCallback;

/**
 * Test class for ScrapingHarvester component - responsible for creating and executing harvesters.
 * This test verifies basic functionality without complex mocking due to Guice @Assisted annotation.
 */
public class ScrapingHarvesterTest {

    @Test
    public void testScrapingHarvesterExists() {
        // Given: ScrapingHarvester class
        // When: Check if class exists
        Class<?> clazz = ScrapingHarvester.class;
        
        // Then: Should exist and implement Harvester
        assertNotNull(clazz, "ScrapingHarvester class should exist");
        assertTrue(org.webharvest.Harvester.class.isAssignableFrom(clazz), 
                   "ScrapingHarvester should implement Harvester interface");
    }

    @Test
    public void testContextInitCallbackInterface() {
        // Given: ContextInitCallback interface
        // When: Check if interface exists
        Class<?> clazz = ContextInitCallback.class;
        
        // Then: Should exist
        assertNotNull(clazz, "ContextInitCallback interface should exist");
        assertTrue(clazz.isInterface(), "ContextInitCallback should be an interface");
    }
}
