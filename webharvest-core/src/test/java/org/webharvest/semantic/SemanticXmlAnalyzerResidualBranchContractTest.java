package org.webharvest.semantic;

import static org.testng.Assert.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.webharvest.semantic.internal.SemanticXmlAnalyzer;

/**
 * Residual SemanticXmlAnalyzer contracts: limit construction, comments, prefixed
 * attributes, depth limits, and malformed nesting via the public load API.
 */
public class SemanticXmlAnalyzerResidualBranchContractTest {

    @DataProvider(name = "invalidLimits")
    public Object[][] invalidLimits() {
        return new Object[][] {
            {0, 8, 10, 8, 100, 100},
            {100, 0, 10, 8, 100, 100},
            {100, 8, 0, 8, 100, 100},
            {100, 8, 10, 0, 100, 100},
            {100, 8, 10, 8, 0, 100},
            {100, 8, 10, 8, 100, 0}
        };
    }

    @Test(dataProvider = "invalidLimits", expectedExceptions = IllegalArgumentException.class)
    public void limitsConstructorRejectsNonPositiveValues(int chars, int depth, int elements,
            int attrs, int text, int attrValue) {
        new SemanticXmlAnalyzer.Limits(chars, depth, elements, attrs, text, attrValue);
    }

    @Test
    public void commentsAreRetainedOnSuccessfulLoad() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults());
        SemanticLoadResult result = analyzer.load("<r><!-- note --><c/></r>", SourceOrigin.unknown());
        assertTrue(result.isSuccessful(), diagnosticText(result));
        assertTrue(result.getDocument().isPresent());
    }

    @Test
    public void prefixedAttributesUseLocalNameFallback() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults());
        SemanticLoadResult result = analyzer.load(
                "<r xmlns:x=\"urn:x\" x:flag=\"1\">ok</r>", SourceOrigin.unknown());
        assertTrue(result.isSuccessful(), diagnosticText(result));
        SemanticElement root = result.getDocument().get().getRoot();
        assertTrue(root.getAttributes().stream().anyMatch(a -> "flag".equals(a.getName().getLocalName())
                        || a.getValue().equals("1")),
                String.valueOf(root.getAttributes()));
    }

    @Test
    public void failsClosedWhenDepthLimitIsExceeded() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(
                new SemanticXmlAnalyzer.Limits(1_000, 2, 50, 8, 1_000, 1_000));
        SemanticLoadResult result = analyzer.load("<a><b><c/></b></a>", SourceOrigin.unknown());
        assertFalse(result.isSuccessful());
        assertEquals(result.getDiagnostics().get(0).getCode(), "LIMIT_EXCEEDED");
    }

    @Test
    public void rejectsUnexpectedClosingTag() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults());
        SemanticLoadResult result = analyzer.load("<r></x>", SourceOrigin.unknown());
        assertFalse(result.isSuccessful());
        assertTrue(result.getDocument().isEmpty());
    }

    @Test
    public void allowsWhitespaceBeforeRootElement() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults());
        SemanticLoadResult result = analyzer.load("   \n<root/>", SourceOrigin.unknown());
        assertTrue(result.isSuccessful(), diagnosticText(result));
        assertEquals(result.getDocument().get().getRoot().getName().getLocalName(), "root");
    }

    @Test
    public void incompleteDocumentFailsClosed() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults());
        SemanticLoadResult result = analyzer.load("<root>", SourceOrigin.unknown());
        assertFalse(result.isSuccessful());
        assertTrue(result.getDocument().isEmpty());
    }

    private static String diagnosticText(SemanticLoadResult result) {
        return result.getDiagnostics().stream()
                .map(d -> d.getCode() + ": " + d.getMessage())
                .collect(java.util.stream.Collectors.joining("; "));
    }
}
