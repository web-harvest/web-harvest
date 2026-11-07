package org.webharvest.semantic;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SemanticModelTest {
    private final SemanticModel model = new SemanticModel();

    @Test
    public void preservesOrderedMixedContentAndLexicalNodes() {
        String xml = "<?probe before?><root a=\"1\" b=\"2\"> left<![CDATA[<raw>]]><!--note--><child/> right</root>";
        SemanticLoadResult result = model.load(xml, new SourceOrigin("test:ordered"));
        assertTrue(result.isSuccessful(), diagnosticText(result));
        SemanticDocument document = result.getDocument().get();
        assertEquals(document.getOrigin().getValue(), "test:ordered");
        assertEquals(document.getSourceXml(), xml);
        assertEquals(document.getRoot().getAttributes().get(0).getName().getLocalName(), "a");
        assertEquals(document.getRoot().getAttributes().get(1).getName().getLocalName(), "b");
        assertTrue(document.getContent().get(0) instanceof SemanticProcessingInstruction);
        assertSame(document.getContent().get(1), document.getRoot(),
                "The document root must be the same immutable instance exposed in document content");
        assertTrue(document.getRoot().getContent().get(0) instanceof SemanticText);
        assertTrue(((SemanticText) document.getRoot().getContent().get(1)).isCdata());
        assertTrue(document.getRoot().getContent().get(2) instanceof SemanticComment);
        assertTrue(document.getRoot().getContent().get(3) instanceof SemanticElement);
        assertTrue(document.getRoot().getContent().get(4) instanceof SemanticText);
        assertSame(model.inspect(document).getDocument(), document);
        assertEquals(model.inspect(document).getElementCount(), 2);
    }

    @Test
    public void createsDeterministicElementPaths() {
        String xml = "<r><x/><x/><y><x/></y></r>";
        SemanticDocument first = model.load(xml, SourceOrigin.unknown()).getDocument().get();
        SemanticDocument second = model.load(xml, SourceOrigin.unknown()).getDocument().get();
        assertEquals(first.getRoot().getContent().get(0).getId(), second.getRoot().getContent().get(0).getId());
        assertEquals(first.getRoot().getContent().get(1).getId(), second.getRoot().getContent().get(1).getId());
        assertEquals(first.getRoot().getContent().get(0).getId(), "/{}r[1]/{}x[1]");
        assertEquals(first.getRoot().getContent().get(1).getId(), "/{}r[1]/{}x[2]");
    }

    @Test
    public void rejectsDoctypeAndDoesNotReturnPartialDocument() {
        String xml = "<!DOCTYPE root [<!ENTITY x 'expanded'>]><root>&x;</root>";
        SemanticLoadResult result = model.load(xml, SourceOrigin.unknown());
        assertFalse(result.isSuccessful());
        assertTrue(result.getDocument().isEmpty());
        assertTrue(result.getDiagnostics().stream().anyMatch(d ->
                "SECURITY_VIOLATION".equals(d.getCode())));
        assertEquals(result.getSourceXml().get(), xml);
    }

    @Test
    public void rejectsExternalEntityWithoutReadingIt() {
        String xml = "<!DOCTYPE root SYSTEM 'file:///etc/passwd'><root/>";
        SemanticLoadResult result = model.load(xml, SourceOrigin.unknown());
        assertFalse(result.isSuccessful());
        assertTrue(result.getDocument().isEmpty());
    }

    @Test
    public void failsClosedWhenDepthLimitIsExceeded() {
        SemanticModel limited = new SemanticModel(new SemanticXmlAnalyzer(
                new SemanticXmlAnalyzer.Limits(100, 2, 10, 4, 100)));
        SemanticLoadResult result = limited.load("<r><a><b/></a></r>", SourceOrigin.unknown());
        assertFalse(result.isSuccessful());
        assertTrue(result.getDocument().isEmpty());
        assertEquals(result.getDiagnostics().get(0).getCode(), "LIMIT_EXCEEDED");
    }

    @Test
    public void failsClosedWhenTextLimitIsExceeded() {
        SemanticModel limited = new SemanticModel(new SemanticXmlAnalyzer(
                new SemanticXmlAnalyzer.Limits(100, 4, 10, 4, 5)));
        SemanticLoadResult result = limited.load("<r>123456</r>", SourceOrigin.unknown());
        assertFalse(result.isSuccessful());
        assertTrue(result.getDocument().isEmpty());
        assertEquals(result.getDiagnostics().get(0).getCode(), "LIMIT_EXCEEDED");
    }

    @Test
    public void reportsNamespaceAwareNames() {
        SemanticLoadResult result = model.load(
                "<p:root xmlns:p='urn:example'><p:item/></p:root>", SourceOrigin.unknown());
        assertTrue(result.isSuccessful(), diagnosticText(result));
        assertEquals(result.getDocument().get().getRoot().getName().getNamespaceUri(), "urn:example");
        SemanticElement child = (SemanticElement) result.getDocument().get().getRoot().getContent().get(0);
        assertEquals(child.getName().getLocalName(), "item");
        assertEquals(child.getName().getPrefix(), "p");
    }

    private static String diagnosticText(SemanticLoadResult result) {
        return result.getDiagnostics().stream()
                .map(d -> d.getCode() + ": " + d.getMessage())
                .collect(java.util.stream.Collectors.joining("; "));
    }
}
