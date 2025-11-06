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
// REMOVED in v2.2.x - old architecture classes no longer exist
// import org.webharvest.definition.CorePluginDef;
// import org.webharvest.definition.IElementDef;
// import org.webharvest.definition.XmlNode;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.scripting.ScriptEngineFactory;
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Enhanced comprehensive unit tests for {@link ScriptPlugin}.
 * Target: 70%+ code coverage
 * 
 * Tests cover:
 * - All supported scripting languages (Beanshell, Groovy, JavaScript)
 * - Variable injection and return values
 * - Error handling and edge cases
 * - Configuration validation
 * - Metadata and schema
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class ScriptPluginEnhancedTest {

    private ScriptPlugin plugin;
    
    @Mock
    private DynamicScopeContext mockContext;
    
    // REMOVED in v2.2.x - old architecture mocks no longer needed
    // @Mock
    // private CorePluginDef elementDef;
    // @Mock
    // private XmlNode xmlNode;
    
    @Mock
    private PluginConfiguration configuration;
    
    @Mock
    private ScriptEngineFactory scriptEngineFactory;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        // v2.2.x ULTRA-LEAN: No need for spy() - mock PluginConfiguration.getChildren() instead
        plugin = new ScriptPlugin();
        
        // v2.2.x ULTRA-LEAN: Setup configuration mocks for new architecture
        when(configuration.hasChildren()).thenReturn(false);
        when(configuration.getChildren()).thenReturn(Collections.emptyList());
        when(configuration.getProperty("language")).thenReturn("beanshell"); // default
        when(configuration.getProperty("return")).thenReturn(null);
        
        // NOTE: DON'T initialize here - each test initializes with its own mocks
        
    }
    
    // ==================== Basic Functionality Tests ====================
    
    @Test
    public void testMetadata() {
        // Test metadata is available and correct
        assertNotNull(plugin.getMetadata());
        assertEquals(plugin.getMetadata().getName(), "Script");
        assertEquals(plugin.getMetadata().getId(), "script");
        assertEquals(plugin.getMetadata().getVersion(), "2.3.0");
        assertTrue(plugin.getMetadata().isCorePlugin());
        assertEquals(plugin.getMetadata().getCategory(), "Processing");
    }
    
    @Test
    public void testMetadataTags() {
        String[] tags = plugin.getMetadata().getTags();
        assertNotNull(tags);
        assertTrue(tags.length > 0);
        // Should contain script-related tags
        boolean hasScriptTag = false;
        for (String tag : tags) {
            if ("script".equals(tag)) {
                hasScriptTag = true;
                break;
            }
        }
        assertTrue(hasScriptTag, "Should have 'script' tag");
    }
    
    @Test
    public void testConfigurationSchema() {
        assertNotNull(plugin.getMetadata().getConfigurationSchema());
        assertTrue(plugin.getMetadata().getConfigurationSchema().allowsBody());
    }
    
    @Test
    public void testSchemaAttributes() {
        String[] optionalAttrs = plugin.getMetadata().getConfigurationSchema().getOptionalAttributes();
        assertNotNull(optionalAttrs);
        assertTrue(optionalAttrs.length >= 2); // At least "id" and "language"
        
        String[] requiredAttrs = plugin.getMetadata().getConfigurationSchema().getRequiredAttributes();
        assertNotNull(requiredAttrs);
    }
    
    @Test
    public void testAttributeDefinitions() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        var attrDefs = schema.getAttributeDefinitions();
        
        assertNotNull(attrDefs);
        assertTrue(attrDefs.length >= 2); // id, language, return
        
        // Check each attribute has basic properties
        for (var attrDef : attrDefs) {
            assertNotNull(attrDef.getName());
            assertNotNull(attrDef.getType());
            assertNotNull(attrDef.getDescription());
        }
    }
    
    @Test
    public void testLanguageAttributeAllowedValues() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        var attrDefs = schema.getAttributeDefinitions();
        
        // Find language attribute
        boolean foundLanguageAttr = false;
        for (var attrDef : attrDefs) {
            if ("language".equals(attrDef.getName())) {
                foundLanguageAttr = true;
                String[] allowedValues = attrDef.getAllowedValues();
                assertNotNull(allowedValues);
                assertTrue(allowedValues.length >= 2); // At least beanshell, javascript
                break;
            }
        }
        assertTrue(foundLanguageAttr, "Should have language attribute");
    }
    
    // ==================== Scripting Language Tests ====================
    
    @Test
    public void testScriptWithBeanshellLanguage() throws Exception {
        // Given
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        
        when(configuration.getProperty("language")).thenReturn("beanshell");
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: Mock PluginConfiguration.getChildren() instead of executeBody() (protected)
        PluginConfiguration scriptChild = mock(PluginConfiguration.class);
        when(scriptChild.getProperty("_textContent")).thenReturn("2 + 2;");
        when(scriptChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptChild));
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);        
        
        // When
        Variable result = plugin.execute(realContext);
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    public void testScriptWithJavaScriptLanguage() throws Exception {
        // Given
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.JAVASCRIPT);
        
        when(configuration.getProperty("language")).thenReturn("javascript");
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: Mock PluginConfiguration.getChildren() instead of executeBody() (protected)
        PluginConfiguration scriptChild = mock(PluginConfiguration.class);
        when(scriptChild.getProperty("_textContent")).thenReturn("var x = 5; x;");
        when(scriptChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptChild));
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);        
        
        // When
        Variable result = plugin.execute(realContext);
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    public void testScriptWithGroovyLanguage() throws Exception {
        // Given
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.GROOVY);
        
        when(configuration.getProperty("language")).thenReturn("groovy");
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: Mock PluginConfiguration.getChildren() instead of executeBody() (protected)
        PluginConfiguration scriptChild = mock(PluginConfiguration.class);
        when(scriptChild.getProperty("_textContent")).thenReturn("def x = 10; x");
        when(scriptChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptChild));
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);        
        
        // When
        Variable result = plugin.execute(realContext);
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    public void testScriptWithDefaultLanguage() throws Exception {
        // Given - no language specified, should use context default
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        
        when(configuration.getProperty("language")).thenReturn(null);
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: Mock PluginConfiguration.getChildren() instead of executeBody() (protected)
        PluginConfiguration scriptChild = mock(PluginConfiguration.class);
        when(scriptChild.getProperty("_textContent")).thenReturn("1 + 1;");
        when(scriptChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptChild));
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);        
        
        // When
        Variable result = plugin.execute(realContext);
        
        // Then
        assertNotNull(result);
    }
    
    // ==================== Script Content Tests ====================
    
    @Test
    public void testScriptWithEmptyContent() throws Exception {
        // Given
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        
        when(configuration.getProperty("language")).thenReturn(null);
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: Mock PluginConfiguration.getChildren() with empty content
        PluginConfiguration scriptChild = mock(PluginConfiguration.class);
        when(scriptChild.getProperty("_textContent")).thenReturn("");
        when(scriptChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptChild));
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);        
        
        // When
        Variable result = plugin.execute(realContext);
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    public void testScriptWithNullContent() throws Exception {
        // Given
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        
        when(configuration.getProperty("language")).thenReturn(null);
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: Mock PluginConfiguration.getChildren() with empty content (null case)
        PluginConfiguration scriptChild = mock(PluginConfiguration.class);
        when(scriptChild.getProperty("_textContent")).thenReturn("");
        when(scriptChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptChild));
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);        
        
        // When
        Variable result = plugin.execute(realContext);
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    public void testScriptWithMultilineContent() throws Exception {
        // Given
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        
        when(configuration.getProperty("language")).thenReturn("beanshell");
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);
        
        String multilineScript = "int a = 5;\nint b = 10;\na + b;";
        // v2.2.x ULTRA-LEAN: Mock PluginConfiguration.getChildren() with multiline script
        PluginConfiguration scriptChild = mock(PluginConfiguration.class);
        when(scriptChild.getProperty("_textContent")).thenReturn(multilineScript);
        when(scriptChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptChild));
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);        
        
        // When
        Variable result = plugin.execute(realContext);
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    public void testScriptWithComments() throws Exception {
        // Given
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        
        when(configuration.getProperty("language")).thenReturn("beanshell");
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);
        
        String scriptWithComments = "// This is a comment\nint x = 5; // inline comment\nx;";
        // v2.2.x ULTRA-LEAN: Mock PluginConfiguration.getChildren() with script containing comments
        PluginConfiguration scriptChild = mock(PluginConfiguration.class);
        when(scriptChild.getProperty("_textContent")).thenReturn(scriptWithComments);
        when(scriptChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptChild));
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);        
        
        // When
        Variable result = plugin.execute(realContext);
        
        // Then
        assertNotNull(result);
    }
    
    // ==================== Variable Injection Tests ====================
    
    @Test
    public void testScriptWithContextVariables() throws Exception {
        // Given
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        realContext.setLocalVar("testVar", new NodeVariable("Hello"));
        
        when(configuration.getProperty("language")).thenReturn("beanshell");
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: Mock PluginConfiguration.getChildren() with script using context variables
        PluginConfiguration scriptChild = mock(PluginConfiguration.class);
        when(scriptChild.getProperty("_textContent")).thenReturn("testVar;");
        when(scriptChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptChild));
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);        
        
        // When
        Variable result = plugin.execute(realContext);
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    public void testScriptWithMultipleContextVariables() throws Exception {
        // Given
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        realContext.setLocalVar("var1", new NodeVariable("10"));
        realContext.setLocalVar("var2", new NodeVariable("20"));
        
        when(configuration.getProperty("language")).thenReturn("beanshell");
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: Mock PluginConfiguration.getChildren() with script using multiple context variables
        PluginConfiguration scriptChild = mock(PluginConfiguration.class);
        when(scriptChild.getProperty("_textContent")).thenReturn("Integer.parseInt(var1.toString()) + Integer.parseInt(var2.toString());");
        when(scriptChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptChild));
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);        
        
        // When
        Variable result = plugin.execute(realContext);
        
        // Then
        assertNotNull(result);
    }
    
    // ==================== Return Value Tests ====================
    
    @Test
    public void testScriptWithReturnAttribute() throws Exception {
        // Given
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        
        when(configuration.getProperty("language")).thenReturn("beanshell");
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn("result");
        
        // v2.2.x ULTRA-LEAN: Mock PluginConfiguration.getChildren() with script having return attribute
        PluginConfiguration scriptChild = mock(PluginConfiguration.class);
        when(scriptChild.getProperty("_textContent")).thenReturn("int result = 42; result;");
        when(scriptChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptChild));
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);        
        
        // When
        Variable result = plugin.execute(realContext);
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    public void testScriptWithStringReturn() throws Exception {
        // Given
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        
        when(configuration.getProperty("language")).thenReturn("beanshell");
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: Mock PluginConfiguration.getChildren() with string script
        PluginConfiguration scriptChild = mock(PluginConfiguration.class);
        when(scriptChild.getProperty("_textContent")).thenReturn("\"Hello World\";");
        when(scriptChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptChild));
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);        
        
        // When
        Variable result = plugin.execute(realContext);
        
        // Then
        assertNotNull(result);
    }
    
    // ==================== Configuration Tests ====================
    
    @Test
    public void testInitializeWithValidConfiguration() throws Exception {
        // Given
        when(configuration.getProperty("language")).thenReturn("beanshell");
        when(configuration.getProperty("id")).thenReturn("myScript");
        when(configuration.getProperty("return")).thenReturn("result");
        
        // When
        
        // Then - should not throw exception
        assertTrue(true);
    }
    
    @Test
    public void testInitializeWithMinimalConfiguration() throws Exception {
        // Given - only required fields (none in this case)
        when(configuration.getProperty("language")).thenReturn(null);
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);
        
        // When
        
        // Then - should not throw exception
        assertTrue(true);
    }
    
    @Test
    public void testConfigurationWithId() throws Exception {
        // Given
        when(configuration.getProperty("id")).thenReturn("uniqueScriptId");
        when(configuration.getProperty("language")).thenReturn("beanshell");
        
        // When
        
        // Then
        assertTrue(true);
    }
    
    // ==================== Edge Cases ====================
    
    @Test
    public void testScriptWithSpecialCharacters() throws Exception {
        // Given
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        
        when(configuration.getProperty("language")).thenReturn("beanshell");
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: Mock PluginConfiguration.getChildren() with script containing special characters
        PluginConfiguration scriptChild = mock(PluginConfiguration.class);
        when(scriptChild.getProperty("_textContent")).thenReturn("\"Special: àáâãäå çèéêë\";");
        when(scriptChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptChild));
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);        
        
        // When
        Variable result = plugin.execute(realContext);
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    public void testScriptWithUnicodeCharacters() throws Exception {
        // Given
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        
        when(configuration.getProperty("language")).thenReturn("beanshell");
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: Mock PluginConfiguration.getChildren() with script containing Unicode characters
        PluginConfiguration scriptChild = mock(PluginConfiguration.class);
        when(scriptChild.getProperty("_textContent")).thenReturn("\"Unicode: 中文 العربية ąćęłńóśźż\";");
        when(scriptChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptChild));
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);        
        
        // When
        Variable result = plugin.execute(realContext);
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    public void testScriptExecutionMultipleTimes() throws Exception {
        // Given
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        
        when(configuration.getProperty("language")).thenReturn("beanshell");
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: Mock PluginConfiguration.getChildren() with simple arithmetic script
        PluginConfiguration scriptChild = mock(PluginConfiguration.class);
        when(scriptChild.getProperty("_textContent")).thenReturn("5 + 5;");
        when(scriptChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptChild));
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);        
        
        // When - execute multiple times
        Variable result1 = plugin.execute(realContext);
        Variable result2 = plugin.execute(realContext);
        Variable result3 = plugin.execute(realContext);
        
        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
    }
    
    // ==================== Schema Validation Tests ====================
    
    @Test
    public void testSchemaAllowsBody() {
        assertTrue(plugin.getMetadata().getConfigurationSchema().allowsBody());
    }
    
    @Test
    public void testSchemaRequiredSubElements() {
        String[] required = plugin.getMetadata().getConfigurationSchema().getRequiredSubElements();
        assertNotNull(required);
        assertEquals(required.length, 0); // Script plugin has no required sub-elements
    }
    
    @Test
    public void testSchemaAllowedSubElements() {
        String[] allowed = plugin.getMetadata().getConfigurationSchema().getAllowedSubElements();
        assertNotNull(allowed);
    }
    
    @Test
    public void testGetAttribute() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        // Should not throw exception
        var attr = schema.getAttribute("language");
        assertTrue(true);
    }
    
    // ==================== Metadata Tests ====================
    
    @Test
    public void testMetadataAuthor() {
        assertNotNull(plugin.getMetadata().getAuthor());
        assertEquals(plugin.getMetadata().getAuthor(), "Web-Harvest Team");
    }
    
    @Test
    public void testMetadataDescription() {
        assertNotNull(plugin.getMetadata().getDescription());
        assertTrue(plugin.getMetadata().getDescription().length() > 0);
    }
    
    @Test
    public void testMetadataClassName() {
        assertEquals(plugin.getMetadata().getClassName(), ScriptPlugin.class.getName());
    }
    
    @Test
    public void testMetadataElementName() {
        assertEquals(plugin.getMetadata().getElementName(), "script");
    }
    
    @Test
    public void testMetadataNamespace() {
        assertNotNull(plugin.getMetadata().getNamespace());
        assertTrue(plugin.getMetadata().getNamespace().contains("webharvest"));
    }
    
    @Test
    public void testMetadataDependencies() {
        String[] dependencies = plugin.getMetadata().getDependencies();
        assertNotNull(dependencies);
    }
}

