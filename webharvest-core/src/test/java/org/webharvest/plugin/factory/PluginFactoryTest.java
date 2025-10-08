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

package org.webharvest.plugin.factory;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.registry.PluginRegistry;
import org.webharvest.plugin.exceptions.PluginException;

/**
 * Unit tests for PluginFactory interface and DefaultPluginFactory implementation.
 * 
 * @author Robert Bala
 * @since 2.2.0-SNAPSHOT
 */
public class PluginFactoryTest {
    
    private PluginFactory pluginFactory;
    private PluginRegistry pluginRegistry;
    
    @BeforeMethod
    public void setUp() {
        pluginRegistry = mock(PluginRegistry.class);
        pluginFactory = new DefaultPluginFactory(pluginRegistry);
    }
    
    @Test
    public void testCreatePluginWithClass() throws Exception {
        Plugin plugin = pluginFactory.createPlugin(TestPlugin.class);
        
        assertNotNull(plugin, "Plugin should not be null");
        assertTrue(plugin instanceof TestPlugin, "Plugin should be instance of TestPlugin");
    }
    
    @Test
    public void testCreatePluginWithClassAndConfiguration() throws Exception {
        PluginConfiguration config = mock(PluginConfiguration.class);
        Plugin plugin = pluginFactory.createPlugin(TestPlugin.class, config);
        
        assertNotNull(plugin, "Plugin should not be null");
        assertTrue(plugin instanceof TestPlugin, "Plugin should be instance of TestPlugin");
    }
    
    @Test
    public void testCreatePluginWithNullClass() {
        try {
            pluginFactory.createPlugin((Class<? extends Plugin>) null);
            fail("Expected PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("Plugin class cannot be null"), 
                "Exception should mention null plugin class");
        }
    }
    
    @Test
    public void testCreatePluginWithPluginId() throws Exception {
        String pluginId = "test-plugin";
        TestPlugin existingPlugin = new TestPlugin();
        
        when(pluginRegistry.getPlugin(pluginId)).thenReturn(existingPlugin);
        
        Plugin plugin = pluginFactory.createPlugin(pluginId, (PluginConfiguration) null);
        
        assertNotNull(plugin, "Plugin should not be null");
        assertTrue(plugin instanceof TestPlugin, "Plugin should be instance of TestPlugin");
    }
    
    @Test
    public void testCreatePluginWithPluginIdNotFound() {
        String pluginId = "non-existent-plugin";
        
        when(pluginRegistry.getPlugin(pluginId)).thenReturn(null);
        
        try {
            pluginFactory.createPlugin(pluginId);
            fail("Expected PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("Plugin not found"), 
                "Exception should mention plugin not found");
        }
    }
    
    @Test
    public void testCreatePluginWithNullPluginId() {
        try {
            pluginFactory.createPlugin((String) null);
            fail("Expected PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("Plugin ID cannot be null or empty"), 
                "Exception should mention null plugin ID");
        }
    }
    
    @Test
    public void testCreatePluginWithEmptyPluginId() {
        try {
            pluginFactory.createPlugin("");
            fail("Expected PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("Plugin ID cannot be null or empty"), 
                "Exception should mention empty plugin ID");
        }
    }
    
    @Test
    public void testCreatePluginWithNamespaceAndElementName() throws Exception {
        String namespace = "http://test.org";
        String elementName = "test-element";
        TestPlugin existingPlugin = new TestPlugin();
        
        when(pluginRegistry.getPlugin(namespace, elementName)).thenReturn(existingPlugin);
        
        Plugin plugin = pluginFactory.createPlugin(namespace, elementName);
        
        assertNotNull(plugin, "Plugin should not be null");
        assertTrue(plugin instanceof TestPlugin, "Plugin should be instance of TestPlugin");
    }
    
    @Test
    public void testCreatePluginWithNamespaceAndElementNameNotFound() {
        String namespace = "http://test.org";
        String elementName = "non-existent-element";
        
        when(pluginRegistry.getPlugin(namespace, elementName)).thenReturn(null);
        
        try {
            pluginFactory.createPlugin(namespace, elementName);
            fail("Expected PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("Plugin not found"), 
                "Exception should mention plugin not found");
        }
    }
    
    @Test
    public void testCreatePluginWithNullNamespace() {
        try {
            pluginFactory.createPlugin(null, "element");
            fail("Expected PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("Namespace cannot be null or empty"), 
                "Exception should mention null namespace");
        }
    }
    
    @Test
    public void testCreatePluginWithNullElementName() {
        try {
            pluginFactory.createPlugin("http://test.org", (String) null);
            fail("Expected PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("Element name cannot be null or empty"), 
                "Exception should mention null element name");
        }
    }
    
    @Test
    public void testCreateMockPlugin() throws Exception {
        Plugin mockPlugin = pluginFactory.createMockPlugin(TestPlugin.class);
        
        assertNotNull(mockPlugin, "Mock plugin should not be null");
        assertTrue(mockPlugin instanceof org.webharvest.plugin.mock.MockPlugin, 
            "Should be instance of MockPlugin");
    }
    
