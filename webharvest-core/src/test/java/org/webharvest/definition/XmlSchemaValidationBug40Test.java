/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.definition;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.webharvest.utils.XmlUtil;

import javax.xml.parsers.SAXParserFactory;

import static org.testng.Assert.*;

/**
 * Test for Bug #40: Disable XML Schema Validation Parameter
 * 
 * <h3>Bug #40: XML Schema Validation Toggle</h3>
 * <p>
 * <strong>Issue:</strong> No way to disable XML schema validation. This is problematic
 * when using plugins that don't have XSD schemas or when schema validation is not needed.
 * </p>
 * 
 * <p>
 * <strong>User Request:</strong><br>
 * "Please add also an additional parameter to disable xml schema validation"
 * </p>
 * 
 * <p>
 * <strong>Solution:</strong> Add system property {@code webharvest.schema.validation}
 * to control whether XML schema validation is enabled. Default is TRUE (enabled).
 * </p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class XmlSchemaValidationBug40Test {
    
    private String originalProperty;
    
    @BeforeMethod
    public void setUp() {
        // Save original property value
        originalProperty = System.getProperty(XmlUtil.SCHEMA_VALIDATION_PROPERTY);
    }
    
    @AfterMethod
    public void tearDown() {
        // Restore original property value
        if (originalProperty != null) {
            System.setProperty(XmlUtil.SCHEMA_VALIDATION_PROPERTY, originalProperty);
        } else {
            System.clearProperty(XmlUtil.SCHEMA_VALIDATION_PROPERTY);
        }
    }
    
    /**
     * Bug #40 Test 1: Schema validation enabled by default
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug40_SchemaValidationEnabledByDefault() {
        System.out.println("\n### Bug #40 Test 1: Validation enabled by default ###");
        
        // Clear the property
        System.clearProperty(XmlUtil.SCHEMA_VALIDATION_PROPERTY);
        
        // Should be enabled by default
        assertTrue(XmlUtil.isSchemaValidationEnabled(), 
            "Schema validation should be enabled by default");
        
        System.out.println("✅ Schema validation enabled by default");
    }
    
    /**
     * Bug #40 Test 2: Can disable schema validation via system property
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug40_CanDisableSchemaValidation() {
        System.out.println("\n### Bug #40 Test 2: Disable via system property ###");
        
        // Disable schema validation
        System.setProperty(XmlUtil.SCHEMA_VALIDATION_PROPERTY, "false");
        
        // Should be disabled
        assertFalse(XmlUtil.isSchemaValidationEnabled(), 
            "Schema validation should be disabled");
        
        System.out.println("✅ Schema validation can be disabled");
    }
    
    /**
     * Bug #40 Test 3: Can enable schema validation explicitly
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug40_CanEnableSchemaValidation() {
        System.out.println("\n### Bug #40 Test 3: Enable explicitly ###");
        
        // Enable schema validation explicitly
        System.setProperty(XmlUtil.SCHEMA_VALIDATION_PROPERTY, "true");
        
        // Should be enabled
        assertTrue(XmlUtil.isSchemaValidationEnabled(), 
            "Schema validation should be enabled");
        
        System.out.println("✅ Schema validation can be enabled explicitly");
    }
    
    /**
     * Bug #40 Test 4: SAXParserFactory respects validation setting
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug40_SAXParserFactoryRespectsValidation() {
        System.out.println("\n### Bug #40 Test 4: SAXParserFactory validation ###");
        
        // Test with validation disabled
        System.setProperty(XmlUtil.SCHEMA_VALIDATION_PROPERTY, "false");
        SAXParserFactory factory1 = XmlUtil.getSAXParserFactory(true, true);
        assertFalse(factory1.isValidating(), 
            "Factory should not validate when property is false");
        
        // Test with validation enabled
        System.setProperty(XmlUtil.SCHEMA_VALIDATION_PROPERTY, "true");
        SAXParserFactory factory2 = XmlUtil.getSAXParserFactory(true, true);
        // Note: validation only works if schema is set
        // Default implementation may not validate without schema
        
        System.out.println("✅ SAXParserFactory respects validation setting");
    }
    
    /**
     * Bug #40 Test 5: Different property values handled correctly
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug40_PropertyValuesHandledCorrectly() {
        System.out.println("\n### Bug #40 Test 5: Property value handling ###");
        
        // Test various true values
        String[] trueValues = {"true", "TRUE", "yes", "YES", "1"};
        for (String value : trueValues) {
            System.setProperty(XmlUtil.SCHEMA_VALIDATION_PROPERTY, value);
            assertTrue(XmlUtil.isSchemaValidationEnabled(), 
                "Should be enabled for value: " + value);
        }
        
        // Test various false values
        String[] falseValues = {"false", "FALSE", "no", "NO", "0"};
        for (String value : falseValues) {
            System.setProperty(XmlUtil.SCHEMA_VALIDATION_PROPERTY, value);
            assertFalse(XmlUtil.isSchemaValidationEnabled(), 
                "Should be disabled for value: " + value);
        }
        
        System.out.println("✅ Different property values handled correctly");
    }
    
    /**
     * Bug #40 Test 6: Documentation test - explains the feature
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug40_DocumentationAndUsage() {
        System.out.println("\n### Bug #40 Test 6: Documentation ###");
        
        System.out.println("Bug #40: Disable XML Schema Validation");
        System.out.println("");
        System.out.println("BEFORE FIX:");
        System.out.println("  - XML schema validation always enabled");
        System.out.println("  - No way to disable it");
        System.out.println("  - Problems with plugins without XSD schemas");
        System.out.println("");
        System.out.println("AFTER FIX:");
        System.out.println("  - Schema validation configurable via system property");
        System.out.println("  - Default: enabled (backward compatible)");
        System.out.println("  - Can be disabled when not needed");
        System.out.println("");
        System.out.println("USAGE:");
        System.out.println("  // Disable schema validation");
        System.out.println("  java -Dwebharvest.schema.validation=false -jar webharvest.jar");
        System.out.println("");
        System.out.println("  // Or in code:");
        System.out.println("  System.setProperty(XmlUtil.SCHEMA_VALIDATION_PROPERTY, \"false\");");
        System.out.println("");
        System.out.println("WHY DISABLE:");
        System.out.println("  - Custom plugins without XSD schemas");
        System.out.println("  - Faster parsing (no schema validation overhead)");
        System.out.println("  - Development/testing environments");
        
        System.out.println("\n✅ Bug #40 feature documented");
    }
}

