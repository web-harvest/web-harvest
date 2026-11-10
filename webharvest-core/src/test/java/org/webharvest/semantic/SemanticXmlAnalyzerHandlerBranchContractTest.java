package org.webharvest.semantic;

import org.testng.annotations.Test;
import org.webharvest.semantic.internal.SemanticXmlAnalyzer;
import static org.testng.Assert.*;

/**
 * Handler edge contracts reachable through the public {@link SemanticXmlAnalyzer#load} API.
 */
public class SemanticXmlAnalyzerHandlerBranchContractTest {

    @Test
    public void failsClosedWhenElementCountLimitIsExceeded() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(
                new SemanticXmlAnalyzer.Limits(1_000, 8, 2, 8, 1_000, 1_000_000));
        SemanticLoadResult result = analyzer.load("<r><a/><b/></r>", SourceOrigin.unknown());
        assertFalse(result.isSuccessful());
        assertTrue(result.getDocument().isEmpty());
        assertEquals(result.getDiagnostics().get(0).getCode(), "LIMIT_EXCEEDED");
    }

    @Test
    public void failsClosedWhenAttributeValueIsTooLong() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(
                new SemanticXmlAnalyzer.Limits(1_000, 8, 10, 8, 1_000, 3));
        SemanticLoadResult result = analyzer.load("<r long=\"abcd\"/>", SourceOrigin.unknown());
        assertFalse(result.isSuccessful());
        assertEquals(result.getDiagnostics().get(0).getCode(), "LIMIT_EXCEEDED");
    }

    @Test
    public void failsClosedOnMultipleRootElements() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults());
        SemanticLoadResult result = analyzer.load("<one/><two/>", SourceOrigin.unknown());
        assertFalse(result.isSuccessful());
        assertTrue(result.getDocument().isEmpty());
        assertTrue(result.getDiagnostics().stream().anyMatch(d -> "XML_PARSE_ERROR".equals(d.getCode())),
                diagnosticText(result));
    }

    @Test
    public void rejectsNonWhitespaceOutsideDocumentElement() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults());
        SemanticLoadResult result = analyzer.load("noise<r/>", SourceOrigin.unknown());
        assertFalse(result.isSuccessful());
        assertTrue(result.getDocument().isEmpty());
    }

    @Test
    public void mergesAdjacentCdataTextInSameMode() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults());
        SemanticLoadResult result = analyzer.load(
                "<r><![CDATA[part1]]><![CDATA[part2]]></r>", SourceOrigin.unknown());
        assertTrue(result.isSuccessful(), diagnosticText(result));
        SemanticElement root = result.getDocument().get().getRoot();
        assertEquals(root.getContent().size(), 1);
        SemanticText text = (SemanticText) root.getContent().get(0);
        assertTrue(text.isCdata());
        assertEquals(text.getText(), "part1part2");
    }

    private static String diagnosticText(SemanticLoadResult result) {
        return result.getDiagnostics().stream()
                .map(d -> d.getCode() + ": " + d.getMessage())
                .collect(java.util.stream.Collectors.joining("; "));
    }
}
