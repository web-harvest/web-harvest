package org.webharvest.plugin.semantic;

import java.util.Locale;
import java.util.Set;

import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;

/**
 * Builds {@link PluginSemantics} descriptors combining static heuristics with
 * metadata supplied by {@link PluginMetadata} and {@link PluginConfigurationSchema}.
 */
public final class PluginSemanticsResolver {

    private static final Set<String> CONTEXT_CONTAINERS = Set.of(
        "config",
        "loop",
        "while",
        "if",
        "try",
        "switch",
        "case",
        "function",
        "ftp",
        "mail",
        "zip",
        "database",
        "webbrowser",
        "call",
        "http"
    );

    private static final Set<String> BODY_EVALUATORS = Set.of(
        "def",
        "xpath",
        "xquery",
        "regexp",
        "script",
        "template",
        "xslt",
        "json-to-xml",
        "xml-to-json",
        "html-to-xml",
        "xml",
        "text",
        "list",
        "empty",
        "constant"
    );

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
        String normalizedName = elementName.toLowerCase(Locale.ROOT);

        PluginConfigurationSchema schema = metadata.getConfigurationSchema();
        boolean allowsBody = schema != null && schema.allowsBody();
        String[] allowedChildren = schema != null ? schema.getAllowedSubElements() : null;
        boolean hasExplicitChildren = allowedChildren != null && allowedChildren.length > 0;
        String[] requiredParents = schema != null ? schema.getRequiredParents() : new String[0];

        if (allowsBody) {
            builder.allowsChildPlugins(true)
                   .allowsTextContent(true);
        }

        builder.allowedChildElements(allowedChildren);
        builder.requiredParentElements(requiredParents);

        if ("config".equals(normalizedName)) {
            builder.role(PluginNodeRole.ROOT_CONTAINER)
                   .allowsChildPlugins(true)
                   .allowsTextContent(false)
                   .providesExecutionContext(true)
                   .executable(false)
                   .allowsAnyChild(true);
            return builder.build();
        }

        boolean requiresParent = requiredParents.length > 0;
        if (requiresParent) {
            builder.role(PluginNodeRole.DEPENDENT);
        }

        if (BODY_EVALUATORS.contains(normalizedName) || (allowsBody && !hasExplicitChildren && !requiresParent
                && !CONTEXT_CONTAINERS.contains(normalizedName))) {
            builder.role(PluginNodeRole.BODY_EVALUATOR)
                   .allowsChildPlugins(true)
                   .allowsTextContent(true);
        }

        if (CONTEXT_CONTAINERS.contains(normalizedName)) {
            builder.role(PluginNodeRole.CONTEXT_CONTAINER)
                   .allowsChildPlugins(true)
                   .providesExecutionContext(true)
                   .allowsAnyChild(!hasExplicitChildren);
        }

        if (!allowsBody && !hasExplicitChildren && !CONTEXT_CONTAINERS.contains(normalizedName)) {
            if (requiresParent) {
                builder.role(PluginNodeRole.DEPENDENT);
            } else if (!BODY_EVALUATORS.contains(normalizedName)) {
                builder.role(PluginNodeRole.LEAF);
            }
        }

        return builder.build();
    }
}

