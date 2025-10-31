package org.webharvest.runtime;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.webharvest.Harvester;
import org.webharvest.definition.Config;
import org.webharvest.ioc.ContextFactory;
import org.webharvest.ioc.HarvesterFactory;
import org.webharvest.ioc.ScraperScope;

/**
 * Replaces the Guice assisted-inject based {@link HarvesterFactory}
 * implementation with a simple, allocation-only factory. This avoids the
 * reflective access warnings triggered by the legacy assisted inject module on
 * newer JVMs.
 */
@Singleton
public final class DefaultHarvesterFactory implements HarvesterFactory {

    private final Provider<WebScraper> scraperProvider;
    private final ContextFactory contextFactory;
    private final Provider<EventBus> eventBusProvider;
    private final ScraperScope scraperScope;

    @Inject
    public DefaultHarvesterFactory(final Provider<WebScraper> scraperProvider,
                                   final ContextFactory contextFactory,
                                   final Provider<EventBus> eventBusProvider,
                                   final ScraperScope scraperScope) {
        this.scraperProvider = scraperProvider;
        this.contextFactory = contextFactory;
        this.eventBusProvider = eventBusProvider;
        this.scraperScope = scraperScope;
    }

    @Override
    public Harvester create(final Config config) {
        return new ScrapingHarvester(scraperProvider, contextFactory, eventBusProvider, scraperScope, config);
    }
}
