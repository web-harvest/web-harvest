/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the following
   disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the following
   disclaimer in the documentation and/or other materials provided
   with the distribution.

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

package org.webharvest.plugin.integration;

import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Deterministic integration tests for the HTTP plugin.
 *
 * <p>The base test fixture injects a local HttpService stub. These tests
 * therefore exercise the complete XML/plugin/response path without relying
 * on external network availability.</p>
 *
 * @since 2.2.0
 */
public class HttpPluginsIntegrationTest extends XmlBasedIntegrationTestBase {

    @Test(groups = {"integration", "http-plugins"})
    public void testHttpPluginReturnsStubbedResponse() throws Exception {
        String xmlPath = getNewPluginExamplePath("05_http_plugin_example.xml");
        String outputPath = "http_output.txt";

        String content = executeAndReadOutputFile(xmlPath, outputPath);

        assertNotNull(content, "HTTP output should be present");
        assertTrue(content.contains("\"url\":\"https://httpbin.org/get\""),
                "Output should contain the deterministic stub response URL");
        assertTrue(content.contains("\"origin\":\"127.0.0.1\""),
                "Output should contain the deterministic stub response body");

        DynamicScopeContext context = executeXmlConfig(xmlPath);
        Variable response = context.getVar("response");
        assertNotNull(response, "The XML definition should store the HTTP response");
        assertTrue(response.toString().contains("\"url\":\"https://httpbin.org/get\""),
                "The response variable should contain the stubbed response");
    }

    @Test(groups = {"integration", "http-plugins"})
    public void testHttpPluginConfigurationExecutesAgainstStub() throws Exception {
        String xmlPath = getNewPluginExamplePath("32_http_plugin_example.xml");

        DynamicScopeContext context = executeXmlConfig(xmlPath);

        assertNotNull(context, "HTTP configuration should produce an execution context");
    }
}
