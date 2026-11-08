package org.webharvest.semantic;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of resolving a document element against {@link PluginDescriptorIndex}.
 *
 * <p>Document path / source location are left to the caller. Message fields are
 * diagnostics-friendly summaries only.
 */
public final class PluginResolution {
    private final PluginResolutionStatus status;
    private final Stage1PluginDescriptor descriptor;
    private final boolean runtimeExecutable;
    private final List<Stage1PluginDescriptor> namespaceMismatchCandidates;
    private final String message;
    private final String detail;

    private PluginResolution(PluginResolutionStatus status,
            Stage1PluginDescriptor descriptor,
            boolean runtimeExecutable,
            List<Stage1PluginDescriptor> namespaceMismatchCandidates,
            String message,
            String detail) {
        this.status = Objects.requireNonNull(status, "status");
        this.descriptor = descriptor;
        this.runtimeExecutable = runtimeExecutable;
        this.namespaceMismatchCandidates = namespaceMismatchCandidates == null
                ? List.of()
                : List.copyOf(namespaceMismatchCandidates);
        this.message = message == null ? "" : message;
        this.detail = detail == null ? "" : detail;
    }

    public static PluginResolution resolved(Stage1PluginDescriptor descriptor, boolean runtimeExecutable) {
        Objects.requireNonNull(descriptor, "descriptor");
        String detail = runtimeExecutable
                ? "runtime registry reports the local name as registered"
                : "descriptor known in catalog but not registered for runtime execution";
        return new PluginResolution(
                PluginResolutionStatus.RESOLVED,
                descriptor,
                runtimeExecutable,
                List.of(),
                "Resolved plugin {" + descriptor.getNamespaceUri() + "}" + descriptor.getLocalName(),
                detail);
    }

    public static PluginResolution unknown(String canonicalNamespaceUri, String localName) {
        return new PluginResolution(
                PluginResolutionStatus.UNKNOWN_ELEMENT,
                null,
                false,
                List.of(),
                "Unknown element {" + nullToEmpty(canonicalNamespaceUri) + "}" + localName,
                "No catalog descriptor for this canonical key");
    }

    public static PluginResolution unknownWithMismatch(String canonicalNamespaceUri, String localName,
            List<Stage1PluginDescriptor> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        StringBuilder detail = new StringBuilder("Local name matches under other namespace(s):");
        for (Stage1PluginDescriptor candidate : candidates) {
            detail.append(' ')
                    .append('{').append(candidate.getNamespaceUri()).append('}')
                    .append(candidate.getLocalName());
        }
        return new PluginResolution(
                PluginResolutionStatus.UNKNOWN_ELEMENT,
                null,
                false,
                candidates,
                "Unknown element {" + nullToEmpty(canonicalNamespaceUri) + "}" + localName
                        + " (namespace mismatch candidates present)",
                detail.toString());
    }

    public static PluginResolution ambiguous(String canonicalNamespaceUri, String localName,
            List<Stage1PluginDescriptor> colliding) {
        Objects.requireNonNull(colliding, "colliding");
        return new PluginResolution(
                PluginResolutionStatus.AMBIGUOUS,
                null,
                false,
                List.of(),
                "Ambiguous element {" + nullToEmpty(canonicalNamespaceUri) + "}" + localName,
                "Catalog contains " + colliding.size() + " descriptors for this canonical key");
    }

    public static PluginResolution unavailableOptional(OptionalPluginProfile.Entry entry) {
        Objects.requireNonNull(entry, "entry");
        return new PluginResolution(
                PluginResolutionStatus.UNAVAILABLE_OPTIONAL,
                null,
                false,
                List.of(),
                "Optional plugin {" + entry.getNamespaceUri() + "}" + entry.getLocalName()
                        + " is not available",
                "Class not present on classpath: " + entry.getClassName());
    }

    public PluginResolutionStatus getStatus() {
        return status;
    }

    public Optional<Stage1PluginDescriptor> getDescriptor() {
        return Optional.ofNullable(descriptor);
    }

    /**
     * Meaningful when {@link #getStatus()} is {@link PluginResolutionStatus#RESOLVED}:
     * whether {@code CorePluginRegistry} (or an injected predicate) reports the
     * local name as registered for runtime dispatch.
     */
    public boolean isRuntimeExecutable() {
        return runtimeExecutable;
    }

    /**
     * Descriptors that share the requested local name under a different namespace.
     * Populated for {@link PluginResolutionStatus#UNKNOWN_ELEMENT} mismatch cases.
     */
    public List<Stage1PluginDescriptor> getNamespaceMismatchCandidates() {
        return namespaceMismatchCandidates;
    }

    public String getMessage() {
        return message;
    }

    public String getDetail() {
        return detail;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
