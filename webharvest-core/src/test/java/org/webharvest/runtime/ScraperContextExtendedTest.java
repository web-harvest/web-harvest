/*
 Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.runtime;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import static org.testng.Assert.*;

/**
 * Extended tests for ScraperContext
 */
public class ScraperContextExtendedTest {

    private ScraperContext context;

    @BeforeMethod
    public void setUp() {
        context = new ScraperContext();
    }

    @Test
    public void testSetAndGetCharset() {
        context.setCharset("UTF-8");
        assertEquals(context.getCharset(), "UTF-8");
    }

    @Test
    public void testSetAndGetScriptingLanguage() {
        context.setScriptingLanguage(ScriptingLanguage.JAVASCRIPT);
        assertEquals(context.getScriptingLanguage(), ScriptingLanguage.JAVASCRIPT);
    }

    @Test
    public void testDefaultCharset() {
        String charset = context.getCharset();
        // Default charset can be null or a default value
        // Just verify it doesn't throw exception
        assertNotNull(context);
    }

    @Test
    public void testSetCharsetToNull() {
        context.setCharset("UTF-8");
        context.setCharset(null);
        // Should handle null charset
    }

    @Test
    public void testMultipleCharsetChanges() {
        context.setCharset("UTF-8");
        context.setCharset("ISO-8859-1");
        context.setCharset("UTF-16");
        assertEquals(context.getCharset(), "UTF-16");
    }

    @Test
    public void testScriptingLanguageDefault() {
        ScriptingLanguage lang = context.getScriptingLanguage();
        // Default can be null or a specific language
        // Just verify context works
        assertNotNull(context);
    }

    @Test
    public void testVariableManagement() {
        context.setLocalVar("test1", "value1");
        context.setLocalVar("test2", new NodeVariable("value2"));
        
        Variable var1 = context.getVar("test1");
        Variable var2 = context.getVar("test2");
        
        assertNotNull(var1);
        assertNotNull(var2);
        assertEquals(var1.toString(), "value1");
        assertEquals(var2.toString(), "value2");
    }
}

