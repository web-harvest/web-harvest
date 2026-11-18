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

package org.webharvest.plugin.ftp;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginValidationException;

/**
 * Complete metadata and configuration-schema contract tests for FTP plugins.
 */
public class FtpPluginMetadataSchemaTest {

    @Test
    public void allFtpPluginsExposeCompleteMetadataAndSchemaContracts() throws Exception {
        AbstractPlugin[] plugins = {
                new FtpPlugin(),
                new FtpGetPlugin(),
                new FtpListPlugin(),
                new FtpPutPlugin()
        };
        String[] elementNames = { "ftp", "ftp-get", "ftp-list", "ftp-put" };
        String[] ids = { "FtpPlugin", "FtpGetPlugin", "FtpListPlugin", "FtpPutPlugin" };

        for (int index = 0; index < plugins.length; index++) {
            AbstractPlugin plugin = plugins[index];
            PluginMetadata metadata = plugin.getMetadata();

            assertNotNull(metadata.getName());
            assertNotNull(metadata.getDescription());
            assertEquals(metadata.getVersion(), "2.1.0");
            assertEquals(metadata.getClassName(), plugin.getClass().getName());
            assertTrue(metadata.isCorePlugin());
            assertEquals(metadata.getElementName(), elementNames[index]);
            assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
            assertEquals(metadata.getDependencies().length, 0);
            assertEquals(metadata.getTags().length, 0);
            assertNotNull(metadata.getCategory());
            assertEquals(metadata.getAuthor(), "Web-Harvest Team");
            assertEquals(metadata.getId(), ids[index]);
            assertNotNull(metadata.getDeclaredRoles());

            PluginConfigurationSchema schema = metadata.getConfigurationSchema();
            assertNotNull(schema);
            assertEquals(schema.getAttributeDefinitions().length, 0);
            assertTrue(schema.getRequiredAttributes().length > 0);
            assertTrue(schema.getOptionalAttributes().length > 0);
            assertEquals(schema.getVersion(), "2.1.0");
            assertEquals(schema.allowsBody(), index != 0);
            assertEquals(schema.getRequiredSubElements().length, 0);
            assertNotNull(schema.getAllowedSubElements());
            assertNull(schema.getAttribute("host"));

            if (index == 0) {
                assertEquals(schema.getAllowedSubElements().length, 3);
                assertEquals(schema.getRequiredParents().length, 0);
                assertTrue(schema.canAppearAtRoot());
                assertFalse(schema.isContextSpecific());
            } else {
                assertEquals(schema.getAllowedSubElements().length, 0);
                assertEquals(schema.getRequiredParents().length, 1);
                assertEquals(schema.getRequiredParents()[0], "ftp");
                assertTrue(schema.isContextSpecific());
                assertEquals(schema.canAppearAtRoot(), index != 3);
            }

            schema.validate(configurationWithHost("localhost"));
            assertSchemaRejects(schema, null);
            assertSchemaRejects(schema, configurationWithHost(null));
            assertSchemaRejects(schema, configurationWithHost(""));
        }
    }

    private PluginConfiguration configurationWithHost(String host) {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            if ("host".equals(name)) {
                return host;
            }
            if ("username".equals(name) || "user".equals(name)) {
                return "ftp-user";
            }
            if ("password".equals(name)) {
                return "secret";
            }
            if ("action".equals(name)) {
                return "list";
            }
            return null;
        });
        return configuration;
    }

    private void assertSchemaRejects(PluginConfigurationSchema schema,
                                     PluginConfiguration configuration) throws Exception {
        try {
            schema.validate(configuration);
            fail("Expected FTP schema validation to reject invalid configuration");
        } catch (PluginValidationException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
