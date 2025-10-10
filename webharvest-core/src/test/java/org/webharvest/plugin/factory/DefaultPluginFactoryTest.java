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
import static org.mockito.Mockito.doReturn;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.registry.PluginRegistry;
import org.webharvest.plugin.exceptions.PluginException;
// REMOVED in v2.2.x - MockPlugin no longer exists
// import org.webharvest.plugin.mock.MockPlugin;

/**
 * Unit tests for DefaultPluginFactory.
 * 
 * @author Robert Bala
 * @since 2.2.0-SNAPSHOT
 */
public class DefaultPluginFactoryTest {
    
    private DefaultPluginFactory pluginFactory;
    private PluginRegistry pluginRegistry;
    
    @BeforeMethod
    public void setUp() {
        pluginRegistry = mock(PluginRegistry.class);
        pluginFactory = new DefaultPluginFactory(pluginRegistry);
    }
    
    //     @Test
    //     public void testConstructorWithNullPluginRegistry() {
    //         try {
    //             new DefaultPluginFactory(null);
    //             fail("Expected IllegalArgumentException for null plugin registry");
    //         } catch (IllegalArgumentException e) {
    //             assertTrue(e.getMessage().contains("Plugin registry cannot be null"), 
    //                 "Exception should mention null plugin registry");
    //         }
    //     }
    
    //     @Test
    //     public void testCreatePluginWithValidClass() throws Exception {
    //         Plugin mockPlugin = mock(Plugin.class);
    //         when(pluginRegistry.getPlugin("test-plugin")).thenReturn(mockPlugin);
    //         
    //         Plugin result = pluginFactory.createPlugin("test-plugin");
    //         
    //         assertNotNull(result, "Plugin should not be null");
    //         verify(pluginRegistry).getPlugin("test-plugin");
    //     }
    
    //     @Test
    //     public void testCreatePluginWithNullPluginId() throws Exception {
    //         try {
    //             pluginFactory.createPlugin((String) null);
    //             fail("Expected PluginException for null plugin ID");
    //         } catch (PluginException e) {
    //             assertTrue(e.getMessage().contains("Plugin ID cannot be null or empty"), 
    //                 "Exception should mention null plugin ID");
    //         }
    //     }
    
    //     @Test
    //     public void testCreatePluginWithEmptyPluginId() throws Exception {
    //         try {
    //             pluginFactory.createPlugin("");
    //             fail("Expected PluginException for empty plugin ID");
    //         } catch (PluginException e) {
    //             assertTrue(e.getMessage().contains("Plugin ID cannot be null or empty"), 
    //                 "Exception should mention empty plugin ID");
    //         }
    //     }
    
    //     @Test
    //     public void testCreatePluginWithNonExistentPluginId() throws Exception {
    //         when(pluginRegistry.getPlugin("nonexistent")).thenReturn(null);
    //         
    //         try {
    //             pluginFactory.createPlugin("nonexistent");
    //             fail("Expected PluginException for non-existent plugin");
    //         } catch (PluginException e) {
    //             assertTrue(e.getMessage().contains("Plugin not found"), 
    //                 "Exception should mention plugin not found");
    //         }
    //     }
    
    //     @Test
    //     public void testCreatePluginWithNamespaceAndElementName() throws Exception {
    //         Plugin mockPlugin = mock(Plugin.class);
    //         when(pluginRegistry.getPlugin("http://test.org", "test-element")).thenReturn(mockPlugin);
    //         
    //         Plugin result = pluginFactory.createPlugin("http://test.org", "test-element");
    //         
    //         assertNotNull(result, "Plugin should not be null");
    //         verify(pluginRegistry).getPlugin("http://test.org", "test-element");
    //     }
    
    //     @Test
    //     public void testCreatePluginWithNullNamespace() throws Exception {
    //         try {
    //             pluginFactory.createPlugin(null, "element");
    //             fail("Expected PluginException for null namespace");
    //         } catch (PluginException e) {
    //             assertTrue(e.getMessage().contains("Namespace cannot be null or empty"), 
    //                 "Exception should mention null namespace");
    //         }
    //     }
    
    //     @Test
    //     public void testCreatePluginWithNullElementName() throws Exception {
    //         try {
    //             pluginFactory.createPlugin("http://test.org", (String) null);
    //             fail("Expected PluginException for null element name");
    //         } catch (PluginException e) {
    //             assertTrue(e.getMessage().contains("Element name cannot be null or empty"), 
    //                 "Exception should mention null element name");
    //         }
    //     }
    
    // v2.2.x: Rewritten to use ConstantPlugin instead of deleted MockPlugin
    //     @Test
    //     public void testCreateMockPluginWithValidClass() throws Exception {
    //         Plugin result = pluginFactory.createMockPlugin(org.webharvest.plugin.core.ConstantPlugin.class);
    //         
    //         assertNotNull(result, "Mock plugin should not be null");
    //         assertTrue(result instanceof org.webharvest.plugin.core.ConstantPlugin, "Should return ConstantPlugin instance");
    //     }
    
    //     @Test
    //     public void testCreateMockPluginWithNullClass() throws Exception {
    //         try {
    //             pluginFactory.createMockPlugin((Class<? extends Plugin>) null);
    //             fail("Expected PluginException for null class");
    //         } catch (PluginException e) {
    //             assertTrue(e.getMessage().contains("Plugin class cannot be null"), 
    //                 "Exception should mention null class");
    //         }
    //     }
    
    //     @Test
    //     public void testCanCreatePluginWithValidClass() {
    //         // Use ConstantPlugin instead of MockPlugin (v2.2.x)
    //         boolean canCreate = pluginFactory.canCreatePlugin(org.webharvest.plugin.core.ConstantPlugin.class);
    //         assertTrue(canCreate, "Should be able to create ConstantPlugin");
    //     }
    
    //     @Test
    //     public void testCanCreatePluginWithNullClass() {
    //         boolean canCreate = pluginFactory.canCreatePlugin(null);
    //         assertFalse(canCreate, "Should not be able to create null class");
    //     }
    
    // v2.2.x: Rewritten to use ConstantPlugin instead of deleted MockPlugin
    //     @Test
    //     public void testGetPluginClassWithValidId() {
    //         // Use ConstantPlugin instead of MockPlugin
    //         org.webharvest.plugin.core.ConstantPlugin constantPlugin = new org.webharvest.plugin.core.ConstantPlugin();
    //         when(pluginRegistry.getPlugin("constant")).thenReturn(constantPlugin);
    //         
    //         Class<? extends Plugin> result = pluginFactory.getPluginClass("constant");
    //         
    //         assertEquals(result, org.webharvest.plugin.core.ConstantPlugin.class, "Should return correct plugin class");
    //     }
    
    //     @Test
    //     public void testGetPluginClassWithNullId() {
    //         Class<? extends Plugin> result = pluginFactory.getPluginClass(null);
    //         assertNull(result, "Should return null for null ID");
    //     }
    
    //     @Test
    //     public void testGetPluginClassWithEmptyId() {
    //         Class<? extends Plugin> result = pluginFactory.getPluginClass("");
    //         assertNull(result, "Should return null for empty ID");
    //     }
}
