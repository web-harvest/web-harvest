/*  Copyright (c) 2006-2025 the original author or authors.
    All rights reserved.

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

    You can contact Vladimir Nikic by sending e-mail to
    nikic_vladimir@yahoo.com. Please include the word "Web-Harvest" in the
    subject line.
*/
package org.webharvest.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.events.ScraperExecutionStoppedEvent;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.definition.CorePluginRegistry;
import org.webharvest.definition.PluginConfigurationTree;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

/**
 * Basic runtime class.
 */
public class Scraper implements WebScraper {

    private static final Logger LOG = LoggerFactory.getLogger(Scraper.class);

    private final EventBus eventBus;
    private final RootConfigurationExecutor rootExecutor;
    
    /**
     * Default constructor for non-Guice usage.
     * Creates a new EventBus instance.
     */
    public Scraper() {
        this(new EventBus(), new RootConfigurationExecutor());
    }
    
    /**
     * Constructor for custom EventBus.
     * 
     * @param eventBus the EventBus to use
     */
    public Scraper(EventBus eventBus) {
        this(eventBus, new RootConfigurationExecutor());
    }

    @Inject
    public Scraper(EventBus eventBus, RootConfigurationExecutor rootExecutor) {
        this.eventBus = eventBus != null ? eventBus : new EventBus();
        this.rootExecutor = rootExecutor != null ? rootExecutor : new RootConfigurationExecutor();
    }

    public void execute(final DynamicScopeContext context) {
        long startTime = System.currentTimeMillis();

        // Note: Session lifecycle events (Started/Completed/Failed) are now published
        // by ScraperSessionImpl, not here. This keeps concerns separated:
        // - Scraper = low-level execution engine
        // - Session = high-level lifecycle management with events

        try {
            // v2.2.x ULTRA-LEAN: Use CorePluginRegistry + PluginConfiguration instead of ProcessorFactory + IElementDef
            final PluginConfigurationTree tree = context.getConfig().getConfigurationTree();
            if (tree != null) {
                final PluginConfiguration rootConfig = tree.getConfiguration();
                if (rootConfig != null) {
                    if ("config".equals(rootConfig.getElementName())) {
                        rootExecutor.execute(tree, context);
                    } else {
                        CorePluginRegistry registry = CorePluginRegistry.getInstance();
                        Class<? extends Plugin> pluginClass = registry.getPluginClass(rootConfig.getElementName());

                        if (pluginClass != null) {
                            Plugin plugin = pluginClass.newInstance();
                            plugin.initialize(rootConfig);
                            plugin.execute(context);
                        } else {
                            LOG.warn("Plugin class not found for root element: {}", rootConfig.getElementName());
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            informListenersAboutError(e);
            Thread.currentThread().interrupt();
        } catch (InstantiationException | IllegalAccessException e) {
            LOG.error("Failed to create root plugin instance", e);
            informListenersAboutError(e);
        } catch (org.webharvest.plugin.exceptions.PluginException e) {
            LOG.error("Failed to initialize root plugin", e);
            informListenersAboutError(e);
        }
    }

    /**
     * Logs information that Scraper's execution has been stopped.
     *
     * @param event an instance of {@link ScraperExecutionStoppedEvent}
     */
    @Subscribe
    public void onExecutionStopped(final ScraperExecutionStoppedEvent event) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Configuration stopped!");
        }
    }

    /**
     * Informs listeners about execution errors.
     * 
     * @param e the exception
     * @deprecated Error events are now published by ScraperSessionImpl.markFailed()
     * as SessionFailedEvent. This method is kept for backward compatibility.
     */
    @Deprecated
    public void informListenersAboutError(Exception e) {
        // Note: SessionFailedEvent is now published by ScraperSessionImpl.markFailed()
        // This method is kept for backward compatibility but no longer publishes events
        LOG.error("Scraper execution error", e);
    }

}
