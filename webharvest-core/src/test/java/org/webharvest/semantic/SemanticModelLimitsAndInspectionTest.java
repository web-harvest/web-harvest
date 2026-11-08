package org.webharvest.semantic;

import java.util.List;
import org.testng.annotations.Test;
import org.webharvest.semantic.internal.SemanticXmlAnalyzer;
import static org.testng.Assert.*;

/**
 * F6 fail-closed limit / error / immutability coverage and F7
 * {@link InspectionView#getTextNodeCount()} counting contracts.
 */
public class SemanticModelLimitsAndInspectionTest {

    // -------------------------------------------------------------------------
    // F6 — numeric limits (below / at / above when practical)
    // -------------------------------------------------------------------------

    @Test
    public void elementCountLimit_belowAtAndAboveBoundary() {
        // maxElements=2: root + one child is the boundary.
        SemanticModel limited = limitedModel(1_000, 8, 2, 8, 1_000, 1_000);

        SemanticLoadResult below = limited.load("<r/>", SourceOrigin.unknown());
        assertSuccessfulWithSource(below, "<r/>");
        assertEquals(limited.inspect(below.getDocument().get()).getElementCount(), 1);

        String atXml = "<r><a/></r>";
        SemanticLoadResult at = limited.load(atXml, SourceOrigin.unknown());
        assertSuccessfulWithSource(at, atXml);
        assertEquals(limited.inspect(at.getDocument().get()).getElementCount(), 2);

        String aboveXml = "<r><a/><b/></r>";
        SemanticLoadResult above = limited.load(aboveXml, SourceOrigin.unknown());
        assertFailClosed(above, "LIMIT_EXCEEDED", aboveXml);
    }

    @Test
    public void attributesPerElementLimit_belowAtAndAboveBoundary() {
        SemanticModel limited = limitedModel(1_000, 8, 10, 2, 1_000, 1_000);

        String belowXml = "<r a=\"1\"/>";
        SemanticLoadResult below = limited.load(belowXml, SourceOrigin.unknown());
        assertSuccessfulWithSource(below, belowXml);
        assertEquals(below.getDocument().get().getRoot().getAttributes().size(), 1);

        String atXml = "<r a=\"1\" b=\"2\"/>";
        SemanticLoadResult at = limited.load(atXml, SourceOrigin.unknown());
        assertSuccessfulWithSource(at, atXml);
        assertEquals(at.getDocument().get().getRoot().getAttributes().size(), 2);

        String aboveXml = "<r a=\"1\" b=\"2\" c=\"3\"/>";
        SemanticLoadResult above = limited.load(aboveXml, SourceOrigin.unknown());
        assertFailClosed(above, "LIMIT_EXCEEDED", aboveXml);
    }

    @Test
    public void inputSizeLimit_belowAtAndAboveBoundary() {
        String belowXml = "<r/>"; // 4 chars
        String atXml = "<ab/>"; // 5 chars
        String aboveXml = "<abc/>"; // 6 chars
        SemanticModel limited = limitedModel(5, 8, 10, 8, 1_000, 1_000);

        SemanticLoadResult below = limited.load(belowXml, SourceOrigin.unknown());
        assertSuccessfulWithSource(below, belowXml);

        SemanticLoadResult at = limited.load(atXml, SourceOrigin.unknown());
        assertSuccessfulWithSource(at, atXml);

        SemanticLoadResult above = limited.load(aboveXml, SourceOrigin.unknown());
        assertFailClosed(above, "LIMIT_EXCEEDED", aboveXml);
    }

    @Test
    public void attributeValueSizeLimit_atBoundarySucceeds_aboveFailsClosed() {
        // F2: UTF-16 length via String#length(); small custom limit avoids huge allocations.
        SemanticModel limited = limitedModel(1_000, 8, 10, 8, 1_000, 10);

        String atValue = "0123456789"; // length 10
        String atXml = "<r a=\"" + atValue + "\"/>";
        SemanticLoadResult at = limited.load(atXml, SourceOrigin.unknown());
        assertSuccessfulWithSource(at, atXml);
        assertEquals(at.getDocument().get().getRoot().getAttributes().get(0).getValue(), atValue);

        String aboveValue = "0123456789X"; // length 11
        String aboveXml = "<r a=\"" + aboveValue + "\"/>";
        SemanticLoadResult above = limited.load(aboveXml, SourceOrigin.unknown());
        assertFailClosed(above, "LIMIT_EXCEEDED", aboveXml);
    }

    // -------------------------------------------------------------------------
    // F6 — input / parse failures
    // -------------------------------------------------------------------------

