package org.webharvest.runtime.processors;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Test class for ProcessorExecutionContext.
 */
public class ProcessorExecutionContextTest {

    private ProcessorExecutionContext executionContext;
    private Processor processor;
    private DynamicScopeContext context;
    private Map<String, Object> properties;
    private long startTime;
    private String processorId;

    @BeforeMethod
    public void setUp() {
        processor = mock(Processor.class);
        context = mock(DynamicScopeContext.class);
        properties = new HashMap<>();
        startTime = System.currentTimeMillis();
        processorId = "test-processor";
        
        executionContext = new ProcessorExecutionContext(processor, context, properties, startTime, processorId);
    }

    @Test
    public void testConstructor() {
        // When
        ProcessorExecutionContext newContext = new ProcessorExecutionContext(
            processor, context, properties, startTime, processorId);
        
        // Then
        assertNotNull(newContext);
        assertEquals(newContext.getProcessor(), processor);
        assertEquals(newContext.getContext(), context);
        assertEquals(newContext.getProperties(), properties);
        assertEquals(newContext.getStartTime(), startTime);
        assertEquals(newContext.getProcessorId(), processorId);
    }

    @Test
    public void testGetProcessor() {
        // When
        Processor result = executionContext.getProcessor();
        
        // Then
        assertEquals(result, processor);
    }

    @Test
    public void testGetContext() {
        // When
        DynamicScopeContext result = executionContext.getContext();
        
        // Then
        assertEquals(result, context);
    }

    @Test
    public void testGetProperties() {
        // When
        Map<String, Object> result = executionContext.getProperties();
        
        // Then
        assertEquals(result, properties);
    }

    @Test
    public void testGetStartTime() {
        // When
        long result = executionContext.getStartTime();
        
        // Then
        assertEquals(result, startTime);
    }

    @Test
    public void testGetProcessorId() {
        // When
        String result = executionContext.getProcessorId();
        
        // Then
        assertEquals(result, processorId);
    }

    @Test
    public void testGetExecutionTime() {
        // Given
        // Wait a small amount to ensure execution time > 0
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When
        long executionTime = executionContext.getExecutionTime();
        
        // Then
        assertTrue(executionTime >= 0);
    }

    @Test
    public void testGetExecutionTimeImmediately() {
        // When
        long executionTime = executionContext.getExecutionTime();
        
        // Then
        assertTrue(executionTime >= 0);
    }

    @Test
    public void testWithNullProcessorId() {
        // Given
        ProcessorExecutionContext contextWithNullId = new ProcessorExecutionContext(
            processor, context, properties, startTime, null);
        
        // When
        String result = contextWithNullId.getProcessorId();
        
        // Then
        assertNull(result);
    }

    @Test
    public void testWithEmptyProperties() {
        // Given
        Map<String, Object> emptyProperties = new HashMap<>();
        ProcessorExecutionContext contextWithEmptyProps = new ProcessorExecutionContext(
            processor, context, emptyProperties, startTime, processorId);
        
        // When
        Map<String, Object> result = contextWithEmptyProps.getProperties();
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}













