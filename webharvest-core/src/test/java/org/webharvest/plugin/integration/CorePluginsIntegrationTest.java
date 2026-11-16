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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.variables.ListVariable;

import static org.testng.Assert.*;

/**
 * Integration tests for core plugins based on XML examples from examples/new_plugins directory.
 * Tests basic functionality: constant, text, var, def, template, file plugins.
 * 
 * These tests verify that plugins work correctly when executed through full XML configurations.
 * 
 * @since 2.2.0
 */
public class CorePluginsIntegrationTest extends XmlBasedIntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(CorePluginsIntegrationTest.class);

    @Test(groups = {"integration", "core-plugins"})
    public void testConstantPlugin() throws Exception {
        LOG.info("Testing ConstantPlugin integration with XML example");
        
        String xmlPath = getNewPluginExamplePath("01_constant_plugin_example.xml");
        String outputPath = "constant_output.txt";
        
        String content = executeAndReadOutputFile(xmlPath, outputPath);
        
        assertNotNull(content);
        assertTrue(content.contains("Hello from Constant Plugin!"), 
            "Output should contain constant text");
    }

    @Test(groups = {"integration", "core-plugins"})
    public void testTextPlugin() throws Exception {
        LOG.info("Testing TextPlugin integration with XML example");
        
        String xmlPath = getNewPluginExamplePath("02_text_plugin_example.xml");
        String content = executeAndReadOutputFile(xmlPath, "text_output.txt");

        assertEquals(content, "This is plain text content from Text Plugin",
            "TextPlugin should write the exact configured text");
    }

    @Test(groups = {"integration", "core-plugins"})
    public void testVarPlugin() throws Exception {
        LOG.info("Testing VarPlugin integration with XML example");
        
        String xmlPath = getNewPluginExamplePath("03_var_plugin_example.xml");
        
        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        assertNotNull(ctx);
        
        // Verify that variables were set
        Variable var = ctx.getVar("testVar");
        assertNotNull(var, "Variable 'testVar' should be set");
    }

    @Test(groups = {"integration", "core-plugins"})
    public void testDefPlugin() throws Exception {
        LOG.info("Testing DefPlugin integration with XML example");
        
        String xmlPath = getNewPluginExamplePath("04_def_plugin_example.xml");
        String content = executeAndReadOutputFile(xmlPath, "def_output.txt");

        assertEquals(content, "Greeting: Hello World!, User: Web-Harvest User",
            "DefPlugin should define variables consumed by the template");
    }

    @Test(groups = {"integration", "core-plugins"})
    public void testSetVarPlugin() throws Exception {
        LOG.info("Testing SetVarPlugin integration with XML example");
        
        String xmlPath = getNewPluginExamplePath("20_setvar_plugin_example.xml");

        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        Variable value = ctx.getVar("initial");

        assertNotNull(value, "Legacy set-var should leave the existing variable available");
        assertEquals(value.toString(), "updated value from setvar example",
            "Legacy set-var name attribute and body value should update the variable");
    }

    @Test(groups = {"integration", "core-plugins"})
    public void testGetVarPlugin() throws Exception {
        LOG.info("Testing GetVarPlugin integration with XML example");
        
        String xmlPath = getNewPluginExamplePath("21_getvar_plugin_example.xml");

        assertEquals(executeAndGetVariable(xmlPath, "retrievedText").toString(), "Hello World",
            "GetVarPlugin should return the retrieved text variable to its caller");
        assertEquals(executeAndGetVariable(xmlPath, "retrievedNumber").toString(), "42",
            "GetVarPlugin should return the retrieved numeric variable to its caller");
    }

    @Test(groups = {"integration", "core-plugins"})
    public void testFilePlugin() throws Exception {
        LOG.info("Testing FilePlugin integration with XML example");

        String xmlPath = getNewPluginExamplePath("22_file_plugin_example.xml");
        String content = executeAndReadOutputFile(xmlPath, "test_file.txt");

        assertEquals(content, "Hello from FilePlugin!",
            "FilePlugin should write the exact configured body to the target directory");
    }

    @Test(groups = {"integration", "core-plugins"})
    public void testSleepPlugin() throws Exception {
        LOG.info("Testing SleepPlugin integration with XML example");
        
        String xmlPath = getNewPluginExamplePath("23_sleep_plugin_example.xml");
        
        long startTime = System.nanoTime();
        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        long durationMillis = (System.nanoTime() - startTime) / 1_000_000;

        assertNotNull(ctx);

        LOG.debug("Sleep execution took {} ms", durationMillis);
        assertTrue(durationMillis >= 80,
            "Sleep plugin should honor the configured 100 ms delay (allowing scheduler and clock tolerance)");
    }

    @Test(groups = {"integration", "core-plugins"})
    public void testListPlugin() throws Exception {
        LOG.info("Testing ListPlugin integration with XML example");

        String xmlPath = getNewPluginExamplePath("28_list_plugin_example.xml");
        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        Variable result = ctx.getVar("listResult");

        assertNotNull(result, "ListPlugin should expose its result in the execution context");
        assertTrue(result instanceof ListVariable, "ListPlugin should return a ListVariable");

        ListVariable list = (ListVariable) result;
        assertEquals(list.getList().size(), 3, "ListPlugin should preserve all three child values");
        assertEquals(list.get(0).toString(), "Item 1");
        assertEquals(list.get(1).toString(), "Item 2");
        assertEquals(list.get(2).toString(), "Item 3");
    }

    @Test(groups = {"integration", "core-plugins"})
    public void testValueOfPlugin() throws Exception {
        LOG.info("Testing ValueOfPlugin integration with XML example");

        String xmlPath = getNewPluginExamplePath("29_valueof_plugin_example.xml");
        DynamicScopeContext ctx = executeXmlConfig(xmlPath);
        Variable result = ctx.getVar("evaluatedValue");

        assertNotNull(result, "The enclosing def should capture the value-of result");
        assertEquals(result.toString(), "Hello World",
            "ValueOfPlugin should evaluate the configured expression in the current context");
    }

}

