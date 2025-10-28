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

package org.webharvest.plugin.core;

import org.webharvest.exception.PluginException;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Sleep/delay processor - pauses execution for specified duration.
 * 
 * <p>
 * The {@code <sleep>} processor pauses scraper execution for a specified time period.
 * This is essential for rate limiting, respecting robots.txt crawl delays, waiting for
 * dynamic content, retry backoff strategies, and avoiding server overload.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Fixed delay between requests</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <loop item="url" maxloops="100">
 *   <tokenize>${urls}</tokenize>
 *   
 *   <http url="${url}"/>
 *   
 *   <!-- Wait 2 seconds between requests -->
 *   <sleep milliseconds="2000"/>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Retry with exponential backoff</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="retryCount" value="0"/>
 * <def var="success" value="false"/>
 * 
 * <while condition="${!success && retryCount < 5}">
 *   <def var="response">
 *     <http url="https://api.example.com/data"/>
 *   </def>
 *   
 *   <if condition="${empty(response)}">
 *     <!-- Exponential backoff: 1s, 2s, 4s, 8s, 16s -->
 *     <def var="delay">
 *       <script>Math.pow(2, parseInt(context.getVar("retryCount"))) * 1000</script>
 *     </def>
 *     <sleep milliseconds="${delay}"/>
 *     <def var="retryCount">
 *       <script>parseInt(context.getVar("retryCount")) + 1</script>
 *     </def>
 *   </if>
 *   <else>
 *     <def var="success" value="true"/>
 *   </else>
 * </while>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Respect rate limits</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- API allows 10 requests per minute -->
 * <loop item="endpoint" maxloops="10">
 *   <tokenize>${endpoints}</tokenize>
 *   
 *   <http url="https://api.example.com/${endpoint}"/>
 *   
 *   <!-- 6 seconds = 10 requests/minute -->
 *   <sleep milliseconds="6000"/>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>milliseconds:</strong> Sleep duration in milliseconds (1000ms = 1 second)</li>
 *   <li><strong>time:</strong> Alternative attribute name for milliseconds</li>
 * </ul>
 * 
 * <h3>Common Durations</h3>
 * <ul>
 *   <li><strong>100ms:</strong> milliseconds="100" (very short delay)</li>
 *   <li><strong>1 second:</strong> milliseconds="1000"</li>
 *   <li><strong>5 seconds:</strong> milliseconds="5000"</li>
 *   <li><strong>1 minute:</strong> milliseconds="60000"</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Thread blocking:</strong> Blocks current thread (no other processing during sleep)</li>
 *   <li><strong>Interruptible:</strong> Sleep can be interrupted by cancel/stop operations</li>
 *   <li><strong>Template support:</strong> Duration supports variable interpolation</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <http>} - Often used together for rate limiting</li>
 *   <li>{@code <while>} - Retry loops with sleep backoff</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 2.0
 */
@CorePlugin(
    elementName = "sleep",
    description = "Sleep plugin that pauses execution for specified milliseconds",
    version = "2.1.0",
    category = "Utilities"
)
public class SleepPlugin extends AbstractPlugin {

    private static final String MILLISECONDS_ATT_NAME = "milliseconds";

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() {
                return "sleep";
            }

            @Override
            public String getName() {
                return "Sleep Plugin";
            }

            @Override
            public String getVersion() {
                return "2.1.0";
            }

            @Override
            public String getDescription() {
                return "Sleep plugin that pauses execution for specified number of milliseconds";
            }

            @Override
            public String getAuthor() {
                return "Web-Harvest Team";
            }

            @Override
            public String getCategory() {
                return "Utilities";
            }

            @Override
            public String[] getTags() {
                return new String[]{"core", "sleep", "delay", "pause"};
            }

            @Override
            public String[] getDependencies() {
                return new String[0];
            }

            @Override
            public String getClassName() {
                return SleepPlugin.class.getName();
            }

            @Override
            public boolean isCorePlugin() {
                return true;
            }

            @Override
            public String getElementName() {
                return "sleep";
            }

            @Override
            public String getNamespace() {
                return "http://org.webharvest/schema/2.1/core";
            }

            @Override
            public PluginConfigurationSchema getConfigurationSchema() {
                return SleepPlugin.this.getConfigurationSchema();
            }
        };
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        // No validation needed - milliseconds is handled in doExecute with default value
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new PluginConfigurationSchema() {
            @Override
            public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
                return new PluginConfigurationSchema.AttributeDefinition[]{
                    new PluginConfigurationSchema.AttributeDefinition() {
                        @Override
                        public String getName() {
                            return MILLISECONDS_ATT_NAME;
                        }

                        @Override
                        public PluginConfigurationSchema.AttributeType getType() {
                            return PluginConfigurationSchema.AttributeType.INTEGER;
                        }

                        @Override
                        public boolean isRequired() {
                            return false;
                        }

                        @Override
                        public String getDefaultValue() {
                            return "0";
                        }

                        @Override
                        public String getDescription() {
                            return "Number of milliseconds to sleep";
                        }

                        @Override
                        public String[] getAllowedValues() {
                            return null;
                        }

                        @Override
                        public Number getMinValue() {
                            return 0;
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
            public PluginConfigurationSchema.AttributeDefinition getAttribute(String name) {
                for (PluginConfigurationSchema.AttributeDefinition attr : getAttributeDefinitions()) {
                    if (attr.getName().equals(name)) {
                        return attr;
                    }
                }
                return null;
            }

            @Override
            public String getVersion() {
                return "2.1.0";
            }

            @Override
            public boolean allowsBody() {
                return false;
            }

            @Override
            public String[] getRequiredSubElements() {
                return new String[0];
            }

            @Override
            public String[] getAllowedSubElements() {
                return new String[0];
            }

            @Override
            public String[] getOptionalAttributes() {
                return new String[]{MILLISECONDS_ATT_NAME};
            }

            @Override
            public String[] getRequiredAttributes() {
                return new String[0];
            }

            @Override
            public void validate(PluginConfiguration configuration) throws PluginValidationException {
                if (configuration == null) {
                    throw new PluginValidationException("Configuration cannot be null");
                }
            }
        };
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        String millisecondsStr = getConfiguration().getProperty("milliseconds");
        int millis = 0;
        if (millisecondsStr != null && !millisecondsStr.trim().isEmpty()) {
            try {
                millis = Integer.parseInt(millisecondsStr.trim());
            } catch (NumberFormatException e) {
                // Use default value 0 for invalid input, like original plugin
                millis = 0;
            }
        }
        // Sleep configuration completed
        if (millis > 0) {
            Thread.sleep(millis);
        }
        
        // Execute child elements using new architecture
        Variable bodyResult = super.executeBody(context);
        return bodyResult != null ? bodyResult : EmptyVariable.INSTANCE;
    }
}
