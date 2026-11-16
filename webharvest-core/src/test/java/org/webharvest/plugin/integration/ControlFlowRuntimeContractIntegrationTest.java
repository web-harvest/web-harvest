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
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Runtime contract tests for conditional control-flow semantics.
 *
 * These tests intentionally exercise cases not covered by the basic examples:
 * expression evaluation, false branches, default branches and Case first-match.
 */
public class ControlFlowRuntimeContractIntegrationTest extends XmlBasedIntegrationTestBase {

    private static final String CONFIG_PREFIX =
        "<config xmlns=\"http://org.webharvest/schema/2.1/core\">";
    private static final String CONFIG_SUFFIX = "</config>";

    @Test(groups = {"integration", "control-flow-plugins"})
    public void ifMustEvaluateNumericComparisonsRatherThanTreatUnknownExpressionsAsTrue() throws Exception {
        String xml = CONFIG_PREFIX
            + "<def var=\"count\" value=\"1\"/>"
            + "<if condition=\"${count > 10}\">"
            + "  <def var=\"unexpected\" value=\"executed\"/>"
            + "</if>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);

        assertNull(context.getVar("unexpected"),
            "A false numeric comparison must not execute the If body");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void ifMustTreatFalseBooleanLiteralAsFalse() throws Exception {
        String xml = CONFIG_PREFIX
            + "<if condition=\"${false}\">"
            + "  <def var=\"unexpected\" value=\"executed\"/>"
            + "</if>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);

        assertNull(context.getVar("unexpected"),
            "The false boolean literal must not execute the If body");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void elseMustExecuteWhenPrecedingIfConditionIsFalse() throws Exception {
        String xml = CONFIG_PREFIX
            + "<def var=\"condition\" value=\"false\"/>"
            + "<if condition=\"${condition}\">"
            + "  <def var=\"selectedBranch\" value=\"if\"/>"
            + "</if>"
            + "<else>"
            + "  <def var=\"selectedBranch\" value=\"else\"/>"
            + "</else>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        Variable selectedBranch = context.getVar("selectedBranch");

        assertEquals(selectedBranch == null ? null : selectedBranch.toString(), "else",
            "Else must execute when the immediately preceding If condition is false");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void caseMustExecuteOnlyTheFirstMatchingBranch() throws Exception {
        String xml = CONFIG_PREFIX
            + "<def var=\"value\" value=\"2\"/>"
            + "<case>"
            + "  <if condition=\"${value == 2}\">"
            + "    <def var=\"selectedBranch\" value=\"first\"/>"
            + "  </if>"
            + "  <if condition=\"${value == 2}\">"
            + "    <def var=\"selectedBranch\" value=\"second\"/>"
            + "  </if>"
            + "</case>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        Variable selectedBranch = context.getVar("selectedBranch");

        assertEquals(selectedBranch == null ? null : selectedBranch.toString(), "first",
            "Case must stop after executing the first matching branch");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void caseWithoutMatchingBranchOrDefaultMustNotDefineSelection() throws Exception {
        String xml = CONFIG_PREFIX
            + "<def var=\"value\" value=\"2\"/>"
            + "<case>"
            + "  <if condition=\"\u0024{value == 99}\">"
            + "    <def var=\"selectedBranch\" value=\"matched\"/>"
            + "  </if>"
            + "</case>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        assertNull(context.getVar("selectedBranch"),
            "A Case without a matching branch or default must not define a selection");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void caseMustExecuteDefaultElseOnlyWhenNoBranchMatches() throws Exception {
        String xml = CONFIG_PREFIX
            + "<def var=\"value\" value=\"2\"/>"
            + "<case>"
            + "  <if condition=\"${value == 99}\">"
            + "    <def var=\"selectedBranch\" value=\"matched\"/>"
            + "  </if>"
            + "  <else>"
            + "    <def var=\"selectedBranch\" value=\"default\"/>"
            + "  </else>"
            + "</case>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        Variable selectedBranch = context.getVar("selectedBranch");

        assertEquals(selectedBranch == null ? null : selectedBranch.toString(), "default",
            "Case default Else must execute when no condition matches");
    }
    @Test(groups = {"integration", "control-flow-plugins"})
    public void ifMustEvaluateTrueNumericComparisons() throws Exception {
        String xml = CONFIG_PREFIX
            + "<def var=\"count\" value=\"11\"/>"
            + "<if condition=\"\u0024{count > 10}\">"
            + "  <def var=\"selectedBranch\" value=\"true\"/>"
            + "</if>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);

        assertEquals(context.getVar("selectedBranch").toString(), "true",
            "A true numeric comparison must execute the If body");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void siblingElseMustNotExecuteWhenIfConditionIsTrue() throws Exception {
        String xml = CONFIG_PREFIX
            + "<if condition=\"\u0024{true}\">"
            + "  <def var=\"selectedBranch\" value=\"if\"/>"
            + "</if>"
            + "<else>"
            + "  <def var=\"selectedBranch\" value=\"else\"/>"
            + "</else>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        assertEquals(context.getVar("selectedBranch").toString(), "if");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void nestedElseMustExecuteWhenIfConditionIsFalse() throws Exception {
        String xml = CONFIG_PREFIX
            + "<if condition=\"\u0024{false}\">"
            + "  <def var=\"unexpected\" value=\"if\"/>"
            + "  <else><def var=\"selectedBranch\" value=\"else\"/></else>"
            + "</if>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        assertNull(context.getVar("unexpected"));
        assertEquals(context.getVar("selectedBranch").toString(), "else");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void nestedElseMustNotExecuteWhenIfConditionIsTrue() throws Exception {
        String xml = CONFIG_PREFIX
            + "<if condition=\"\u0024{true}\">"
            + "  <def var=\"selectedBranch\" value=\"if\"/>"
            + "  <else><def var=\"selectedBranch\" value=\"else\"/></else>"
            + "</if>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        assertEquals(context.getVar("selectedBranch").toString(), "if");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void caseMustContinueAfterNonMatchingBranchAndStopAtFirstMatch() throws Exception {
        String xml = CONFIG_PREFIX
            + "<def var=\"value\" value=\"2\"/>"
            + "<case>"
            + "  <if condition=\"\u0024{value == 1}\">"
            + "    <def var=\"selectedBranch\" value=\"first\"/>"
            + "  </if>"
            + "  <if condition=\"\u0024{value == 2}\">"
            + "    <def var=\"selectedBranch\" value=\"second\"/>"
            + "  </if>"
            + "  <else><def var=\"selectedBranch\" value=\"default\"/></else>"
            + "</case>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        assertEquals(context.getVar("selectedBranch").toString(), "second");
    }


    @Test(groups = {"integration", "control-flow-plugins"})
    public void caseBranchMustPreserveNestedIfElseSelection() throws Exception {
        String xml = CONFIG_PREFIX
            + "<def var=\"value\" value=\"2\"/>"
            + "<def var=\"enabled\" value=\"false\"/>"
            + "<case>"
            + "  <if condition=\"\u0024{value == 2}\">"
            + "    <if condition=\"\u0024{enabled}\">"
            + "      <def var=\"selectedBranch\" value=\"nested-if\"/>"
            + "    </if>"
            + "    <else>"
            + "      <def var=\"selectedBranch\" value=\"nested-else\"/>"
            + "    </else>"
            + "  </if>"
            + "  <else><def var=\"selectedBranch\" value=\"case-default\"/></else>"
            + "</case>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        Variable selectedBranch = context.getVar("selectedBranch");

        assertEquals(selectedBranch == null ? null : selectedBranch.toString(), "nested-else",
            "The first matching Case branch must execute its nested If/Else selection");
    }


    @Test(groups = {"integration", "control-flow-plugins"})
    public void loopMustExecuteIfElsePerIterationInOrder() throws Exception {
        String xml = CONFIG_PREFIX
            + "<def var=\"loopResult\">"
            + "  <loop items=\"a,,b\" item=\"entry\" index=\"idx\">"
            + "    <if condition=\"\u0024{entry}\">"
            + "      <def var=\"branchValue\" value=\"A\"/>"
            + "    </if>"
            + "    <else>"
            + "      <def var=\"branchValue\" value=\"B\"/>"
            + "    </else>"
            + "  </loop>"
            + "</def>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        Variable loopResult = context.getVar("loopResult");

        assertEquals(loopResult == null ? null : loopResult.toString(), "ABA",
            "Loop must execute the selected If/Else branch once per item in order");
        assertEquals(context.getVar("entry").toString(), "b",
            "The final loop item remains visible for compatibility");
        assertEquals(context.getVar("idx").toString(), "2",
            "The final loop index remains visible for compatibility");
    }


    @Test(groups = {"integration", "control-flow-plugins"})
    public void outerIfMustPreserveNestedSiblingElseAndItsOwnSiblingElse() throws Exception {
        String xml = CONFIG_PREFIX
            + "<def var=\"outerEnabled\" value=\"true\"/>"
            + "<def var=\"innerEnabled\" value=\"false\"/>"
            + "<if condition=\"\u0024{outerEnabled}\">"
            + "  <def var=\"outerBody\" value=\"retained\"/>"
            + "  <if condition=\"\u0024{innerEnabled}\">"
            + "    <def var=\"selectedBranch\" value=\"nested-if\"/>"
            + "  </if>"
            + "  <else><def var=\"selectedBranch\" value=\"nested-else\"/></else>"
            + "</if>"
            + "<else><def var=\"selectedBranch\" value=\"outer-else\"/></else>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);

        assertEquals(context.getVar("selectedBranch").toString(), "nested-else",
            "Nested sibling Else must remain paired with its own If");
        assertEquals(context.getVar("outerBody").toString(), "retained",
            "Ordinary outer If body plugins must execute alongside nested If/Else pairs");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void outerSiblingElseMustRunWhenOuterIfIsFalseDespiteNestedElse() throws Exception {
        String xml = CONFIG_PREFIX
            + "<def var=\"outerEnabled\" value=\"false\"/>"
            + "<def var=\"innerEnabled\" value=\"false\"/>"
            + "<if condition=\"\u0024{outerEnabled}\">"
            + "  <if condition=\"\u0024{innerEnabled}\">"
            + "    <def var=\"selectedBranch\" value=\"nested-if\"/>"
            + "  </if>"
            + "  <else><def var=\"selectedBranch\" value=\"nested-else\"/></else>"
            + "</if>"
            + "<else><def var=\"selectedBranch\" value=\"outer-else\"/></else>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);

        assertEquals(context.getVar("selectedBranch").toString(), "outer-else",
            "Outer sibling Else must remain available when the outer If is false");
    }


    @Test(groups = {"integration", "control-flow-plugins"})
    public void outerElseMustNotBeConsumedAsNestedElseWhenOuterIfIsTrue() throws Exception {
        String xml = CONFIG_PREFIX
            + "<def var=\"outerEnabled\" value=\"true\"/>"
            + "<def var=\"innerEnabled\" value=\"false\"/>"
            + "<if condition=\"\u0024{outerEnabled}\">"
            + "  <if condition=\"\u0024{innerEnabled}\">"
            + "    <def var=\"selectedBranch\" value=\"nested-if\"/>"
            + "  </if>"
            + "</if>"
            + "<else><def var=\"selectedBranch\" value=\"outer-else\"/></else>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        assertNull(context.getVar("selectedBranch"),
            "An outer sibling Else must not become the fallback of the final nested If");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void outerElseMustRemainAvailableWhenFinalNestedIfHasNoElse() throws Exception {
        String xml = CONFIG_PREFIX
            + "<def var=\"outerEnabled\" value=\"false\"/>"
            + "<def var=\"innerEnabled\" value=\"false\"/>"
            + "<if condition=\"\u0024{outerEnabled}\">"
            + "  <if condition=\"\u0024{innerEnabled}\">"
            + "    <def var=\"selectedBranch\" value=\"nested-if\"/>"
            + "  </if>"
            + "</if>"
            + "<else><def var=\"selectedBranch\" value=\"outer-else\"/></else>"
            + CONFIG_SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        assertEquals(context.getVar("selectedBranch").toString(), "outer-else",
            "The outer sibling Else must remain available when the outer If is false");
    }


}
