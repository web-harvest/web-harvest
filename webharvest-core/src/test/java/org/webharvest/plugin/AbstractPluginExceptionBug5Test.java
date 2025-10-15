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
package org.webharvest.plugin;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * TDD Tests for Bug #5: Exception Handling & Stack Cleanup in AbstractPlugin
 * 
 * <p>
 * <strong>Problem (old architecture - BaseProcessor):</strong>
 * When execute() threw an exception, cleanup code was not executed:
 * - scraper.finishExecutingProcessor() - stack cleanup
 * - setProperty() - execution time
 * - writeDebugFile() - debug output
 * </p>
 * 
 * <p>
 * <strong>Solution (patch):</strong>
 * Wrap execute() in try-finally block to ensure cleanup always runs.
 * </p>
 * 
 * <p>
 * <strong>v2.2.0 Analysis:</strong>
 * AbstractPlugin.execute() has try-catch but NO finally block.
 * This test verifies cleanup behavior when exceptions occur.
 * </p>
 * 
 * @author Robert Bala
 * @since 2.2.1
 */
public class AbstractPluginExceptionBug5Test {
    
    private DynamicScopeContext context;
    private TestPluginWithCleanup cleanupPlugin;
    private TestPluginThatThrows throwingPlugin;
    
    @Mock
    private PluginConfiguration mockConfig;
    
    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = new ScraperContext();
        cleanupPlugin = new TestPluginWithCleanup();
        throwingPlugin = new TestPluginThatThrows();
        
        // Setup mock config
        when(mockConfig.getElementName()).thenReturn("test");
        when(mockConfig.hasChildren()).thenReturn(false);
    }
    
    /**
     * TDD Cycle 1 (RED): Test that cleanup IS called even when doExecute() throws
     */
    @Test(expectedExceptions = RuntimeException.class)
    public void testBug5_Cycle1_CleanupNotCalledOnException() throws Exception {
        // GIVEN: Plugin that throws in doExecute() and tracks cleanup
        throwingPlugin.initialize(mockConfig);
        
        // WHEN: execute() called and doExecute() throws
        try {
            throwingPlugin.execute(context);
        } finally {
            // THEN: Cleanup should be called (currently it's NOT)
            assertTrue(throwingPlugin.wasCleanupCalled(), 
                "Bug #5: Cleanup must be called even when doExecute() throws exception!");
        }
    }
    
    /**
     * TDD Cycle 2 (GREEN): Test that cleanup records metrics on exception
     */
    @Test(expectedExceptions = RuntimeException.class)
    public void testBug5_Cycle2_CleanupRecordsMetricsOnException() throws Exception {
        // GIVEN: Plugin that throws and records metrics in cleanup
        TestPluginWithMetrics metricsPlugin = new TestPluginWithMetrics();
        metricsPlugin.initialize(mockConfig);
        
        // WHEN: execute() throws
        try {
            metricsPlugin.execute(context);
        } finally {
            // THEN: Cleanup should be called and metrics recorded
            assertTrue(metricsPlugin.wasCleanupCalled(), 
                "Bug #5: Cleanup must be called even on exception!");
            assertTrue(metricsPlugin.wasMetricRecorded(),
                "Bug #5: Metrics should be recorded in cleanup!");
        }
    }
    
    /**
     * TDD Cycle 3 (RED): Test normal execution still calls cleanup
     */
    @Test
    public void testBug5_Cycle3_CleanupCalledOnSuccess() throws Exception {
        // GIVEN: Plugin that completes successfully
        cleanupPlugin.initialize(mockConfig);
        
        // WHEN: execute() succeeds
        Variable result = cleanupPlugin.execute(context);
        
        // THEN: Cleanup should be called
        assertNotNull(result, "Result should not be null");
        assertTrue(cleanupPlugin.wasCleanupCalled(), 
            "Cleanup must be called on successful execution");
    }
    
    /**
     * TDD Cycle 4 (RED): Test exception is still propagated after cleanup
     */
    @Test
    public void testBug5_Cycle4_ExceptionStillPropagatedAfterCleanup() throws Exception {
        // GIVEN: Plugin that throws
        throwingPlugin.initialize(mockConfig);
        
        // WHEN/THEN: Exception is thrown but cleanup was called
        try {
            throwingPlugin.execute(context);
            fail("Should have thrown RuntimeException");
        } catch (RuntimeException e) {
            // Expected exception
            assertTrue(e.getMessage().contains("Plugin execution failed"),
                "Exception message should indicate plugin failure");
            
            // BUT cleanup should have been called
            assertTrue(throwingPlugin.wasCleanupCalled(),
                "Bug #5: Cleanup must be called before exception propagates!");
        }
    }
    
    // ========== Helper Classes ==========
    
    /**
     * Test plugin that tracks cleanup calls
     */
    private static class TestPluginWithCleanup extends AbstractPlugin {
        private boolean cleanupCalled = false;
        private Long executionTime = null;
        
        @Override
        protected void doInitialize(PluginConfiguration configuration) throws PluginException {
            // No-op
        }
        
        @Override
        protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
            return EmptyVariable.INSTANCE;
        }
        
        @Override
        protected void doValidateConfiguration(PluginConfiguration configuration) 
                throws PluginValidationException {
            // No-op
        }
        
        @Override
        protected void doCleanup() {
            this.cleanupCalled = true;
        }
        
        public boolean wasCleanupCalled() {
            return cleanupCalled;
        }
        
        public void setExecutionTime(Long time) {
            this.executionTime = time;
        }
        
        public Long getExecutionTime() {
            return executionTime;
        }
        
        @Override
        public PluginMetadata getMetadata() {
            return null; // Not needed for test
        }
        
        public PluginConfigurationSchema getConfigurationSchema() {
            return null; // Not needed for test
        }
    }
    
    /**
     * Test plugin that throws exception in doExecute()
     */
    private static class TestPluginThatThrows extends TestPluginWithCleanup {
        @Override
        protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
            throw new RuntimeException("Test exception from doExecute()");
        }
    }
    
    /**
     * Test plugin that records metrics in cleanup
     */
    private static class TestPluginWithMetrics extends TestPluginWithCleanup {
        private boolean metricRecorded = false;
        
        @Override
        protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
            throw new RuntimeException("Test exception - should record metrics");
        }
        
        @Override
        protected void doCleanup() {
            super.doCleanup();  // Call parent cleanup
            this.metricRecorded = true;  // Record metric
        }
        
        public boolean wasMetricRecorded() {
            return metricRecorded;
        }
    }
}

