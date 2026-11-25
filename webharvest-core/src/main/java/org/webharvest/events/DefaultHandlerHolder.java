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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.AlreadyBoundException;
import org.webharvest.Harvester;
import org.webharvest.Registry;
import org.webharvest.ScrapingAware;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Runtime-wide handler catalogue and HarvesterEvent router.
 *
 * <p>Handlers are retained once per runtime. The shared runtime EventBus
 * delivers HarvesterEvents to this dispatcher, which admits each dispatch only
 * while the event's Harvester route is active. Dispatches admitted before route
 * closure may finish after unbinding; the route remains reserved until those
 * dispatch leases drain, so the same Harvester cannot be rebound prematurely.</p>
 *
 * <p>Independent sessions may dispatch concurrently. Handlers must therefore
 * be safe for concurrent calls, as they were when registered on separate
 * session buses.</p>
 */
public final class DefaultHandlerHolder implements HandlerHolder, ScrapingAware {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultHandlerHolder.class);

    private final CopyOnWriteArrayList<HandlerRegistration> handlers =
        new CopyOnWriteArrayList<HandlerRegistration>();
    private final Registry<Harvester, EventBus> registry;
    private final Provider<EventBus> provider;
    private final ConcurrentMap<Harvester, HarvesterRoute> routes =
        new ConcurrentHashMap<Harvester, HarvesterRoute>();

    @Inject
    public DefaultHandlerHolder(final Registry<Harvester, EventBus> registry,
                                 final Provider<EventBus> provider) {
        this.registry = registry;
        this.provider = provider;
    }

    @Override
    public void register(final EventHandler<?> handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler is required");
        }
        handlers.add(new HandlerRegistration(handler, findSubscriberMethods(handler)));
    }

    @Override
    public void onBeforeScraping(final Harvester harvester) {
        if (harvester == null) {
            throw new IllegalArgumentException("Harvester is required");
        }
        final EventBus eventBus = provider.get();
        final HarvesterRoute route = new HarvesterRoute(harvester, eventBus);
        if (routes.putIfAbsent(harvester, route) != null) {
            throw new IllegalStateException("Harvester already has an active or draining event route");
        }
        try {
            registry.bind(harvester, eventBus);
        } catch (AlreadyBoundException e) {
            // The route is not active until registry binding succeeds, so no
            // dispatch lease can exist on this failed startup generation.
            route.closeAdmission();
            route.markUnbound();
            routes.remove(harvester, route);
            throw new IllegalStateException("Harvester already has an active event route", e);
        }
        if (!route.activate()) {
            // Teardown won a concurrent onBefore/onAfter race while registry
            // binding was in progress. onAfterScraping owns unbinding/draining.
            throw new IllegalStateException("Harvester route was closed while it was being bound");
        }
        LOG.debug("Bound harvester [{}] to runtime EventBus [{}]", harvester, eventBus);
    }

    @Override
    public void onAfterScraping(final Harvester harvester) {
        if (harvester == null) {
            throw new IllegalArgumentException("Harvester is required");
        }
        final EventBus eventBus = registry.lookup(harvester);
        if (eventBus == null) {
            throw new IllegalStateException("Could not find active EventBus route for harvester");
        }
        final HarvesterRoute route = routes.get(harvester);
        if (route == null) {
            throw new IllegalStateException("No owned event route exists for harvester");
        }
        if (route.eventBus != eventBus) {
            throw new IllegalStateException("Harvester route does not match its registered EventBus");
        }
        route.closeAdmission();
        registry.unbind(harvester);
        // Keep the closed generation reserved until registry unbind has
        // completed and all previously admitted dispatch leases have drained.
        if (route.markUnbound()) {
            routes.remove(harvester, route);
        }
        LOG.debug("Unbound harvester [{}] from runtime EventBus [{}]", harvester, eventBus);
    }

    /**
     * Receives runtime-wide bus notifications and dispatches only to handlers
     * for an active Harvester. The bus invokes this method concurrently across
     * independent sessions; no global serialization is imposed.
     */
    @Subscribe
    @AllowConcurrentEvents
    public void onHarvesterEvent(final HarvesterEvent event) {
        if (event == null || event.getHarvester() == null) {
            return;
        }
        final HarvesterRoute route = routes.get(event.getHarvester());
        if (route == null) {
            return;
        }
        final EventBus activeBus = registry.lookup(event.getHarvester());
        if (activeBus == null || activeBus != route.eventBus) {
            return;
        }
        final DispatchLease lease = route.acquire();
        if (lease == null) {
            return;
        }

        try {
            Throwable firstFailure = null;
            for (HandlerRegistration registration : handlers) {
                for (Method method : registration.subscriberMethods) {
                    if (!method.getParameterTypes()[0].isAssignableFrom(event.getClass())) {
                        continue;
                    }
                    try {
                        method.invoke(registration.handler, event);
                    } catch (IllegalAccessException failure) {
                        firstFailure = appendFailure(firstFailure, failure);
                    } catch (InvocationTargetException failure) {
                        firstFailure = appendFailure(firstFailure, failure.getCause());
                    }
                }
            }
            if (firstFailure != null) {
                if (firstFailure instanceof RuntimeException) {
                    throw (RuntimeException) firstFailure;
                }
                if (firstFailure instanceof Error) {
                    throw (Error) firstFailure;
                }
                throw new IllegalStateException("Harvester event handler dispatch failed", firstFailure);
            }
        } finally {
            lease.close();
        }
    }

    private List<Method> findSubscriberMethods(final EventHandler<?> handler) {
        final List<Method> methods = new ArrayList<Method>();
        for (Method method : handler.getClass().getMethods()) {
            if (method.isBridge() || method.isSynthetic()
                || !method.isAnnotationPresent(Subscribe.class)) {
                continue;
            }
            if (method.getParameterTypes().length != 1) {
                throw new IllegalArgumentException(
                    "Event handler subscriber methods must declare exactly one parameter: " + method);
            }
            method.setAccessible(true);
            methods.add(method);
        }
        return Collections.unmodifiableList(methods);
    }

    private Throwable appendFailure(final Throwable first, final Throwable next) {
        // Every call site supplies a Throwable from a catch clause; null is not
        // a possible dispatch failure and does not need a separate branch.
        if (next == first) {
            return first;
        }
        if (first == null) {
            return next;
        }
        first.addSuppressed(next);
        return first;
    }

    /**
     * One Harvester-to-runtime-bus generation. Admission closes atomically;
     * already-admitted dispatches retain leases until their callbacks finish.
     */
    private final class HarvesterRoute {
        private final Harvester harvester;
        private final EventBus eventBus;
        private RouteState state = RouteState.STARTING;
        private boolean unbound;
        private int activeDispatches;

        private HarvesterRoute(Harvester harvester, EventBus eventBus) {
            this.harvester = harvester;
            this.eventBus = eventBus;
        }

        private synchronized boolean activate() {
            if (state != RouteState.STARTING) {
                return false;
            }
            state = RouteState.ACTIVE;
            return true;
        }

        private synchronized DispatchLease acquire() {
            if (state != RouteState.ACTIVE) {
                return null;
            }
            activeDispatches++;
            return new DispatchLease(this);
        }

        private synchronized void closeAdmission() {
            state = RouteState.CLOSED;
        }

        private synchronized boolean markUnbound() {
            unbound = true;
            return isReleasable();
        }

        private synchronized boolean release() {
            if (activeDispatches <= 0) {
                throw new IllegalStateException("Harvester dispatch lease released without admission");
            }
            activeDispatches--;
            return isReleasable();
        }

        private boolean isReleasable() {
            return state == RouteState.CLOSED && unbound && activeDispatches == 0;
        }

        private enum RouteState {
            STARTING,
            ACTIVE,
            CLOSED
        }
    }

    private final class DispatchLease implements AutoCloseable {
        private final HarvesterRoute route;
        private final AtomicBoolean released = new AtomicBoolean();

        private DispatchLease(HarvesterRoute route) {
            this.route = route;
        }

        @Override
        public void close() {
            if (released.compareAndSet(false, true) && route.release()) {
                routes.remove(route.harvester, route);
            }
        }
    }

    private static final class HandlerRegistration {
        private final EventHandler<?> handler;
        private final List<Method> subscriberMethods;

        private HandlerRegistration(final EventHandler<?> handler,
                                    final List<Method> subscriberMethods) {
            this.handler = handler;
            this.subscriberMethods = subscriberMethods;
        }
    }
}
