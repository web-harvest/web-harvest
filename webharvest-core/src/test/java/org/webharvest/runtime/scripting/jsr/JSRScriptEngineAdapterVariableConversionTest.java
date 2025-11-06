package org.webharvest.runtime.scripting.jsr;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.scripting.ScriptSource;
import org.webharvest.runtime.variables.NodeVariable;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.webharvest.ioc.ScraperModule;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Test for JSRScriptEngineAdapter variable conversion.
 * 
 * PROBLEM: BeanShell receives Variable objects instead of their values!
 * This causes: Operator '+' inappropriate for objects
 * 
 * This test verifies that variables are correctly converted to Java types
 * before being passed to BeanShell.
 */
public class JSRScriptEngineAdapterVariableConversionTest {

    private DynamicScopeContext context;
    private JSRScriptEngineFactory factory;
    private JSRScriptEngineAdapter engine;

    @BeforeMethod
    public void setUp() {
        // Create Guice injector to get ScriptEngineFactory
        // Use target/ as working directory - all generated files will go to target/
        String targetDir = "target";
        new java.io.File(targetDir).mkdirs();
        Injector injector = Guice.createInjector(new ScraperModule(targetDir));
        factory = (JSRScriptEngineFactory) injector.getInstance(
            org.webharvest.runtime.scripting.ScriptEngineFactory.class);
        
        // Create real ScraperContext
        context = injector.getInstance(ScraperContext.class);
        
        // Setup mock config for context
        org.webharvest.definition.Config mockConfig = mock(org.webharvest.definition.Config.class);
        context.setConfig(mockConfig);
        context.setScriptingLanguage(org.webharvest.runtime.scripting.ScriptingLanguage.BEANSHELL);
        
        // Create engine for BeanShell
        org.webharvest.runtime.scripting.ScriptSource dummySource = 
            new org.webharvest.runtime.scripting.ScriptSource("dummy", 
                org.webharvest.runtime.scripting.ScriptingLanguage.BEANSHELL);
        engine = (JSRScriptEngineAdapter) factory.getEngine(dummySource);
    }

    @Test
    public void testArithmeticWithNodeVariable() {
        // GIVEN: NodeVariable with numeric value
        context.setLocalVar("counter", new NodeVariable("5"));
        
        // WHEN: Evaluate BeanShell expression with arithmetic
        ScriptSource script = new ScriptSource("counter + 1", 
            org.webharvest.runtime.scripting.ScriptingLanguage.BEANSHELL);
        Object result = engine.evaluate(context, script);
        
        // THEN: Should return 6 (not throw "inappropriate for objects")
        assertNotNull(result, "Result should not be null");
        
        // BeanShell might return Integer or Double
        if (result instanceof Number) {
            assertEquals(((Number) result).intValue(), 6, 
                "counter (5) + 1 should equal 6");
        } else {
            fail("Expected Number result, got: " + result.getClass());
        }
    }
    
    @Test
    public void testStringConcatenationWithNodeVariable() {
        // GIVEN: NodeVariable with string value
        context.setLocalVar("name", new NodeVariable("World"));
        
        // WHEN: Evaluate BeanShell expression with string concatenation
        ScriptSource script = new ScriptSource("\"Hello \" + name", 
            org.webharvest.runtime.scripting.ScriptingLanguage.BEANSHELL);
        Object result = engine.evaluate(context, script);
        
        // THEN: Should return "Hello World"
        assertNotNull(result);
        assertEquals(result.toString(), "Hello World", 
            "String concatenation should work");
    }
    
    @Test
    public void testComparisonWithNodeVariable() {
        // GIVEN: NodeVariable with numeric value
        context.setLocalVar("count", new NodeVariable("3"));
        
        // WHEN: Evaluate BeanShell comparison expression
        ScriptSource script = new ScriptSource("count < 5", 
            org.webharvest.runtime.scripting.ScriptingLanguage.BEANSHELL);
        Object result = engine.evaluate(context, script);
        
        // THEN: Should return true
        assertNotNull(result);
        assertTrue(result instanceof Boolean, "Result should be Boolean");
        assertTrue((Boolean) result, "count (3) < 5 should be true");
    }
    
    @Test
    public void testMultipleVariablesArithmetic() {
        // GIVEN: Multiple NodeVariables
        context.setLocalVar("a", new NodeVariable("10"));
        context.setLocalVar("b", new NodeVariable("20"));
        
        // WHEN: Evaluate complex expression
        ScriptSource script = new ScriptSource("a + b * 2", 
            org.webharvest.runtime.scripting.ScriptingLanguage.BEANSHELL);
        Object result = engine.evaluate(context, script);
        
        // THEN: Should return 50 (10 + 20*2 = 10 + 40)
        assertNotNull(result);
        if (result instanceof Number) {
            assertEquals(((Number) result).intValue(), 50, 
                "a (10) + b (20) * 2 should equal 50");
        } else {
            fail("Expected Number result, got: " + result.getClass());
        }
    }
}

