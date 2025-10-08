package org.webharvest.runtime.processors;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Test class for DefaultProcessorExecutionStrategy.
 */
public class DefaultProcessorExecutionStrategyTest {

    private DefaultProcessorExecutionStrategy strategy;
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        strategy = new DefaultProcessorExecutionStrategy();
        context = mock(DynamicScopeContext.class);
    }

    @Test
    public void testExecuteWithAbstractProcessor() throws InterruptedException {
        // Given
        AbstractProcessor<?> abstractProcessor = mock(AbstractProcessor.class);
        Variable expectedResult = new NodeVariable("test-result");
        when(abstractProcessor.execute(context)).thenReturn(expectedResult);
        
        // When
        Variable result = strategy.execute(abstractProcessor, context);
        
        // Then
        assertEquals(result, expectedResult);
        verify(abstractProcessor).execute(context);
    }

    @Test
    public void testExecuteWithNonAbstractProcessor() throws InterruptedException {
        // Given
        Processor nonAbstractProcessor = mock(Processor.class);
        Variable expectedResult = new NodeVariable("test-result");
        when(nonAbstractProcessor.run(context)).thenReturn(expectedResult);
        
        // When
        Variable result = strategy.execute(nonAbstractProcessor, context);
        
        // Then
        assertEquals(result, expectedResult);
        verify(nonAbstractProcessor).run(context);
    }

    @Test
    public void testExecuteWithInterruptedThread() throws InterruptedException {
        // Given
        Processor processor = mock(Processor.class);
        Thread.currentThread().interrupt();
        
        // When & Then
        try {
            strategy.execute(processor, context);
            fail("Expected InterruptedException");
        } catch (InterruptedException e) {
            // Expected
        } finally {
            Thread.interrupted(); // Clear interrupt flag
        }
    }

    @Test
    public void testExecuteWithNullProcessor() throws InterruptedException {
        // When & Then
        try {
            strategy.execute(null, context);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public void testExecuteWithNullContext() throws InterruptedException {
        // Given
        Processor processor = mock(Processor.class);
        
        // When & Then
        try {
            strategy.execute(processor, null);
            fail("Expected exception");
        } catch (Exception e) {
            // Expected - any exception is fine
            assertNotNull(e);
        }
    }

    @Test
    public void testExecuteWithAbstractProcessorThrowingException() throws InterruptedException {
        // Given
        AbstractProcessor<?> abstractProcessor = mock(AbstractProcessor.class);
        when(abstractProcessor.execute(context)).thenThrow(new InterruptedException("Test exception"));
        
        // When & Then
        try {
            strategy.execute(abstractProcessor, context);
            fail("Expected InterruptedException");
        } catch (InterruptedException e) {
            assertEquals(e.getMessage(), "Test exception");
        }
    }

    @Test
    public void testExecuteWithNonAbstractProcessorThrowingException() throws InterruptedException {
        // Given
        Processor nonAbstractProcessor = mock(Processor.class);
        when(nonAbstractProcessor.run(context)).thenThrow(new InterruptedException("Test exception"));
        
        // When & Then
        try {
            strategy.execute(nonAbstractProcessor, context);
            fail("Expected InterruptedException");
        } catch (InterruptedException e) {
            assertEquals(e.getMessage(), "Test exception");
        }
    }
}
