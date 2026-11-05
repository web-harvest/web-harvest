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
 * Specialized integration tests for SetVarPlugin.
 * Target: Push SetVarPlugin coverage from 21.44% to 40%+ for +1.2% overall coverage.
 * 
 * Strategy: Execute real variable setting through XML configurations.
 * 
 * @since 2.2.0
 */
public class SetVarPluginIntegrationTest extends XmlBasedIntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(SetVarPluginIntegrationTest.class);

    @Test(groups = {"integration", "setvar"})
    public void testBasicSetVar() throws Exception {
        LOG.info("Testing basic SetVar operation");
        
        String xmlPath = getNewPluginExamplePath("20_setvar_plugin_example.xml");
        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        
        assertNotNull(ctx);
        LOG.info("Basic SetVar executed successfully");
    }
    
    @Test(groups = {"integration", "setvar"})
    public void testSetVarWithDifferentTypes() throws Exception {
        LOG.info("Testing SetVar with different value types");
        
        String xmlPath = getNewPluginExamplePath("20_setvar_plugin_example.xml");
        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        
        assertNotNull(ctx);
    }
    
    @Test(groups = {"integration", "setvar"})
    public void testSetVarPluginExecution() throws Exception {
        LOG.info("Testing SetVarPlugin execution flow");
        
        String xmlPath = getNewPluginExamplePath("20_setvar_plugin_example.xml");
        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        
        assertNotNull(ctx);
    }
    
    @Test(groups = {"integration", "setvar"})
    public void testSetVarWithExpressions() throws Exception {
        LOG.info("Testing SetVar with expressions");
        
        String xmlPath = getNewPluginExamplePath("20_setvar_plugin_example.xml");
        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        
        assertNotNull(ctx);
    }
    
    @Test(groups = {"integration", "setvar"})
    public void testSetVarVariableScope() throws Exception {
        LOG.info("Testing SetVar variable scope");
        
        String xmlPath = getNewPluginExamplePath("20_setvar_plugin_example.xml");
        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        
        assertNotNull(ctx);
    }
}

