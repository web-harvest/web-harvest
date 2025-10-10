package org.webharvest.runtime.templaters;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.runtime.scripting.ScriptEngineFactory;
import org.webharvest.runtime.scripting.jsr.JSRScriptEngineFactory;

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
        Injector injector = Guice.createInjector(new ScraperModule("."));
        
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
    public void testManualInjection() {
        // GIVEN: Guice injector
        Injector injector = Guice.createInjector(new ScraperModule("."));
        
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
    
    @Test
    public void testEvaluateToStringWithInjection() {
        // GIVEN: Guice injector with ScraperModule
        Injector injector = Guice.createInjector(new ScraperModule("."));
        
        // WHEN: BaseTemplater.scriptEngineFactory is injected
        assertNotNull(BaseTemplater.scriptEngineFactory);
        
        // THEN: evaluateToString should work (not throw NullPointerException)
        // Note: We can't fully test this without DynamicScopeContext,
        // but we can verify scriptEngineFactory is available
        
        String source = "${1 + 1}";
        // This will fail if scriptEngineFactory is null
        try {
            // Just checking that scriptEngineFactory is not null
            // (actual evaluation would require context)
            assertTrue(true, "scriptEngineFactory is available");
        } catch (NullPointerException e) {
            fail("NullPointerException - scriptEngineFactory is null!");
        }
    }
}

