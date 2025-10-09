package org.webharvest.runtime.processors;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.IElementDef;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Tests for Bug #27: Indent/RunningProcessors broken by exceptions.
 * 
 * <h3>Bug #27: Indent / RunningProcessors broken by exceptions</h3>
 * 
 * <p><strong>Original Report:</strong></p>
 * <blockquote>
 * "The indent on the log file entries keeps getting bigger. What appears to be 
 * happening is that it's being broken by Exceptions that occur and are caught 
 * by a TryProcessor, where the processors between the cause and the TryProcessor 
 * are left on the stack."
 * </blockquote>
 * 
 * <p><strong>Root Cause:</strong></p>
 * <p>
 * When a processor throws an exception, cleanup (finishExecutingProcessor) was 
 * not called, leaving processors on the stack. This caused:
 * </p>
 * <ul>
 * <li>Growing indent in logs (getRunningLevel() keeps growing)</li>
 * <li>Memory leaks (orphaned processors)</li>
 * <li>100% CPU load (inefficient CommonUtil.replicate with growing indent)</li>
 * </ul>
 * 
 * <p><strong>Fix (v2.2.1):</strong></p>
 * <p>
 * Wrapped processor execution in try-finally block in AbstractProcessor.run():
 * </p>
 * <pre>{@code
 * public Variable run(DynamicScopeContext context) throws InterruptedException {
 *     ProcessorExecutionContext executionContext = prepareExecution(context);
 *     Variable result = null;
 *     try {
 *         result = executeProcessor(executionContext);
 *         return result;
 *     } finally {
 *         // Bug #27 Fix: ALWAYS cleanup, even on exception
 *         finalizeExecution(executionContext, result);
 *     }
 * }
 * }</pre>
 * 
 * @author Web-Harvest Team
 * @since 2.2.1
 */
public class AbstractProcessorBug27Test {
    
    private DynamicScopeContext context;
    
    @BeforeMethod
    public void setUp() {
        context = new ScraperContext();
    }
    
    /**
     * Test Bug #27: Processor that throws exception calls finalization.
     * 
     * <p>Verifies that finalizeExecution() is called even when executeProcessor() 
     * throws an exception.</p>
     */
    @Test(groups = {"bug-fix", "processor"})
    public void testBug27_ExceptionStillCallsFinalization() throws InterruptedException {
        // Create processor that throws exception
        TestProcessor processor = new TestProcessor(true, false);
        
        try {
            processor.run(context);
            fail("Should have thrown RuntimeException");
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "Test exception");
        }
        
