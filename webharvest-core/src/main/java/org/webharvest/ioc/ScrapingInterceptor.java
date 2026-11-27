/*
 Copyright (c) 2006-2012 the original author or authors.

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.Harvester;
import org.webharvest.ScrapingAware;

import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Guice AOP interceptor responsible for taking action for method annotated with
 * {@link Scraping} annotation. New scraping scope is created for each annotated
 * method and is persistent until the end of invocation. Only ancestors of
 * {@link Harvester} are applicable. An attempt to apply scraping scope to any
 * other type will end up with thrown {@link IllegalStateException}. Just after
 * the scope is created and shortly before it is left all registered
 * {@link ScrapingAware} listeners are notified about the lifecycle change.
 *
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 * @version %I%, %G%
 * @see Scraping
 * @see MethodInterceptor
 * @see ScrapingAware
 */
public final class ScrapingInterceptor implements MethodInterceptor {

    private static final Logger LOG = LoggerFactory
            .getLogger(ScrapingInterceptor.class);

    private final Provider<Injector> injectorProvider;

    /**
     * Legacy constructor retained for callers that instantiate the interceptor
     * outside a Guice module. Runtime modules should pass their owning injector
     * provider explicitly.
     *
     * @deprecated use {@link #ScrapingInterceptor(Provider)}
     */
    @Deprecated
    public ScrapingInterceptor() {
        this(InjectorHelper::getInjector);
    }

    /**
     * Creates an interceptor bound to the Guice injector that owns its binding.
     *
     * @param injectorProvider provider for the owning injector
     */
    public ScrapingInterceptor(final Provider<Injector> injectorProvider) {
        if (injectorProvider == null) {
            throw new IllegalArgumentException("Injector provider is required");
        }
        this.injectorProvider = injectorProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        final Object subject = invocation.getThis();
        if (!(subject instanceof Harvester)) {
            throw new IllegalStateException("Not an instance of Harvester");
        }
        Injector injector = injectorProvider.get();
        if (injector == null) {
            throw new IllegalStateException("Owning Guice injector is not available");
        }
        return invoke(injector, (Harvester) subject, new Callback() {

                    @Override
                    public Object execute() throws Throwable {
                        return invocation.proceed();
                    }
                });
    }

    /**
     * Helper method that creates new scraping scope, executes one of the
     * {@link Harvester} object's method (the one annotated with
     * {@code Scraping} annotation) and leaves it. Just after the scope is
     * created and shortly before it is left all registered
     * {@link ScrapingAware} listeners are notified about the lifecycle change.
     */
    // FIXME rbala Since we use injector directly then we can not test scope management on unit level!
    Object invoke(final Injector injector, final Harvester harvester,
            final Callback callback) throws Throwable {
        final ScraperScope scope = injector.getInstance(ScraperScope.class);
        final ScrapingAwareHelper helper = injector
                .getInstance(ScrapingAwareHelper.class);
        scope.enter(injector.getInstance(AttributeHolder.class));

        Throwable executionFailure = null;
        try {
            helper.onBeforeScraping(harvester);
            return callback.execute();
        } catch (Throwable failure) {
            executionFailure = failure;
            throw failure;
        } finally {
            Throwable cleanupFailure = null;
            try {
                // Run the after notification even when a before listener or the
                // scraper callback fails, so partially initialized listeners
                // get a best-effort opportunity to release their scope state.
                helper.onAfterScraping(harvester);
            } catch (Throwable failure) {
                cleanupFailure = failure;
            }

            try {
                scope.exit();
            } catch (Throwable failure) {
                if (cleanupFailure == null) {
                    cleanupFailure = failure;
                } else if (cleanupFailure != failure) {
                    cleanupFailure.addSuppressed(failure);
                }
            }

            if (cleanupFailure != null) {
                if (executionFailure != null) {
                    if (executionFailure != cleanupFailure) {
                        executionFailure.addSuppressed(cleanupFailure);
                    }
                } else {
                    throw cleanupFailure;
                }
            }
        }
    }

    /**
     * Represents method invocation helper that allows to delay execution of
     * code to the last responsible moment and treat it just like any regular
     * parameter to any method.
     *
     * @author Robert Bala
     * @since 2.1.0-SNAPSHOT
     * @version %I%, %G%
     */
    interface Callback {

        /**
         * Execute some code in delayed fashion.
         *
         * @return result of reflective method invocation.
         * @throws Throwable
         *             Occurred exception thrown upon execution.
         */
        Object execute() throws Throwable;

    }

    /**
     * Guice aware helper class that maintains collection of registered
     * {@link ScrapingAware} listeners. It simplifies notification of all
     * registered listeners about updates the instances of {@link ScrapingAware}
     * are interested in. That is when new scraping scope is created or tore
     * down. It has almost exactly the same methods (
     * {@link #onBeforeScraping(Harvester)} and
     * {@link #onAfterScraping(Harvester)} as those introduced by
     * {@link ScrapingAware}. However by contrast it does not implement this
     * interface as there is a threat it would be automatically self-registered
     * by Guice. The instance of this object is meant to be a singleton
     * (especially for Guice).
     *
     * @author Robert Bala
     * @since 2.1.0-SNAPSHOT
     * @version %I%, %G%
     * @see ScrapingAware
     */
    // TODO Missing unit test
    // TODO It is easy to register listener but how about unregistering it?
    // TODO Resolve duplication of methods with ScrapingAware
    /**
     * Thread-safe helper for snapshot-based scraping lifecycle notification.
     * Concurrent scraping scopes may invoke the same listener concurrently.
     */
    @Singleton
    public static class ScrapingAwareHelper {

