/*
 * Copyright (c) 2006-2025 the original author or authors.
 */
package org.webharvest.plugin.integration;

import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Focused runtime regression tests for If expression evaluation.
 */
public class IfExpressionRuntimeIntegrationTest extends XmlBasedIntegrationTestBase {

    private static final String PREFIX =
        "<config xmlns=\"http://org.webharvest/schema/2.1/core\">";
    private static final String SUFFIX = "</config>";

    @Test(groups = {"integration", "control-flow-plugins"})
    public void falseBooleanLiteralMustNotExecuteIfBody() throws Exception {
        String xml = PREFIX
            + "<if condition=\"\u0024{false}\">"
            + "  <def var=\"unexpected\" value=\"executed\"/>"
            + "</if>" + SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        assertNull(context.getVar("unexpected"));
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void falseNumericComparisonMustNotExecuteIfBody() throws Exception {
        String xml = PREFIX
            + "<def var=\"count\" value=\"1\"/>"
            + "<if condition=\"\u0024{count > 10}\">"
            + "  <def var=\"unexpected\" value=\"executed\"/>"
            + "</if>" + SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        assertNull(context.getVar("unexpected"));
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void trueNumericComparisonMustExecuteIfBody() throws Exception {
        String xml = PREFIX
            + "<def var=\"count\" value=\"11\"/>"
            + "<if condition=\"\u0024{count > 10}\">"
            + "  <def var=\"selectedBranch\" value=\"true\"/>"
            + "</if>" + SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        Variable selectedBranch = context.getVar("selectedBranch");
        assertEquals(selectedBranch == null ? null : selectedBranch.toString(), "true");
    }
    @Test(groups = {"integration", "control-flow-plugins"})
    public void nonEmptyStringVariableMustRemainTruthy() throws Exception {
        String xml = PREFIX
            + "<def var=\"flag\" value=\"enabled\"/>"
            + "<if condition=\"\u0024{flag}\">"
            + "  <def var=\"selectedBranch\" value=\"executed\"/>"
            + "</if>" + SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        Variable selectedBranch = context.getVar("selectedBranch");
        assertEquals(selectedBranch == null ? null : selectedBranch.toString(), "executed",
            "A non-empty ordinary string must remain truthy for If conditions");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void trueBooleanLiteralMustExecuteIfBody() throws Exception {
        String xml = PREFIX
            + "<if condition=\"\u0024{true}\">"
            + "  <def var=\"selectedBranch\" value=\"true\"/>"
            + "</if>" + SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        Variable selectedBranch = context.getVar("selectedBranch");
        assertEquals(selectedBranch == null ? null : selectedBranch.toString(), "true");
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void zeroNumericLiteralMustNotExecuteIfBody() throws Exception {
        String xml = PREFIX
            + "<if condition=\"\u0024{0}\">"
            + "  <def var=\"unexpected\" value=\"executed\"/>"
            + "</if>" + SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        assertNull(context.getVar("unexpected"));
    }

    @Test(groups = {"integration", "control-flow-plugins"})
    public void nonZeroNumericLiteralMustExecuteIfBody() throws Exception {
        String xml = PREFIX
            + "<if condition=\"\u0024{1}\">"
            + "  <def var=\"selectedBranch\" value=\"executed\"/>"
            + "</if>" + SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        Variable selectedBranch = context.getVar("selectedBranch");
        assertEquals(selectedBranch == null ? null : selectedBranch.toString(), "executed");
    }


    @Test(groups = {"integration", "control-flow-plugins"})
    public void variableTruthinessMustDistinguishFalseZeroEmptyAndTruthyValues() throws Exception {
        String xml = PREFIX
            + "<def var=\"falseFlag\" value=\"false\"/>"
            + "<def var=\"zeroFlag\" value=\"0\"/>"
            + "<def var=\"emptyFlag\" value=\"\"/>"
            + "<def var=\"oneFlag\" value=\"1\"/>"
            + "<def var=\"ordinaryFlag\" value=\"enabled\"/>"
            + "<if condition=\"\u0024{falseFlag}\"><def var=\"unexpectedFalse\" value=\"yes\"/></if>"
            + "<if condition=\"\u0024{zeroFlag}\"><def var=\"unexpectedZero\" value=\"yes\"/></if>"
            + "<if condition=\"\u0024{emptyFlag}\"><def var=\"unexpectedEmpty\" value=\"yes\"/></if>"
            + "<if condition=\"\u0024{oneFlag}\"><def var=\"oneSelected\" value=\"yes\"/></if>"
            + "<if condition=\"\u0024{ordinaryFlag}\"><def var=\"ordinarySelected\" value=\"yes\"/></if>"
            + SUFFIX;

        DynamicScopeContext context = executeXmlString(xml);
        assertNull(context.getVar("unexpectedFalse"));
        assertNull(context.getVar("unexpectedZero"));
        assertNull(context.getVar("unexpectedEmpty"));
        assertEquals(context.getVar("oneSelected").toString(), "yes");
        assertEquals(context.getVar("ordinarySelected").toString(), "yes");
    }


}
