package org.webharvest.examples;

import org.testng.annotations.Test;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Integration test for XQuery with node() parameter.
 * Tests the exact scenario from xquery_demo.xml example.
 */
public class XQueryNodeParameterTest extends ScraperTestBase {

    @Test(description = "Test XQuery with node() parameter from variable")
    public void testXQueryWithNodeParameter() throws Exception {
        String config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
                "    <!-- Create sample product XML -->\n" +
                "    <def var=\"productsXml\">\n" +
                "        <template><![CDATA[<products>\n" +
                "    <product>\n" +
                "        <name>Book A</name>\n" +
                "        <price>25.50</price>\n" +
                "        <stock>15</stock>\n" +
                "    </product>\n" +
                "    <product>\n" +
                "        <name>Book B</name>\n" +
                "        <price>18.99</price>\n" +
                "        <stock>8</stock>\n" +
                "    </product>\n" +
                "    <product>\n" +
                "        <name>Book C</name>\n" +
                "        <price>32.75</price>\n" +
                "        <stock>22</stock>\n" +
                "    </product>\n" +
                "</products>]]></template>\n" +
                "    </def>\n" +
                "    \n" +
                "    <!-- Process with XQuery -->\n" +
                "    <def var=\"analysis\">\n" +
                "        <xquery>\n" +
                "            <xq-param name=\"data\" type=\"node()\">\n" +
                "                <get var=\"productsXml\"/>\n" +
                "            </xq-param>\n" +
                "            <xq-expression>\n" +
                "                <![CDATA[\n" +
                "                declare variable $data as node() external;\n" +
                "                \n" +
                "                let $count := count($data//product)\n" +
                "                let $totalValue := sum($data//product/price)\n" +
                "                let $avgPrice := round($totalValue div $count * 100) div 100\n" +
                "                let $totalStock := sum($data//product/stock)\n" +
                "                \n" +
                "                return\n" +
                "                <analysis>\n" +
                "                    <total_products>{$count}</total_products>\n" +
                "                    <total_value>{$totalValue}</total_value>\n" +
                "                    <average_price>{$avgPrice}</average_price>\n" +
                "                    <total_stock>{$totalStock}</total_stock>\n" +
                "                </analysis>\n" +
                "                ]]>\n" +
                "            </xq-expression>\n" +
                "        </xquery>\n" +
                "    </def>\n" +
                "    \n" +
                "    <!-- Extract analysis results -->\n" +
                "    <def var=\"totalProducts\">\n" +
                "        <xpath expression=\"//total_products/text()\">\n" +
                "            <get var=\"analysis\"/>\n" +
                "        </xpath>\n" +
                "    </def>\n" +
                "    \n" +
                "    <def var=\"totalValue\">\n" +
                "        <xpath expression=\"//total_value/text()\">\n" +
                "            <get var=\"analysis\"/>\n" +
                "        </xpath>\n" +
                "    </def>\n" +
                "</config>";

        // Execute configuration
        executeXmlString(config);
        
        // Verify productsXml was created
        Variable productsXml = executeAndGetVariable(config, "productsXml");
        assertNotNull(productsXml, "productsXml should not be null");
        String productsStr = productsXml.toString();
        assertTrue(productsStr.contains("Book A"), "productsXml should contain Book A");
        assertTrue(productsStr.contains("25.50"), "productsXml should contain price 25.50");
        
        // Verify analysis was created
        Variable analysis = executeAndGetVariable(config, "analysis");
        assertNotNull(analysis, "analysis should not be null");
        String analysisStr = analysis.toString();
        System.out.println("Analysis result: " + analysisStr);
        assertTrue(!analysisStr.isEmpty(), "analysis should not be empty");
        assertTrue(analysisStr.contains("total_products"), "analysis should contain total_products element");
        
        // Verify extracted values
        Variable totalProducts = executeAndGetVariable(config, "totalProducts");
        assertNotNull(totalProducts, "totalProducts should not be null");
        assertEquals(totalProducts.toString().trim(), "3", "totalProducts should be 3");
        
        Variable totalValue = executeAndGetVariable(config, "totalValue");
        assertNotNull(totalValue, "totalValue should not be null");
        assertEquals(totalValue.toString().trim(), "77.24", "totalValue should be 77.24");
    }
}

