package org.webharvest.runtime.scripting;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

/**
 * Contracts for {@link ScriptSource} construction guards and value identity.
 */
public class ScriptSourceBranchContractTest {

    @Test
    public void rejectsNullSourceOrLanguage() {
        try {
            new ScriptSource(null, ScriptingLanguage.BEANSHELL);
            fail("null source");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("source"));
        }
        try {
            new ScriptSource("1+1", null);
            fail("null language");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("language"));
        }
    }

    @Test
    public void equalityUsesSourceAndLanguage() {
        ScriptSource a = new ScriptSource("x", ScriptingLanguage.BEANSHELL);
        ScriptSource same = new ScriptSource("x", ScriptingLanguage.BEANSHELL);
        ScriptSource differentCode = new ScriptSource("y", ScriptingLanguage.BEANSHELL);
        ScriptSource differentLang = new ScriptSource("x", ScriptingLanguage.JAVASCRIPT);

        assertEquals(a.getSourceCode(), "x");
        assertEquals(a.getLanguage(), ScriptingLanguage.BEANSHELL);
        assertEquals(a, a);
        assertEquals(a, same);
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(differentCode));
        assertFalse(a.equals(differentLang));
        assertFalse(a.equals(null));
        assertFalse(a.equals("x"));
    }
}
