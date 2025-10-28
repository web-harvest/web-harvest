/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.testng.annotations.Test;
import org.webharvest.plugin.PluginMetadata;

import static org.testng.Assert.*;

/**
 * TDD Test for proper plugin categorization.
 * 
 * <p>
 * Categories should be semantic and match Visual Designer palette:
 * - Network: http, ftp (external), mail (external)
 * - Extraction: xpath, xquery, regexp, html-to-xml, json-to-xml
 * - Variables: def, get-var
 * - Control: loop, while, if, try, catch, case
 * - Processing: script, template, xslt
 * - Storage: file
 * - Utilities: log, sleep, return, exit
 * </p>
 * 
 * @author Robert Bala
 * @since 2.3.1
 */
public class PluginCategoriesTest {
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testHttpPluginCategory() {
        HttpPlugin plugin = new HttpPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Network",
            "HTTP plugin should be in 'Network' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testXPathPluginCategory() {
        XPathPlugin plugin = new XPathPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Extraction",
            "XPath plugin should be in 'Extraction' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testXQueryPluginCategory() {
        XQueryPlugin plugin = new XQueryPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Extraction",
            "XQuery plugin should be in 'Extraction' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testRegexpPluginCategory() {
        RegexpPlugin plugin = new RegexpPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Extraction",
            "Regexp plugin should be in 'Extraction' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testHtmlToXmlPluginCategory() {
        HtmlToXmlPlugin plugin = new HtmlToXmlPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Extraction",
            "HtmlToXml plugin should be in 'Extraction' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testJsonToXmlPluginCategory() {
        JsonToXmlPlugin plugin = new JsonToXmlPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Extraction",
            "JsonToXml plugin should be in 'Extraction' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testDefPluginCategory() {
        DefPlugin plugin = new DefPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Variables",
            "Def plugin should be in 'Variables' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testGetVarPluginCategory() {
        GetVarPlugin plugin = new GetVarPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Variables",
            "GetVar plugin should be in 'Variables' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testLoopPluginCategory() {
        LoopPlugin plugin = new LoopPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Control",
            "Loop plugin should be in 'Control' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testWhilePluginCategory() {
        WhilePlugin plugin = new WhilePlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Control",
            "While plugin should be in 'Control' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testIfPluginCategory() {
        IfPlugin plugin = new IfPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Control",
            "If plugin should be in 'Control' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testTryPluginCategory() {
        TryPlugin plugin = new TryPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Control",
            "Try plugin should be in 'Control' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testCasePluginCategory() {
        CasePlugin plugin = new CasePlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Control",
            "Case plugin should be in 'Control' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testScriptPluginCategory() {
        ScriptPlugin plugin = new ScriptPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Processing",
            "Script plugin should be in 'Processing' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testTemplatePluginCategory() {
        TemplatePlugin plugin = new TemplatePlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Processing",
            "Template plugin should be in 'Processing' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testXsltPluginCategory() {
        XsltPlugin plugin = new XsltPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Processing",
            "XSLT plugin should be in 'Processing' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testFilePluginCategory() {
        FilePlugin plugin = new FilePlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Storage",
            "File plugin should be in 'Storage' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testLogPluginCategory() {
        LogPlugin plugin = new LogPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Utilities",
            "Log plugin should be in 'Utilities' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testSleepPluginCategory() {
        SleepPlugin plugin = new SleepPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Utilities",
            "Sleep plugin should be in 'Utilities' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testReturnPluginCategory() {
        ReturnPlugin plugin = new ReturnPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Utilities",
            "Return plugin should be in 'Utilities' category");
    }
    
    @Test(groups = {"unit", "metadata", "categories"})
    public void testExitPluginCategory() {
        ExitPlugin plugin = new ExitPlugin();
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getCategory(), "Utilities",
            "Exit plugin should be in 'Utilities' category");
    }
}

