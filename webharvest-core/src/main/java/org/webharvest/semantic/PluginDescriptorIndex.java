package org.webharvest.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.webharvest.definition.CorePluginRegistry;
import org.webharvest.plugin.metadata.PluginDescriptor;
import org.webharvest.plugin.metadata.PluginMetadataCatalog;

/**
 * Read-only index of Stage-1 plugin descriptors keyed by canonical
 * {@code (namespaceUri, localName)}.
 *
 * <p>Primary key collisions are stored as {@link PluginResolutionStatus#AMBIGUOUS}
 * and never resolved by silent pick. Runtime availability is separate from catalog
 * presence and reports only the namespace-unaware local-name registry probe.
 *
 * <p>Resolution algorithm (deterministic):
 * <ol>
 *   <li>Apply {@link ElementAliasTable} to the local name (default: none).</li>
 *   <li>Canonicalize namespace via {@link NamespaceAliasTable}.</li>
 *   <li>If the ambiguous multi-map has the key → {@code AMBIGUOUS}.</li>
 *   <li>If a unique descriptor exists → {@code RESOLVED} (+ runtimeExecutable).</li>
 *   <li>Else if {@link OptionalPluginProfile} matches and {@code Class.forName} fails
 *       → {@code UNAVAILABLE_OPTIONAL}.</li>
 *   <li>Else if the same local name exists under other namespace(s) →
 *       {@code UNKNOWN_ELEMENT} with mismatch candidates.</li>
 *   <li>Else {@code UNKNOWN_ELEMENT}.</li>
 * </ol>
 */
public final class PluginDescriptorIndex {
    private final Map<IndexKey, Stage1PluginDescriptor> unique;
    private final Map<IndexKey, List<Stage1PluginDescriptor>> ambiguous;
    private final Map<String, List<Stage1PluginDescriptor>> byLocalName;
    private final NamespaceAliasTable namespaceAliases;
    private final ElementAliasTable elementAliases;
    private final OptionalPluginProfile optionalProfile;
    private final RuntimeAvailability runtimeAvailability;

    private PluginDescriptorIndex(Map<IndexKey, Stage1PluginDescriptor> unique,
            Map<IndexKey, List<Stage1PluginDescriptor>> ambiguous,
            Map<String, List<Stage1PluginDescriptor>> byLocalName,
            NamespaceAliasTable namespaceAliases,
            ElementAliasTable elementAliases,
            OptionalPluginProfile optionalProfile,
            RuntimeAvailability runtimeAvailability) {
        this.unique = unique;
        this.ambiguous = ambiguous;
        this.byLocalName = byLocalName;
        this.namespaceAliases = namespaceAliases;
        this.elementAliases = elementAliases;
        this.optionalProfile = optionalProfile;
        this.runtimeAvailability = runtimeAvailability;
    }

    /**
     * Builds an index from {@link PluginMetadataCatalog#getAllDescriptors()} using
     * core namespace defaults, an empty element-alias table, known optional extension
     * profile entries, and {@link CorePluginRegistry} for runtime availability.
     */
    public static PluginDescriptorIndex fromCatalog() {
        return builder()
                .descriptorsFromCatalog(PluginMetadataCatalog.getAllDescriptors())
                .namespaceAliases(NamespaceAliasTable.coreDefaults())
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(OptionalPluginProfile.knownExtensions())
                .runtimeAvailability(RuntimeAvailability.corePluginRegistry())
                .build();
    }

