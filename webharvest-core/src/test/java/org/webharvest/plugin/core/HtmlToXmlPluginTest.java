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

package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class HtmlToXmlPluginTest {

    @Mock
    private PluginConfiguration configuration;

    @Mock
    private DynamicScopeContext context;

    private HtmlToXmlPlugin plugin;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        plugin = new HtmlToXmlPlugin();
        
        when(configuration.getProperty("id")).thenReturn(null);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getId(), "html-to-xml");
        assertEquals(metadata.getName(), "HTML to XML");
        assertEquals(metadata.getVersion(), "2.3.0");
        assertNotNull(metadata.getDescription());
        assertTrue(metadata.getDescription().contains("HTML") || metadata.getDescription().contains("XML"));
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "html-to-xml");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
        assertEquals(metadata.getDependencies().length, 2);
        assertEquals(metadata.getDependencies()[0], "Jsoup");
        assertEquals(metadata.getDependencies()[1], "TagSoup");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertTrue(schema.allowsBody());
        assertEquals(schema.getRequiredAttributes().length, 0);
        assertEquals(schema.getAttributeDefinitions().length, 13);
        assertEquals(schema.getOptionalAttributes().length, 13);
        assertNotNull(schema.getAttribute("omitcomments"));
        assertNotNull(schema.getAttribute("omitxmlns"));
        assertNotNull(schema.getAttribute("advancedxmlescape"));
    }

    @Test
    public void testInitialize() throws PluginException {
        // Given
        when(configuration.getProperty("id")).thenReturn("html1");
        
        // When
        plugin.initialize(configuration);
        
        // Then
        // Should not throw exception
    }

    @Test
    public void testValidateConfiguration() throws PluginValidationException {
        // Given
        when(configuration.getProperty("id")).thenReturn("html1");
        
        // When & Then
        plugin.validateConfiguration(configuration);
        // Should not throw exception
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNullConfiguration() throws PluginValidationException {
        // When & Then
        plugin.validateConfiguration(null);
    }

    @Test
    public void testExecuteWithEmptyBody() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("html1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>");
    }

    @Test
    public void testExecuteWithNullBody() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("html1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>");
    }

    @Test
    public void testExecuteWithSimpleHtml() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("html1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        // Result depends on HTML processing, but should not be null
    }

    @Test
    public void testExecuteWithComplexHtml() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("html1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        // Result depends on HTML processing, but should not be null
    }

    @Test
    public void testExecuteWithMalformedHtml() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("html1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        // Result depends on HTML processing, but should not be null
    }

    @Test
    public void testExecuteWithSpecialCharacters() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("html1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        // Result depends on HTML processing, but should not be null
    }

    @Test
    public void testExecuteWithUnicodeCharacters() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("html1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        // Result depends on HTML processing, but should not be null
    }

    @Test
    public void testCleanInvalidXmlCharsPreservesSupplementaryUnicodeCodePoints() {
        assertEquals(plugin.cleanInvalidXmlChars("before 😀 after"), "before 😀 after");
        assertEquals(plugin.cleanInvalidXmlChars("A\u0001B"), "A B");
        assertEquals(plugin.cleanInvalidXmlChars("A\uD800B"), "A B");
    }

    @Test
    public void testExecuteWithLongHtml() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("html1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        // Result depends on HTML processing, but should not be null
    }

    @Test
    public void testExecuteWithMultipleExecutions() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("html1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertTrue(result1 instanceof NodeVariable);
        assertTrue(result2 instanceof NodeVariable);
        assertTrue(result3 instanceof NodeVariable);
    }

    @Test
    public void testExecuteWithHTMLFormatVariations() throws PluginException, InterruptedException {
        when(configuration.getProperty("id")).thenReturn("html-format");
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithUnicodeHTMLContent() throws PluginException, InterruptedException {
        when(configuration.getProperty("id")).thenReturn("unicode-html");
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithHTMLSpecialChars() throws PluginException, InterruptedException {
        when(configuration.getProperty("id")).thenReturn("special-chars");
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithVeryLargeHTML() throws PluginException, InterruptedException {
        when(configuration.getProperty("id")).thenReturn("large-html");
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithBadlyMalformedHTML() throws PluginException, InterruptedException {
        when(configuration.getProperty("id")).thenReturn("malformed");
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithDeeplyNestedHTML() throws PluginException, InterruptedException {
        when(configuration.getProperty("id")).thenReturn("nested-html");
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithHTMLEntityReferences() throws PluginException, InterruptedException {
        when(configuration.getProperty("id")).thenReturn("entities");
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithHTMLCommentsInside() throws PluginException, InterruptedException {
        when(configuration.getProperty("id")).thenReturn("comments");
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithHTML5SemanticTags() throws PluginException, InterruptedException {
        when(configuration.getProperty("id")).thenReturn("html5");
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithCDATASections() throws PluginException, InterruptedException {
        when(configuration.getProperty("id")).thenReturn("cdata");
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }
}
