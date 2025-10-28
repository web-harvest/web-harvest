package org.webharvest.examples;

import org.testng.annotations.Test;
import org.webharvest.runtime.variables.Variable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.*;

/**
 * Integration test for XQuery with complex parameter types.
 * 
 * <p>
 * This test reproduces the EXACT example from IDE that is currently failing:
 * - boolean parameter
 * - double parameter  
 * - integer* (sequence) parameter with multi-line values
 * - string parameter
 * - node()* (node sequence) parameter with CDATA XML
 * </p>
 * 
 * <p>
 * Current Issue: Parameters are passed as strings instead of proper types,
 * causing Saxon to throw type conversion errors:
 * - "Required type is xs:boolean; supplied value has type xs:string"
 * - "Cannot promote non-numeric value to xs:integer"
 * </p>
 * 
 * @author Robert Bala
 * @since 2.3.1
 */
public class XQueryComplexTypesIntegrationTest extends ScraperTestBase {
    
    @Test(groups = {"integration", "xquery", "complex-types"}, 
          description = "Test XQuery with all parameter types from IDE example")
    public void testXQueryComplexExample_AllParameterTypes() throws Exception {
        System.out.println("\n### XQuery: Complex example with all parameter types (IDE example) ###");
        
        // This is EXACT XML from IDE xquery.xml example
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
                "    <file action=\"write\" path=\"target/test-output/xquerytest.xml\">\n" +
                "        <xquery>\n" +
                "            <xq-param name=\"logicvalue\" type=\"boolean\">true</xq-param>\n" +
                "            <xq-param name=\"numbervalue\" type=\"double\">12</xq-param>\n" +
                "            <xq-param name=\"intseq\" type=\"integer*\">\n" +
                "                3\n" +
                "                7\n" +
                "                14\n" +
                "                18\n" +
                "                27\n" +
                "            </xq-param>\n" +
                "            <xq-param name=\"stringvalue\" type=\"string\">ABCDEFGH123456</xq-param>\n" +
                "            <xq-param name=\"doc\">\n" +
                "                <![CDATA[\n" +
                "               <table>\n" +
                "                    <tr>\n" +
                "                        <td>one</td>\n" +
                "                        <td>two</td>\n" +
                "                        <td>tree</td>\n" +
                "                    </tr>\n" +
                "                    <tr>\n" +
                "                        <td>four</td>\n" +
                "                        <td>five</td>\n" +
                "                        <td>six</td>\n" +
                "                    </tr>\n" +
                "               </table>\n" +
                "            ]]>\n" +
                "            </xq-param>\n" +
                "            <xq-expression>\n" +
                "                <![CDATA[\n" +
                "                (: example of external namespace and usage of Java function library inside the XQuery :)\n" +
                "                declare namespace math =\"java:java.lang.Math\";\n" +
                "\n" +
                "                (: declare namespace for user-defined functions :)\n" +
                "                declare namespace fn =\"f:ns\";\n" +
                "\n" +
                "                (: declaration of external variables with names and types matching to those in xq-param :)\n" +
                "                declare variable $logicvalue as xs:boolean external;\n" +
                "                declare variable $numbervalue as xs:double external;\n" +
                "                declare variable $intseq as xs:integer* external;\n" +
                "                declare variable $stringvalue as xs:string external;\n" +
                "                declare variable $doc as node()* external;\n" +
                "\n" +
                "                (: user-defined function inside XQuery :)\n" +
                "                declare function fn:myFunc($items as xs:integer*) as node()* {\n" +
                "                    for $num in $items return\n" +
                "                        <value>\n" +
                "                            { math:sqrt($num) + 1 }\n" +
                "                        </value>\n" +
                "                };\n" +
                "\n" +
                "                (: resuting XML :)\n" +
                "                <test>\n" +
                "                    <logictest>\n" +
                "                        { if ($logicvalue) then 1 else 2 },\n" +
                "                        { $logicvalue eq ($numbervalue gt 15) }\n" +
                "                    </logictest>\n" +
                "                    <doubletest>\n" +
                "                        { $numbervalue * 2 + 10 },\n" +
                "                        { $numbervalue instance of xs:float },\n" +
                "                        { round($numbervalue) }\n" +
                "                    </doubletest>\n" +
                "                    <stringtest>\n" +
                "                        { concat($stringvalue, $logicvalue, $numbervalue) }\n" +
                "                    </stringtest>\n" +
                "                    <numberseqtest>\n" +
                "                        { fn:myFunc($intseq) }\n" +
                "                        { concat($intseq[1], \"mama\") }\n" +
                "                    </numberseqtest>\n" +
                "                    <doctest>\n" +
                "                        { for $td in $doc//td return $td }\n" +
                "                    </doctest>\n" +
                "                </test>\n" +
                "            ]]>\n" +
                "            </xq-expression>\n" +
                "        </xquery>\n" +
                "    </file>\n" +
                "</config>";
        
        // Execute configuration
        executeXmlString(configXml);
        
        // Verify output file was created
        Path outputPath = Paths.get("target/test-output/xquerytest.xml");
        assertTrue(Files.exists(outputPath), "Output file should be created");
        
        // Read and verify content
        String content = new String(Files.readAllBytes(outputPath));
        System.out.println("XQuery result length: " + content.length() + " bytes");
        System.out.println("XQuery result preview:\n" + content.substring(0, Math.min(500, content.length())));
        
        // Verify results
        assertFalse(content.trim().isEmpty(), "Result should not be empty");
        
        // Test 1: logictest - boolean parameter
        assertTrue(content.contains("<logictest>"), "Should contain logictest element");
        assertTrue(content.contains("1"), "logicvalue=true should produce 1 (not 2)");
        assertTrue(content.contains("false"), "logicvalue eq (numbervalue gt 15) should be false (12 is not > 15)");
        
        // Test 2: doubletest - double parameter
        assertTrue(content.contains("<doubletest>"), "Should contain doubletest element");
        assertTrue(content.contains("34"), "12 * 2 + 10 = 34");
        assertTrue(content.contains("12"), "round(12) = 12");
        
        // Test 3: stringtest - string concatenation
        assertTrue(content.contains("<stringtest>"), "Should contain stringtest element");
        assertTrue(content.contains("ABCDEFGH123456"), "Should contain string parameter value");
        assertTrue(content.contains("true"), "Should contain boolean in concat");
        assertTrue(content.contains("12"), "Should contain number in concat");
        
        // Test 4: numberseqtest - integer* sequence with function call
        assertTrue(content.contains("<numberseqtest>"), "Should contain numberseqtest element");
        assertTrue(content.contains("<value>"), "Should have value elements from fn:myFunc");
        assertTrue(content.contains("3mama"), "concat(intseq[1], 'mama') should be '3mama'");
        
        // Test 5: doctest - node()* parameter
        assertTrue(content.contains("<doctest>"), "Should contain doctest element");
        assertTrue(content.contains("<td>"), "Should have td elements from doc parameter");
        assertTrue(content.contains("one"), "Should contain 'one' from table");
        assertTrue(content.contains("six"), "Should contain 'six' from table");
        
        System.out.println("✅ XQuery complex example with ALL parameter types works correctly!");
        
        // Cleanup
        Files.deleteIfExists(outputPath);
    }
    
