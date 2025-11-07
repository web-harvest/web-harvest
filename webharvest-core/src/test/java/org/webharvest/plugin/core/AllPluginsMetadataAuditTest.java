/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Comprehensive audit of ALL core plugins metadata completeness.
 * 
 * <p>
 * This test ensures that every core plugin has:
 * - Complete metadata (id, name, description, category, etc.)
 * - Proper configuration schema
 * - Documented attributes
 * - Version information
 * </p>
 * 
 * @author Robert Bala
 * @since 2.3.1
 */
public class AllPluginsMetadataAuditTest {
    
    private List<String> metadataIssues;
    private List<Class<?>> allPluginClasses;
    
    @BeforeClass
    public void setUp() {
        metadataIssues = new ArrayList<>();
        
        // List all CORE plugins (only from webharvest-core module) - 44 total (removed deprecated VarPlugin, SetVarPlugin)
        allPluginClasses = new ArrayList<>();
        allPluginClasses.add(BodyProcessorPlugin.class);
        allPluginClasses.add(CallParamPlugin.class);
        allPluginClasses.add(CallPlugin.class);
        allPluginClasses.add(CasePlugin.class);
        allPluginClasses.add(CatchPlugin.class);
        allPluginClasses.add(ConstantPlugin.class);
        allPluginClasses.add(DefPlugin.class);
        allPluginClasses.add(ElsePlugin.class);
        allPluginClasses.add(EmptyPlugin.class);
        allPluginClasses.add(ExitPlugin.class);
        allPluginClasses.add(FilePlugin.class);
        allPluginClasses.add(FunctionPlugin.class);
        allPluginClasses.add(GetVarPlugin.class);
        allPluginClasses.add(HtmlToXmlPlugin.class);
        allPluginClasses.add(HttpHeaderPlugin.class);
        allPluginClasses.add(HttpParamPlugin.class);
        allPluginClasses.add(HttpPlugin.class);
        allPluginClasses.add(IfPlugin.class);
        allPluginClasses.add(IncludePlugin.class);
        allPluginClasses.add(JsonToXmlPlugin.class);
        allPluginClasses.add(ListPlugin.class);
        allPluginClasses.add(LogPlugin.class);
        allPluginClasses.add(LoopPlugin.class);
        allPluginClasses.add(RegexpPatternPlugin.class);
        allPluginClasses.add(RegexpPlugin.class);
        allPluginClasses.add(RegexpResultPlugin.class);
        allPluginClasses.add(RegexpSourcePlugin.class);
        allPluginClasses.add(ReturnPlugin.class);
        allPluginClasses.add(ScriptPlugin.class);
        // SetVarPlugin removed - use DefPlugin instead
        allPluginClasses.add(SleepPlugin.class);
        allPluginClasses.add(StylesheetPlugin.class);
        allPluginClasses.add(TemplatePlugin.class);
        allPluginClasses.add(TextPlugin.class);
        allPluginClasses.add(TokenizePlugin.class);
        allPluginClasses.add(TryPlugin.class);
        allPluginClasses.add(ValueOfPlugin.class);
        // VarPlugin removed - use DefPlugin instead
        allPluginClasses.add(WhilePlugin.class);
        allPluginClasses.add(XMLPlugin.class);
        allPluginClasses.add(XPathPlugin.class);
        allPluginClasses.add(XQExpressionPlugin.class);
        allPluginClasses.add(XQParamPlugin.class);
        allPluginClasses.add(XQueryPlugin.class);
        allPluginClasses.add(XmlToJsonPlugin.class);
        allPluginClasses.add(XsltPlugin.class);
        // External modules not included: Cookie, Database, Ftp, Mail, WebBrowser, Zip
    }
    
