package org.webharvest.plugin.annotations;

/**
 * Enumerates UI field kinds that the metadata catalog can expose to downstream
 * editors (Visual Designer, VS Code extension, etc.). Kept intentionally small
 * and technology-agnostic so that different host shells can map the value to
 * their preferred widgets.
 */
public enum UiFieldKind {
    TEXT,
    TEXTAREA,
    SELECT,
    BOOLEAN,
    EXPRESSION,
    CODE,
    NUMBER,
    SLIDER,
    URL,
    JSON
}


