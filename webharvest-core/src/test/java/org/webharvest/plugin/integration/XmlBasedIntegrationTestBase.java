/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
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
import org.webharvest.runtime.variables.Variable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Base class for XML-based integration tests.
 * Provides utilities for loading and executing XML configurations from the examples directory.
 * 
 * @since 2.2.0
 */
public abstract class XmlBasedIntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(XmlBasedIntegrationTestBase.class);
    
    protected Injector injector;
    protected Harvest harvest;
    protected DynamicScopeContext context;
    protected List<String> outputFilesToCleanup = new ArrayList<>();
    
    // Base directory for examples - using test resources
    protected static final String TEST_RESOURCES_BASE = "integration-tests";
    protected static final String NEW_PLUGINS_DIR = TEST_RESOURCES_BASE + "/new_plugins";
    
    @BeforeMethod
    public void setUp() {
        LOG.info("Setting up XML-based integration test environment");
        
        // Initialize Guice injector with required modules
        // Note: HttpModule removed during HttpComponents 4.x migration
        // Use target/ as working directory - all generated files will go to target/
        // This prevents polluting project root with test output files
        String targetDir = "target";
        new File(targetDir).mkdirs();
        injector = Guice.createInjector(
            new ScraperModule(targetDir)
        );
        
        harvest = injector.getInstance(Harvest.class);
        outputFilesToCleanup = new ArrayList<>();
    }
    
    @AfterMethod
    public void tearDown() {
        LOG.info("Tearing down XML-based integration test environment");
        
        // Cleanup output files created during tests
        for (String filePath : outputFilesToCleanup) {
            try {
                File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                    LOG.debug("Cleaned up test file: {}", filePath);
                }
            } catch (Exception e) {
                LOG.warn("Failed to cleanup test file: {}", filePath, e);
            }
        }
        
        harvest = null;
        injector = null;
        context = null;
    }
    
    /**
     * Executes an XML configuration file and returns the result context.
     * 
     * @param xmlFilePath Path to the XML configuration file relative to test resources
     * @return The execution context containing results
     * @throws Exception If execution fails
     */
    protected DynamicScopeContext executeXmlConfig(String xmlFilePath) throws Exception {
        LOG.info("Executing XML configuration: {}", xmlFilePath);
        
        // Try to load from test resources first
        java.net.URL resourceUrl = getClass().getClassLoader().getResource(xmlFilePath);
        File xmlFile;
        
        if (resourceUrl != null) {
            xmlFile = new File(resourceUrl.getFile());
            LOG.debug("Loaded XML from test resources: {}", xmlFile.getAbsolutePath());
        } else {
            // Fallback to absolute path if not in resources
            xmlFile = new File(xmlFilePath);
            LOG.debug("Using absolute path: {}", xmlFile.getAbsolutePath());
        }
        
        assertNotNull(xmlFile, "XML file should exist: " + xmlFilePath);
        assertTrue(xmlFile.exists(), "XML file should exist: " + xmlFilePath + " (resolved to: " + xmlFile.getAbsolutePath() + ")");
        
        ConfigSource configSource = new FileConfigSource(xmlFile);
        
        final DynamicScopeContext[] resultContext = new DynamicScopeContext[1];
        
        // Create a HarvestLoadCallback that does nothing (configuration loads automatically)
        org.webharvest.HarvestLoadCallback loadCallback = new org.webharvest.HarvestLoadCallback() {
            @Override
            public void onSuccess(org.webharvest.definition.PluginConfigurationTree configTree) {
                LOG.debug("Configuration loaded: {}", configTree);
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
    
    /**
     * Executes an XML configuration and verifies that no exception was thrown.
     * 
     * @param xmlFilePath Path to the XML configuration file
     * @return true if execution succeeded without exceptions
     */
    protected boolean executeAndVerifyNoException(String xmlFilePath) {
        try {
            DynamicScopeContext ctx = executeXmlConfig(xmlFilePath);
            assertNotNull(ctx);
            return true;
        } catch (Exception e) {
            LOG.error("Execution failed with exception", e);
            fail("XML execution should not throw exception: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Executes an XML configuration and retrieves a variable from the context.
     * 
     * @param xmlFilePath Path to the XML configuration file
     * @param varName Name of the variable to retrieve
     * @return The variable value, or null if not found
     */
    protected Variable executeAndGetVariable(String xmlFilePath, String varName) throws Exception {
        DynamicScopeContext ctx = executeXmlConfig(xmlFilePath);
        Variable var = ctx.getVar(varName);
        assertNotNull(var, "Variable '" + varName + "' should exist in context");
        return var;
    }
    
    /**
     * Executes an XML configuration that writes to a file and returns the file content.
     * 
     * @param xmlFilePath Path to the XML configuration file
     * @param outputFilePath Path to the expected output file
     * @return Content of the output file
     */
    protected String executeAndReadOutputFile(String xmlFilePath, String outputFilePath) throws Exception {
        // Test outputs now go to target/ directory (workingDir is set to "target")
        String fullOutputPath = "target/" + outputFilePath;
        
        // Add to cleanup list
        outputFilesToCleanup.add(fullOutputPath);
        
        // Execute configuration
        executeXmlConfig(xmlFilePath);
        
        // Read output file from target/
        Path outputPath = Paths.get(fullOutputPath);
        assertTrue(Files.exists(outputPath), "Output file should exist: " + fullOutputPath);
        
        String content = new String(Files.readAllBytes(outputPath));
        assertNotNull(content, "Output file content should not be null");
        
        return content;
    }
    
    /**
     * Verifies that a file was created by the XML configuration.
     * 
     * @param filePath Path to the file to check
     */
    protected void assertFileExists(String filePath) {
        outputFilesToCleanup.add(filePath);
        File file = new File(filePath);
        assertTrue(file.exists(), "File should exist: " + filePath);
    }
    
    /**
     * Verifies that a file contains expected text.
     * 
     * @param filePath Path to the file to check
     * @param expectedText Text that should be contained in the file
     */
    protected void assertFileContains(String filePath, String expectedText) throws IOException {
        assertFileExists(filePath);
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        assertTrue(content.contains(expectedText), 
            "File should contain text '" + expectedText + "' but was: " + content);
    }
    
    /**
     * Gets the absolute path for an example XML file in the new_plugins directory.
     * 
     * @param filename Name of the XML file (e.g., "01_constant_plugin_example.xml")
     * @return Absolute path to the file
     */
    protected String getNewPluginExamplePath(String filename) {
        return NEW_PLUGINS_DIR + "/" + filename;
    }
    
    /**
     * Gets the path for an example XML file in the test resources directory.
     * 
     * @param filename Name of the XML file
     * @return Path to the file relative to test resources
     */
    protected String getExamplePath(String filename) {
        return TEST_RESOURCES_BASE + "/" + filename;
    }
    
    /**
     * Executes an XML configuration from a String and returns the result context.
     * 
     * @param xmlString The XML configuration as a String
     * @return The execution context containing results
     * @throws Exception If execution fails
     */
    protected DynamicScopeContext executeXmlString(String xmlString) throws Exception {
        LOG.info("Executing XML configuration from String");
        
        // Create config source from String
        org.webharvest.definition.ConfigSource configSource = 
            new org.webharvest.definition.BufferConfigSource(xmlString);
        
        final DynamicScopeContext[] resultContext = new DynamicScopeContext[1];
        
        // Create a HarvestLoadCallback that does nothing (configuration loads automatically)
        org.webharvest.HarvestLoadCallback loadCallback = new org.webharvest.HarvestLoadCallback() {
            @Override
            public void onSuccess(org.webharvest.definition.PluginConfigurationTree configTree) {
                LOG.debug("Configuration loaded: {}", configTree);
            }
        };
        
        Harvester harvester = harvest.getHarvester(configSource, loadCallback);
        resultContext[0] = harvester.execute(new Harvester.ContextInitCallback() {
            @Override
            public void onSuccess(DynamicScopeContext context) {
                LOG.debug("XML configuration executed successfully");
            }
        });
        
        assertNotNull(resultContext[0], "Execution context should not be null");
        return resultContext[0];
    }
}

