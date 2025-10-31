package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;
import static org.webharvest.plugin.core.MetadataTestUtils.expectedCategory;

/**
 * Additional tests for HtmlToXmlPlugin to boost coverage.
 * Simple metadata and configuration tests.
 */
public class HtmlToXmlPluginAdditionalTest {

    private HtmlToXmlPlugin plugin;
    private PluginConfiguration config;
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() throws Exception {
        plugin = new HtmlToXmlPlugin();
        config = mock(PluginConfiguration.class);
        context = mock(DynamicScopeContext.class);
        
        when(config.getProperty("id")).thenReturn(null);
        when(config.getProperty("charset")).thenReturn("UTF-8");
        when(context.getCharset()).thenReturn("UTF-8");
    }

    // ========== Metadata Tests ==========

    @Test
    public void testMetadataTags() {
        String[] tags = plugin.getMetadata().getTags();
        assertNotNull(tags);
        assertTrue(tags.length > 0);
    }

    @Test
    public void testMetadataAuthor() {
        assertNotNull(plugin.getMetadata().getAuthor());
    }

    @Test
    public void testMetadataCategory() {
        assertEquals(plugin.getMetadata().getCategory(), expectedCategory(HtmlToXmlPlugin.class));
    }

    @Test
    public void testMetadataDependencies() {
        String[] deps = plugin.getMetadata().getDependencies();
        assertNotNull(deps);
    }

    @Test
    public void testMetadataClassName() {
        assertEquals(plugin.getMetadata().getClassName(), HtmlToXmlPlugin.class.getName());
    }

    // ========== Configuration Schema Tests ==========

    @Test
    public void testSchemaGetAttribute() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        // Should handle null gracefully
        schema.getAttribute("nonexistent");
    }

    @Test
    public void testSchemaValidate() throws Exception {
        var schema = plugin.getMetadata().getConfigurationSchema();
        schema.validate(config);
        // Should not throw
    }

    @Test
    public void testSchemaAllowedSubElements() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        String[] allowed = schema.getAllowedSubElements();
        assertNotNull(allowed);
    }

    @Test
    public void testSchemaRequiredSubElements() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        String[] required = schema.getRequiredSubElements();
        assertNotNull(required);
    }

    @Test
    public void testSchemaOptionalAttributes() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        String[] optional = schema.getOptionalAttributes();
        assertNotNull(optional);
    }

    @Test
    public void testSchemaRequiredAttributes() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        String[] required = schema.getRequiredAttributes();
        assertNotNull(required);
    }

    @Test
    public void testSchemaVersion() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        assertNotNull(schema.getVersion());
    }

    @Test
    public void testSchemaAttributeDefinitions() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        var attrs = schema.getAttributeDefinitions();
        assertNotNull(attrs);
    }

    // ========== Different charset tests ==========

    @Test
    public void testCharsetISO88591() throws Exception {
        when(config.getProperty("charset")).thenReturn("ISO-8859-1");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testCharsetWindows1252() throws Exception {
        when(config.getProperty("charset")).thenReturn("windows-1252");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testCharsetUTF16() throws Exception {
        when(config.getProperty("charset")).thenReturn("UTF-16");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testCharsetNull() throws Exception {
        when(config.getProperty("charset")).thenReturn(null);
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    // ========== Different outputtype tests ==========

    @Test
    public void testOutputTypeSimple() throws Exception {
        when(config.getProperty("outputtype")).thenReturn("simple");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testOutputTypeBrowser() throws Exception {
        when(config.getProperty("outputtype")).thenReturn("browser");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testOutputTypeCompact() throws Exception {
        when(config.getProperty("outputtype")).thenReturn("compact");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testOutputTypePretty() throws Exception {
        when(config.getProperty("outputtype")).thenReturn("pretty");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testOutputTypeNull() throws Exception {
        when(config.getProperty("outputtype")).thenReturn(null);
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    // ========== ID parameter tests ==========

    @Test
    public void testIdParameter() throws Exception {
        when(config.getProperty("id")).thenReturn("test-id");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testIdParameterEmpty() throws Exception {
        when(config.getProperty("id")).thenReturn("");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testIdParameterWithSpecialChars() throws Exception {
        when(config.getProperty("id")).thenReturn("test-123_id");
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }
}

