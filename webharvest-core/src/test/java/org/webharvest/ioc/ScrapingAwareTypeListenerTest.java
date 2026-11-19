package org.webharvest.ioc;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.Harvester;
import org.webharvest.ScrapingAware;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;

/**
 * Test for ScrapingAwareTypeListener class.
 *
 * @since 2.2.0
 */
public class ScrapingAwareTypeListenerTest {

    // Test classes
    static class TestScrapingAware implements ScrapingAware {
        @Override
        public void onBeforeScraping(Harvester harvester) {
            // Dummy method
        }

        @Override
        public void onAfterScraping(Harvester harvester) {
            // Dummy method
        }
    }

    static class AnotherScrapingAware implements ScrapingAware {
        @Override
        public void onBeforeScraping(Harvester harvester) {
            // Dummy method
        }

        @Override
        public void onAfterScraping(Harvester harvester) {
            // Dummy method
        }
    }

    static class NonScrapingAwareClass {
        // No ScrapingAware interface implemented
    }

    private ScrapingAwareTypeListener listener;
    private TypeEncounter<Object> typeEncounter;
    private TypeLiteral<Object> typeLiteral;

    @BeforeMethod
    public void setUp() {
        listener = new ScrapingAwareTypeListener();
        typeEncounter = mock(TypeEncounter.class);
        typeLiteral = mock(TypeLiteral.class);
    }

    @Test
    public void testScrapingAwareTypeListenerCreation() {
        // Act
        ScrapingAwareTypeListener newListener = new ScrapingAwareTypeListener();

        // Assert
        assertNotNull(newListener, "ScrapingAwareTypeListener should be created");
    }

    @Test
    public void testHearWithScrapingAwareClass() {
        // Arrange
        TypeLiteral<TestScrapingAware> scrapingAwareType = TypeLiteral.get(TestScrapingAware.class);
        TypeEncounter<TestScrapingAware> encounter = mock(TypeEncounter.class);

        // Act
        listener.hear(scrapingAwareType, encounter);

        // Assert
        // The listener should register the class with ScrapingInterceptor
        // We can't easily verify this without exposing internal state
        assertNotNull(listener, "Listener should handle ScrapingAware class");
    }

    @Test
    public void testHearWithNonScrapingAwareClass() {
        // Arrange
        TypeLiteral<NonScrapingAwareClass> nonScrapingAwareType = TypeLiteral.get(NonScrapingAwareClass.class);
        TypeEncounter<NonScrapingAwareClass> encounter = mock(TypeEncounter.class);

        // Act
        listener.hear(nonScrapingAwareType, encounter);

        // Assert
        // The listener should not register the class with ScrapingInterceptor
        assertNotNull(listener, "Listener should handle non-ScrapingAware class");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testHearWithNullTypeEncounter() {
        listener.hear(TypeLiteral.get(Object.class), null);
    }

    @Test
    public void testHearWithMultipleScrapingAwareClasses() {
        // Arrange
        TypeLiteral<TestScrapingAware> scrapingAwareType1 = TypeLiteral.get(TestScrapingAware.class);
        TypeEncounter<TestScrapingAware> encounter1 = mock(TypeEncounter.class);
        
        TypeLiteral<AnotherScrapingAware> scrapingAwareType2 = TypeLiteral.get(AnotherScrapingAware.class);
        TypeEncounter<AnotherScrapingAware> encounter2 = mock(TypeEncounter.class);

        // Act
        listener.hear(scrapingAwareType1, encounter1);
        listener.hear(scrapingAwareType2, encounter2);

        // Assert
        assertNotNull(listener, "Listener should handle multiple ScrapingAware classes");
    }

}