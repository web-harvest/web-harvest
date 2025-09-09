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

package org.webharvest;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.unitils.UnitilsTestNG;
import org.unitils.mock.Mock;
import org.webharvest.ioc.DebugFileLogger;
import org.webharvest.ioc.InjectorHelper;
import org.webharvest.runtime.StatusHolder;
import org.webharvest.runtime.scripting.ScriptEngineFactory;
import org.webharvest.runtime.templaters.BaseTemplater;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Monitor;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.webharvest.definition.ConfigurableResolver;
import org.webharvest.definition.ElementsRegistry;
import org.webharvest.definition.ElementsRegistryImpl;
import org.webharvest.definition.ResolverPostProcessor;
import org.webharvest.definition.AnnotatedPluginsPostProcessor;
import org.webharvest.ioc.DefinitionResolverProvider;
import java.util.Arrays;
import java.util.List;

/**
 * Extension of {@link UnitilsTestNG} which before each test method inject
 * appropriate mocks such as {@link Injector} mock and
 * {@link ScriptEngineFactory} mock into static fields in {@link InjectorHelper}
 * and {@link BaseTemplater} classes.
 *
 * Each test class verifying WebHarvest's processor or plugin should extend this
 * class.
 *
 * @author mczapiewski
 * @since 2.1-SNAPSHOT
 * @version %I%, %G%
 */
public abstract class UnitilsTestNGExtension extends UnitilsTestNG {

    protected Mock<ScriptEngineFactory> scriptEngineFactoryMock;
    protected Mock<Logger> loggerMock;
    protected Mock<StatusHolder> holderMock;

    /**
     * Put mocks into static fields of InjectorHelper and BaseTemplater classes.
     * It must be done in this way, because unit tests work out of Guice IoC
     * which injects to these classes appropriate objects.
     */
    @BeforeMethod
    protected void injectStaticFields() throws Exception {

        // Puts Injector's mock into InjectorHelper class
        final Field injectorField = InjectorHelper.class
                .getDeclaredField("injector");
        injectorField.setAccessible(true);
        injectorField.set(null, createTestInjector());

        // Puts ScriptEngineFactory's mock into BaseTemplater class
        final Field templaterField = BaseTemplater.class
                .getDeclaredField("scriptEngineFactory");
        templaterField.setAccessible(true);
        templaterField.set(null, getScriptEngineFactory());

    }

    /**
     * Helper method returning {@link ScriptEngineFactory} instance which by
     * default is a mock of this interface. This method could be overridden.
     *
     * @return {@link ScriptEngineFactory} instance
     */
    protected ScriptEngineFactory getScriptEngineFactory() {
        return scriptEngineFactoryMock.getMock();
    }

    /**
     * Helper method returning instance of {@link Injector} created for
     * {@link TestModule}.
     *
     * @return instance of {@link Injector} created for {@link TestModule}.
     */
    private Injector createTestInjector() {
        return Guice.createInjector(new TestModule());
    }

    /*
     * Guice module designed for processor tests.
     *
     * In the future this class could be split on smaller modules which should
     * be specified by annotation marking test class e.g.
     *
     * @GuiceModules({ ComponentsTestModule.class, ServicesTestModule.class })
     */
    private class TestModule extends AbstractModule {

        @Override
        protected void configure() {
            // AbstractProcessor dependecies
            bind(EventBus.class).in(Singleton.class);
            bind(Logger.class).annotatedWith(DebugFileLogger.class).toInstance(
                    loggerMock.getMock());

            // ScriptProcessor dependecies
            bind(ScriptEngineFactory.class)
                    .toInstance(getScriptEngineFactory());

            // Processor's decorators dependencies
            bind(Monitor.class).in(Singleton.class);
            bind(StatusHolder.class).toInstance(holderMock.getMock());
            
            // ConfigurableResolver dependencies
            bind(ElementsRegistry.class).to(ElementsRegistryImpl.class);
            bind(new TypeLiteral<List<? extends ResolverPostProcessor>>(){})
                    .annotatedWith(Names.named("resolverPostProcessors"))
                    .toInstance(Arrays.<ResolverPostProcessor>asList(
                        new AnnotatedPluginsPostProcessor("org.webharvest.runtime.processors")
                    ));
            bind(ConfigurableResolver.class).toProvider(DefinitionResolverProvider.class).in(Singleton.class);
        }

        @Inject
        @Provides
        public Monitor.Guard getMonitorGuard(final Monitor monitor) {
            return new Monitor.Guard(monitor) {
                @Override
                public boolean isSatisfied() {
                    return true;
                }
            };
        }

    }

}
