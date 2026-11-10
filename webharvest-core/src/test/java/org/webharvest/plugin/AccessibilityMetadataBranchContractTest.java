package org.webharvest.plugin;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

/**
 * Contracts for {@link AccessibilityMetadata} null-safe navigation cloning and identity.
 */
public class AccessibilityMetadataBranchContractTest {

    @Test
    public void nullKeyboardNavigationBecomesEmptyCloneableArray() {
        AccessibilityMetadata meta = new AccessibilityMetadata("label", "desc", null);
        assertEquals(meta.getAriaLabel(), "label");
        assertEquals(meta.getAriaDescription(), "desc");
        assertEquals(meta.getKeyboardNavigation().length, 0);

        String[] nav = new String[] {"Tab", "Enter"};
        AccessibilityMetadata withNav = new AccessibilityMetadata("label", "desc", nav);
        assertEquals(withNav.getKeyboardNavigation(), nav);
        nav[0] = "mutated";
        assertEquals(withNav.getKeyboardNavigation()[0], "Tab");
    }

    @Test
    public void equalityDependsOnAriaLabel() {
        AccessibilityMetadata a = new AccessibilityMetadata("open", "d1", new String[] {"Enter"});
        AccessibilityMetadata same = new AccessibilityMetadata("open", "d2", null);
        AccessibilityMetadata other = new AccessibilityMetadata("close", "d1", null);
        assertEquals(a, same);
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(other));
        assertFalse(a.equals(null));
        assertTrue(a.toString().contains("open"));
    }
}
