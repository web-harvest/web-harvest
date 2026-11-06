package org.webharvest.examples;

import com.google.common.eventbus.EventBus;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.webharvest.definition.BufferConfigSource;
import org.webharvest.definition.Config;
import org.webharvest.definition.ConfigFactory;
import org.webharvest.definition.ConfigSource;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.Scraper;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.session.ScraperSessionImpl;

import java.io.File;
import java.util.UUID;

import static org.testng.Assert.assertNotNull;

/**
 * Base class for integration tests using v2.2 Scraper API.
 * Provides utilities for executing XML configurations and verifying results.
 * 
 * This replaces XmlBasedIntegrationTestBase which uses deprecated getHarvester() API.
 * 
 * @since 2.2.0
 */
public abstract class ScraperTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(ScraperTestBase.class);
    
    protected Injector injector;
    protected ConfigFactory configFactory;
    
    @BeforeMethod
    public void setUp() {
        LOG.info("Setting up v2.2 Scraper test environment");
        // Use target/ as working directory - all generated files will go to target/
        String targetDir = "target";
        new File(targetDir).mkdirs();
        injector = Guice.createInjector(new ScraperModule(targetDir));
        configFactory = injector.getInstance(ConfigFactory.class);
    }
    
    @AfterMethod
    public void tearDown() {
        LOG.info("Tearing down v2.2 Scraper test environment");
        configFactory = null;
        injector = null;
    }
    
    /**
     * Executes an XML configuration string and returns the execution context.
     * Uses v2.2 Scraper API (NOT deprecated getHarvester).
     * 
     * @param xmlString The XML configuration as a String
     * @return The execution context containing variables and results
     * @throws Exception If execution fails
     */
    protected DynamicScopeContext executeXmlString(String xmlString) throws Exception {
        LOG.info("Executing XML configuration from String (v2.2 API)");
        
        // Parse config
        ConfigSource configSource = new BufferConfigSource(xmlString);
        Config config = configFactory.create(configSource);
        config.reload(); // CRITICAL!
        
        // Create execution context
        // Resolve the context through Guice so @PostConstruct initialization runs.
        // A manually constructed ScraperContext misses the built-in `sys` variable.
        DynamicScopeContext context = injector.getInstance(ScraperContext.class);
        context.setConfig(config);
        context.setScriptingLanguage(org.webharvest.runtime.scripting.ScriptingLanguage.BEANSHELL);
        context.setCharset("UTF-8");
        
        // Create session
        String sessionId = UUID.randomUUID().toString();
        ScraperSessionImpl session = new ScraperSessionImpl(sessionId, "test-client", config);
        context.setEventBus(new EventBus());
        context.setSession(session);
        session.markRunning(context);
        
        // Execute scraper
        Scraper scraper = new Scraper(context.getEventBus());
        scraper.execute(context);
        
        LOG.info("XML configuration executed successfully");
        assertNotNull(context, "Execution context should not be null");
        
        return context;
    }
    
    /**
     * Executes an XML configuration and returns the value of a specific variable.
     * Uses v2.2 Scraper API.
     * 
     * @param xmlString The XML configuration as a String
     * @param variableName The name of the variable to retrieve
     * @return The variable value
     * @throws Exception If execution fails
     */
    protected org.webharvest.runtime.variables.Variable executeAndGetVariable(String xmlString, String variableName) throws Exception {
        DynamicScopeContext context = executeXmlString(xmlString);
        return context.getVar(variableName);
    }
}

