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
    private final RuntimeAvailabilityStatus runtimeAvailabilityStatus;
    private final List<Stage1PluginDescriptor> namespaceMismatchCandidates;
    private final String message;
    private final String detail;

    private PluginResolution(PluginResolutionStatus status,
            Stage1PluginDescriptor descriptor,
            RuntimeAvailabilityStatus runtimeAvailabilityStatus,
            List<Stage1PluginDescriptor> namespaceMismatchCandidates,
            String message,
            String detail) {
        this.status = Objects.requireNonNull(status, "status");
        this.descriptor = descriptor;
        this.runtimeAvailabilityStatus = Objects.requireNonNull(runtimeAvailabilityStatus, "runtimeAvailabilityStatus");
        this.namespaceMismatchCandidates = namespaceMismatchCandidates == null
                ? List.of()
                : List.copyOf(namespaceMismatchCandidates);
        this.message = message == null ? "" : message;
        this.detail = detail == null ? "" : detail;
    }

    /**
     * Compatibility bridge for consumers compiled against the original boolean factory.
     * A false value means the legacy local-name registry probe did not positively
     * establish registration; use the enum overload when UNKNOWN must be distinguished.
     *
     * @deprecated use {@link #resolved(Stage1PluginDescriptor, RuntimeAvailabilityStatus)}
     */
    @Deprecated
    public static PluginResolution resolved(Stage1PluginDescriptor descriptor, boolean runtimeExecutable) {
        return resolved(descriptor, runtimeExecutable
                ? RuntimeAvailabilityStatus.LEGACY_LOCAL_NAME_REGISTERED
                : RuntimeAvailabilityStatus.NOT_REGISTERED_IN_CORE_REGISTRY);
    }

    public static PluginResolution resolved(Stage1PluginDescriptor descriptor,
            RuntimeAvailabilityStatus runtimeAvailabilityStatus) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(runtimeAvailabilityStatus, "runtimeAvailabilityStatus");
        String detail;
        switch (runtimeAvailabilityStatus) {
            case LEGACY_LOCAL_NAME_REGISTERED:
                detail = "CorePluginRegistry contains this local name; namespace is not part of the probe";
                break;
            case NOT_REGISTERED_IN_CORE_REGISTRY:
                detail = "CorePluginRegistry does not contain this local name in the current registry snapshot";
                break;
            default:
                detail = "runtime availability was not established";
                break;
        }
        return new PluginResolution(PluginResolutionStatus.RESOLVED, descriptor, runtimeAvailabilityStatus,
                List.of(), "Resolved plugin {" + descriptor.getNamespaceUri() + "}" + descriptor.getLocalName(), detail);
    }

    public static PluginResolution unknown(String canonicalNamespaceUri, String localName) {
        return new PluginResolution(
                PluginResolutionStatus.UNKNOWN_ELEMENT,
                null,
                RuntimeAvailabilityStatus.UNKNOWN,
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
                RuntimeAvailabilityStatus.UNKNOWN,
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
                RuntimeAvailabilityStatus.UNKNOWN,
                List.of(),
                "Ambiguous element {" + nullToEmpty(canonicalNamespaceUri) + "}" + localName,
                "Catalog contains " + colliding.size() + " descriptors for this canonical key");
    }

    public static PluginResolution unavailableOptional(OptionalPluginProfile.Entry entry) {
        Objects.requireNonNull(entry, "entry");
        return new PluginResolution(
                PluginResolutionStatus.UNAVAILABLE_OPTIONAL,
                null,
                RuntimeAvailabilityStatus.UNKNOWN,
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
     * Scoped availability signal. It describes the legacy local-name registry probe,
     * not namespace-qualified executability or guaranteed successful execution.
     */
    public RuntimeAvailabilityStatus getRuntimeAvailabilityStatus() {
        return runtimeAvailabilityStatus;
    }

    /**
     * Compatibility bridge for the former boolean runtime probe.
     * Returns true only when the legacy local-name registry probe positively
     * reports registration. False also covers UNKNOWN; use
     * {@link #getRuntimeAvailabilityStatus()} to distinguish those cases.
     *
     * @deprecated use {@link #getRuntimeAvailabilityStatus()}
     */
    @Deprecated
    public boolean isRuntimeExecutable() {
        return runtimeAvailabilityStatus == RuntimeAvailabilityStatus.LEGACY_LOCAL_NAME_REGISTERED;
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
