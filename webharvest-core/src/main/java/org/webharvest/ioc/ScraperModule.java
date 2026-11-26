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

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.AbstractRegistry;
import org.webharvest.Harvest;
import org.webharvest.Harvester;
import org.webharvest.LockedRegistry;
import org.webharvest.Registry;
import org.webharvest.ScrapingAware;
import org.webharvest.definition.*;
// import org.webharvest.definition.CorePluginsPostProcessor;
import org.webharvest.events.DefaultHandlerHolder;
import org.webharvest.events.EventSink;
import org.webharvest.events.HandlerHolder;
import org.webharvest.events.HarvesterEventSink;
import org.webharvest.ioc.ScrapingInterceptor.ScrapingAwareHelper;
import org.webharvest.runtime.DefaultHarvest;
import org.webharvest.runtime.DefaultHarvesterFactory;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.EventBasedStatusHolder;
import org.webharvest.runtime.RunningStatusGuard;
// import org.webharvest.runtime.RuntimeConfig;
import org.webharvest.runtime.Scraper;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.ScrapingHarvester;
import org.webharvest.runtime.StatusHolder;
import org.webharvest.runtime.WebScraper;
// Database imports moved to DatabaseModule in webharvest-database
import org.webharvest.runtime.scripting.ScriptEngineFactory;
import org.webharvest.runtime.scripting.jsr.JSRScriptEngineFactory;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.webharvest.plugin.services.HttpService;
import org.webharvest.plugin.services.HttpServiceImpl;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Monitor;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;

/**
 * Guice module for WebHarvest configuration.
 * Refactored to use smaller, focused methods for better maintainability.
 * 
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 */
public final class ScraperModule extends AbstractModule {

    private static final ScraperScope SCRAPER_SCOPE = new ScraperScope();

    private final String workingDir;

    /**
     * Creates a ScraperModule with default configuration.
     * 
     * @param workingDir the working directory
     */
    public ScraperModule(final String workingDir) {
        this.workingDir = workingDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        // v2.2.x ULTRA-LEAN: Register core plugins first
        org.webharvest.definition.CorePluginsRegistrar.registerCorePlugins();
        
        // Core bindings
        bindCoreServices();
        bindScopes();
        bindFactories();
        bindInterceptors();
        bindAdvancedFeatures();
        
        // Static injections
        requestStaticInjection(InjectorHelper.class);
        // REMOVED in v2.2.x - DefinitionResolver no longer exists
        // requestStaticInjection(DefinitionResolver.class);
    }
    
    private void bindCoreServices() {
        // REMOVED in v2.2.x - ResolverPostProcessor/CorePluginsPostProcessor no longer exist
        // Resolver post processors
        // LOGGER.debug("ScraperModule: Creating CorePluginsPostProcessor instances");
        // CorePluginsPostProcessor coreProcessor = new CorePluginsPostProcessor("org.webharvest.plugin.core");
        // CorePluginsPostProcessor dbProcessor = new CorePluginsPostProcessor("org.webharvest.plugin.db");
        // CorePluginsPostProcessor ftpProcessor = new CorePluginsPostProcessor("org.webharvest.plugin.ftp");
        // CorePluginsPostProcessor mailProcessor = new CorePluginsPostProcessor("org.webharvest.plugin.mail");
        // CorePluginsPostProcessor webbrowserProcessor = new CorePluginsPostProcessor("org.webharvest.plugin.webbrowser");
        // CorePluginsPostProcessor zipProcessor = new CorePluginsPostProcessor("org.webharvest.plugin.zip");
        // AnnotatedPluginsPostProcessor annotatedProcessor = new AnnotatedPluginsPostProcessor("org.webharvest.runtime.processors");
        //
        // LOGGER.debug("ScraperModule: Created " + 6 + " CorePluginsPostProcessor instances");
        //
        // bind(new TypeLiteral<List<? extends ResolverPostProcessor>>(){})
        //         .annotatedWith(Names.named("resolverPostProcessors"))
        //         .toInstance(Arrays.asList(
        //             coreProcessor,
        //             dbProcessor,
        //             ftpProcessor,
        //             mailProcessor,
        //             webbrowserProcessor,
        //             zipProcessor,
        //             annotatedProcessor
        //         ));

        bind(ScrapingAwareHelper.class).toInstance(new ScrapingAwareHelper());
        bindListener(Matchers.any(), new PostConstructListener());
        bind(ElementsRegistry.class).to(ElementsRegistryImpl.class);
        
        // Registry bindings
        bind(new TypeLiteral<Registry<Harvester, EventBus>>() {})
                .toInstance(new LockedRegistry<Harvester, EventBus>(
                        new AbstractRegistry<Harvester, EventBus>() { }));
        
        bind(AttributeHolder.class).to(ScopeAttributeHolder.class);
        bind(Harvest.class).to(DefaultHarvest.class).in(Singleton.class);
        bind(HandlerHolder.class).to(DefaultHandlerHolder.class).in(Singleton.class);
        
        // Plugin services
        bind(HttpService.class).to(HttpServiceImpl.class).in(Singleton.class);
    }
    
