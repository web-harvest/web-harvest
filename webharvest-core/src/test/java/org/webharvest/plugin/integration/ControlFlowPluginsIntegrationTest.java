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

import static org.testng.Assert.*;

/**
 * Integration tests for control flow plugins: If/Else, Loop, While, Try/Catch, Case plugins.
 *
 * Tests are based on XML examples from examples/new_plugins directory.
 *
 * @since 2.2.0
 */
public class ControlFlowPluginsIntegrationTest extends XmlBasedIntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(ControlFlowPluginsIntegrationTest.class);

    @Test(groups = {"integration", "control-flow-plugins"})
    public void testIfElsePlugin() throws Exception {
        LOG.info("Testing If/Else plugins integration with XML example");

        String xmlPath = getNewPluginExamplePath("11_if_else_plugin_example.xml");
        String output = executeAndReadOutputFile(xmlPath, "if_output.txt");

        assertEquals(output.trim(), "Condition is true!",
            "If should select the true branch and Else should not emit output");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void testLoopPlugin() throws Exception {
        LOG.info("Testing LoopPlugin integration with XML example");

        String xmlPath = getNewPluginExamplePath("12_loop_plugin_example.xml");
        String output = executeAndReadOutputFile(xmlPath, "loop_output.txt");

        int item1 = output.indexOf("Item: item1");
        int item2 = output.indexOf("Item: item2");
        int item3 = output.indexOf("Item: item3");
        assertTrue(item1 >= 0, "Loop output should contain the first item");
        assertTrue(item2 > item1, "Loop output should contain the second item after the first");
        assertTrue(item3 > item2, "Loop output should contain the third item after the second");
        assertEquals(countOccurrences(output, "Item: "), 3,
            "Loop should render exactly one output for each configured item");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void testWhilePlugin() throws Exception {
        LOG.info("Testing WhilePlugin integration with observable iteration output");

        String xmlPath = getNewPluginExamplePath("13_while_plugin_example.xml");
        String output = executeAndReadOutputFile(xmlPath, "while_output.txt");

        int counter0 = output.indexOf("Counter: 0");
        int counter1 = output.indexOf("Counter: 1");
        int counter2 = output.indexOf("Counter: 2");
        assertTrue(counter0 >= 0, "While output should contain the initial counter value");
        assertTrue(counter1 > counter0, "While output should contain the incremented counter value");
        assertTrue(counter2 > counter1, "While output should contain the final counter value");
        assertEquals(countOccurrences(output, "Counter: "), 3,
            "While should execute exactly three iterations before its condition becomes false");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void testTryCatchSkipsCatchWhenTryBodySucceeds() throws Exception {
        LOG.info("Testing that Catch is skipped when the Try body succeeds");

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <try><template>completed</template></try>"
            + "  <catch><def var=\"catchMarker\" value=\"caught\"/></catch>"
            + "  <def var=\"afterTry\" value=\"completed\"/>"
            + "</config>";

        DynamicScopeContext context = executeXmlString(xml);

        assertNull(context.getVar("catchMarker"),
            "Catch body must not execute when the Try body completes without an error");
        assertNotNull(context.getVar("afterTry"),
            "Execution should continue after a successful Try/Catch construct");
        assertEquals(context.getVar("afterTry").toString(), "completed");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void testTryCatchHandlesRuntimeFailureAndSkipsRemainingTryBody() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <try>"
            + "    <xquery><xq-expression><![CDATA[for $item in $input/root/item "
            + "return <result>{$item/text()}</result>]]></xq-expression></xquery>"
            + "    <def var=\"afterFailure\" value=\"must-not-run\"/>"
            + "    <catch>"
            + "      <def var=\"catchMarker\" value=\"caught\"/>"
            + "      <def var=\"caughtError\"><get var=\"error\"/></def>"
            + "      <def var=\"caughtErrorAlias\"><get var=\"_error\"/></def>"
            + "      <def var=\"caughtErrorType\"><get var=\"errorType\"/></def>"
            + "  </try>"
            + "  <catch>"
            + "    <def var=\"catchMarker\" value=\"caught\"/>"
            + "    <def var=\"caughtError\"><get var=\"error\"/></def>"
            + "    <def var=\"caughtErrorAlias\"><get var=\"_error\"/></def>"
            + "    <def var=\"caughtErrorType\"><get var=\"errorType\"/></def>"
            + "  </catch>"
            + "</config>";

        DynamicScopeContext context = executeXmlString(xml);

        assertEquals(context.getVar("catchMarker").toString(), "caught",
            "Catch must execute after a runtime failure in the Try body");
        assertNull(context.getVar("afterFailure"),
            "Remaining Try body siblings must not execute after the first failure");
        assertNotNull(context.getVar("caughtError"),
            "The catch body must be able to read the canonical error variable");
        assertTrue(context.getVar("caughtError").toString().contains("XQuery"),
            "The error variable must retain the underlying XQuery failure");
        assertEquals(context.getVar("caughtErrorAlias").toString(), context.getVar("caughtError").toString(),
            "The legacy _error variable must remain available as an alias");
        assertNotNull(context.getVar("caughtErrorType"),
            "The catch body must be able to read the error type");
        assertFalse(context.getVar("caughtErrorType").toString().trim().isEmpty(),
            "The error type must not be empty");
    }
    @Test(groups = {"integration", "control-flow-plugins"})
    public void testOrdinaryBodyFailureOutsideTryKeepsBestEffortSiblingExecution() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <regexp><regexp-pattern>[</regexp-pattern><regexp-source>value</regexp-source></regexp>"
            + "  <def var=\"afterFailure\" value=\"continued\"/>"
            + "</config>";

        DynamicScopeContext context = executeXmlString(xml);

        assertEquals(context.getVar("afterFailure").toString(), "continued",
            "Ordinary failures outside Try must preserve best-effort sibling execution");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void testCasePlugin() throws Exception {
        LOG.info("Testing CasePlugin integration with XML example");

        String xmlPath = getNewPluginExamplePath("27_case_plugin_example.xml");
        String output = executeAndReadOutputFile(xmlPath, "case_output.txt");

        assertEquals(output.trim(), "Case 2",
            "Case should emit only the branch matching the configured value");
    }

    private int countOccurrences(String text, String token) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }
}
