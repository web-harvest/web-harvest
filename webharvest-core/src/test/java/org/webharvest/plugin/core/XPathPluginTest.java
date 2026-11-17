/*
 Copyright (c) 2006-2025 the original author or authors.
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
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for XPathPlugin.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class XPathPluginTest {

    @Mock
    private PluginConfiguration configuration;
    @Mock
    private DynamicScopeContext context;

    private XPathPlugin plugin;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        plugin = new XPathPlugin();
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("expression")).thenReturn("//test");
    }

    @Test
    public void testGetMetadata() {
        assertNotNull(plugin.getMetadata());
        assertEquals(plugin.getMetadata().getId(), "xpath");
        assertEquals(plugin.getMetadata().getName(), "XPath");
        assertEquals(plugin.getMetadata().getVersion(), "2.3.0");
        assertNotNull(plugin.getMetadata().getDescription());
        assertEquals(plugin.getMetadata().getAuthor(), "Web-Harvest Team");
        assertEquals(plugin.getMetadata().getCategory(), "Extraction");
        assertEquals(plugin.getMetadata().getElementName(), "xpath");
        assertTrue(plugin.getMetadata().isCorePlugin());
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertEquals(schema.getVersion(), "2.3.0");
        assertTrue(schema.allowsBody());
        assertEquals(schema.getRequiredSubElements().length, 0);
        assertEquals(schema.getAllowedSubElements().length, 0);
        assertEquals(schema.getRequiredAttributes().length, 0);
        assertTrue(schema.getOptionalAttributes().length >= 2, "XPath plugin should have at least 2 optional attributes (expression, type)");

        // Check attribute definitions
        PluginConfigurationSchema.AttributeDefinition[] attributes = schema.getAttributeDefinitions();
        assertEquals(attributes.length, 2);

        // Check id attribute (optional)
        PluginConfigurationSchema.AttributeDefinition idAttr = schema.getAttribute("id");
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        assertEquals(idAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertFalse(idAttr.isRequired());
        assertNull(idAttr.getDefaultValue());

        // Check expression attribute (optional)
        PluginConfigurationSchema.AttributeDefinition expressionAttr = schema.getAttribute("expression");
        assertNotNull(expressionAttr);
        assertEquals(expressionAttr.getName(), "expression");
        assertEquals(expressionAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertFalse(expressionAttr.isRequired());
        assertNull(expressionAttr.getDefaultValue());
    }

    @Test
    public void testInitializeWithValidConfiguration() throws PluginException {
        plugin.initialize(configuration);
        assertTrue(plugin.isInitialized(), "Valid configuration should initialize the plugin");
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNullConfiguration() throws PluginValidationException {
        plugin.validateConfiguration(null);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithMissingExpression() throws PluginValidationException {
        when(configuration.getProperty("expression")).thenReturn(null);
        plugin.validateConfiguration(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithEmptyExpression() throws PluginValidationException {
        when(configuration.getProperty("expression")).thenReturn("");
        plugin.validateConfiguration(configuration);
    }

    @Test
    public void testExecute() throws InterruptedException, PluginException {
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithIdAttribute() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("test-xpath");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteMultipleTimes() throws InterruptedException, PluginException {
        plugin.initialize(configuration);
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1 instanceof EmptyVariable);
        assertTrue(result2 instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithComplexExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div[@class='test']");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithSimpleExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("/root");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithAttributeExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//@id");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTextExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//text()");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNodeExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//node()");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithParentExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//parent::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithChildExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//child::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithSiblingExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//following-sibling::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithAncestorExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//ancestor::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithDescendantExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//descendant::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithPositionExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div[position()=1]");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithLastExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div[last()]");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithCountExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div[count(*)=2]");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithStringExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div[string-length()>5]");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithBooleanExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div[boolean(@class)]");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNumberExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div[number(@id)>0]");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithUnionExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div | //span");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithIntersectionExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div[.//span]");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNamespaceExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//*[namespace-uri()='http://example.com']");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithLocalNameExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//*[local-name()='div']");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNameExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//*[name()='div']");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithInvalidExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//invalid[");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithComplexXPathExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div[@class='test' and @id='main']/span[position()=1]");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithFunctions() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div[contains(@class, 'test') and starts-with(@id, 'main')]");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithPredicates() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div[1]/span[last()]/text()[normalize-space()]");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithAxes() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div/ancestor::body/descendant::span");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithWildcards() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//*[@*='value']");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithNamespaces() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//ns:div[@ns:class='test']");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithComments() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div[comment()]");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithProcessingInstructions() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//processing-instruction()");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithDocumentNode() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("/");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithRootNode() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("/*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithElementNode() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//element()");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithAttributeNode() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//attribute()");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithTextNode() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//text()");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithCommentNode() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//comment()");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithProcessingInstructionNode() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//processing-instruction()");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithNamespaceNode() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//namespace::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithSelfAxis() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div/self::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithChildAxis() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div/child::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithParentAxis() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div/parent::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithAncestorAxis() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div/ancestor::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithAncestorOrSelfAxis() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div/ancestor-or-self::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithDescendantAxis() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div/descendant::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithDescendantOrSelfAxis() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div/descendant-or-self::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithFollowingAxis() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div/following::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithFollowingSiblingAxis() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div/following-sibling::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithPrecedingAxis() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div/preceding::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithPrecedingSiblingAxis() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div/preceding-sibling::*");
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithXPathWithMultipleExecutions() throws InterruptedException, PluginException {
        when(configuration.getProperty("expression")).thenReturn("//div");
        plugin.initialize(configuration);
        
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertTrue(result1 instanceof EmptyVariable);
        assertTrue(result2 instanceof EmptyVariable);
        assertTrue(result3 instanceof EmptyVariable);
    }
}
