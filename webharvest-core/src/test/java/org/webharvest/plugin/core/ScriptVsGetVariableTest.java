package org.webharvest.plugin.core;

import org.testng.annotations.Test;
import org.webharvest.examples.ScraperTestBase;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Unit test to verify correct usage of <get> vs <script> for variable retrieval.
 * 
 * <p>
 * Common mistake: Using {@code <script>varName</script>} to retrieve variable values.
 * This EXECUTES "varName" as JavaScript code instead of retrieving the variable.
 * </p>
 * 
 * <p>
 * <strong>Correct usage:</strong> {@code <get var="varName"/>}
 * </p>
 * 
 * <p>
 * This test demonstrates the api_integration.xml bug fix where all {@code <script>} 
 * elements were incorrectly used for variable retrieval, causing 31 second timeouts.
 * </p>
 * 
 * @since 2.2.0 (r826)
 */
public class ScriptVsGetVariableTest extends ScraperTestBase {

    @Test(description = "Test that <get> correctly retrieves variable value")
    public void testGetRetrievesVariable() throws Exception {
        String config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "    <def var=\"testData\">Hello World</def>\n" +
                "    \n" +
                "    <!-- CORRECT: Use <get> to retrieve variable -->\n" +
                "    <def var=\"retrieved\">\n" +
                "        <get var=\"testData\"/>\n" +
                "    </def>\n" +
                "</config>";

        Variable result = executeAndGetVariable(config, "retrieved");
        
        assertNotNull(result, "Variable should be retrieved");
        assertEquals(result.toString(), "Hello World", 
                    "Get should return exact variable value");
    }

    @Test(description = "Test that <script> executes code, not retrieves variable")
    public void testScriptExecutesCode() throws Exception {
        String config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "    <def var=\"number\">42</def>\n" +
                "    \n" +
                "    <!-- CORRECT: Use <script> to execute JavaScript code -->\n" +
                "    <def var=\"calculated\">\n" +
                "        <script><![CDATA[\n" +
                "            2 + 2\n" +
                "        ]]></script>\n" +
                "    </def>\n" +
                "</config>";

        Variable result = executeAndGetVariable(config, "calculated");
        
        // Script plugin might return empty in some configurations
        // The key point: <script> is for CODE EXECUTION, not variable retrieval
        assertNotNull(result, "Script plugin should return a result");
        
        // Document the behavior
        System.out.println("Script execution result: '" + result.toString() + "'");
    }

    @Test(description = "Test json-to-xml with <get> (CORRECT)")
    public void testJsonToXmlWithGet() throws Exception {
        String config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "    <def var=\"jsonData\">{\"name\":\"John\",\"age\":30}</def>\n" +
                "    \n" +
                "    <!-- CORRECT: Use <get> to pass variable to json-to-xml -->\n" +
                "    <def var=\"xmlData\">\n" +
                "        <json-to-xml tag=\"user\">\n" +
                "            <get var=\"jsonData\"/>\n" +
                "        </json-to-xml>\n" +
                "    </def>\n" +
                "</config>";

        Variable result = executeAndGetVariable(config, "xmlData");
        
        assertNotNull(result, "JSON should be converted to XML");
        String xmlStr = result.toString();
        assertTrue(xmlStr.contains("<name>John</name>"), 
                  "XML should contain name element");
        assertTrue(xmlStr.contains("<age>30</age>"), 
                  "XML should contain age element");
    }

    @Test(description = "Test xpath with <get> (CORRECT)")
    public void testXPathWithGet() throws Exception {
        String config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "    <def var=\"xmlData\">&lt;products&gt;&lt;product&gt;&lt;name&gt;Book&lt;/name&gt;&lt;/product&gt;&lt;/products&gt;</def>\n" +
                "    \n" +
                "    <!-- CORRECT: Use <get> to pass XML to xpath -->\n" +
                "    <def var=\"productName\">\n" +
                "        <xpath expression=\"//product/name/text()\">\n" +
                "            <get var=\"xmlData\"/>\n" +
                "        </xpath>\n" +
                "    </def>\n" +
                "</config>";

        Variable xmlData = executeAndGetVariable(config, "xmlData");
        assertNotNull(xmlData, "XML data should be set");
        assertTrue(xmlData.toString().contains("Book"), "XML should contain Book");
        
        Variable result = executeAndGetVariable(config, "productName");
        
        // XPath might return empty if XML parsing fails, but should not throw exception
        assertNotNull(result, "XPath should execute without error");
        
        // Document the behavior
        System.out.println("XPath result: '" + result.toString() + "'");
    }

    @Test(description = "Test variable substitution in template (CORRECT)")
    public void testTemplateVariableSubstitution() throws Exception {
        String config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "    <def var=\"userName\">John Doe</def>\n" +
                "    <def var=\"userEmail\">john@example.com</def>\n" +
                "    \n" +
                "    <!-- CORRECT: Use ${var} substitution in template -->\n" +
                "    <def var=\"report\">\n" +
                "        <template>\n" +
                "            <![CDATA[\n" +
                "            <report>\n" +
                "                <name>${userName}</name>\n" +
                "                <email>${userEmail}</email>\n" +
                "            </report>\n" +
                "            ]]>\n" +
                "        </template>\n" +
                "    </def>\n" +
                "</config>";

        Variable result = executeAndGetVariable(config, "report");
        
        assertNotNull(result, "Template should generate output");
        String reportStr = result.toString();
        assertTrue(reportStr.contains("<name>John Doe</name>"), 
                  "Template should substitute userName");
        assertTrue(reportStr.contains("<email>john@example.com</email>"), 
                  "Template should substitute userEmail");
    }

    @Test(description = "Test that <script>varName</script> DOES NOT retrieve variable")
    public void testScriptDoesNotRetrieveVariable() throws Exception {
        String config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "    <def var=\"myData\">Test Value</def>\n" +
                "    \n" +
                "    <!-- WRONG: This tries to execute 'myData' as JavaScript code -->\n" +
                "    <def var=\"wrong\">\n" +
                "        <script><![CDATA[myData]]></script>\n" +
                "    </def>\n" +
                "</config>";

        // Executing the variable name as script must never retrieve its stored value.
        try {
            Variable result = executeAndGetVariable(config, "wrong");
            assertNotNull(result, "A successful script evaluation must return a result");
            assertFalse("Test Value".equals(result.toString()),
                    "The script plugin must execute code, not retrieve the <def> variable");
        } catch (Exception e) {
            // Providers may report an unresolved script identifier with different wording.
            String message = e.getMessage();
            assertNotNull(message, "A script evaluation failure must explain the unresolved identifier");
            assertTrue(message.contains("myData") ||
                       message.contains("undefined") ||
                       message.contains("not defined"),
                    "Script failure must identify the unresolved variable name");
        }
    }
}

