package org.webharvest.ide;

import com.google.common.eventbus.EventBus;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.webharvest.Harvest;
import org.webharvest.Harvester;
import org.webharvest.HarvestLoadCallback;
import org.webharvest.definition.*;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.Scraper;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.session.ScraperSessionImpl;
import org.webharvest.runtime.variables.Variable;

import java.util.UUID;

import static org.testng.Assert.*;

/**
 * Test to compare OLD (working) API vs NEW (potentially broken) API.
 * This helps identify if the v2.2 migration broke variable setting.
 */
public class APIComparisonTest {

    private Injector injector;

    @BeforeClass
    public void setUp() {
        injector = Guice.createInjector(new ScraperModule("."));
    }

    @Test(description = "Test that OLD API (Harvest.getHarvester) sets variables correctly")
    public void testOldAPIWorking() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                          "    <def var=\"test\">Hello from OLD API!</def>\n" +
                          "</config>";

        Harvest harvest = injector.getInstance(Harvest.class);
        ConfigSource configSource = new BufferConfigSource(configXml);

        final DynamicScopeContext[] resultContext = new DynamicScopeContext[1];

        HarvestLoadCallback loadCallback = new HarvestLoadCallback() {
            @Override
            public void onSuccess(PluginConfigurationTree configTree) {
                // Config loaded
            }
        };

        Harvester harvester = harvest.getHarvester(configSource, loadCallback);
        resultContext[0] = harvester.execute(new Harvester.ContextInitCallback() {
            @Override
            public void onSuccess(DynamicScopeContext context) {
                // Execution successful
            }
        });

        assertNotNull(resultContext[0], "OLD API should return context");

        Variable testVar = resultContext[0].getVar("test");
        assertNotNull(testVar, "OLD API should set 'test' variable");
        assertEquals(testVar.toString().trim(), "Hello from OLD API!",
                    "OLD API should set correct value");
    }

    @Test(description = "Test that NEW API (Scraper) sets variables correctly")
    public void testNewAPIWorking() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                          "    <def var=\"test\">Hello from NEW API!</def>\n" +
                          "</config>";

        ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);
        ConfigSource configSource = new BufferConfigSource(configXml);
        Config config = configFactory.create(configSource);
        config.reload(); // CRITICAL!

        // Create execution context
        DynamicScopeContext context = new ScraperContext();
        context.setConfig(config);

        // Create session
        String sessionId = UUID.randomUUID().toString();
        ScraperSessionImpl session = new ScraperSessionImpl(sessionId, "test-client", config);
        context.setEventBus(new EventBus());
        context.setSession(session);
        session.markRunning(context);

        // Execute
        Scraper scraper = new Scraper(context.getEventBus());
        scraper.execute(context);

        // Check if variable was set
        Variable testVar = context.getVar("test");
        assertNotNull(testVar, "NEW API should set 'test' variable");
        assertEquals(testVar.toString().trim(), "Hello from NEW API!",
                    "NEW API should set correct value");
    }
}

