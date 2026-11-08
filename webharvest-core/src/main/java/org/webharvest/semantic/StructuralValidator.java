package org.webharvest.semantic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.webharvest.semantic.Stage1PluginDescriptor.AttributeDefinition;

/**
 * Package-private structural validation walk over a {@link SemanticDocument}.
 *
 * <p>Deterministic: document-content BFS order, then fixed per-element check order
 * (resolution → runtime → metadata limitation → root/parent → attributes → children → text).
 */
final class StructuralValidator {

    static final String UNKNOWN_ELEMENT = "UNKNOWN_ELEMENT";
    static final String AMBIGUOUS_ELEMENT = "AMBIGUOUS_ELEMENT";
    static final String UNAVAILABLE_OPTIONAL = "UNAVAILABLE_OPTIONAL";
    static final String NAMESPACE_MISMATCH = "NAMESPACE_MISMATCH";
    static final String RUNTIME_UNAVAILABLE = "RUNTIME_UNAVAILABLE";
    static final String REQUIRED_ATTRIBUTE_MISSING = "REQUIRED_ATTRIBUTE_MISSING";
    static final String UNKNOWN_ATTRIBUTE = "UNKNOWN_ATTRIBUTE";
    static final String INVALID_ATTRIBUTE_VALUE = "INVALID_ATTRIBUTE_VALUE";
    static final String DISALLOWED_CHILD = "DISALLOWED_CHILD";
    static final String REQUIRED_CHILD_MISSING = "REQUIRED_CHILD_MISSING";
    static final String INVALID_PARENT = "INVALID_PARENT";
    static final String DISALLOWED_ROOT = "DISALLOWED_ROOT";
    static final String DISALLOWED_TEXT = "DISALLOWED_TEXT";
    static final String METADATA_LIMITATION = "METADATA_LIMITATION";

    private StructuralValidator() {
    }