    /**
     * Test / explicit hook: index a prepared Stage-1 descriptor list with core
     * defaults for aliases and optional profile, and an injectable runtime predicate.
     */
    public static PluginDescriptorIndex fromDescriptors(List<Stage1PluginDescriptor> descriptors,
            RuntimeAvailability runtimeAvailability) {
        return builder()
                .descriptors(descriptors)
                .namespaceAliases(NamespaceAliasTable.coreDefaults())
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(OptionalPluginProfile.knownExtensions())
                .runtimeAvailability(runtimeAvailability)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public PluginResolution resolve(QualifiedName name) {
        Objects.requireNonNull(name, "name");
        return resolve(name.getNamespaceUri(), name.getLocalName());
    }

    public PluginResolution resolve(String namespaceUri, String localName) {
        Objects.requireNonNull(localName, "localName");
        String aliasedLocal = elementAliases.canonicalize(localName);
        String canonicalNs = namespaceAliases.canonicalize(namespaceUri);
        IndexKey key = new IndexKey(canonicalNs, aliasedLocal);

        List<Stage1PluginDescriptor> colliding = ambiguous.get(key);
        if (colliding != null && !colliding.isEmpty()) {
            return PluginResolution.ambiguous(canonicalNs, aliasedLocal, colliding);
        }

        Stage1PluginDescriptor uniqueHit = unique.get(key);
        if (uniqueHit != null) {
            RuntimeAvailabilityStatus availability;
            try {
                availability = runtimeAvailability.isRegistered(aliasedLocal)
                        ? RuntimeAvailabilityStatus.LEGACY_LOCAL_NAME_REGISTERED
                        : RuntimeAvailabilityStatus.NOT_REGISTERED_IN_CORE_REGISTRY;
            } catch (RuntimeException ex) {
                availability = RuntimeAvailabilityStatus.UNKNOWN;
            }
            return PluginResolution.resolved(uniqueHit, availability);
        }

        Optional<OptionalPluginProfile.Entry> profileHit = optionalProfile.find(canonicalNs, aliasedLocal);
        if (profileHit.isPresent() && !profileHit.get().isClassPresent()) {
            return PluginResolution.unavailableOptional(profileHit.get());
        }

        List<Stage1PluginDescriptor> sameLocal = findByLocalName(aliasedLocal);
        if (!sameLocal.isEmpty()) {
            List<Stage1PluginDescriptor> mismatches = new ArrayList<>();
            for (Stage1PluginDescriptor candidate : sameLocal) {
                String candidateNs = namespaceAliases.canonicalize(candidate.getNamespaceUri());
                if (!candidateNs.equals(canonicalNs)) {
                    mismatches.add(candidate);
                }
            }
            if (!mismatches.isEmpty()) {
                return PluginResolution.unknownWithMismatch(canonicalNs, aliasedLocal, mismatches);
            }
        }

        return PluginResolution.unknown(canonicalNs, aliasedLocal);
    }

    /**
     * All indexed descriptors with the given local name (after element-alias
     * canonicalization of the query). Used for namespace-mismatch diagnostics.
     */
    public List<Stage1PluginDescriptor> findByLocalName(String localName) {
        Objects.requireNonNull(localName, "localName");
        String aliasedLocal = elementAliases.canonicalize(localName);
        List<Stage1PluginDescriptor> list = byLocalName.get(aliasedLocal);
        return list == null ? List.of() : list;
    }

    public int size() {
        return unique.size();
    }

    public int ambiguousKeyCount() {
        return ambiguous.size();
    }

    public NamespaceAliasTable getNamespaceAliases() {
        return namespaceAliases;
    }

    public ElementAliasTable getElementAliases() {
        return elementAliases;
    }

    public OptionalPluginProfile getOptionalProfile() {
        return optionalProfile;
    }

    /**
     * Predicate for runtime dispatch availability (local-name only, matching
     * {@link CorePluginRegistry#isPluginRegistered(String)}).
     */
    @FunctionalInterface
    public interface RuntimeAvailability {
        boolean isRegistered(String localName);

        static RuntimeAvailability corePluginRegistry() {
            return localName -> CorePluginRegistry.getInstance().isPluginRegistered(localName);
        }

        static RuntimeAvailability always(boolean value) {
            return localName -> value;
        }
    }

    private static final class IndexKey {
        private final String namespaceUri;
        private final String localName;

        IndexKey(String namespaceUri, String localName) {
            this.namespaceUri = namespaceUri;
            this.localName = localName;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof IndexKey)) {
                return false;
            }
            IndexKey that = (IndexKey) other;
            return namespaceUri.equals(that.namespaceUri) && localName.equals(that.localName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namespaceUri, localName);
        }
    }

    public static final class Builder {
        private final List<Stage1PluginDescriptor> descriptors = new ArrayList<>();
        private NamespaceAliasTable namespaceAliases = NamespaceAliasTable.coreDefaults();
        private ElementAliasTable elementAliases = ElementAliasTable.empty();
        private OptionalPluginProfile optionalProfile = OptionalPluginProfile.knownExtensions();
        private RuntimeAvailability runtimeAvailability = RuntimeAvailability.corePluginRegistry();

        private Builder() {
        }

        public Builder descriptors(List<Stage1PluginDescriptor> descriptors) {
            Objects.requireNonNull(descriptors, "descriptors");
            this.descriptors.clear();
            this.descriptors.addAll(descriptors);
            return this;
        }

        public Builder addDescriptor(Stage1PluginDescriptor descriptor) {
            this.descriptors.add(Objects.requireNonNull(descriptor, "descriptor"));
            return this;
        }

        public Builder descriptorsFromCatalog(List<PluginDescriptor> catalogDescriptors) {
            Objects.requireNonNull(catalogDescriptors, "catalogDescriptors");
            this.descriptors.clear();
            for (PluginDescriptor catalog : catalogDescriptors) {
                this.descriptors.add(Stage1PluginDescriptor.fromCatalog(catalog));
            }
            return this;
        }

        public Builder namespaceAliases(NamespaceAliasTable namespaceAliases) {
            this.namespaceAliases = Objects.requireNonNull(namespaceAliases, "namespaceAliases");
            return this;
        }

        public Builder elementAliases(ElementAliasTable elementAliases) {
            this.elementAliases = Objects.requireNonNull(elementAliases, "elementAliases");
            return this;
        }

        public Builder optionalProfile(OptionalPluginProfile optionalProfile) {
            this.optionalProfile = Objects.requireNonNull(optionalProfile, "optionalProfile");
            return this;
        }

        public Builder runtimeAvailability(RuntimeAvailability runtimeAvailability) {
            this.runtimeAvailability = Objects.requireNonNull(runtimeAvailability, "runtimeAvailability");
            return this;
        }

        public PluginDescriptorIndex build() {
            Map<IndexKey, Stage1PluginDescriptor> unique = new LinkedHashMap<>();
            Map<IndexKey, List<Stage1PluginDescriptor>> ambiguous = new LinkedHashMap<>();
            Map<String, List<Stage1PluginDescriptor>> byLocalName = new LinkedHashMap<>();

            for (Stage1PluginDescriptor descriptor : descriptors) {
                String canonicalNs = namespaceAliases.canonicalize(descriptor.getNamespaceUri());
                String canonicalLocal = elementAliases.canonicalize(descriptor.getLocalName());
                IndexKey key = new IndexKey(canonicalNs, canonicalLocal);

                List<Stage1PluginDescriptor> existingAmbiguous = ambiguous.get(key);
                if (existingAmbiguous != null) {
                    existingAmbiguous.add(descriptor);
                } else if (unique.containsKey(key)) {
                    List<Stage1PluginDescriptor> group = new ArrayList<>();
                    group.add(unique.remove(key));
                    group.add(descriptor);
                    ambiguous.put(key, group);
                } else {
                    unique.put(key, descriptor);
                }

                byLocalName
                        .computeIfAbsent(canonicalLocal, ignored -> new ArrayList<>())
                        .add(descriptor);
            }

            Map<IndexKey, List<Stage1PluginDescriptor>> frozenAmbiguous = new LinkedHashMap<>();
            for (Map.Entry<IndexKey, List<Stage1PluginDescriptor>> entry : ambiguous.entrySet()) {
                frozenAmbiguous.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            Map<String, List<Stage1PluginDescriptor>> frozenByLocal = new LinkedHashMap<>();
            for (Map.Entry<String, List<Stage1PluginDescriptor>> entry : byLocalName.entrySet()) {
                frozenByLocal.put(entry.getKey(), List.copyOf(entry.getValue()));
            }

            return new PluginDescriptorIndex(
                    Collections.unmodifiableMap(unique),
                    Collections.unmodifiableMap(frozenAmbiguous),
                    Collections.unmodifiableMap(frozenByLocal),
                    namespaceAliases,
                    elementAliases,
                    optionalProfile,
                    runtimeAvailability);
        }
    }
}
