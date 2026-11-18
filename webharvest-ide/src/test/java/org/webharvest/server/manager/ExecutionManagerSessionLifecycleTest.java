package org.webharvest.server.manager;

import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.eventbus.Subscribe;
import org.testng.annotations.Test;
import org.webharvest.definition.Config;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.session.ScraperSessionImpl;
import org.webharvest.runtime.session.SessionStatus;
import org.webharvest.runtime.session.events.SessionCompletedEvent;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ExecutionManagerSessionLifecycleTest {

    @Test
    public void sessionTerminalEventsArePublishedOnTheContextEventBus() {
        ScraperContext context = new ScraperContext();
        ScraperSessionImpl session = ExecutionManager.createSessionForContext(
                "event-bus-session", "test-client", mock(Config.class), context);
        AtomicBoolean completionReceived = new AtomicBoolean(false);
        context.getEventBus().register(new Object() {
            @Subscribe
            public void onSessionCompleted(SessionCompletedEvent event) {
                completionReceived.set(true);
            }
        });

        session.markRunning(context);
        session.markCompleted();

        assertNotNull(context.getEventBus());
        assertEquals(session.getStatus(), SessionStatus.COMPLETED);
        assertTrue(completionReceived.get(),
                "Session terminal events must reach subscribers registered on the context EventBus");
    }
}
