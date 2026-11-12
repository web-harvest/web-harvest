package org.webharvest.semantic;

import static org.testng.Assert.*;

import java.util.List;
import org.testng.annotations.Test;
import org.webharvest.semantic.internal.SemanticXmlAnalyzer;

/** Focused SAX handler contracts for document-level misc content and limits. */
public class SemanticXmlAnalyzerContentBoundaryBranchContractTest {

    @Test
    public void preservesProcessingInstructionsAndCommentsAroundRootInSourceOrder() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults());
        SemanticLoadResult result = analyzer.load(
                "<?before alpha?><root/><!--after--><?after omega?>", SourceOrigin.unknown());

        assertTrue(result.isSuccessful(), diagnosticText(result));
        List<SemanticContent> content = result.getDocument().get().getContent();
        assertEquals(content.size(), 4);
        assertTrue(content.get(0) instanceof SemanticProcessingInstruction);
        assertEquals(((SemanticProcessingInstruction) content.get(0)).getTarget(), "before");
        assertTrue(content.get(1) instanceof SemanticElement);
        assertTrue(content.get(2) instanceof SemanticComment);
        assertEquals(((SemanticComment) content.get(2)).getText(), "after");
        assertTrue(content.get(3) instanceof SemanticProcessingInstruction);
        assertEquals(((SemanticProcessingInstruction) content.get(3)).getTarget(), "after");
    }

    @Test
    public void preservesProcessingInstructionsAndCommentsInsideRoot() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults());
        SemanticLoadResult result = analyzer.load(
                "<root><?step data?><!--note--><child/></root>", SourceOrigin.unknown());

        assertTrue(result.isSuccessful(), diagnosticText(result));
        List<SemanticContent> content = result.getDocument().get().getRoot().getContent();
        assertEquals(content.size(), 3);
        assertTrue(content.get(0) instanceof SemanticProcessingInstruction);
        assertTrue(content.get(1) instanceof SemanticComment);
        assertTrue(content.get(2) instanceof SemanticElement);
    }

    @Test
    public void failsClosedWhenTextCharacterLimitIsExceeded() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(
                new SemanticXmlAnalyzer.Limits(100, 8, 10, 8, 3, 100));
        SemanticLoadResult result = analyzer.load("<root>four</root>", SourceOrigin.unknown());

        assertFalse(result.isSuccessful());
        assertTrue(result.getDocument().isEmpty());
        assertEquals(result.getDiagnostics().get(0).getCode(), "LIMIT_EXCEEDED");
    }

    @Test
    public void failsClosedWhenAttributeCountLimitIsExceeded() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(
                new SemanticXmlAnalyzer.Limits(100, 8, 10, 1, 100, 100));
        SemanticLoadResult result = analyzer.load("<root a='1' b='2'/>", SourceOrigin.unknown());

        assertFalse(result.isSuccessful());
        assertTrue(result.getDocument().isEmpty());
        assertEquals(result.getDiagnostics().get(0).getCode(), "LIMIT_EXCEEDED");
    }

    @Test
    public void rejectsDocumentContainingOnlyWhitespaceWithoutRoot() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults());
        SemanticLoadResult result = analyzer.load("  \n\t ", SourceOrigin.unknown());

        assertFalse(result.isSuccessful());
        assertTrue(result.getDocument().isEmpty());
        assertTrue(result.getDiagnostics().stream()
                .anyMatch(d -> "XML_PARSE_ERROR".equals(d.getCode())), diagnosticText(result));
    }

    private static String diagnosticText(SemanticLoadResult result) {
        return result.getDiagnostics().stream()
                .map(d -> d.getCode() + ": " + d.getMessage())
                .collect(java.util.stream.Collectors.joining("; "));
    }
}
