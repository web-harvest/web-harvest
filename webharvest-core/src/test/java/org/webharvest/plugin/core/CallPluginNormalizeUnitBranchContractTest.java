package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Direct normalization contracts for {@link CallPlugin} return shaping (ListVariable, brackets, empties).
 */
public class CallPluginNormalizeUnitBranchContractTest {

    private Method normalizeReturnValue;

    @BeforeMethod
    public void setUp() throws Exception {
        normalizeReturnValue = CallPlugin.class.getDeclaredMethod("normalizeReturnValue", Variable.class);
        normalizeReturnValue.setAccessible(true);
    }

    private Variable normalize(Variable value) throws Exception {
        return (Variable) normalizeReturnValue.invoke(new CallPlugin(), value);
    }

    @Test
    public void listVariableReturnsLastNonEmptyItem() throws Exception {
        ListVariable list = new ListVariable(Arrays.asList(
                EmptyVariable.INSTANCE,
                new NodeVariable("first"),
                new NodeVariable("last")));
        assertEquals(normalize(list).toString(), "last");
    }

    @Test
    public void listVariableWithOnlyEmptyItemsBecomesEmpty() throws Exception {
        ListVariable list = new ListVariable(Arrays.asList(EmptyVariable.INSTANCE, EmptyVariable.INSTANCE));
        assertSame(normalize(list), EmptyVariable.INSTANCE);
    }

    @Test
    public void nodeWrappingSingleListUnwrapsScalar() throws Exception {
        NodeVariable wrapped = new NodeVariable(Collections.singletonList("only"));
        assertEquals(normalize(wrapped).toString(), "only");
    }

    @Test
    public void nodeWrappingMultipleListValuesJoinsWithSpaces() throws Exception {
        NodeVariable wrapped = new NodeVariable(Arrays.asList("[Hello]", "[World]"));
        assertEquals(normalize(wrapped).toString(), "Hello World");
    }

    @Test
    public void bracketedTemplateStringIsCleaned() throws Exception {
        NodeVariable wrapped = new NodeVariable("Hello [Alice] [Bob]");
        assertEquals(normalize(wrapped).toString(), "Hello Alice Bob");
    }

    @Test
    public void nullAndEmptyInputsPassThroughOrEmpty() throws Exception {
        assertNull(normalize(null));
        assertSame(normalize(EmptyVariable.INSTANCE), EmptyVariable.INSTANCE);
    }

    @Test
    public void listVariableSkipsNullItemsWhenSelectingLastValue() throws Exception {
        ListVariable list = new ListVariable(Arrays.asList(
                null,
                new NodeVariable("keep"),
                null));
        assertEquals(normalize(list).toString(), "keep");
    }

    @Test
    public void emptyWrappedListBecomesEmptyVariable() throws Exception {
        assertSame(normalize(new NodeVariable(Collections.emptyList())), EmptyVariable.INSTANCE);
    }

    @Test
    public void nestedSingleListUnwrapsRecursively() throws Exception {
        NodeVariable nested = new NodeVariable(Collections.singletonList(Collections.singletonList("deep")));
        assertEquals(normalize(nested).toString(), "deep");
    }

    @Test
    public void multiItemListTreatsNullEntriesAsEmptyFragments() throws Exception {
        NodeVariable wrapped = new NodeVariable(Arrays.asList(null, "alpha", null));
        assertEquals(normalize(wrapped).toString().trim(), "alpha");
    }

    @Test
    public void plainNodeWithoutBracketsPassesThroughUnchanged() throws Exception {
        NodeVariable plain = new NodeVariable("plain-text");
        assertSame(normalize(plain), plain);
    }

    @Test
    public void capitalizeHandlesNullEmptyAndSingleCharacterKeys() throws Exception {
        Method capitalize = CallPlugin.class.getDeclaredMethod("capitalize", String.class);
        capitalize.setAccessible(true);
        CallPlugin plugin = new CallPlugin();
        assertNull(capitalize.invoke(plugin, (Object) null));
        assertEquals(capitalize.invoke(plugin, ""), "");
        assertEquals(capitalize.invoke(plugin, "a"), "A");
        assertEquals(capitalize.invoke(plugin, "name"), "Name");
    }
}
