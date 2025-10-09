/*
 Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.runtime;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import static org.testng.Assert.*;

/**
 * Extended tests for DynamicScopeContext
 */
public class DynamicScopeContextExtendedTest {

    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        context = new ScraperContext();
    }

    @Test
    public void testSetAndGetLocalVar() {
        context.setLocalVar("test", "value");
        Variable var = context.getVar("test");
        assertNotNull(var);
        assertEquals(var.toString(), "value");
    }

    @Test
    public void testGetVarNotExists() {
        Variable var = context.getVar("nonexistent");
        assertNull(var);
    }

    @Test
    public void testSetLocalVarOverwrite() {
        context.setLocalVar("key", "value1");
        context.setLocalVar("key", "value2");
        Variable var = context.getVar("key");
        assertEquals(var.toString(), "value2");
    }

    @Test
    public void testSetLocalVarWithVariable() {
        NodeVariable nodeVar = new NodeVariable("test value");
        context.setLocalVar("myVar", nodeVar);
        Variable result = context.getVar("myVar");
        assertNotNull(result);
        assertEquals(result.toString(), "test value");
    }

    @Test
    public void testMultipleVariables() {
        context.setLocalVar("var1", "value1");
        context.setLocalVar("var2", "value2");
        context.setLocalVar("var3", "value3");
        
        assertEquals(context.getVar("var1").toString(), "value1");
        assertEquals(context.getVar("var2").toString(), "value2");
        assertEquals(context.getVar("var3").toString(), "value3");
    }
}













