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

package org.webharvest.plugin.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * ZIP processor - creates ZIP archives from child zip-entry elements.
 * 
 * @author WebHarvest Team
 * @since 2.1.0
 */
@CorePlugin(elementName = "zip")
public class ZipPlugin extends AbstractPlugin {

    private ZipOutputStream zipOutStream = null;

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // No initialization needed
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        zipOutStream = new ZipOutputStream(byteArrayOutputStream);
        
        try {
            executeBody(context);
        } finally {
            try {
                zipOutStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing ZIP stream: " + e.getMessage(), e);
        }
        }
        
        return new NodeVariable(byteArrayOutputStream.toByteArray());
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        // No validation needed - no attributes
    }

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() {
                return "zip";
            }

            @Override
            public String getName() {
                return "ZIP Plugin";
            }

            @Override
            public String getVersion() {
                return "2.1.0";
            }

            @Override
            public String getDescription() {
                return "Creates ZIP archives from child zip-entry elements";
            }

            @Override
            public String getAuthor() {
                return "Web-Harvest Team";
            }

            @Override
            public String getCategory() {
                return "Archive";
            }

            @Override
            public String[] getTags() {
                return new String[]{"zip", "archive", "compression"};
            }

            @Override
            public String[] getDependencies() {
                return new String[0];
            }

            @Override
            public PluginConfigurationSchema getConfigurationSchema() {
                return ZipPlugin.this.getConfigurationSchema();
            }

            @Override
            public String getNamespace() {
                return "http://org.webharvest/schema/2.1/zip";
            }

            @Override
            public String getElementName() {
                return "zip";
            }

            @Override
            public boolean isCorePlugin() {
                return false;
            }

            @Override
            public String getClassName() {
                return ZipPlugin.class.getName();
            }
        };
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new PluginConfigurationSchema() {
            @Override
            public AttributeDefinition[] getAttributeDefinitions() {
                return new AttributeDefinition[0];
            }

            @Override
            public AttributeDefinition getAttribute(String name) {
                return null;
            }

            @Override
            public String[] getRequiredAttributes() {
                return new String[0];
            }

            @Override
            public String[] getOptionalAttributes() {
                return new String[0];
            }

            @Override
            public String[] getAllowedSubElements() {
                return new String[]{"zip-entry"};
            }

            @Override
            public String[] getRequiredSubElements() {
                return new String[0];
            }

            @Override
            public boolean allowsBody() {
                return true;
            }

            @Override
            public String getVersion() {
                return "2.1.0";
            }

            @Override
            public void validate(PluginConfiguration configuration) throws PluginValidationException {
                // No validation needed - no attributes
            }
        };
    }

    /**
     * Gets the ZIP output stream for child zip-entry elements.
     * 
     * @return the ZIP output stream
     */
    public ZipOutputStream getZipOutStream() {
        return zipOutStream;
    }
}
