/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.integration;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.webharvest.Harvest;
import org.webharvest.Harvester;
import org.webharvest.definition.ConfigSource;
import org.webharvest.definition.FileConfigSource;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.runtime.DynamicScopeContext;

import java.io.File;

import static org.testng.Assert.*;

/**
 * Base class for WebBrowser plugin integration tests using XML configurations.
 * 
 * @since 2.2.0
 */
public abstract class WebBrowserIntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(WebBrowserIntegrationTestBase.class);
    private static final String TEST_RESOURCES_BASE = "integration-tests/";
    
    protected Injector injector;
    protected Harvest harvest;

    @BeforeMethod
    public void setUp() {
        LOG.info("Setting up WebBrowser integration test environment");
        
        String workingDir = System.getProperty("user.dir");
        injector = Guice.createInjector(new ScraperModule(workingDir));
        harvest = injector.getInstance(Harvest.class);
        
        LOG.debug("WebBrowser integration test environment ready");
    }

    @AfterMethod
    public void tearDown() {
        LOG.debug("Tearing down WebBrowser integration test environment");
        injector = null;
        harvest = null;
    }
    
    protected String getExamplePath(String filename) {
        return TEST_RESOURCES_BASE + filename;
    }
    
    protected DynamicScopeContext executeXmlConfig(String xmlFilePath) throws Exception {
        LOG.info("Executing XML configuration: {}", xmlFilePath);

        java.net.URL resourceUrl = getClass().getClassLoader().getResource(xmlFilePath);
        File xmlFile;

        if (resourceUrl != null) {
            xmlFile = new File(resourceUrl.getFile());
            LOG.debug("Loaded XML from test resources: {}", xmlFile.getAbsolutePath());
        } else {
            xmlFile = new File(xmlFilePath);
            LOG.debug("Using absolute path: {}", xmlFile.getAbsolutePath());
        }

        assertNotNull(xmlFile, "XML file should exist: " + xmlFilePath);
        assertTrue(xmlFile.exists(), "XML file should exist: " + xmlFilePath);

        ConfigSource configSource = new FileConfigSource(xmlFile);

        final DynamicScopeContext[] resultContext = new DynamicScopeContext[1];

        org.webharvest.HarvestLoadCallback loadCallback = new org.webharvest.HarvestLoadCallback() {
            @Override
            public void onSuccess(java.util.List<org.webharvest.definition.IElementDef> elements) {
                LOG.debug("Configuration loaded with {} elements", elements.size());
            }
        };

        Harvester harvester = harvest.getHarvester(configSource, loadCallback);
        harvester.execute(new Harvester.ContextInitCallback() {
            @Override
            public void onSuccess(DynamicScopeContext context) {
                resultContext[0] = context;
                LOG.debug("XML configuration executed successfully");
            }
        });

        assertNotNull(resultContext[0], "Execution context should not be null");
        return resultContext[0];
    }
}

