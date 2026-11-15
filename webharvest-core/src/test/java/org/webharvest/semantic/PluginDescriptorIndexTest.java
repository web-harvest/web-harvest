package org.webharvest.semantic;

import static org.testng.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;
import org.webharvest.WHConstants;

public class PluginDescriptorIndexTest {

    private static final String NS_A = "http://example.org/ns-a";
    private static final String NS_B = "http://example.org/ns-b";

    @Test
    public void anyChildRequiresExplicitVisualMetadataProvenance() {
        ChildPolicyProvenance[] invalidProvenance = {
                ChildPolicyProvenance.CATALOG_SEMANTICS,
                ChildPolicyProvenance.SCHEMA_ALLOW_LIST,
                ChildPolicyProvenance.LEGACY_BODY_HEURISTIC,
                ChildPolicyProvenance.MISSING_METADATA,
                ChildPolicyProvenance.EXPLICIT_CLOSED_POLICY
        };
        for (ChildPolicyProvenance provenance : invalidProvenance) {
            try {
                Stage1PluginDescriptor.builder()
                        .id("invalid-any@" + NS_A).namespaceUri(NS_A).localName("invalid-any")
                        .className("org.example.InvalidAny")
                        .childPolicy(ChildPolicy.ANY_CHILD, provenance)
                        .build();
                fail("ANY_CHILD must reject provenance " + provenance);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("EXPLICIT_VISUAL_METADATA"));
            }
        }

