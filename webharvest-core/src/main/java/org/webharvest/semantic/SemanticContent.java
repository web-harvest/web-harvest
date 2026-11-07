package org.webharvest.semantic;

/** Ordered item in an XML element or document content sequence. */
public interface SemanticContent {
    String getId();
    SourceLocation getLocation();
}
