package org.webharvest.plugin.core.integration;

import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * TDD Integration Test for Template with variable interpolation.
 * 
 * PROBLEM: Template output in IDE shows raw HTML instead of interpolated template!
 * In user's example:
 * - title = '' (empty, OK - no <title> in HTML)
 * - heading = '<h1>Herman Melville - Moby-Dick</h1>' (OK)
 * - BUT file output is FULL HTML instead of "Title: \nHeading: ..."
 * 
 * This test verifies:
 * 1. Template interpolates variables correctly
 * 2. Template handles empty variables (${title} when title='')
 * 3. FilePlugin gets correct content from Template child
 */
public class TemplateWithVariablesIntegrationTest extends XmlBasedIntegrationTestBase {
    
    @Test
    public void testTemplateWithEmptyVariable() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"title\"></def>\n" +
            "    <def var=\"heading\">Herman Melville</def>\n" +
            "    <def var=\"result\">\n" +
            "        <template>Title: ${title}\nHeading: ${heading}</template>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable result = context.getVar("result");
        
        assertNotNull(result, "Template result should not be null");
        String content = result.toString();
        
        System.out.println("Template result: [" + content + "]");
        
        // Should interpolate both variables (title empty, heading has value)
        assertTrue(content.contains("Title:"), "Should contain 'Title:' label");
        assertTrue(content.contains("Heading:"), "Should contain 'Heading:' label");
        assertTrue(content.contains("Herman Melville"), "Should contain heading value");
        
        // Should NOT contain literal ${
        assertFalse(content.contains("${"), 
            "Should NOT contain literal ${, but got: " + content);
    }
    
    @Test
    public void testFileWithTemplateChild() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"name\">John</def>\n" +
            "    <def var=\"age\">30</def>\n" +
            "    <file action=\"write\" path=\"target/test-output/template_test.txt\">\n" +
            "        <template>Name: ${name}, Age: ${age}</template>\n" +
            "    </file>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        // Read file content
        java.io.File outputFile = new java.io.File("target/test-output/template_test.txt");
        assertTrue(outputFile.exists(), "Output file should be created");
        
        String content = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()));
        System.out.println("File content: [" + content + "]");
        
        // Should contain interpolated values
        assertTrue(content.contains("Name: John"), "Should contain 'Name: John'");
        assertTrue(content.contains("Age: 30"), "Should contain 'Age: 30'");
        
        // Should NOT contain literal ${
        assertFalse(content.contains("${"), 
            "File should NOT contain literal ${, but got: " + content);
    }
    
    @Test
    public void testTemplateWithHttpAndXPath() throws Exception {
        // Simplified version of user's example
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"heading\">Test Heading</def>\n" +
            "    <def var=\"result\">\n" +
            "        <template>Heading: ${heading}</template>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable result = context.getVar("result");
        
        assertNotNull(result);
        String content = result.toString();
        
        System.out.println("Template result: [" + content + "]");
        
        assertEquals(content.trim(), "Heading: Test Heading", 
            "Should interpolate heading variable");
    }
}

