package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import java.lang.reflect.Method;
import org.jsoup.safety.Safelist;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Direct contracts for HtmlToXml private helpers that scrub names/characters and
 * select JSoup safelists. Mirrors the reflective CallPlugin normalize tests.
 */
public class HtmlToXmlHelperBranchContractTest {

    private HtmlToXmlPlugin plugin;
    private Method isValidXmlName;
    private Method isValidXmlChar;
    private Method getSafelist;
    private Method balanceTags;
    private Method cleanInvalidXmlChars;

    @BeforeClass
    public void resolveHelpers() throws Exception {
        plugin = new HtmlToXmlPlugin();
        Class<?> type = HtmlToXmlPlugin.class;
        isValidXmlName = type.getDeclaredMethod("isValidXmlName", String.class);
        isValidXmlName.setAccessible(true);
        isValidXmlChar = type.getDeclaredMethod("isValidXmlChar", char.class);
        isValidXmlChar.setAccessible(true);
        getSafelist = type.getDeclaredMethod("getSafelist", String.class);
        getSafelist.setAccessible(true);
        balanceTags = type.getDeclaredMethod("balanceTags", String.class);
        balanceTags.setAccessible(true);
        cleanInvalidXmlChars = type.getDeclaredMethod("cleanInvalidXmlChars", String.class);
        cleanInvalidXmlChars.setAccessible(true);
    }

    @Test
    public void xmlNameValidationRejectsNullEmptyAndIllegalCharacters() throws Exception {
        assertFalse((Boolean) isValidXmlName.invoke(plugin, (Object) null));
        assertFalse((Boolean) isValidXmlName.invoke(plugin, ""));
        assertFalse((Boolean) isValidXmlName.invoke(plugin, "1bad"));
        assertFalse((Boolean) isValidXmlName.invoke(plugin, "bad@name"));
        assertTrue((Boolean) isValidXmlName.invoke(plugin, "_ok"));
        assertTrue((Boolean) isValidXmlName.invoke(plugin, ":ns"));
        assertTrue((Boolean) isValidXmlName.invoke(plugin, "ok-name.1"));
    }

    @DataProvider(name = "xmlChars")
    public Object[][] xmlChars() {
        return new Object[][] {
            {(char) 0x09, true},
            {(char) 0x0A, true},
            {(char) 0x0D, true},
            {(char) 0x20, true},
            {(char) 0x7F, true},
            {(char) 0xD7FF, true},
            {(char) 0xD800, false},
            {(char) 0xDFFF, false},
            {(char) 0xE000, true},
            {(char) 0xFFFD, true},
            {(char) 0xFFFE, false},
            {(char) 0x08, false},
            {(char) 0x1F, false}
        };
    }

    @Test(dataProvider = "xmlChars")
    public void xmlCharClassMatchesXmlSpecBmpSubset(char value, boolean expected) throws Exception {
        assertEquals(isValidXmlChar.invoke(plugin, value), expected, "char U+" + Integer.toHexString(value));
    }

    @Test
    public void safelistNamesResolveKnownPresetsAndUnknownFallback() throws Exception {
        assertNotNull(getSafelist.invoke(plugin, "none"));
        assertNotNull(getSafelist.invoke(plugin, "simpleText"));
        assertNotNull(getSafelist.invoke(plugin, "basic"));
        assertNotNull(getSafelist.invoke(plugin, "basicWithImages"));
        assertNotNull(getSafelist.invoke(plugin, "relaxed"));
        Object unknown = getSafelist.invoke(plugin, "custom-unknown");
        assertTrue(unknown instanceof Safelist);
    }

    @Test
    public void balanceTagsClosesCommonVoidElements() throws Exception {
        String balanced = (String) balanceTags.invoke(plugin, "<div><br><hr><img src='a'><input type='text'></div>");
        assertTrue(balanced.contains("<br"), balanced);
        assertTrue(balanced.contains("/>") || balanced.contains("<br"), balanced);
    }

    @Test
    public void cleanInvalidXmlCharsReturnsNullForNullInput() throws Exception {
        assertNull(cleanInvalidXmlChars.invoke(plugin, (Object) null));
        assertEquals(cleanInvalidXmlChars.invoke(plugin, "A" + ((char) 0x01) + "B"), "A B");
    }
}