    private void bindScopes() {
        bindScope(ScrapingScope.class, SCRAPER_SCOPE);
        bind(ScraperScope.class).toInstance(SCRAPER_SCOPE);
        
        bindListener(TypeMatchers.subclassesOf(ScrapingAware.class),
                new ScrapingAwareTypeListener());
        bindListener(Matchers.any(), new EventBusTypeListener());
        
        // One shared event bus per injector/runtime, not one bus per scraping session.
        bind(EventBus.class).in(Singleton.class);
        bind(EventSink.class).to(HarvesterEventSink.class).in(Singleton.class);
    }
    
    private void bindFactories() {
        bind(ConfigFactory.class).to(DefaultConfigFactory.class).in(Singleton.class);
        bind(HarvesterFactory.class).to(DefaultHarvesterFactory.class).in(Singleton.class);
    }
    
    private void bindInterceptors() {
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Scraping.class),
                new ScrapingInterceptor());
    }
    
    private void bindAdvancedFeatures() {
        // Database bindings moved to DatabaseModule in webharvest-database
        bindScraperContext();
        bindDebugFileLogger();
        bindStatusHolder();
        bindConfigParser();
        
        bind(WebScraper.class).to(Scraper.class).in(ScrapingScope.class);
        
        // Scripting
        bind(ScriptEngineFactory.class).to(JSRScriptEngineFactory.class).in(Singleton.class);
        requestStaticInjection(BaseTemplater.class);
        // REMOVED in v2.2.x - RuntimeConfig no longer exists
        // bind(RuntimeConfig.class).in(Singleton.class);
        
        // Configuration
        bindConstant().annotatedWith(WorkingDir.class).to(workingDir);
        // REMOVED in v2.2.x - ConfigurableResolver no longer exists
        // bind(ConfigurableResolver.class).toProvider(DefinitionResolverProvider.class).in(Singleton.class);
    }

    // bindDBConnectionFactory() method removed - moved to DatabaseModule in webharvest-database

    protected void bindScraperContext() {
        bind(ContextFactory.class).toInstance(new ContextFactory() {

            @Inject private Provider<ScraperContext> newProvider;

            @Override
            public DynamicScopeContext create(final Config config) {
                final DynamicScopeContext context = newProvider.get();
                // TODO rbala So far this should be enough. Find better option to pass configuration when instantiating context
                context.setConfig(config);

                return context;
            }
        });
    }

    protected void bindDebugFileLogger() {
        bind(Logger.class).annotatedWith(DebugFileLogger.class).toInstance(
                LoggerFactory.getLogger(DebugFileLogger.NAME));
    }

    protected void bindStatusHolder() {
        bind(Monitor.class).in(ScrapingScope.class);
        bind(StatusHolder.class).to(EventBasedStatusHolder.class)
            .in(ScrapingScope.class);
        bind(Monitor.Guard.class).to(RunningStatusGuard.class)
            .in(ScrapingScope.class);
    }

    protected void bindConfigParser() {
        // v2.2.x: Use StreamingXmlParser (ULTRA-LEAN architecture)
        // SAXConfigParser removed - old architecture eliminated
        bind(ConfigParser.class).to(org.webharvest.definition.StreamingXmlParser.class);
    }

    // FIXME rbala Plain old approach to guice's factory binding. Please consider it as temporary workaround. Can stay longer then actual work on 2.1 version.
    // TODO rbala Untested code
    @Provides
    ConfigSourceFactory getConfigSourceFactory() {
        return new ConfigSourceFactory() {

            @Override
            public ConfigSource create(final URL source) {
                return new URLConfigSource(source);
            }

            @Override
            public ConfigSource create(final File source) {
                return new FileConfigSource(source);
            }

            @Override
            public ConfigSource create(final String source) {
                return new BufferConfigSource(source);
            }

        };
    }

}
