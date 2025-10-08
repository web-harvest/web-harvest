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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.annotations.InjectDependency;

/**
 * Unit tests for PluginDependencyInjector interface and DefaultPluginDependencyInjector implementation.
 * 
 * @author Robert Bala
 * @since 2.2.0-SNAPSHOT
 */
public class PluginDependencyInjectorTest {
    
    private PluginDependencyInjector injector;
    
    @BeforeMethod
    public void setUp() {
        injector = new DefaultPluginDependencyInjector();
    }
    
    @Test
    public void testRegisterSingleton() throws Exception {
        String testInstance = "test-singleton";
        injector.registerSingleton(String.class, testInstance);
        
        String result = injector.getDependency(String.class);
        assertEquals(result, testInstance, "Should return registered singleton");
    }
    
    @Test
    public void testRegisterProvider() throws Exception {
        DependencyProvider<String> provider = () -> "provided-instance";
        injector.registerProvider(String.class, provider);
        
        String result = injector.getDependency(String.class);
        assertEquals(result, "provided-instance", "Should return instance from provider");
    }
    
    @Test
    public void testGetDependencyNotFound() {
        try {
            injector.getDependency(String.class);
            fail("Expected PluginDependencyInjectionException");
        } catch (PluginDependencyInjectionException e) {
            assertTrue(e.getMessage().contains("No provider or singleton registered"), 
                "Exception should mention no provider registered");
        }
    }
    
    @Test
    public void testGetDependencyWithNullType() throws Exception {
        try {
            injector.getDependency(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Dependency type cannot be null"), 
                "Exception should mention null dependency type");
        }
    }
    
    @Test
    public void testRegisterSingletonWithNullType() {
        try {
            injector.registerSingleton(null, "instance");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Dependency type cannot be null"), 
                "Exception should mention null dependency type");
        }
    }
    
    @Test
    public void testRegisterSingletonWithNullInstance() {
        try {
            injector.registerSingleton(String.class, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Instance cannot be null"), 
                "Exception should mention null instance");
        }
    }
    
    @Test
    public void testRegisterProviderWithNullType() {
        try {
            injector.registerProvider(null, () -> "test");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Dependency type cannot be null"), 
                "Exception should mention null dependency type");
        }
    }
    
    @Test
    public void testRegisterProviderWithNullProvider() {
        try {
            injector.registerProvider(String.class, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Provider cannot be null"), 
                "Exception should mention null provider");
        }
    }
    
    @Test
    public void testProviderThrowsException() {
        DependencyProvider<String> failingProvider = () -> {
            throw new RuntimeException("Provider failed");
        };
        injector.registerProvider(String.class, failingProvider);
        
        try {
            injector.getDependency(String.class);
            fail("Expected PluginDependencyInjectionException");
        } catch (PluginDependencyInjectionException e) {
            assertTrue(e.getMessage().contains("Failed to create dependency instance"), 
                "Exception should mention failed creation");
            assertTrue(e.getCause() instanceof RuntimeException, 
                "Cause should be the original RuntimeException");
        }
    }
    
    @Test
    public void testInjectDependenciesWithNullPlugin() {
        try {
            injector.injectDependencies(null);
            fail("Expected PluginDependencyInjectionException");
        } catch (PluginDependencyInjectionException e) {
            assertTrue(e.getMessage().contains("Plugin cannot be null"), 
                "Exception should mention null plugin");
        }
    }
    
    @Test
    public void testInjectDependencies() throws Exception {
        // Register a dependency
        String testDependency = "injected-dependency";
        injector.registerSingleton(String.class, testDependency);
        
        // Create a test plugin with @InjectDependency field
        TestPlugin plugin = new TestPlugin();
        
        // Inject dependencies
        injector.injectDependencies(plugin);
        
        // Verify injection
        assertEquals(plugin.getInjectedString(), testDependency, 
            "Dependency should be injected");
    }
    
    @Test
    public void testInjectDependenciesWithMissingDependency() throws Exception {
        // Create a test plugin with @InjectDependency field
        TestPlugin plugin = new TestPlugin();
        
        try {
            injector.injectDependencies(plugin);
            fail("Expected PluginDependencyInjectionException");
        } catch (PluginDependencyInjectionException e) {
            assertTrue(e.getMessage().contains("No provider or singleton registered"), 
                "Exception should mention missing dependency");
        }
    }
    
    /**
     * Test plugin class for dependency injection testing.
     */
    private static class TestPlugin implements Plugin {
        @InjectDependency
        private String injectedString;
        
        public String getInjectedString() {
            return injectedString;
        }
        
        @Override
        public void initialize(org.webharvest.plugin.PluginConfiguration configuration) throws org.webharvest.plugin.exceptions.PluginException {
            // Not used in tests
        }
        
        @Override
        public org.webharvest.runtime.variables.Variable execute(org.webharvest.runtime.DynamicScopeContext context) throws InterruptedException {
            // Not used in tests
            return null;
        }
        
        @Override
        public org.webharvest.plugin.PluginMetadata getMetadata() {
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
