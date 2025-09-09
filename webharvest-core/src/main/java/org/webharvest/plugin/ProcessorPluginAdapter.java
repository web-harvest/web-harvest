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

package org.webharvest.plugin;

import org.webharvest.definition.IElementDef;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.processors.Processor;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;

/**
 * Adapter that allows existing processors to work as plugins.
 * This class provides backward compatibility during the migration process.
 * 
 * <p>
 * The adapter wraps a Processor instance and implements the Plugin interface,
 * delegating calls to the underlying processor while maintaining compatibility
 * with the existing processor system.
 * </p>
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public class ProcessorPluginAdapter implements Plugin {

    private final Processor<? extends IElementDef> processor;
    private final PluginMetadata metadata;
    private boolean initialized = false;

    /**
     * Creates a new adapter for the given processor.
     * 
     * @param processor the processor to adapt
     * @param metadata the plugin metadata
     */
    public ProcessorPluginAdapter(Processor<? extends IElementDef> processor, PluginMetadata metadata) {
        if (processor == null) {
            throw new IllegalArgumentException("Processor cannot be null");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        this.processor = processor;
        this.metadata = metadata;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Variable execute(DynamicScopeContext context) throws InterruptedException {
        return processor.run(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginMetadata getMetadata() {
        return metadata;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(PluginConfiguration configuration) throws PluginException {
        // Processors don't need explicit initialization
        this.initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        // Processors don't have configuration validation in the old system
        // This could be extended in the future
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanup() {
        // Processors don't need explicit cleanup
        this.initialized = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IElementDef getElementDef() {
        return processor.getElementDef();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setElementDef(IElementDef elementDef) {
        // Note: This is a type-safe cast since we know the processor type
        @SuppressWarnings("unchecked")
        Processor<IElementDef> typedProcessor = (Processor<IElementDef>) processor;
        typedProcessor.setElementDef(elementDef);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Plugin getParentPlugin() {
        Processor<?> parentProcessor = processor.getParentProcessor();
        if (parentProcessor != null) {
            // Create a temporary adapter for the parent processor
            // This is a simplified approach - in a full implementation,
            // you might want to maintain a registry of adapters
            return new ProcessorPluginAdapter(parentProcessor, createMetadataForProcessor(parentProcessor));
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParentPlugin(Plugin parentPlugin) {
        if (parentPlugin instanceof ProcessorPluginAdapter) {
            ProcessorPluginAdapter parentAdapter = (ProcessorPluginAdapter) parentPlugin;
            processor.setParentProcessor(parentAdapter.processor);
        } else {
            throw new IllegalArgumentException("Parent plugin must be a ProcessorPluginAdapter");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRunningLevel() {
        return processor.getRunningLevel();
    }

    /**
     * Gets the underlying processor.
     * 
     * @return the processor
     */
    public Processor<? extends IElementDef> getProcessor() {
        return processor;
    }

    /**
     * Creates metadata for a processor.
     * This is a helper method for creating parent plugin adapters.
     * 
     * @param processor the processor
     * @return the metadata
     */
    private PluginMetadata createMetadataForProcessor(Processor<?> processor) {
        // This is a simplified implementation
        // In a full implementation, you would extract metadata from the processor
        return new PluginMetadata() {
            @Override
            public String getId() {
                return processor.getClass().getSimpleName();
            }

            @Override
            public String getName() {
                return processor.getClass().getSimpleName();
            }

            @Override
            public String getVersion() {
                return "1.0.0";
            }

            @Override
            public String getDescription() {
                return "Processor: " + processor.getClass().getSimpleName();
            }

            @Override
            public String getAuthor() {
                return "Web-Harvest";
            }

            @Override
            public String getCategory() {
                return "core";
            }

            @Override
            public String[] getTags() {
                return new String[0];
            }

            @Override
            public String[] getDependencies() {
                return new String[0];
            }

            @Override
            public PluginConfigurationSchema getConfigurationSchema() {
                return null;
            }

            @Override
            public String getNamespace() {
                return "http://org.webharvest/schema/2.1/core";
            }

            @Override
            public String getElementName() {
                return processor.getClass().getSimpleName().toLowerCase();
            }

            @Override
            public boolean isCorePlugin() {
                return true;
            }

            @Override
            public String getClassName() {
                return processor.getClass().getName();
            }
        };
    }
}
