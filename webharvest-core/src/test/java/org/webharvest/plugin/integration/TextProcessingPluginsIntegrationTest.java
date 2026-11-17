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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Integration tests for Regexp, Tokenize and Script plugins.
  *
  * Each test asserts the values produced by the plugin in the execution context.
  *
  * @since 2.2.0
  */
public class TextProcessingPluginsIntegrationTest extends XmlBasedIntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(TextProcessingPluginsIntegrationTest.class);

    @Test(groups = {"integration", "text-processing-plugins"})
    public void testRegexpPluginExposesMatchAndCaptureGroups() throws Exception {
        LOG.info("Testing Regexp match output and capture-group variables");

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <def var=\"matches\">"
            + "    <regexp>"
            + "      <regexp-pattern>(\\w+)-(\\d+)</regexp-pattern>"
            + "      <regexp-source>item-42</regexp-source>"
            + "      <regexp-result>&lt;var name='group1'/&gt;-&lt;var name='group2'/&gt;</regexp-result>"
            + "    </regexp>"
            + "  </def>"
            + "</config>";

        DynamicScopeContext context = executeXmlString(xml);
        Variable matches = context.getVar("matches");

        assertNotNull(matches, "The enclosing definition should store the regexp result");
        assertTrue(matches instanceof ListVariable, "Regexp should return a ListVariable");
        ListVariable resultList = (ListVariable) matches;
        assertEquals(resultList.getList().size(), 1, "The expression should produce exactly one match");
        assertEquals(resultList.get(0).toString(), "item-42",
            "Regexp should substitute both capture groups in the configured result template");
    }

    @Test(groups = {"integration", "text-processing-plugins"})
    public void testTokenizePluginSplitsTextIntoOrderedTokens() throws Exception {
        LOG.info("Testing Tokenize output and token order");

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <def var=\"source\">alpha,beta,gamma</def>"
            + "  <def var=\"tokens\">"
            + "    <tokenize delimiters=\",\"><get var=\"source\"/></tokenize>"
            + "  </def>"
            + "</config>";

        DynamicScopeContext context = executeXmlString(xml);
        Variable tokens = context.getVar("tokens");

        assertNotNull(tokens, "The enclosing definition should store the token list");
        assertTrue(tokens instanceof ListVariable, "Tokenize should return a ListVariable");
        ListVariable resultList = (ListVariable) tokens;
        assertEquals(resultList.getList().size(), 3, "Tokenize should produce three tokens");
        assertEquals(resultList.get(0).toString(), "alpha");
        assertEquals(resultList.get(1).toString(), "beta");
        assertEquals(resultList.get(2).toString(), "gamma");
    }

    @Test(groups = {"integration", "text-processing-plugins"})
    public void testScriptPluginEvaluatesJavaScriptAndReturnsValue() throws Exception {
        LOG.info("Testing Script output using the JavaScript engine");

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <def var=\"scriptCode\">var result = \"Hello from JavaScript!\"; result;</def>"
            + "  <def var=\"scriptResult\">"
            + "    <script language=\"javascript\"><get var=\"scriptCode\"/></script>"
            + "  </def>"
            + "</config>";

        DynamicScopeContext context = executeXmlString(xml);
        Variable result = context.getVar("scriptResult");

        assertNotNull(result, "The enclosing definition should store the script result");
        assertEquals(result.toString().trim(), "Hello from JavaScript!",
            "Script should evaluate the configured JavaScript and return its final expression");
    }
}
