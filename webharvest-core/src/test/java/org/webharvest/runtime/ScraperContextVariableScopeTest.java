package org.webharvest.runtime;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * Unit test for ScraperContext variable scope management.
 * 
 * Tests the critical bug where variables disappear after nested scope cleanup.
 * 
 * PROBLEM: When executeWithinNewContext() exits (via finally block),
 * it calls removeVarFromCRT() which pops variables from the stack.
 * If the stack becomes empty, the variable is completely removed from centralReferenceTable.
 * 
 * This causes variables set in the root scope to disappear after child scopes exit.
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class ScraperContextVariableScopeTest {

    private ScraperContext context;

    @BeforeMethod
    public void setUp() {
        context = new ScraperContext();
        context.initContext();
    }

    @Test
    public void testVariableExistsInRootScope() {
        // BASELINE: Variables set in root scope should exist
        context.setLocalVar("testVar", new NodeVariable("test value"));
        
        Variable result = context.getVar("testVar");
        
        assertNotNull(result, "Variable should exist in root scope");
        assertEquals("test value", result.toString());
    }

    @Test
    public void testVariableSurvivesNestedScopeExit() throws InterruptedException {
        // PROBLEM TEST: This currently FAILS!
        // Variables set in root scope should survive after nested scope exits
        
        // 1. Set variable in root scope
        context.setLocalVar("rootVar", new NodeVariable("root value"));
        System.out.println("✓ Set rootVar in root scope");
        
        // 2. Execute in nested scope (simulating plugin execution)
        context.executeWithinNewContext(() -> {
            System.out.println("  → Entered nested scope");
            
            // Nested scope can see parent variables
            Variable parentVar = context.getVar("rootVar");
            assertNotNull(parentVar, "Should see parent variable in nested scope");
            
            // Set a local variable in nested scope
            context.setLocalVar("nestedVar", new NodeVariable("nested value"));
            System.out.println("  ✓ Set nestedVar in nested scope");
            
            return null;
        });
        System.out.println("✓ Exited nested scope");
        
        // 3. Check if root variable still exists
        Variable rootVar = context.getVar("rootVar");
        
        // THIS ASSERTION FAILS - the bug!
        assertNotNull(rootVar, 
            "Root variable should survive after nested scope exits! " +
            "BUG: removeVarFromCRT() removes variable from centralReferenceTable when stack becomes empty");
        assertEquals("root value", rootVar.toString());
    }

    @Test
    public void testNestedVariableDisappearsAfterScopeExit() throws InterruptedException {
        // This should work correctly - nested variables should disappear
        
        context.setLocalVar("rootVar", new NodeVariable("root"));
        
        context.executeWithinNewContext(() -> {
            context.setLocalVar("nestedVar", new NodeVariable("nested"));
            
            // Both should exist in nested scope
            assertNotNull(context.getVar("rootVar"));
            assertNotNull(context.getVar("nestedVar"));
            
            return null;
        });
        
        // Root variable should exist
        assertNotNull(context.getVar("rootVar"), "Root var should exist");
        
        // Nested variable should NOT exist (this is correct behavior)
        assertNull(context.getVar("nestedVar"), "Nested var should not exist after scope exit");
    }

    @Test
    public void testMultipleNestedScopes() throws InterruptedException {
        // PROBLEM TEST: Multiple nested scopes should not affect root variables
        
        context.setLocalVar("level0", new NodeVariable("value0"));
        
        context.executeWithinNewContext(() -> {
            context.setLocalVar("level1", new NodeVariable("value1"));
            
            context.executeWithinNewContext(() -> {
                context.setLocalVar("level2", new NodeVariable("value2"));
                
                // All levels visible in deepest scope
                assertNotNull(context.getVar("level0"));
                assertNotNull(context.getVar("level1"));
                assertNotNull(context.getVar("level2"));
                
                return null;
            });
            
            // After level2 exits, only level0 and level1 should exist
            assertNotNull(context.getVar("level0"));
            assertNotNull(context.getVar("level1"));
            assertNull(context.getVar("level2"));
            
            return null;
        });
        
        // After all nested scopes exit, only level0 should exist
        // THIS FAILS - level0 disappears!
        assertNotNull(context.getVar("level0"), "Level 0 variable should survive all nested scope exits");
        assertNull(context.getVar("level1"));
        assertNull(context.getVar("level2"));
    }

    @Test
    public void testVariableOverridingInNestedScope() throws InterruptedException {
        // Test variable shadowing - nested scope overrides parent variable
        
        context.setLocalVar("var", new NodeVariable("parent value"));
        
        context.executeWithinNewContext(() -> {
            // Override in nested scope
            context.setLocalVar("var", new NodeVariable("child value"));
            
            Variable var = context.getVar("var");
            assertEquals("child value", var.toString(), "Should see child value in nested scope");
            
            return null;
        });
        
        // After nested scope exits, should see parent value again
        Variable var = context.getVar("var");
        // THIS FAILS - variable completely disappears instead of reverting to parent value!
        assertNotNull(var, "Variable should revert to parent value, not disappear!");
        assertEquals("parent value", var.toString(), "Should see parent value after nested scope exits");
    }
}

