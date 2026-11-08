package org.webharvest.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.webharvest.plugin.metadata.PluginDescriptor;

/**
 * Immutable Stage-1 view of a catalog plugin descriptor, including the structural
 * schema snapshot needed by later validation (Agent B2).
 *
 * <p>When {@code extensions.schema} is absent, {@link #hasReliableSchema()} is
 * {@code false} and body/root flags are unknown ({@link Optional#empty()}); lists
 * are empty rather than invented.
 *
 * <p>Sub-element and parent names are local names as declared in the catalog schema
 * (namespace-unaware as stored today).
 */
public final class Stage1PluginDescriptor {
    private final String id;
    private final String namespaceUri;
    private final String localName;
    private final String className;
    private final boolean reliableSchema;
    private final List<String> requiredAttributes;
    private final List<String> optionalAttributes;
    private final List<AttributeDefinition> attributeDefinitions;
    private final List<String> allowedSubElements;
    private final List<String> requiredSubElements;
    private final Optional<Boolean> allowsBody;
    private final Optional<Boolean> canAppearAtRoot;
    private final List<String> requiredParents;
    private final List<QualifiedName> allowedChildNames;
    private final List<QualifiedName> requiredChildNames;
    private final List<QualifiedName> requiredParentNames;
    private final ChildPolicy childPolicy;
    private final ChildPolicyProvenance childPolicyProvenance;

    private Stage1PluginDescriptor(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.namespaceUri = builder.namespaceUri == null ? "" : builder.namespaceUri;
        this.localName = Objects.requireNonNull(builder.localName, "localName");
        this.className = Objects.requireNonNull(builder.className, "className");
        this.reliableSchema = builder.reliableSchema;
        this.requiredAttributes = List.copyOf(builder.requiredAttributes);
        this.optionalAttributes = List.copyOf(builder.optionalAttributes);
        this.attributeDefinitions = List.copyOf(builder.attributeDefinitions);
        this.allowedSubElements = List.copyOf(builder.allowedSubElements);
        this.requiredSubElements = List.copyOf(builder.requiredSubElements);
        this.allowsBody = builder.allowsBody;
        this.canAppearAtRoot = builder.canAppearAtRoot;
        this.requiredParents = List.copyOf(builder.requiredParents);
        this.allowedChildNames = List.copyOf(builder.allowedChildNames);
        this.requiredChildNames = List.copyOf(builder.requiredChildNames);
        this.requiredParentNames = List.copyOf(builder.requiredParentNames);
        this.childPolicy = Objects.requireNonNull(builder.childPolicy, "childPolicy");
        this.childPolicyProvenance = Objects.requireNonNull(builder.childPolicyProvenance, "childPolicyProvenance");
        if (this.childPolicy == ChildPolicy.FORBID_CHILDREN
                && this.childPolicyProvenance != ChildPolicyProvenance.EXPLICIT_CLOSED_POLICY) {
            throw new IllegalArgumentException(
                    "FORBID_CHILDREN requires EXPLICIT_CLOSED_POLICY provenance");
        }
    }

    /**
     * Builds a Stage-1 view from a catalog {@link PluginDescriptor}.
     *
     * <p>Namespace comes from {@code extensions.namespace}; structural fields from
     * {@code extensions.schema} when present.
     */
    @SuppressWarnings("unchecked")
    public static Stage1PluginDescriptor fromCatalog(PluginDescriptor catalog) {
        Objects.requireNonNull(catalog, "catalog");
        Map<String, Object> extensions = catalog.getExtensions();
        Object ns = extensions.get("namespace");
        String namespaceUri = ns instanceof String ? (String) ns : "";

        Builder builder = builder()
                .id(catalog.getId())
                .namespaceUri(namespaceUri)
                .localName(catalog.getElementName())
                .className(catalog.getClassName());

        Object schemaObj = extensions.get("schema");
        if (!(schemaObj instanceof Map)) {
            builder.reliableSchema(false);
            applyCatalogSemantics(builder, catalog.getSemantics());
            return builder.build();
        }

        Map<String, Object> schema = (Map<String, Object>) schemaObj;
        builder.reliableSchema(true);
        builder.requiredAttributes(stringList(schema.get("requiredAttributes")));
        builder.optionalAttributes(stringList(schema.get("optionalAttributes")));
        builder.allowedSubElements(stringList(schema.get("allowedSubElements")));
        builder.requiredSubElements(stringList(schema.get("requiredSubElements")));
        builder.requiredParents(stringList(schema.get("requiredParents")));

        if (schema.containsKey("allowsBody")) {
            builder.allowsBody(asBoolean(schema.get("allowsBody")));
        }
        if (schema.containsKey("canAppearAtRoot")) {
            builder.canAppearAtRoot(asBoolean(schema.get("canAppearAtRoot")));
        }

        Object attrDefs = schema.get("attributeDefinitions");
        if (attrDefs instanceof List) {
            List<AttributeDefinition> parsed = new ArrayList<>();
            for (Object item : (List<?>) attrDefs) {
                if (item instanceof Map) {
                    parsed.add(AttributeDefinition.fromMap((Map<String, Object>) item));
                }
            }
            builder.attributeDefinitions(parsed);
        }
        applyCatalogSemantics(builder, catalog.getSemantics());
        return builder.build();
    }

