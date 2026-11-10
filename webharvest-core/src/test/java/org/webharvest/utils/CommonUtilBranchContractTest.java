package org.webharvest.utils;

import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import org.testng.annotations.Test;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Additional public CommonUtil contracts for URL composition, XML escaping,
 * indentation, and Variable factory behavior used across Core plugins.
 */
public class CommonUtilBranchContractTest {

    @Test
    public void indentReturnsEmptyForNonPositiveAndScalesWithNestedLevel() {
        assertEquals(CommonUtil.indent(0), "");
        assertEquals(CommonUtil.indent(-3), "");
        assertEquals(CommonUtil.indent(1), "");
        assertEquals(CommonUtil.indent(2), "    ");
        assertEquals(CommonUtil.indent(3), "        ");
    }

    @Test
    public void escapeXmlHandlesNullEntitiesAndSpecialCharacters() {
        assertNull(CommonUtil.escapeXml(null));
        assertEquals(CommonUtil.escapeXml("a & b < c > d \"e\" 'f'"),
                "a &amp; b &lt; c &gt; d &quot;e&quot; &apos;f&apos;");
        assertEquals(CommonUtil.escapeXml("keep &amp; &lt; &gt; &apos; &quot; intact"),
                "keep &amp; &lt; &gt; &apos; &quot; intact");
        assertEquals(CommonUtil.escapeXml("plain"), "plain");
    }

    @Test
    public void fullUrlResolvesAbsoluteRelativeQueryAndParentSegments() {
        assertEquals(CommonUtil.fullUrl("http://example.com/a/b.html", "http://other.test/x"),
                "http://other.test/x");
        assertEquals(CommonUtil.fullUrl("http://example.com/path/page.html", "?q=1"),
                "http://example.com/path/page.html?q=1");
        assertEquals(CommonUtil.fullUrl("http://example.com/path/page.html?", "?q=1"),
                "http://example.com/path/page.html?q=1");
        assertEquals(CommonUtil.fullUrl("http://example.com/path/page.html?x=1", "?y=2"),
                "http://example.com/path/page.html?x=1&y=2");
        assertEquals(CommonUtil.fullUrl("http://example.com/dir/page.html", "/root.txt"),
                "http://example.com/root.txt");
        assertEquals(CommonUtil.fullUrl("http://example.com/dir/page.html", "child.txt"),
                "http://example.com/dir/child.txt");
        assertEquals(CommonUtil.fullUrl("example.com/dir/page.html", "../sibling.txt"),
                "http://example.com/sibling.txt");
        assertFalse(CommonUtil.isFullUrl(null));
        assertFalse(CommonUtil.isFullUrl("not a url"));
        assertTrue(CommonUtil.isFullUrl("https://example.com/a"));
    }

    @Test
    public void createVariablePreservesEmptyCollectionsArraysAndExistingVariables() {
        assertSame(CommonUtil.createVariable(null), EmptyVariable.INSTANCE);
        assertSame(CommonUtil.createVariable(""), EmptyVariable.INSTANCE);
        assertSame(CommonUtil.createVariable(EmptyVariable.INSTANCE), EmptyVariable.INSTANCE);

        Variable existing = new NodeVariable("kept");
        assertSame(CommonUtil.createVariable(existing), existing);

        Variable emptyNode = new NodeVariable("");
        assertSame(CommonUtil.createVariable(emptyNode), EmptyVariable.INSTANCE);

        Variable fromCollection = CommonUtil.createVariable(Arrays.asList("a", "b"));
        assertTrue(fromCollection instanceof ListVariable);
        assertEquals(fromCollection.toString().contains("a"), true);

        Variable fromArray = CommonUtil.createVariable(new Object[] {"x", "y"});
        assertTrue(fromArray instanceof ListVariable);

        Variable fromEmptyCollection = CommonUtil.createVariable(Collections.emptyList());
        assertTrue(fromEmptyCollection instanceof ListVariable);

        assertEquals(CommonUtil.createVariable(42).toString(), "42");
    }
}
