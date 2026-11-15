package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Unit tests for LoopPlugin iteration limits and index tracking.
 */
public class LoopPluginTest {

    private LoopPlugin plugin;
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        plugin = new LoopPlugin();
        context = new ScraperContext();
    }

    @Test(description = "Loop exposes a zero-based index for every iteration")
    public void testLoopCreatesIndexVariable() throws Exception {
        executeLoop("3", null);

        assertEquals(context.getVar("_index").toString(), "2",
            "The final index after three iterations must be 2");
    }

    @Test(description = "Loop executes exactly the configured number of iterations")
    public void testLoopIterationCount() throws Exception {
        executeLoop("5", null);

        assertEquals(context.getVar("_index").toString(), "4",
            "The final index after five iterations must be 4");
    }

    @Test(description = "Loop resolves a variable reference in maxloops")
    public void testLoopWithVariableMaxloops() throws Exception {
        context.setLocalVar("totalPages", new NodeVariable("3"));

        executeLoop("${totalPages}", null);

        assertEquals(context.getVar("_index").toString(), "2",
            "maxloops must resolve totalPages instead of falling back to 100");
    }

    @Test(description = "Unknown maxloops variable retains the safe default limit")
    public void testLoopWithUnknownMaxloopsVariable() throws Exception {
        executeLoop("${missingLimit}", "first,second,third");

        assertEquals(context.getVar("_index").toString(), "2",
            "An unresolved limit must retain the default and process the supplied items");
    }

    @Test(description = "Negative maxloops processes all explicitly supplied items")
    public void testLoopWithUnlimitedExplicitItems() throws Exception {
        executeLoop("-1", "first,second,third");

        assertEquals(context.getVar("_index").toString(), "2",
            "maxloops=-1 must not truncate an explicit item list");
        assertEquals(context.getVar("_item").toString(), "third");
    }

    @Test(description = "Loop plugin metadata")
    public void testLoopPluginMetadata() {
        assertNotNull(plugin.getMetadata(), "Plugin should have metadata");
        assertEquals(plugin.getMetadata().getName(), "Loop");
    }

    private void executeLoop(String maxloops, String items) throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("maxloops", maxloops);
        if (items != null) {
            properties.put("items", items);
        }

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
            "loop",
            "http://org.webharvest/schema/2.1/core",
            "loop",
            properties
        );
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result, "Loop execution must return a result");
    }
}
