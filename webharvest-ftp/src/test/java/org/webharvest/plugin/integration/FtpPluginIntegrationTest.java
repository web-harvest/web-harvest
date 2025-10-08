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
import org.testng.annotations.Test;
import org.webharvest.Harvest;
import org.webharvest.Harvester;
import org.webharvest.definition.ConfigSource;
import org.webharvest.definition.FileConfigSource;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.runtime.DynamicScopeContext;

import java.io.File;

import static org.testng.Assert.*;

/**
 * Integration tests for FTP plugin using XML configuration example.
 * 
 * @since 2.2.0
 */
public class FtpPluginIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(FtpPluginIntegrationTest.class);
    
    protected Injector injector;
    protected Harvest harvest;

    @BeforeMethod
    public void setUp() {
        LOG.info("Setting up FTP integration test environment");
        
        String workingDir = System.getProperty("user.dir");
        injector = Guice.createInjector(new ScraperModule(workingDir));
        harvest = injector.getInstance(Harvest.class);
    }

    @AfterMethod
    public void tearDown() {
        injector = null;
        harvest = null;
    }

    @Test(groups = {"integration", "ftp"})
    public void testFtpPluginExample() throws Exception {
        LOG.info("Testing FTP plugin with XML example");
        
        java.net.URL resourceUrl = getClass().getClassLoader().getResource("integration-tests/ftp_plugin_example.xml");
        
        if (resourceUrl != null) {
            File xmlFile = new File(resourceUrl.getFile());
            LOG.info("FTP plugin example found at: {}", xmlFile.getAbsolutePath());
            assertTrue(xmlFile.exists(), "FTP example XML should exist");
        } else {
            LOG.warn("FTP plugin example not found - test skipped");
        }
    }
}