    private static void applyCatalogSemantics(Builder builder,
            org.webharvest.plugin.metadata.PluginSemantics semantics) {
        if (semantics != null) {
            List<String> catalogChildren = semantics.getAllowedChildTypes();
            if (catalogChildren != null && !catalogChildren.isEmpty()) {
                // Catalog semantics contains the schema + visual metadata union.
                // Preserve it even when an explicit ANY_CHILD policy takes precedence.
                builder.allowedSubElements(catalogChildren);
            }
            if (semantics.explicitlyAcceptsAnyChild()) {
                builder.childPolicy(ChildPolicy.ANY_CHILD, ChildPolicyProvenance.EXPLICIT_VISUAL_METADATA);
                return;
            }
            if (catalogChildren != null && !catalogChildren.isEmpty()) {
                builder.childPolicy(ChildPolicy.ALLOW_LIST, ChildPolicyProvenance.CATALOG_SEMANTICS);
                return;
            }
            if (semantics.acceptsAnyChild()) {
                // Legacy BODY_EVALUATOR inference is not structural authority.
                builder.childPolicy(ChildPolicy.UNKNOWN, ChildPolicyProvenance.LEGACY_BODY_HEURISTIC);
                return;
            }
        }
        if (!builder.allowedSubElements.isEmpty()) {
            builder.childPolicy(ChildPolicy.ALLOW_LIST, ChildPolicyProvenance.SCHEMA_ALLOW_LIST);
        } else {
            builder.childPolicy(ChildPolicy.UNKNOWN, ChildPolicyProvenance.MISSING_METADATA);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getNamespaceUri() {
        return namespaceUri;
    }

    public String getLocalName() {
        return localName;
    }

    public String getClassName() {
        return className;
    }

    /** {@code true} only when catalog {@code extensions.schema} was present. */
    public boolean hasReliableSchema() {
        return reliableSchema;
    }

    public List<String> getRequiredAttributes() {
        return requiredAttributes;
    }

    public List<String> getOptionalAttributes() {
        return optionalAttributes;
    }

    /**
     * Union of required and optional attribute names, plus names from
     * {@link #getAttributeDefinitions()} when lists alone are incomplete.
     */
    public Set<String> getKnownAttributeNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.addAll(requiredAttributes);
        names.addAll(optionalAttributes);
        for (AttributeDefinition def : attributeDefinitions) {
            names.add(def.getName());
        }
        return Collections.unmodifiableSet(names);
    }

    public List<AttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    /** Local names as declared; namespace-unaware in today's catalog schema. */
    public List<String> getAllowedSubElements() {
        return allowedSubElements;
    }

    /** Local names as declared; namespace-unaware in today's catalog schema. */
    public List<String> getRequiredSubElements() {
        return requiredSubElements;
    }

    /** Empty when schema is missing — do not invent. */
    public Optional<Boolean> getAllowsBody() {
        return allowsBody;
    }

    /** Empty when schema is missing — do not invent. */
    public Optional<Boolean> getCanAppearAtRoot() {
        return canAppearAtRoot;
    }

    /** Local names as declared; namespace-unaware in today's catalog schema. */
    public List<String> getRequiredParents() {
        return requiredParents;
    }

    /** Exact namespace-qualified child identities; legacy local-name edges are kept separately. */
    public List<QualifiedName> getAllowedChildNames() { return allowedChildNames; }
    public List<QualifiedName> getRequiredChildNames() { return requiredChildNames; }
    public List<QualifiedName> getRequiredParentNames() { return requiredParentNames; }

    public ChildPolicy getChildPolicy() {
        return childPolicy;
    }

    public ChildPolicyProvenance getChildPolicyProvenance() {
        return childPolicyProvenance;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Stage1PluginDescriptor)) {
            return false;
        }
        Stage1PluginDescriptor that = (Stage1PluginDescriptor) other;
        return id.equals(that.id)
                && namespaceUri.equals(that.namespaceUri)
                && localName.equals(that.localName)
                && className.equals(that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, namespaceUri, localName, className);
    }

    @Override
    public String toString() {
        return id + "[" + "{" + namespaceUri + "}" + localName + "]";
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item != null) {
                out.add(String.valueOf(item));
            }
        }
        return out;
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /** Immutable attribute constraint snapshot from catalog schema extensions. */
    public static final class AttributeDefinition {
        private final String name;
        private final String typeName;
        private final boolean required;
        private final List<String> allowedValues;
        private final Number minValue;
        private final Number maxValue;
        private final String pattern;

        public AttributeDefinition(String name, String typeName, boolean required,
                List<String> allowedValues, Number minValue, Number maxValue, String pattern) {
            this.name = Objects.requireNonNull(name, "name");
            this.typeName = typeName == null ? "STRING" : typeName;
            this.required = required;
            this.allowedValues = allowedValues == null ? List.of() : List.copyOf(allowedValues);
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.pattern = pattern;
        }

        @SuppressWarnings("unchecked")
        static AttributeDefinition fromMap(Map<String, Object> map) {
            String name = String.valueOf(map.getOrDefault("name", ""));
            String typeName = map.get("type") != null ? String.valueOf(map.get("type")) : "STRING";
            boolean required = Boolean.TRUE.equals(map.get("required"))
                    || "true".equalsIgnoreCase(String.valueOf(map.get("required")));
            List<String> allowed = stringList(map.get("allowedValues"));
            Number min = asNumber(map.get("minValue"));
            Number max = asNumber(map.get("maxValue"));
            String pattern = map.get("pattern") != null ? String.valueOf(map.get("pattern")) : null;
            if (pattern != null && pattern.isEmpty()) {
                pattern = null;
            }
            return new AttributeDefinition(name, typeName, required, allowed, min, max, pattern);
        }

        private static Number asNumber(Object value) {
            if (value instanceof Number) {
                return (Number) value;
            }
            if (value == null) {
                return null;
            }
            try {
                return Double.valueOf(String.valueOf(value));
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        public String getName() {
            return name;
        }

        public String getTypeName() {
            return typeName;
        }

        public boolean isRequired() {
            return required;
        }

        public List<String> getAllowedValues() {
            return allowedValues;
        }

        public Optional<Number> getMinValue() {
            return Optional.ofNullable(minValue);
        }

        public Optional<Number> getMaxValue() {
            return Optional.ofNullable(maxValue);
        }

        public Optional<String> getPattern() {
            return Optional.ofNullable(pattern);
        }
    }

    public static final class Builder {
        private String id;
        private String namespaceUri = "";
        private String localName;
        private String className;
        private boolean reliableSchema;
        private List<String> requiredAttributes = List.of();
        private List<String> optionalAttributes = List.of();
        private List<AttributeDefinition> attributeDefinitions = List.of();
        private List<String> allowedSubElements = List.of();
        private List<String> requiredSubElements = List.of();
        private Optional<Boolean> allowsBody = Optional.empty();
        private Optional<Boolean> canAppearAtRoot = Optional.empty();
        private List<String> requiredParents = List.of();
        private List<QualifiedName> allowedChildNames = List.of();
        private List<QualifiedName> requiredChildNames = List.of();
        private List<QualifiedName> requiredParentNames = List.of();
        private ChildPolicy childPolicy = ChildPolicy.UNKNOWN;
        private ChildPolicyProvenance childPolicyProvenance = ChildPolicyProvenance.MISSING_METADATA;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder namespaceUri(String namespaceUri) {
            this.namespaceUri = namespaceUri;
            return this;
        }

        public Builder localName(String localName) {
            this.localName = localName;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder reliableSchema(boolean reliableSchema) {
            this.reliableSchema = reliableSchema;
            return this;
        }

        public Builder requiredAttributes(List<String> requiredAttributes) {
            this.requiredAttributes = requiredAttributes == null ? List.of() : requiredAttributes;
            return this;
        }

        public Builder optionalAttributes(List<String> optionalAttributes) {
            this.optionalAttributes = optionalAttributes == null ? List.of() : optionalAttributes;
            return this;
        }

        public Builder attributeDefinitions(List<AttributeDefinition> attributeDefinitions) {
            this.attributeDefinitions = attributeDefinitions == null ? List.of() : attributeDefinitions;
            return this;
        }

        public Builder allowedSubElements(List<String> allowedSubElements) {
            this.allowedSubElements = allowedSubElements == null ? List.of() : allowedSubElements;
            return this;
        }

        public Builder requiredSubElements(List<String> requiredSubElements) {
            this.requiredSubElements = requiredSubElements == null ? List.of() : requiredSubElements;
            return this;
        }

        public Builder allowsBody(Boolean allowsBody) {
            this.allowsBody = allowsBody == null ? Optional.empty() : Optional.of(allowsBody);
            return this;
        }

        public Builder canAppearAtRoot(Boolean canAppearAtRoot) {
            this.canAppearAtRoot = canAppearAtRoot == null ? Optional.empty() : Optional.of(canAppearAtRoot);
            return this;
        }

        public Builder requiredParents(List<String> requiredParents) {
            this.requiredParents = requiredParents == null ? List.of() : requiredParents;
            return this;
        }

        public Builder allowedChildNames(List<QualifiedName> names) {
            this.allowedChildNames = names == null ? List.of() : List.copyOf(names);
            return this;
        }

        public Builder requiredChildNames(List<QualifiedName> names) {
            this.requiredChildNames = names == null ? List.of() : List.copyOf(names);
            return this;
        }

        public Builder requiredParentNames(List<QualifiedName> names) {
            this.requiredParentNames = names == null ? List.of() : List.copyOf(names);
            return this;
        }

        public Builder childPolicy(ChildPolicy policy, ChildPolicyProvenance provenance) {
            this.childPolicy = Objects.requireNonNull(policy, "policy");
            this.childPolicyProvenance = Objects.requireNonNull(provenance, "provenance");
            return this;
        }

        public Stage1PluginDescriptor build() {
            return new Stage1PluginDescriptor(this);
        }
    }
}