        Stage1PluginDescriptor valid = Stage1PluginDescriptor.builder()
                .id("valid-any@" + NS_A).namespaceUri(NS_A).localName("valid-any")
                .className("org.example.ValidAny")
                .childPolicy(ChildPolicy.ANY_CHILD, ChildPolicyProvenance.EXPLICIT_VISUAL_METADATA)
                .build();
        assertEquals(valid.getChildPolicy(), ChildPolicy.ANY_CHILD);
    }

    @Test
    public void catalogRootPlacementSurvivesMissingSchemaAndIsValidated() {
        org.webharvest.plugin.metadata.PluginSemantics semantics =
                new org.webharvest.plugin.metadata.PluginSemantics(
                        java.util.Set.of(org.webharvest.plugin.metadata.PluginSemanticRole.LEAF),
                        false, false, false, false, false, List.of(), List.of(), false);
        org.webharvest.plugin.metadata.PluginDescriptor catalog =
                new org.webharvest.plugin.metadata.PluginDescriptor(
                        "leaf@" + NS_A, "leaf", "Leaf", "test", "org.example.Leaf",
                        "test", "test", null, semantics, null, List.of(),
                        java.util.Map.of("namespace", NS_A));

        Stage1PluginDescriptor descriptor = Stage1PluginDescriptor.fromCatalog(catalog);
        assertFalse(descriptor.hasReliableSchema());
        assertEquals(descriptor.getCanAppearAtRoot(), java.util.Optional.of(false));

        SemanticModel model = new SemanticModel(indexOf(descriptor));
        SemanticLoadResult loaded = model.load(
                "<leaf xmlns=\"" + NS_A + "\"/>", new org.webharvest.semantic.SourceOrigin("test:root-placement"));
        assertTrue(loaded.isSuccessful(), loaded.getDiagnostics().toString());
        StructuralValidationResult result = model.validateStructural(loaded.getDocument().get());
        assertTrue(result.getDiagnostics().stream()
                .anyMatch(d -> "DISALLOWED_ROOT".equals(d.getCode())), result.getDiagnostics().toString());
    }

    @Test
    public void catalogProjectionPreservesExplicitAndUnknownChildPolicies() {
        org.webharvest.plugin.metadata.PluginDescriptor httpCatalog =
                org.webharvest.plugin.metadata.PluginMetadataCatalog.getDescriptor("http");
        org.webharvest.plugin.metadata.PluginDescriptor defCatalog =
                org.webharvest.plugin.metadata.PluginMetadataCatalog.getDescriptor("def");
        org.webharvest.plugin.metadata.PluginDescriptor loopCatalog =
                org.webharvest.plugin.metadata.PluginMetadataCatalog.getDescriptor("loop");
        assertNotNull(httpCatalog);
        assertNotNull(defCatalog);
        assertNotNull(loopCatalog);

        Stage1PluginDescriptor http = Stage1PluginDescriptor.fromCatalog(httpCatalog);
        Stage1PluginDescriptor def = Stage1PluginDescriptor.fromCatalog(defCatalog);
        Stage1PluginDescriptor loop = Stage1PluginDescriptor.fromCatalog(loopCatalog);

        assertEquals(http.getChildPolicy(), ChildPolicy.ALLOW_LIST);
        assertTrue(http.getAllowedSubElements().containsAll(List.of("http-header", "http-param")));
        assertTrue(http.getAllowedChildNames().isEmpty(),
                "Catalog local-name relationships must not be promoted to qualified identities");
        assertEquals(def.getChildPolicy(), ChildPolicy.UNKNOWN,
                "Body evaluator inference is not structural authority");
        assertEquals(def.getChildPolicyProvenance(), ChildPolicyProvenance.LEGACY_BODY_HEURISTIC);
        assertEquals(loop.getChildPolicy(), ChildPolicy.ANY_CHILD);
        assertEquals(loop.getChildPolicyProvenance(), ChildPolicyProvenance.EXPLICIT_VISUAL_METADATA);
    }

    @Test
    public void catalogSemanticsArePreservedWithoutLegacySchemaExtension() {
        org.webharvest.plugin.metadata.PluginSemantics semantics =
                new org.webharvest.plugin.metadata.PluginSemantics(
                        java.util.Set.of(org.webharvest.plugin.metadata.PluginSemanticRole.CONTEXT_CONTAINER),
                        true, true, false, true, true, List.of("visual-child"), List.of("outer"), false);
        org.webharvest.plugin.metadata.PluginDescriptor catalog =
                new org.webharvest.plugin.metadata.PluginDescriptor(
                        "container@" + NS_A, "container", "Container", "test", "org.example.Container",
                        "test", "test", null, semantics, null, List.of(), java.util.Map.of("namespace", NS_A));
        Stage1PluginDescriptor descriptor = Stage1PluginDescriptor.fromCatalog(catalog);

        assertFalse(descriptor.hasReliableSchema());
        assertEquals(descriptor.getAllowedSubElements(), List.of("visual-child"));
        assertEquals(descriptor.getChildPolicy(), ChildPolicy.ALLOW_LIST);
        assertEquals(descriptor.getChildPolicyProvenance(), ChildPolicyProvenance.CATALOG_SEMANTICS);
        assertEquals(descriptor.getRequiredParents(), List.of("outer"),
                "Visual/catalog parent relationships must survive even without extensions.schema");
    }

    @Test
    public void runtimeProbeFailureProducesUnknownAvailability() {
        Stage1PluginDescriptor descriptor = descriptor(NS_A, "http", "org.example.Http");
        PluginDescriptorIndex index = PluginDescriptorIndex.builder()
                .addDescriptor(descriptor)
                .namespaceAliases(NamespaceAliasTable.empty())
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(OptionalPluginProfile.empty())
                .runtimeAvailability(localName -> { throw new IllegalStateException("snapshot unavailable"); })
                .build();
        PluginResolution result = index.resolve(NS_A, "http");
        assertEquals(result.getRuntimeAvailabilityStatus(), RuntimeAvailabilityStatus.UNKNOWN);
        assertTrue(result.getDetail().contains("not established"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void legacyRuntimeBooleanRemainsCompatibleButEnumPreservesUnknown() {
        Stage1PluginDescriptor descriptor = descriptor(NS_A, "legacy", "org.example.Legacy");
        PluginResolution legacy = PluginResolution.resolved(descriptor, true);
        assertTrue(legacy.isRuntimeExecutable());
        assertEquals(legacy.getRuntimeAvailabilityStatus(), RuntimeAvailabilityStatus.LEGACY_LOCAL_NAME_REGISTERED);
        PluginResolution unknown = PluginResolution.resolved(descriptor, RuntimeAvailabilityStatus.UNKNOWN);
        assertFalse(unknown.isRuntimeExecutable());
        assertEquals(unknown.getRuntimeAvailabilityStatus(), RuntimeAvailabilityStatus.UNKNOWN);
    }

    @Test
    public void resolvesExactNamespaceAndLocalName() {
        Stage1PluginDescriptor http = descriptor(NS_A, "http", "org.example.Http");
        PluginDescriptorIndex index = indexOf(http);

        PluginResolution resolution = index.resolve(NS_A, "http");
        assertEquals(resolution.getStatus(), PluginResolutionStatus.RESOLVED);
        assertTrue(resolution.getDescriptor().isPresent());
        assertEquals(resolution.getDescriptor().get(), http);
        assertEquals(resolution.getRuntimeAvailabilityStatus(), RuntimeAvailabilityStatus.LEGACY_LOCAL_NAME_REGISTERED);
    }

    @Test
    public void unknownElementWithoutCandidates() {
        PluginDescriptorIndex index = indexOf(descriptor(NS_A, "http", "org.example.Http"));

        PluginResolution resolution = index.resolve(NS_A, "nope");
        assertEquals(resolution.getStatus(), PluginResolutionStatus.UNKNOWN_ELEMENT);
        assertTrue(resolution.getDescriptor().isEmpty());
        assertTrue(resolution.getNamespaceMismatchCandidates().isEmpty());
        assertFalse(resolution.isRuntimeExecutable());
    }

    @Test
    public void wrongNamespaceYieldsUnknownWithMismatchCandidates() {
        Stage1PluginDescriptor inA = descriptor(NS_A, "loop", "org.example.LoopA");
        Stage1PluginDescriptor inB = descriptor(NS_B, "loop", "org.example.LoopB");
        PluginDescriptorIndex index = indexOf(inA, inB);

        PluginResolution resolution = index.resolve("http://example.org/other", "loop");
        assertEquals(resolution.getStatus(), PluginResolutionStatus.UNKNOWN_ELEMENT);
        assertEquals(resolution.getNamespaceMismatchCandidates().size(), 2);
        assertTrue(resolution.getNamespaceMismatchCandidates().contains(inA));
        assertTrue(resolution.getNamespaceMismatchCandidates().contains(inB));
        assertTrue(resolution.getMessage().contains("namespace mismatch"));
    }

    @Test
    public void coreDefaultsCanonicalizeLegacyAliasesAndEmptyNamespace() {
        Stage1PluginDescriptor legacy = descriptor(WHConstants.XMLNS_CORE_10, "file", "org.example.File");
        PluginDescriptorIndex index = PluginDescriptorIndex.builder()
                .addDescriptor(legacy)
                .namespaceAliases(NamespaceAliasTable.coreDefaults())
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(OptionalPluginProfile.empty())
                .runtimeAvailability(PluginDescriptorIndex.RuntimeAvailability.always(true))
                .build();

        String alias = WHConstants.XMLNS_CORE_10_ALIASES.iterator().next();
        assertEquals(index.resolve(alias, "file").getStatus(), PluginResolutionStatus.RESOLVED);
        assertEquals(index.resolve("", "file").getStatus(), PluginResolutionStatus.RESOLVED);
        assertEquals(index.resolve(null, "file").getStatus(), PluginResolutionStatus.RESOLVED);
        assertEquals(
                index.getNamespaceAliases().canonicalize(alias),
                WHConstants.XMLNS_CORE_10);
    }

    @Test
    public void rejectedNonAliasNamespaceDoesNotMapToCore10() {
        NamespaceAliasTable table = NamespaceAliasTable.coreDefaults();
        assertEquals(table.canonicalize(WHConstants.XMLNS_CORE), WHConstants.XMLNS_CORE);
        assertFalse(WHConstants.XMLNS_CORE_10.equals(table.canonicalize(WHConstants.XMLNS_CORE)));

        Stage1PluginDescriptor core10 = descriptor(WHConstants.XMLNS_CORE_10, "http", "org.example.Http10");
        PluginDescriptorIndex index = PluginDescriptorIndex.builder()
                .addDescriptor(core10)
                .namespaceAliases(table)
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(OptionalPluginProfile.empty())
                .runtimeAvailability(PluginDescriptorIndex.RuntimeAvailability.always(true))
                .build();

        PluginResolution as21 = index.resolve(WHConstants.XMLNS_CORE, "http");
        assertEquals(as21.getStatus(), PluginResolutionStatus.UNKNOWN_ELEMENT);
        assertEquals(as21.getNamespaceMismatchCandidates().size(), 1);
        assertEquals(as21.getNamespaceMismatchCandidates().get(0), core10);
    }

    @Test
    public void duplicateDescriptorsAreAmbiguousNeverSilentPick() {
        Stage1PluginDescriptor first = Stage1PluginDescriptor.builder()
                .id("a").namespaceUri(NS_A).localName("dup").className("org.example.A").build();
        Stage1PluginDescriptor second = Stage1PluginDescriptor.builder()
                .id("b").namespaceUri(NS_A).localName("dup").className("org.example.B").build();
        PluginDescriptorIndex index = indexOf(first, second);

        assertEquals(index.ambiguousKeyCount(), 1);
        assertEquals(index.size(), 0);

        PluginResolution resolution = index.resolve(NS_A, "dup");
        assertEquals(resolution.getStatus(), PluginResolutionStatus.AMBIGUOUS);
        assertTrue(resolution.getDescriptor().isEmpty());
        assertFalse(resolution.isRuntimeExecutable());
    }

    @Test
    public void databaseVersusDbAreNotConflated() {
        Stage1PluginDescriptor db = descriptor(WHConstants.XMLNS_CORE, "db",
                "org.webharvest.plugin.db.DatabasePlugin");
        PluginDescriptorIndex index = PluginDescriptorIndex.builder()
                .addDescriptor(db)
                .namespaceAliases(NamespaceAliasTable.coreDefaults())
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(OptionalPluginProfile.empty())
                .runtimeAvailability(PluginDescriptorIndex.RuntimeAvailability.always(true))
                .build();

        assertEquals(index.resolve(WHConstants.XMLNS_CORE, "db").getStatus(),
                PluginResolutionStatus.RESOLVED);
        PluginResolution database = index.resolve(WHConstants.XMLNS_CORE, "database");
        assertEquals(database.getStatus(), PluginResolutionStatus.UNKNOWN_ELEMENT);
        assertTrue(database.getNamespaceMismatchCandidates().isEmpty());
        assertTrue(index.findByLocalName("database").isEmpty());
        assertEquals(index.findByLocalName("db").size(), 1);
    }

    @Test
    public void resolvedButRuntimeUnavailableViaInjectablePredicate() {
        Stage1PluginDescriptor http = descriptor(NS_A, "http", "org.example.Http");
        PluginDescriptorIndex index = PluginDescriptorIndex.builder()
                .addDescriptor(http)
                .namespaceAliases(NamespaceAliasTable.empty())
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(OptionalPluginProfile.empty())
                .runtimeAvailability(PluginDescriptorIndex.RuntimeAvailability.always(false))
                .build();

        PluginResolution resolution = index.resolve(NS_A, "http");
        assertEquals(resolution.getStatus(), PluginResolutionStatus.RESOLVED);
        assertFalse(resolution.isRuntimeExecutable());
    }

    @Test
    public void unavailableOptionalOnlyWhenProfileMatchesAndClassAbsent() {
        OptionalPluginProfile profile = new OptionalPluginProfile(List.of(
                new OptionalPluginProfile.Entry(NS_A, "ghost", "org.webharvest.semantic.DefinitelyMissingPluginXYZ")));
        PluginDescriptorIndex index = PluginDescriptorIndex.builder()
                .descriptors(List.of())
                .namespaceAliases(NamespaceAliasTable.empty())
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(profile)
                .runtimeAvailability(PluginDescriptorIndex.RuntimeAvailability.always(false))
                .build();

        PluginResolution optionalMiss = index.resolve(NS_A, "ghost");
        assertEquals(optionalMiss.getStatus(), PluginResolutionStatus.UNAVAILABLE_OPTIONAL);
        assertTrue(optionalMiss.getMessage().contains("not available"));

        PluginResolution plainUnknown = index.resolve(NS_A, "totally-unknown");
        assertEquals(plainUnknown.getStatus(), PluginResolutionStatus.UNKNOWN_ELEMENT);
    }

    @Test
    public void resolveIsDeterministicAcrossRepeatedCalls() {
        Stage1PluginDescriptor http = descriptor(NS_A, "http", "org.example.Http");
        AtomicInteger probes = new AtomicInteger();
        PluginDescriptorIndex index = PluginDescriptorIndex.builder()
                .addDescriptor(http)
                .namespaceAliases(NamespaceAliasTable.empty())
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(OptionalPluginProfile.empty())
                .runtimeAvailability(localName -> {
                    probes.incrementAndGet();
                    return true;
                })
                .build();

        PluginResolution first = index.resolve(NS_A, "http");
        PluginResolution second = index.resolve(new QualifiedName(NS_A, "http", "x"));
        assertEquals(first.getStatus(), second.getStatus());
        assertEquals(first.getDescriptor(), second.getDescriptor());
        assertEquals(first.isRuntimeExecutable(), second.isRuntimeExecutable());
        assertEquals(first.getMessage(), second.getMessage());
        assertEquals(probes.get(), 2);
    }

    @Test
    public void semanticModelHoldsIndexWithoutStructuralValidation() {
        PluginDescriptorIndex custom = indexOf(descriptor(NS_A, "x", "org.example.X"));
        SemanticModel model = new SemanticModel(custom);
        assertSame(model.getPluginDescriptorIndex(), custom);

        SemanticModel lazy = new SemanticModel();
        PluginDescriptorIndex a = lazy.getPluginDescriptorIndex();
        PluginDescriptorIndex b = lazy.getPluginDescriptorIndex();
        assertSame(a, b);
    }

    @Test
    public void stage1DescriptorPreservesUnknownBodyWhenSchemaMissing() {
        Stage1PluginDescriptor bare = Stage1PluginDescriptor.builder()
                .id("bare")
                .namespaceUri(NS_A)
                .localName("bare")
                .className("org.example.Bare")
                .reliableSchema(false)
                .build();
        assertFalse(bare.hasReliableSchema());
        assertTrue(bare.getAllowsBody().isEmpty());
        assertTrue(bare.getCanAppearAtRoot().isEmpty());
    }

    private static Stage1PluginDescriptor descriptor(String namespaceUri, String localName, String className) {
        return Stage1PluginDescriptor.builder()
                .id(localName + "@" + namespaceUri)
                .namespaceUri(namespaceUri)
                .localName(localName)
                .className(className)
                .reliableSchema(true)
                .allowsBody(true)
                .canAppearAtRoot(true)
                .build();
    }

    private static PluginDescriptorIndex indexOf(Stage1PluginDescriptor... descriptors) {
        return PluginDescriptorIndex.builder()
                .descriptors(List.of(descriptors))
                .namespaceAliases(NamespaceAliasTable.empty())
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(OptionalPluginProfile.empty())
                .runtimeAvailability(PluginDescriptorIndex.RuntimeAvailability.always(true))
                .build();
    }
}
