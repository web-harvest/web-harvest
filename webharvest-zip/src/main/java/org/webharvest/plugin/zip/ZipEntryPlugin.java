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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition;
import org.webharvest.plugin.PluginConfigurationSchema.AttributeType;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;

/**
 * Zip entry plugin - can be used only inside zip plugin.
 * 
 * @author WebHarvest Team
 * @since 2.1.0
 */
@CorePlugin(elementName = "zip-entry")
public class ZipEntryPlugin extends AbstractPlugin {

    private static final String NAME_ATT_NAME = "name";
    private static final String CHARSET_ATT_NAME = "charset";

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // No initialization needed
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        // Get parent ZIP plugin from context
        ZipPlugin zipPlugin = getParentZipPlugin(context);
        if (zipPlugin == null) {
            throw new RuntimeException("Cannot use zip entry plugin out of zip plugin context!");
        }

        String name = getConfiguration().getProperty(NAME_ATT_NAME);
        if (CommonUtil.isEmptyString(name)) {
            throw new RuntimeException("Name of zip entry cannot be empty!");
        }

        String charset = getConfiguration().getProperty(CHARSET_ATT_NAME);
        if (CommonUtil.isEmptyString(charset)) {
            charset = context.getCharset();
        }

        ZipOutputStream zipOutStream = zipPlugin.getZipOutStream();
        Variable bodyResult = executeBody(context);
        
        try {
            zipOutStream.putNextEntry(new ZipEntry(name));
            zipOutStream.write(bodyResult.toBinary(charset));
            zipOutStream.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException("Error writing ZIP entry: " + e.getMessage(), e);
        }
        
        return EmptyVariable.INSTANCE;
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        String name = configuration.getProperty(NAME_ATT_NAME);
        if (CommonUtil.isEmptyString(name)) {
            throw new PluginValidationException("Name attribute is required");
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() {
                return "zip-entry";
            }

            @Override
            public String getName() {
                return "ZIP Entry Plugin";
            }

            @Override
            public String getVersion() {
                return "2.1.0";
            }

            @Override
            public String getDescription() {
                return "Adds an entry to a ZIP archive";
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
                return new String[]{"zip", "entry", "file"};
            }

            @Override
            public String[] getDependencies() {
                return new String[]{"zip"};
            }

            @Override
            public PluginConfigurationSchema getConfigurationSchema() {
                return ZipEntryPlugin.this.getConfigurationSchema();
            }

            @Override
            public String getNamespace() {
                return "http://org.webharvest/schema/2.1/zip";
            }

            @Override
            public String getElementName() {
                return "zip-entry";
            }

            @Override
            public boolean isCorePlugin() {
                return false;
            }

            @Override
            public String getClassName() {
                return ZipEntryPlugin.class.getName();
            }
        };
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new PluginConfigurationSchema() {
            @Override
            public AttributeDefinition[] getAttributeDefinitions() {
                return new AttributeDefinition[]{
                    new AttributeDefinition() {
                        @Override
                        public String getName() {
                            return NAME_ATT_NAME;
                        }

                        @Override
                        public AttributeType getType() {
                            return AttributeType.STRING;
                        }

                        @Override
                        public boolean isRequired() {
                            return true;
                        }

                        @Override
                        public String getDefaultValue() {
                            return null;
                        }

                        @Override
                        public String getDescription() {
                            return "Name of the ZIP entry";
                        }

                        @Override
                        public String[] getAllowedValues() {
                            return null;
                        }

                        @Override
                        public Number getMinValue() {
                            return null;
                        }

                        @Override
                        public Number getMaxValue() {
                            return null;
                        }

                        @Override
                        public String getPattern() {
                            return null;
                        }
                    },
                    new AttributeDefinition() {
                        @Override
                        public String getName() {
                            return CHARSET_ATT_NAME;
                        }

                        @Override
                        public AttributeType getType() {
                            return AttributeType.STRING;
                        }

                        @Override
                        public boolean isRequired() {
                            return false;
                        }

                        @Override
                        public String getDefaultValue() {
                            return "UTF-8";
                        }

                        @Override
                        public String getDescription() {
                            return "Character set for encoding the entry content";
                        }

                        @Override
                        public String[] getAllowedValues() {
                            Set<String> charsetKeys = Charset.availableCharsets().keySet();
                            return new ArrayList<String>(charsetKeys).toArray(new String[charsetKeys.size()]);
                        }

                        @Override
                        public Number getMinValue() {
                            return null;
                        }

                        @Override
                        public Number getMaxValue() {
                            return null;
                        }

                        @Override
                        public String getPattern() {
                            return null;
                        }
                    }
                };
            }

            @Override
            public AttributeDefinition getAttribute(String name) {
                for (AttributeDefinition attr : getAttributeDefinitions()) {
                    if (attr.getName().equals(name)) {
                        return attr;
                    }
                }
                return null;
            }

            @Override
            public String[] getRequiredAttributes() {
                return new String[]{NAME_ATT_NAME};
            }

            @Override
            public String[] getOptionalAttributes() {
                return new String[]{CHARSET_ATT_NAME};
            }

            @Override
            public String[] getAllowedSubElements() {
                return new String[0];
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
                String name = configuration.getProperty(NAME_ATT_NAME);
                if (CommonUtil.isEmptyString(name)) {
                    throw new PluginValidationException("Name attribute is required");
                }
            }
        };
    }

    /**
     * Gets the parent ZIP plugin from the execution context.
     * This is a simplified approach - in a real implementation, you might need
     * a more sophisticated way to find the parent plugin.
     * 
     * @param context the execution context
     * @return the parent ZIP plugin or null if not found
     */
    private ZipPlugin getParentZipPlugin(DynamicScopeContext context) {
        // This is a simplified implementation
        // In a real scenario, you might need to traverse the processor hierarchy
        // or use a different mechanism to find the parent ZIP plugin
        return null; // TODO: Implement proper parent plugin resolution
    }
}
