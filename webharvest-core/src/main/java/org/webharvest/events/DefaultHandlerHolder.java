/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.events;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.AlreadyBoundException;
import org.webharvest.Harvester;
import org.webharvest.Registry;
import org.webharvest.ScrapingAware;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Default implementation of {@link HandlerHolder} storing references to
 * registered {@link EventHandler} listeners. The class implements
 * {@link ScrapingAware} as well which makes it possible to receive updates
 * about entering/leaving scraping scope for particular {@link Harvester}
 * execution.
 * Once the update is receive the action is taken to register scope's
 * {@link EventBus} (obtained form provider object) under the {@link Harvester}
 * as association identifier.
 * Such an association is a foundation for scope's based event sourcing as
 * we are able to send events to particular scope without worrying about
 * possibility that such an event will be addressed to a scope it doesn't apply
 * to.
 * In most cases scraping scope runs in its own thread so there is a need for a
 * kind of synchronization.
 *
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 * @version %I%, %G%
 * @see HandlerHolder
 * @see ScrapingAware
 */
public final class DefaultHandlerHolder implements HandlerHolder,
        ScrapingAware {

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory
            .getLogger(DefaultHandlerHolder.class);

    private final List<EventHandler<?>> handlers =
            new LinkedList<EventHandler<?>>();

    /**
     * Registry storing current scope's based event bus objects. The binding is
     * identified by reference to the {@link Harvester} object that is
     * associated with particular session.
     */
    private final Registry<Harvester, EventBus> registry;

    /**
     * The {@link EventBus} guice's provider. Since the {@link EventBus} is not
     * available upon module initialization (as it is scope based) we use a
     * trick with lazy initialization by keeping reference to the provider. The
     * provider itself is used when scope is already created.
     */
    private final Provider<EventBus> provider;

    /**
     * Default class constructor specifying the {@link Registry} storing
     * associations between Harvester's scope and particular {@link EventBus}
     * that is bound to it. The other parameter is just a {@link Provider}
     * giving the access to reference to {@link EventBus}.
     *
     * @param registry
     *            Scope's registry for {@link EventBus} obejcts.
     * @param provider
     *            the {@link Provider} for scope's {@link EventBus}.
     */
    @Inject
    public DefaultHandlerHolder(final Registry<Harvester, EventBus> registry,
            final Provider<EventBus> provider) {
        this.registry = registry;
        this.provider = provider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(final EventHandler<?> handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler is required");
        }
        handlers.add(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBeforeScraping(final Harvester harvester) {
        if (harvester == null) {
            throw new IllegalArgumentException("Harvester is required");
        }
        final EventBus eventBus = provider.get();
        for (final EventHandler<?> handler : handlers) {
            eventBus.register(handler);
            LOG.debug("Registered event bus handler [{}]", handler);
        }
        try {
            registry.bind(harvester, eventBus);
        } catch (AlreadyBoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAfterScraping(final Harvester harvester) {
        if (harvester == null) {
            throw new IllegalArgumentException("Harvester is required");
        }
        final EventBus eventBus = registry.lookup(harvester);
        if (eventBus == null) {
            throw new IllegalStateException("Cound not find event bus");
        }
        for (final EventHandler<?> handler : handlers) {
            eventBus.unregister(handler);
            LOG.debug("Unregistered event bus handler [{}]", handler);
        }
        registry.unbind(harvester);
    }

}
