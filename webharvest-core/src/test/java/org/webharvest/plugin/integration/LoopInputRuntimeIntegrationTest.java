package org.webharvest.plugin.integration;

import org.testng.annotations.Test;
import org.webharvest.plugin.core.LoopPlugin;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Runtime contract tests for Loop input-source normalization.
 *
 * These tests execute real XML through the parser, plugin registry and runtime;
 * they intentionally assert observable output rather than only successful startup.
 */
public class LoopInputRuntimeIntegrationTest extends XmlBasedIntegrationTestBase {

    private static final String FIXTURE = "integration-tests/loop_input_sources.xml";

    @Test(groups = {"integration", "loop-input"})
    public void testItemsAttributeResolvesListVariableAndPreservesItsElements() throws Exception {
        Variable output = executeAndGetVariable(FIXTURE, "variableSourceOutput");

        assertEquals(output.toString(), "0:alpha|1:beta|2:gamma|");
    }

    @Test(groups = {"integration", "loop-input"})
    public void testItemsAttributeTreatsScalarVariableAsSingleItem() throws Exception {
        Variable output = executeAndGetVariable(FIXTURE, "scalarVariableOutput");

        assertEquals(output.toString(), "0:solo|");
    }

    @Test(groups = {"integration", "loop-input"})
    public void testLeadingListChildIsExecutedOnceAndBodyWrapperIsUnwrapped() throws Exception {
        Variable output = executeAndGetVariable(FIXTURE, "childListSourceOutput");

        assertEquals(output.toString(), "0:red|1:blue|");
    }

    @Test(groups = {"integration", "loop-input"})
    public void testRemainingDirectChildrenExecuteAsBodyWhenBodyWrapperIsAbsent() throws Exception {
        Variable output = executeAndGetVariable(FIXTURE, "directChildBodyOutput");

        assertEquals(output.toString(), "0:north|1:south|");
    }

    @Test(groups = {"integration", "loop-input"})
    public void testInlineTextContentIsUsedAsNewlineDelimitedSource() throws Exception {
        Variable output = executeAndGetVariable(FIXTURE, "inlineTextSourceOutput");

        assertEquals(output.toString(), "0:first|1:second|2:third|");
    }

    @Test(groups = {"integration", "loop-input"})
    public void testDelimiterIsLiteralAndSupportsRegexMetacharacters() throws Exception {
        Variable output = executeAndGetVariable(FIXTURE, "literalDelimiterOutput");

        assertEquals(output.toString(), "0:a|1:b|2:c|");
    }

    @Test(groups = {"integration", "loop-input"})
    public void testEmptyFalsePreservesInteriorAndTrailingEmptyItems() throws Exception {
        Variable output = executeAndGetVariable(FIXTURE, "emptyPreservedOutput");

        assertEquals(output.toString(), "0[a]|1[]|2[b]|3[]|");
    }

    @Test(groups = {"integration", "loop-input"})
    public void testEmptyTrueSkipsEmptyItemsWithoutRenumberingSourceIndexes() throws Exception {
        Variable output = executeAndGetVariable(FIXTURE, "emptySkippedOutput");

        assertEquals(output.toString(), "0[a]|2[b]|");
    }

    @Test(groups = {"integration", "loop-input"})
    public void testDelimitedScalarVariableIsSplitUsingConfiguredDelimiter() throws Exception {
        Variable output = executeAndGetVariable(FIXTURE, "delimitedScalarVariableOutput");

        assertEquals(output.toString(), "0:one|1:two|");
    }

    @Test(groups = {"integration", "loop-input"})
    public void testInlineTextVariableIsResolvedBeforeNewlineSplitting() throws Exception {
        Variable output = executeAndGetVariable(FIXTURE, "inlineVariableTextOutput");

        assertEquals(output.toString(), "0:north|1:south|");
    }

    @Test(groups = {"integration", "loop-input"})
    public void testLeadingXPathChildIsEvaluatedOnceAsSource() throws Exception {
        Variable output = executeAndGetVariable(FIXTURE, "xpathSourceOutput");

        assertEquals(output.toString(), "0:left|1:right|");
    }

    @Test(groups = {"integration", "loop-input"})
    public void testSourceOnlyLoopReturnsItsItems() throws Exception {
        Variable output = executeAndGetVariable(FIXTURE, "sourceOnlyOutput");

        assertEquals(output.toString(), "catdog");
    }

    @Test(groups = {"integration", "loop-input"})
    public void testItemsIsDeclaredAsAnOptionalConfigurationAttribute() {
        LoopPlugin plugin = new LoopPlugin();

        assertTrue(java.util.Arrays.asList(plugin.getConfigurationSchema().getOptionalAttributes())
            .contains("items"));
    }

    @Test(groups = {"integration", "loop-input"})
    public void testMaxloopsCountsExecutedIterationsAfterEmptyItemsAreSkipped() throws Exception {
        Variable output = executeAndGetVariable(FIXTURE, "emptySkippedWithinMaxloopsOutput");

        assertEquals(output.toString(), "1:alpha|2:beta|");
    }


    @Test(groups = {"integration", "loop-input"})
    public void testEmptyTrueTreatsWhitespaceOnlyLiteralTokensAsEmpty() throws Exception {
        Variable output = executeAndGetVariable(FIXTURE, "emptyWhitespaceOutput");

        assertEquals(output.toString(), "0:a|2:b|");
    }


    @Test(groups = {"integration", "loop-input"})
    public void testMaxloopsCapsDelimitedScalarVariableItems() throws Exception {
        Variable output = executeAndGetVariable(FIXTURE, "delimitedScalarVariableMaxloopsOutput");

        assertEquals(output.toString(), "0:one|");
    }


}
