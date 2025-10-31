package org.webharvest.runtime;

import com.google.common.eventbus.EventBus;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;
import org.webharvest.Harvester;
import org.webharvest.definition.Config;
import org.webharvest.ioc.ContextFactory;
import org.webharvest.ioc.ScraperScope;
import org.webharvest.runtime.session.ScraperSession;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class DefaultHarvesterFactoryTest {

    @Test
    public void shouldProduceHarvesterThatInitialisesContextAndScraper() {
        WebScraper scraper = mock(WebScraper.class);

        ContextFactory contextFactory = mock(ContextFactory.class);
        DynamicScopeContext context = mock(DynamicScopeContext.class);
        Config config = mock(Config.class);
        when(contextFactory.create(config)).thenReturn(context);

        @SuppressWarnings("unchecked")
        Provider<EventBus> eventBusProvider = mock(Provider.class);
        EventBus eventBus = mock(EventBus.class);
        when(eventBusProvider.get()).thenReturn(eventBus);

        ScraperScope scraperScope = new ScraperScope();

        Provider<WebScraper> scraperProvider = () -> {
            scraperScope.get();
            return scraper;
        };

        DefaultHarvesterFactory factory = new DefaultHarvesterFactory(scraperProvider, contextFactory, eventBusProvider, scraperScope);
        Harvester harvester = factory.create(config);

        assertTrue(harvester instanceof ScrapingHarvester, "Factory should create ScrapingHarvester instances");
        verify(eventBusProvider, never()).get();

        Harvester.ContextInitCallback callback = mock(Harvester.ContextInitCallback.class);

        harvester.execute(callback);

        verify(contextFactory).create(config);
        verify(callback).onSuccess(context);
        verify(scraper).execute(context);
        verify(eventBusProvider).get();
        verify(context).setEventBus(eventBus);

        ArgumentCaptor<ScraperSession> sessionCaptor = ArgumentCaptor.forClass(ScraperSession.class);
        verify(context).setSession(sessionCaptor.capture());
        ScraperSession session = sessionCaptor.getValue();
        assertNotNull(session, "Session should be initialised");
        assertSame(config, session.getConfig(), "Session should reference provided config");

        try {
            scraperScope.get();
            fail("Scope should be exited after execution");
        } catch (OutOfScopeException expected) {
            // expected path
        }
    }
}
