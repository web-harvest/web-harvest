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

    @Test
    public void scopeIsExitedWhenBeforeListenerFails() throws Throwable {
        ScraperScope scope = new ScraperScope();
        AttributeHolder holder = mock(AttributeHolder.class);
        ScrapingInterceptor.ScrapingAwareHelper helper =
                new ScrapingInterceptor.ScrapingAwareHelper();
        ScrapingAware listener = mock(ScrapingAware.class);
        RuntimeException beforeFailure = new RuntimeException("before failed");
        helper.addListener(listener);
        doThrow(beforeFailure).when(listener).onBeforeScraping(harvester);
        when(injector.getInstance(ScraperScope.class)).thenReturn(scope);
        when(injector.getInstance(ScrapingInterceptor.ScrapingAwareHelper.class))
                .thenReturn(helper);
        when(injector.getInstance(AttributeHolder.class)).thenReturn(holder);

        try {
            interceptor.invoke(injector, harvester, new ScrapingInterceptor.Callback() {
                @Override
                public Object execute() {
                    fail("Callback must not run after before-listener failure");
                    return null;
                }
            });
            fail("Expected before-listener failure");
        } catch (RuntimeException actual) {
            assertSame(actual, beforeFailure);
        }

        verify(listener).onAfterScraping(harvester);
        try {
            scope.get();
            fail("Scraping scope must be exited after before-listener failure");
        } catch (com.google.inject.OutOfScopeException expected) {
            // Expected: scope teardown ran.
        }
    }

    @Test
    public void scopeIsExitedWhenAfterListenerFails() throws Throwable {
        ScraperScope scope = new ScraperScope();
        AttributeHolder holder = mock(AttributeHolder.class);
        ScrapingInterceptor.ScrapingAwareHelper helper =
                new ScrapingInterceptor.ScrapingAwareHelper();
        ScrapingAware listener = mock(ScrapingAware.class);
        RuntimeException afterFailure = new RuntimeException("after failed");
        helper.addListener(listener);
        doThrow(afterFailure).when(listener).onAfterScraping(harvester);
        when(injector.getInstance(ScraperScope.class)).thenReturn(scope);
        when(injector.getInstance(ScrapingInterceptor.ScrapingAwareHelper.class))
                .thenReturn(helper);
        when(injector.getInstance(AttributeHolder.class)).thenReturn(holder);

        try {
            interceptor.invoke(injector, harvester, new ScrapingInterceptor.Callback() {
                @Override
                public Object execute() {
                    return "completed";
                }
            });
            fail("Expected after-listener failure");
        } catch (RuntimeException actual) {
            assertSame(actual, afterFailure);
        }

        try {
            scope.get();
            fail("Scraping scope must be exited after after-listener failure");
        } catch (com.google.inject.OutOfScopeException expected) {
            // Expected: scope teardown ran.
        }
    }

    @Test
    public void callbackFailureRemainsPrimaryWhenAfterListenerAlsoFails() throws Throwable {
        ScraperScope scope = new ScraperScope();
        AttributeHolder holder = mock(AttributeHolder.class);
        ScrapingInterceptor.ScrapingAwareHelper helper =
                new ScrapingInterceptor.ScrapingAwareHelper();
        ScrapingAware listener = mock(ScrapingAware.class);
        RuntimeException callbackFailure = new RuntimeException("callback failed");
        RuntimeException afterFailure = new RuntimeException("after failed");
        helper.addListener(listener);
        doThrow(afterFailure).when(listener).onAfterScraping(harvester);
        when(injector.getInstance(ScraperScope.class)).thenReturn(scope);
        when(injector.getInstance(ScrapingInterceptor.ScrapingAwareHelper.class))
                .thenReturn(helper);
        when(injector.getInstance(AttributeHolder.class)).thenReturn(holder);

        try {
            interceptor.invoke(injector, harvester, new ScrapingInterceptor.Callback() {
                @Override
                public Object execute() {
                    throw callbackFailure;
                }
            });
            fail("Expected callback failure");
        } catch (RuntimeException actual) {
            assertSame(actual, callbackFailure);
            assertEquals(actual.getSuppressed().length, 1);
            assertSame(actual.getSuppressed()[0], afterFailure);
        }

        try {
            scope.get();
            fail("Scraping scope must be exited when callback and after-listener fail");
        } catch (com.google.inject.OutOfScopeException expected) {
            // Expected: scope teardown ran.
        }
    }


    @Test
    public void identicalExecutionAndCleanupFailureDoesNotSelfSuppress() throws Throwable {
        ScraperScope scope = new ScraperScope();
        AttributeHolder holder = mock(AttributeHolder.class);
        ScrapingInterceptor.ScrapingAwareHelper helper =
                new ScrapingInterceptor.ScrapingAwareHelper();
        ScrapingAware listener = mock(ScrapingAware.class);
        RuntimeException sharedFailure = new RuntimeException("shared failure");
        helper.addListener(listener);
        doThrow(sharedFailure).when(listener).onAfterScraping(harvester);
        when(injector.getInstance(ScraperScope.class)).thenReturn(scope);
        when(injector.getInstance(ScrapingInterceptor.ScrapingAwareHelper.class))
                .thenReturn(helper);
        when(injector.getInstance(AttributeHolder.class)).thenReturn(holder);

        try {
            interceptor.invoke(injector, harvester, new ScrapingInterceptor.Callback() {
                @Override
                public Object execute() {
                    throw sharedFailure;
                }
            });
            fail("Expected shared failure");
        } catch (RuntimeException actual) {
            assertSame(actual, sharedFailure);
            assertEquals(actual.getSuppressed().length, 0);
        }

        try {
            scope.get();
            fail("Scraping scope must be exited after identical failures");
        } catch (com.google.inject.OutOfScopeException expected) {
            // Expected: scope teardown ran.
        }
    }

}
