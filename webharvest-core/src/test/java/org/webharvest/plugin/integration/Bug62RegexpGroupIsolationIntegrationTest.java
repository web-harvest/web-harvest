package org.webharvest.plugin.integration;

import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Integration test for Bug #62: Regexp Group Isolation
 * 
 * Issue: When two REGEX statements are used (not nested), the second regex
 * retrieves the first group value from the FIRST regex instead of its own groups.
 * 
 * Reported: GitHub Issue #62
 * Fixed in: v2.2.0
 */
public class Bug62RegexpGroupIsolationIntegrationTest extends XmlBasedIntegrationTestBase {

    /**
     * Test Bug #62: Two independent regexp statements should have isolated groups
     * 
     * Expected behavior:
     * - First regexp extracts pairs: ["name:John", "age:30", "city:NYC"]
     * - Second regexp (in loop) extracts key:value from each pair
     * - Each pair should show correct key and value:
     *   Pair 0: Key=name, Value=John
     *   Pair 1: Key=age, Value=30
     *   Pair 2: Key=city, Value=NYC
     * 
     * Bug behavior (before fix):
     * - Second regexp's group1 returns values from FIRST regexp
     * - Results would be incorrect/mixed
     */
    @Test(groups = {"integration", "bugs"})
    public void testBug62_RegexpGroupIsolation() throws Exception {
        // Execute configuration
        String xmlPath = "integration-tests/bug62_regexp_group_isolation.xml";
        String output = executeAndReadOutputFile(xmlPath, "bug62_output.txt");
        
        // Verify output contains correct key-value pairs
        assertNotNull(output, "Output should not be null");
        assertFalse(output.trim().isEmpty(), "Output should not be empty");
        
        // Expected results (after fix)
        assertTrue(output.contains("Key=name"), "Should extract 'name' as key");
        assertTrue(output.contains("Value=John"), "Should extract 'John' as value");
        
        assertTrue(output.contains("Key=age"), "Should extract 'age' as key");
        assertTrue(output.contains("Value=30"), "Should extract '30' as value");
        
        assertTrue(output.contains("Key=city"), "Should extract 'city' as key");
        assertTrue(output.contains("Value=NYC"), "Should extract 'NYC' as value");
        
        // Verify specific pair outputs
        assertTrue(output.contains("Pair 0: Key=name, Value=John"), 
                "First pair should have correct key and value");
        assertTrue(output.contains("Pair 1: Key=age, Value=30"), 
                "Second pair should have correct key and value");
        assertTrue(output.contains("Pair 2: Key=city, Value=NYC"), 
                "Third pair should have correct key and value");
        
        System.out.println("Bug #62 test PASSED - Regexp groups are properly isolated");
        System.out.println("Output:\n" + output);
    }
    
    /**
     * Simplified test: Just verify that two sequential regexp calls
     * don't interfere with each other's groups
     */
    @Test(groups = {"integration", "bugs"})
    public void testSimpleGroupIsolation() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "  <!-- First regexp: capture 'ABC' -->\n" +
            "  <regexp replace=\"false\">\n" +
            "    <regexp-pattern>([A-Z]+)</regexp-pattern>\n" +
            "    <regexp-source>ABC123</regexp-source>\n" +
            "  </regexp>\n" +
            "  <def var=\"firstgroup\">\n" +
            "    <get var=\"group1\"/>\n" +
            "  </def>\n" +
            "  \n" +
            "  <!-- Second regexp: capture '456' -->\n" +
            "  <regexp replace=\"false\">\n" +
            "    <regexp-pattern>([0-9]+)</regexp-pattern>\n" +
            "    <regexp-source>XYZ456</regexp-source>\n" +
            "  </regexp>\n" +
            "  <def var=\"secondgroup\">\n" +
            "    <get var=\"group1\"/>\n" +
            "  </def>\n" +
            "</config>";

        DynamicScopeContext context = executeXmlString(xml);

        Variable first = context.getVar("firstgroup");
        Variable second = context.getVar("secondgroup");

        assertNotNull(first, "firstgroup variable should exist");
        assertNotNull(second, "secondgroup variable should exist");

        assertEquals(first.toString().trim(), "ABC", "First regexp should capture 'ABC'");
        assertEquals(second.toString().trim(), "456", "Second regexp should capture '456'");

        assertTrue(!second.toString().trim().equals(first.toString().trim()),
                "Bug #62: Second regexp should NOT see groups from first regexp");
    }
}