    @Test
    public void nullInput_reportsInvalidInputAndNoDocument() {
        SemanticModel model = new SemanticModel();
        SemanticLoadResult result = model.load(null, SourceOrigin.unknown());
        assertFalse(result.isSuccessful());
        assertTrue(result.getDocument().isEmpty());
        assertEquals(result.getDiagnostics().get(0).getCode(), "INVALID_INPUT");
        // Null input is rejected before source retention; source may be absent.
        assertTrue(result.getSourceXml().isEmpty());
    }

    @Test
    public void malformedXml_reportsParseErrorAndNoDocument() {
        String xml = "<root><unclosed>";
        SemanticLoadResult result = new SemanticModel().load(xml, SourceOrigin.unknown());
        assertFailClosed(result, "XML_PARSE_ERROR", xml);
    }

    @Test
    public void limitExceeded_neverReturnsPartialDocument() {
        SemanticModel limited = limitedModel(1_000, 8, 1, 8, 1_000, 1_000);
        String xml = "<r><child/></r>";
        SemanticLoadResult result = limited.load(xml, SourceOrigin.unknown());
        assertFailClosed(result, "LIMIT_EXCEEDED", xml);
        assertTrue(result.getDocument().isEmpty(),
                "Fail-closed loads must not expose a partial document graph");
    }

    // -------------------------------------------------------------------------
    // F6 — immutability
    // -------------------------------------------------------------------------

    @Test
    public void returnedCollectionsAreImmutable() {
        String xml = "<root a=\"1\">text</root>";
        SemanticLoadResult result = new SemanticModel().load(xml, SourceOrigin.unknown());
        assertTrue(result.isSuccessful(), diagnosticText(result));
        SemanticDocument document = result.getDocument().get();

        assertUnsupportedMutation(() -> document.getContent().add(
                new SemanticComment("/#x", "x", SourceLocation.unknown())));
        assertUnsupportedMutation(() -> document.getRoot().getContent().clear());
        assertUnsupportedMutation(() -> document.getRoot().getAttributes().remove(0));
        assertUnsupportedMutation(() -> result.getDiagnostics().add(
                new SemanticDiagnostic("X", SemanticDiagnostic.Severity.ERROR, "x",
                        SourceLocation.unknown())));
    }

    // -------------------------------------------------------------------------
    // F6 — lexical / text fidelity
    // -------------------------------------------------------------------------

    @Test
    public void preservesExactElementTextWhitespaceWithoutTrimming() {
        String xml = "<r>  hi \t\n </r>";
        SemanticLoadResult result = new SemanticModel().load(xml, SourceOrigin.unknown());
        assertTrue(result.isSuccessful(), diagnosticText(result));
        SemanticText text = (SemanticText) result.getDocument().get().getRoot().getContent().get(0);
        assertEquals(text.getText(), "  hi \t\n ");
        assertFalse(text.isCdata());
    }

    @Test
    public void assertsCdataContentValue() {
        String xml = "<r><![CDATA[<raw>&not-entity;]]></r>";
        SemanticLoadResult result = new SemanticModel().load(xml, SourceOrigin.unknown());
        assertTrue(result.isSuccessful(), diagnosticText(result));
        SemanticText text = (SemanticText) result.getDocument().get().getRoot().getContent().get(0);
        assertTrue(text.isCdata());
        assertEquals(text.getText(), "<raw>&not-entity;");
    }

    @Test
    public void assertsCommentContentValue() {
        String xml = "<r><!-- note &amp; keep --></r>";
        SemanticLoadResult result = new SemanticModel().load(xml, SourceOrigin.unknown());
        assertTrue(result.isSuccessful(), diagnosticText(result));
        SemanticComment comment = (SemanticComment) result.getDocument().get().getRoot().getContent().get(0);
        assertEquals(comment.getText(), " note &amp; keep ");
    }

    // -------------------------------------------------------------------------
    // F7 — textNodeCount
    // -------------------------------------------------------------------------

    @Test
    public void textNodeCount_countsTextInsideElement() {
        SemanticModel model = new SemanticModel();
        SemanticDocument document = model.load("<r>inside</r>", SourceOrigin.unknown()).getDocument().get();
        assertEquals(model.inspect(document).getTextNodeCount(), 1);
    }

    @Test
    public void documentLevelText_nonWhitespaceOutsideRootFailsParse() {
        SemanticLoadResult result = new SemanticModel().load("x<root/>", SourceOrigin.unknown());
        assertFailClosed(result, "XML_PARSE_ERROR", "x<root/>");
    }

