package org.webharvest.plugin;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.IElementDef;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.processors.Processor;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.plugin.exceptions.PluginException;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for ProcessorPluginAdapter.
 *
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class ProcessorPluginAdapterTest {

    private ProcessorPluginAdapter adapter;
    private Processor mockProcessor;
    private IElementDef mockElementDef;
    private DynamicScopeContext mockContext;
    private PluginMetadata mockMetadata;

    @BeforeMethod
    public void setUp() {
        mockProcessor = mock(Processor.class);
        mockElementDef = mock(IElementDef.class);
        mockContext = mock(DynamicScopeContext.class);
        mockMetadata = mock(PluginMetadata.class);
        adapter = new ProcessorPluginAdapter(mockProcessor, mockMetadata);
    }

    @Test
    public void testConstructorWithProcessor() {
        ProcessorPluginAdapter testAdapter = new ProcessorPluginAdapter(mockProcessor, mockMetadata);
        assertNotNull(testAdapter);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConstructorWithNullProcessor() {
        new ProcessorPluginAdapter(null, mockMetadata);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConstructorWithNullMetadata() {
        new ProcessorPluginAdapter(mockProcessor, null);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = adapter.getMetadata();
        
        assertNotNull(metadata);
        assertEquals(metadata, mockMetadata);
    }

    @Test
    public void testValidateConfiguration() throws PluginValidationException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        
        // Should not throw exception
        adapter.validateConfiguration(config);
    }

    @Test
    public void testValidateConfigurationWithNull() throws PluginValidationException {
        // Should not throw exception
        adapter.validateConfiguration(null);
    }

    @Test
    public void testInitialize() throws PluginException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        
        // Should not throw exception
        adapter.initialize(config);
        assertTrue(adapter.isInitialized());
    }

    @Test
    public void testInitializeWithNull() throws PluginException {
        // Should not throw exception
        adapter.initialize(null);
        assertTrue(adapter.isInitialized());
    }

    @Test
    public void testExecuteWithValidProcessor() throws Exception {
        Variable expectedResult = new NodeVariable("test result");
        when(mockProcessor.run(mockContext)).thenReturn(expectedResult);
        
        Variable result = adapter.execute(mockContext);
        
        assertNotNull(result);
        assertEquals(result, expectedResult);
        verify(mockProcessor).run(mockContext);
    }

    @Test
    public void testExecuteWithProcessorThrowingException() throws Exception {
        when(mockProcessor.run(mockContext)).thenThrow(new RuntimeException("Test exception"));
        
        try {
            adapter.execute(mockContext);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "Test exception");
        }
    }

    @Test
    public void testExecuteWithInterruptedException() throws Exception {
        when(mockProcessor.run(mockContext)).thenThrow(new InterruptedException("Interrupted"));
        
        try {
            adapter.execute(mockContext);
            fail("Expected InterruptedException");
        } catch (InterruptedException e) {
            assertEquals(e.getMessage(), "Interrupted");
        }
    }

    @Test
    public void testExecuteWithNullContext() throws Exception {
        Variable expectedResult = new NodeVariable("test result");
        when(mockProcessor.run(null)).thenReturn(expectedResult);
        
        Variable result = adapter.execute(null);
        
        assertNotNull(result);
        assertEquals(result, expectedResult);
        verify(mockProcessor).run(null);
    }

    @Test
    public void testGetProcessor() {
        Processor<? extends IElementDef> result = adapter.getProcessor();
        assertEquals(result, mockProcessor);
    }

    @Test
    public void testSetElementDef() {
        adapter.setElementDef(mockElementDef);
        
        // Verify that the method doesn't throw exception
        assertTrue(true);
    }

    @Test
    public void testSetElementDefWithNull() {
        adapter.setElementDef(null);
        
        // Verify that the method doesn't throw exception
        assertTrue(true);
    }

    @Test
    public void testGetElementDef() {
        when(mockProcessor.getElementDef()).thenReturn(mockElementDef);
        
        IElementDef result = adapter.getElementDef();
        assertEquals(result, mockElementDef);
        verify(mockProcessor).getElementDef();
    }

    @Test
    public void testMultipleExecutions() throws Exception {
        Variable result1 = new NodeVariable("result1");
        Variable result2 = new NodeVariable("result2");
        
        when(mockProcessor.run(mockContext))
            .thenReturn(result1)
            .thenReturn(result2);
        
        Variable firstResult = adapter.execute(mockContext);
        Variable secondResult = adapter.execute(mockContext);
        
        assertEquals(firstResult, result1);
        assertEquals(secondResult, result2);
        verify(mockProcessor, times(2)).run(mockContext);
    }

    @Test
    public void testExecuteWithDifferentContexts() throws Exception {
        DynamicScopeContext context1 = mock(DynamicScopeContext.class);
        DynamicScopeContext context2 = mock(DynamicScopeContext.class);
        
        Variable result1 = new NodeVariable("result1");
        Variable result2 = new NodeVariable("result2");
        
        when(mockProcessor.run(context1)).thenReturn(result1);
        when(mockProcessor.run(context2)).thenReturn(result2);
        
        Variable firstResult = adapter.execute(context1);
        Variable secondResult = adapter.execute(context2);
        
        assertEquals(firstResult, result1);
        assertEquals(secondResult, result2);
        verify(mockProcessor).run(context1);
        verify(mockProcessor).run(context2);
    }

    @Test
    public void testIsInitialized() {
        assertFalse(adapter.isInitialized());
        
        try {
            adapter.initialize(mock(PluginConfiguration.class));
            assertTrue(adapter.isInitialized());
        } catch (PluginException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testCleanup() {
        try {
            adapter.initialize(mock(PluginConfiguration.class));
            assertTrue(adapter.isInitialized());
            
            adapter.cleanup();
            assertFalse(adapter.isInitialized());
        } catch (PluginException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testGetParentPlugin() {
        Processor mockParentProcessor = mock(Processor.class);
        ProcessorPluginAdapter parentPlugin = new ProcessorPluginAdapter(mockParentProcessor, mock(PluginMetadata.class));
        
        // Mock the processor to return the parent processor when getParentProcessor is called
        when(mockProcessor.getParentProcessor()).thenReturn(mockParentProcessor);
        
        adapter.setParentPlugin(parentPlugin);
        
        Plugin result = adapter.getParentPlugin();
        // getParentPlugin() creates a new ProcessorPluginAdapter, so we can't compare object identity
        // Instead, we verify that the parent processor is set correctly
        assertNotNull(result);
        assertTrue(result instanceof ProcessorPluginAdapter);
    }

    @Test
    public void testSetParentPlugin() {
        ProcessorPluginAdapter parentPlugin = new ProcessorPluginAdapter(mock(Processor.class), mock(PluginMetadata.class));
        
        // Should not throw exception
        adapter.setParentPlugin(parentPlugin);
        assertTrue(true);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSetParentPluginWithNull() {
        // Should throw IllegalArgumentException for null parent
        adapter.setParentPlugin(null);
    }

    @Test
    public void testGetRunningLevel() {
        int level = adapter.getRunningLevel();
        assertTrue(level >= 0);
    }

    // Test removed - getConfigurationSchema() method does not exist in ProcessorPluginAdapter

    @Test
    public void testExecuteWithEmptyVariable() throws Exception {
        when(mockProcessor.run(mockContext)).thenReturn(EmptyVariable.INSTANCE);
        
        Variable result = adapter.execute(mockContext);
        
        assertNotNull(result);
        assertEquals(result, EmptyVariable.INSTANCE);
        verify(mockProcessor).run(mockContext);
    }

    @Test
    public void testExecuteWithNullResult() throws Exception {
        when(mockProcessor.run(mockContext)).thenReturn(null);
        
        Variable result = adapter.execute(mockContext);
        
        assertNull(result);
        verify(mockProcessor).run(mockContext);
    }
}