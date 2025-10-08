/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.runtime.variables;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for variable classes.
 * 
 * @author Robert Bala
 * @since 2.2.0-SNAPSHOT
 */
public class VariablesTest {
    
    @Test
    public void testEmptyVariable() {
        EmptyVariable empty = EmptyVariable.INSTANCE;
        assertNotNull(empty, "EmptyVariable should not be null");
        assertSame(empty, EmptyVariable.INSTANCE, "Should be singleton");
        
        // Test basic methods
        assertEquals(empty.toString(), "", "Empty string should be empty");
        assertTrue(empty.isEmpty(), "Should be empty");
        
        // Test binary conversion
        byte[] binary = empty.toBinary();
        assertNotNull(binary, "Binary should not be null");
        assertEquals(binary.length, 0, "Binary should be empty");
        
        byte[] binaryUtf8 = empty.toBinary("UTF-8");
        assertNotNull(binaryUtf8, "Binary UTF-8 should not be null");
        assertEquals(binaryUtf8.length, 0, "Binary UTF-8 should be empty");
    }
    
    @Test
    public void testNodeVariable() {
        String content = "<root><item>test</item></root>";
        NodeVariable node = new NodeVariable(content);
        assertNotNull(node, "NodeVariable should not be null");
        
        assertEquals(node.toString(), content, "Content should match");
        assertFalse(node.isEmpty(), "Should not be empty");
        
        // Test binary conversion
        byte[] binary = node.toBinary();
        assertNotNull(binary, "Binary should not be null");
        assertTrue(binary.length > 0, "Binary should not be empty");
        
        byte[] binaryUtf8 = node.toBinary("UTF-8");
        assertNotNull(binaryUtf8, "Binary UTF-8 should not be null");
        assertTrue(binaryUtf8.length > 0, "Binary UTF-8 should not be empty");
    }
    
    @Test
    public void testNodeVariableWithNull() {
        NodeVariable node = new NodeVariable(null);
        assertNotNull(node, "NodeVariable should not be null");
        
        assertEquals(node.toString(), "", "Null content should be empty string");
        assertTrue(node.isEmpty(), "Should be empty");
    }
    
    @Test
    public void testListVariable() {
        List<String> items = Arrays.asList("item1", "item2", "item3");
        ListVariable list = new ListVariable(items);
        assertNotNull(list, "ListVariable should not be null");
        
        assertFalse(list.isEmpty(), "Should not be empty");
        
        // Test iteration
        int count = 0;
        for (Object item : list) {
            assertNotNull(item, "Item should not be null");
            count++;
        }
        assertEquals(count, 3, "Should iterate over 3 items");
        
        // Test toString
        String str = list.toString();
        assertNotNull(str, "String should not be null");
        assertTrue(str.contains("item1"), "Should contain item1");
    }
    
    @Test
    public void testListVariableWithEmptyList() {
        List<String> emptyList = Arrays.asList();
        ListVariable list = new ListVariable(emptyList);
        assertNotNull(list, "ListVariable should not be null");
        
        assertTrue(list.isEmpty(), "Should be empty");
        
        // Test iteration
        int count = 0;
        for (Object item : list) {
            count++;
        }
        assertEquals(count, 0, "Should iterate over 0 items");
    }
    
    @Test
    public void testListVariableWithNull() {
        ListVariable list = new ListVariable(null);
        assertNotNull(list, "ListVariable should not be null");
        
        assertTrue(list.isEmpty(), "Should be empty");
    }
    
    @Test
    public void testScriptingVariable() {
        String script = "var x = 1 + 2; return x;";
        ScriptingVariable scriptVar = new ScriptingVariable(script);
        assertNotNull(scriptVar, "ScriptingVariable should not be null");
        
        assertEquals(scriptVar.toString(), script, "Script should match");
        assertFalse(scriptVar.isEmpty(), "Should not be empty");
        
        // Test binary conversion
        byte[] binary = scriptVar.toBinary();
        assertNotNull(binary, "Binary should not be null");
        assertTrue(binary.length > 0, "Binary should not be empty");
    }
    
    @Test
    public void testScriptingVariableWithNull() {
        ScriptingVariable scriptVar = new ScriptingVariable(null);
        assertNotNull(scriptVar, "ScriptingVariable should not be null");
        
        assertEquals(scriptVar.toString(), "", "Null script should be empty string");
        assertTrue(scriptVar.isEmpty(), "Should be empty");
    }
    
    @Test
    public void testVariableName() {
        String name = "testVariable";
        VariableName varName = new VariableName(name);
        assertNotNull(varName, "VariableName should not be null");
        
        assertEquals(varName.getValue(), name, "Value should match");
        // toString() returns object representation, not the value
        assertNotNull(varName.toString(), "ToString should not be null");
    }
    
    @Test(expectedExceptions = org.webharvest.exception.VariableException.class)
    public void testVariableNameWithNull() {
        // VariableName constructor throws exception for null
        new VariableName(null);
    }
    
    @Test
    public void testTypes() {
        // Test Types constants
        assertNotNull(Types.TYPE_EMPTY, "TYPE_EMPTY should not be null");
        assertNotNull(Types.TYPE_TEXT, "TYPE_TEXT should not be null");
        assertNotNull(Types.TYPE_XML, "TYPE_XML should not be null");
        assertNotNull(Types.TYPE_BINARY, "TYPE_BINARY should not be null");
        assertNotNull(Types.TYPE_LIST, "TYPE_LIST should not be null");
        assertNotNull(Types.TYPE_DEFAULT, "TYPE_DEFAULT should not be null");
        
        // Test that they are different
        assertFalse(Types.TYPE_EMPTY.equals(Types.TYPE_TEXT), "Types should be different");
        assertFalse(Types.TYPE_XML.equals(Types.TYPE_BINARY), "Types should be different");
        assertFalse(Types.TYPE_LIST.equals(Types.TYPE_DEFAULT), "Types should be different");
    }
    
    @Test
    public void testVariableEquality() {
        String content = "test content";
        NodeVariable var1 = new NodeVariable(content);
        NodeVariable var2 = new NodeVariable(content);
        
        // Test string representation (since equals might not be implemented)
        assertEquals(var1.toString(), var2.toString(), "Variables with same content should have same string representation");
    }
    
    @Test
    public void testVariableInequality() {
        NodeVariable var1 = new NodeVariable("content1");
        NodeVariable var2 = new NodeVariable("content2");
        
        // Test not equals
        assertFalse(var1.equals(var2), "Variables with different content should not be equal");
        assertFalse(var2.equals(var1), "Inequality should be symmetric");
    }
    
    @Test
    public void testVariableWithDifferentTypes() {
        NodeVariable nodeVar = new NodeVariable("test");
        ListVariable listVar = new ListVariable(Arrays.asList("test"));
        
        // Different types should not be equal
        assertFalse(nodeVar.equals(listVar), "Different variable types should not be equal");
        assertFalse(listVar.equals(nodeVar), "Inequality should be symmetric");
    }
}