        private final CopyOnWriteArrayList<ListenerRegistration> listeners =
                new CopyOnWriteArrayList<ListenerRegistration>();
        private final ThreadLocal<Deque<List<ListenerRegistration>>> scopeSnapshots =
                new ThreadLocal<Deque<List<ListenerRegistration>>>();

        /**
         * Compatibility registration method. The registration remains active
         * for this helper's lifetime unless a caller uses {@link #registerListener(ScrapingAware)}
         * and retains its token.
         */
        public void addListener(final ScrapingAware listener) {
            registerListener(listener);
        }

        /**
         * Registers a listener and returns an idempotent token for future-snapshot
         * removal. Closing the token does not wait for notifications that already
         * captured this registration.
         *
         * @param listener listener to register
         * @return token that removes this exact registration from future snapshots
         */
        public ListenerRegistration registerListener(final ScrapingAware listener) {
            if (listener == null) {
                throw new IllegalArgumentException("Listener is required");
            }
            ListenerRegistration registration = new ListenerRegistration(this, listener);
            listeners.add(registration);
            return registration;
        }

        /**
         * Capture one immutable listener snapshot for this scraping scope. The
         * matching onAfterScraping call on the same thread reuses this snapshot,
         * even if registrations are removed while the scope is active. Listener
         * callbacks may overlap across concurrent scraping scopes,
         * including callbacks to the same listener instance; listeners with
         * mutable state must provide their own thread safety.
         */
        public void onBeforeScraping(final Harvester harvester) {
            LOG.debug("Entering scraper scope [{}]", harvester);
            List<ListenerRegistration> snapshot = new ArrayList<ListenerRegistration>(listeners);
            Deque<List<ListenerRegistration>> activeScopes = scopeSnapshots.get();
            if (activeScopes == null) {
                activeScopes = new ArrayDeque<List<ListenerRegistration>>();
                scopeSnapshots.set(activeScopes);
            }
            activeScopes.push(snapshot);

            Throwable firstFailure = null;
            for (ListenerRegistration registration : snapshot) {
                try {
                    registration.listener.onBeforeScraping(harvester);
                } catch (Throwable failure) {
                    if (firstFailure == null) {
                        firstFailure = failure;
                    } else if (firstFailure != failure) {
                        firstFailure.addSuppressed(failure);
                    }
                }
            }
            if (firstFailure != null) {
                throwListenerFailure(firstFailure);
            }
        }

        /**
         * Notify the exact listener snapshot captured by the matching before
         * notification. Nested scopes are paired in LIFO order; a direct legacy
         * after call without a matching before call uses the current snapshot.
         * Failures are aggregated so one listener cannot skip later teardown.
         */
        public void onAfterScraping(final Harvester harvester) {
            LOG.debug("Leaving scraper scope [{}]", harvester);
            Deque<List<ListenerRegistration>> activeScopes = scopeSnapshots.get();
            List<ListenerRegistration> snapshot;
            if (activeScopes == null || activeScopes.isEmpty()) {
                snapshot = new ArrayList<ListenerRegistration>(listeners);
            } else {
                snapshot = activeScopes.pop();
                if (activeScopes.isEmpty()) {
                    scopeSnapshots.remove();
                }
            }

            Throwable firstFailure = null;
            for (ListenerRegistration registration : snapshot) {
                try {
                    registration.listener.onAfterScraping(harvester);
                } catch (Throwable failure) {
                    if (firstFailure == null) {
                        firstFailure = failure;
                    } else if (firstFailure != failure) {
                        firstFailure.addSuppressed(failure);
                    }
                }
            }
            if (firstFailure != null) {
                throwListenerFailure(firstFailure);
            }
        }

        private static void throwListenerFailure(Throwable failure) {
            if (failure instanceof Error) {
                throw (Error) failure;
            }
            if (failure instanceof RuntimeException) {
                throw (RuntimeException) failure;
            }
            throw new IllegalStateException("ScrapingAware listener failed", failure);
        }

        /**
         * One helper-scoped registration. This token removes only its own entry;
         * it removes the registration from future scopes; an already active scope
         * retains it in its captured snapshot so it receives the matching after callback.
         */
        public static final class ListenerRegistration implements AutoCloseable {
            private final ScrapingAwareHelper helper;
            private final ScrapingAware listener;
            private final AtomicBoolean closed = new AtomicBoolean();

            private ListenerRegistration(ScrapingAwareHelper helper, ScrapingAware listener) {
                this.helper = helper;
                this.listener = listener;
            }

            @Override
            public void close() {
                if (closed.compareAndSet(false, true)) {
                    helper.listeners.remove(this);
                }
            }

            /** Returns whether this exact registration token has been closed. */
            public boolean isClosed() {
                return closed.get();
            }
        }
    }

}
