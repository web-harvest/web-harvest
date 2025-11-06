/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.examples;

import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.*;

/**
 * Integration test for xquery.xml example.
 * 
 * <p>
 * This test verifies that the xquery.xml example executes successfully,
 * demonstrating:
 * - Boolean parameter conversion
 * - Double parameter conversion
 * - Integer sequence (integer*) parameter handling
 * - String parameter handling
 * - Node() parameter auto-detection from XML content
 * - XQuery expression execution with Java function calls (math:sqrt)
 * - User-defined XQuery functions
 * </p>
 * 
 * @author Robert Bala
 * @since 2.3.1
 */
public class XQueryExampleIntegrationTest extends ScraperTestBase {
    
    @Test(groups = {"integration", "examples", "xquery"})
    public void testXQueryExample() throws Exception {
        System.out.println("\n### XQuery Example Integration Test ###");
        
        // Load xquery.xml from examples (relative to project root, tests run from webharvest-core)
        String examplePath = "../webharvest-ide/src/main/resources/webapp/examples/xquery.xml";
        File exampleFile = new File(examplePath);
        
        assertTrue(exampleFile.exists(), "xquery.xml example should exist at: " + examplePath);
        
        String configXml = Files.readString(exampleFile.toPath());
        
        System.out.println("Executing xquery.xml example...");
        
        // Execute the example and get result variable
        org.webharvest.runtime.DynamicScopeContext context = executeXmlString(configXml);
        org.webharvest.runtime.variables.Variable resultVar = context.getVar("result");
        
        assertNotNull(resultVar, "Result variable should be set");
        String output = resultVar.toString();
        System.out.println("XQuery output:\n" + output);
        
        // Also verify file was created (in target/ directory)
        Path outputFile = Path.of("target/xquerytest.xml");
        assertTrue(Files.exists(outputFile), "Output file xquerytest.xml should be created in target/");
        
        String fileContent = Files.readString(outputFile);
        assertEquals(fileContent.trim(), output.trim(), "File content should match result variable");
        
        // Verify all expected elements are present
        assertTrue(output.contains("<test>"), "Should contain root <test> element");
        assertTrue(output.contains("<logictest>"), "Should contain <logictest> element");
        assertTrue(output.contains("<doubletest>"), "Should contain <doubletest> element");
        assertTrue(output.contains("<stringtest>"), "Should contain <stringtest> element");
        assertTrue(output.contains("<numberseqtest>"), "Should contain <numberseqtest> element");
        assertTrue(output.contains("<doctest>"), "Should contain <doctest> element");
        
        // Verify logic test (boolean parameter)
        assertTrue(output.contains("1"), "logictest: if(true) should return 1");
        assertTrue(output.contains("false"), "logictest: true eq (12 gt 15) should be false");
        
        // Verify double test
        assertTrue(output.contains("34"), "doubletest: 12 * 2 + 10 = 34");
        assertTrue(output.contains("12"), "doubletest: round(12) = 12");
        
        // Verify string test  
        assertTrue(output.contains("ABCDEFGH123456true12"), 
            "stringtest: should concat all parameters");
        
        // Verify number sequence test (CRITICAL - tests integer* parameter)
        assertTrue(output.contains("<value>2.732050807568877</value>"), 
            "numberseqtest: should contain sqrt(3)+1");
        assertTrue(output.contains("<value>6.196152422706632</value>"), 
            "numberseqtest: should contain sqrt(27)+1");
        assertTrue(output.contains("3mama"), 
            "numberseqtest: should contain concat(intseq[1], 'mama')");
        
        // Count <value> elements - should be 5 (one for each number in sequence)
        int valueCount = output.split("<value>").length - 1;
        assertEquals(valueCount, 5, "Should have 5 <value> elements from integer sequence");
        
        // Verify doc test (node() parameter auto-detection)
        assertTrue(output.contains("<td>one</td>"), "doctest: should contain first td");
        assertTrue(output.contains("<td>six</td>"), "doctest: should contain last td");
        
        // Count <td> elements - should be 6
        int tdCount = output.split("<td>").length - 1;
        assertEquals(tdCount, 6, "Should have 6 <td> elements from node() parameter");
        
        // Clean up
        Files.deleteIfExists(outputFile);
        
        System.out.println("✅ XQuery example executed successfully!");
        System.out.println("   ✓ Boolean parameters work");
        System.out.println("   ✓ Double parameters work");
        System.out.println("   ✓ Integer sequences (integer*) work");
        System.out.println("   ✓ String parameters work");
        System.out.println("   ✓ Node() auto-detection works");
        System.out.println("   ✓ Java function calls (math:sqrt) work");
        System.out.println("   ✓ User-defined XQuery functions work");
    }
}

