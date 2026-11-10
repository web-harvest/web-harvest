package org.webharvest.semantic;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

/**
 * Identity contracts for {@link SourceOrigin}.
 */
public class SourceOriginBranchContractTest {

    @Test
    public void unknownFactoryAndEquality() {
        SourceOrigin unknown = SourceOrigin.unknown();
        assertEquals(unknown.getValue(), "unknown");
        assertEquals(unknown.toString(), "unknown");

        SourceOrigin a = new SourceOrigin("file:config.xml");
        SourceOrigin same = new SourceOrigin("file:config.xml");
        SourceOrigin other = new SourceOrigin("classpath:other.xml");

        assertEquals(a, same);
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(other));
        assertFalse(a.equals(null));
        assertFalse(a.equals("file:config.xml"));
        assertEquals(a, a);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void rejectsNullValue() {
        new SourceOrigin(null);
    }
}
