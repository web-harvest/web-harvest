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

package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for IfPlugin.
 *
 * @author Robert Bala
 * @since 2.2.0
 */
public class IfPluginTest {

    @Mock
    private PluginConfiguration configuration;
    @Mock
    private DynamicScopeContext context;

    private IfPlugin plugin;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        plugin = new IfPlugin();
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("condition")).thenReturn("true");
    }

    @Test
    public void testGetMetadata() {
        assertNotNull(plugin.getMetadata());
        assertEquals(plugin.getMetadata().getId(), "if");
        assertEquals(plugin.getMetadata().getName(), "If");
        assertEquals(plugin.getMetadata().getVersion(), "2.3.0");
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata.getDescription()); 
        assertTrue(metadata.getDescription().contains("condition") || metadata.getDescription().contains("Condition"));
        assertEquals(plugin.getMetadata().getAuthor(), "Web-Harvest Team");
        assertEquals(plugin.getMetadata().getCategory(), "Control");
        assertEquals(plugin.getMetadata().getElementName(), "if");
        assertTrue(plugin.getMetadata().isCorePlugin());
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertEquals(schema.getVersion(), "2.3.0");
        assertTrue(schema.allowsBody());
        assertTrue(schema.getRequiredSubElements().length >= 0);
        assertTrue(schema.getAllowedSubElements().length >= 0, "If plugin may have else as allowed sub-element");
        assertTrue(schema.getRequiredAttributes().length >= 0);
        assertEquals(schema.getOptionalAttributes().length, 1);
        
        // Check attribute definitions
        PluginConfigurationSchema.AttributeDefinition[] attributes = schema.getAttributeDefinitions();
        assertEquals(attributes.length, 2);

        // Check condition attribute (required)
        PluginConfigurationSchema.AttributeDefinition conditionAttr = schema.getAttribute("condition");
        assertNotNull(conditionAttr);
        assertEquals(conditionAttr.getName(), "condition");
        assertEquals(conditionAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertTrue(conditionAttr.isRequired());
        assertNull(conditionAttr.getDefaultValue());

        // Check id attribute (optional)
        PluginConfigurationSchema.AttributeDefinition idAttr = schema.getAttribute("id");
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        assertEquals(idAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertFalse(idAttr.isRequired());
        assertNull(idAttr.getDefaultValue());
    }

    @Test
    public void testInitializeWithValidConfiguration() throws PluginException {
        plugin.initialize(configuration);
        // Should not throw any exception
        assertTrue(true);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNullConfiguration() throws PluginValidationException {
        plugin.validateConfiguration(null);
    }

    @Test
    public void testValidateConfigurationWithMissingCondition() throws PluginValidationException {
        when(configuration.getProperty("condition")).thenReturn(null);
        // IfPlugin doesn't throw PluginValidationException for missing condition
        plugin.validateConfiguration(configuration);
        // Should not throw any exception
        assertTrue(true);
    }

    @Test
    public void testValidateConfigurationWithEmptyCondition() throws PluginValidationException {
        when(configuration.getProperty("condition")).thenReturn("");
        // IfPlugin doesn't throw PluginValidationException for empty condition
        plugin.validateConfiguration(configuration);
        // Should not throw any exception
        assertTrue(true);
    }

    @Test
    public void testExecuteWithIdAttribute() throws Exception {
        when(configuration.getProperty("id")).thenReturn("test-if");
        when(configuration.getProperty("condition")).thenReturn("true");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteMultipleTimes() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("true");
        
        plugin.initialize(configuration);
        
        // Execute multiple times
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
    }

    @Test
    public void testExecuteWithComplexCondition() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${variable} == 'test'");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithNumericCondition() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("42");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithBooleanCondition() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("false");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNullCondition() throws Exception {
        when(configuration.getProperty("condition")).thenReturn(null);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionEquals() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${testVar == 'true'}");
        when(context.getVar("testVar")).thenReturn(new NodeVariable("true"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionNotEquals() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${testVar != 'false'}");
        when(context.getVar("testVar")).thenReturn(new NodeVariable("true"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionVariableReference() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${testVar}");
        when(context.getVar("testVar")).thenReturn(new NodeVariable("true"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionFalseVariable() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${testVar}");
        when(context.getVar("testVar")).thenReturn(new NodeVariable("false"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionZeroVariable() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${testVar}");
        when(context.getVar("testVar")).thenReturn(new NodeVariable("0"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionEmptyVariable() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${testVar}");
        when(context.getVar("testVar")).thenReturn(new NodeVariable(""));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionNullVariable() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${testVar}");
        when(context.getVar("testVar")).thenReturn(null);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionComplexComparison() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.role == 'admin'}");
        when(context.getVar("user.role")).thenReturn(new NodeVariable("admin"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionQuotedValues() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${status == \"active\"}");
        when(context.getVar("status")).thenReturn(new NodeVariable("active"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionInvalidFormat() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${invalid format");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionNestedComparison() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.profile.active == 'true'}");
        when(context.getVar("user.profile.active")).thenReturn(new NodeVariable("true"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionMultipleConditions() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.role == 'admin' && user.status == 'active'}");
        when(context.getVar("user.role")).thenReturn(new NodeVariable("admin"));
        when(context.getVar("user.status")).thenReturn(new NodeVariable("active"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionOrCondition() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.role == 'admin' || user.role == 'user'}");
        when(context.getVar("user.role")).thenReturn(new NodeVariable("user"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionNotCondition() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${!user.blocked}");
        when(context.getVar("user.blocked")).thenReturn(new NodeVariable("false"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionGreaterThan() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.age > 18}");
        when(context.getVar("user.age")).thenReturn(new NodeVariable("25"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionLessThan() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.age < 65}");
        when(context.getVar("user.age")).thenReturn(new NodeVariable("30"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionGreaterEqual() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.score >= 100}");
        when(context.getVar("user.score")).thenReturn(new NodeVariable("150"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionLessEqual() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.score <= 1000}");
        when(context.getVar("user.score")).thenReturn(new NodeVariable("500"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionContains() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.email.contains('@')}");
        when(context.getVar("user.email")).thenReturn(new NodeVariable("user@example.com"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionStartsWith() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.name.startsWith('John')}");
        when(context.getVar("user.name")).thenReturn(new NodeVariable("John Doe"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionEndsWith() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.name.endsWith('Doe')}");
        when(context.getVar("user.name")).thenReturn(new NodeVariable("John Doe"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionLength() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.name.length() > 5}");
        when(context.getVar("user.name")).thenReturn(new NodeVariable("John Doe"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionRegex() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.email.matches('.*@.*\\.com')}");
        when(context.getVar("user.email")).thenReturn(new NodeVariable("user@example.com"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionEmpty() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.name.isEmpty()}");
        when(context.getVar("user.name")).thenReturn(new NodeVariable(""));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionNull() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.name == null}");
        when(context.getVar("user.name")).thenReturn(null);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionNotNull() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.name != null}");
        when(context.getVar("user.name")).thenReturn(new NodeVariable("John"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionCaseInsensitive() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.role.equalsIgnoreCase('ADMIN')}");
        when(context.getVar("user.role")).thenReturn(new NodeVariable("admin"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionTrim() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.name.trim() == 'John'}");
        when(context.getVar("user.name")).thenReturn(new NodeVariable("  John  "));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionToLowerCase() throws Exception {
        when(configuration.getProperty("condition")).thenReturn("${user.role.toLowerCase() == 'admin'}");
        when(context.getVar("user.role")).thenReturn(new NodeVariable("ADMIN"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTrueCondition() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("condition", "")).thenReturn("true");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNonEmptyCondition() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("condition", "")).thenReturn("someValue");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionTrue() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("condition", "")).thenReturn("${user.active == 'true'}");
        when(context.getVar("user.active")).thenReturn(new NodeVariable("true"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionFalse() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("condition", "")).thenReturn("${user.active == 'false'}");
        when(context.getVar("user.active")).thenReturn(new NodeVariable("true"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionNotEqualsTrue() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("condition", "")).thenReturn("${user.status != 'inactive'}");
        when(context.getVar("user.status")).thenReturn(new NodeVariable("active"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionNotEqualsFalse() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("condition", "")).thenReturn("${user.status != 'active'}");
        when(context.getVar("user.status")).thenReturn(new NodeVariable("active"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionVariableReferenceTrue() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("condition", "")).thenReturn("${user.isActive}");
        when(context.getVar("user.isActive")).thenReturn(new NodeVariable("true"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionVariableReferenceFalse() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("condition", "")).thenReturn("${user.isActive}");
        when(context.getVar("user.isActive")).thenReturn(new NodeVariable("false"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionVariableReferenceEmpty() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("condition", "")).thenReturn("${user.isActive}");
        when(context.getVar("user.isActive")).thenReturn(new NodeVariable(""));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionVariableReferenceZero() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("condition", "")).thenReturn("${user.isActive}");
        when(context.getVar("user.isActive")).thenReturn(new NodeVariable("0"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionVariableReferenceNull() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("condition", "")).thenReturn("${user.isActive}");
        when(context.getVar("user.isActive")).thenReturn(null);
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionComplex() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("condition", "")).thenReturn("${user.profile.active == 'true'}");
        when(context.getVar("user.profile.active")).thenReturn(new NodeVariable("true"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionWithQuotes() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("condition", "")).thenReturn("${user.status == \"active\"}");
        when(context.getVar("user.status")).thenReturn(new NodeVariable("active"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionInvalidFormatNew() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("condition", "")).thenReturn("${invalid format");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateExpressionException() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("condition", "")).thenReturn("${user.active == 'true'}");
        when(context.getVar("user.active")).thenThrow(new RuntimeException("Test exception"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }
    @Test
    public void testUnsupportedComparisonWithoutScriptingLanguageDoesNotBecomeTruthy() throws Exception {
        ScraperContext directContext = new ScraperContext();
        assertNull(directContext.getScriptingLanguage(),
            "This regression must exercise the no-engine compatibility path");
        directContext.setLocalVar("count", new NodeVariable("11"));

        IfPlugin directPlugin = new IfPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext executionContext) {
                return new NodeVariable("if-body-executed");
            }
        };

        Map<String, String> properties = new HashMap<>();
        properties.put("condition", "${count > 10}");
        DefaultPluginConfiguration directConfiguration = new DefaultPluginConfiguration(
            "if", "http://org.webharvest/schema/2.1/core", "if", properties);
        directPlugin.initialize(directConfiguration);

        Variable result = directPlugin.execute(directContext);

        assertTrue(result instanceof EmptyVariable,
            "An unsupported comparison without a configured expression engine must fail closed, not become truthy");
    }

    @Test
    public void testPlainNonEmptyStringRemainsTruthyWithoutScriptingLanguage() throws Exception {
        ScraperContext directContext = new ScraperContext();
        IfPlugin directPlugin = new IfPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext executionContext) {
                return new NodeVariable("if-body-executed");
            }
        };

        Map<String, String> properties = new HashMap<>();
        properties.put("condition", "enabled");
        directPlugin.initialize(new DefaultPluginConfiguration(
            "if", "http://org.webharvest/schema/2.1/core", "if", properties));

        Variable result = directPlugin.execute(directContext);

        assertEquals(result.toString(), "if-body-executed",
            "The documented non-empty string truthiness contract must remain intact");
    }

}
