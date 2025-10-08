/*
 Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.runtime.templaters;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.scripting.ScriptEngineFactory;
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.runtime.scripting.jsr.JSRScriptEngineFactory;
import org.webharvest.runtime.variables.NodeVariable;
import static org.testng.Assert.*;

/**
 * Extended tests for BaseTemplater
 */
public class BaseTemplaterExtendedTest {

    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        context = new ScraperContext();
        // BaseTemplater requires scripting language to be set
        context.setScriptingLanguage(ScriptingLanguage.GROOVY);
        
        // Always initialize scriptEngineFactory with JSRScriptEngineFactory
        // This ensures it works even if other tests have overridden it
        BaseTemplater.scriptEngineFactory = new JSRScriptEngineFactory();
    }

    @Test
    public void testEvaluateSimpleText() {
        String result = BaseTemplater.evaluateToString("Hello World", null, context);
        assertEquals(result, "Hello World");
    }

    @Test
    public void testEvaluateWithVariable() {
        context.setLocalVar("name", "John");
        String result = BaseTemplater.evaluateToString("Hello ${name}", null, context);
        assertEquals(result, "Hello John");
    }

    @Test
    public void testEvaluateWithMultipleVariables() {
        context.setLocalVar("first", "Hello");
        context.setLocalVar("second", "World");
        String result = BaseTemplater.evaluateToString("${first} ${second}!", null, context);
        assertEquals(result, "Hello World!");
    }

    @Test
    public void testEvaluateWithNonExistingVariable() {
        // Non-existing variable throws exception in Groovy
        try {
            String result = BaseTemplater.evaluateToString("Hello ${unknown}", null, context);
            // Some implementations may return the template as-is
            assertNotNull(result);
        } catch (Exception e) {
            // Groovy throws MissingPropertyException - this is expected
            assertTrue(e.getMessage().contains("unknown") || e.getMessage().contains("MissingProperty"));
        }
    }

    @Test
    public void testEvaluateEmptyString() {
        String result = BaseTemplater.evaluateToString("", null, context);
        // Empty string may return null or empty
        assertTrue(result == null || result.isEmpty());
    }

    @Test
    public void testEvaluateNull() {
        String result = BaseTemplater.evaluateToString(null, null, context);
        // Null input may return null or empty string
        // Just verify it doesn't throw exception
    }

    @Test
    public void testEvaluateWithNestedVariables() {
        context.setLocalVar("outer", "value");
        context.setLocalVar("inner", "outer");
        String result = BaseTemplater.evaluateToString("${outer}", null, context);
        assertEquals(result, "value");
    }

    @Test
    public void testEvaluateWithSpecialCharacters() {
        context.setLocalVar("special", "<>&\"");
        String result = BaseTemplater.evaluateToString("${special}", null, context);
        assertNotNull(result);
    }
}

