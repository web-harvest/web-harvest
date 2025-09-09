/*  Copyright (c) 2006-2007, Vladimir Nikic
    All rights reserved.

    Redistribution and use of this software in source and binary forms,
    with or without modification, are permitted provided that the following
    conditions are met:

    * Redistributions of source code must retain the above
      copyright notice, this list of conditions and the
      following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the
      following disclaimer in the documentation and/or other
      materials provided with the distribution.

    * The name of Web-Harvest may not be used to endorse or promote
      products derived from this software without specific prior
      written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.

    You can contact Vladimir Nikic by sending e-mail to
    nikic_vladimir@yahoo.com. Please include the word "Web-Harvest" in the
    subject line.
*/
package org.webharvest.plugin.mock;

import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Mock implementation of Plugin for testing purposes.
 * Allows setting up expected behavior and verifying interactions.
 * 
 * @author Web-Harvest Team
 * @since 2.1.0
 */
public class MockPlugin implements Plugin {

    private final PluginMetadata metadata;
    private final String id;
    private final String name;
    private final String version;
    private final String description;
    private final String category;
    private final String namespace;
    private final String elementName;
    
    private PluginConfiguration configuration;
    private boolean initialized = false;
    private boolean shouldThrowException = false;
    private Exception exceptionToThrow;
    private Function<DynamicScopeContext, Variable> executionFunction;
    private Variable fixedReturnValue;
    private AtomicInteger executionCount = new AtomicInteger(0);
    private DynamicScopeContext lastExecutionContext;
    private PluginConfiguration lastConfiguration;

    /**
     * Constructor for creating a mock plugin.
     * 
     * @param id plugin ID
     * @param name plugin name
     * @param version plugin version
     * @param description plugin description
     * @param category plugin category
     * @param namespace plugin namespace
     * @param elementName element name
     */
    public MockPlugin(String id, String name, String version, String description, 
                     String category, String namespace, String elementName) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.category = category;
        this.namespace = namespace;
        this.elementName = elementName;
        this.metadata = new MockPluginMetadata(id, name, version, description, category, namespace, elementName);
    }

    /**
     * Creates a simple mock plugin with basic information.
     * 
     * @param id plugin ID
     * @param name plugin name
     * @return mock plugin instance
     */
    public static MockPlugin create(String id, String name) {
        return new MockPlugin(id, name, "1.0.0", "Mock plugin for testing", 
                             "test", "http://test.namespace", name);
    }

    @Override
    public Variable execute(DynamicScopeContext context) throws InterruptedException {
        executionCount.incrementAndGet();
        lastExecutionContext = context;
        
        if (shouldThrowException && exceptionToThrow != null) {
            if (exceptionToThrow instanceof InterruptedException) {
                throw (InterruptedException) exceptionToThrow;
            } else if (exceptionToThrow instanceof RuntimeException) {
                throw (RuntimeException) exceptionToThrow;
            } else {
                throw new RuntimeException(exceptionToThrow);
            }
        }
        
        if (executionFunction != null) {
            return executionFunction.apply(context);
        }
        
        if (fixedReturnValue != null) {
            return fixedReturnValue;
        }
        
        return new MockVariable("mock result");
    }

    @Override
    public PluginMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void initialize(PluginConfiguration configuration) throws PluginException {
        this.configuration = configuration;
        this.lastConfiguration = configuration;
        this.initialized = true;
    }

    @Override
    public void validateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        // Mock implementation - always passes validation
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void cleanup() {
        this.initialized = false;
        this.configuration = null;
    }

    // Mock-specific methods for testing

    /**
     * Sets the function to be called during execution.
     * 
     * @param executionFunction function to call
     * @return this mock plugin for chaining
     */
    public MockPlugin whenExecute(Function<DynamicScopeContext, Variable> executionFunction) {
        this.executionFunction = executionFunction;
        return this;
    }

    /**
     * Sets a fixed return value for execution.
     * 
     * @param returnValue fixed return value
     * @return this mock plugin for chaining
     */
    public MockPlugin thenReturn(Variable returnValue) {
        this.fixedReturnValue = returnValue;
        return this;
    }

    /**
     * Sets an exception to be thrown during execution.
     * 
     * @param exception exception to throw
     * @return this mock plugin for chaining
     */
    public MockPlugin thenThrow(Exception exception) {
        this.shouldThrowException = true;
        this.exceptionToThrow = exception;
        return this;
    }

    /**
     * Gets the number of times execute was called.
     * 
     * @return execution count
     */
    public int getExecutionCount() {
        return executionCount.get();
    }

    /**
     * Gets the last execution context.
     * 
     * @return last execution context
     */
    public DynamicScopeContext getLastExecutionContext() {
        return lastExecutionContext;
    }

    /**
     * Gets the last configuration used.
     * 
     * @return last configuration
     */
    public PluginConfiguration getLastConfiguration() {
        return lastConfiguration;
    }

    /**
     * Resets the mock state.
     */
    public void reset() {
        this.executionCount.set(0);
        this.lastExecutionContext = null;
        this.lastConfiguration = null;
        this.shouldThrowException = false;
        this.exceptionToThrow = null;
        this.executionFunction = null;
        this.fixedReturnValue = null;
    }

    /**
     * Mock implementation of PluginMetadata.
     */
    private class MockPluginMetadata implements PluginMetadata {
        private final String id;
        private final String name;
        private final String version;
        private final String description;
        private final String category;
        private final String namespace;
        private final String elementName;
        
        public MockPluginMetadata(String id, String name, String version, String description, 
                                 String category, String namespace, String elementName) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.description = description;
            this.category = category;
            this.namespace = namespace;
            this.elementName = elementName;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getVersion() { return version; }
        public String getDescription() { return description; }
        public String getAuthor() { return "Mock Author"; }
        public String getCategory() { return category; }
        public java.util.List<String> getTags() { return java.util.Arrays.asList("mock", "test"); }
        public java.util.Map<String, String> getDependencies() { return java.util.Collections.emptyMap(); }
        public PluginConfigurationSchema getConfigurationSchema() { return null; }
        public String getNamespace() { return namespace; }
        public String getElementName() { return elementName; }
        public boolean isCorePlugin() { return false; }
        public String getClassName() { return MockPlugin.class.getName(); }
    }

    /**
     * Mock implementation of Variable.
     */
    private static class MockVariable extends org.webharvest.runtime.variables.NodeVariable {
        
        public MockVariable(String value) {
            super(value);
        }
    }
}
