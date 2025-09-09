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

package org.webharvest.plugin.schema;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.Collections;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.validation.SchemaResolver;
import org.webharvest.definition.validation.SchemaSource;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.mock.MockPlugin;
import org.webharvest.plugin.registry.PluginRegistry;

/**
 * Unit tests for PluginSchemaPostProcessor.
 *
 * @author Robert Bala
 * @since 2.1.0
 */
public class PluginSchemaPostProcessorTest {

    private PluginRegistry mockPluginRegistry;
    private SchemaResolver mockSchemaResolver;
    private PluginSchemaPostProcessor postProcessor;

    @BeforeMethod
    public void setUp() {
        mockPluginRegistry = mock(PluginRegistry.class);
        mockSchemaResolver = mock(SchemaResolver.class);
        postProcessor = new PluginSchemaPostProcessor(mockPluginRegistry);
    }

    @Test
    public void testPostProcessWithNoPlugins() {
        // Given
        when(mockPluginRegistry.getAllPlugins()).thenReturn(Collections.emptyList());

        // When
        postProcessor.postProcess(mockSchemaResolver);

        // Then
        verify(mockPluginRegistry).getAllPlugins();
        verifyNoInteractions(mockSchemaResolver);
    }

    @Test
    public void testPostProcessWithPlugins() {
        // Given
        Plugin mockPlugin1 = MockPlugin.create("test1", "Test Plugin 1")
                .whenExecute(ctx -> null);
        Plugin mockPlugin2 = MockPlugin.create("test2", "Test Plugin 2")
                .whenExecute(ctx -> null);

        when(mockPluginRegistry.getAllPlugins()).thenReturn(Arrays.asList(mockPlugin1, mockPlugin2));

        // When
        postProcessor.postProcess(mockSchemaResolver);

        // Then
        verify(mockPluginRegistry).getAllPlugins();
        // Verify that schema sources are registered (at least twice for both namespaces)
        verify(mockSchemaResolver, atLeast(2)).registerSchemaSource(any(SchemaSource.class));
    }

    @Test
    public void testPostProcessWithException() {
        // Given
        when(mockPluginRegistry.getAllPlugins()).thenThrow(new RuntimeException("Registry error"));

        // When - should not throw exception
        postProcessor.postProcess(mockSchemaResolver);

        // Then
        verify(mockPluginRegistry).getAllPlugins();
        verifyNoInteractions(mockSchemaResolver);
    }

    @Test
    public void testConstructor() {
        // When
        PluginSchemaPostProcessor processor = new PluginSchemaPostProcessor(mockPluginRegistry);

        // Then
        assertNotNull(processor);
    }

    @Test
    public void testConstructorWithNullRegistry() {
        // When - should create processor even with null registry
        PluginSchemaPostProcessor processor = new PluginSchemaPostProcessor(null);

        // Then
        assertNotNull(processor);
        
        // When post-processing with null registry, should handle gracefully
        processor.postProcess(mockSchemaResolver);
        
        // Should not interact with schema resolver when registry is null
        verifyNoInteractions(mockSchemaResolver);
    }
}
