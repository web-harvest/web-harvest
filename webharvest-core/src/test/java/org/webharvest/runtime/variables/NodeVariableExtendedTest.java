/*
 Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.runtime.variables;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Extended tests for NodeVariable
 */
public class NodeVariableExtendedTest {

    @Test
    public void testConstructorWithString() {
        NodeVariable var = new NodeVariable("test");
        assertEquals(var.toString(), "test");
    }

    @Test
    public void testConstructorWithInteger() {
        NodeVariable var = new NodeVariable(123);
        assertEquals(var.toString(), "123");
    }

    @Test
    public void testConstructorWithNull() {
        NodeVariable var = new NodeVariable(null);
        assertEquals(var.toString(), "");
    }

    @Test
    public void testConstructorWithBoolean() {
        NodeVariable varTrue = new NodeVariable(true);
        NodeVariable varFalse = new NodeVariable(false);
        assertEquals(varTrue.toString(), "true");
        assertEquals(varFalse.toString(), "false");
    }

    @Test
    public void testToStringMethod() {
        NodeVariable var = new NodeVariable("Hello World");
        assertNotNull(var.toString());
        assertEquals(var.toString(), "Hello World");
    }

    @Test
    public void testToStringWithEmptyString() {
        NodeVariable var = new NodeVariable("");
        assertEquals(var.toString(), "");
    }

    @Test
    public void testToStringWithWhitespace() {
        NodeVariable var = new NodeVariable("   ");
        assertEquals(var.toString(), "   ");
    }

    @Test
    public void testToStringWithSpecialCharacters() {
        NodeVariable var = new NodeVariable("<>&\"'");
        assertTrue(var.toString().length() > 0);
    }
}













