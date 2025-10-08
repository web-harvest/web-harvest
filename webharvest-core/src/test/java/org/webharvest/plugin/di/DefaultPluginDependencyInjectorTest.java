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

package org.webharvest.plugin.di;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.di.PluginDependencyInjectionException;
import org.webharvest.plugin.di.DependencyProvider;

/**
 * Unit tests for DefaultPluginDependencyInjector.
 * 
 * @author Robert Bala
 * @since 2.2.0-SNAPSHOT
 */
public class DefaultPluginDependencyInjectorTest {
    
    private DefaultPluginDependencyInjector injector;
    
    @BeforeMethod
    public void setUp() {
        injector = new DefaultPluginDependencyInjector();
    }
    
    @Test
    public void testInjectDependenciesWithValidPlugin() throws Exception {
        Plugin plugin = mock(Plugin.class);
        
        // Should not throw exception
        injector.injectDependencies(plugin);
        
        // Verify plugin was processed (no exceptions thrown)
        assertNotNull(plugin, "Plugin should not be null");
    }
    
    @Test
    public void testInjectDependenciesWithNullPlugin() throws Exception {
        try {
            injector.injectDependencies(null);
            fail("Expected PluginDependencyInjectionException for null plugin");
        } catch (PluginDependencyInjectionException e) {
            assertTrue(e.getMessage().contains("Plugin cannot be null"), 
                "Exception should mention null plugin");
        }
    }
    
    @Test
    public void testGetDependencyWithValidType() throws Exception {
        // Test getting a dependency
        try {
            injector.getDependency(String.class);
            fail("Expected PluginDependencyInjectionException for unregistered dependency");
        } catch (PluginDependencyInjectionException e) {
            assertTrue(e.getMessage().contains("No provider or singleton registered"), 
                "Exception should mention no provider registered");
        }
    }
    
    @Test
    public void testGetDependencyWithNullType() throws Exception {
        try {
            injector.getDependency(null);
            fail("Expected IllegalArgumentException for null type");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Dependency type cannot be null"), 
                "Exception should mention null type");
        }
    }
    
    @Test
    public void testRegisterProvider() throws Exception {
        DependencyProvider<String> provider = mock(DependencyProvider.class);
        when(provider.provide()).thenReturn("test");
        
        injector.registerProvider(String.class, provider);
        
        String result = injector.getDependency(String.class);
        assertEquals(result, "test", "Should return dependency from provider");
        verify(provider).provide();
    }
    
    @Test
    public void testRegisterProviderWithNullType() throws Exception {
        DependencyProvider<String> provider = mock(DependencyProvider.class);
        
        try {
            injector.registerProvider(null, provider);
            fail("Expected IllegalArgumentException for null type");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Dependency type cannot be null"), 
                "Exception should mention null type");
        }
    }
    
    @Test
    public void testRegisterProviderWithNullProvider() throws Exception {
        try {
            injector.registerProvider(String.class, null);
            fail("Expected IllegalArgumentException for null provider");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Provider cannot be null"), 
                "Exception should mention null provider");
        }
    }
    
    @Test
    public void testRegisterSingleton() throws Exception {
        String singleton = "singleton";
        
        injector.registerSingleton(String.class, singleton);
        
        String result1 = injector.getDependency(String.class);
        String result2 = injector.getDependency(String.class);
        
        assertEquals(result1, singleton, "Should return singleton instance");
        assertEquals(result2, singleton, "Should return same singleton instance");
        assertSame(result1, result2, "Should return same instance for singleton");
    }
    
    @Test
    public void testRegisterSingletonWithNullType() throws Exception {
        try {
            injector.registerSingleton(null, "test");
            fail("Expected IllegalArgumentException for null type");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Dependency type cannot be null"), 
                "Exception should mention null type");
        }
    }
    
    @Test
    public void testRegisterSingletonWithNullInstance() throws Exception {
        try {
            injector.registerSingleton(String.class, null);
            fail("Expected IllegalArgumentException for null instance");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Instance cannot be null"), 
                "Exception should mention null instance");
        }
    }
}
