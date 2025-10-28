/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.testng.annotations.Test;
import org.webharvest.examples.ScraperTestBase;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Test for XQuery parameter type conversion.
 * 
 * <p>
 * Tests that xq-param elements with different types (boolean, integer, double, string)
 * are properly converted from text content to the correct Java types before being
 * passed to Saxon XQuery engine.
 * </p>
 * 
 * <p>
 * Bug: XQuery parameters were passed as strings regardless of type attribute,
 * causing Saxon to throw "Required type is xs:boolean; supplied value has type xs:string"
 * errors during execution.
 * </p>
 * 
 * @author Robert Bala
 * @since 2.3.1
 */
public class XQueryParameterTypesTest extends ScraperTestBase {
    
    @Test(groups = {"integration", "xquery", "parameter-types"})
    public void testBooleanParameterConversion() throws Exception {
        System.out.println("\n### XQuery: Boolean parameter type conversion ###");
        
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "  <def var=\"result\">\n" +
                "    <xquery>\n" +
                "      <xq-param name=\"isActive\" type=\"boolean\">true</xq-param>\n" +
                "      <xq-expression>\n" +
                "        <![CDATA[\n" +
                "          declare variable $isActive as xs:boolean external;\n" +
                "          <result>{if ($isActive) then 'YES' else 'NO'}</result>\n" +
                "        ]]>\n" +
                "      </xq-expression>\n" +
                "    </xquery>\n" +
                "  </def>\n" +
                "</config>";
        
        Variable result = executeAndGetVariable(configXml, "result");
        
        assertNotNull(result, "Result should not be null");
        String resultStr = result.toString().trim();
        assertTrue(resultStr.contains("YES"), "Boolean true should result in 'YES', got: " + resultStr);
        
        System.out.println("✅ Boolean parameter correctly converted from 'true' string to xs:boolean");
    }
    
    @Test(groups = {"integration", "xquery", "parameter-types"})
    public void testDoubleParameterConversion() throws Exception {
        System.out.println("\n### XQuery: Double parameter type conversion ###");
        
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "  <def var=\"result\">\n" +
                "    <xquery>\n" +
                "      <xq-param name=\"price\" type=\"double\">25.50</xq-param>\n" +
                "      <xq-expression>\n" +
                "        <![CDATA[\n" +
                "          declare variable $price as xs:double external;\n" +
                "          <result>{$price * 2}</result>\n" +
                "        ]]>\n" +
                "      </xq-expression>\n" +
                "    </xquery>\n" +
                "  </def>\n" +
                "</config>";
        
        Variable result = executeAndGetVariable(configXml, "result");
        
        assertNotNull(result, "Result should not be null");
        String resultStr = result.toString().trim();
        assertTrue(resultStr.contains("51"), "25.50 * 2 should be 51, got: " + resultStr);
        
        System.out.println("✅ Double parameter correctly converted from '25.50' string to xs:double");
    }
    
    @Test(groups = {"integration", "xquery", "parameter-types"})
    public void testIntegerParameterConversion() throws Exception {
        System.out.println("\n### XQuery: Integer parameter type conversion ###");
        
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "  <def var=\"result\">\n" +
                "    <xquery>\n" +
                "      <xq-param name=\"count\" type=\"integer\">10</xq-param>\n" +
                "      <xq-expression>\n" +
                "        <![CDATA[\n" +
                "          declare variable $count as xs:integer external;\n" +
                "          <result>{$count + 5}</result>\n" +
                "        ]]>\n" +
                "      </xq-expression>\n" +
                "    </xquery>\n" +
                "  </def>\n" +
                "</config>";
        
        Variable result = executeAndGetVariable(configXml, "result");
        
        assertNotNull(result, "Result should not be null");
        String resultStr = result.toString().trim();
        assertTrue(resultStr.contains("15"), "10 + 5 should be 15, got: " + resultStr);
        
        System.out.println("✅ Integer parameter correctly converted from '10' string to xs:integer");
    }
    
    @Test(groups = {"integration", "xquery", "parameter-types"})
    public void testStringParameterDefault() throws Exception {
        System.out.println("\n### XQuery: String parameter (default type) ###");
        
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "  <def var=\"result\">\n" +
                "    <xquery>\n" +
                "      <xq-param name=\"message\" type=\"string\">Hello World</xq-param>\n" +
                "      <xq-expression>\n" +
                "        <![CDATA[\n" +
                "          declare variable $message as xs:string external;\n" +
                "          <result>{concat($message, '!')}</result>\n" +
                "        ]]>\n" +
                "      </xq-expression>\n" +
                "    </xquery>\n" +
                "  </def>\n" +
                "</config>";
        
        Variable result = executeAndGetVariable(configXml, "result");
        
        assertNotNull(result, "Result should not be null");
        String resultStr = result.toString().trim();
        assertTrue(resultStr.contains("Hello World!"), "Should contain concatenated string, got: " + resultStr);
        
        System.out.println("✅ String parameter works correctly");
    }
    
