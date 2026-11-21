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
 * Event published when a plugin execution starts.
 * 
 * <p>This event is part of the event sourcing mechanism introduced in Bug #24.
 * It provides a stream of all plugin executions for debugging, replay, auditing,
 * and real-time monitoring.</p>
 * 
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li><b>Debugging:</b> Track execution order and timing of plugins</li>
 *   <li><b>Replay:</b> Reconstruct execution flow from event stream</li>
 *   <li><b>Auditing:</b> Log all plugin operations for compliance</li>
 *   <li><b>Monitoring:</b> Real-time dashboard of scraper execution</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Subscribe to plugin execution events
 * EventBus eventBus = injector.getInstance(EventBus.class);
 * eventBus.register(new Object() {
 *     @Subscribe
 *     public void onPluginStarted(PluginExecutionStartedEvent event) {
 *         System.out.println("Plugin " + event.getPluginName() + 
 *                          " started in session " + event.getSessionId());
 *     }
 * });
 * }</pre>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @see PluginExecutionCompletedEvent
 * @see SessionEvent
 */
public final class PluginExecutionStartedEvent extends SessionEvent {
    
    private final String pluginName;
    private final Class<?> pluginClass;
    
    /**
     * Creates a new plugin execution started event.
     * 
     * @param session the session in which plugin is executing (must not be null)
     * @param pluginName the name of the plugin (e.g., "http", "xpath") (must not be null or empty)
     * @param pluginClass the class of the plugin (must not be null)
     * @throws IllegalArgumentException if session is null, pluginName is null/empty, or pluginClass is null
     */
    public PluginExecutionStartedEvent(ScraperSession session, String pluginName, Class<?> pluginClass) {
        super(session);
        
        if (pluginName == null || pluginName.trim().isEmpty()) {
            throw new IllegalArgumentException("Plugin name cannot be null or empty");
        }
        if (pluginClass == null) {
            throw new IllegalArgumentException("Plugin class cannot be null");
        }
        
        this.pluginName = pluginName;
        this.pluginClass = pluginClass;
    }
    
    /**
     * Returns the name of the plugin that started execution.
     * 
     * @return the plugin name (e.g., "http", "xpath", "loop")
     */
    public String getPluginName() {
        return pluginName;
    }
    
    /**
     * Returns the class of the plugin that started execution.
     * 
     * @return the plugin class (never null)
     */
    public Class<?> getPluginClass() {
        return pluginClass;
    }
    
    @Override
    public String toString() {
        return "PluginExecutionStartedEvent{" +
                "sessionId='" + getSessionId() + '\'' +
                ", clientId='" + getClientId() + '\'' +
                ", pluginName='" + pluginName + '\'' +
                ", pluginClass=" + pluginClass.getSimpleName() +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}

