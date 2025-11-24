package org.webharvest.plugin.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Integration tests for Function and Call plugins.
 * 
 * Tests the complete function definition and execution flow:
 * 1. Function definition with FunctionPlugin
 * 2. Function execution with CallPlugin
 * 3. Return value handling with ReturnPlugin
 * 4. Parameter passing with CallParamPlugin
 * 
 * @since 2.3.1
 */
public class FunctionCallIntegrationTest extends XmlBasedIntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(FunctionCallIntegrationTest.class);

    /**
     * Test: Simple function definition and call with return value.
     * 
     * XML:
     * <function name="myFunction">
     *   <return>
     *     <template>Hello World</template>
     *   </return>
     * </function>
     * <def var="result">
     *   <call name="myFunction"/>
     * </def>
     * 
     * Expected:
     * - myFunction variable should exist (function definition)
     * - result variable should contain "Hello World"
     */
    @Test(groups = {"integration", "function", "call"})
    public void testSimpleFunctionWithReturn() throws Exception {
        LOG.info("Testing simple function with return value");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "  <function name=\"myFunction\">\n" +
            "    <return>\n" +
            "      <template>Hello World</template>\n" +
            "    </return>\n" +
            "  </function>\n" +
            "  <def var=\"result\">\n" +
            "    <call name=\"myFunction\"/>\n" +
            "  </def>\n" +
            "</config>";
        
        DynamicScopeContext ctx = executeXmlStringFromTest(xml);
        assertNotNull(ctx, "Context should not be null");
        
        // Check that function definition exists
        Variable functionDef = ctx.getVar("myFunction");
        assertNotNull(functionDef, "Function definition should exist");
        
        // Check that result variable contains the return value
        Variable result = ctx.getVar("result");
        assertNotNull(result, "Result variable should exist");
        assertFalse(result instanceof EmptyVariable, "Result should not be empty");
        assertEquals(result.toString(), "Hello World", "Result should contain 'Hello World'");
        
        LOG.info("✓ Simple function with return executed successfully");
    }

    /**
     * Test: Function without return - should return last expression.
     * 
     * XML:
     * <function name="myFunction">
     *   <template>Hello World</template>
     * </function>
     * <def var="result">
     *   <call name="myFunction"/>
     * </def>
     * 
     * Expected:
     * - result variable should contain "Hello World" (last expression)
     */
    @Test(groups = {"integration", "function", "call"})
    public void testFunctionWithoutReturn() throws Exception {
        LOG.info("Testing function without return (last expression)");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "  <function name=\"myFunction\">\n" +
            "    <template>Hello World</template>\n" +
            "  </function>\n" +
            "  <def var=\"result\">\n" +
            "    <call name=\"myFunction\"/>\n" +
            "  </def>\n" +
            "</config>";
        
        DynamicScopeContext ctx = executeXmlStringFromTest(xml);
        assertNotNull(ctx, "Context should not be null");
        
        // Check that result variable contains the last expression
        Variable result = ctx.getVar("result");
        assertNotNull(result, "Result variable should exist");
        assertFalse(result instanceof EmptyVariable, "Result should not be empty");
        assertEquals(result.toString(), "Hello World", "Result should contain 'Hello World'");
        
        LOG.info("✓ Function without return executed successfully");
    }

    /**
     * Test: Function with parameters.
     * 
     * XML:
     * <function name="greet">
     *   <def var="name"><call-param name="name"/></def>
     *   <template>Hello ${name}</template>
     * </function>
     * <def var="result">
     *   <call name="greet\">
     *     <call-param name="name">World</call-param>
     *   </call>
     * </def>
     * 
     * Expected:
     * - result variable should contain "Hello World"
     */
    @Test(groups = {"integration", "function", "call", "parameters"})
    public void testFunctionWithParameters() throws Exception {
        LOG.info("Testing function with parameters");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "  <function name=\"greet\">\n" +
            "    <def var=\"name\"><call-param name=\"name\"/></def>\n" +
            "    <template>Hello ${name}</template>\n" +
            "  </function>\n" +
            "  <def var=\"result\">\n" +
            "    <call name=\"greet\">\n" +
            "      <call-param name=\"name\">World</call-param>\n" +
            "    </call>\n" +
            "  </def>\n" +
            "</config>";
        
        DynamicScopeContext ctx = executeXmlStringFromTest(xml);
        assertNotNull(ctx, "Context should not be null");
        
        // Check that result variable contains the parameterized result
        Variable result = ctx.getVar("result");
        assertNotNull(result, "Result variable should exist");
        assertFalse(result instanceof EmptyVariable, "Result should not be empty");
        assertEquals(result.toString(), "Hello World", "Result should contain 'Hello World'");
        
        LOG.info("✓ Function with parameters executed successfully");
    }

    /**
     * Test: Function with multiple parameters.
     * 
     * XML:
     * <function name="concat">
     *   <def var="a"><call-param name="a"/></def>
     *   <def var="b"><call-param name="b"/></def>
     *   <template>${a}${b}</template>
     * </function>
     * <def var="result">
     *   <call name="concat\">
     *     <call-param name="a">Hello </call-param>
     *     <call-param name="b">World</call-param>
     *   </call>
     * </def>
     * 
     * Expected:
     * - result variable should contain "Hello World"
     */
    @Test(groups = {"integration", "function", "call", "parameters"})
    public void testFunctionWithMultipleParameters() throws Exception {
        LOG.info("Testing function with multiple parameters");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "  <function name=\"concat\">\n" +
            "    <def var=\"a\"><call-param name=\"a\"/></def>\n" +
            "    <def var=\"b\"><call-param name=\"b\"/></def>\n" +
            "    <template>${a}${b}</template>\n" +
            "  </function>\n" +
            "  <def var=\"result\">\n" +
            "    <call name=\"concat\">\n" +
            "      <call-param name=\"a\">Hello </call-param>\n" +
            "      <call-param name=\"b\">World</call-param>\n" +
            "    </call>\n" +
            "  </def>\n" +
            "</config>";
        
        DynamicScopeContext ctx = executeXmlStringFromTest(xml);
        assertNotNull(ctx, "Context should not be null");
        
        // Check that result variable contains the concatenated result
        Variable result = ctx.getVar("result");
        assertNotNull(result, "Result variable should exist");
        assertFalse(result instanceof EmptyVariable, "Result should not be empty");
        assertEquals(result.toString(), "Hello World", "Result should contain 'Hello World'");
        
        LOG.info("✓ Function with multiple parameters executed successfully");
    }

    /**
     * Test: Function called multiple times.
     * 
     * XML:
     * <function name="greet">
     *   <def var="name"><call-param name="name"/></def>
     *   <template>Hello ${name}</template>
     * </function>
     * <def var="result1">
     *   <call name="greet\">
     *     <call-param name="name">Alice</call-param>
     *   </call>
     * </def>
     * <def var="result2">
     *   <call name="greet\">
     *     <call-param name="name">Bob</call-param>
     *   </call>
     * </def>
     * 
     * Expected:
     * - result1 should contain "Hello Alice"
     * - result2 should contain "Hello Bob"
     */
    @Test(groups = {"integration", "function", "call", "multiple-calls"})
    public void testFunctionCalledMultipleTimes() throws Exception {
        LOG.info("Testing function called multiple times");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "  <function name=\"greet\">\n" +
            "    <def var=\"name\"><call-param name=\"name\"/></def>\n" +
            "    <template>Hello ${name}</template>\n" +
            "  </function>\n" +
            "  <def var=\"result1\">\n" +
            "    <call name=\"greet\">\n" +
            "      <call-param name=\"name\">Alice</call-param>\n" +
            "    </call>\n" +
            "  </def>\n" +
            "  <def var=\"result2\">\n" +
            "    <call name=\"greet\">\n" +
            "      <call-param name=\"name\">Bob</call-param>\n" +
            "    </call>\n" +
            "  </def>\n" +
            "</config>";
        
        DynamicScopeContext ctx = executeXmlStringFromTest(xml);
        assertNotNull(ctx, "Context should not be null");
        
        // Check first call
        Variable result1 = ctx.getVar("result1");
        assertNotNull(result1, "Result1 variable should exist");
        assertEquals(result1.toString(), "Hello Alice", "Result1 should contain 'Hello Alice'");
        
        // Check second call
        Variable result2 = ctx.getVar("result2");
        assertNotNull(result2, "Result2 variable should exist");
        assertEquals(result2.toString(), "Hello Bob", "Result2 should contain 'Hello Bob'");
        
        LOG.info("✓ Function called multiple times executed successfully");
    }

    /**
     * Test: Function that doesn't exist should fail gracefully.
     * 
     * XML:
     * <def var="result">
     *   <call name="nonExistentFunction"/>
     * </def>
     * 
     * Expected:
     * - Should throw exception or return EmptyVariable
     */
    @Test(groups = {"integration", "function", "call", "error-handling"})
    public void testCallNonExistentFunction() throws Exception {
        LOG.info("Testing call to non-existent function");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "  <def var=\"result\">\n" +
            "    <call name=\"nonExistentFunction\"/>\n" +
            "  </def>\n" +
            "</config>";
        
        try {
            DynamicScopeContext ctx = executeXmlStringFromTest(xml);
            // If execution succeeds, result should be empty
            Variable result = ctx.getVar("result");
            // Result should be EmptyVariable (CallPlugin returns EmptyVariable for non-existent functions)
            assertTrue(result == null || result instanceof EmptyVariable || result.isEmpty(), 
                "Result should be empty for non-existent function");
        } catch (Exception e) {
            // Exception is also acceptable
            assertNotNull(e.getMessage(), "Exception should have a message");
            LOG.info("Expected exception for non-existent function: {}", e.getMessage());
        }
        
        LOG.info("✓ Call to non-existent function handled");
    }

    /**
     * Helper method to execute XML string directly.
     */
    protected DynamicScopeContext executeXmlStringFromTest(String xml) throws Exception {
        // Create a temporary file in target/ directory
        java.io.File targetDir = new java.io.File("target");
        targetDir.mkdirs();
        java.nio.file.Path tempFile = java.nio.file.Paths.get(targetDir.getAbsolutePath(), 
            "test-function-" + System.currentTimeMillis() + ".xml");
        
        try {
            java.nio.file.Files.write(tempFile, xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return executeXmlConfig(tempFile.toString());
        } finally {
            // Cleanup will be handled by tearDown if file is added to outputFilesToCleanup
            // For now, we'll delete immediately
            try {
                java.nio.file.Files.deleteIfExists(tempFile);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
}

