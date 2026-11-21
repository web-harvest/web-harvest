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

package org.webharvest.runtime.session.events;

import org.webharvest.runtime.session.ScraperSession;

/**
 * Event published when a plugin execution fails due to an error.
 * 
 * <p>This event is part of the event sourcing mechanism introduced in Bug #24.
 * It captures failures during plugin execution for debugging, error tracking,
 * and alerting purposes.</p>
 * 
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li><b>Error Tracking:</b> Monitor and log plugin failures for debugging</li>
 *   <li><b>Alerting:</b> Trigger notifications when critical plugins fail</li>
 *   <li><b>Debugging:</b> Analyze failure patterns and timing</li>
 *   <li><b>Replay:</b> Reconstruct execution including failures</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Subscribe to plugin failure events
 * EventBus eventBus = injector.getInstance(EventBus.class);
 * eventBus.register(new Object() {
 *     @Subscribe
 *     public void onPluginFailed(PluginExecutionFailedEvent event) {
 *         logger.error("Plugin {} failed after {}ms: {}", 
 *             event.getPluginName(), 
 *             event.getExecutionTimeMs(),
 *             event.getError().getMessage());
 *             
 *         // Send alert for critical plugins
 *         if ("http".equals(event.getPluginName())) {
 *             alertingService.sendAlert("HTTP plugin failed!");
 *         }
 *     }
 * });
 * }</pre>
 * 
 * <h3>Note on Error Propagation:</h3>
 * <p>This event is published when a plugin throws an exception. The exception
 * is still propagated up the call stack after the event is published. This event
 * provides a way to observe failures without changing error handling logic.</p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @see PluginExecutionStartedEvent
 * @see PluginExecutionCompletedEvent
 * @see SessionEvent
 */
public final class PluginExecutionFailedEvent extends SessionEvent {
    
    private final String pluginName;
    private final Class<?> pluginClass;
    private final long executionTimeMs;
    private final Throwable error;
    
    /**
     * Creates a new plugin execution failed event.
     * 
     * @param session the session in which plugin failed (must not be null)
     * @param pluginName the name of the plugin (e.g., "http", "xpath") (must not be null or empty)
     * @param pluginClass the class of the plugin (must not be null)
     * @param executionTimeMs the execution time before failure in milliseconds (must be >= 0)
     * @param error the error/exception that caused failure, or null for generic failure
     * @throws IllegalArgumentException if session is null, pluginName is null/empty, 
     *         pluginClass is null, or executionTimeMs is negative
     */
    public PluginExecutionFailedEvent(ScraperSession session, 
                                      String pluginName, 
                                      Class<?> pluginClass,
                                      long executionTimeMs,
                                      Throwable error) {
        super(session);
        
        if (pluginName == null || pluginName.trim().isEmpty()) {
            throw new IllegalArgumentException("Plugin name cannot be null or empty");
        }
        if (pluginClass == null) {
            throw new IllegalArgumentException("Plugin class cannot be null");
        }
        if (executionTimeMs < 0) {
            throw new IllegalArgumentException("Execution time cannot be negative");
        }
        
        this.pluginName = pluginName;
        this.pluginClass = pluginClass;
        this.executionTimeMs = executionTimeMs;
        this.error = error;  // null is valid - generic failure
    }
    
    /**
     * Returns the name of the plugin that failed.
     * 
     * @return the plugin name (e.g., "http", "xpath", "loop")
     */
    public String getPluginName() {
        return pluginName;
    }
    
    /**
     * Returns the class of the plugin that failed.
     * 
     * @return the plugin class (never null)
     */
    public Class<?> getPluginClass() {
        return pluginClass;
    }
    
    /**
     * Returns the execution time before failure in milliseconds.
     * 
     * <p>This is the time from plugin start until the exception was thrown.
     * Useful for identifying if failure occurred immediately or after some processing.</p>
     * 
     * @return the execution time in milliseconds (>= 0)
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    /**
     * Returns the error/exception that caused the failure.
     * 
     * @return the error, or null for generic failure
     */
    public Throwable getError() {
        return error;
    }
    
    @Override
    public String toString() {
        return "PluginExecutionFailedEvent{" +
                "sessionId='" + getSessionId() + '\'' +
                ", clientId='" + getClientId() + '\'' +
                ", pluginName='" + pluginName + '\'' +
                ", pluginClass=" + pluginClass.getSimpleName() +
                ", executionTimeMs=" + executionTimeMs +
                ", error=" + (error != null ? error.getMessage() : "unknown") +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}