    @Test
    public void documentLevelWhitespace_notSurfacedByActiveSaxProvider() {
        // Analyzer has appendDocumentText for whitespace-only characters() outside the
        // root, but the active JDK Xerces SAX provider does not report that whitespace
        // into ContentHandler.characters for this input shape — so the loaded graph
        // contains only the root element. Document-level text counting is proven via
        // public SemanticDocument construction in
        // textNodeCount_countsManuallyConstructedDocumentLevelText.
        SemanticModel model = new SemanticModel();
        String xml = " \n<root/>\t ";
        SemanticLoadResult result = model.load(xml, SourceOrigin.unknown());
        assertTrue(result.isSuccessful(), diagnosticText(result));
        SemanticDocument document = result.getDocument().get();
        long documentLevelText = document.getContent().stream()
                .filter(SemanticText.class::isInstance)
                .count();
        assertEquals(documentLevelText, 0L,
                "Active SAX provider did not surface document-level whitespace; content="
                        + document.getContent());
        assertEquals(model.inspect(document).getTextNodeCount(), 0);
    }

    @Test
    public void textNodeCount_countsManuallyConstructedDocumentLevelText() {
        // Proves inspect counting independently of SAX whitespace reporting quirks.
        SemanticElement root = new SemanticElement(
                "/{}r[1]",
                new QualifiedName("", "r", ""),
                List.of(),
                List.of(),
                SourceLocation.unknown());
        SemanticText before = new SemanticText("/#text()[1]", " ", false, SourceLocation.unknown());
        SemanticText after = new SemanticText("/#text()[3]", "\n", false, SourceLocation.unknown());
        SemanticDocument document = new SemanticDocument(
                SourceOrigin.unknown(),
                " <r/>\n",
                List.of(before, root, after),
                root);
        assertEquals(new SemanticModel().inspect(document).getTextNodeCount(), 2);
    }

    @Test
    public void textNodeCount_countsCdataAsText() {
        SemanticModel model = new SemanticModel();
        SemanticDocument document = model.load("<r><![CDATA[x]]></r>", SourceOrigin.unknown())
                .getDocument().get();
        assertEquals(model.inspect(document).getTextNodeCount(), 1);
        assertTrue(((SemanticText) document.getRoot().getContent().get(0)).isCdata());
    }

    @Test
    public void textNodeCount_ignoresCommentsAndProcessingInstructions() {
        SemanticModel model = new SemanticModel();
        String xml = "<?probe data?><r><!--c--><?inner x?>text</r>";
        SemanticDocument document = model.load(xml, SourceOrigin.unknown()).getDocument().get();
        assertTrue(document.getContent().get(0) instanceof SemanticProcessingInstruction);
        assertTrue(document.getRoot().getContent().get(0) instanceof SemanticComment);
        assertTrue(document.getRoot().getContent().get(1) instanceof SemanticProcessingInstruction);
        assertTrue(document.getRoot().getContent().get(2) instanceof SemanticText);
        // Only the element text node contributes; comments and PIs do not.
        assertEquals(model.inspect(document).getTextNodeCount(), 1);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static SemanticModel limitedModel(int maxXmlCharacters, int maxDepth, int maxElements,
            int maxAttributesPerElement, int maxTextCharacters, int maxAttributeValueCharacters) {
        return new SemanticModel(new SemanticXmlAnalyzer(new SemanticXmlAnalyzer.Limits(
                maxXmlCharacters, maxDepth, maxElements, maxAttributesPerElement,
                maxTextCharacters, maxAttributeValueCharacters)));
    }

    private static void assertSuccessfulWithSource(SemanticLoadResult result, String expectedSource) {
        assertTrue(result.isSuccessful(), diagnosticText(result));
        assertTrue(result.getDocument().isPresent());
        assertEquals(result.getSourceXml().get(), expectedSource);
    }

    private static void assertFailClosed(SemanticLoadResult result, String expectedCode, String expectedSource) {
        assertFalse(result.isSuccessful(), diagnosticText(result));
        assertTrue(result.getDocument().isEmpty());
        assertTrue(result.getDiagnostics().stream().anyMatch(d -> expectedCode.equals(d.getCode())),
                "Expected diagnostic code " + expectedCode + " but was: " + diagnosticText(result));
        assertEquals(result.getSourceXml().get(), expectedSource);
    }

    private static void assertUnsupportedMutation(Runnable mutation) {
        try {
            mutation.run();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // immutable collections from List.copyOf / unmodifiable views
        }
    }

    private static String diagnosticText(SemanticLoadResult result) {
        return result.getDiagnostics().stream()
                .map(d -> d.getCode() + ": " + d.getMessage())
                .collect(java.util.stream.Collectors.joining("; "));
    }
}
