package org.webharvest.plugin.metadata;

import java.util.Collections;
import java.util.List;

/**
 * Semantic constraints and capabilities for a plugin node.
 */
public final class PluginSemantics {

    private final PluginSemanticRole role;
    private final boolean canAppearAtRoot;
    private final boolean allowsChildren;
    private final boolean acceptsAnyChild;
    private final boolean allowBody;
    private final boolean allowTextContent;
    private final List<String> allowedChildTypes;
    private final List<String> requiredParentTypes;

    public PluginSemantics(PluginSemanticRole role,
            boolean canAppearAtRoot,
            boolean allowsChildren,
            boolean acceptsAnyChild,
            boolean allowBody,
            boolean allowTextContent,
            List<String> allowedChildTypes,
            List<String> requiredParentTypes) {
        this.role = role;
        this.canAppearAtRoot = canAppearAtRoot;
        this.allowsChildren = allowsChildren;
        this.acceptsAnyChild = allowsChildren && acceptsAnyChild;
        this.allowBody = allowBody;
        this.allowTextContent = allowTextContent;
        this.allowedChildTypes = allowedChildTypes == null
                ? Collections.emptyList()
                : List.copyOf(allowedChildTypes);
        this.requiredParentTypes = requiredParentTypes == null
                ? Collections.emptyList()
                : List.copyOf(requiredParentTypes);
    }

    public PluginSemanticRole getRole() {
        return role;
    }

    public boolean canAppearAtRoot() {
        return canAppearAtRoot;
    }

    public boolean allowsChildren() {
        return allowsChildren;
    }

    public boolean acceptsAnyChild() {
        return acceptsAnyChild;
    }

    public boolean allowsBody() {
        return allowBody;
    }

    public boolean allowsTextContent() {
        return allowTextContent;
    }

    public List<String> getAllowedChildTypes() {
        return allowedChildTypes;
    }

    public List<String> getRequiredParentTypes() {
        return requiredParentTypes;
    }
}

