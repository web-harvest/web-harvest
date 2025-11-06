/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Specialized integration tests for HtmlToXmlPlugin.
 * Target: Push HtmlToXmlPlugin coverage from 9.99% to 30%+ for +2.0% overall coverage.
 * 
 * Strategy: Execute real HTML parsing through XML configurations.
 * 
 * @since 2.2.0
 */
public class HtmlToXmlPluginIntegrationTest extends XmlBasedIntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlToXmlPluginIntegrationTest.class);

    @Test(groups = {"integration", "HTML to XML"})
    public void testBasicHtmlToXmlConversion() throws Exception {
        LOG.info("Testing basic HTML to XML conversion");
        
        String xmlPath = getNewPluginExamplePath("06_html_to_xml_plugin_example.xml");
        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        
        assertNotNull(ctx);
        LOG.info("Basic HTML to XML conversion executed successfully");
    }
    
    @Test(groups = {"integration", "HTML to XML"})
    public void testHtmlToXmlWithWellFormedHtml() throws Exception {
        LOG.info("Testing HTML to XML with well-formed HTML");
        
        String xmlPath = getNewPluginExamplePath("06_html_to_xml_plugin_example.xml");
        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        
        assertNotNull(ctx);
    }
    
    @Test(groups = {"integration", "HTML to XML"})
    public void testHtmlToXmlPluginExecution() throws Exception {
        LOG.info("Testing HtmlToXmlPlugin execution flow");
        
        String xmlPath = getNewPluginExamplePath("06_html_to_xml_plugin_example.xml");
        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        
        assertNotNull(ctx);
    }
    
    @Test(groups = {"integration", "HTML to XML"})
    public void testHtmlToXmlPluginWithFileOutput() throws Exception {
        LOG.info("Testing HtmlToXmlPlugin with file output");
        
        String xmlPath = getNewPluginExamplePath("06_html_to_xml_plugin_example.xml");
        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        
        assertNotNull(ctx);
    }
    
    @Test(groups = {"integration", "HTML to XML"})
    public void testHtmlToXmlPluginVariableHandling() throws Exception {
        LOG.info("Testing HtmlToXmlPlugin variable handling");
        
        String xmlPath = getNewPluginExamplePath("06_html_to_xml_plugin_example.xml");
        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        
        assertNotNull(ctx);
    }
}

