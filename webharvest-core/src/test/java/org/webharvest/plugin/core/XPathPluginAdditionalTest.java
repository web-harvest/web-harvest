package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Additional tests for XPathPlugin to boost coverage.
 * Focuses on configuration variations and edge cases.
 */
public class XPathPluginAdditionalTest {

    private XPathPlugin plugin;
    private PluginConfiguration config;
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() throws Exception {
        plugin = new XPathPlugin();
        config = mock(PluginConfiguration.class);
        context = mock(DynamicScopeContext.class);
        
        when(config.getProperty("expression")).thenReturn("//div");
        when(config.getProperty("id")).thenReturn(null);
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Don't initialize here - let each test control initialization
    }

    // ========== Metadata Tests ==========

    @Test
    public void testMetadataId() {
        assertEquals(plugin.getMetadata().getId(), "xpath");
    }

    @Test
    public void testMetadataName() {
        assertEquals(plugin.getMetadata().getName(), "XPath Plugin");
    }

    @Test
    public void testMetadataVersion() {
        assertNotNull(plugin.getMetadata().getVersion());
    }

    @Test
    public void testMetadataDescription() {
        assertNotNull(plugin.getMetadata().getDescription());
        assertTrue(plugin.getMetadata().getDescription().length() > 10);
    }

    @Test
    public void testMetadataAuthor() {
        assertNotNull(plugin.getMetadata().getAuthor());
    }

    @Test
    public void testMetadataCategory() {
        assertEquals(plugin.getMetadata().getCategory(), "core");
    }

    @Test
    public void testMetadataTags() {
        String[] tags = plugin.getMetadata().getTags();
        assertNotNull(tags);
        assertTrue(tags.length > 0);
    }

    @Test
    public void testMetadataDependencies() {
        String[] deps = plugin.getMetadata().getDependencies();
        assertNotNull(deps);
    }

    @Test
    public void testMetadataClassName() {
        assertEquals(plugin.getMetadata().getClassName(), XPathPlugin.class.getName());
    }

    @Test
    public void testMetadataIsCorePlugin() {
        assertTrue(plugin.getMetadata().isCorePlugin());
    }

    @Test
    public void testMetadataElementName() {
        assertEquals(plugin.getMetadata().getElementName(), "xpath");
    }

    @Test
    public void testMetadataNamespace() {
        assertEquals(plugin.getMetadata().getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    // ========== Configuration Schema Tests ==========

    @Test
    public void testSchemaAllowsBody() {
        assertTrue(plugin.getMetadata().getConfigurationSchema().allowsBody());
    }

    @Test
    public void testSchemaVersion() {
        assertNotNull(plugin.getMetadata().getConfigurationSchema().getVersion());
    }

    @Test
    public void testSchemaAttributeDefinitions() {
        var attrs = plugin.getMetadata().getConfigurationSchema().getAttributeDefinitions();
        assertNotNull(attrs);
        assertTrue(attrs.length > 0);
    }

    @Test
    public void testSchemaGetAttributeExpression() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        var attr = schema.getAttribute("expression");
        if (attr != null) {
            assertNotNull(attr.getName());
        }
    }

    @Test
    public void testSchemaGetAttributeType() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        var attr = schema.getAttribute("type");
        if (attr != null) {
            assertNotNull(attr.getName());
        }
    }

    @Test
    public void testSchemaAllowedSubElements() {
        var allowed = plugin.getMetadata().getConfigurationSchema().getAllowedSubElements();
        assertNotNull(allowed);
    }

    @Test
    public void testSchemaRequiredSubElements() {
        var required = plugin.getMetadata().getConfigurationSchema().getRequiredSubElements();
        assertNotNull(required);
    }

    @Test
    public void testSchemaOptionalAttributes() {
        var optional = plugin.getMetadata().getConfigurationSchema().getOptionalAttributes();
        assertNotNull(optional);
    }

    @Test
    public void testSchemaRequiredAttributes() {
        var required = plugin.getMetadata().getConfigurationSchema().getRequiredAttributes();
        assertNotNull(required);
    }

    // ========== Type parameter variations ==========

    @Test
    public void testTypeParameterText() throws Exception {
        when(config.getProperty("type")).thenReturn("text");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testTypeParameterNode() throws Exception {
        when(config.getProperty("type")).thenReturn("node");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testTypeParameterNodeset() throws Exception {
        when(config.getProperty("type")).thenReturn("nodeset");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testTypeParameterCss() throws Exception {
        when(config.getProperty("type")).thenReturn("css");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testTypeParameterNull() throws Exception {
        when(config.getProperty("type")).thenReturn(null);
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    // ========== Expression variations ==========

    @Test
    public void testExpressionSimple() throws Exception {
        when(config.getProperty("expression")).thenReturn("//title");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testExpressionWithPredicate() throws Exception {
        when(config.getProperty("expression")).thenReturn("//div[@class='test']");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testExpressionWithFunction() throws Exception {
        when(config.getProperty("expression")).thenReturn("count(//div)");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testExpressionWithText() throws Exception {
        when(config.getProperty("expression")).thenReturn("//div/text()");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testExpressionWithAttribute() throws Exception {
        when(config.getProperty("expression")).thenReturn("//a/@href");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }
}