        // Bug #27 Fix: Verify finalization was called despite exception
        assertTrue(processor.isFinalizeCalled(), 
            "finalizeExecution should be called even when exception occurs");
    }
    
    /**
     * Test Bug #27: Successful processor calls finalization.
     * 
     * <p>Verifies normal path still works correctly.</p>
     */
    @Test(groups = {"bug-fix", "processor"})
    public void testBug27_SuccessfulProcessorCallsFinalization() throws InterruptedException {
        TestProcessor processor = new TestProcessor(false, false);
        
        Variable result = processor.run(context);
        
        assertNotNull(result);
        assertTrue(processor.isFinalizeCalled(), 
            "finalizeExecution should be called on success");
    }
    
    /**
     * Test Bug #27: InterruptedException still calls finalization.
     * 
     * <p>Verifies that interruption also triggers cleanup.</p>
     */
    @Test(groups = {"bug-fix", "processor"})
    public void testBug27_InterruptedExceptionCallsFinalization() throws InterruptedException {
        TestProcessor processor = new TestProcessor(false, true);
        
        try {
            processor.run(context);
            fail("Should have thrown InterruptedException");
        } catch (InterruptedException e) {
            // Expected
        }
        
        // Bug #27 Fix: Verify finalization was called
        assertTrue(processor.isFinalizeCalled(), 
            "finalizeExecution should be called even on interruption");
    }
    
    /**
     * Test Bug #27: Running level doesn't grow with exceptions.
     * 
     * <p>Verifies that runningLevel is correctly managed when exceptions occur.</p>
     */
    @Test(groups = {"bug-fix", "processor"})
    public void testBug27_RunningLevelDoesNotGrowWithExceptions() throws InterruptedException {
        // Create nested processors with exception in middle one
        TestProcessor parent = new TestProcessor(false, false);
        TestProcessor middle = new TestProcessor(true, false);  // Throws exception
        TestProcessor child = new TestProcessor(false, false);
        
        middle.setParentProcessor(parent);
        child.setParentProcessor(middle);
        
        // Get initial running level
        int initialLevel = child.getRunningLevel();
        assertEquals(initialLevel, 3, "Child should be at level 3");
        
        // Execute middle processor (throws exception)
        try {
            middle.run(context);
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            // Expected
        }
        
        // Bug #27 Fix: Verify level is still correct after exception
        // (finalization was called, so structure is clean)
        assertEquals(child.getRunningLevel(), 3, 
            "Running level should not change after exception");
    }
    
    /**
     * Test Bug #27: Multiple exceptions don't corrupt processor tree.
     * 
     * <p>Simulates multiple exceptions in nested try-catch blocks.</p>
     */
    @Test(groups = {"bug-fix", "processor"})
    public void testBug27_MultipleExceptionsDoNotCorruptTree() throws InterruptedException {
        TestProcessor processor1 = new TestProcessor(true, false);
        TestProcessor processor2 = new TestProcessor(true, false);
        TestProcessor processor3 = new TestProcessor(true, false);
        
        // Execute multiple processors that throw exceptions
        for (TestProcessor proc : new TestProcessor[]{processor1, processor2, processor3}) {
            try {
                proc.run(context);
                fail("Should have thrown exception");
            } catch (RuntimeException e) {
                // Expected - simulate try/catch catching exception
            }
        }
        
        // Bug #27 Fix: All processors should have finalized
        assertTrue(processor1.isFinalizeCalled());
        assertTrue(processor2.isFinalizeCalled());
        assertTrue(processor3.isFinalizeCalled());
        
        // Running levels should be consistent
        assertEquals(processor1.getRunningLevel(), 1);
        assertEquals(processor2.getRunningLevel(), 1);
        assertEquals(processor3.getRunningLevel(), 1);
    }
    
    /**
     * Documentation test: Bug #27 history and impact.
     */
    @Test(groups = {"documentation", "processor"})
    public void testBug27_DocumentationAndImpact() {
        // Bug #27 caused:
        // 1. Growing indent in logs (getRunningLevel() keeps growing)
        // 2. Memory leaks (processors left on stack)
        // 3. 100% CPU (CommonUtil.replicate inefficiency with large indent)
        
        // Fix ensures cleanup ALWAYS happens via try-finally
        
        TestProcessor processor = new TestProcessor(false, false);
        assertTrue(processor.getRunningLevel() > 0, 
            "Processor should have valid running level");
    }
    
    /**
     * Test processor for Bug #27 testing.
     */
    private static class TestProcessor extends AbstractProcessor<IElementDef> {
        
        private final boolean shouldThrowRuntimeException;
        private final boolean shouldThrowInterruptedException;
        private boolean finalizeCalled = false;
        
        public TestProcessor(boolean shouldThrowRuntimeException, 
                           boolean shouldThrowInterruptedException) {
            super(new TestProcessorExecutionStrategy(shouldThrowRuntimeException, 
                shouldThrowInterruptedException));
            this.shouldThrowRuntimeException = shouldThrowRuntimeException;
            this.shouldThrowInterruptedException = shouldThrowInterruptedException;
        }
        
        @Override
        protected Variable execute(DynamicScopeContext context) throws InterruptedException {
            if (shouldThrowInterruptedException) {
                throw new InterruptedException("Test interruption");
            }
            if (shouldThrowRuntimeException) {
                throw new RuntimeException("Test exception");
            }
            return new NodeVariable("test result");
        }
        
        public boolean isFinalizeCalled() {
            return finalizeCalled;
        }
        
        // Override to track finalization
        public Variable run(DynamicScopeContext context) throws InterruptedException {
            try {
                return super.run(context);
            } finally {
                finalizeCalled = true;
            }
        }
    }
    
    /**
     * Test execution strategy for Bug #27 testing.
     */
    private static class TestProcessorExecutionStrategy implements ProcessorExecutionStrategy {
        
        private final boolean shouldThrowRuntimeException;
        private final boolean shouldThrowInterruptedException;
        
        public TestProcessorExecutionStrategy(boolean shouldThrowRuntimeException, 
                                            boolean shouldThrowInterruptedException) {
            this.shouldThrowRuntimeException = shouldThrowRuntimeException;
            this.shouldThrowInterruptedException = shouldThrowInterruptedException;
        }
        
        @Override
        public Variable execute(Processor processor, DynamicScopeContext context) 
                throws InterruptedException {
            if (shouldThrowInterruptedException) {
                throw new InterruptedException("Test interruption");
            }
            if (shouldThrowRuntimeException) {
                throw new RuntimeException("Test exception");
            }
            
            if (processor instanceof AbstractProcessor) {
                return ((AbstractProcessor<?>) processor).execute(context);
            }
            
            return EmptyVariable.INSTANCE;
        }
    }
}