    @Test
    public void testCreateMockPluginWithConfiguration() throws Exception {
        PluginConfiguration config = mock(PluginConfiguration.class);
        Plugin mockPlugin = pluginFactory.createMockPlugin(TestPlugin.class, config);
        
        assertNotNull(mockPlugin, "Mock plugin should not be null");
        assertTrue(mockPlugin instanceof org.webharvest.plugin.mock.MockPlugin, 
            "Should be instance of MockPlugin");
    }
    
    @Test
    public void testCreateMockPluginWithNullClass() {
        try {
            pluginFactory.createMockPlugin((Class<? extends Plugin>) null);
            fail("Expected PluginException");
        } catch (PluginException e) {
            assertTrue(e.getMessage().contains("Plugin class cannot be null"), 
                "Exception should mention null plugin class");
        }
    }
    
    @Test
    public void testCanCreatePlugin() {
        assertTrue(pluginFactory.canCreatePlugin(TestPlugin.class), 
            "Should be able to create TestPlugin");
        assertFalse(pluginFactory.canCreatePlugin(null), 
            "Should not be able to create null plugin");
    }
    
    @Test
    public void testGetPluginClassById() {
        String pluginId = "test-plugin";
        TestPlugin existingPlugin = new TestPlugin();
        
        when(pluginRegistry.getPlugin(pluginId)).thenReturn(existingPlugin);
        
        Class<? extends Plugin> pluginClass = pluginFactory.getPluginClass(pluginId);
        
        assertEquals(pluginClass, TestPlugin.class, "Plugin class should match");
    }
    
    @Test
    public void testGetPluginClassByIdNotFound() {
        String pluginId = "non-existent-plugin";
        
        when(pluginRegistry.getPlugin(pluginId)).thenReturn(null);
        
        Class<? extends Plugin> pluginClass = pluginFactory.getPluginClass(pluginId);
        
        assertNull(pluginClass, "Plugin class should be null for non-existent plugin");
    }
    
    @Test
    public void testGetPluginClassByNamespaceAndElementName() {
        String namespace = "http://test.org";
        String elementName = "test-element";
        TestPlugin existingPlugin = new TestPlugin();
        
        when(pluginRegistry.getPlugin(namespace, elementName)).thenReturn(existingPlugin);
        
        Class<? extends Plugin> pluginClass = pluginFactory.getPluginClass(namespace, elementName);
        
        assertEquals(pluginClass, TestPlugin.class, "Plugin class should match");
    }
    
    @Test
    public void testGetPluginClassByNamespaceAndElementNameNotFound() {
        String namespace = "http://test.org";
        String elementName = "non-existent-element";
        
        when(pluginRegistry.getPlugin(namespace, elementName)).thenReturn(null);
        
        Class<? extends Plugin> pluginClass = pluginFactory.getPluginClass(namespace, elementName);
        
        assertNull(pluginClass, "Plugin class should be null for non-existent plugin");
    }
    
    @Test
    public void testGetPluginClassWithNullNamespace() {
        Class<? extends Plugin> pluginClass = pluginFactory.getPluginClass(null, "element");
        
        assertNull(pluginClass, "Plugin class should be null for null namespace");
    }
    
    @Test
    public void testGetPluginClassWithNullElementName() {
        Class<? extends Plugin> pluginClass = pluginFactory.getPluginClass("http://test.org", null);
        
        assertNull(pluginClass, "Plugin class should be null for null element name");
    }
    
    @Test
    public void testConstructorWithNullRegistry() {
        try {
            new DefaultPluginFactory(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Plugin registry cannot be null"), 
                "Exception should mention null plugin registry");
        }
    }
    
    /**
     * Test plugin class for testing purposes.
     */
    private static class TestPlugin implements Plugin {
        @Override
        public void initialize(PluginConfiguration configuration) throws org.webharvest.plugin.exceptions.PluginException {
            // Not used in tests
        }
        
        @Override
        public org.webharvest.runtime.variables.Variable execute(org.webharvest.runtime.DynamicScopeContext context) throws InterruptedException {
            // Not used in tests
            return null;
        }
        
        @Override
        public PluginMetadata getMetadata() {
            // Not used in tests
            return null;
        }
        
        public org.webharvest.plugin.PluginConfigurationSchema getConfigurationSchema() {
            // Not used in tests
            return null;
        }
        
        @Override
        public int getRunningLevel() {
            // Not used in tests
            return 0;
        }
        
        @Override
        public void setParentPlugin(Plugin parent) {
            // Not used in tests
        }
        
        @Override
        public Plugin getParentPlugin() {
            // Not used in tests
            return null;
        }
        
        @Override
        public void setElementDef(org.webharvest.definition.IElementDef elementDef) {
            // Not used in tests
        }
        
        @Override
        public org.webharvest.definition.IElementDef getElementDef() {
            // Not used in tests
            return null;
        }
        
        @Override
        public void cleanup() {
            // Not used in tests
        }
        
        @Override
        public boolean isInitialized() {
            // Not used in tests
            return false;
        }
        
        @Override
        public void validateConfiguration(PluginConfiguration configuration) throws org.webharvest.plugin.exceptions.PluginValidationException {
            // Not used in tests
        }
    }
}
