package org.webharvest.semantic.internal;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.webharvest.semantic.QualifiedName;
import org.webharvest.semantic.SemanticAttribute;
import org.webharvest.semantic.SemanticComment;
import org.webharvest.semantic.SemanticContent;
import org.webharvest.semantic.SemanticDiagnostic;
import org.webharvest.semantic.SemanticDocument;
import org.webharvest.semantic.SemanticElement;
import org.webharvest.semantic.SemanticLoadResult;
import org.webharvest.semantic.SemanticProcessingInstruction;
import org.webharvest.semantic.SemanticText;
import org.webharvest.semantic.SourceLocation;
import org.webharvest.semantic.SourceOrigin;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Dedicated hardened, read-only SAX + LexicalHandler XML analyzer.
 *
 * <p>This type is an internal implementation detail of
 * {@link org.webharvest.semantic.SemanticModel}. It is {@code public} so tests and
 * the façade can construct it, but it is <strong>not</strong> a supported public API.</p>
 */
public final class SemanticXmlAnalyzer {
    private static final String DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String EXTERNAL_GENERAL = "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER = "http://xml.org/sax/features/external-parameter-entities";
    private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    public static final String LEXICAL_HANDLER = "http://xml.org/sax/properties/lexical-handler";

    /** Supplies the SAX reader used for a load; package-visible for fail-closed tests. */
    @FunctionalInterface
    interface ReaderFactory {
        XMLReader create() throws Exception;
    }

    public static final class Limits {
        final int maxXmlCharacters;
        final int maxDepth;
        final int maxElements;
        final int maxAttributesPerElement;
        final int maxTextCharacters;
        /** Maximum length of a single attribute value in UTF-16 code units ({@link String#length()}). */
        final int maxAttributeValueCharacters;

        public Limits(int maxXmlCharacters, int maxDepth, int maxElements,
                int maxAttributesPerElement, int maxTextCharacters, int maxAttributeValueCharacters) {
            if (maxXmlCharacters < 1 || maxDepth < 1 || maxElements < 1
                    || maxAttributesPerElement < 1 || maxTextCharacters < 1
                    || maxAttributeValueCharacters < 1) {
                throw new IllegalArgumentException("All XML limits must be positive");
            }
            this.maxXmlCharacters = maxXmlCharacters;
            this.maxDepth = maxDepth;
            this.maxElements = maxElements;
            this.maxAttributesPerElement = maxAttributesPerElement;
            this.maxTextCharacters = maxTextCharacters;
            this.maxAttributeValueCharacters = maxAttributeValueCharacters;
        }

        public static Limits defaults() {
            return new Limits(5_000_000, 256, 100_000, 256, 5_000_000, 1_000_000);
        }
    }

    private final Limits limits;
    private final ReaderFactory readerFactory;

    public SemanticXmlAnalyzer(Limits limits) {
        this(limits, SemanticXmlAnalyzer::newSecureReader);
    }

    SemanticXmlAnalyzer(Limits limits, ReaderFactory readerFactory) {
        this.limits = limits;
        this.readerFactory = readerFactory;
    }

    public SemanticLoadResult load(String xml, SourceOrigin origin) {
        List<SemanticDiagnostic> diagnostics = new ArrayList<>();
        if (xml == null) {
            diagnostics.add(error("INVALID_INPUT", "XML input must not be null", SourceLocation.unknown()));
            return new SemanticLoadResult(origin, null, null, diagnostics);
        }
        if (xml.length() > limits.maxXmlCharacters) {
            diagnostics.add(error("LIMIT_EXCEEDED", "XML input exceeds the configured character limit",
                    SourceLocation.unknown()));
            return new SemanticLoadResult(origin, xml, null, diagnostics);
        }

        Handler handler = new Handler(limits);
        XMLReader reader;
        try {
            reader = readerFactory.create();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.setEntityResolver(REJECTING_RESOLVER);
            setAndVerifyLexicalHandler(reader, handler);
        } catch (Exception ex) {
            diagnostics.add(error("XML_ANALYZER_FAILURE",
                    ex.getClass().getSimpleName() + ": " + String.valueOf(ex.getMessage()),
                    handler.location()));
            return new SemanticLoadResult(origin, xml, null, diagnostics);
        }

        try {
            reader.parse(new InputSource(new StringReader(xml)));
            SemanticDocument document = handler.buildDocument(origin, xml);
            return new SemanticLoadResult(origin, xml, document, diagnostics);
        } catch (LimitException ex) {
            diagnostics.add(error("LIMIT_EXCEEDED", ex.getMessage(), handler.location()));
        } catch (SecurityExceptionSax ex) {
            diagnostics.add(error("SECURITY_VIOLATION", ex.getMessage(), handler.location()));
        } catch (SAXParseException ex) {
            String message = ex.getMessage() == null ? "XML parse failure" : ex.getMessage();
            diagnostics.add(error("XML_PARSE_ERROR", message, location(ex)));
        } catch (SAXException ex) {
            String message = ex.getMessage() == null ? "XML parser rejected the input" : ex.getMessage();
            diagnostics.add(error("XML_PARSE_ERROR", message, handler.location()));
        } catch (Exception ex) {
            diagnostics.add(error("XML_ANALYZER_FAILURE",
                    ex.getClass().getSimpleName() + ": " + String.valueOf(ex.getMessage()), handler.location()));
        }
        // Never expose a partial graph after a parse, security or limit failure.
        return new SemanticLoadResult(origin, xml, null, diagnostics);
    }

