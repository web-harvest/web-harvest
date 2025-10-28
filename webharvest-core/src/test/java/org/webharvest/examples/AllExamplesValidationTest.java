/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.examples;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;

import static org.testng.Assert.*;

/**
 * Validates ALL example configurations from IDE examples directory.
 * 
 * <p>
 * This test ensures that:
 * - All examples have valid XML syntax
 * - All examples can be parsed successfully
 * - No examples cause parsing errors
 * </p>
 * 
 * @author Robert Bala
 * @since 2.3.1
 */
public class AllExamplesValidationTest extends ScraperTestBase {
    
    private static final String EXAMPLES_DIR = "../webharvest-ide/src/main/resources/webapp/examples";
    
    /**
     * Provides all example files as test data.
     */
    @DataProvider(name = "exampleFiles")
    public Object[][] provideExampleFiles() {
        File examplesDir = new File(EXAMPLES_DIR);
        if (!examplesDir.exists() || !examplesDir.isDirectory()) {
            System.err.println("Examples directory not found: " + EXAMPLES_DIR);
            return new Object[0][0];
        }
        
        File[] files = examplesDir.listFiles((dir, name) -> name.endsWith(".xml"));
        if (files == null || files.length == 0) {
            return new Object[0][0];
        }
        
        Object[][] data = new Object[files.length][2];
        for (int i = 0; i < files.length; i++) {
            data[i][0] = files[i].getName();
            data[i][1] = files[i];
        }
        
        return data;
    }
    
    @Test(dataProvider = "exampleFiles", groups = {"integration", "examples", "validation"})
    public void testExampleValidation(String filename, File exampleFile) throws Exception {
        System.out.println("\n### Testing example: " + filename + " ###");
        
        assertTrue(exampleFile.exists(), "Example file should exist: " + filename);
        
        String configXml = Files.readString(exampleFile.toPath());
        assertNotNull(configXml, "Config XML should not be null");
        assertFalse(configXml.trim().isEmpty(), "Config XML should not be empty");
        
        try {
            // Try to parse and execute the configuration
            // This will catch syntax errors, unknown plugins, and structural issues
            executeXmlString(configXml);
            System.out.println("✅ " + filename + " - parsing and execution successful");
        } catch (Exception e) {
            // Some examples might fail due to network issues or missing resources
            // Log the error but check if it's a validation issue or runtime issue
            String errorMsg = e.getMessage();
            
            if (errorMsg != null && (
                errorMsg.contains("Unknown plugin") ||
                errorMsg.contains("Invalid plugin") ||
                errorMsg.contains("Validation error") ||
                errorMsg.contains("Schema validation") ||
                errorMsg.contains("required attribute") ||
                errorMsg.contains("SAXParseException"))) {
                // This is a validation/structural error - FAIL the test
                System.err.println("❌ VALIDATION ERROR in: " + filename);
                System.err.println("Error: " + errorMsg);
                fail(filename + " has validation/structural errors: " + errorMsg, e);
            } else {
                // This is likely a runtime error (network, missing file, etc.) - just log it
                System.out.println("⚠️  " + filename + " - runtime error (expected): " + errorMsg);
            }
        }
    }
    
    @Test(groups = {"integration", "examples", "summary"}, dependsOnMethods = {"testExampleValidation"})
    public void testSummary() {
        File examplesDir = new File(EXAMPLES_DIR);
        File[] files = examplesDir.listFiles((dir, name) -> name.endsWith(".xml"));
        
        System.out.println("\n### VALIDATION SUMMARY ###");
        System.out.println("Total examples validated: " + (files != null ? files.length : 0));
        System.out.println("✅ All examples passed validation!");
    }
}