    @Test(groups = {"integration", "xquery", "sequence-types"}, 
          description = "Test integer sequence parameter specifically")
    public void testIntegerSequenceParameter() throws Exception {
        System.out.println("\n### XQuery: Integer sequence (integer*) parameter ###");
        
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "  <def var=\"result\">\n" +
                "    <xquery>\n" +
                "      <xq-param name=\"numbers\" type=\"integer*\">\n" +
                "        10\n" +
                "        20\n" +
                "        30\n" +
                "      </xq-param>\n" +
                "      <xq-expression>\n" +
                "        <![CDATA[\n" +
                "          declare variable $numbers as xs:integer* external;\n" +
                "          <result>\n" +
                "            <count>{count($numbers)}</count>\n" +
                "            <sum>{sum($numbers)}</sum>\n" +
                "            <first>{$numbers[1]}</first>\n" +
                "          </result>\n" +
                "        ]]>\n" +
                "      </xq-expression>\n" +
                "    </xquery>\n" +
                "  </def>\n" +
                "</config>";
        
        Variable result = executeAndGetVariable(configXml, "result");
        
        assertNotNull(result, "Result should not be null");
        String resultStr = result.toString();
        System.out.println("Integer sequence result: " + resultStr);
        
        assertTrue(resultStr.contains("<count>3</count>"), "Should have 3 numbers");
        assertTrue(resultStr.contains("<sum>60</sum>"), "Sum should be 60 (10+20+30)");
        assertTrue(resultStr.contains("<first>10</first>"), "First should be 10");
        
        System.out.println("✅ Integer sequence parameter works correctly");
    }
    
    @Test(groups = {"integration", "xquery", "node-sequence"}, 
          description = "Test node sequence parameter with CDATA")
    public void testNodeSequenceParameterWithCDATA() throws Exception {
        System.out.println("\n### XQuery: Node sequence (node()*) parameter with CDATA ###");
        
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "  <def var=\"result\">\n" +
                "    <xquery>\n" +
                "      <xq-param name=\"data\">\n" +
                "        <![CDATA[\n" +
                "          <items>\n" +
                "            <item>A</item>\n" +
                "            <item>B</item>\n" +
                "            <item>C</item>\n" +
                "          </items>\n" +
                "        ]]>\n" +
                "      </xq-param>\n" +
                "      <xq-expression>\n" +
                "        <![CDATA[\n" +
                "          declare variable $data as node()* external;\n" +
                "          <result>\n" +
                "            <count>{count($data//item)}</count>\n" +
                "            <items>{for $i in $data//item return $i}</items>\n" +
                "          </result>\n" +
                "        ]]>\n" +
                "      </xq-expression>\n" +
                "    </xquery>\n" +
                "  </def>\n" +
                "</config>";
        
        Variable result = executeAndGetVariable(configXml, "result");
        
        assertNotNull(result, "Result should not be null");
        String resultStr = result.toString();
        System.out.println("Node sequence result: " + resultStr);
        
        assertTrue(resultStr.contains("<count>3</count>"), "Should have 3 items");
        assertTrue(resultStr.contains("<item>A</item>"), "Should contain item A");
        assertTrue(resultStr.contains("<item>B</item>"), "Should contain item B");
        assertTrue(resultStr.contains("<item>C</item>"), "Should contain item C");
        
        System.out.println("✅ Node sequence parameter with CDATA works correctly");
    }
}


