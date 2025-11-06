package org.webharvest.examples;

import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.*;

/**
 * Integration test that runs ALL examples from IDE examples directory.
 * 
 * <p>
 * This test ensures that all built-in IDE examples execute without errors.
 * It's a comprehensive smoke test for the IDE example quality.
 * </p>
 * 
 * @author Robert Bala
 * @since 2.3.1
 */
public class AllIDEExamplesIntegrationTest extends ScraperTestBase {
    
    private static final String EXAMPLES_DIR = "../webharvest-ide/src/main/resources/webapp/examples";
    
    @Test(groups = {"integration", "ide-examples"}, 
          description = "Test XQuery example with all parameter types")
    public void testXQueryExample() throws Exception {
        System.out.println("\n### IDE Example: xquery.xml ###");
        testExampleFile("xquery.xml");
        
        // Verify output file was created (example writes to target/ directory)
        Path outputPath = Paths.get("target/xquerytest.xml");
        assertTrue(Files.exists(outputPath), "xquerytest.xml should be created in target/");
        
        String content = new String(Files.readAllBytes(outputPath));
        System.out.println("XQuery output length: " + content.length() + " bytes");
        
        // Verify key results
        assertFalse(content.trim().isEmpty(), "XQuery result should not be empty");
        assertTrue(content.contains("<test>"), "Should contain test element");
        assertTrue(content.contains("<logictest>"), "Should contain logictest");
        assertTrue(content.contains("<doubletest>"), "Should contain doubletest");
        assertTrue(content.contains("<stringtest>"), "Should contain stringtest");
        
        System.out.println("✅ xquery.xml PASSED");
        
        // Cleanup and move to test-output for traceability
        Path targetOutput = Paths.get("target/test-output/xquerytest.xml");
        Files.createDirectories(targetOutput.getParent());
        Files.copy(outputPath, targetOutput, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(outputPath);
    }
    
    @Test(groups = {"integration", "ide-examples"}, 
          description = "Test basic variables example")
    public void testBasicVariablesExample() throws Exception {
        System.out.println("\n### IDE Example: basic_variables_example.xml ###");
        testExampleFile("basic_variables_example.xml");
        System.out.println("✅ basic_variables_example.xml PASSED");
    }
    
    @Test(groups = {"integration", "ide-examples"}, 
          description = "Test simple test example")
    public void testSimpleTestExample() throws Exception {
        System.out.println("\n### IDE Example: simple_test.xml ###");
        testExampleFile("simple_test.xml");
        System.out.println("✅ simple_test.xml PASSED");
    }
    
    @Test(groups = {"integration", "ide-examples"}, 
          description = "Test functions example")
    public void testFunctionsExample() throws Exception {
        System.out.println("\n### IDE Example: functions.xml ###");
        testExampleFile("functions.xml");
        System.out.println("✅ functions.xml PASSED");
    }
    
    @Test(groups = {"integration", "ide-examples"}, 
          description = "Test data processing pipeline")
    public void testDataProcessingPipelineExample() throws Exception {
        System.out.println("\n### IDE Example: data_processing_pipeline.xml ###");
        testExampleFile("data_processing_pipeline.xml");
        System.out.println("✅ data_processing_pipeline.xml PASSED");
    }
    
    @Test(groups = {"integration", "ide-examples"}, 
          description = "Test modern web scraping example")
    public void testModernWebScrapingExample() throws Exception {
        System.out.println("\n### IDE Example: modern_web_scraping.xml ###");
        testExampleFile("modern_web_scraping.xml");
        System.out.println("✅ modern_web_scraping.xml PASSED");
    }
    
    @Test(groups = {"integration", "ide-examples"},
          description = "Test advanced search example")
    public void testAdvancedSearchExample() throws Exception {
        System.out.println("\n### IDE Example: advanced_search_example.xml ###");
        testExampleFile("advanced_search_example.xml");
        System.out.println("✅ advanced_search_example.xml PASSED");
    }
    
    @Test(groups = {"integration", "ide-examples"},
          description = "Test API integration example")
    public void testApiIntegrationExample() throws Exception {
        System.out.println("\n### IDE Example: api_integration.xml ###");
        testExampleFile("api_integration.xml");
        System.out.println("✅ api_integration.xml PASSED");
    }
    
    @Test(groups = {"integration", "ide-examples"},
          description = "Test product catalog example")
    public void testProductCatalogExample() throws Exception {
        System.out.println("\n### IDE Example: product_catalog.xml ###");
        testExampleFile("product_catalog.xml");
        System.out.println("✅ product_catalog.xml PASSED");
    }
    
    /**
     * Helper method to test an example file.
     * Loads XML from IDE examples directory and executes it.
     * 
     * @param filename Example filename (e.g., "xquery.xml")
     */
    private void testExampleFile(String filename) throws Exception {
        Path examplePath = Paths.get(EXAMPLES_DIR, filename);
        
        if (!Files.exists(examplePath)) {
            fail("Example file not found: " + examplePath);
        }
        
        String configXml = new String(Files.readAllBytes(examplePath));
        
        // Execute the example
        try {
            executeXmlString(configXml);
        } catch (Exception e) {
            System.err.println("❌ Failed to execute " + filename + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}





