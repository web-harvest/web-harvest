/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Feature Request #2: Log Plugin
 * 
 * <p>
 * Writes text to standard output (stdout) or standard error (stderr).
 * Useful for debugging, progress monitoring, and integration with CI/CD pipelines.
 * </p>
 * 
 * <h3>Overview</h3>
 * <p>
 * The {@code <log>} plugin provides a simple way to output messages during scraper execution.
 * Unlike the {@code <echo>} plugin which stores values in variables, {@code <log>} writes
 * directly to console streams, making it perfect for:
 * </p>
 * <ul>
 *   <li><strong>Debugging:</strong> Track execution flow and variable values</li>
 *   <li><strong>Progress Monitoring:</strong> Show scraping progress in real-time</li>
 *   <li><strong>Error Reporting:</strong> Log errors to stderr for CI/CD visibility</li>
 *   <li><strong>Production Monitoring:</strong> Track scraper behavior in batch mode</li>
 * </ul>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>dest</strong> (optional): Destination stream
 *     <ul>
 *       <li>"out" - Standard output (default)</li>
 *       <li>"err" - Standard error</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Basic Progress Logging</h4>
 * <pre>{@code
 * <config>
 *     <loop item="url" index="i">
 *         <list>
 *             <template>https://page1.com</template>
 *             <template>https://page2.com</template>
 *             <template>https://page3.com</template>
 *         </list>
 *         
 *         <log>Processing page ${i}: ${url}</log>
 *         
 *         <var-def name="response">
 *             <http url="${url}"/>
 *         </var-def>
 *     </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Error Logging</h4>
 * <pre>{@code
 * <config>
 *     <try>
 *         <var-def name="data">
 *             <http url="https://api.example.com/data"/>
 *         </var-def>
 *     </try>
 *     <catch>
 *         <log dest="err">ERROR: Failed to fetch data - ${sys.exception}</log>
 *         <log dest="err">Timestamp: ${sys.currentTime()}</log>
 *     </catch>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Debug Information</h4>
 * <pre>{@code
 * <config>
 *     <log>Starting scraper...</log>
 *     
 *     <var-def name="page">
 *         <http url="https://example.com"/>
 *     </var-def>
 *     
 *     <log>Page fetched: ${sys.length(page)} bytes</log>
 *     
 *     <var-def name="title">
 *         <xpath type="css" expression="title">
 *             <html-to-xml>${page}</html-to-xml>
 *         </xpath>
 *     </var-def>
 *     
 *     <log>Title extracted: ${title}</log>
 *     <log>Scraper completed successfully!</log>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Conditional Logging</h4>
 * <pre>{@code
 * <config>
 *     <var-def name="count">
 *         <xpath expression="count(//item)">
 *             <html-to-xml>${response}</html-to-xml>
 *         </xpath>
 *     </var-def>
 *     
 *     <if condition="${count > 0}">
 *         <log>Found ${count} items to process</log>
 *     </if>
 *     <else>
 *         <log dest="err">WARNING: No items found!</log>
 *     </else>
 * </config>
 * }</pre>
 * 
 * <h3>Best Practices</h3>
 * <ul>
 *   <li><strong>Use stdout for info:</strong> Progress, status, normal messages</li>
 *   <li><strong>Use stderr for errors:</strong> Errors, warnings, failures</li>
 *   <li><strong>Include context:</strong> Add timestamps, iteration counters, variable values</li>
 *   <li><strong>Be concise:</strong> Short messages are easier to read in logs</li>
 *   <li><strong>Structure messages:</strong> Use prefixes like [INFO], [ERROR], [DEBUG]</li>
 * </ul>
 * 
 * <h3>Integration with CI/CD</h3>
 * <p>
 * LogPlugin is particularly useful in CI/CD pipelines:
 * </p>
 * <ul>
 *   <li>Stdout messages appear in build logs</li>
 *   <li>Stderr triggers CI/CD error detection</li>
 *   <li>Real-time monitoring during batch execution</li>
 *   <li>No file management needed</li>
 * </ul>
 * 
 * <h3>Comparison with Alternatives</h3>
 * 
 * <table border="1">
 *   <tr>
 *     <th>Feature</th>
 *     <th>&lt;log&gt;</th>
 *     <th>&lt;echo&gt;</th>
 *     <th>SLF4J</th>
 *   </tr>
 *   <tr>
 *     <td>Console output</td>
 *     <td>✅ Yes</td>
 *     <td>❌ No (variables)</td>
 *     <td>✅ Yes (Java)</td>
 *   </tr>
 *   <tr>
 *     <td>stdout/stderr control</td>
 *     <td>✅ Yes</td>
 *     <td>❌ No</td>
 *     <td>⚠️ Via config</td>
 *   </tr>
 *   <tr>
 *     <td>XML usage</td>
 *     <td>✅ Direct</td>
 *     <td>✅ Direct</td>
 *     <td>❌ Java only</td>
 *   </tr>
 *   <tr>
 *     <td>CI/CD friendly</td>
 *     <td>✅ Perfect</td>
 *     <td>❌ No</td>
 *     <td>✅ Yes</td>
 *   </tr>
 * </table>
 * 
 * <h3>Return Value</h3>
 * <p>
 * The plugin returns the logged message as a {@link Variable}, allowing it to be
 * used in variable assignments or passed to other processors.
 * </p>
 * 
 * <pre>{@code
 * <!-- Log AND store the message -->
 * <var-def name="status">
 *     <log>Processing completed at ${sys.currentTime()}</log>
 * </var-def>
 * }</pre>
 * 
 * @author Robert Bala (TDD implementation based on original concept by zsombor)
 * @since 2.2.0
 * @see org.webharvest.plugin.core.EchoPlugin
 */
@CorePlugin(
    elementName = "log",
    description = "Log plugin that writes text to stdout or stderr",
    version = "2.2.0"
)
public class LogPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogPlugin.class);
    
    private String dest;

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        
        String dest = configuration.getProperty("dest");
        if (dest != null && !dest.isEmpty()) {
            if (!"out".equals(dest) && !"err".equals(dest)) {
                throw new PluginValidationException("dest must be 'out' or 'err', got: " + dest);
            }
        }
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        this.dest = configuration.getProperty("dest");
        if (this.dest == null || this.dest.trim().isEmpty()) {
            this.dest = "out"; // Default to stdout
        }
        
        LOGGER.debug("LogPlugin initialized: dest={}", this.dest);
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        // Get body content
        Variable bodyContent = executeBody(context);
        
        if (bodyContent == null || bodyContent instanceof EmptyVariable) {
            return EmptyVariable.INSTANCE;
        }
        
        String message = bodyContent.toString();
        
        // Write to appropriate stream
        return writeLog(message, this.dest);
    }
    
    /**
     * Write log message to stdout or stderr.
     * TDD helper method for testing.
     * 
     * @param message Message to log
     * @param destination "out" for stdout, "err" for stderr
     * @return Original message as Variable
     */
    protected Variable writeLog(String message, String destination) {
        if ("err".equals(destination)) {
            System.err.println(message);
            LOGGER.debug("Logged to stderr: {}", message);
        } else {
            System.out.println(message);
            LOGGER.debug("Logged to stdout: {}", message);
        }
        
        return new NodeVariable(message);
    }

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "log";
        }

        @Override
        public String getName() {
            return "Log Plugin";
        }

        @Override
        public String getVersion() {
            return "2.2.0";
        }

        @Override
        public String getDescription() {
            return "Writes text to standard output (stdout) or standard error (stderr)";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            return "core";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "log", "output", "debug", "console"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return LogPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "log";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new PluginConfigurationSchema() {
                @Override
                public AttributeDefinition[] getAttributeDefinitions() {
                    return new AttributeDefinition[]{
                        new AttributeDefinition() {
                            @Override
                            public String getName() {
                                return "dest";
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
                                return "out";
                            }

                            @Override
                            public String getDescription() {
                                return "Destination: 'out' for stdout, 'err' for stderr";
                            }

                            @Override
                            public String[] getAllowedValues() {
                                return new String[]{"out", "err"};
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
                public String getVersion() {
                    return "2.2.0";
                }

                @Override
                public boolean allowsBody() {
                    return true;
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
                    return new String[]{"dest"};
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
    };

    @Override
    public PluginMetadata getMetadata() {
        return METADATA;
    }
}

