/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.definition.IElementDef;
import org.webharvest.plugin.services.HttpServiceException;
import org.webharvest.plugin.services.HttpResponse;

/**
 * Unit tests for plugin interfaces and basic classes.
 * 
 * @author Robert Bala
 * @since 2.2.0-SNAPSHOT
 */
public class PluginInterfacesTest {
    
    private PluginMetadata mockMetadata;
    private PluginConfiguration mockConfig;
    private DynamicScopeContext mockContext;
    
    @BeforeMethod
    public void setUp() {
        mockMetadata = mock(PluginMetadata.class);
        mockConfig = mock(PluginConfiguration.class);
        mockContext = mock(DynamicScopeContext.class);
    }
    
    @Test
    public void testPluginMetadata() {
        when(mockMetadata.getId()).thenReturn("test-plugin");
        when(mockMetadata.getName()).thenReturn("Test Plugin");
        when(mockMetadata.getVersion()).thenReturn("1.0.0");
        when(mockMetadata.getDescription()).thenReturn("Test plugin description");
        when(mockMetadata.getAuthor()).thenReturn("Test Author");
        when(mockMetadata.getCategory()).thenReturn("test");
        when(mockMetadata.getTags()).thenReturn(new String[]{"test", "plugin"});
        when(mockMetadata.getDependencies()).thenReturn(new String[0]);
        when(mockMetadata.getConfigurationSchema()).thenReturn(null);
        when(mockMetadata.getNamespace()).thenReturn("http://test.org");
        when(mockMetadata.getElementName()).thenReturn("test");
        when(mockMetadata.isCorePlugin()).thenReturn(false);
        when(mockMetadata.getClassName()).thenReturn("TestPlugin");
        
        assertEquals(mockMetadata.getId(), "test-plugin");
        assertEquals(mockMetadata.getName(), "Test Plugin");
        assertEquals(mockMetadata.getVersion(), "1.0.0");
        assertEquals(mockMetadata.getDescription(), "Test plugin description");
        assertEquals(mockMetadata.getAuthor(), "Test Author");
        assertEquals(mockMetadata.getCategory(), "test");
        assertTrue(java.util.Arrays.equals(mockMetadata.getTags(), new String[]{"test", "plugin"}));
        assertTrue(java.util.Arrays.equals(mockMetadata.getDependencies(), new String[0]));
        assertNull(mockMetadata.getConfigurationSchema());
        assertEquals(mockMetadata.getNamespace(), "http://test.org");
        assertEquals(mockMetadata.getElementName(), "test");
        assertFalse(mockMetadata.isCorePlugin());
        assertEquals(mockMetadata.getClassName(), "TestPlugin");
    }
    
    @Test
    public void testPluginConfiguration() {
        when(mockConfig.getProperty("test.key")).thenReturn("test.value");
        when(mockConfig.getProperty("nonexistent.key")).thenReturn(null);
        when(mockConfig.getProperty("test.key", "default")).thenReturn("test.value");
        when(mockConfig.getProperty("nonexistent.key", "default")).thenReturn("default");
        when(mockConfig.hasProperty("test.key")).thenReturn(true);
        when(mockConfig.hasProperty("nonexistent.key")).thenReturn(false);
        
        assertEquals(mockConfig.getProperty("test.key"), "test.value");
        assertNull(mockConfig.getProperty("nonexistent.key"));
        assertEquals(mockConfig.getProperty("test.key", "default"), "test.value");
        assertEquals(mockConfig.getProperty("nonexistent.key", "default"), "default");
        assertTrue(mockConfig.hasProperty("test.key"));
        assertFalse(mockConfig.hasProperty("nonexistent.key"));
    }
    
    @Test
    public void testDefaultPluginConfiguration() {
        PluginConfiguration testConfig = new DefaultPluginConfiguration("test", "http://test.org", "test");
        
        // Test initial state
        assertFalse(testConfig.hasProperty("test.key"));
        assertNull(testConfig.getProperty("test.key"));
        assertEquals(testConfig.getProperty("test.key", "default"), "default");
    }
    
    @Test
    public void testBodyContentProvider() {
        IElementDef elementDef = mock(IElementDef.class);
        BodyContentProvider provider = new SimpleBodyContentProvider(elementDef);
        
        assertNotNull(provider);
        // SimpleBodyContentProvider should be testable
        assertTrue(provider instanceof BodyContentProvider);
    }
    
    @Test
    public void testExecutableBodyContentProvider() {
        IElementDef elementDef = mock(IElementDef.class);
        BodyContentProvider provider = new ExecutableBodyContentProvider(elementDef);
        
        assertNotNull(provider);
        // ExecutableBodyContentProvider should be testable
        assertTrue(provider instanceof BodyContentProvider);
    }
    
    @Test
    public void testDefaultBodyContentProvider() {
        IElementDef elementDef = mock(IElementDef.class);
        BodyContentProvider provider = new DefaultBodyContentProvider(elementDef);
        
        assertNotNull(provider);
        // DefaultBodyContentProvider should be testable
        assertTrue(provider instanceof BodyContentProvider);
    }
    
    @Test
    public void testPluginConfigurationSchema() {
        PluginConfigurationSchema schema = mock(PluginConfigurationSchema.class);
        PluginConfigurationSchema.AttributeDefinition[] emptyAttrArray = new PluginConfigurationSchema.AttributeDefinition[0];
        String[] emptyStringArray = new String[0];
        when(schema.getAttributeDefinitions()).thenReturn(emptyAttrArray);
        when(schema.allowsBody()).thenReturn(true);
        when(schema.getRequiredSubElements()).thenReturn(emptyStringArray);
        when(schema.getAllowedSubElements()).thenReturn(emptyStringArray);
        
        assertTrue(java.util.Arrays.equals(schema.getAttributeDefinitions(), emptyAttrArray));
        assertTrue(schema.allowsBody());
        assertTrue(java.util.Arrays.equals(schema.getRequiredSubElements(), emptyStringArray));
        assertTrue(java.util.Arrays.equals(schema.getAllowedSubElements(), emptyStringArray));
    }
    
    @Test
    public void testHttpServiceException() {
        String message = "HTTP service error";
        HttpServiceException exception = new HttpServiceException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof Exception);
    }
    
    @Test
    public void testHttpServiceExceptionWithCause() {
        String message = "HTTP service error";
        Throwable cause = new RuntimeException("Root cause");
        HttpServiceException exception = new HttpServiceException(message, cause);
        
        assertEquals(exception.getMessage(), message);
        assertEquals(exception.getCause(), cause);
    }
    
    @Test
    public void testHttpResponse() {
        HttpResponse response = mock(HttpResponse.class);
        when(response.getStatusCode()).thenReturn(200);
        when(response.getBody()).thenReturn("test body");
        when(response.getHeaders()).thenReturn(new java.util.HashMap<>());
        
        assertEquals(response.getStatusCode(), 200);
        assertEquals(response.getBody(), "test body");
        assertNotNull(response.getHeaders());
    }
}
