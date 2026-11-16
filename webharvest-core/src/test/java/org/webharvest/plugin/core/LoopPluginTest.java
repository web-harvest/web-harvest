package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

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

        try {
            failingLoop.execute(isolatedContext);
            fail("A Loop body failure must propagate instead of looking like an empty result");
        } catch (RuntimeException expected) {
            assertTrue(expected.getCause() instanceof IllegalStateException,
                "The original body failure must remain available as the cause");
            assertEquals(expected.getCause().getMessage(), "simulated body failure");
        }

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
    @Test(description = "Loop preserves Variable instances from a ListVariable source")
    public void testListVariableSourcePreservesElementInstances() throws Exception {
        ScraperContext sourceContext = new ScraperContext();
        NodeVariable firstItem = new NodeVariable(Integer.valueOf(42));
        ListVariable source = new ListVariable();
        source.addVariable(firstItem);
        sourceContext.setLocalVar("source", source);

        java.util.List<Variable> observedItems = new java.util.ArrayList<>();
        LoopPlugin observingLoop = new LoopPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext executionContext) {
                observedItems.add(executionContext.getVar("entry"));
                return EmptyVariable.INSTANCE;
            }
        };

        Map<String, String> properties = new HashMap<>();
        properties.put("items", "${source}");
        properties.put("item", "entry");
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
            "loop",
            "http://org.webharvest/schema/2.1/core",
            "loop",
            properties
        );
        observingLoop.initialize(configuration);
        observingLoop.execute(sourceContext);

        assertEquals(observedItems.size(), 1);
        assertSame(observedItems.get(0), firstItem,
            "ListVariable elements must not be converted to new NodeVariable instances");
        assertEquals(observedItems.get(0).getWrappedObject(), Integer.valueOf(42),
            "The original wrapped value type must be preserved");
    }

    @Test(description = "empty=false preserves EmptyVariable elements from a ListVariable source")
    public void testEmptyFalsePreservesEmptyVariableElements() throws Exception {
        ListVariable source = new ListVariable();
        source.addVariable(EmptyVariable.INSTANCE);
        source.addVariable(new NodeVariable("value"));

        java.util.List<Variable> observed = captureLoopItems(source, false);

        assertEquals(observed.size(), 2);
        assertSame(observed.get(0), EmptyVariable.INSTANCE);
    }

    @Test(description = "empty=true skips EmptyVariable elements from a ListVariable source")
    public void testEmptyTrueSkipsEmptyVariableElements() throws Exception {
        ListVariable source = new ListVariable();
        source.addVariable(EmptyVariable.INSTANCE);
        NodeVariable value = new NodeVariable("value");
        source.addVariable(value);

        java.util.List<Variable> observed = captureLoopItems(source, true);

        assertEquals(observed.size(), 1);
        assertSame(observed.get(0), value);
    }

    @Test(description = "empty=true preserves whitespace-only typed values because Variable.isEmpty treats them as non-empty")
    public void testEmptyTruePreservesWhitespaceOnlyTypedVariable() throws Exception {
        NodeVariable whitespace = new NodeVariable("   ");
        ListVariable source = new ListVariable();
        source.addVariable(whitespace);

        java.util.List<Variable> observed = captureLoopItems(source, true);

        assertEquals(observed.size(), 1);
        assertSame(observed.get(0), whitespace);
    }


    private java.util.List<Variable> captureLoopItems(ListVariable source, boolean skipEmpty)
            throws Exception {
        ScraperContext sourceContext = new ScraperContext();
        sourceContext.setLocalVar("source", source);
        java.util.List<Variable> observedItems = new java.util.ArrayList<>();

        LoopPlugin observingLoop = new LoopPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext executionContext) {
                observedItems.add(executionContext.getVar("entry"));
                return EmptyVariable.INSTANCE;
            }
        };

        Map<String, String> properties = new HashMap<>();
        properties.put("items", "${source}");
        properties.put("item", "entry");
        if (skipEmpty) {
            properties.put("empty", "true");
        }
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
            "loop",
            "http://org.webharvest/schema/2.1/core",
            "loop",
            properties
        );
        observingLoop.initialize(configuration);
        observingLoop.execute(sourceContext);
        return observedItems;
    }

    @Test(description = "An explicitly referenced empty ListVariable does not trigger count-only fallback")
    public void testEmptyReferencedListVariableDoesNotUseMaxloopsFallback() throws Exception {
        ScraperContext sourceContext = new ScraperContext();
        sourceContext.setLocalVar("source", new ListVariable());
        int[] executions = new int[]{0};

        LoopPlugin observingLoop = new LoopPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext executionContext) {
                executions[0]++;
                return EmptyVariable.INSTANCE;
            }
        };

        Map<String, String> properties = new HashMap<>();
        properties.put("items", "${source}");
        properties.put("maxloops", "3");
        observingLoop.initialize(new DefaultPluginConfiguration(
            "loop", "http://org.webharvest/schema/2.1/core", "loop", properties));

        observingLoop.execute(sourceContext);

        assertEquals(executions[0], 0,
            "An explicitly supplied empty collection is zero items, not an absent source");
        assertNull(sourceContext.getVar("_item"));
        assertNull(sourceContext.getVar("_index"));
    }

    @Test(description = "An unresolved items variable retains the historical literal-token behavior")
    public void testUnresolvedItemsReferenceRemainsLiteralToken() throws Exception {
        ScraperContext sourceContext = new ScraperContext();
        java.util.List<String> observedItems = new java.util.ArrayList<>();

        LoopPlugin observingLoop = new LoopPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext executionContext) {
                observedItems.add(executionContext.getVar("_item").toString());
                return EmptyVariable.INSTANCE;
            }
        };

        Map<String, String> properties = new HashMap<>();
        properties.put("items", "${missingSource}");
        properties.put("maxloops", "3");
        observingLoop.initialize(new DefaultPluginConfiguration(
            "loop", "http://org.webharvest/schema/2.1/core", "loop", properties));

        observingLoop.execute(sourceContext);

        assertEquals(observedItems, Collections.singletonList("${missingSource}"));
    }

    @Test(description = "A zero maxloops limit avoids evaluating a child input source")
    public void testZeroMaxloopsDoesNotEvaluateChildSource() throws Exception {
        CountingScraperContext sourceContext = new CountingScraperContext();
        LoopPlugin loop = new LoopPlugin();
        Map<String, String> properties = new HashMap<>();
        properties.put("maxloops", "0");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
            "loop", "http://org.webharvest/schema/2.1/core", "loop", properties);
        configuration.setChildren(Collections.singletonList(
            new DefaultPluginConfiguration("list", "http://org.webharvest/schema/2.1/core", "list")));
        loop.initialize(configuration);

        loop.execute(sourceContext);

        assertEquals(sourceContext.getScopeCount(), 0,
            "A zero iteration limit must not execute a source plugin");
        assertNull(sourceContext.getVar("_item"));
        assertNull(sourceContext.getVar("_index"));
    }

    private static class CountingScraperContext extends ScraperContext {
        private int scopeCount;

        @Override
        public <R> R executeWithinNewContext(Callable<R> callable) throws InterruptedException {
            scopeCount++;
            return super.executeWithinNewContext(callable);
        }

        int getScopeCount() {
            return scopeCount;
        }
    }

    @Test(description = "Loop preserves nested ListVariable elements as typed items")
    public void testListVariableSourcePreservesNestedListElements() throws Exception {
        ScraperContext sourceContext = new ScraperContext();
        ListVariable nested = new ListVariable();
        nested.addVariable(new NodeVariable("nested-value"));
        NodeVariable scalar = new NodeVariable("scalar-value");

        // The Iterable constructor preserves direct Variable members. addVariable(ListVariable)
        // intentionally flattens its argument and therefore cannot construct this fixture.
        ListVariable source = new ListVariable(java.util.Arrays.asList(nested, scalar));
        sourceContext.setLocalVar("source", source);

        java.util.List<Variable> observedItems = new java.util.ArrayList<>();
        LoopPlugin observingLoop = new LoopPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext executionContext) {
                observedItems.add(executionContext.getVar("entry"));
                return EmptyVariable.INSTANCE;
            }
        };

        Map<String, String> properties = new HashMap<>();
        properties.put("items", "${source}");
        properties.put("item", "entry");
        observingLoop.initialize(new DefaultPluginConfiguration(
            "loop", "http://org.webharvest/schema/2.1/core", "loop", properties));

        observingLoop.execute(sourceContext);

        assertEquals(observedItems.size(), 2,
            "Each direct ListVariable member must produce exactly one iteration");
        assertSame(observedItems.get(0), nested,
            "A nested ListVariable member must remain a typed item, not be recursively flattened");
        assertSame(observedItems.get(1), scalar);
        assertEquals(((ListVariable) observedItems.get(0)).getList().size(), 1,
            "The nested collection structure must remain intact");
    }

}
