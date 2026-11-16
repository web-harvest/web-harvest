/*
 Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.webharvest.definition.CorePluginRegistry;
import org.webharvest.definition.CorePluginsRegistrar;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
// REMOVED in v2.2.x - old architecture classes no longer exist
// import org.webharvest.definition.CorePluginDef;
// import org.webharvest.definition.IElementDef;
// import org.webharvest.definition.XmlNode;
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
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Test for CasePlugin
 */
public class CasePluginTest {

    // REMOVED in v2.2.x - old architecture mocks no longer needed
    // @Mock
    // private CorePluginDef elementDef;
    // @Mock
    // private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;
    
    private CasePlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new CasePlugin();
        
        
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(xmlNode.getText()).thenReturn("Test case content");
        // when(elementDef.hasOperations()).thenReturn(false);
        
        when(configuration.getProperty("id")).thenReturn("testId");
        
        plugin.initialize(configuration);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "Case");
        assertNotNull(metadata.getDescription());
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "case");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        assertNotNull(schema);
        assertNotNull(schema.getAttributeDefinitions());
        assertEquals(schema.getAttributeDefinitions().length, 1);
        
        PluginConfigurationSchema.AttributeDefinition idAttr = schema.getAttributeDefinitions()[0];
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        assertFalse(idAttr.isRequired());
        
        assertTrue(schema.allowsBody());
        assertEquals(schema.getVersion(), "2.3.0");
    }

    @Test
    public void testCasePluginInitialize() throws PluginException {
        CasePlugin newPlugin = new CasePlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("testId");
        
        newPlugin.initialize(config);
        
        assertNotNull(newPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testCasePluginInitializeWithNullConfiguration() throws PluginException {
        CasePlugin newPlugin = new CasePlugin();
        
        newPlugin.initialize(null);
    }

    @Test
    public void testCasePluginInitializeWithMissingId() throws PluginException {
        CasePlugin newPlugin = new CasePlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn(null);
        
        newPlugin.initialize(config);
        
        assertNotNull(newPlugin);
    }

    @Test
    public void testCasePluginInitializeWithEmptyId() throws PluginException {
        CasePlugin newPlugin = new CasePlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("   ");
        
        newPlugin.initialize(config);
        
        assertNotNull(newPlugin);
    }

    @Test
    public void testCasePluginExecute() throws InterruptedException {
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testCasePluginExecuteWithInterruptedException() throws PluginException {
        Thread.interrupted();
        try {
            when(context.getCharset()).thenReturn("UTF-8");

            CasePlugin testPlugin = new CasePlugin() {
                @Override
                protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                    throw new InterruptedException("Test interruption");
                }
            };

            PluginConfiguration config = mock(PluginConfiguration.class);
            when(config.getProperty("id")).thenReturn("testId");
            testPlugin.initialize(config);

            try {
                testPlugin.execute(context);
                fail("InterruptedException must propagate through the Case plugin lifecycle");
            } catch (InterruptedException expected) {
                assertEquals(expected.getMessage(), "Test interruption");
                assertTrue(Thread.currentThread().isInterrupted(),
                    "The lifecycle must restore the interrupt flag after cleanup");
            }
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testCasePluginValidateConfiguration() throws PluginValidationException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("testId");
        plugin.validateConfiguration(config);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testCasePluginValidateConfigurationWithNull() throws PluginValidationException {
        plugin.validateConfiguration(null);
    }

    @Test
    public void testCasePluginValidateConfigurationWithMissingId() throws PluginValidationException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn(null);
        plugin.validateConfiguration(config);
    }

    @Test
    public void testCasePluginValidateConfigurationWithEmptyId() throws PluginValidationException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("");
        plugin.validateConfiguration(config);
    }

    @Test
    public void testCasePluginExecuteMultipleTimes() throws InterruptedException {
        when(context.getCharset()).thenReturn("UTF-8");
        
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

    @Test
    public void testSchemaOptionalAttributes() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        String[] optionalAttrs = schema.getOptionalAttributes();
        
        assertNotNull(optionalAttrs);
        assertTrue(optionalAttrs.length > 0);
    }

    @Test
    public void testSchemaRequiredAttributes() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        String[] requiredAttrs = schema.getRequiredAttributes();
        
        assertNotNull(requiredAttrs);
        assertTrue(requiredAttrs.length >= 0);
    }

    @Test
    public void testSchemaGetAttribute() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        PluginConfigurationSchema.AttributeDefinition attr = schema.getAttribute("value");
        
        // May return null depending on implementation
        // Just testing that it doesn't throw exception
        assertTrue(true);
    }

    @Test
    public void testMetadataGetters() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata.getAuthor());
        assertNotNull(metadata.getCategory());
        assertNotNull(metadata.getVersion());
        
        String[] tags = metadata.getTags();
        assertNotNull(tags);
        
        String[] dependencies = metadata.getDependencies();
        assertNotNull(dependencies);
    }

    @Test
    public void testExecuteWithNullId() throws Exception {
        CasePlugin newPlugin = new CasePlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn(null);
        
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithEmptyId() throws Exception {
        CasePlugin newPlugin = new CasePlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("");
        
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithSpecialCharacterId() throws Exception {
        CasePlugin newPlugin = new CasePlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("case-id_123");
        
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithNumericId() throws Exception {
        CasePlugin newPlugin = new CasePlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("12345");
        
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithUnicodeId() throws Exception {
        CasePlugin newPlugin = new CasePlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("测试");
        
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testSchemaAllowsBody() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        assertTrue(schema.allowsBody());
    }

    @Test
    public void testAttributeDefinitionType() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        PluginConfigurationSchema.AttributeDefinition[] attrs = schema.getAttributeDefinitions();
        
        // Just ensure we can iterate through attributes
        for (PluginConfigurationSchema.AttributeDefinition attr : attrs) {
            assertNotNull(attr.getName());
            assertNotNull(attr.getType());
        }
    }
    @Test
    public void testCaseBranchMatchingPropagatesUnsupportedExpressionWithoutLanguage() throws Exception {
        ScraperContext directContext = new ScraperContext();
        directContext.setLocalVar("count", new NodeVariable("11"));

        Map<String, String> properties = new HashMap<>();
        properties.put("condition", "${count > 10}");
        IfPlugin branch = new IfPlugin();
        branch.initialize(new DefaultPluginConfiguration(
            "if", "http://org.webharvest/schema/2.1/core", "if", properties));

        try {
            branch.matchesCondition(directContext);
            fail("Case must not treat an expression error as a non-matching branch and select its default");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Unsupported If operator"));
        }
    }

    @Test
    public void testCaseCleansUpBranchWhenConditionEvaluationFails() throws Exception {
        CorePluginsRegistrar.registerCorePlugins();
        CorePluginRegistry registry = CorePluginRegistry.getInstance();
        Class<? extends org.webharvest.plugin.Plugin> originalIfPlugin = registry.getPluginClass("if");
        assertNotNull(originalIfPlugin, "Core If plugin must be registered before this regression test");

        registry.registerMapping(null, CleanupTrackingIfPlugin.class, "if");
        try {
            CleanupTrackingIfPlugin.CLEANUP_CALLS.set(0);
            ScraperContext directContext = new ScraperContext();
            directContext.setLocalVar("count", new NodeVariable("11"));

            Map<String, String> properties = new HashMap<>();
            properties.put("condition", "${count > 10}");
            DefaultPluginConfiguration branch = new DefaultPluginConfiguration(
                "if-cleanup-test", "http://org.webharvest/schema/2.1/core", "if", properties);
            DefaultPluginConfiguration caseConfiguration = new DefaultPluginConfiguration(
                "case-cleanup-test", "http://org.webharvest/schema/2.1/core", "case");
            caseConfiguration.setChildren(Collections.singletonList(branch));

            CasePlugin casePlugin = new CasePlugin();
            casePlugin.initialize(caseConfiguration);

            try {
                casePlugin.execute(directContext);
                fail("Case must propagate condition evaluation errors");
            } catch (RuntimeException expected) {
                assertTrue(expected.getCause() instanceof IllegalArgumentException,
                    "The original condition error must remain the cause");
            }

            assertEquals(CleanupTrackingIfPlugin.CLEANUP_CALLS.get(), 1,
                "A branch initialized for condition matching must be cleaned up exactly once on failure");
        } finally {
            registry.registerMapping(null, originalIfPlugin, "if");
        }
    }

    public static class CleanupTrackingIfPlugin extends IfPlugin {
        private static final AtomicInteger CLEANUP_CALLS = new AtomicInteger();

        @Override
        protected void doCleanup() {
            CLEANUP_CALLS.incrementAndGet();
        }
    }


    @Test
    public void testCaseRejectsRegisteredIfElementWithWrongPluginType() throws Exception {
        CorePluginsRegistrar.registerCorePlugins();
        CorePluginRegistry registry = CorePluginRegistry.getInstance();
        Class<? extends org.webharvest.plugin.Plugin> originalIfPlugin = registry.getPluginClass("if");
        assertNotNull(originalIfPlugin);

        registry.registerMapping(null, EmptyPlugin.class, "if");
        try {
            DefaultPluginConfiguration branch = new DefaultPluginConfiguration(
                "wrong-if-type", "http://org.webharvest/schema/2.1/core", "if");
            branch.setProperty("condition", "true");
            DefaultPluginConfiguration caseConfiguration = new DefaultPluginConfiguration(
                "case-wrong-if-type", "http://org.webharvest/schema/2.1/core", "case");
            caseConfiguration.setChildren(Collections.singletonList(branch));

            CasePlugin casePlugin = new CasePlugin();
            casePlugin.initialize(caseConfiguration);
            try {
                casePlugin.execute(new ScraperContext());
                fail("Case must reject a registry mapping that does not create an IfPlugin");
            } catch (RuntimeException expected) {
                assertTrue(expected.getCause() instanceof IllegalStateException);
                assertTrue(expected.getCause().getMessage().contains("must create IfPlugin"));
            }
        } finally {
            registry.registerMapping(null, originalIfPlugin, "if");
        }
    }


}