    private static SemanticDiagnostic error(String code, String message, SourceLocation location) {
        return new SemanticDiagnostic(code, SemanticDiagnostic.Severity.ERROR, message, location);
    }

    private static SourceLocation location(SAXParseException ex) {
        return new SourceLocation(ex.getLineNumber(), ex.getColumnNumber());
    }

    private static final EntityResolver REJECTING_RESOLVER = (publicId, systemId) -> {
        throw new SecurityExceptionSax("External entity resolution is prohibited");
    };

    public static XMLReader newSecureReader() throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        try {
            factory.setXIncludeAware(false);
            if (factory.isXIncludeAware()) {
                throw new SAXException("Unable to disable XInclude");
            }
        } catch (UnsupportedOperationException ex) {
            throw new SAXException("XML provider cannot disable XInclude", ex);
        }

        SAXParser parser = factory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        setAndVerify(reader, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setAndVerify(reader, DISALLOW_DOCTYPE, true);
        setAndVerify(reader, EXTERNAL_GENERAL, false);
        setAndVerify(reader, EXTERNAL_PARAMETER, false);
        setAndVerify(reader, LOAD_EXTERNAL_DTD, false);
        setAndVerify(reader, "http://xml.org/sax/features/validation", false);
        reader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        reader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        if (!"".equals(reader.getProperty(XMLConstants.ACCESS_EXTERNAL_DTD))
                || !"".equals(reader.getProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA))) {
            throw new SAXException("Unable to verify external XML access restrictions");
        }
        return reader;
    }

    static void setAndVerifyLexicalHandler(XMLReader reader, LexicalHandler handler) throws SAXException {
        reader.setProperty(LEXICAL_HANDLER, handler);
        if (reader.getProperty(LEXICAL_HANDLER) != handler) {
            throw new SAXException("XML provider did not retain the lexical handler");
        }
    }

    private static void setAndVerify(XMLReader reader, String feature, boolean value) throws SAXException {
        reader.setFeature(feature, value);
        if (reader.getFeature(feature) != value) {
            throw new SAXException("XML provider did not retain required security feature: " + feature);
        }
    }

    private static final class LimitException extends SAXException {
        LimitException(String message) { super(message); }
    }

    /** Typed SAX security rejection; classified as {@code SECURITY_VIOLATION}. */
    static final class SecurityExceptionSax extends SAXException {
        SecurityExceptionSax(String message) { super(message); }
    }

    private static final class ElementBuilder {
        final String id;
        final QualifiedName name;
        final List<SemanticAttribute> attributes;
        final List<SemanticContent> content = new ArrayList<>();
        final Map<QualifiedName, Integer> childOccurrences = new HashMap<>();
        final SourceLocation location;
        int contentIndex;

        ElementBuilder(String id, QualifiedName name, List<SemanticAttribute> attributes, SourceLocation location) {
            this.id = id;
            this.name = name;
            this.attributes = attributes;
            this.location = location;
        }

        SemanticElement freeze() {
            List<SemanticContent> frozen = new ArrayList<>(content.size());
            for (SemanticContent item : content) {
                if (item instanceof ElementBuilderContent) {
                    frozen.add(((ElementBuilderContent) item).freeze());
                } else {
                    frozen.add(item);
                }
            }
            return new SemanticElement(id, name, attributes, frozen, location);
        }
    }

    /** Internal wrapper permits builders and immutable nodes in one ordered list. */
    private static final class ElementBuilderContent implements SemanticContent {
        private final ElementBuilder builder;
        ElementBuilderContent(ElementBuilder builder) { this.builder = builder; }
        SemanticElement freeze() { return builder.freeze(); }
        @Override public String getId() { return builder.id; }
        @Override public SourceLocation getLocation() { return builder.location; }
    }

    private static final class Handler extends DefaultHandler implements LexicalHandler {
        private final Limits limits;
        private final Deque<ElementBuilder> stack = new ArrayDeque<>();
        private final List<SemanticContent> documentContent = new ArrayList<>();
        private Locator locator;
        private boolean inCdata;
        private int elementCount;
        private int textCharacters;
        private ElementBuilder rootBuilder;

        Handler(Limits limits) { this.limits = limits; }

        @Override public void setDocumentLocator(Locator locator) { this.locator = locator; }

        SourceLocation location() {
            return locator == null ? SourceLocation.unknown()
                    : new SourceLocation(locator.getLineNumber(), locator.getColumnNumber());
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if (stack.size() + 1 > limits.maxDepth) throw new LimitException("XML nesting depth limit exceeded");
            if (++elementCount > limits.maxElements) throw new LimitException("XML element count limit exceeded");
            if (attributes.getLength() > limits.maxAttributesPerElement) {
                throw new LimitException("Attributes-per-element limit exceeded");
            }
            String local = localName == null || localName.isEmpty() ? localPart(qName) : localName;
            QualifiedName name = new QualifiedName(uri, local, prefixPart(qName));
            String id;
            if (stack.isEmpty()) {
                if (rootBuilder != null) throw new SAXException("Multiple document elements are not allowed");
                id = "/" + name.expandedName() + "[1]";
            } else {
                ElementBuilder parent = stack.peek();
                int occurrence = parent.childOccurrences.merge(name, 1, Integer::sum);
                id = parent.id + "/" + name.expandedName() + "[" + occurrence + "]";
            }
            List<SemanticAttribute> attrs = new ArrayList<>(attributes.getLength());
            for (int i = 0; i < attributes.getLength(); i++) {
                String attrValue = attributes.getValue(i);
                if (attrValue != null && attrValue.length() > limits.maxAttributeValueCharacters) {
                    throw new LimitException("XML attribute value character limit exceeded");
                }
                String attrQName = attributes.getQName(i);
                String attrLocal = attributes.getLocalName(i);
                if (attrLocal == null || attrLocal.isEmpty()) attrLocal = localPart(attrQName);
                attrs.add(new SemanticAttribute(
                        new QualifiedName(attributes.getURI(i), attrLocal, prefixPart(attrQName)),
                        attrValue));
            }
            ElementBuilder builder = new ElementBuilder(id, name, attrs, location());
            if (stack.isEmpty()) {
                rootBuilder = builder;
                documentContent.add(new ElementBuilderContent(builder));
            } else {
                append(stack.peek(), new ElementBuilderContent(builder));
            }
            stack.push(builder);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (stack.isEmpty()) throw new SAXException("Unexpected closing element");
            stack.pop();
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (length == 0) return;
            textCharacters += length;
            if (textCharacters > limits.maxTextCharacters) throw new LimitException("XML text character limit exceeded");
            String text = new String(ch, start, length);
            if (stack.isEmpty()) {
                if (!text.trim().isEmpty()) throw new SAXException("Non-whitespace text outside the document element");
                appendDocumentText(text);
            } else {
                appendText(stack.peek(), text, inCdata);
            }
        }

        private void appendDocumentText(String value) {
            int index = documentContent.size() + 1;
            documentContent.add(new SemanticText("/#text()[" + index + "]", value, inCdata, location()));
        }

        private void appendText(ElementBuilder parent, String value, boolean cdata) {
            if (!parent.content.isEmpty()) {
                SemanticContent last = parent.content.get(parent.content.size() - 1);
                if (last instanceof SemanticText && ((SemanticText) last).isCdata() == cdata) {
                    SemanticText previous = (SemanticText) last;
                    parent.content.set(parent.content.size() - 1,
                            new SemanticText(previous.getId(), previous.getText() + value, cdata, previous.getLocation()));
                    return;
                }
            }
            int index = ++parent.contentIndex;
            parent.content.add(new SemanticText(parent.id + "/#content[" + index + "]", value, cdata, location()));
        }

        private void append(ElementBuilder parent, SemanticContent child) {
            parent.contentIndex++;
            parent.content.add(child);
        }

        private void appendMisc(SemanticContent item) {
            if (stack.isEmpty()) documentContent.add(item);
            else {
                ElementBuilder parent = stack.peek();
                parent.contentIndex++;
                parent.content.add(item);
            }
        }

        @Override public void processingInstruction(String target, String data) {
            String id = stack.isEmpty() ? "/#pi()[" + (documentContent.size() + 1) + "]"
                    : stack.peek().id + "/#content[" + (stack.peek().contentIndex + 1) + "]";
            appendMisc(new SemanticProcessingInstruction(id, target, data, location()));
        }

        @Override public void comment(char[] ch, int start, int length) {
            String id = stack.isEmpty() ? "/#comment()[" + (documentContent.size() + 1) + "]"
                    : stack.peek().id + "/#content[" + (stack.peek().contentIndex + 1) + "]";
            appendMisc(new SemanticComment(id, new String(ch, start, length), location()));
        }

        @Override public void startCDATA() { inCdata = true; }
        @Override public void endCDATA() { inCdata = false; }
        @Override public void startDTD(String name, String publicId, String systemId) throws SAXException {
            throw new SecurityExceptionSax("DOCTYPE declarations are prohibited");
        }
        @Override public void endDTD() { }
        @Override public void startEntity(String name) { }
        @Override public void endEntity(String name) { }

        @Override public void warning(SAXParseException exception) { }
        @Override public void error(SAXParseException exception) throws SAXException { throw exception; }
        @Override public void fatalError(SAXParseException exception) throws SAXException { throw exception; }

        SemanticDocument buildDocument(SourceOrigin origin, String xml) throws SAXException {
            if (!stack.isEmpty() || rootBuilder == null) throw new SAXException("XML document has no complete root element");
            List<SemanticContent> frozen = new ArrayList<>(documentContent.size());
            SemanticElement root = null;
            for (SemanticContent item : documentContent) {
                if (item instanceof ElementBuilderContent) {
                    ElementBuilder builder = ((ElementBuilderContent) item).builder;
                    SemanticElement element = builder.freeze();
                    frozen.add(element);
                    if (builder == rootBuilder) root = element;
                } else {
                    frozen.add(item);
                }
            }
            if (root == null) throw new SAXException("XML document root was not retained");
            return new SemanticDocument(origin, xml, frozen, root);
        }

        private static String localPart(String qName) {
            if (qName == null) return "";
            int colon = qName.indexOf(':');
            return colon < 0 ? qName : qName.substring(colon + 1);
        }

        private static String prefixPart(String qName) {
            if (qName == null) return "";
            int colon = qName.indexOf(':');
            return colon < 0 ? "" : qName.substring(0, colon);
        }
    }

    /**
     * Minimal XMLReader test double that accepts a lexical-handler property but does
     * not retain the supplied instance.
     */
    static final class NonRetainingLexicalHandlerReader implements XMLReader {
        @Override public boolean getFeature(String name) { return false; }
        @Override public void setFeature(String name, boolean value) { }
        @Override public Object getProperty(String name) {
            return LEXICAL_HANDLER.equals(name) ? null : null;
        }
        @Override public void setProperty(String name, Object value) {
            // Accept without retaining so verification fails closed.
        }
        @Override public void setEntityResolver(EntityResolver resolver) { }
        @Override public EntityResolver getEntityResolver() { return null; }
        @Override public void setDTDHandler(DTDHandler handler) { }
        @Override public DTDHandler getDTDHandler() { return null; }
        @Override public void setContentHandler(ContentHandler handler) { }
        @Override public ContentHandler getContentHandler() { return null; }
        @Override public void setErrorHandler(ErrorHandler handler) { }
        @Override public ErrorHandler getErrorHandler() { return null; }
        @Override public void parse(InputSource input) {
            throw new UnsupportedOperationException("parse is not used by the lexical-handler retention test");
        }
        @Override public void parse(String systemId) {
            throw new UnsupportedOperationException("parse is not used by the lexical-handler retention test");
        }
    }
}
