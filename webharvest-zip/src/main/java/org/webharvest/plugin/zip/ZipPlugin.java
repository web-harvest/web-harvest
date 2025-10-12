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
 * ZIP archive creation processor - packages multiple files into ZIP archives.
 * 
 * <p>
 * The {@code <zip>} processor creates ZIP archives from multiple {@code <zip-entry>} child elements.
 * Supports both text and binary content with automatic compression.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Create ZIP archive with scraped data</h4>
 * <pre>{@code
 * <zip path="output/scraped-data.zip">
 *     <zip-entry name="products.xml">
 *         <http url="https://example.com/products"/>
 *     </zip-entry>
 *     
 *     <zip-entry name="data.json">
 *         <http url="https://api.example.com/data"/>
 *     </zip-entry>
 * </zip>
 * }</pre>
 * 
 * <h4>Example 2: Archive multiple pages with loop</h4>
 * <pre>{@code
 * <def var="pages">1,2,3,4,5</def>
 * 
 * <zip path="pages-archive.zip">
 *     <loop item="page" delimiter=",">
 *         ${pages}
 *         
 *         <def var="pageContent">
 *             <http url="https://example.com/page/${page}"/>
 *         </def>
 *         
 *         <zip-entry name="page-${page}.html">
 *             ${pageContent}
 *         </zip-entry>
 *     </loop>
 * </zip>
 * }</pre>
 * 
 * <h4>Example 3: Archive binary files (images)</h4>
 * <pre>{@code
 * <zip path="images.zip">
 *     <loop item="imageUrl">
 *         <xpath expression="//img/@src">
 *             <html-to-xml>
 *                 <http url="https://example.com"/>
 *             </html-to-xml>
 *         </xpath>
 *         
 *         <def var="imageData">
 *             <http url="${imageUrl}"/>
 *         </def>
 *         
 *         <zip-entry name="images/image-${_index}.jpg" type="binary">
 *             <get-var var="imageData"/>
 *         </zip-entry>
 *     </loop>
 * </zip>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>path:</strong> Output ZIP file path (required)</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Compression:</strong> Standard DEFLATE compression algorithm</li>
 *   <li><strong>Path separators:</strong> Use forward slashes (/) for ZIP entry paths</li>
 *   <li><strong>Memory:</strong> Entire ZIP built in memory - consider size</li>
 *   <li><strong>Overwrite:</strong> Existing file is overwritten</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <zip-entry>} - Define ZIP entries</li>
 *   <li>{@code <file>} - File I/O</li>
 *   <li>{@code <http>} - Download content to archive</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 2.0
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
