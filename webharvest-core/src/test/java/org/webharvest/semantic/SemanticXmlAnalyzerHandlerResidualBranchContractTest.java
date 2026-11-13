package org.webharvest.semantic;

import static org.testng.Assert.*;

import java.util.List;
import org.testng.annotations.Test;
import org.webharvest.semantic.internal.SemanticXmlAnalyzer;

/**
 * Residual {@link SemanticXmlAnalyzer.Handler} contracts: nesting depth, prefixed
 * names, adjacent text merge, and whitespace outside the document element.
 */
public class SemanticXmlAnalyzerHandlerResidualBranchContractTest {

    @Test
    public void failsClosedWhenNestingDepthLimitIsExceeded() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(
                new SemanticXmlAnalyzer.Limits(1_000, 2, 100, 8, 1_000, 1_000));
        SemanticLoadResult result = analyzer.load("<r><a><b/></a></r>", SourceOrigin.unknown());

        assertFalse(result.isSuccessful());
        assertTrue(result.getDocument().isEmpty());
        assertEquals(result.getDiagnostics().get(0).getCode(), "LIMIT_EXCEEDED");
    }

    @Test
    public void preservesPrefixedElementAndAttributeNames() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults());
        SemanticLoadResult result = analyzer.load(
                "<ns:root xmlns:ns=\"http://example.com/ns\" ns:flag=\"yes\"><ns:child/></ns:root>",
                SourceOrigin.unknown());

        assertTrue(result.isSuccessful(), diagnosticText(result));
        SemanticElement root = result.getDocument().get().getRoot();
        assertEquals(root.getName().getPrefix(), "ns");
        assertEquals(root.getName().getLocalName(), "root");
        assertEquals(root.getName().getNamespaceUri(), "http://example.com/ns");
        assertTrue(root.getAttributes().stream().anyMatch(a ->
                "flag".equals(a.getName().getLocalName())
                        && "ns".equals(a.getName().getPrefix())
                        && "yes".equals(a.getValue())),
                "Prefixed attribute ns:flag must be retained");
        assertEquals(root.getContent().size(), 1);
        SemanticElement child = (SemanticElement) root.getContent().get(0);
        assertEquals(child.getName().getLocalName(), "child");
        assertEquals(child.getName().getPrefix(), "ns");
    }

    @Test
    public void mergesAdjacentPlainTextSegmentsInSameMode() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults());
        SemanticLoadResult result = analyzer.load(
                "<r>hello<!--x-->world</r>", SourceOrigin.unknown());

        assertTrue(result.isSuccessful(), diagnosticText(result));
        List<SemanticContent> content = result.getDocument().get().getRoot().getContent();
        assertEquals(content.size(), 3);
        assertTrue(content.get(0) instanceof SemanticText);
        assertFalse(((SemanticText) content.get(0)).isCdata());
        assertEquals(((SemanticText) content.get(0)).getText(), "hello");
        assertTrue(content.get(1) instanceof SemanticComment);
        assertTrue(content.get(2) instanceof SemanticText);
        assertEquals(((SemanticText) content.get(2)).getText(), "world");
    }

    @Test
    public void acceptsExactMaxDepthWithoutExceedingLimit() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(
                new SemanticXmlAnalyzer.Limits(1_000, 2, 100, 8, 1_000, 1_000));
        SemanticLoadResult result = analyzer.load("<r><a/></r>", SourceOrigin.unknown());

        assertTrue(result.isSuccessful(), diagnosticText(result));
        assertEquals(result.getDocument().get().getRoot().getContent().size(), 1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsNonPositiveLimits() {
        new SemanticXmlAnalyzer.Limits(0, 1, 1, 1, 1, 1);
    }

    private static String diagnosticText(SemanticLoadResult result) {
        return result.getDiagnostics().stream()
                .map(d -> d.getCode() + ": " + d.getMessage())
                .collect(java.util.stream.Collectors.joining("; "));
    }
}
