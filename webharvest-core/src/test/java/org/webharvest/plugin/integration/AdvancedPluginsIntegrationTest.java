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

package org.webharvest.plugin.integration;

import org.testng.annotations.Test;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.assertEquals;

/**
 * Integration tests for variable definition and template output.
 *
 * Include and XSLT currently have known implementation gaps tracked in issue
 * #143. Function and CallParam behavior is covered by FunctionCallIntegrationTest.
 *
 * @since 2.2.0
 */
public class AdvancedPluginsIntegrationTest extends XmlBasedIntegrationTestBase {

    @Test(groups = {"integration", "advanced-plugins"})
    public void testDefPluginStoresExactValueInContext() throws Exception {
        Variable value = executeAndGetVariable(
                getNewPluginExamplePath("35_def_plugin_example.xml"), "testVar");

        assertEquals(value.toString(), "Hello World",
                "DefPlugin must store the configured value in the execution context");
    }

    @Test(groups = {"integration", "advanced-plugins"})
    public void testVariablePluginStoresExactBodyValueInContext() throws Exception {
        Variable value = executeAndGetVariable(
                getNewPluginExamplePath("03_var_plugin_example.xml"), "testVar");

        assertEquals(value.toString(), "Sample value from var example",
                "Variable body content must be preserved exactly");
    }

    @Test(groups = {"integration", "advanced-plugins"})
    public void testDefinitionTemplateWritesExpectedOutput() throws Exception {
        String output = executeAndReadOutputFile(
                getNewPluginExamplePath("04_def_plugin_example.xml"), "def_output.txt");

        assertEquals(output, "Greeting: Hello World!, User: Web-Harvest User",
                "Template output must contain the interpolated variables");
    }
}
