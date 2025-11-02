package org.webharvest.plugin.metadata;

import java.util.List;
import java.util.Map;

/**
 * Unified metadata descriptor combining static semantics with runtime schema.
 */
public final class PluginDescriptor {

    private final String id;
    private final String elementName;
    private final String displayName;
    private final String origin;
    private final String className;
    private final String category;
    private final String description;
    private final PluginDocumentation documentation;
    private final PluginSemantics semantics;
    private final PluginUIHints uiHints;
    private final List<String> dependencies;
    private final Map<String, Object> extensions;

    public PluginDescriptor(String id,
            String elementName,
            String displayName,
            String origin,
            String className,
            String category,
            String description,
            PluginDocumentation documentation,
            PluginSemantics semantics,
            PluginUIHints uiHints,
            List<String> dependencies,
            Map<String, Object> extensions) {
        this.id = id;
        this.elementName = elementName;
        this.displayName = displayName;
        this.origin = origin;
        this.className = className;
        this.category = category;
        this.description = description;
        this.documentation = documentation;
        this.semantics = semantics;
        this.uiHints = uiHints;
        this.dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        this.extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }

    public String getId() {
        return id;
    }

    public String getElementName() {
        return elementName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getOrigin() {
        return origin;
    }

    public String getClassName() {
        return className;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public PluginDocumentation getDocumentation() {
        return documentation;
    }

    public PluginSemantics getSemantics() {
        return semantics;
    }

    public PluginUIHints getUiHints() {
        return uiHints;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }
}

