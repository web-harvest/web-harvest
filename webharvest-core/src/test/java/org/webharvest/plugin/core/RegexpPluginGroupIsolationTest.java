package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.*;

/**
 * Comprehensive unit tests for Bug #62: Regexp group isolation
 * 
 * Issue: When two REGEX statements are used (not nested), the second regex
 * retrieves the first group value from the FIRST regex instead of its own groups.
 * 
 * Root cause: Groups are stored in shared context without proper isolation.
 * 
 * These tests are DIFFICULT and COMPREHENSIVE to ensure quality and catch edge cases.
 */
public class RegexpPluginGroupIsolationTest {

    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        // Create simple context for testing (without Guice complexity)
        context = new ScraperContext();
    }

    /**
     * HARD TEST 1: Two sequential regexps with different group counts
     * First has 3 groups, second has 1 group
     * Bug symptom: group2 and group3 from first persist into second execution
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testDifferentGroupCounts_OldGroupsShouldBeCleared() throws Exception {
        RegexpPlugin plugin = new RegexpPlugin();
        
        // First regexp: 3 groups - extract email parts (user@domain.com)
        Map<String, String> config1 = new HashMap<>();
        config1.put("pattern", "([^@]+)@([^.]+)\\.(.+)");
        config1.put("source", "john@example.com");
        
        plugin.initialize(new DefaultPluginConfiguration("test-regexp", "http://org.webharvest/schema/2.1/core", "regexp", config1));
        Variable result1 = plugin.execute(context);
        
        // Verify first regexp sets 4 group variables (group0-group3)
        assertEquals(context.getVar("group0").toString(), "john@example.com", "group0 should be full match");
        assertEquals(context.getVar("group1").toString(), "john", "group1 should be username");
        assertEquals(context.getVar("group2").toString(), "example", "group2 should be domain");
        assertEquals(context.getVar("group3").toString(), "com", "group3 should be TLD");
        
        // Second regexp: 1 group - extract number
        RegexpPlugin plugin2 = new RegexpPlugin();
        Map<String, String> config2 = new HashMap<>();
        config2.put("pattern", "([0-9]+)");
        config2.put("source", "Price: 999");
        
        plugin2.initialize(new DefaultPluginConfiguration("test-regexp-id", "http://org.webharvest/schema/2.1/core", "regexp", config2));
        Variable result2 = plugin2.execute(context);
        
        // CRITICAL: After second regexp, old groups (group2, group3) should be CLEARED
        assertEquals(context.getVar("group0").toString(), "999", "group0 should be from second regexp");
        assertEquals(context.getVar("group1").toString(), "999", "group1 should be from second regexp");
        
        // Bug #62: These should be EMPTY, not values from first regexp!
        Variable group2 = context.getVar("group2");
        Variable group3 = context.getVar("group3");
        
        assertTrue(group2 == null || group2 instanceof EmptyVariable || group2.toString().isEmpty(),
                "BUG #62: group2 should be cleared, not '" + group2 + "' from first regexp");
        assertTrue(group3 == null || group3 instanceof EmptyVariable || group3.toString().isEmpty(),
                "BUG #62: group3 should be cleared, not '" + group3 + "' from first regexp");
    }

    /**
     * HARD TEST 2: Multiple sequential regexps in loop
     * Simulates real-world scenario from bug report
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testMultipleRegexpsInLoop_EachShouldHaveIsolatedGroups() throws Exception {
        String[] testCases = {
            "name:John:30",     // 3 parts
            "city:NYC",         // 2 parts
            "email:test@example.com:verified"  // 3 parts
        };
        
        for (String testCase : testCases) {
            RegexpPlugin plugin = new RegexpPlugin();
            
            // Extract parts separated by colon
            Map<String, String> config = new HashMap<>();
            config.put("pattern", "([^:]+):([^:]+)(?::([^:]+))?");
            config.put("source", testCase);
            
            plugin.initialize(new DefaultPluginConfiguration("test-regexp-id", "http://org.webharvest/schema/2.1/core", "regexp", config));
            Variable result = plugin.execute(context);
            
            // Verify groups are from CURRENT iteration, not previous
            String[] parts = testCase.split(":");
            assertEquals(context.getVar("group1").toString(), parts[0],
                    "group1 should be from current iteration: " + testCase);
            assertEquals(context.getVar("group2").toString(), parts[1],
                    "group2 should be from current iteration: " + testCase);
            
            // group3 may or may not exist depending on test case
            if (parts.length > 2) {
                assertEquals(context.getVar("group3").toString(), parts[2],
                        "group3 should be from current iteration: " + testCase);
            }
        }
    }

    /**
     * HARD TEST 3: Nested patterns with overlapping group numbers
     * Most complex scenario - outer and inner regexps both use group1
     */
    @Test(groups = {"unit", "bugs", "hard", "complex"})
    public void testNestedPatternsWithOverlappingGroups() throws Exception {
        // Outer regexp: extract FIRST key-value pair
        RegexpPlugin outerPlugin = new RegexpPlugin();
        Map<String, String> outerConfig = new HashMap<>();
        outerConfig.put("pattern", "([^=]+)=(.+)");  // Simple pattern for key=value
        outerConfig.put("source", "key1=value1");
        
        outerPlugin.initialize(new DefaultPluginConfiguration("test-regexp-id", "http://org.webharvest/schema/2.1/core", "regexp", outerConfig));
        Variable outerResult = outerPlugin.execute(context);
        
        String outerGroup1 = context.getVar("group1").toString();
        String outerGroup2 = context.getVar("group2").toString();
        
        assertEquals(outerGroup1, "key1", "Outer group1 should be key");
        assertEquals(outerGroup2, "value1", "Outer group2 should be value");
        
        // Inner regexp: extract parts of the value (simulate nested processing)
        RegexpPlugin innerPlugin = new RegexpPlugin();
        Map<String, String> innerConfig = new HashMap<>();
        innerConfig.put("pattern", "([a-z]+)([0-9]+)");
        innerConfig.put("source", "value1");
        
        innerPlugin.initialize(new DefaultPluginConfiguration("test-regexp-id", "http://org.webharvest/schema/2.1/core", "regexp", innerConfig));
        Variable innerResult = innerPlugin.execute(context);
        
        // CRITICAL: Inner execution should REPLACE outer groups
        String innerGroup1 = context.getVar("group1").toString();
        String innerGroup2 = context.getVar("group2").toString();
        
        assertEquals(innerGroup1, "value", "Inner group1 should be 'value', not '" + outerGroup1 + "' from outer");
        assertEquals(innerGroup2, "1", "Inner group2 should be '1', not '" + outerGroup2 + "' from outer");
        
        // Verify old groups are gone
        assertFalse(innerGroup1.equals(outerGroup1), "Bug #62: Inner should not see outer group1");
        assertFalse(innerGroup2.equals(outerGroup2), "Bug #62: Inner should not see outer group2");
    }

    /**
     * HARD TEST 4: Zero groups in second regexp
     * Edge case: second regexp has NO capturing groups
     * Old groups from first should still be cleared
     */
    @Test(groups = {"unit", "bugs", "hard", "edge-case"})
    public void testZeroGroupsInSecondRegexp_OldGroupsShouldStillBeCleared() throws Exception {
        RegexpPlugin plugin1 = new RegexpPlugin();
        
        // First: 2 capturing groups
        Map<String, String> config1 = new HashMap<>();
        config1.put("pattern", "([A-Z]+):([0-9]+)");
        config1.put("source", "CODE:123");
        
        plugin1.initialize(new DefaultPluginConfiguration("test-regexp-id", "http://org.webharvest/schema/2.1/core", "regexp", config1));
        plugin1.execute(context);
        
        assertEquals(context.getVar("group1").toString(), "CODE");
        assertEquals(context.getVar("group2").toString(), "123");
        
        // Second: NO capturing groups (non-capturing pattern)
        RegexpPlugin plugin2 = new RegexpPlugin();
        Map<String, String> config2 = new HashMap<>();
        config2.put("pattern", "test");  // No groups
        config2.put("source", "this is a test string");
        
        plugin2.initialize(new DefaultPluginConfiguration("test-regexp-id", "http://org.webharvest/schema/2.1/core", "regexp", config2));
        plugin2.execute(context);
        
        // group0 should exist (full match)
        assertEquals(context.getVar("group0").toString(), "test");
        
        // Bug #62: group1 and group2 from first regexp should be CLEARED
        Variable group1 = context.getVar("group1");
        Variable group2 = context.getVar("group2");
        
        assertTrue(group1 == null || group1 instanceof EmptyVariable || group1.toString().isEmpty(),
                "Bug #62: group1 should be cleared when second regexp has no groups");
        assertTrue(group2 == null || group2 instanceof EmptyVariable || group2.toString().isEmpty(),
                "Bug #62: group2 should be cleared when second regexp has no groups");
    }

    /**
     * HARD TEST 5: Many groups (stress test)
     * First regexp has 10 groups, second has 2
     * Ensures cleanup works for large numbers of groups
     */
    @Test(groups = {"unit", "bugs", "hard", "stress"})
    public void testManyGroups_AllOldGroupsShouldBeCleared() throws Exception {
        RegexpPlugin plugin1 = new RegexpPlugin();
        
        // First: 10 capturing groups
        Map<String, String> config1 = new HashMap<>();
        config1.put("pattern", "(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)");//10 single chars
        config1.put("source", "ABCDEFGHIJ");
        
        plugin1.initialize(new DefaultPluginConfiguration("test-regexp-id", "http://org.webharvest/schema/2.1/core", "regexp", config1));
        plugin1.execute(context);
        
        // Verify all 10 groups are set
        for (int i = 1; i <= 10; i++) {
            assertNotNull(context.getVar("group" + i), "group" + i + " should exist after first regexp");
        }
        
        // Second: Only 2 groups
        RegexpPlugin plugin2 = new RegexpPlugin();
        Map<String, String> config2 = new HashMap<>();
        config2.put("pattern", "(\\d+)-(\\d+)");
        config2.put("source", "99-88");
        
        plugin2.initialize(new DefaultPluginConfiguration("test-regexp-id", "http://org.webharvest/schema/2.1/core", "regexp", config2));
        plugin2.execute(context);
        
        // Verify new groups
        assertEquals(context.getVar("group1").toString(), "99");
        assertEquals(context.getVar("group2").toString(), "88");
        
        // CRITICAL: groups 3-10 should be CLEARED
        for (int i = 3; i <= 10; i++) {
            Variable groupVar = context.getVar("group" + i);
            assertTrue(groupVar == null || groupVar instanceof EmptyVariable || groupVar.toString().isEmpty(),
                    "Bug #62: group" + i + " should be cleared after second regexp");
        }
    }

}

