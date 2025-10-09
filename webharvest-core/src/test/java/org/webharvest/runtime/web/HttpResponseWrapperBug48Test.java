package org.webharvest.runtime.web;

import org.apache.commons.httpclient.HttpMethodBase;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Test for Bug #48: Use getResponseBodyAsStream instead of getResponseBody
 * 
 * Issue: HttpResponseWrapper used getResponseBody() which buffers entire response
 * in memory, causing WARNING for large responses and potential OutOfMemoryError.
 * 
 * Fix: Use getResponseBodyAsStream() which returns a stream for efficient processing.
 */
public class HttpResponseWrapperBug48Test {

    /**
     * Test Bug #48: Should use getResponseBodyAsStream instead of getResponseBody
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testUsesStreamInsteadOfBuffer() throws Exception {
        HttpMethodBase mockMethod = mock(HttpMethodBase.class);
        
        // Prepare test data
        String responseData = "Test response content";
        InputStream stream = new ByteArrayInputStream(responseData.getBytes("UTF-8"));
        
        // Mock method to return stream (Bug #48 fix)
        when(mockMethod.getResponseBodyAsStream()).thenReturn(stream);
        when(mockMethod.getResponseCharSet()).thenReturn("UTF-8");
        when(mockMethod.getStatusCode()).thenReturn(200);
        when(mockMethod.getStatusText()).thenReturn("OK");
        when(mockMethod.getResponseHeaders()).thenReturn(new org.apache.commons.httpclient.Header[0]);
        
        // Create wrapper
        HttpResponseWrapper wrapper = new HttpResponseWrapper(mockMethod);
        
        // Read body as array
        byte[] body = wrapper.readBodyAsArray();
        
        // Verify stream was used (not getResponseBody())
        verify(mockMethod, times(1)).getResponseBodyAsStream();
        verify(mockMethod, never()).getResponseBody();  // Should NOT call this!
        
        // Verify data is correct
        assertEquals(new String(body, "UTF-8"), responseData);
        
        System.out.println("✅ Bug #48: Uses getResponseBodyAsStream() instead of getResponseBody()");
    }

    /**
     * Test that getBodyAsInputStream returns actual stream
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testGetBodyAsInputStream_ReturnsActualStream() throws Exception {
        HttpMethodBase mockMethod = mock(HttpMethodBase.class);
        
        String responseData = "Streaming response data";
        InputStream originalStream = new ByteArrayInputStream(responseData.getBytes("UTF-8"));
        
        when(mockMethod.getResponseBodyAsStream()).thenReturn(originalStream);
        when(mockMethod.getResponseCharSet()).thenReturn("UTF-8");
        when(mockMethod.getStatusCode()).thenReturn(200);
        when(mockMethod.getStatusText()).thenReturn("OK");
        when(mockMethod.getResponseHeaders()).thenReturn(new org.apache.commons.httpclient.Header[0]);
        
        HttpResponseWrapper wrapper = new HttpResponseWrapper(mockMethod);
        
        // Get stream
        InputStream bodyStream = wrapper.getBodyAsInputStream();
        
        // Verify stream is usable
        assertNotNull(bodyStream, "Stream should not be null");
        
        // Read from stream
        byte[] buffer = new byte[1024];
        int bytesRead = bodyStream.read(buffer);
        
        assertTrue(bytesRead > 0, "Should read data from stream");
        String readData = new String(buffer, 0, bytesRead, "UTF-8");
        assertEquals(readData, responseData);
        
        System.out.println("✅ Bug #48: getBodyAsInputStream() returns actual stream");
    }

    /**
     * REGRESSION: Backward compatibility - readBodyAsArray still works
     */
    @Test(groups = {"unit", "bugs", "regression"})
    public void testReadBodyAsArray_BackwardCompatible() throws Exception {
        HttpMethodBase mockMethod = mock(HttpMethodBase.class);
        
        String content = "Backward compatible response";
        InputStream stream = new ByteArrayInputStream(content.getBytes("UTF-8"));
        
        when(mockMethod.getResponseBodyAsStream()).thenReturn(stream);
        when(mockMethod.getResponseCharSet()).thenReturn("UTF-8");
        when(mockMethod.getStatusCode()).thenReturn(200);
        when(mockMethod.getStatusText()).thenReturn("OK");
        when(mockMethod.getResponseHeaders()).thenReturn(new org.apache.commons.httpclient.Header[0]);
        
        HttpResponseWrapper wrapper = new HttpResponseWrapper(mockMethod);
        byte[] body = wrapper.readBodyAsArray();
        
        assertEquals(new String(body, "UTF-8"), content);
        System.out.println("✅ Backward compatibility: readBodyAsArray() still works");
    }

    /**
     * REGRESSION: Empty response handling
     */
    @Test(groups = {"unit", "bugs", "regression", "edge-case"})
    public void testEmptyResponse_NoException() throws Exception {
        HttpMethodBase mockMethod = mock(HttpMethodBase.class);
        
        // Null stream (empty response)
        when(mockMethod.getResponseBodyAsStream()).thenReturn(null);
        when(mockMethod.getResponseCharSet()).thenReturn("UTF-8");
        when(mockMethod.getStatusCode()).thenReturn(204);  // No Content
        when(mockMethod.getStatusText()).thenReturn("No Content");
        when(mockMethod.getResponseHeaders()).thenReturn(new org.apache.commons.httpclient.Header[0]);
        
        HttpResponseWrapper wrapper = new HttpResponseWrapper(mockMethod);
        
        // Should handle null stream gracefully
        byte[] body = wrapper.readBodyAsArray();
        assertNotNull(body, "Should return empty array, not null");
        assertEquals(body.length, 0, "Empty response should return empty array");
        
        InputStream stream = wrapper.getBodyAsInputStream();
        assertNotNull(stream, "Stream should not be null for empty response");
        
        System.out.println("✅ Empty response handled gracefully");
    }

    /**
     * PERFORMANCE TEST: Large response doesn't cause buffer warning
     */
    @Test(groups = {"unit", "bugs", "performance"})
    public void testLargeResponse_NoBufferWarning() throws Exception {
        HttpMethodBase mockMethod = mock(HttpMethodBase.class);
        
        // Simulate large response (1 MB)
        byte[] largeData = new byte[1024 * 1024];  // 1 MB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        InputStream largeStream = new ByteArrayInputStream(largeData);
        
        when(mockMethod.getResponseBodyAsStream()).thenReturn(largeStream);
        when(mockMethod.getResponseCharSet()).thenReturn("UTF-8");
        when(mockMethod.getStatusCode()).thenReturn(200);
        when(mockMethod.getStatusText()).thenReturn("OK");
        when(mockMethod.getResponseHeaders()).thenReturn(new org.apache.commons.httpclient.Header[0]);
        
        HttpResponseWrapper wrapper = new HttpResponseWrapper(mockMethod);
        
        // This should NOT trigger buffer warning
        byte[] body = wrapper.readBodyAsArray();
        
        assertEquals(body.length, largeData.length, "Large response should be read completely");
        
        // Verify used stream, not getResponseBody()
        verify(mockMethod, times(1)).getResponseBodyAsStream();
        verify(mockMethod, never()).getResponseBody();
        
        System.out.println("✅ Large response (1MB) processed without buffer warning");
    }
}

