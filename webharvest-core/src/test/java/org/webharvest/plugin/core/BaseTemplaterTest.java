package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
// REMOVED in v2.2.x - old architecture classes no longer exist
// import org.webharvest.UnitilsTestNGExtension;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.scripting.ScriptEngine;
import org.webharvest.runtime.scripting.ScriptEngineFactory;
import org.webharvest.runtime.scripting.ScriptSource;
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Test for BaseTemplater to verify variable resolution.
 */
public class BaseTemplaterTest {
    // REMOVED in v2.2.x - UnitilsTestNGExtension no longer exists
    // extends UnitilsTestNGExtension

    // REMOVED in v2.2.x - UnitilsTestNGExtension methods no longer needed
    // @Override
    // protected ScriptEngineFactory getScriptEngineFactory() {
    private ScriptEngineFactory getScriptEngineFactory() {
        return new ScriptEngineFactory() {
            @Override
            public ScriptEngine getEngine(ScriptSource scriptSource) {
                return new ScriptEngine() {
                    @Override
                    public Object evaluate(DynamicScopeContext context, ScriptSource script) {
                        String expression = script.getSourceCode();
                        
                        // Simple variable resolution for test
                        if ("testVar".equals(expression)) {
                            return "Hello World";
                        } else if ("number".equals(expression)) {
                            return "42";
                        } else if ("testVar + ' ' + number".equals(expression)) {
                            return "Hello World 42";
                        } else if ("name".equals(expression)) {
                            return "John";
                        } else if ("age".equals(expression)) {
                            return "25";
                        }
                        return expression; // Return as-is for other expressions
                    }
                };
            }
        };
    }

    @Test
    public void testBaseTemplaterWithVariables() throws Exception {
        // Create a simple test context with variables
        DynamicScopeContext context = new ScraperContext();
        
        context.setLocalVar("testVar", new NodeVariable("Hello World"));
        context.setLocalVar("number", new NodeVariable("42"));
        
        // Test simple variable resolution
        String template = "Text: ${testVar}, Number: ${number}";
        System.out.println("Testing BaseTemplater with template: " + template);
        
        Variable result = BaseTemplater.evaluateToVariable(template, ScriptingLanguage.BEANSHELL, context);
        
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result should not be empty");
        
        String resultString = result.toString();
        System.out.println("BaseTemplater result: " + resultString);
        
        // Check if variables were resolved
        assertTrue(resultString.contains("Hello World"), "Should contain resolved testVar");
        assertTrue(resultString.contains("42"), "Should contain resolved number");
    }
    
    @Test
    public void testBaseTemplaterWithoutVariables() throws Exception {
        // Create a simple test context
        DynamicScopeContext context = new ScraperContext();
        
        // Test text without variables
        String template = "Simple text without variables";
        Variable result = BaseTemplater.evaluateToVariable(template, ScriptingLanguage.BEANSHELL, context);
        
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result should not be empty");
        
        String resultString = result.toString();
        System.out.println("BaseTemplater result (no vars): " + resultString);
        
        // Check if text is preserved
        assertEquals(resultString, "Simple text without variables", "Should preserve original text");
    }
    
    @Test
    public void testBaseTemplaterWithMixedContent() throws Exception {
        // Create a simple test context with variables
        DynamicScopeContext context = new ScraperContext();
        context.setLocalVar("name", new NodeVariable("John"));
        context.setLocalVar("age", new NodeVariable("25"));
        
        // Test mixed content
        String template = "Hello ${name}, you are ${age} years old. Static text here.";
        Variable result = BaseTemplater.evaluateToVariable(template, ScriptingLanguage.BEANSHELL, context);
        
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result should not be empty");
        
        String resultString = result.toString();
        System.out.println("BaseTemplater result (mixed): " + resultString);
        
        // Check if variables were resolved
        assertTrue(resultString.contains("John"), "Should contain resolved name");
        assertTrue(resultString.contains("25"), "Should contain resolved age");
        assertTrue(resultString.contains("Static text here"), "Should contain static text");
    }
}
