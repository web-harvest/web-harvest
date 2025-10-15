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

package org.webharvest.runtime.session.events;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.Config;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.runtime.session.ScraperSessionImpl;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Integration tests for Bug #24: Event Sourcing full event stream.
 * 
 * <p>These tests verify that the complete event sourcing mechanism works end-to-end,
 * capturing all plugin executions as an event stream for debugging, replay, and auditing.</p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class EventSourcingFullStreamBug24IntegrationTest {
    
    private EventBus eventBus;
    private EventCollector collector;
    private Config mockConfig;
    private ScraperSession session;
    private DynamicScopeContext context;
    
    @BeforeMethod
    public void setUp() {
        eventBus = new EventBus();
        collector = new EventCollector();
        eventBus.register(collector);
        
        mockConfig = mock(Config.class);
        session = new ScraperSessionImpl("test-session", "test-client", mockConfig, eventBus);
        
        context = new ScraperContext();
        context.setConfig(mockConfig);
        context.setEventBus(eventBus);
        context.setSession(session);
    }
    
    /**
     * Integration test: Full event stream for successful plugin execution.
     */
    @Test(groups = {"integration", "events", "bug24"})
    public void testFullEventStream_SuccessfulPlugin_PublishesStartedAndCompleted() throws Exception {
        // GIVEN
        TestPlugin plugin = new TestPlugin("test-plugin", false);
        PluginConfiguration mockPluginConfig = mock(PluginConfiguration.class);
        when(mockPluginConfig.getElementName()).thenReturn("test-plugin");
        plugin.initialize(mockPluginConfig);
        
        // WHEN
        Variable result = plugin.execute(context);
        
        // THEN
        assertNotNull(result, "Plugin should return result");
        assertEquals(collector.startedEvents.size(), 1, "Should publish 1 started event");
        assertEquals(collector.completedEvents.size(), 1, "Should publish 1 completed event");
        assertEquals(collector.failedEvents.size(), 0, "Should not publish failed event");
        
        // Verify event ordering
        PluginExecutionStartedEvent startedEvent = collector.startedEvents.get(0);
        PluginExecutionCompletedEvent completedEvent = collector.completedEvents.get(0);
        
        assertEquals(startedEvent.getPluginName(), "test-plugin");
        assertEquals(completedEvent.getPluginName(), "test-plugin");
        assertTrue(startedEvent.getTimestamp().isBefore(completedEvent.getTimestamp()) ||
                   startedEvent.getTimestamp().equals(completedEvent.getTimestamp()),
                   "Started event must be before or equal to completed event");
    }
    
    /**
     * Integration test: Full event stream for failed plugin execution.
     */
    @Test(groups = {"integration", "events", "bug24"})
    public void testFullEventStream_FailedPlugin_PublishesStartedAndFailed() throws Exception {
        // GIVEN
        TestPlugin plugin = new TestPlugin("failing-plugin", true);
        PluginConfiguration mockPluginConfig = mock(PluginConfiguration.class);
        when(mockPluginConfig.getElementName()).thenReturn("failing-plugin");
        plugin.initialize(mockPluginConfig);
        
        // WHEN
        try {
            plugin.execute(context);
            fail("Plugin should throw exception");
        } catch (RuntimeException e) {
            // Expected
        }
        
        // THEN
        assertEquals(collector.startedEvents.size(), 1, "Should publish 1 started event");
        assertEquals(collector.completedEvents.size(), 0, "Should not publish completed event");
        assertEquals(collector.failedEvents.size(), 1, "Should publish 1 failed event");
        
        // Verify event details
        PluginExecutionStartedEvent startedEvent = collector.startedEvents.get(0);
        PluginExecutionFailedEvent failedEvent = collector.failedEvents.get(0);
        
        assertEquals(startedEvent.getPluginName(), "failing-plugin");
        assertEquals(failedEvent.getPluginName(), "failing-plugin");
        assertNotNull(failedEvent.getError(), "Failed event should contain error");
        assertEquals(failedEvent.getError().getMessage(), "Test failure");
    }
    
    /**
     * Integration test: Event stream captures timing information.
     */
    @Test(groups = {"integration", "events", "bug24"})
    public void testFullEventStream_CapturesTiming() throws Exception {
        // GIVEN
        TestPlugin plugin = new TestPlugin("timed-plugin", false);
        PluginConfiguration mockPluginConfig = mock(PluginConfiguration.class);
        when(mockPluginConfig.getElementName()).thenReturn("timed-plugin");
        plugin.initialize(mockPluginConfig);
        
        // WHEN
        long startTime = System.currentTimeMillis();
        plugin.execute(context);
        long endTime = System.currentTimeMillis();
        
        // THEN
        PluginExecutionCompletedEvent completedEvent = collector.completedEvents.get(0);
        long executionTime = completedEvent.getExecutionTimeMs();
        
        assertTrue(executionTime >= 0, "Execution time should be non-negative");
        assertTrue(executionTime <= (endTime - startTime) + 10, 
            "Execution time should be reasonable");
    }
    
    /**
     * Integration test: Multiple plugins create complete event stream.
     */
    @Test(groups = {"integration", "events", "bug24"})
    public void testFullEventStream_MultiplePlugins_OrderedStream() throws Exception {
        // GIVEN
        TestPlugin plugin1 = new TestPlugin("plugin-1", false);
        TestPlugin plugin2 = new TestPlugin("plugin-2", false);
        TestPlugin plugin3 = new TestPlugin("plugin-3", false);
        
        PluginConfiguration mockConfig1 = mock(PluginConfiguration.class);
        when(mockConfig1.getElementName()).thenReturn("plugin-1");
        PluginConfiguration mockConfig2 = mock(PluginConfiguration.class);
        when(mockConfig2.getElementName()).thenReturn("plugin-2");
        PluginConfiguration mockConfig3 = mock(PluginConfiguration.class);
        when(mockConfig3.getElementName()).thenReturn("plugin-3");
        
        plugin1.initialize(mockConfig1);
        plugin2.initialize(mockConfig2);
        plugin3.initialize(mockConfig3);
        
        // WHEN
        plugin1.execute(context);
        plugin2.execute(context);
        plugin3.execute(context);
        
        // THEN
        assertEquals(collector.startedEvents.size(), 3, "Should publish 3 started events");
        assertEquals(collector.completedEvents.size(), 3, "Should publish 3 completed events");
        
        // Verify ordering
        List<String> startedOrder = collector.startedEvents.stream()
            .map(PluginExecutionStartedEvent::getPluginName)
            .collect(Collectors.toList());
        
        assertEquals(startedOrder.get(0), "plugin-1");
        assertEquals(startedOrder.get(1), "plugin-2");
        assertEquals(startedOrder.get(2), "plugin-3");
    }
    
    /**
     * Integration test: Session metrics are updated by events.
     */
    @Test(groups = {"integration", "events", "bug24"})
    public void testFullEventStream_UpdatesSessionMetrics() throws Exception {
        // GIVEN
        TestPlugin plugin = new TestPlugin("metric-plugin", false);
        PluginConfiguration mockPluginConfig = mock(PluginConfiguration.class);
        when(mockPluginConfig.getElementName()).thenReturn("metric-plugin");
        plugin.initialize(mockPluginConfig);
        
        // WHEN
        plugin.execute(context);
        
        // THEN
        assertNotNull(session.getMetrics().getPluginBreakdown(), 
            "Session should have plugin breakdown");
        assertTrue(session.getMetrics().getPluginBreakdown().containsKey("metric-plugin"),
            "Metrics should contain plugin execution time");
    }
    
    /**
     * Thread-safety test: Concurrent plugin execution publishes events correctly.
     */
    @Test(groups = {"integration", "events", "bug24", "concurrent"})
    public void testThreadSafety_ConcurrentExecution_AllEventsPublished() throws Exception {
        // GIVEN
        final int THREAD_COUNT = 10;
        final int EXECUTIONS_PER_THREAD = 5;
        
        List<Thread> threads = new ArrayList<>();
        
        // WHEN
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                for (int j = 0; j < EXECUTIONS_PER_THREAD; j++) {
                    try {
                        String pluginName = "plugin-" + threadId + "-" + j;
                        TestPlugin plugin = new TestPlugin(pluginName, false);
                        PluginConfiguration mockPluginConfig = mock(PluginConfiguration.class);
                        when(mockPluginConfig.getElementName()).thenReturn(pluginName);
                        plugin.initialize(mockPluginConfig);
                        plugin.execute(context);
                    } catch (Exception e) {
                        fail("Thread execution failed: " + e.getMessage());
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }
        
        // THEN
        int expectedEventCount = THREAD_COUNT * EXECUTIONS_PER_THREAD;
        assertEquals(collector.startedEvents.size(), expectedEventCount, 
            "All started events should be published");
        assertEquals(collector.completedEvents.size(), expectedEventCount, 
            "All completed events should be published");
    }
    
    /**
     * Test plugin for integration testing.
     */
    private static class TestPlugin extends AbstractPlugin {
        private final String pluginName;
        private final boolean shouldFail;
        
        public TestPlugin(String pluginName, boolean shouldFail) {
            this.pluginName = pluginName;
            this.shouldFail = shouldFail;
        }
        
        @Override
        protected void doInitialize(PluginConfiguration configuration) throws PluginException {
            // Mock configuration
        }
        
        @Override
        protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
            if (shouldFail) {
                throw new RuntimeException("Test failure");
            }
            return new NodeVariable("<result>" + pluginName + "</result>");
        }
        
        @Override
        protected void doValidateConfiguration(PluginConfiguration configuration) {
            // No validation needed for test
        }
        
        @Override
        protected void doCleanup() {
            // No cleanup needed for test
        }
        
        @Override
        public PluginMetadata getMetadata() {
            return null; // Not needed for test
        }
        
        @Override
        public String toString() {
            return pluginName;
        }
    }
    
    /**
     * Event collector for assertions.
     */
    private static class EventCollector {
        // Use CopyOnWriteArrayList for thread-safety
        final List<PluginExecutionStartedEvent> startedEvents = new CopyOnWriteArrayList<>();
        final List<PluginExecutionCompletedEvent> completedEvents = new CopyOnWriteArrayList<>();
        final List<PluginExecutionFailedEvent> failedEvents = new CopyOnWriteArrayList<>();
        
        @Subscribe
        public void onPluginStarted(PluginExecutionStartedEvent event) {
            startedEvents.add(event);
        }
        
        @Subscribe
        public void onPluginCompleted(PluginExecutionCompletedEvent event) {
            completedEvents.add(event);
        }
        
        @Subscribe
        public void onPluginFailed(PluginExecutionFailedEvent event) {
            failedEvents.add(event);
        }
    }
}

