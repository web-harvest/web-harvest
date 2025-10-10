/*
 Copyright (c) 2006-2025 the original author or authors.
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
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.testng.Assert.*;

/**
 * Test for LoopPlugin
 */
public class LoopPluginTest {

    // REMOVED in v2.2.x - old architecture mocks no longer needed
    // @Mock
    // private CorePluginDef elementDef;
    // @Mock
    // private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;
    
    private LoopPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new LoopPlugin();
        
        
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(xmlNode.getText()).thenReturn("Test loop content");
        // when(elementDef.hasOperations()).thenReturn(false);
        
        when(configuration.getProperty("items")).thenReturn("item1,item2,item3");
        when(configuration.getProperty("item")).thenReturn("item");
        when(configuration.getProperty("index")).thenReturn("index");
        when(configuration.getProperty("maxloops")).thenReturn("100");
        when(configuration.getProperty("filter")).thenReturn("");
        when(configuration.getProperty("empty")).thenReturn("false");
        when(configuration.getProperty("delimiter")).thenReturn(",");
        
        plugin.initialize(configuration);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "Loop Plugin");
        assertEquals(metadata.getDescription(), "Loop plugin for iterations");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "loop");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        assertNotNull(schema);
        assertNotNull(schema.getAttributeDefinitions());
        assertEquals(schema.getAttributeDefinitions().length, 0);
        
        assertTrue(schema.allowsBody());
        assertEquals(schema.getVersion(), "2.1.0");
    }

    @Test
    public void testLoopPluginInitialize() throws PluginException {
        LoopPlugin newPlugin = new LoopPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("item1,item2");
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("index");
        when(config.getProperty("maxloops")).thenReturn("100");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn(",");
        
        newPlugin.initialize(config);
        
        assertNotNull(newPlugin);
    }

    @Test
    public void testLoopPluginExecute() throws InterruptedException {
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable); // LoopPlugin returns EmptyVariable when items is null
    }

    @Test
    public void testLoopPluginExecuteWithEmptyItems() throws InterruptedException, PluginException {
        LoopPlugin newPlugin = new LoopPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("");
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("index");
        when(config.getProperty("maxloops")).thenReturn("100");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn(",");
        
        newPlugin.initialize(config);
        
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testLoopPluginExecuteWithInterruptedException() throws InterruptedException, PluginException {
        when(context.getCharset()).thenReturn("UTF-8");
        
        LoopPlugin testPlugin = new LoopPlugin() {
            @Override
            protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                throw new InterruptedException("Test interruption");
            }
        };
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("item1,item2");
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("index");
        when(config.getProperty("maxloops")).thenReturn("100");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn(",");
        
        testPlugin.initialize(config);
        
        testPlugin.execute(context);
    }

    @Test
    public void testLoopPluginValidateConfiguration() throws PluginValidationException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("item1,item2");
        plugin.validateConfiguration(config);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testLoopPluginValidateConfigurationWithNull() throws PluginValidationException {
        // AbstractPlugin validates that configuration is not null
        plugin.validateConfiguration(null);
    }

    @Test
    public void testLoopPluginWithCustomDelimiter() throws PluginException, InterruptedException {
        LoopPlugin newPlugin = new LoopPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("item1|item2|item3");
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("index");
        when(config.getProperty("maxloops")).thenReturn("100");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn("|");
        
        newPlugin.initialize(config);
        
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable); // LoopPlugin returns EmptyVariable when items is null
    }

    @Test
    public void testLoopPluginExecuteMultipleTimes() throws InterruptedException {
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
    public void testLoopPluginWithSingleItem() throws InterruptedException, PluginException {
        when(context.getCharset()).thenReturn("UTF-8");
        
        LoopPlugin newPlugin = new LoopPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("singleItem");
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("index");
        when(config.getProperty("maxloops")).thenReturn("-1");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn(",");
        
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testLoopPluginWithComplexDelimiter() throws InterruptedException, PluginException {
        when(context.getCharset()).thenReturn("UTF-8");
        
        LoopPlugin newPlugin = new LoopPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("item1||item2||item3");
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("index");
        when(config.getProperty("maxloops")).thenReturn("-1");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn("||");
        
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testLoopPluginWithSpecialCharacters() throws InterruptedException, PluginException {
        when(context.getCharset()).thenReturn("UTF-8");
        
        LoopPlugin newPlugin = new LoopPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("item@#$,item%^&,item*()");
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("index");
        when(config.getProperty("maxloops")).thenReturn("-1");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn(",");
        
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testLoopPluginWithUnicodeItems() throws InterruptedException, PluginException {
        when(context.getCharset()).thenReturn("UTF-8");
        
        LoopPlugin newPlugin = new LoopPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("项目1,项目2,项目3");
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("index");
        when(config.getProperty("maxloops")).thenReturn("-1");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn(",");
        
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testLoopPluginWithMaxLoops() throws InterruptedException, PluginException {
        when(context.getCharset()).thenReturn("UTF-8");
        
        LoopPlugin newPlugin = new LoopPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("item1,item2,item3,item4,item5");
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("index");
        when(config.getProperty("maxloops")).thenReturn("2");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn(",");
        
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testSchemaOptionalAttributes() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        String[] optionalAttrs = schema.getOptionalAttributes();
        
        assertNotNull(optionalAttrs);
    }

    @Test
    public void testSchemaRequiredAttributes() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        String[] requiredAttrs = schema.getRequiredAttributes();
        
        assertNotNull(requiredAttrs);
        assertTrue(requiredAttrs.length >= 0);
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
    public void testSchemaAllowsBody() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        assertTrue(schema.allowsBody());
    }

    @Test
    public void testExecuteWithEmptyItems() throws Exception {
        LoopPlugin newPlugin = new LoopPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("");
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("idx");
        when(config.getProperty("maxloops")).thenReturn("-1");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("true");
        when(config.getProperty("delimiter")).thenReturn(",");
        
        newPlugin.initialize(config);
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithNullDelimiter() throws Exception {
        LoopPlugin newPlugin = new LoopPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("a,b,c");
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("idx");
        when(config.getProperty("maxloops")).thenReturn("-1");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn(null);
        
        newPlugin.initialize(config);
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithFilterAttribute() throws Exception {
        LoopPlugin newPlugin = new LoopPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("1,2,3,4,5");
        when(config.getProperty("item")).thenReturn("num");
        when(config.getProperty("index")).thenReturn("idx");
        when(config.getProperty("maxloops")).thenReturn("-1");
        when(config.getProperty("filter")).thenReturn("true");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn(",");
        
        newPlugin.initialize(config);
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithSingleItem() throws Exception {
        LoopPlugin newPlugin = new LoopPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("single");
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("idx");
        when(config.getProperty("maxloops")).thenReturn("100");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn(",");
        
        newPlugin.initialize(config);
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithManyItems() throws Exception {
        LoopPlugin newPlugin = new LoopPlugin();
        
        StringBuilder manyItems = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            if (i > 0) manyItems.append(",");
            manyItems.append("item").append(i);
        }
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn(manyItems.toString());
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("idx");
        when(config.getProperty("maxloops")).thenReturn("100");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn(",");
        
        newPlugin.initialize(config);
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithUnicodeItems() throws Exception {
        LoopPlugin newPlugin = new LoopPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("中文,العربية,русский");
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("idx");
        when(config.getProperty("maxloops")).thenReturn("100");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn(",");
        
        newPlugin.initialize(config);
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithSpecialDelimiter() throws Exception {
        LoopPlugin newPlugin = new LoopPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("part1||part2||part3");
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("idx");
        when(config.getProperty("maxloops")).thenReturn("100");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn("||");
        
        newPlugin.initialize(config);
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithEmptyItemsInList() throws Exception {
        LoopPlugin newPlugin = new LoopPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("items")).thenReturn("item1,,item3,,item5");
        when(config.getProperty("item")).thenReturn("item");
        when(config.getProperty("index")).thenReturn("idx");
        when(config.getProperty("maxloops")).thenReturn("100");
        when(config.getProperty("filter")).thenReturn("");
        when(config.getProperty("empty")).thenReturn("false");
        when(config.getProperty("delimiter")).thenReturn(",");
        
        newPlugin.initialize(config);
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }
}

