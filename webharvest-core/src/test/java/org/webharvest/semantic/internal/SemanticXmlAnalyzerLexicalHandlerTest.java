package org.webharvest.semantic.internal;

import org.testng.annotations.Test;
import org.webharvest.semantic.SemanticLoadResult;
import org.webharvest.semantic.SourceOrigin;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import static org.testng.Assert.*;

public class SemanticXmlAnalyzerLexicalHandlerTest {

    @Test
    public void setAndVerifyLexicalHandlerRequiresSameInstance() throws Exception {
        var reader = SemanticXmlAnalyzer.newSecureReader();
        LexicalHandler handler = new NoOpLexicalHandler();
        SemanticXmlAnalyzer.setAndVerifyLexicalHandler(reader, handler);
        assertSame(reader.getProperty(SemanticXmlAnalyzer.LEXICAL_HANDLER), handler);
    }

    @Test
    public void loadFailsClosedWhenProviderDoesNotRetainLexicalHandler() {
        SemanticXmlAnalyzer analyzer = new SemanticXmlAnalyzer(
                SemanticXmlAnalyzer.Limits.defaults(),
                SemanticXmlAnalyzer.NonRetainingLexicalHandlerReader::new);
        SemanticLoadResult result = analyzer.load("<root/>", SourceOrigin.unknown());
        assertFalse(result.isSuccessful());
        assertTrue(result.getDocument().isEmpty());
        assertEquals(result.getDiagnostics().get(0).getCode(), "XML_ANALYZER_FAILURE");
        assertTrue(result.getDiagnostics().get(0).getMessage().contains("lexical handler")
                || result.getDiagnostics().get(0).getMessage().contains("SAXException"),
                result.getDiagnostics().get(0).getMessage());
    }

    @Test
    public void setAndVerifyLexicalHandlerFailsWhenInstanceNotRetained() {
        SemanticXmlAnalyzer.NonRetainingLexicalHandlerReader reader =
                new SemanticXmlAnalyzer.NonRetainingLexicalHandlerReader();
        try {
            SemanticXmlAnalyzer.setAndVerifyLexicalHandler(reader, new NoOpLexicalHandler());
            fail("Expected SAXException when lexical handler is not retained");
        } catch (SAXException ex) {
            assertTrue(ex.getMessage().contains("lexical handler"));
        }
    }

    private static final class NoOpLexicalHandler implements LexicalHandler {
        @Override public void startDTD(String name, String publicId, String systemId) { }
        @Override public void endDTD() { }
        @Override public void startEntity(String name) { }
        @Override public void endEntity(String name) { }
        @Override public void startCDATA() { }
        @Override public void endCDATA() { }
        @Override public void comment(char[] ch, int start, int length) { }
    }
}
