package org.webharvest.plugin.semantic;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.webharvest.plugin.ParentChildRelationship;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.VisualPluginMetadata;

/**
 * Builds {@link PluginSemantics} descriptors using the metadata declared by
 * each plugin (configuration schema, visual metadata, declared roles).
 */
public final class PluginSemanticsResolver {

    private PluginSemanticsResolver() {
    }

    public static PluginSemantics resolve(Class<? extends Plugin> pluginClass, PluginMetadata metadata) {
        PluginSemantics.Builder builder = PluginSemantics.builder();

        if (metadata == null) {
            return builder.build();
        }

        String elementName = metadata.getElementName();
        if (elementName == null || elementName.isEmpty()) {
            elementName = pluginClass.getSimpleName();
        }

        PluginConfigurationSchema schema = metadata.getConfigurationSchema();
        VisualPluginMetadata visualMetadata = metadata instanceof VisualPluginMetadata
                ? (VisualPluginMetadata) metadata
                : null;

        Set<PluginNodeRole> declaredRoles = metadata.getDeclaredRoles() == null
                ? Collections.emptySet()
                : metadata.getDeclaredRoles();

        EnumSet<PluginNodeRole> roles = declaredRoles.isEmpty()
                ? EnumSet.noneOf(PluginNodeRole.class)
                : EnumSet.copyOf(declaredRoles);

        boolean allowsBody = roles.contains(PluginNodeRole.BODY_EVALUATOR)
                || (schema != null && schema.allowsBody());
        if (allowsBody) {
            roles.add(PluginNodeRole.BODY_EVALUATOR);
        }

        Set<String> allowedChildren = new LinkedHashSet<>();
        if (schema != null && schema.getAllowedSubElements() != null) {
            allowedChildren.addAll(Arrays.asList(schema.getAllowedSubElements()));
        }
        if (visualMetadata != null && visualMetadata.getAllowedChildPlugins() != null) {
            allowedChildren.addAll(Arrays.asList(visualMetadata.getAllowedChildPlugins()));
        }

        Set<String> requiredParents = new LinkedHashSet<>();
        if (schema != null && schema.getRequiredParents() != null) {
            requiredParents.addAll(Arrays.asList(schema.getRequiredParents()));
        }
        ParentChildRelationship parentRelationship = visualMetadata != null ? visualMetadata.getParentRelationship() : null;
        if (parentRelationship != null && parentRelationship.getParentElementName() != null) {
            requiredParents.add(parentRelationship.getParentElementName());
        }

        boolean acceptsAnyChild = visualMetadata != null && visualMetadata.acceptsArbitraryChildren();
        if (acceptsAnyChild) {
            roles.add(PluginNodeRole.CONTEXT_CONTAINER);
        }

        boolean requiresParent = !requiredParents.isEmpty()
                || (parentRelationship != null && parentRelationship.isRequired())
                || roles.contains(PluginNodeRole.DEPENDENT);
        if (requiresParent) {
            roles.add(PluginNodeRole.DEPENDENT);
        }

        if (!allowedChildren.isEmpty()) {
            roles.add(PluginNodeRole.CONTEXT_CONTAINER);
        }

        if (roles.isEmpty()) {
            roles.add(PluginNodeRole.LEAF);
        }

        boolean allowsChildren = roles.contains(PluginNodeRole.CONTEXT_CONTAINER)
                || roles.contains(PluginNodeRole.ROOT_CONTAINER);

        builder.addRoles(roles);

        if (allowsChildren) {
            builder.allowsChildPlugins(true)
                   .providesExecutionContext(roles.contains(PluginNodeRole.CONTEXT_CONTAINER)
                           || roles.contains(PluginNodeRole.ROOT_CONTAINER));
        }

        if (allowsBody) {
            builder.allowsTextContent(true)
                   .allowsChildPlugins(true);
        }

        if (roles.contains(PluginNodeRole.ROOT_CONTAINER)) {
            builder.executable(false)
                   .providesExecutionContext(true)
                   .allowsChildPlugins(true);
        }

        final boolean allowsAnyChild = acceptsAnyChild || (allowsBody && allowedChildren.isEmpty());

        builder.allowedChildElements(allowedChildren.isEmpty()
                ? null
                : allowedChildren.toArray(String[]::new));
        builder.requiredParentElements(requiredParents.toArray(String[]::new));
        builder.explicitlyAcceptsAnyChild(acceptsAnyChild)
               .allowsAnyChild(allowsAnyChild);

        return builder.build();
    }
}