    static StructuralValidationResult validate(SemanticDocument document, PluginDescriptorIndex index) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(index, "index");
        List<SemanticDiagnostic> diagnostics = new ArrayList<>();
        Deque<Frame> pending = new ArrayDeque<>();
        for (SemanticContent item : document.getContent()) {
            if (item instanceof SemanticElement) {
                pending.addLast(new Frame((SemanticElement) item, null));
            }
        }
        while (!pending.isEmpty()) {
            Frame frame = pending.removeFirst();
            validateElement(frame.element, frame.parent, document.getRoot(), index, diagnostics);
            for (SemanticContent child : frame.element.getContent()) {
                if (child instanceof SemanticElement) {
                    pending.addLast(new Frame((SemanticElement) child, frame.element));
                }
            }
        }
        return new StructuralValidationResult(document, diagnostics);
    }

    private static void validateElement(SemanticElement element,
            SemanticElement parent,
            SemanticElement documentRoot,
            PluginDescriptorIndex index,
            List<SemanticDiagnostic> diagnostics) {
        PluginResolution resolution = index.resolve(element.getName());
        emitResolutionDiagnostics(element, resolution, diagnostics);

        if (resolution.getStatus() == PluginResolutionStatus.RESOLVED
                && !resolution.isRuntimeExecutable()) {
            diagnostics.add(warning(RUNTIME_UNAVAILABLE,
                    describe(element) + ": plugin is recognized but not registered for runtime execution",
                    element.getLocation()));
        }

        Optional<Stage1PluginDescriptor> descriptorOpt = resolution.getDescriptor();
        if (descriptorOpt.isEmpty()) {
            return;
        }
        Stage1PluginDescriptor descriptor = descriptorOpt.get();
        if (!descriptor.hasReliableSchema()) {
            diagnostics.add(warning(METADATA_LIMITATION,
                    describe(element) + ": reliable schema metadata is absent; "
                            + "structural attribute/child/body/root/parent rules were not applied",
                    element.getLocation()));
            return;
        }

        boolean isRoot = element == documentRoot;
        validateRootAndParent(element, parent, isRoot, descriptor, diagnostics);
        validateAttributes(element, descriptor, diagnostics);
        validateChildren(element, descriptor, diagnostics);
        validateText(element, descriptor, diagnostics);
    }

    private static void emitResolutionDiagnostics(SemanticElement element,
            PluginResolution resolution,
            List<SemanticDiagnostic> diagnostics) {
        switch (resolution.getStatus()) {
            case UNKNOWN_ELEMENT: {
                StringBuilder message = new StringBuilder(describe(element))
                        .append(": ").append(resolution.getMessage());
                List<Stage1PluginDescriptor> mismatches = resolution.getNamespaceMismatchCandidates();
                if (!mismatches.isEmpty()) {
                    message.append("; namespace mismatch candidates:");
                    for (Stage1PluginDescriptor candidate : mismatches) {
                        message.append(' ')
                                .append('{').append(candidate.getNamespaceUri()).append('}')
                                .append(candidate.getLocalName());
                    }
                    diagnostics.add(error(UNKNOWN_ELEMENT, message.toString(), element.getLocation()));
                    diagnostics.add(warning(NAMESPACE_MISMATCH,
                            describe(element) + ": local name exists under other namespace(s): "
                                    + candidateNsList(mismatches),
                            element.getLocation()));
                } else {
                    diagnostics.add(error(UNKNOWN_ELEMENT, message.toString(), element.getLocation()));
                }
                break;
            }
            case AMBIGUOUS:
                diagnostics.add(error(AMBIGUOUS_ELEMENT,
                        describe(element) + ": " + resolution.getMessage(),
                        element.getLocation()));
                break;
            case UNAVAILABLE_OPTIONAL:
                diagnostics.add(error(UNAVAILABLE_OPTIONAL,
                        describe(element) + ": " + resolution.getMessage(),
                        element.getLocation()));
                break;
            case RESOLVED:
                break;
            default:
                break;
        }
    }

    private static void validateRootAndParent(SemanticElement element,
            SemanticElement parent,
            boolean isRoot,
            Stage1PluginDescriptor descriptor,
            List<SemanticDiagnostic> diagnostics) {
        if (isRoot) {
            Optional<Boolean> canAppearAtRoot = descriptor.getCanAppearAtRoot();
            if (canAppearAtRoot.isPresent() && !canAppearAtRoot.get()) {
                diagnostics.add(error(DISALLOWED_ROOT,
                        describe(element) + ": element must not appear as document root",
                        element.getLocation()));
            }
        }

        List<String> requiredParents = descriptor.getRequiredParents();
        if (!requiredParents.isEmpty()) {
            if (parent == null) {
                diagnostics.add(error(INVALID_PARENT,
                        describe(element) + ": requires parent in " + requiredParents
                                + " but element is at document root (no parent)",
                        element.getLocation()));
            } else {
                String parentLocal = parent.getName().getLocalName();
                if (!requiredParents.contains(parentLocal)) {
                    diagnostics.add(error(INVALID_PARENT,
                            describe(element) + ": parent local name '" + parentLocal
                                    + "' is not in requiredParents " + requiredParents,
                            element.getLocation()));
                }
            }
        }
    }

    private static void validateAttributes(SemanticElement element,
            Stage1PluginDescriptor descriptor,
            List<SemanticDiagnostic> diagnostics) {
        Map<String, SemanticAttribute> byLocalName = new HashMap<>();
        for (SemanticAttribute attribute : element.getAttributes()) {
            if (isXmlnsAttribute(attribute)) {
                continue;
            }
            byLocalName.putIfAbsent(attribute.getName().getLocalName(), attribute);
        }

        for (String required : descriptor.getRequiredAttributes()) {
            if (!byLocalName.containsKey(required)) {
                diagnostics.add(error(REQUIRED_ATTRIBUTE_MISSING,
                        describe(element) + ": required attribute '" + required + "' is missing",
                        element.getLocation()));
            }
        }

        Optional<Set<String>> knownNames = closedAttributeNameSet(descriptor);
        if (knownNames.isEmpty()) {
            diagnostics.add(warning(METADATA_LIMITATION,
                    describe(element) + ": no closed attribute name set in schema "
                            + "(attributeDefinitions / required / optional); unknown-attribute check skipped",
                    element.getLocation()));
        }

        Map<String, AttributeDefinition> definitionsByName = new HashMap<>();
        for (AttributeDefinition definition : descriptor.getAttributeDefinitions()) {
            definitionsByName.put(definition.getName(), definition);
        }

        for (SemanticAttribute attribute : element.getAttributes()) {
            if (isXmlnsAttribute(attribute)) {
                continue;
            }
            String localName = attribute.getName().getLocalName();
            if (knownNames.isPresent() && !knownNames.get().contains(localName)) {
                diagnostics.add(error(UNKNOWN_ATTRIBUTE,
                        describe(element) + ": unknown attribute '" + localName + "'",
                        element.getLocation()));
                continue;
            }
            AttributeDefinition definition = definitionsByName.get(localName);
            if (definition != null) {
                Optional<String> invalidReason = attributeValueInvalidReason(definition, attribute.getValue());
                if (invalidReason.isPresent()) {
                    diagnostics.add(error(INVALID_ATTRIBUTE_VALUE,
                            describe(element) + ": attribute '" + localName + "' "
                                    + invalidReason.get(),
                            element.getLocation()));
                }
            }
        }
    }

    private static Optional<Set<String>> closedAttributeNameSet(Stage1PluginDescriptor descriptor) {
        if (!descriptor.getAttributeDefinitions().isEmpty()) {
            LinkedHashSet<String> names = new LinkedHashSet<>();
            for (AttributeDefinition definition : descriptor.getAttributeDefinitions()) {
                names.add(definition.getName());
            }
            return Optional.of(names);
        }
        if (!descriptor.getRequiredAttributes().isEmpty()
                || !descriptor.getOptionalAttributes().isEmpty()) {
            LinkedHashSet<String> names = new LinkedHashSet<>();
            names.addAll(descriptor.getRequiredAttributes());
            names.addAll(descriptor.getOptionalAttributes());
            return Optional.of(names);
        }
        return Optional.empty();
    }

    private static Optional<String> attributeValueInvalidReason(AttributeDefinition definition, String value) {
        if (!definition.getAllowedValues().isEmpty() && !definition.getAllowedValues().contains(value)) {
            return Optional.of("value '" + value + "' is not in allowedValues " + definition.getAllowedValues());
        }
        Optional<String> pattern = definition.getPattern();
        if (pattern.isPresent()) {
            try {
                if (!Pattern.compile(pattern.get()).matcher(value).matches()) {
                    return Optional.of("value '" + value + "' does not match pattern '" + pattern.get() + "'");
                }
            } catch (PatternSyntaxException ex) {
                return Optional.of("declared pattern '" + pattern.get() + "' is invalid: " + ex.getDescription());
            }
        }

        String typeName = definition.getTypeName() == null
                ? "STRING"
                : definition.getTypeName().trim().toUpperCase(Locale.ROOT);
        if (!"STRING".equals(typeName) && !"ENUM".equals(typeName)
                && !"URL".equals(typeName) && !"FILE_PATH".equals(typeName)
                && !"REGEX".equals(typeName)) {
            Optional<String> typeReason = validateTypedValue(typeName, value);
            if (typeReason.isPresent()) {
                return typeReason;
            }
        }

        if (definition.getMinValue().isPresent() || definition.getMaxValue().isPresent()) {
            try {
                double number = Double.parseDouble(value.trim());
                if (definition.getMinValue().isPresent()
                        && number < definition.getMinValue().get().doubleValue()) {
                    return Optional.of("value '" + value + "' is below minValue "
                            + definition.getMinValue().get());
                }
                if (definition.getMaxValue().isPresent()
                        && number > definition.getMaxValue().get().doubleValue()) {
                    return Optional.of("value '" + value + "' is above maxValue "
                            + definition.getMaxValue().get());
                }
            } catch (NumberFormatException ex) {
                return Optional.of("value '" + value + "' is not numeric for min/max check");
            }
        }
        return Optional.empty();
    }

    private static Optional<String> validateTypedValue(String typeName, String value) {
        switch (typeName) {
            case "INTEGER":
                try {
                    Integer.parseInt(value.trim());
                    return Optional.empty();
                } catch (NumberFormatException ex) {
                    return Optional.of("value '" + value + "' is not a valid INTEGER");
                }
            case "LONG":
                try {
                    Long.parseLong(value.trim());
                    return Optional.empty();
                } catch (NumberFormatException ex) {
                    return Optional.of("value '" + value + "' is not a valid LONG");
                }
            case "DOUBLE":
                try {
                    Double.parseDouble(value.trim());
                    return Optional.empty();
                } catch (NumberFormatException ex) {
                    return Optional.of("value '" + value + "' is not a valid DOUBLE");
                }
            case "BOOLEAN":
                if ("true".equalsIgnoreCase(value.trim()) || "false".equalsIgnoreCase(value.trim())) {
                    return Optional.empty();
                }
                return Optional.of("value '" + value + "' is not a valid BOOLEAN");
            default:
                return Optional.empty();
        }
    }

    private static void validateChildren(SemanticElement element,
            Stage1PluginDescriptor descriptor,
            List<SemanticDiagnostic> diagnostics) {
        List<SemanticElement> childElements = new ArrayList<>();
        for (SemanticContent content : element.getContent()) {
            if (content instanceof SemanticElement) {
                childElements.add((SemanticElement) content);
            }
        }

        List<String> allowed = descriptor.getAllowedSubElements();
        if (allowed.isEmpty()) {
            // acceptsAnyChild is not exposed on Stage1PluginDescriptor — documented limitation.
            diagnostics.add(warning(METADATA_LIMITATION,
                    describe(element) + ": allowedSubElements is empty; child-allow policy unknown "
                            + "(acceptsAnyChild not exposed on Stage1PluginDescriptor); "
                            + "DISALLOWED_CHILD checks were not applied",
                    element.getLocation()));
        } else {
            Set<String> allowedSet = new HashSet<>(allowed);
            for (SemanticElement child : childElements) {
                String childLocal = child.getName().getLocalName();
                if (!allowedSet.contains(childLocal)) {
                    diagnostics.add(error(DISALLOWED_CHILD,
                            describe(element) + ": child '" + childLocal
                                    + "' (" + describe(child) + ") is not in allowedSubElements "
                                    + allowed,
                            element.getLocation()));
                }
            }
        }

        Set<String> presentChildLocals = new HashSet<>();
        for (SemanticElement child : childElements) {
            presentChildLocals.add(child.getName().getLocalName());
        }
        for (String required : descriptor.getRequiredSubElements()) {
            if (!presentChildLocals.contains(required)) {
                diagnostics.add(error(REQUIRED_CHILD_MISSING,
                        describe(element) + ": required child '" + required + "' is missing",
                        element.getLocation()));
            }
        }
    }

    private static void validateText(SemanticElement element,
            Stage1PluginDescriptor descriptor,
            List<SemanticDiagnostic> diagnostics) {
        Optional<Boolean> allowsBody = descriptor.getAllowsBody();
        if (allowsBody.isEmpty() || allowsBody.get()) {
            return;
        }
        for (SemanticContent content : element.getContent()) {
            if (content instanceof SemanticText) {
                SemanticText text = (SemanticText) content;
                if (!text.getText().trim().isEmpty()) {
                    diagnostics.add(error(DISALLOWED_TEXT,
                            describe(element) + ": non-whitespace text/CDATA is not allowed "
                                    + "(allowsBody=false); text id=" + text.getId(),
                            text.getLocation()));
                }
            }
        }
    }

    private static boolean isXmlnsAttribute(SemanticAttribute attribute) {
        QualifiedName name = attribute.getName();
        if ("xmlns".equals(name.getPrefix()) || "xmlns".equals(name.getLocalName())) {
            return true;
        }
        return "http://www.w3.org/2000/xmlns/".equals(name.getNamespaceUri());
    }

    private static String candidateNsList(List<Stage1PluginDescriptor> candidates) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append('{').append(candidates.get(i).getNamespaceUri()).append('}');
        }
        return builder.toString();
    }

    private static String describe(SemanticElement element) {
        return "element id=" + element.getId() + " name=" + element.getName().expandedName();
    }

    private static SemanticDiagnostic error(String code, String message, SourceLocation location) {
        return new SemanticDiagnostic(code, SemanticDiagnostic.Severity.ERROR, message, location);
    }

    private static SemanticDiagnostic warning(String code, String message, SourceLocation location) {
        return new SemanticDiagnostic(code, SemanticDiagnostic.Severity.WARNING, message, location);
    }

    private static final class Frame {
        final SemanticElement element;
        final SemanticElement parent;

        Frame(SemanticElement element, SemanticElement parent) {
            this.element = element;
            this.parent = parent;
        }
    }
}
