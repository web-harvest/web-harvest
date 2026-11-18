package org.webharvest.runtime;

import static org.testng.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.testng.annotations.Test;
import org.webharvest.definition.Config;
import org.webharvest.ioc.ScraperScope;
import org.webharvest.runtime.session.SessionStatus;
import org.webharvest.runtime.session.events.SessionCompletedEvent;
import org.webharvest.Harvester.ContextInitCallback;

/**
 * Test class for ScrapingHarvester component - responsible for creating and executing harvesters.
 * This test verifies basic functionality of the ScrapingHarvester component.
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
    @Test
    public void testExecutionCompletesSessionOnTheContextEventBus() {
        EventBus eventBus = new EventBus();
        AtomicBoolean completionEventReceived = new AtomicBoolean(false);
        eventBus.register(new Object() {
            @Subscribe
            public void onSessionCompleted(SessionCompletedEvent event) {
                completionEventReceived.set(true);
            }
        });

        Config config = mock(Config.class);
        ScrapingHarvester harvester = new ScrapingHarvester(
                () -> mock(WebScraper.class),
                ignored -> new ScraperContext(),
                () -> eventBus,
                new ScraperScope(),
                config);

        DynamicScopeContext context = harvester.execute(ignored -> { });

        assertNotNull(context.getSession());
        assertEquals(context.getSession().getStatus(), SessionStatus.COMPLETED);
        assertTrue(completionEventReceived.get(),
                "The session completion event must be published on the context EventBus");
    }

}
