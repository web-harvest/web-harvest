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

package org.webharvest.runtime.service;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.definition.Config;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.Scraper;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.client.ClientContext;
import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.runtime.session.ScraperSessionImpl;
import org.webharvest.runtime.session.SessionRegistry;
import org.webharvest.runtime.session.SessionRegistryImpl;
import org.webharvest.runtime.tracking.TokenTracker;
import org.webharvest.runtime.tracking.TokenTrackerImpl;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of {@link WebHarvestService}.
 * 
 * <p>This implementation uses:</p>
 * <ul>
 *   <li>{@link ExecutorService} for concurrent session execution</li>
 *   <li>{@link SessionRegistry} for session tracking</li>
 *   <li>{@link EventBus} for event publishing</li>
 * </ul>
 * 
 * <h3>Thread Pool Configuration:</h3>
 * <p>By default, uses a cached thread pool that creates threads on demand
 * and reuses idle threads. Can be customized via constructor.</p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version %I%, %G%
 */
public class WebHarvestServiceImpl implements WebHarvestService {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebHarvestServiceImpl.class);
    
    private final ExecutorService executorService;
    private final SessionRegistry sessionRegistry;
    private final EventBus eventBus;
    private volatile boolean shutdown = false;
    
    /**
     * Creates a service with default configuration.
     * Uses cached thread pool with unlimited threads.
     */
    public WebHarvestServiceImpl() {
        this(Executors.newCachedThreadPool(new SessionThreadFactory()));
    }
    
    /**
     * Creates a service with custom thread pool.
     * 
     * @param executorService the executor service for running sessions
     * @throws IllegalArgumentException if executorService is null
     */
    public WebHarvestServiceImpl(ExecutorService executorService) {
        this(executorService, new SessionRegistryImpl(), new EventBus());
    }
    
    /**
     * Creates a service with all custom components (for testing).
     * 
     * @param executorService the executor service
     * @param sessionRegistry the session registry
     * @param eventBus the event bus
     * @throws IllegalArgumentException if any parameter is null
     */
    public WebHarvestServiceImpl(ExecutorService executorService,
                                  SessionRegistry sessionRegistry,
                                  EventBus eventBus) {
        if (executorService == null) {
            throw new IllegalArgumentException("ExecutorService cannot be null");
        }
        if (sessionRegistry == null) {
            throw new IllegalArgumentException("SessionRegistry cannot be null");
        }
        if (eventBus == null) {
            throw new IllegalArgumentException("EventBus cannot be null");
        }
        
        this.executorService = executorService;
        this.sessionRegistry = sessionRegistry;
        this.eventBus = eventBus;
    }
    
    @Override
    public ScraperSession executeAsync(Config config, ClientContext client, SessionOptions options) {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        if (shutdown) {
            throw new IllegalStateException("Service is shut down");
        }
        
        // Use defaults if not provided
        SessionOptions opts = options != null ? options : SessionOptions.DEFAULT;
        String clientId = client != null ? client.getClientId() : null;
        
        // Create session with unique ID
        String sessionId = UUID.randomUUID().toString();
        ScraperSessionImpl session = new ScraperSessionImpl(sessionId, clientId, config, eventBus);
        
        // Create token tracker if enabled
        TokenTracker tokenTracker = opts.isTrackTokens() ? 
            new TokenTrackerImpl(sessionId, clientId) : null;
        
        // Register session
        sessionRegistry.register(session);
        
        LOG.info("Submitting session {} for client {}", sessionId, clientId);
        
        // Submit for execution
        executorService.submit(() -> executeSession(session, tokenTracker, opts));
        
        return session;
    }
    
    /**
     * Executes a session in the executor thread.
     */
    private void executeSession(ScraperSessionImpl session, 
                                 TokenTracker tokenTracker, 
                                 SessionOptions options) {
        try {
            // Create execution context
            DynamicScopeContext context = new ScraperContext();
            context.setConfig(session.getConfig());
            
            // Mark as running
            session.markRunning(context);
            
            // Create scraper with shared EventBus
            Scraper scraper = new Scraper(eventBus);
            
            // Execute with timeout if configured
            if (options.hasTimeout()) {
                executeWithTimeout(scraper, context, session, options.getTimeout());
            } else {
                scraper.execute(context);
            }
            
            // Mark as completed
            session.markCompleted();
            
            LOG.info("Session {} completed successfully in {}", 
                session.getSessionId(), 
                session.getMetrics().getDuration());
            
        } catch (InterruptedException e) {
            LOG.warn("Session {} interrupted", session.getSessionId());
            session.markFailed(e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("Session {} failed", session.getSessionId(), e);
            session.markFailed(e);
        }
    }
    
    /**
     * Executes scraper with timeout using Future.
     */
    private void executeWithTimeout(Scraper scraper, 
                                      DynamicScopeContext context,
                                      ScraperSession session,
                                      Duration timeout) throws Exception {
        Future<?> future = Executors.newSingleThreadExecutor().submit(() -> {
            try {
                scraper.execute(context);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        try {
            future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Session execution exceeded timeout: " + timeout);
        }
    }
    
    @Override
    public Optional<ScraperSession> getSession(String sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        return sessionRegistry.getSession(sessionId);
    }
    
    @Override
    public List<ScraperSession> listSessions(String clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        return sessionRegistry.getSessionsByClientId(clientId);
    }
    
    @Override
    public List<ScraperSession> listAllSessions() {
        return sessionRegistry.getAllSessions();
    }
    
    @Override
    public boolean cancelSession(String sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        
        Optional<ScraperSession> session = sessionRegistry.getSession(sessionId);
        return session.map(ScraperSession::cancel).orElse(false);
    }
    
    @Override
    public void shutdown() throws InterruptedException {
        LOG.info("Shutting down WebHarvestService...");
        shutdown = true;
        executorService.shutdown();
        executorService.awaitTermination(60, TimeUnit.SECONDS);
        LOG.info("WebHarvestService shut down complete");
    }
    
    @Override
    public void shutdownNow() {
        LOG.warn("Forcing immediate shutdown of WebHarvestService");
        shutdown = true;
        
        // Cancel all running sessions
        for (ScraperSession session : sessionRegistry.getAllSessions()) {
            session.cancel();
        }
        
        executorService.shutdownNow();
    }
    
    @Override
    public boolean isShutdown() {
        return shutdown;
    }
    
    /**
     * Thread factory that names session execution threads for easier debugging.
     */
    private static class SessionThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("webharvest-session-" + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        }
    }
}

