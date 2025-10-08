package org.webharvest.runtime;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

/**
 * Test class for Scraper component - responsible for executing scraping sessions.
 * This test verifies basic Scraper functionality without complex mocking.
 */
public class ScraperTest {

    @Test
    public void testScraperExists() {
        // Given: Scraper class
        // When: Check if class exists
        Class<?> clazz = Scraper.class;
        
        // Then: Should exist and implement WebScraper
        assertNotNull(clazz, "Scraper class should exist");
        assertTrue(org.webharvest.runtime.WebScraper.class.isAssignableFrom(clazz), 
                   "Scraper should implement WebScraper interface");
    }

    @Test
    public void testScraperConstructor() {
        // Given: Scraper class
        // When: Create new instance
        Scraper scraper = new Scraper();
        
        // Then: Should be created successfully
        assertNotNull(scraper, "Scraper should be created successfully");
    }

    @Test
    public void testScraperConstructorWithEventBus() {
        // Given: Scraper class and EventBus
        com.google.common.eventbus.EventBus eventBus = new com.google.common.eventbus.EventBus();
        
        // When: Create new instance with EventBus
        Scraper scraper = new Scraper(eventBus);
        
        // Then: Should be created successfully
        assertNotNull(scraper, "Scraper with EventBus should be created successfully");
    }
}
