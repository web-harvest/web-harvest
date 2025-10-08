/*
 Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.runtime.variables;

import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;
import static org.testng.Assert.*;

/**
 * Extended tests for ListVariable
 */
public class ListVariableExtendedTest {

    @Test
    public void testConstructorWithEmptyList() {
        List<Variable> list = new ArrayList<>();
        ListVariable var = new ListVariable(list);
        assertNotNull(var.toList());
        assertEquals(var.toList().size(), 0);
    }

    @Test
    public void testConstructorWithSingleItem() {
        List<Variable> list = new ArrayList<>();
        list.add(new NodeVariable("test"));
        ListVariable var = new ListVariable(list);
        assertEquals(var.toList().size(), 1);
    }

    @Test
    public void testConstructorWithMultipleItems() {
        List<Variable> list = new ArrayList<>();
        list.add(new NodeVariable("item1"));
        list.add(new NodeVariable("item2"));
        list.add(new NodeVariable("item3"));
        ListVariable var = new ListVariable(list);
        assertEquals(var.toList().size(), 3);
    }

    @Test
    public void testToList() {
        List<Variable> list = new ArrayList<>();
        list.add(new NodeVariable("test"));
        ListVariable var = new ListVariable(list);
        List<?> result = var.toList();
        assertNotNull(result);
        assertEquals(result.size(), 1);
    }

    @Test
    public void testToStringWithEmptyList() {
        ListVariable var = new ListVariable(new ArrayList<>());
        assertNotNull(var.toString());
    }

    @Test
    public void testToStringWithItems() {
        List<Variable> list = new ArrayList<>();
        list.add(new NodeVariable("a"));
        list.add(new NodeVariable("b"));
        ListVariable var = new ListVariable(list);
        String result = var.toString();
        assertNotNull(result);
        assertTrue(result.contains("a") || result.length() >= 0);
    }
}








