package org.webharvest.runtime.templaters;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.scripting.ScriptEngine;
import org.webharvest.runtime.scripting.ScriptEngineFactory;
import org.webharvest.runtime.scripting.ScriptSource;
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.runtime.scripting.jsr.JSRScriptEngineFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

/**
 * Test for BaseTemplater static injection.
 * 
 * PROBLEM: BaseTemplater.scriptEngineFactory is null in integration tests!
 * This test verifies that Guice static injection works correctly.
 */
public class BaseTemplaterStaticInjectionTest {

    @BeforeMethod
    public void setUp() {
        // Reset static field before each test
        BaseTemplater.scriptEngineFactory = null;
    }

    @Test
    public void testStaticInjectionWorks() {
        // GIVEN: Guice injector with ScraperModule
        // Use target/ as working directory - all generated files will go to target/
        String targetDir = "target";
        new java.io.File(targetDir).mkdirs();
        Injector injector = Guice.createInjector(new ScraperModule(targetDir));
        
        // WHEN: requestStaticInjection is called in ScraperModule.configure()
        // (it's already called in ScraperModule line 209)
        
        // THEN: BaseTemplater.scriptEngineFactory should NOT be null
        assertNotNull(BaseTemplater.scriptEngineFactory, 
            "BaseTemplater.scriptEngineFactory should be injected by Guice!");
        
        // AND: It should be instance of JSRScriptEngineFactory
        assertTrue(BaseTemplater.scriptEngineFactory instanceof JSRScriptEngineFactory,
            "scriptEngineFactory should be JSRScriptEngineFactory");
    }
    
    @Test
    public void runtimeContextsUseTheirOwnScriptEngineFactory() {
        ScriptEngineFactory firstFactory = markerFactory("runtime-A");
        ScriptEngineFactory secondFactory = markerFactory("runtime-B");

        Injector firstInjector = Guice.createInjector(
            Modules.override(new ScraperModule("target")).with(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(ScriptEngineFactory.class).toInstance(firstFactory);
                }
            }));
        Injector secondInjector = Guice.createInjector(
            Modules.override(new ScraperModule("target")).with(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(ScriptEngineFactory.class).toInstance(secondFactory);
                }
            }));

        ScraperContext firstContext = firstInjector.getInstance(ScraperContext.class);
        ScraperContext secondContext = secondInjector.getInstance(ScraperContext.class);

        assertSame(firstContext.getScriptEngineFactory(), firstFactory);
        assertSame(secondContext.getScriptEngineFactory(), secondFactory);
        assertSame(BaseTemplater.scriptEngineFactory, secondFactory,
            "The legacy static field is deliberately overwritten by the second injector");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<String> firstResult = executor.submit(() -> {
                start.await();
                return BaseTemplater.evaluateToString("${value}", ScriptingLanguage.BEANSHELL, firstContext);
            });
            Future<String> secondResult = executor.submit(() -> {
                start.await();
                return BaseTemplater.evaluateToString("${value}", ScriptingLanguage.BEANSHELL, secondContext);
            });
            start.countDown();
            assertEquals(firstResult.get(5, TimeUnit.SECONDS), "runtime-A",
                "The first context must not use the second runtime's static factory");
            assertEquals(secondResult.get(5, TimeUnit.SECONDS), "runtime-B");
        } finally {
            executor.shutdownNow();
        }
    }

    private static ScriptEngineFactory markerFactory(final String marker) {
        return new ScriptEngineFactory() {
            @Override
            public ScriptEngine getEngine(ScriptSource scriptSource) {
                return new ScriptEngine() {
                    @Override
                    public Object evaluate(DynamicScopeContext context, ScriptSource source) {
                        return marker;
                    }
                };
            }
        };
    }

    @Test
    public void testManualInjection() {
        // GIVEN: Guice injector
        // Use target/ as working directory - all generated files will go to target/
        String targetDir = "target";
        new java.io.File(targetDir).mkdirs();
        Injector injector = Guice.createInjector(new ScraperModule(targetDir));
        
        // WHEN: We manually get ScriptEngineFactory from injector
        ScriptEngineFactory factory = injector.getInstance(ScriptEngineFactory.class);
        
        // THEN: It should not be null
        assertNotNull(factory, "ScriptEngineFactory from injector should not be null");
        
        // AND: BaseTemplater.scriptEngineFactory should also not be null
        // (due to requestStaticInjection in ScraperModule)
        assertNotNull(BaseTemplater.scriptEngineFactory,
            "Static injection should have set BaseTemplater.scriptEngineFactory");
        
        // AND: They should be the same instance (because Singleton scope)
        assertSame(BaseTemplater.scriptEngineFactory, factory,
            "Static field and injector instance should be the same (Singleton)");
    }
    
}

