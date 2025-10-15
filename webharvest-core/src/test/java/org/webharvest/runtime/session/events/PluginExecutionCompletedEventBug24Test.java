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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.runtime.session.SessionStatus;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.plugin.core.HttpPlugin;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

/**
 * TDD tests for {@link PluginExecutionCompletedEvent}.
 * 
 * <h3>Bug #24: Event Sourcing for Inter-Processor Communication</h3>
 * <p>Tests verify that plugin execution completed events contain:</p>
 * <ul>
 *   <li>Plugin name and class</li>
 *   <li>Execution time in milliseconds</li>
 *   <li>Result variable (optional)</li>
 *   <li>Session context</li>
 * </ul>
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class PluginExecutionCompletedEventBug24Test {
    
    private ScraperSession mockSession;
    private Variable mockResult;
    
    @BeforeMethod
    public void setUp() {
        mockSession = mock(ScraperSession.class);
        when(mockSession.getSessionId()).thenReturn("test-session-123");
        when(mockSession.getClientId()).thenReturn("test-client");
        when(mockSession.getStatus()).thenReturn(SessionStatus.RUNNING);
        
        mockResult = new NodeVariable("<result>test</result>");
    }
    
    /**
     * RED: Test that event can be created with all parameters.
     */
    @Test
    public void testCreateEvent_WithAllParameters_Success() {
        // GIVEN
        String pluginName = "http";
        Class<?> pluginClass = HttpPlugin.class;
        long executionTimeMs = 150L;
        
        // WHEN
        PluginExecutionCompletedEvent event = new PluginExecutionCompletedEvent(
            mockSession, 
            pluginName, 
            pluginClass,
            executionTimeMs,
            mockResult
        );
        
        // THEN
        assertNotNull(event, "Event should be created");
        assertEquals(event.getPluginName(), pluginName, "Plugin name should match");
        assertEquals(event.getPluginClass(), pluginClass, "Plugin class should match");
        assertEquals(event.getExecutionTimeMs(), executionTimeMs, "Execution time should match");
        assertEquals(event.getResult(), mockResult, "Result should match");
        assertEquals(event.getSessionId(), "test-session-123", "Session ID should match");
        assertNotNull(event.getTimestamp(), "Timestamp should be set");
    }
    
    /**
     * RED: Test that event can be created with null result (some plugins don't return values).
     */
    @Test
    public void testCreateEvent_WithNullResult_Success() {
        // WHEN
        PluginExecutionCompletedEvent event = new PluginExecutionCompletedEvent(
            mockSession, 
            "http", 
            HttpPlugin.class,
            100L,
            null  // null result is valid
        );
        
        // THEN
        assertNotNull(event, "Event should be created");
        assertNull(event.getResult(), "Result should be null");
    }
    
    /**
     * RED: Test that event creation fails with negative execution time.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateEvent_NegativeExecutionTime_ThrowsException() {
        // WHEN
        new PluginExecutionCompletedEvent(
            mockSession, 
            "http", 
            HttpPlugin.class,
            -1L,  // negative time is invalid
            mockResult
        );
        
        // THEN - exception expected
    }
    
    /**
     * RED: Test that event creation fails with null session.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateEvent_NullSession_ThrowsException() {
        // WHEN
        new PluginExecutionCompletedEvent(null, "http", HttpPlugin.class, 100L, mockResult);
        
        // THEN - exception expected
    }
    
    /**
     * RED: Test that event creation fails with null plugin name.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateEvent_NullPluginName_ThrowsException() {
        // WHEN
        new PluginExecutionCompletedEvent(mockSession, null, HttpPlugin.class, 100L, mockResult);
        
        // THEN - exception expected
    }
    
    /**
     * RED: Test that event creation fails with null plugin class.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateEvent_NullPluginClass_ThrowsException() {
        // WHEN
        new PluginExecutionCompletedEvent(mockSession, "http", null, 100L, mockResult);
        
        // THEN - exception expected
    }
    
    /**
     * RED: Test toString() for debugging.
     */
    @Test
    public void testToString_ContainsAllInfo() {
        // GIVEN
        PluginExecutionCompletedEvent event = new PluginExecutionCompletedEvent(
            mockSession, 
            "http", 
            HttpPlugin.class,
            150L,
            mockResult
        );
        
        // WHEN
        String result = event.toString();
        
        // THEN
        assertNotNull(result, "toString should not be null");
        assertTrue(result.contains("PluginExecutionCompletedEvent"), 
            "Should contain event class name");
        assertTrue(result.contains("http"), "Should contain plugin name");
        assertTrue(result.contains("150"), "Should contain execution time");
        assertTrue(result.contains("test-session-123"), "Should contain session ID");
    }
    
    /**
     * RED: Test that execution time is properly recorded for metrics.
     */
    @Test
    public void testExecutionTime_ForMetricsRecording() {
        // GIVEN - fast execution
        PluginExecutionCompletedEvent fastEvent = new PluginExecutionCompletedEvent(
            mockSession, "http", HttpPlugin.class, 10L, mockResult);
        
        // AND - slow execution
        PluginExecutionCompletedEvent slowEvent = new PluginExecutionCompletedEvent(
            mockSession, "http", HttpPlugin.class, 5000L, mockResult);
        
        // THEN
        assertTrue(fastEvent.getExecutionTimeMs() < 100, "Fast event should be < 100ms");
        assertTrue(slowEvent.getExecutionTimeMs() > 1000, "Slow event should be > 1000ms");
    }
}

