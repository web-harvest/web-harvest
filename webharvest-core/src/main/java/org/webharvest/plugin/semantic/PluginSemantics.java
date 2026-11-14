package org.webharvest.plugin.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable semantic descriptor describing how a plugin behaves in the
 * configuration tree.
 */
public final class PluginSemantics {

    public static final PluginSemantics DEFAULT = PluginSemantics.builder().build();

    private final Set<PluginNodeRole> roles;
    private final boolean allowsChildPlugins;
    private final boolean allowsTextContent;
    private final boolean providesExecutionContext;
    private final boolean executable;
    private final boolean allowsAnyChild;
    private final boolean explicitlyAcceptsAnyChild;
    private final Set<String> allowedChildElements;
    private final Set<String> requiredParentElements;

    private PluginSemantics(Builder builder) {
        this.roles = Collections.unmodifiableSet(EnumSet.copyOf(builder.roles));
        this.allowsChildPlugins = builder.allowsChildPlugins;
        this.allowsTextContent = builder.allowsTextContent;
        this.providesExecutionContext = builder.providesExecutionContext;
        this.executable = builder.executable;
        this.allowsAnyChild = builder.allowsAnyChild;
        this.explicitlyAcceptsAnyChild = builder.explicitlyAcceptsAnyChild;
        this.allowedChildElements = Collections.unmodifiableSet(builder.allowedChildElements);
        this.requiredParentElements = Collections.unmodifiableSet(builder.requiredParentElements);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns an immutable view of all semantic roles assigned to this plugin.
     */
    public Set<PluginNodeRole> getRoles() {
        return roles;
    }

    /**
     * @deprecated Use {@link #getRoles()} or {@link #hasRole(PluginNodeRole)} instead.
     */
    @Deprecated
    public PluginNodeRole getRole() {
        return roles.iterator().next();
    }

    public boolean hasRole(PluginNodeRole role) {
        return roles.contains(role);
    }

    public boolean allowsChildPlugins() {
        return allowsChildPlugins;
    }

    public boolean allowsTextContent() {
        return allowsTextContent;
    }

    public boolean providesExecutionContext() {
        return providesExecutionContext;
    }

    public boolean isExecutable() {
        return executable;
    }

    public boolean allowsAnyChild() {
        return allowsAnyChild;
    }

    /** True only for an explicit VisualPluginMetadata declaration, not a body-role heuristic. */
    public boolean explicitlyAcceptsAnyChild() {
        return explicitlyAcceptsAnyChild;
    }

    public Set<String> getAllowedChildElements() {
        return allowedChildElements;
    }

    public Set<String> getRequiredParentElements() {
        return requiredParentElements;
    }

    public boolean requiresParent() {
        return !requiredParentElements.isEmpty();
    }

    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("roles", new ArrayList<>(roles));
        map.put("role", roles.iterator().next().name());
        map.put("allowsChildPlugins", allowsChildPlugins);
        map.put("allowsTextContent", allowsTextContent);
        map.put("providesExecutionContext", providesExecutionContext);
        map.put("executable", executable);
        map.put("allowsAnyChild", allowsAnyChild);
        map.put("explicitlyAcceptsAnyChild", explicitlyAcceptsAnyChild);
        map.put("allowedChildElements", allowedChildElements);
        map.put("requiredParentElements", requiredParentElements);
        return map;
    }

    /**
     * Builder for {@link PluginSemantics}.
     */
    public static final class Builder {

        private final EnumSet<PluginNodeRole> roles = EnumSet.noneOf(PluginNodeRole.class);
        private boolean allowsChildPlugins = false;
        private boolean allowsTextContent = false;
        private boolean providesExecutionContext = false;
        private boolean executable = true;
        private boolean allowsAnyChild = false;
        private boolean explicitlyAcceptsAnyChild = false;
        private final Set<String> allowedChildElements = new LinkedHashSet<>();
        private final Set<String> requiredParentElements = new LinkedHashSet<>();

        private Builder() {
        }

        public Builder role(PluginNodeRole role) {
            Objects.requireNonNull(role, "role");
            this.roles.clear();
            this.roles.add(role);
            return this;
        }

        public Builder addRole(PluginNodeRole role) {
            Objects.requireNonNull(role, "role");
            this.roles.add(role);
            return this;
        }

        public Builder addRoles(Set<PluginNodeRole> roles) {
            if (roles != null && !roles.isEmpty()) {
                roles.forEach(this::addRole);
            }
            return this;
        }

        public Builder allowsChildPlugins(boolean allowsChildPlugins) {
            this.allowsChildPlugins = allowsChildPlugins;
            if (!allowsChildPlugins) {
                this.allowsAnyChild = false;
                this.allowedChildElements.clear();
            }
            return this;
        }

        public Builder allowsTextContent(boolean allowsTextContent) {
            this.allowsTextContent = allowsTextContent;
            return this;
        }

        public Builder providesExecutionContext(boolean providesExecutionContext) {
            this.providesExecutionContext = providesExecutionContext;
            return this;
        }

        public Builder executable(boolean executable) {
            this.executable = executable;
            return this;
        }

        public Builder explicitlyAcceptsAnyChild(boolean explicitlyAcceptsAnyChild) {
            this.explicitlyAcceptsAnyChild = explicitlyAcceptsAnyChild;
            return this;
        }

        public Builder allowsAnyChild(boolean allowsAnyChild) {
            this.allowsAnyChild = allowsAnyChild;
            if (allowsAnyChild) {
                this.allowsChildPlugins = true;
                this.allowedChildElements.clear();
            }
            return this;
        }

        public Builder addAllowedChild(String elementName) {
            if (elementName != null && !elementName.isEmpty()) {
                this.allowedChildElements.add(elementName.toLowerCase(Locale.ROOT));
                this.allowsChildPlugins = true;
            }
            return this;
        }

        public Builder allowedChildElements(String[] elementNames) {
            if (elementNames != null) {
                for (String name : elementNames) {
                    addAllowedChild(name);
                }
            }
            return this;
        }

        public Builder addRequiredParent(String elementName) {
            if (elementName != null && !elementName.isEmpty()) {
                this.requiredParentElements.add(elementName.toLowerCase(Locale.ROOT));
            }
            return this;
        }

        public Builder requiredParentElements(String[] elementNames) {
            if (elementNames != null) {
                for (String name : elementNames) {
                    addRequiredParent(name);
                }
            }
            return this;
        }

        public PluginSemantics build() {
            if (roles.isEmpty()) {
                roles.add(PluginNodeRole.LEAF);
            }
            if (!allowsChildPlugins) {
                allowedChildElements.clear();
                allowsAnyChild = false;
            }
            return new PluginSemantics(this);
        }
    }
}