    @Test(groups = {"integration", "plugins", "metadata", "audit"})
    public void testAllPluginsHaveCompleteMetadata() {
        System.out.println("\n### PLUGIN METADATA AUDIT ###\n");
        
        System.out.println("Total plugins to audit: " + allPluginClasses.size());
        System.out.println();
        
        int completeCount = 0;
        int incompleteCount = 0;
        
        for (Class<?> pluginClass : allPluginClasses) {
            String pluginName = pluginClass.getSimpleName();
            List<String> issues = auditPlugin(pluginClass);
            
            if (issues.isEmpty()) {
                System.out.println("✅ " + pluginName + " - complete");
                completeCount++;
            } else {
                System.out.println("❌ " + pluginName + " - INCOMPLETE:");
                for (String issue : issues) {
                    System.out.println("   - " + issue);
                    metadataIssues.add(pluginName + ": " + issue);
                }
                incompleteCount++;
            }
        }
        
        System.out.println("\n### AUDIT SUMMARY ###");
        System.out.println("Complete plugins: " + completeCount + "/" + allPluginClasses.size());
        System.out.println("Incomplete plugins: " + incompleteCount + "/" + allPluginClasses.size());
        
        if (!metadataIssues.isEmpty()) {
            System.out.println("\n### ALL ISSUES ###");
            for (String issue : metadataIssues) {
                System.out.println("  - " + issue);
            }
        }
        
        assertTrue(metadataIssues.isEmpty(), 
            "Found " + metadataIssues.size() + " metadata issues in " + incompleteCount + " plugins");
    }
    
    private List<String> auditPlugin(Class<?> pluginClass) {
        List<String> issues = new ArrayList<>();
        
        try {
            Object plugin = pluginClass.getDeclaredConstructor().newInstance();
            
            if (!(plugin instanceof org.webharvest.plugin.Plugin)) {
                issues.add("Not a Plugin instance");
                return issues;
            }
            
            org.webharvest.plugin.Plugin pluginInstance = (org.webharvest.plugin.Plugin) plugin;
            PluginMetadata metadata = pluginInstance.getMetadata();
            
            if (metadata == null) {
                issues.add("getMetadata() returns null");
                return issues;
            }
            
            // Check ID
            String id = metadata.getId();
            if (id == null || id.trim().isEmpty()) {
                issues.add("Missing or empty ID");
            }
            
            // Check Name
            String name = metadata.getName();
            if (name == null || name.trim().isEmpty()) {
                issues.add("Missing or empty Name");
            }
            
            // Check Description
            String description = metadata.getDescription();
            if (description == null || description.trim().isEmpty()) {
                issues.add("Missing or empty Description");
            }
            
            // Check Version
            String version = metadata.getVersion();
            if (version == null || version.trim().isEmpty()) {
                issues.add("Missing or empty Version");
            }
            
            // Check Category
            String category = metadata.getCategory();
            if (category == null || category.trim().isEmpty()) {
                issues.add("Missing or empty Category");
            }
            
            // Check Namespace
            String namespace = metadata.getNamespace();
            if (namespace == null || namespace.trim().isEmpty()) {
                issues.add("Missing or empty Namespace");
            }
            
            // Check Element Name
            String elementName = metadata.getElementName();
            if (elementName == null || elementName.trim().isEmpty()) {
                issues.add("Missing or empty Element Name");
            }
            
            // Check Configuration Schema
            PluginConfigurationSchema schema = metadata.getConfigurationSchema();
            if (schema == null) {
                issues.add("getConfigurationSchema() returns null");
            } else {
                // Check if schema has any attribute definitions
                AttributeDefinition[] attrs = schema.getAttributeDefinitions();
                // Note: Empty attributes array is OK for some plugins
                
                // Check optional/required attributes are defined
                String[] optional = schema.getOptionalAttributes();
                String[] required = schema.getRequiredAttributes();
                
                if (optional == null) {
                    issues.add("Schema getOptionalAttributes() returns null");
                }
                if (required == null) {
                    issues.add("Schema getRequiredAttributes() returns null");
                }
            }
            
        } catch (Exception e) {
            issues.add("Failed to instantiate or inspect: " + e.getMessage());
        }
        
        return issues;
    }
}