    @Test(groups = {"integration", "xquery", "parameter-types"})
    public void testMultipleTypedParameters() throws Exception {
        System.out.println("\n### XQuery: Multiple typed parameters ###");
        
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "  <def var=\"result\">\n" +
                "    <xquery>\n" +
                "      <xq-param name=\"active\" type=\"boolean\">true</xq-param>\n" +
                "      <xq-param name=\"price\" type=\"double\">12.5</xq-param>\n" +
                "      <xq-param name=\"qty\" type=\"integer\">3</xq-param>\n" +
                "      <xq-expression>\n" +
                "        <![CDATA[\n" +
                "          declare variable $active as xs:boolean external;\n" +
                "          declare variable $price as xs:double external;\n" +
                "          declare variable $qty as xs:integer external;\n" +
                "          <result>\n" +
                "            <active>{$active}</active>\n" +
                "            <total>{$price * $qty}</total>\n" +
                "          </result>\n" +
                "        ]]>\n" +
                "      </xq-expression>\n" +
                "    </xquery>\n" +
                "  </def>\n" +
                "</config>";
        
        Variable result = executeAndGetVariable(configXml, "result");
        
        assertNotNull(result, "Result should not be null");
        String resultStr = result.toString().trim();
        assertTrue(resultStr.contains("true"), "Should contain boolean value, got: " + resultStr);
        assertTrue(resultStr.contains("37.5"), "12.5 * 3 = 37.5, got: " + resultStr);
        
        System.out.println("✅ Multiple typed parameters work correctly");
    }
    
    @Test(groups = {"integration", "xquery", "parameter-types", "sequences"})
    public void testIntegerSequenceParameter() throws Exception {
        System.out.println("\n### XQuery: Integer sequence parameter (integer*) ###");
        
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "  <def var=\"result\">\n" +
                "    <xquery>\n" +
                "      <xq-param name=\"numbers\" type=\"integer*\">\n" +
                "        3\n" +
                "        7\n" +
                "        14\n" +
                "        18\n" +
                "        27\n" +
                "      </xq-param>\n" +
                "      <xq-expression>\n" +
                "        <![CDATA[\n" +
                "          declare variable $numbers as xs:integer* external;\n" +
                "          <result>\n" +
                "            <count>{count($numbers)}</count>\n" +
                "            <first>{$numbers[1]}</first>\n" +
                "            <sum>{sum($numbers)}</sum>\n" +
                "          </result>\n" +
                "        ]]>\n" +
                "      </xq-expression>\n" +
                "    </xquery>\n" +
                "  </def>\n" +
                "</config>";
        
        Variable result = executeAndGetVariable(configXml, "result");
        
        assertNotNull(result, "Result should not be null");
        String resultStr = result.toString().trim();
        System.out.println("Result: " + resultStr);
        
        assertTrue(resultStr.contains("<count>5</count>"), "Should have 5 numbers in sequence, got: " + resultStr);
        assertTrue(resultStr.contains("<first>3</first>"), "First number should be 3, got: " + resultStr);
        assertTrue(resultStr.contains("<sum>69</sum>"), "Sum should be 69 (3+7+14+18+27), got: " + resultStr);
        
        System.out.println("✅ Integer sequence parameter works correctly");
    }
    
    @Test(groups = {"integration", "xquery", "parameter-types", "sequences"})
    public void testDoubleSequenceParameter() throws Exception {
        System.out.println("\n### XQuery: Double sequence parameter (double*) ###");
        
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "  <def var=\"result\">\n" +
                "    <xquery>\n" +
                "      <xq-param name=\"prices\" type=\"double*\">\n" +
                "        10.5\n" +
                "        20.25\n" +
                "        15.75\n" +
                "      </xq-param>\n" +
                "      <xq-expression>\n" +
                "        <![CDATA[\n" +
                "          declare variable $prices as xs:double* external;\n" +
                "          <result>\n" +
                "            <count>{count($prices)}</count>\n" +
                "            <total>{sum($prices)}</total>\n" +
                "          </result>\n" +
                "        ]]>\n" +
                "      </xq-expression>\n" +
                "    </xquery>\n" +
                "  </def>\n" +
                "</config>";
        
        Variable result = executeAndGetVariable(configXml, "result");
        
        assertNotNull(result, "Result should not be null");
        String resultStr = result.toString().trim();
        System.out.println("Result: " + resultStr);
        
        assertTrue(resultStr.contains("<count>3</count>"), "Should have 3 prices in sequence, got: " + resultStr);
        assertTrue(resultStr.contains("<total>46.5</total>"), "Total should be 46.5, got: " + resultStr);
        
        System.out.println("✅ Double sequence parameter works correctly");
    }
    
    @Test(groups = {"integration", "xquery", "parameter-types", "node"})
    public void testNodeParameterWithoutTypeAttribute() throws Exception {
        System.out.println("\n### XQuery: Node parameter WITHOUT type attribute (auto-detect XML) ###");
        
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "  <def var=\"result\">\n" +
                "    <xquery>\n" +
                "      <xq-param name=\"doc\">\n" +  // NO type attribute!
                "        <![CDATA[\n" +
                "          <table>\n" +
                "            <tr><td>one</td><td>two</td></tr>\n" +
                "            <tr><td>three</td><td>four</td></tr>\n" +
                "          </table>\n" +
                "        ]]>\n" +
                "      </xq-param>\n" +
                "      <xq-expression>\n" +
                "        <![CDATA[\n" +
                "          declare variable $doc as node()* external;\n" +
                "          <result>\n" +
                "            <count>{count($doc//td)}</count>\n" +
                "            <first>{$doc//td[1]/text()}</first>\n" +
                "          </result>\n" +
                "        ]]>\n" +
                "      </xq-expression>\n" +
                "    </xquery>\n" +
                "  </def>\n" +
                "</config>";
        
        Variable result = executeAndGetVariable(configXml, "result");
        
        assertNotNull(result, "Result should not be null");
        String resultStr = result.toString().trim();
        System.out.println("Result: " + resultStr);
        
        assertTrue(resultStr.contains("<count>4</count>"), "Should have 4 td elements, got: " + resultStr);
        assertTrue(resultStr.contains("one"), "Should contain 'one' somewhere, got: " + resultStr);
        
        System.out.println("✅ Node parameter without type attribute works (auto-detected XML)");
    }
}

