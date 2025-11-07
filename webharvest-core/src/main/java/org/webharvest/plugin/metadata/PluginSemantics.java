package org.webharvest.plugin.metadata;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Semantic constraints and capabilities for a plugin node.
 */
public final class PluginSemantics {

    private final Set<PluginSemanticRole> roles;
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
        this(EnumSet.of(role), canAppearAtRoot, allowsChildren, acceptsAnyChild, allowBody, allowTextContent, allowedChildTypes, requiredParentTypes);
    }

    public PluginSemantics(Set<PluginSemanticRole> roles,
            boolean canAppearAtRoot,
            boolean allowsChildren,
            boolean acceptsAnyChild,
            boolean allowBody,
            boolean allowTextContent,
            List<String> allowedChildTypes,
            List<String> requiredParentTypes) {
        EnumSet<PluginSemanticRole> roleSet = roles.isEmpty()
                ? EnumSet.of(PluginSemanticRole.LEAF)
                : EnumSet.copyOf(roles);
        this.roles = Collections.unmodifiableSet(roleSet);
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

    public Set<PluginSemanticRole> getRoles() {
        return roles;
    }

    /**
     * @deprecated Use {@link #getRoles()} or {@link #hasRole(PluginSemanticRole)} instead.
     */
    @Deprecated
    public PluginSemanticRole getRole() {
        return roles.iterator().next();
    }

    public boolean hasRole(PluginSemanticRole role) {
        return roles.contains(role);
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

