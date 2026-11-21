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
import org.webharvest.runtime.variables.Variable;

/**
 * Event published when a plugin execution completes successfully.
 * 
 * <p>This event is part of the event sourcing mechanism introduced in Bug #24.
 * It captures the result and timing of plugin execution for debugging, replay,
 * auditing, and performance analysis.</p>
 * 
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li><b>Performance Analysis:</b> Track plugin execution times and identify bottlenecks</li>
 *   <li><b>Debugging:</b> Inspect results and execution order</li>
 *   <li><b>Replay:</b> Reconstruct complete execution flow with results</li>
 *   <li><b>Metrics:</b> Aggregate timing data for dashboard display</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Subscribe to plugin completion events
 * EventBus eventBus = injector.getInstance(EventBus.class);
 * eventBus.register(new Object() {
 *     @Subscribe
 *     public void onPluginCompleted(PluginExecutionCompletedEvent event) {
 *         System.out.printf("Plugin %s completed in %dms%n", 
 *             event.getPluginName(), 
 *             event.getExecutionTimeMs());
 *             
 *         if (event.getExecutionTimeMs() > 5000) {
 *             logger.warn("Slow plugin detected: {}", event.getPluginName());
 *         }
 *     }
 * });
 * }</pre>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @see PluginExecutionStartedEvent
 * @see SessionEvent
 */
public final class PluginExecutionCompletedEvent extends SessionEvent {
    
    private final String pluginName;
    private final Class<?> pluginClass;
    private final long executionTimeMs;
    private final Variable result;
    
    /**
     * Creates a new plugin execution completed event.
     * 
     * @param session the session in which plugin executed (must not be null)
     * @param pluginName the name of the plugin (e.g., "http", "xpath") (must not be null or empty)
     * @param pluginClass the class of the plugin (must not be null)
     * @param executionTimeMs the execution time in milliseconds (must be >= 0)
     * @param result the result variable, or null if plugin doesn't return a value
     * @throws IllegalArgumentException if session is null, pluginName is null/empty, 
     *         pluginClass is null, or executionTimeMs is negative
     */
    public PluginExecutionCompletedEvent(ScraperSession session, 
                                         String pluginName, 
                                         Class<?> pluginClass,
                                         long executionTimeMs,
                                         Variable result) {
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
        this.result = result;  // null is valid - some plugins don't return values
    }
    
    /**
     * Returns the name of the plugin that completed execution.
     * 
     * @return the plugin name (e.g., "http", "xpath", "loop")
     */
    public String getPluginName() {
        return pluginName;
    }
    
    /**
     * Returns the class of the plugin that completed execution.
     * 
     * @return the plugin class (never null)
     */
    public Class<?> getPluginClass() {
        return pluginClass;
    }
    
    /**
     * Returns the execution time in milliseconds.
     * 
     * <p>This is the wall-clock time from plugin start to completion,
     * useful for performance analysis and bottleneck identification.</p>
     * 
     * @return the execution time in milliseconds (>= 0)
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    /**
     * Returns the result variable produced by the plugin.
     * 
     * <p>Note: Some plugins (like {@code <sleep>} or {@code <var-def>}) don't 
     * return meaningful values, so this may be null.</p>
     * 
     * @return the result variable, or null if plugin doesn't return a value
     */
    public Variable getResult() {
        return result;
    }
    
    @Override
    public String toString() {
        return "PluginExecutionCompletedEvent{" +
                "sessionId='" + getSessionId() + '\'' +
                ", clientId='" + getClientId() + '\'' +
                ", pluginName='" + pluginName + '\'' +
                ", pluginClass=" + pluginClass.getSimpleName() +
                ", executionTimeMs=" + executionTimeMs +
                ", hasResult=" + (result != null) +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}

