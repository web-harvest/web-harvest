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
}
