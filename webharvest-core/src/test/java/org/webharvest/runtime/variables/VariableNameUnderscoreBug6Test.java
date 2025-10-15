/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.runtime.variables;

import org.testng.annotations.Test;
import org.webharvest.exception.VariableException;

import static org.testng.Assert.*;

/**
 * TDD tests for Bug #6: Allow underscores in variable names.
 * 
 * <h3>Bug #6: Underscore Support in Variable Names</h3>
 * <p>Original limitation: Variable names could only contain letters and digits.
 * Pattern: {@code ^[a-zA-Z][a-zA-Z0-9]*?$}</p>
 * 
 * <p>User request: Allow underscores for better naming conventions, especially
 * for function parameters (e.g., {@code p_parameter}, {@code input_value}).</p>
 * 
 * <p>New pattern: {@code ^[a-zA-Z][a-zA-Z0-9_]*?$}</p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class VariableNameUnderscoreBug6Test {
    
    /**
     * RED: Test that function parameter with p_ prefix is allowed.
     */
    @Test(groups = {"unit", "variables", "bug6"})
    public void testVariableName_WithPPrefix_Accepted() {
        // GIVEN
        String name = "p_parameter";
        
        // WHEN
        VariableName variableName = new VariableName(name);
        
        // THEN
        assertNotNull(variableName, "Variable name should be created");
        assertEquals(variableName.getValue(), "p_parameter", "Name should match");
    }
    
    /**
     * RED: Test that variable with underscore in middle is allowed.
     */
    @Test(groups = {"unit", "variables", "bug6"})
    public void testVariableName_WithUnderscoreInMiddle_Accepted() {
        // GIVEN
        String name = "input_value";
        
        // WHEN
        VariableName variableName = new VariableName(name);
        
        // THEN
        assertNotNull(variableName, "Variable name should be created");
        assertEquals(variableName.getValue(), "input_value", "Name should match");
    }
    
    /**
     * RED: Test that variable with multiple underscores is allowed.
     */
    @Test(groups = {"unit", "variables", "bug6"})
    public void testVariableName_WithMultipleUnderscores_Accepted() {
        // GIVEN
        String name = "my_long_variable_name";
        
        // WHEN
        VariableName variableName = new VariableName(name);
        
        // THEN
        assertNotNull(variableName, "Variable name should be created");
        assertEquals(variableName.getValue(), "my_long_variable_name", "Name should match");
    }
    
    /**
     * RED: Test that underscore at end is allowed.
     */
    @Test(groups = {"unit", "variables", "bug6"})
    public void testVariableName_WithUnderscoreAtEnd_Accepted() {
        // GIVEN
        String name = "temp_";
        
        // WHEN
        VariableName variableName = new VariableName(name);
        
        // THEN
        assertNotNull(variableName, "Variable name should be created");
        assertEquals(variableName.getValue(), "temp_", "Name should match");
    }
    
    /**
     * RED: Test that variable starting with underscore is REJECTED.
     * Must start with letter!
     */
    @Test(expectedExceptions = VariableException.class, groups = {"unit", "variables", "bug6"})
    public void testVariableName_StartingWithUnderscore_Rejected() {
        // WHEN
        new VariableName("_invalid");
        
        // THEN - exception expected
    }
    
    /**
     * Regression: Test that normal alphanumeric names still work.
     */
    @Test(groups = {"unit", "variables", "bug6", "regression"})
    public void testVariableName_NormalAlphanumeric_StillWorks() {
        // GIVEN
        String[] validNames = {"myVar", "var1", "test123", "a", "xyz99"};
        
        // WHEN & THEN
        for (String name : validNames) {
            VariableName variableName = new VariableName(name);
            assertEquals(variableName.getValue(), name, "Name should match: " + name);
        }
    }
    
    /**
     * Regression: Test that invalid names are still rejected.
     */
    @Test(groups = {"unit", "variables", "bug6", "regression"})
    public void testVariableName_InvalidNames_StillRejected() {
        // GIVEN
        String[] invalidNames = {
            "",           // empty
            "123",        // starts with digit
            "my-var",     // hyphen not allowed
            "my var",     // space not allowed
            "my.var",     // dot not allowed
            "my@var"      // special char not allowed
        };
        
        // WHEN & THEN
        for (String name : invalidNames) {
            try {
                new VariableName(name);
                fail("Should reject invalid name: " + name);
            } catch (VariableException e) {
                // Expected
                assertTrue(e.getMessage().contains("Invalid variable name"),
                    "Exception message should indicate invalid name");
            }
        }
    }
    
    /**
     * Real-world use case: Function parameters with p_ prefix.
     */
    @Test(groups = {"unit", "variables", "bug6", "usecase"})
    public void testVariableName_FunctionParameterPrefixes_Work() {
        // GIVEN - common function parameter naming patterns
        String[] paramNames = {
            "p_url",
            "p_username", 
            "p_password",
            "p_apikey",
            "input_data",
            "output_result"
        };
        
        // WHEN & THEN
        for (String name : paramNames) {
            VariableName variableName = new VariableName(name);
            assertEquals(variableName.getValue(), name, 
                "Function parameter name should be valid: " + name);
        }
    }
    
    /**
     * Edge case: Mix of digits and underscores.
     */
    @Test(groups = {"unit", "variables", "bug6"})
    public void testVariableName_MixedDigitsAndUnderscores_Accepted() {
        // GIVEN
        String name = "var_123_test_456";
        
        // WHEN
        VariableName variableName = new VariableName(name);
        
        // THEN
        assertEquals(variableName.getValue(), name, "Mixed name should work");
    }
}

