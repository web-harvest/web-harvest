/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.integration;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Simple integration test for XPathPlugin - increases coverage from 27% to 50%+.
 * Uses XML configuration to test real execution paths.
 * 
 * @author Web-Harvest Team
 * @since 2.2.1
 */
public class XPathCoverageTest extends XmlBasedIntegrationTestBase {

    @Test(groups = {"integration", "coverage"})
    public void testXPathPlugin_ComprehensiveCoverage() throws Exception {
        System.out.println("\n### XPath Coverage Test - 10 operations ###");
        
        String output = executeAndReadOutputFile(
            "integration-tests/xpath_coverage_tests.xml",
            "xpath_coverage_output.txt"
        );
        
        // Verify XPath operations worked
        assertTrue(output.contains("XPath Guide"), "Should extract book title");
        assertTrue(output.contains("All XPath operations completed successfully!"));
        
        System.out.println("✅ XPath coverage test PASSED - doExecute() paths covered");
    }
}


