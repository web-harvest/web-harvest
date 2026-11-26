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

package org.webharvest.ioc;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.webharvest.ScrapingAware;
import org.webharvest.ioc.ScrapingInterceptor.ScrapingAwareHelper;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Owns automatic Guice-discovered EventBus and ScrapingAware registrations
 * for one ScraperModule injector/runtime.
 *
 * <p>Guice TypeListener does not expose a reliable per-instance scope disposal
 * callback. Automatic registrations therefore have an explicit runtime
 * lifetime, matching the existing strong-reference behavior of EventBus and
 * ScrapingAwareHelper. The runtime service closes this coordinator only after
 * accepted executions and resource-abort work have quiesced.</p>
 *
 * <p>Registration and close share one monitor: a registration is either
 * committed before close and included in retirement, or rejected after close.
 * Identity-based tracking ensures retirement affects only the exact instances
 * registered by this runtime.</p>
 */
@Singleton
public final class RuntimeRegistrationCoordinator implements AutoCloseable {

    private final EventBus eventBus;
    private final ScrapingAwareHelper scrapingAwareHelper;
    private final Map<Object, Boolean> subscribers = new IdentityHashMap<Object, Boolean>();
    private final Map<ScrapingAware, ScrapingAwareHelper.ListenerRegistration> listeners =
        new IdentityHashMap<ScrapingAware, ScrapingAwareHelper.ListenerRegistration>();
    private boolean closeRequested;

    @Inject
    public RuntimeRegistrationCoordinator(final EventBus eventBus,
                                          final ScrapingAwareHelper scrapingAwareHelper) {
        if (eventBus == null || scrapingAwareHelper == null) {
            throw new IllegalArgumentException("Runtime registration dependencies are required");
        }
        this.eventBus = eventBus;
        this.scrapingAwareHelper = scrapingAwareHelper;
    }

    /** Registers one exact subscriber instance for this runtime's lifetime. */
    public synchronized void registerSubscriber(final Object subscriber) {
        requireOpen();
        if (subscriber == null) {
            throw new IllegalArgumentException("Subscriber is required");
        }
        if (subscribers.containsKey(subscriber)) {
            return;
        }
        eventBus.register(subscriber);
        subscribers.put(subscriber, Boolean.TRUE);
    }

    /** Registers one exact ScrapingAware instance and retains its removal token. */
    public synchronized void registerScrapingAware(final ScrapingAware listener) {
        requireOpen();
        if (listener == null) {
            throw new IllegalArgumentException("ScrapingAware listener is required");
        }
        if (listeners.containsKey(listener)) {
            return;
        }
        ScrapingAwareHelper.ListenerRegistration registration =
            scrapingAwareHelper.registerListener(listener);
        listeners.put(listener, registration);
    }

    /**
     * Stops admission and retires all registrations owned by this runtime.
     * Failed EventBus unregistrations remain tracked so a later close call can
     * retry; failures are surfaced rather than silently discarded.
     */
    @Override
    public synchronized void close() {
        closeRequested = true;
        Throwable firstFailure = null;

        for (Object subscriber : new ArrayList<Object>(subscribers.keySet())) {
            try {
                eventBus.unregister(subscriber);
                subscribers.remove(subscriber);
            } catch (RuntimeException failure) {
                firstFailure = appendFailure(firstFailure, failure);
            }
        }

        for (Map.Entry<ScrapingAware, ScrapingAwareHelper.ListenerRegistration> entry
                : new ArrayList<Map.Entry<ScrapingAware, ScrapingAwareHelper.ListenerRegistration>>(
                    listeners.entrySet())) {
            try {
                entry.getValue().close();
                listeners.remove(entry.getKey());
            } catch (Throwable failure) {
                firstFailure = appendFailure(firstFailure, failure);
            }
        }

        if (firstFailure != null) {
            throw new IllegalStateException(
                "One or more runtime registrations could not be retired", firstFailure);
        }
    }

    public synchronized boolean isCloseRequested() {
        return closeRequested;
    }

    public synchronized int getSubscriberRegistrationCount() {
        return subscribers.size();
    }

    public synchronized int getScrapingAwareRegistrationCount() {
        return listeners.size();
    }

    private void requireOpen() {
        if (closeRequested) {
            throw new IllegalStateException("Runtime registration owner is closing or closed");
        }
    }

    private static Throwable appendFailure(final Throwable first, final Throwable next) {
        if (first == null) {
            return next;
        }
        if (first != next) {
            first.addSuppressed(next);
        }
        return first;
    }
}
