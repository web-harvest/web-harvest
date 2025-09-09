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
import org.webharvest.events.DefaultHandlerHolder;
import org.webharvest.events.EventSink;
import org.webharvest.events.HandlerHolder;
import org.webharvest.events.HarvesterEventSink;
import org.webharvest.ioc.ScrapingInterceptor.ScrapingAwareHelper;
import org.webharvest.runtime.DefaultHarvest;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.EventBasedStatusHolder;
import org.webharvest.runtime.RunningStatusGuard;
import org.webharvest.runtime.RuntimeConfig;
import org.webharvest.runtime.Scraper;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.ScrapingHarvester;
import org.webharvest.runtime.StatusHolder;
import org.webharvest.runtime.WebScraper;
import org.webharvest.runtime.database.ConnectionFactory;
import org.webharvest.runtime.database.JNDIConnectionFactory;
import org.webharvest.runtime.database.StandaloneConnectionPool;
import org.webharvest.runtime.processors.Processor;
import org.webharvest.runtime.scripting.ScriptEngineFactory;
import org.webharvest.runtime.scripting.jsr.JSRScriptEngineFactory;
import org.webharvest.runtime.templaters.BaseTemplater;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Monitor;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;

/**
 * Guice module for Web-Harvest configuration.
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
        // Core bindings
        bindCoreServices();
        bindScopes();
        bindFactories();
        bindInterceptors();
        bindAdvancedFeatures();
        
        // Static injections
        requestStaticInjection(InjectorHelper.class);
        requestStaticInjection(DefinitionResolver.class);
    }
    
    private void bindCoreServices() {
        // Resolver post processors
        bind(new TypeLiteral<List<? extends ResolverPostProcessor>>(){})
                .annotatedWith(Names.named("resolverPostProcessors"))
                .toInstance(Arrays.asList(
                    new AnnotatedPluginsPostProcessor("org.webharvest.runtime.processors")
                ));

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
    }
    
    private void bindScopes() {
        bindScope(ScrapingScope.class, SCRAPER_SCOPE);
        bind(ScraperScope.class).toInstance(SCRAPER_SCOPE);
        
        bindListener(TypeMatchers.subclassesOf(ScrapingAware.class),
                new ScrapingAwareTypeListener());
        bindListener(Matchers.any(), new EventBusTypeListener());
        
        bind(EventBus.class).in(ScrapingScope.class);
        bind(EventSink.class).to(HarvesterEventSink.class).in(Singleton.class);
    }
    
    private void bindFactories() {
        install(new FactoryModuleBuilder()
                .implement(Config.class, XMLConfig.class)
                .build(ConfigFactory.class));
        
        install(new FactoryModuleBuilder()
                .implement(Harvester.class, ScrapingHarvester.class)
                .build(HarvesterFactory.class));
    }
    
    private void bindInterceptors() {
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Scraping.class),
                new ScrapingInterceptor());
    }
    
    private void bindAdvancedFeatures() {
        bindDBConnectionFactory();
        bindScraperContext();
        bindDebugFileLogger();
        bindStatusHolder();
        bindConfigParser();
        
        bind(ConnectionFactory.class).to(StandaloneConnectionPool.class).in(ScrapingScope.class);
        bind(WebScraper.class).to(Scraper.class).in(ScrapingScope.class);
        
        // Scripting
        bind(ScriptEngineFactory.class).to(JSRScriptEngineFactory.class).in(Singleton.class);
        requestStaticInjection(BaseTemplater.class);
        bind(RuntimeConfig.class).in(Singleton.class);
        
        // Configuration
        bindConstant().annotatedWith(WorkingDir.class).to(workingDir);
        bind(ConfigurableResolver.class).toProvider(DefinitionResolverProvider.class).in(Singleton.class);
    }

    protected void bindDBConnectionFactory() {
        bind(ConnectionFactory.class).annotatedWith(Names.named("standalone"))
           .to(StandaloneConnectionPool.class).in(ScrapingScope.class);
        bind(ConnectionFactory.class).annotatedWith(Names.named("jndi"))
           .to(JNDIConnectionFactory.class).in(Singleton.class);
    }

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
        bind(ConfigParser.class).to(SAXConfigParser.class);
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
