/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;

import static org.testng.Assert.*;

/**
 * Integration tests for WebBrowser plugin using XML configuration examples.
 * 
 * @since 2.2.0
 */
public class WebBrowserPluginIntegrationTest extends WebBrowserIntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(WebBrowserPluginIntegrationTest.class);

    @Test(groups = {"integration", "webbrowser"})
    public void testWebBrowserPlugin() throws Exception {
        LOG.info("Testing WebBrowserPlugin with XML example");
        
        String xmlPath = getExamplePath("webbrowser_plugin_example.xml");
        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        
        assertNotNull(ctx);
        LOG.info("WebBrowserPlugin test executed successfully");
    }
}

