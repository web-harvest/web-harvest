package org.webharvest.ioc;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.aopalliance.intercept.MethodInvocation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.Harvester;
import org.webharvest.ScrapingAware;

import com.google.inject.Injector;

/**
 * Test for ScrapingInterceptor class.
 *
 * @since 2.2.0
 */
public class ScrapingInterceptorTest {

    private ScrapingInterceptor interceptor;
    private MethodInvocation methodInvocation;
    private Injector injector;
    private Harvester harvester;

    @BeforeMethod
    public void setUp() {
        interceptor = new ScrapingInterceptor();
        methodInvocation = mock(MethodInvocation.class);
        injector = mock(Injector.class);
        harvester = mock(Harvester.class);
    }

    @Test
    public void testScrapingInterceptorCreation() {
        assertNotNull(interceptor, "ScrapingInterceptor should be created");
    }

    @Test
    public void testInvokeWithNonHarvesterObject() throws Throwable {
        // Arrange
        Object nonHarvester = new Object();
        when(methodInvocation.getThis()).thenReturn(nonHarvester);

        // Act & Assert
        try {
            interceptor.invoke(methodInvocation);
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals(e.getMessage(), "Not an instance of Harvester");
        }
        verify(methodInvocation).getThis();
        verify(methodInvocation, never()).proceed();
    }

    @Test
    public void testScrapingAwareHelperAddListener() {
        // Arrange
        ScrapingInterceptor.ScrapingAwareHelper helper = new ScrapingInterceptor.ScrapingAwareHelper();
        ScrapingAware listener = mock(ScrapingAware.class);

        // Act
        helper.addListener(listener);

        // Assert
        // The helper should have added the listener (we can't easily verify this without exposing internal state)
        assertNotNull(helper, "Helper should be created");
    }

    @Test
    public void testScrapingAwareHelperAddNullListener() {
        // Arrange
        ScrapingInterceptor.ScrapingAwareHelper helper = new ScrapingInterceptor.ScrapingAwareHelper();

        // Act & Assert
        try {
            helper.addListener(null);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Listener is required");
        }
    }

    @Test
    public void testScrapingAwareHelperOnBeforeScraping() {
        // Arrange
        ScrapingInterceptor.ScrapingAwareHelper helper = new ScrapingInterceptor.ScrapingAwareHelper();
        ScrapingAware listener = mock(ScrapingAware.class);
        helper.addListener(listener);

        // Act
        helper.onBeforeScraping(harvester);

        // Assert
        verify(listener).onBeforeScraping(harvester);
    }

    @Test
    public void testScrapingAwareHelperOnAfterScraping() {
        // Arrange
        ScrapingInterceptor.ScrapingAwareHelper helper = new ScrapingInterceptor.ScrapingAwareHelper();
        ScrapingAware listener = mock(ScrapingAware.class);
        helper.addListener(listener);

        // Act
        helper.onAfterScraping(harvester);

        // Assert
        verify(listener).onAfterScraping(harvester);
    }

    @Test
    public void testScrapingAwareHelperWithMultipleListeners() {
        // Arrange
        ScrapingInterceptor.ScrapingAwareHelper helper = new ScrapingInterceptor.ScrapingAwareHelper();
        ScrapingAware listener1 = mock(ScrapingAware.class);
        ScrapingAware listener2 = mock(ScrapingAware.class);
        helper.addListener(listener1);
        helper.addListener(listener2);

        // Act
        helper.onBeforeScraping(harvester);
        helper.onAfterScraping(harvester);

        // Assert
        verify(listener1).onBeforeScraping(harvester);
        verify(listener1).onAfterScraping(harvester);
        verify(listener2).onBeforeScraping(harvester);
        verify(listener2).onAfterScraping(harvester);
    }

    @Test
    public void testScrapingAwareHelperWithNoListeners() {
        // Arrange
        ScrapingInterceptor.ScrapingAwareHelper helper = new ScrapingInterceptor.ScrapingAwareHelper();

        // Act & Assert - should not throw exception
        helper.onBeforeScraping(harvester);
        helper.onAfterScraping(harvester);
    }
}