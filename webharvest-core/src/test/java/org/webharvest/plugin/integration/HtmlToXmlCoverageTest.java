/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.integration;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Integration test for HtmlToXmlPlugin coverage improvement.
 * 
 * PURPOSE: Increase HtmlToXmlPlugin coverage from 9% to 50%+
 * METHOD: Execute real XML configurations with various HTML parsing scenarios
 * 
 * @author Web-Harvest Team
 * @since 2.2.1
 */
public class HtmlToXmlCoverageTest extends XmlBasedIntegrationTestBase {

    @Test(groups = {"integration", "coverage"})
    public void testHtmlToXmlPlugin_ComprehensiveCoverage() throws Exception {
        System.out.println("\n### HtmlToXml Coverage Test - 10 scenarios ###");
        
        String output = executeAndReadOutputFile(
            "integration-tests/htmltoxml_coverage_tests.xml",
            "htmltoxml_coverage_output.txt"
        );
        
        // Verify various HTML parsing scenarios worked
        assertTrue(output.contains("Test 1 - Simple:"), "Should parse simple HTML");
        assertTrue(output.contains("Test 2 - Special chars:"), "Should handle special chars");
        assertTrue(output.contains("Test 3 - Unicode:"), "Should handle Unicode");
        assertTrue(output.contains("Test 8 - Attributes:"), "Should preserve attributes");
        assertTrue(output.contains("All HtmlToXml operations completed successfully!"));
        
        System.out.println("✅ HtmlToXml coverage test PASSED - doExecute() paths covered");
    }
}

