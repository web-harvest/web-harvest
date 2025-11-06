/*
 Copyright (c) 2006-2025 the original author or authors.

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
 */

package org.webharvest.plugin.schema;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.definition.validation.SchemaResolver;
import org.webharvest.definition.validation.SchemaResolverPostProcessor;
import org.webharvest.definition.validation.SchemaSource;
import org.webharvest.plugin.registry.PluginRegistry;
import org.webharvest.plugin.metadata.XSDGenerator;

/**
 * Post-processor for the schema resolver that generates XSD schemas dynamically
 * from plugin metadata and registers them with the schema resolver.
 * This ensures compatibility between the new plugin architecture and existing
 * XSD-based validation.
 * 
 * <p>Uses XSDGenerator which generates complete schemas from PluginMetadataCatalog,
 * including all elements, attributes, bodyElements group, bodyType, and config element.</p>
 *
 * @author Robert Bala
 * @since 2.1.0
 * @version 2.3.0 - Now uses XSDGenerator instead of DynamicSchemaGenerator
 */
public class PluginSchemaPostProcessor implements SchemaResolverPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(PluginSchemaPostProcessor.class);

    private static final String WH_CORE_NAMESPACE = "http://org.webharvest/schema/2.1/core";
    private static final String WH_CORE_10_NAMESPACE = "http://org.webharvest/schema/1.0/config";

    private final PluginRegistry pluginRegistry;

    /**
     * Creates a new PluginSchemaPostProcessor.
     *
     * @param pluginRegistry the plugin registry (used for validation, but XSDGenerator uses PluginMetadataCatalog)
     */
    public PluginSchemaPostProcessor(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcess(SchemaResolver resolver) {
        try {
            // Generate schema for core namespace (2.1) using XSDGenerator
            String coreSchema = XSDGenerator.generateXSD(WH_CORE_NAMESPACE);
            if (coreSchema != null && !coreSchema.trim().isEmpty()) {
                registerSchemaSource(resolver, coreSchema, WH_CORE_NAMESPACE + "/plugins.xsd");
                LOG.debug("Generated and registered plugin schema for core namespace: {}", WH_CORE_NAMESPACE);
            }

            // Generate schema for legacy namespace (1.0) for backward compatibility
            String legacySchema = XSDGenerator.generateXSD(WH_CORE_10_NAMESPACE);
            if (legacySchema != null && !legacySchema.trim().isEmpty()) {
                registerSchemaSource(resolver, legacySchema, WH_CORE_10_NAMESPACE + "/plugins.xsd");
                LOG.debug("Generated and registered plugin schema for legacy namespace: {}", WH_CORE_10_NAMESPACE);
            }

            LOG.info("Successfully generated and registered plugin schemas using XSDGenerator");

        } catch (Exception e) {
            LOG.error("Failed to generate plugin schemas", e);
            // Don't rethrow - we want schema loading to continue even if plugin schema generation fails
        }
    }

    /**
     * Registers a schema source with the resolver.
     *
     * @param resolver the schema resolver
     * @param schemaContent the XSD schema content
     * @param systemId the system ID for the schema
     */
    private void registerSchemaSource(SchemaResolver resolver, String schemaContent, String systemId) {
        try {
            InputStream schemaStream = new ByteArrayInputStream(schemaContent.getBytes("UTF-8"));
            SchemaSource schemaSource = new SchemaSource(schemaStream, systemId);
            resolver.registerSchemaSource(schemaSource);
        } catch (Exception e) {
            LOG.error("Failed to register schema source for system ID: " + systemId, e);
        }
    }
}
