package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.EmptyVariable;
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

    @Test(description = "Negative maxloops without explicit items performs zero fallback iterations")
    public void testLoopWithUnlimitedMaxloopsAndNoItems() throws Exception {
        executeLoop("-1", null);

        assertNull(context.getVar("_index"),
            "Without an explicit item source, unlimited mode must not enter an unbounded fallback loop");
        assertNull(context.getVar("_item"),
            "No item variable should be created when the fallback loop performs zero iterations");
    }

    @Test(description = "Loop plugin metadata")
    public void testLoopPluginMetadata() {
        assertNotNull(plugin.getMetadata(), "Plugin should have metadata");
        assertEquals(plugin.getMetadata().getName(), "Loop");
    }

    @Test(description = "Loop iterations isolate body variables while preserving final item/index compatibility")
    public void testLoopCleansIterationScopeAndRestoresOverriddenVariables() throws Exception {
        ScraperContext isolatedContext = new ScraperContext();
        isolatedContext.setLocalVar("iterationScratch", new NodeVariable("outer"));

        LoopPlugin scopedLoop = new LoopPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext executionContext) {
                executionContext.setLocalVar("iterationScratch", new NodeVariable("inner"));
                executionContext.setLocalVar("createdInsideIteration", new NodeVariable("temporary"));
                return new NodeVariable(executionContext.getVar("_item").toString());
            }
        };

        Map<String, String> properties = new HashMap<>();
        properties.put("maxloops", "10");
        properties.put("items", "first,second");
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
            "loop",
            "http://org.webharvest/schema/2.1/core",
            "loop",
            properties
        );
        scopedLoop.initialize(configuration);

        Variable result = scopedLoop.execute(isolatedContext);

        assertEquals(result.toString(), "firstsecond");
        assertEquals(isolatedContext.getVar("iterationScratch").toString(), "outer",
            "A variable overridden inside an iteration must be restored afterwards");
        assertNull(isolatedContext.getVar("createdInsideIteration"),
            "Variables created in the loop body must not leak out of an iteration");
        assertEquals(isolatedContext.getVar("_item").toString(), "second",
            "Keep the current final-item visibility contract");
        assertEquals(isolatedContext.getVar("_index").toString(), "1",
            "Keep the current final-index visibility contract");
    }

    @Test(description = "Loop iteration scope is cleaned even when the body fails")
    public void testLoopCleansIterationScopeWhenBodyThrows() throws Exception {
        ScraperContext isolatedContext = new ScraperContext();
        isolatedContext.setLocalVar("iterationScratch", new NodeVariable("outer"));

        LoopPlugin failingLoop = new LoopPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext executionContext) {
                executionContext.setLocalVar("iterationScratch", new NodeVariable("inner"));
                executionContext.setLocalVar("createdBeforeFailure", new NodeVariable("temporary"));
                throw new IllegalStateException("simulated body failure");
            }
        };

        Map<String, String> properties = new HashMap<>();
        properties.put("maxloops", "10");
        properties.put("items", "first,second");
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
            "loop",
            "http://org.webharvest/schema/2.1/core",
            "loop",
            properties
        );
        failingLoop.initialize(configuration);

        Variable result = failingLoop.execute(isolatedContext);

        assertTrue(result instanceof EmptyVariable,
            "The current Loop failure contract returns EmptyVariable");
        assertEquals(isolatedContext.getVar("iterationScratch").toString(), "outer",
            "The outer value must be restored after a failed iteration");
        assertNull(isolatedContext.getVar("createdBeforeFailure"),
            "Body-created variables must be removed even when the body fails");
        assertEquals(isolatedContext.getVar("_item").toString(), "first",
            "The current iteration item remains visible after a failed body");
        assertEquals(isolatedContext.getVar("_index").toString(), "0",
            "The current iteration index remains visible after a failed body");
    }

    @Test(description = "Loop propagates interruption after cleaning the current iteration scope")
    public void testLoopPropagatesInterruptionAndCleansIterationScope() throws Exception {
        ScraperContext isolatedContext = new ScraperContext();
        isolatedContext.setLocalVar("iterationScratch", new NodeVariable("outer"));
        LoopPlugin interruptedLoop = new LoopPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext executionContext) throws InterruptedException {
                executionContext.setLocalVar("iterationScratch", new NodeVariable("inner"));
                executionContext.setLocalVar("createdBeforeInterrupt", new NodeVariable("temporary"));
                throw new InterruptedException("simulated cancellation");
            }
        };

        Map<String, String> properties = new HashMap<>();
        properties.put("maxloops", "10");
        properties.put("items", "first,second");
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
            "loop",
            "http://org.webharvest/schema/2.1/core",
            "loop",
            properties
        );
        interruptedLoop.initialize(configuration);

        try {
            interruptedLoop.execute(isolatedContext);
            fail("Loop interruption must abort plugin execution");
        } catch (InterruptedException expected) {
            assertEquals(expected.getMessage(), "simulated cancellation");
            assertTrue(Thread.currentThread().isInterrupted(),
                "The plugin lifecycle restores the interrupt flag after cleanup");
            assertNull(isolatedContext.getVar("createdBeforeInterrupt"),
                "Variables created before interruption must be cleaned up");
            assertEquals(isolatedContext.getVar("iterationScratch").toString(), "outer",
                "Overridden outer variables must be restored after interruption");
            assertEquals(isolatedContext.getVar("_item").toString(), "first");
            assertEquals(isolatedContext.getVar("_index").toString(), "0");
        } finally {
            Thread.interrupted();
        }
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
