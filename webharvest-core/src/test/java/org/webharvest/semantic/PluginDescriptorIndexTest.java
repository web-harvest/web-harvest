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
    public void stage1PreservesExplicitAnyChildAndRejectsBodyHeuristicAsAuthority() {
        org.webharvest.plugin.metadata.PluginSemantics explicit =
                new org.webharvest.plugin.metadata.PluginSemantics(
                        java.util.Set.of(org.webharvest.plugin.metadata.PluginSemanticRole.CONTEXT_CONTAINER),
                        true, true, true, true, true, List.of("explicit-child"), List.of(), true);
        org.webharvest.plugin.metadata.PluginSemantics inferred =
                new org.webharvest.plugin.metadata.PluginSemantics(
                        java.util.Set.of(org.webharvest.plugin.metadata.PluginSemanticRole.BODY_EVALUATOR),
                        true, true, true, true, true, List.of(), List.of(), false);

        Stage1PluginDescriptor loop = Stage1PluginDescriptor.fromCatalog(catalogDescriptor("loop", explicit));
        Stage1PluginDescriptor def = Stage1PluginDescriptor.fromCatalog(catalogDescriptor("def", inferred));

        assertEquals(loop.getChildPolicy(), ChildPolicy.ANY_CHILD);
        assertTrue(loop.getAllowedSubElements().contains("explicit-child"),
                "Explicit ANY_CHILD must not discard the catalog's visual allow-list");

        for (String className : List.of(
                "org.webharvest.plugin.core.LoopPlugin",
                "org.webharvest.plugin.core.WhilePlugin",
                "org.webharvest.plugin.core.IfPlugin",
                "org.webharvest.plugin.core.TryPlugin")) {
            Stage1PluginDescriptor container = Stage1PluginDescriptor.fromCatalog(
                    findCatalogDescriptor(className));
            assertEquals(container.getChildPolicy(), ChildPolicy.ANY_CHILD, className);
            assertEquals(container.getChildPolicyProvenance(),
                    ChildPolicyProvenance.EXPLICIT_VISUAL_METADATA, className);
        }
        assertEquals(def.getChildPolicy(), ChildPolicy.UNKNOWN);
        assertEquals(def.getChildPolicyProvenance(), ChildPolicyProvenance.LEGACY_BODY_HEURISTIC);
    }

    @Test
    public void stage1PreservesVisualOnlyAllowListWithoutSchemaExtension() {
        org.webharvest.plugin.metadata.PluginSemantics visualOnly =
                new org.webharvest.plugin.metadata.PluginSemantics(
                        java.util.Set.of(org.webharvest.plugin.metadata.PluginSemanticRole.CONTEXT_CONTAINER),
                        true, true, false, true, true, List.of("http-header"), List.of(), false);

        Stage1PluginDescriptor descriptor = Stage1PluginDescriptor.fromCatalog(
                catalogDescriptor("http", visualOnly));

        assertFalse(descriptor.hasReliableSchema());
        assertEquals(descriptor.getAllowedSubElements(), List.of("http-header"));
        assertEquals(descriptor.getChildPolicy(), ChildPolicy.ALLOW_LIST);
        assertEquals(descriptor.getChildPolicyProvenance(), ChildPolicyProvenance.CATALOG_SEMANTICS);
    }

    private static org.webharvest.plugin.metadata.PluginDescriptor catalogDescriptor(
            String localName, org.webharvest.plugin.metadata.PluginSemantics semantics) {
        return new org.webharvest.plugin.metadata.PluginDescriptor(
                localName + "@" + NS_A, localName, localName, "test", "org.example." + localName,
                "test", "test", null, semantics, null, List.of(), java.util.Map.of("namespace", NS_A));
    }

    @Test
    public void runtimeAvailabilityProbeIsExplicitlyLocalNameOnlyAcrossNamespaces() {
        AtomicInteger probes = new AtomicInteger();
        PluginDescriptorIndex index = PluginDescriptorIndex.builder()
                .addDescriptor(descriptor(NS_A, "shared", "org.example.SharedA"))
                .addDescriptor(descriptor(NS_B, "shared", "org.example.SharedB"))
                .namespaceAliases(NamespaceAliasTable.empty())
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(OptionalPluginProfile.empty())
                .runtimeAvailability(localName -> {
                    assertEquals(localName, "shared");
                    probes.incrementAndGet();
                    return true;
                })
                .build();

        PluginResolution a = index.resolve(NS_A, "shared");
        PluginResolution b = index.resolve(NS_B, "shared");

        assertEquals(a.getRuntimeAvailabilityStatus(), RuntimeAvailabilityStatus.LEGACY_LOCAL_NAME_REGISTERED);
        assertEquals(b.getRuntimeAvailabilityStatus(), RuntimeAvailabilityStatus.LEGACY_LOCAL_NAME_REGISTERED);
        assertEquals(probes.get(), 2);
    }

    @Test
    public void qualifiedNameIdentityIgnoresPrefixButIncludesNamespace() {
        assertEquals(new QualifiedName(NS_A, "http", "a"),
                new QualifiedName(NS_A, "http", "b"));
        assertFalse(new QualifiedName(NS_A, "http", "a").equals(
                new QualifiedName(NS_B, "http", "a")));
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
        assertEquals(resolution.getRuntimeAvailabilityStatus(), RuntimeAvailabilityStatus.UNKNOWN);
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
        assertEquals(resolution.getRuntimeAvailabilityStatus(), RuntimeAvailabilityStatus.UNKNOWN);
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
        assertEquals(resolution.getRuntimeAvailabilityStatus(), RuntimeAvailabilityStatus.NOT_REGISTERED_IN_CORE_REGISTRY);
        assertTrue(resolution.getDetail().contains("current registry snapshot"));
        assertFalse(resolution.getDetail().contains("qualified executability"));
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
        assertEquals(first.getRuntimeAvailabilityStatus(), second.getRuntimeAvailabilityStatus());
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

    @Test
    public void realCatalogCounterexamplesDoNotPromoteBodyHeuristicToAnyChild() {
        org.webharvest.plugin.metadata.PluginDescriptor headerCatalog =
                findCatalogDescriptor("org.webharvest.plugin.core.HttpHeaderPlugin");
        org.webharvest.plugin.metadata.PluginDescriptor regexpCatalog =
                findCatalogDescriptor("org.webharvest.plugin.core.RegexpPatternPlugin");
        org.webharvest.plugin.metadata.PluginDescriptor httpCatalog =
                findCatalogDescriptor("org.webharvest.plugin.core.HttpPlugin");
        Stage1PluginDescriptor header = Stage1PluginDescriptor.fromCatalog(headerCatalog);
        Stage1PluginDescriptor regexp = Stage1PluginDescriptor.fromCatalog(regexpCatalog);
        Stage1PluginDescriptor http = Stage1PluginDescriptor.fromCatalog(httpCatalog);

        assertEquals(header.getChildPolicy(), ChildPolicy.UNKNOWN);
        assertEquals(header.getChildPolicyProvenance(), ChildPolicyProvenance.LEGACY_BODY_HEURISTIC);
        assertEquals(regexp.getChildPolicy(), ChildPolicy.UNKNOWN);
        assertEquals(regexp.getChildPolicyProvenance(), ChildPolicyProvenance.LEGACY_BODY_HEURISTIC);

        assertEquals(http.getChildPolicy(), ChildPolicy.ALLOW_LIST);
        assertTrue(http.getAllowedSubElements().contains("http-header"));
        assertTrue(http.getAllowedSubElements().contains("http-param"));
        // The current catalog projection is local-name-only; do not invent QNames.
        assertTrue(http.getAllowedChildNames().isEmpty(),
                "Catalog local-name allow-lists are not qualified child identities");

        for (String className : List.of(
                "org.webharvest.plugin.core.LoopPlugin",
                "org.webharvest.plugin.core.WhilePlugin",
                "org.webharvest.plugin.core.IfPlugin",
                "org.webharvest.plugin.core.TryPlugin")) {
            Stage1PluginDescriptor container = Stage1PluginDescriptor.fromCatalog(
                    findCatalogDescriptor(className));
            assertEquals(container.getChildPolicy(), ChildPolicy.ANY_CHILD, className);
            assertEquals(container.getChildPolicyProvenance(),
                    ChildPolicyProvenance.EXPLICIT_VISUAL_METADATA, className);
        }
    }

    private static org.webharvest.plugin.metadata.PluginDescriptor findCatalogDescriptor(String className) {
        return org.webharvest.plugin.metadata.PluginMetadataCatalog.getAllDescriptors().stream()
                .filter(descriptor -> className.equals(descriptor.getClassName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing catalog descriptor for " + className));
    }


    @Test
    public void runtimeAvailabilityProbeFailureProducesUnknownStatus() {
        Stage1PluginDescriptor plugin = descriptor(NS_A, "http", "org.example.Http");
        PluginDescriptorIndex index = PluginDescriptorIndex.builder()
                .addDescriptor(plugin)
                .namespaceAliases(NamespaceAliasTable.empty())
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(OptionalPluginProfile.empty())
                .runtimeAvailability(localName -> {
                    throw new IllegalStateException("registry snapshot unavailable");
                })
                .build();

        PluginResolution resolution = index.resolve(NS_A, "http");

        assertEquals(resolution.getStatus(), PluginResolutionStatus.RESOLVED);
        assertEquals(resolution.getRuntimeAvailabilityStatus(), RuntimeAvailabilityStatus.UNKNOWN);
        assertTrue(resolution.getDetail().contains("runtime availability was not established"));
    }

}
