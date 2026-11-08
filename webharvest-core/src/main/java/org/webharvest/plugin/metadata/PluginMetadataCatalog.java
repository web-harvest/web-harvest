package org.webharvest.plugin.metadata;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.annotations.ExpressionCapable;
import org.webharvest.plugin.annotations.MasterDetailConstraint;
import org.webharvest.plugin.annotations.MasterDetailConstraints;
import org.webharvest.plugin.annotations.UiField;
import org.webharvest.plugin.annotations.UiTool;
import org.webharvest.plugin.semantic.PluginNodeRole;
import org.webharvest.plugin.semantic.PluginSemanticsResolver;

/**
 * Builds a unified plugin metadata catalog by combining curated overrides with
 * runtime metadata discovered from actual plugin classes. The resulting
 * descriptors power the IDE palette, property editors, and semantic
 * validations.
 */
public final class PluginMetadataCatalog {

    private static final Logger LOG = LoggerFactory.getLogger(PluginMetadataCatalog.class);
    // UI hints are now fully auto-generated from plugin classes - no static file needed
    // private static final String UI_OVERRIDES_RESOURCE = "/metadata/plugin-ui-overrides.json"; // DEPRECATED - removed
    private static final Gson GSON = new Gson();
    private static final String SCHEMA_RESOURCE = "/metadata/plugin-metadata-schema.json";

    private static final Map<String, PluginDescriptor> DESCRIPTORS;
    private static final String SCHEMA_VERSION = "2.3.0"; // Dynamic generation version
    private static final Map<String, Object> CATALOG_SCHEMA;

    static {
        // All metadata generated dynamically from plugin classes - no static JSON files needed
        // UI hints are auto-generated from plugin category and element names
        CatalogBundle bundle = buildRuntimeCatalog(new UIMetadataOverrides(Map.of()));
        DESCRIPTORS = bundle.descriptors();
        CATALOG_SCHEMA = loadSchemaDocument();
    }

    private static List<String> collectDependencies(PluginMetadata metadata, PluginSemantics semantics) {
        LinkedHashSet<String> deps = new LinkedHashSet<>();

        if (metadata != null && metadata.getDependencies() != null) {
            Arrays.stream(metadata.getDependencies())
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(deps::add);
        }

        if (metadata != null && metadata.getConfigurationSchema() != null) {
            PluginConfigurationSchema schema = metadata.getConfigurationSchema();
            String[] requiredParents = schema.getRequiredParents();
            if (requiredParents != null) {
                Arrays.stream(requiredParents)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(deps::add);
            }
        }

        if (semantics != null && semantics.getRequiredParentTypes() != null) {
            semantics.getRequiredParentTypes().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(deps::add);
        }

        return List.copyOf(deps);
    }

    private PluginMetadataCatalog() {
    }

    public static String getSchemaVersion() {
        return SCHEMA_VERSION;
    }

    public static PluginDescriptor getDescriptor(String id) {
        return DESCRIPTORS.get(id);
    }

    public static List<PluginDescriptor> getAllDescriptors() {
        return List.copyOf(DESCRIPTORS.values());
    }

    public static Map<String, Object> getCatalogSchema() {
        return CATALOG_SCHEMA;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadSchemaDocument() {
        try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(PluginMetadataCatalog.class.getResourceAsStream(SCHEMA_RESOURCE),
                        "Missing metadata schema resource: " + SCHEMA_RESOURCE),
                StandardCharsets.UTF_8)) {
            Object parsed = GSON.fromJson(reader, Map.class);
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsedMap = (Map<String, Object>) parsed;
                return Collections.unmodifiableMap(parsedMap);
            }
        } catch (Exception ex) {
            LOG.warn("Unable to load plugin metadata schema ({}): {}", SCHEMA_RESOURCE, ex.getMessage());
        }
        return Map.of();
    }

    /**
     * UI hints are now fully auto-generated from plugin classes.
     * This method is deprecated - all UI hints come from inferUiHints() based on category and element name.
     */
    @Deprecated
    private static UIMetadataOverrides loadUIMetadataOverrides() {
        // All UI hints are now auto-generated - no static file needed
        LOG.debug("UI hints are auto-generated from plugin classes - no static file needed");
        return new UIMetadataOverrides(Map.of());
    }
    
    /**
     * Applies UI-only overrides to a descriptor.
     * Only affects UI hints (icon, paletteGroup, tags, color), NOT semantics.
     */
    private static PluginDescriptor applyUIOverrides(PluginDescriptor descriptor, UIMetadataOverrides overrides) {
        Map<String, Object> override = overrides.getOverride(descriptor.getId());
        if (override == null || override.isEmpty()) {
            return descriptor;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> uiOverride = (Map<String, Object>) override.get("ui");
        if (uiOverride == null || uiOverride.isEmpty()) {
            return descriptor;
        }
        
        // Apply only UI hints
        String icon = uiOverride.containsKey("icon") ? (String) uiOverride.get("icon") : descriptor.getUiHints().getIcon();
        String paletteGroup = uiOverride.containsKey("paletteGroup") ? (String) uiOverride.get("paletteGroup") : descriptor.getUiHints().getPaletteGroup();
        @SuppressWarnings("unchecked")
        List<String> tags = uiOverride.containsKey("tags") ? (List<String>) uiOverride.get("tags") : descriptor.getUiHints().getTags();
        
        PluginUIHints newUiHints = new PluginUIHints(paletteGroup, icon, tags);
        
        // Return new descriptor with updated UI hints, everything else unchanged
        return new PluginDescriptor(
            descriptor.getId(),
            descriptor.getElementName(),
            descriptor.getDisplayName(),
            descriptor.getOrigin(),
            descriptor.getClassName(),
            descriptor.getCategory(),
            descriptor.getDescription(),
            descriptor.getDocumentation(),
            descriptor.getSemantics(), // Semantics NEVER overridden
            newUiHints,
            descriptor.getDependencies(),
            descriptor.getExtensions()
        );
    }

    /**
     * Builds runtime catalog from dynamically discovered plugins.
     * All metadata generated from plugin classes - no static JSON files needed.
     */
    private static CatalogBundle buildRuntimeCatalog(UIMetadataOverrides uiOverrides) {
        Map<String, PluginDescriptor> descriptors = new LinkedHashMap<>();

        // Generate all metadata dynamically from plugin classes
        for (String className : PluginClassScanner.allPluginClassNames()) {
            Class<? extends Plugin> pluginClass = tryLoadPluginClass(className);
            if (pluginClass == null) {
                continue;
            }
            try {
                PluginDescriptor descriptor = createDescriptor(pluginClass);
                if (descriptor == null) {
                    continue;
                }

                // UI hints are fully auto-generated - no overrides needed

                descriptors.put(descriptor.getId(), descriptor);
            } catch (Exception ex) {
                LOG.warn("Failed to build metadata for plugin {}: {}",
                        pluginClass.getName(), ex.getMessage());
            }
        }

        LOG.info("Assembled metadata catalog with {} plugins (all generated dynamically)", descriptors.size());
        return new CatalogBundle(Collections.unmodifiableMap(descriptors));
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Plugin> tryLoadPluginClass(String className) {
        try {
            Class<?> raw = Class.forName(className);
            if (!Plugin.class.isAssignableFrom(raw)) {
                LOG.debug("Class {} does not implement Plugin", className);
                return null;
            }
            return (Class<? extends Plugin>) raw;
        } catch (ClassNotFoundException ex) {
            LOG.debug("Plugin class {} not present on classpath", className);
            return null;
        }
    }

    private static PluginDescriptor createDescriptor(Class<? extends Plugin> pluginClass) {
        Plugin plugin;
        try {
            plugin = pluginClass.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            LOG.warn("Unable to instantiate plugin {}: {}", pluginClass.getName(), ex.getMessage());
            return null;
        }

        PluginMetadata metadata = plugin.getMetadata();
        if (metadata == null) {
            LOG.warn("Plugin {} returned null metadata", pluginClass.getName());
            return null;
        }

        String id = sanitize(metadata.getId(), metadata.getElementName(), pluginClass.getSimpleName().toLowerCase(Locale.ROOT));
        String elementName = sanitize(metadata.getElementName(), id);
        String displayName = sanitize(metadata.getName(), humanize(elementName));
        String category = sanitize(metadata.getCategory(), "Uncategorized");
        String description = sanitize(metadata.getDescription(), displayName);
        String origin = inferOrigin(pluginClass.getPackageName());

        PluginDocumentation documentation = new PluginDocumentation(description, null,
                metadata.getTags() == null ? List.of() : List.of(metadata.getTags()));

        org.webharvest.plugin.semantic.PluginSemantics resolved = PluginSemanticsResolver.resolve(pluginClass, metadata);
        PluginSemantics semantics = toDescriptorSemantics(resolved);

        PluginUIHints uiHints = inferUiHints(metadata, elementName, category);

        List<String> dependencies = collectDependencies(metadata, semantics);

        // Extract complete schema information for extensions
        Map<String, Object> extensions = extractSchemaExtensions(metadata, pluginClass, resolved);

        return new PluginDescriptor(id, elementName, displayName, origin, pluginClass.getName(), category, description,
                documentation, semantics, uiHints, dependencies, extensions);
    }
    
    /**
     * Extracts complete schema information as extensions map.
     * Includes attribute definitions, namespace, and other schema details.
     */
    private static Map<String, Object> extractSchemaExtensions(PluginMetadata metadata, Class<? extends Plugin> pluginClass, org.webharvest.plugin.semantic.PluginSemantics semantics) {
        Map<String, Object> extensions = new LinkedHashMap<>();
        
        // Extract namespace
        if (metadata.getNamespace() != null) {
            extensions.put("namespace", metadata.getNamespace());
        }
        
        Map<String, Map<String, Object>> attributeUiMetadata = extractAttributeUiMetadata(pluginClass);
        mergeExpressionCapabilities(attributeUiMetadata, pluginClass);
        List<Map<String, Object>> uiTools = extractUiTools(pluginClass);
        
        // Extract complete schema information
        PluginConfigurationSchema schema = metadata.getConfigurationSchema();
        if (schema != null) {
            Map<String, Object> schemaInfo = new LinkedHashMap<>();
            
            // Extract all attribute definitions with full details
            List<Map<String, Object>> attributeDefinitions = new ArrayList<>();
            if (schema.getAttributeDefinitions() != null) {
                for (PluginConfigurationSchema.AttributeDefinition attr : schema.getAttributeDefinitions()) {
                    Map<String, Object> attrDef = new LinkedHashMap<>();
                    attrDef.put("name", attr.getName());
                    attrDef.put("type", attr.getType() != null ? attr.getType().name() : "STRING");
                    attrDef.put("required", attr.isRequired());
                    if (attr.getDefaultValue() != null) {
                        attrDef.put("defaultValue", attr.getDefaultValue());
                    }
                    if (attr.getDescription() != null && !attr.getDescription().isEmpty()) {
                        attrDef.put("description", attr.getDescription());
                    }
                    if (attr.getAllowedValues() != null && attr.getAllowedValues().length > 0) {
                        attrDef.put("allowedValues", Arrays.asList(attr.getAllowedValues()));
                    }
                    if (attr.getMinValue() != null) {
                        attrDef.put("minValue", attr.getMinValue());
                    }
                    if (attr.getMaxValue() != null) {
                        attrDef.put("maxValue", attr.getMaxValue());
                    }
                    if (attr.getPattern() != null && !attr.getPattern().isEmpty()) {
                        attrDef.put("pattern", attr.getPattern());
                    }
                    
                    Map<String, Object> uiMeta = attributeUiMetadata.get(attr.getName());
                    if (uiMeta != null && !uiMeta.isEmpty()) {
                        attrDef.put("ui", new LinkedHashMap<>(uiMeta));
                    }
                    
                    attributeDefinitions.add(attrDef);
                }
            }
            schemaInfo.put("attributeDefinitions", attributeDefinitions);
            
            // Extract required/optional attributes
            if (schema.getRequiredAttributes() != null && schema.getRequiredAttributes().length > 0) {
                schemaInfo.put("requiredAttributes", Arrays.asList(schema.getRequiredAttributes()));
            }
            if (schema.getOptionalAttributes() != null && schema.getOptionalAttributes().length > 0) {
                schemaInfo.put("optionalAttributes", Arrays.asList(schema.getOptionalAttributes()));
            }
            
            // Extract allowed/required sub-elements
            if (schema.getAllowedSubElements() != null && schema.getAllowedSubElements().length > 0) {
                schemaInfo.put("allowedSubElements", Arrays.asList(schema.getAllowedSubElements()));
            }
            if (schema.getRequiredSubElements() != null && schema.getRequiredSubElements().length > 0) {
                schemaInfo.put("requiredSubElements", Arrays.asList(schema.getRequiredSubElements()));
            }
            
            // Extract body support
            schemaInfo.put("allowsBody", schema.allowsBody());
            
            // Extract root placement
            schemaInfo.put("canAppearAtRoot", schema.canAppearAtRoot());
            
            // Extract required parents
            if (schema.getRequiredParents() != null && schema.getRequiredParents().length > 0) {
                schemaInfo.put("requiredParents", Arrays.asList(schema.getRequiredParents()));
            }
            
            // Schema version
            if (schema.getVersion() != null) {
                schemaInfo.put("version", schema.getVersion());
            }
            
            extensions.put("schema", schemaInfo);
        }
        
        // Extract master/detail constraints from annotations
        List<Map<String, Object>> masterDetailConstraints = extractMasterDetailConstraints(pluginClass);
        if (!masterDetailConstraints.isEmpty()) {
            extensions.put("masterDetailConstraints", masterDetailConstraints);
        }
        
        // Extract execution semantics
        Map<String, Object> executionSemantics = extractExecutionSemantics(pluginClass, metadata, semantics);
        if (!executionSemantics.isEmpty()) {
            extensions.put("executionSemantics", executionSemantics);
        }
        
        Map<String, Object> uiMetadata = new LinkedHashMap<>();
        if (!attributeUiMetadata.isEmpty()) {
            Map<String, Map<String, Object>> exportedFields = attributeUiMetadata.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> Collections.unmodifiableMap(new LinkedHashMap<>(entry.getValue())),
                            (a, b) -> a,
                            LinkedHashMap::new));
            uiMetadata.put("fields", Collections.unmodifiableMap(exportedFields));
        }
        if (!uiTools.isEmpty()) {
            uiMetadata.put("tools", uiTools);
        }
        if (!uiMetadata.isEmpty()) {
            uiMetadata.put("version", "1.0");
            extensions.put("ui", Collections.unmodifiableMap(uiMetadata));
        }
        
        return Collections.unmodifiableMap(extensions);
    }
    
    /**
     * Extracts master/detail constraints from @MasterDetailConstraint annotations.
     */
    private static List<Map<String, Object>> extractMasterDetailConstraints(Class<? extends Plugin> pluginClass) {
        List<Map<String, Object>> constraints = new ArrayList<>();
        
        // Check for single annotation
        MasterDetailConstraint single = pluginClass.getAnnotation(MasterDetailConstraint.class);
        if (single != null) {
            constraints.add(buildConstraintMap(single));
        }
        
        // Check for repeatable annotations container
        MasterDetailConstraints container = pluginClass.getAnnotation(MasterDetailConstraints.class);
        if (container != null) {
            for (MasterDetailConstraint constraint : container.value()) {
                constraints.add(buildConstraintMap(constraint));
            }
        }
        
        return constraints;
    }
    
    /**
     * Builds a map representation of a master/detail constraint.
     */
    private static Map<String, Object> buildConstraintMap(MasterDetailConstraint constraint) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("detailType", constraint.detailType());
        map.put("maxOccurs", constraint.maxOccurs());
        map.put("minOccurs", constraint.minOccurs());
        
        if (constraint.sectionTitle() != null && !constraint.sectionTitle().isEmpty()) {
            map.put("sectionTitle", constraint.sectionTitle());
        }
        
        if (constraint.addLabel() != null && !constraint.addLabel().isEmpty()) {
            map.put("addLabel", constraint.addLabel());
        }
        
        if (constraint.emptyLabel() != null && !constraint.emptyLabel().isEmpty()) {
            map.put("emptyLabel", constraint.emptyLabel());
        }
        
        if (constraint.primaryAttribute() != null && !constraint.primaryAttribute().isEmpty()) {
            map.put("primaryAttribute", constraint.primaryAttribute());
        }
        
        if (constraint.secondaryAttribute() != null && !constraint.secondaryAttribute().isEmpty()) {
            map.put("secondaryAttribute", constraint.secondaryAttribute());
        }
        
        if (constraint.usesBody()) {
            map.put("usesBody", true);
        }
        
        return map;
    }

    /**
     * Extract execution semantics from plugin class and metadata
     */
    private static Map<String, Object> extractExecutionSemantics(
            Class<? extends Plugin> pluginClass,
            PluginMetadata metadata,
            org.webharvest.plugin.semantic.PluginSemantics semantics) {
        Map<String, Object> execSemantics = new LinkedHashMap<>();
        
        // Determine if plugin is a container
        boolean isContainer = semantics != null && semantics.allowsChildPlugins();
        execSemantics.put("isContainer", isContainer);
        execSemantics.put("isLeaf", !isContainer);
        
        // Infer execution order from plugin type
        String className = pluginClass.getSimpleName().toLowerCase(Locale.ROOT);
        String executionOrder = "sequential";
        if (className.contains("if") || className.contains("else") || 
            className.contains("try") || className.contains("catch") ||
            className.contains("case")) {
            executionOrder = "conditional";
        } else if (className.contains("loop") || className.contains("while")) {
            executionOrder = "parallel";
        }
        execSemantics.put("executionOrder", executionOrder);
        
        // All plugins can be paused/resumed
        execSemantics.put("canPause", true);
        execSemantics.put("canResume", true);
        
        return execSemantics;
    }

    private static Map<String, Map<String, Object>> extractAttributeUiMetadata(Class<? extends Plugin> pluginClass) {
        Map<String, Map<String, Object>> fields = new LinkedHashMap<>();
        for (UiField uiField : pluginClass.getAnnotationsByType(UiField.class)) {
            if (!hasText(uiField.attribute())) {
                continue;
            }
            fields.put(uiField.attribute(), buildUiFieldMetadata(uiField));
        }
        return fields;
    }

    private static Map<String, Object> buildUiFieldMetadata(UiField uiField) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fieldKind", uiField.kind().name());
        map.put("order", uiField.order());
        if (hasText(uiField.group())) {
            map.put("group", uiField.group());
        }
        if (hasText(uiField.helperText())) {
            map.put("helperText", uiField.helperText());
        }
        if (hasText(uiField.placeholder())) {
            map.put("placeholder", uiField.placeholder());
        }
        if (hasText(uiField.localizationKey())) {
            map.put("localizationKey", uiField.localizationKey());
        }
        if (hasText(uiField.helperTextKey())) {
            map.put("helperTextKey", uiField.helperTextKey());
        }
        if (hasText(uiField.placeholderKey())) {
            map.put("placeholderKey", uiField.placeholderKey());
        }
        if (hasText(uiField.format())) {
            map.put("format", uiField.format());
        }
        if (uiField.minLength() >= 0) {
            map.put("minLength", uiField.minLength());
        }
        if (uiField.maxLength() >= 0) {
            map.put("maxLength", uiField.maxLength());
        }
        if (uiField.expressionCapable()) {
            map.put("expressionCapable", true);
        }
        if (uiField.advanced()) {
            map.put("advanced", true);
        }
        return map;
    }

    private static void mergeExpressionCapabilities(Map<String, Map<String, Object>> attributeUiMetadata,
            Class<? extends Plugin> pluginClass) {
        for (ExpressionCapable capability : pluginClass.getAnnotationsByType(ExpressionCapable.class)) {
            if (!hasText(capability.attribute())) {
                continue;
            }
            Map<String, Object> metadata = attributeUiMetadata.computeIfAbsent(capability.attribute(),
                    key -> new LinkedHashMap<>());
            metadata.put("expressionCapable", true);
            if (hasText(capability.context())) {
                metadata.put("expressionContext", capability.context());
            }
        }
    }

    private static List<Map<String, Object>> extractUiTools(Class<? extends Plugin> pluginClass) {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (UiTool tool : pluginClass.getAnnotationsByType(UiTool.class)) {
            Map<String, Object> toolMap = new LinkedHashMap<>();
            String id = hasText(tool.id()) ? tool.id() : tool.value().getSimpleName();
            toolMap.put("id", id);
            toolMap.put("className", tool.value().getName());
            toolMap.put("placement", tool.placement());
            if (hasText(tool.label())) {
                toolMap.put("label", tool.label());
            } else {
                toolMap.put("label", humanize(id));
            }
            if (hasText(tool.description())) {
                toolMap.put("description", tool.description());
            }
            if (hasText(tool.icon())) {
                toolMap.put("icon", tool.icon());
            }
            tools.add(Collections.unmodifiableMap(toolMap));
        }
        return tools.isEmpty() ? List.of() : List.copyOf(tools);
    }

    private static PluginSemantics toDescriptorSemantics(org.webharvest.plugin.semantic.PluginSemantics semantics) {
        if (semantics == null) {
            return new PluginSemantics(PluginSemanticRole.LEAF, true, false, false,
                    false, false, List.of(), List.of());
        }

        Set<PluginSemanticRole> roles = semantics.getRoles().stream()
                .map(PluginNodeRole::name)
                .map(PluginSemanticRole::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(PluginSemanticRole.class)));
        boolean allowsChildren = semantics.allowsChildPlugins();
        boolean allowsText = semantics.allowsTextContent();
        boolean allowsBody = allowsChildren || allowsText;
        boolean canAppearAtRoot = !semantics.requiresParent();

        List<String> allowedChildTypes = semantics.getAllowedChildElements() == null
                ? List.of()
                : semantics.getAllowedChildElements().stream().sorted().collect(Collectors.toList());
        List<String> requiredParents = semantics.getRequiredParentElements() == null
                ? List.of()
                : semantics.getRequiredParentElements().stream().sorted().collect(Collectors.toList());

        return new PluginSemantics(roles, canAppearAtRoot, allowsChildren, semantics.allowsAnyChild(),
                allowsBody, allowsText, allowedChildTypes, requiredParents,
                semantics.explicitlyAcceptsAnyChild());
    }

    private static PluginDescriptor mergeDescriptors(PluginDescriptor runtimeDescriptor, PluginDescriptor override) {
        if (override == null) {
            return runtimeDescriptor;
        }

        String id = runtimeDescriptor.getId();
        String elementName = firstNonBlank(override.getElementName(), runtimeDescriptor.getElementName());
        String displayName = firstNonBlank(override.getDisplayName(), runtimeDescriptor.getDisplayName());
        String origin = firstNonBlank(override.getOrigin(), runtimeDescriptor.getOrigin());
        String className = firstNonBlank(override.getClassName(), runtimeDescriptor.getClassName());
        String category = firstNonBlank(override.getCategory(), runtimeDescriptor.getCategory());
        String description = firstNonBlank(override.getDescription(), runtimeDescriptor.getDescription());

        PluginDocumentation documentation = override.getDocumentation() != null
                ? override.getDocumentation()
                : runtimeDescriptor.getDocumentation();

        PluginSemantics semantics = override.getSemantics() != null
                ? override.getSemantics()
                : runtimeDescriptor.getSemantics();

        PluginUIHints uiHints = override.getUiHints() != null
                ? override.getUiHints()
                : runtimeDescriptor.getUiHints();

        List<String> dependencies = override.getDependencies().isEmpty()
                ? runtimeDescriptor.getDependencies()
                : override.getDependencies();

        Map<String, Object> extensions = override.getExtensions().isEmpty()
                ? runtimeDescriptor.getExtensions()
                : override.getExtensions();

        return new PluginDescriptor(id, elementName, displayName, origin, className, category, description,
                documentation, semantics, uiHints, dependencies, extensions);
    }

    private static PluginDescriptor toDescriptor(JsonObject obj) {
        String id = obj.get("id").getAsString();
        String element = obj.has("elementName") ? obj.get("elementName").getAsString() : id;
        String displayName = obj.has("displayName") ? obj.get("displayName").getAsString() : humanize(element);
        String origin = obj.has("origin") ? obj.get("origin").getAsString() : "core";
        String category = obj.has("category") ? obj.get("category").getAsString() : "Uncategorized";
        String className = obj.has("className") ? obj.get("className").getAsString() : null;
        String description = obj.has("description") ? obj.get("description").getAsString() : displayName;

        PluginDocumentation documentation = null;
        if (obj.has("documentation")) {
            JsonObject docObj = obj.getAsJsonObject("documentation");
            List<String> examples = new ArrayList<>();
            if (docObj.has("examples")) {
                for (JsonElement el : docObj.getAsJsonArray("examples")) {
                    examples.add(el.getAsString());
                }
            }
            documentation = new PluginDocumentation(
                    docObj.has("summary") ? docObj.get("summary").getAsString() : description,
                    docObj.has("url") ? docObj.get("url").getAsString() : null,
                    examples);
        }

        PluginSemantics semantics = parseSemantics(obj.getAsJsonObject("semantics"));
        PluginUIHints uiHints = parseUiHints(obj.getAsJsonObject("ui"));

        Map<String, Object> extensions = obj.has("extensions")
                ? GSON.fromJson(obj.get("extensions"), Map.class)
                : Map.of();

        List<String> dependencies = new ArrayList<>();
        if (obj.has("dependencies")) {
            for (JsonElement el : obj.getAsJsonArray("dependencies")) {
                dependencies.add(el.getAsString());
            }
        }

        return new PluginDescriptor(id, element, displayName, origin, className, category, description,
                documentation, semantics, uiHints, dependencies, extensions);
    }

    private static PluginSemantics parseSemantics(JsonObject semanticsObj) {
        if (semanticsObj == null) {
            return new PluginSemantics(PluginSemanticRole.LEAF, true, false, false,
                    false, false, List.of(), List.of());
        }

        EnumSet<PluginSemanticRole> roles = EnumSet.noneOf(PluginSemanticRole.class);
        if (semanticsObj.has("roles")) {
            for (JsonElement el : semanticsObj.getAsJsonArray("roles")) {
                roles.add(PluginSemanticRole.valueOf(el.getAsString()));
            }
        } else if (semanticsObj.has("role")) {
            roles.add(PluginSemanticRole.valueOf(semanticsObj.get("role").getAsString()));
        }
        if (roles.isEmpty()) {
            roles.add(PluginSemanticRole.LEAF);
        }

        boolean canAppearAtRoot = semanticsObj.has("canAppearAtRoot")
                && semanticsObj.get("canAppearAtRoot").getAsBoolean();
        boolean allowsChildren = semanticsObj.has("allowsChildren")
                && semanticsObj.get("allowsChildren").getAsBoolean();
        boolean acceptsAnyChild = semanticsObj.has("acceptsAnyChild")
                && semanticsObj.get("acceptsAnyChild").getAsBoolean();
        boolean allowBody = semanticsObj.has("allowsBody")
                && semanticsObj.get("allowsBody").getAsBoolean();
        boolean allowTextContent = semanticsObj.has("allowTextContent")
                && semanticsObj.get("allowTextContent").getAsBoolean();

        List<String> allowedChildTypes = new ArrayList<>();
        if (semanticsObj.has("allowedChildTypes")) {
            for (JsonElement el : semanticsObj.getAsJsonArray("allowedChildTypes")) {
                allowedChildTypes.add(el.getAsString());
            }
        }

        List<String> requiredParentTypes = new ArrayList<>();
        if (semanticsObj.has("requiredParentTypes")) {
            for (JsonElement el : semanticsObj.getAsJsonArray("requiredParentTypes")) {
                requiredParentTypes.add(el.getAsString());
            }
        }

        return new PluginSemantics(roles, canAppearAtRoot, allowsChildren, acceptsAnyChild,
                allowBody, allowTextContent, allowedChildTypes, requiredParentTypes);
    }

    private static PluginUIHints parseUiHints(JsonObject uiObj) {
        if (uiObj == null) {
            return new PluginUIHints(null, null, List.of());
        }

        String paletteGroup = uiObj.has("paletteGroup") ? uiObj.get("paletteGroup").getAsString() : null;
        String icon = uiObj.has("icon") ? uiObj.get("icon").getAsString() : null;
        List<String> tags = new ArrayList<>();
        if (uiObj.has("tags")) {
            for (JsonElement el : uiObj.getAsJsonArray("tags")) {
                tags.add(el.getAsString());
            }
        }

        return new PluginUIHints(paletteGroup, icon, tags);
    }

    private static PluginUIHints inferUiHints(PluginMetadata metadata, String elementName, String category) {
        String paletteGroup = normalizeGroup(category);
        String icon = pickIcon(elementName, category);
        List<String> tags;
        if (metadata.getTags() != null && metadata.getTags().length > 0) {
            Set<String> tagSet = new LinkedHashSet<>();
            Arrays.stream(metadata.getTags())
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(tagSet::add);
            if (tagSet.isEmpty()) {
                tagSet.add(elementName);
                tagSet.add(category.toLowerCase(Locale.ROOT));
            }
            tags = List.copyOf(tagSet);
        } else {
            tags = List.of(elementName, category.toLowerCase(Locale.ROOT));
        }
        return new PluginUIHints(paletteGroup, icon, tags);
    }

    private static String normalizeGroup(String category) {
        if (category == null) {
            return "General";
        }
        String lower = category.toLowerCase(Locale.ROOT);
        switch (lower) {
            case "control flow":
            case "control":
                return "Control";
            case "variables":
                return "Variables";
            case "extraction":
            case "data extraction":
                return "Extraction";
            case "processing":
            case "data processing":
                return "Processing";
            case "storage":
            case "file operations":
                return "Storage";
            case "network":
            case "http & web":
            case "communication":
                return "Network";
            case "configuration":
                return "Configuration";
            default:
                return "General";
        }
    }

    private static String pickIcon(String elementName, String category) {
        String name = elementName.toLowerCase(Locale.ROOT);

        switch (name) {
            case "config":
                return "fa-gear";
            case "loop":
                return "fa-repeat";
            case "while":
                return "fa-rotate";
            case "if":
                return "fa-question";
            case "else":
                return "fa-arrow-down-wide-short";
            case "case":
                return "fa-diagram-project";
            case "try":
                return "fa-shield";
            case "catch":
                return "fa-triangle-exclamation";
            case "script":
                return "fa-code";
            case "template":
                return "fa-file-alt";
            case "xslt":
                return "fa-shuffle";
            case "stylesheet":
                return "fa-code-branch";
            case "html-to-xml":
                return "fa-file-code";
            case "json-to-xml":
                return "fa-brackets-curly";
            case "xml-to-json":
                return "fa-code";
            case "xpath":
                return "fa-code-branch";
            case "xquery":
                return "fa-database";
            case "text":
                return "fa-align-left";
            case "constant":
                return "fa-lock";
            case "empty":
                return "fa-circle";
            case "list":
                return "fa-list";
            case "def":
                return "fa-tag";
            case "get-var":
                return "fa-arrow-right";
            case "value-of":
            case "tokenize":
                return "fa-scissors";
            case "function":
                return "fa-superscript";
            case "call":
                return "fa-phone";
            case "call-param":
                return "fa-sliders";
            case "return":
                return "fa-arrow-turn-down";
            case "body-processor":
                return "fa-layer-group";
            case "http":
                return "fa-globe";
            case "http-header":
                return "fa-heading";
            case "http-param":
                return "fa-sliders";
            case "file":
                return "fa-file";
            case "include":
                return "fa-link";
            case "regexp":
                return "fa-search";
            case "regexp-pattern":
                return "fa-asterisk";
            case "regexp-result":
                return "fa-highlighter";
            case "regexp-source":
                return "fa-input-text";
            case "sleep":
                return "fa-clock";
            case "exit":
                return "fa-right-from-bracket";
            case "log":
                return "fa-file-lines";
            case "xml":
                return "fa-file-code";
            case "ftp":
                return "fa-server";
            case "ftp-get":
                return "fa-arrow-down";
            case "ftp-put":
                return "fa-arrow-up";
            case "ftp-list":
                return "fa-list-check";
            case "mail":
                return "fa-envelope";
            case "database":
                return "fa-database";
            case "connection":
                return "fa-plug";
            case "zip":
                return "fa-file-zipper";
            case "zip-entry":
                return "fa-file-circle-plus";
            case "webbrowser":
                return "fa-window-restore";
            default:
                return defaultIconForCategory(category);
        }
    }

    private static String defaultIconForCategory(String category) {
        if (category == null) {
            return "fa-puzzle-piece";
        }
        String lower = category.toLowerCase(Locale.ROOT);
        switch (lower) {
            case "control":
            case "control flow":
                return "fa-diagram-next";
            case "variables":
                return "fa-box";
            case "extraction":
            case "data extraction":
                return "fa-magnifying-glass";
            case "processing":
            case "data processing":
                return "fa-gears";
            case "network":
            case "http & web":
                return "fa-network-wired";
            case "storage":
            case "file operations":
                return "fa-database";
            case "configuration":
                return "fa-gear";
            default:
                return "fa-puzzle-piece";
        }
    }

    private static String inferOrigin(String packageName) {
        if (packageName == null) {
            return "core";
        }
        if (packageName.contains(".ftp")) {
            return "ftp";
        }
        if (packageName.contains(".mail")) {
            return "mail";
        }
        if (packageName.contains(".db")) {
            return "database";
        }
        if (packageName.contains(".zip")) {
            return "zip";
        }
        if (packageName.contains(".webbrowser")) {
            return "webbrowser";
        }
        return "core";
    }

    private static String sanitize(String value, String fallback) {
        return sanitize(value, fallback, fallback);
    }

    private static String sanitize(String value, String fallback, String defaultValue) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return defaultValue;
    }

    private static String firstNonBlank(String primary, String secondary) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return secondary;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String humanize(String elementName) {
        if (elementName == null || elementName.isBlank()) {
            return "Unnamed Plugin";
        }
        String[] parts = elementName.split("[-_]");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    /**
     * UI-only overrides container (icon, color, paletteGroup).
     * Does NOT contain semantic metadata - that must come from plugin classes.
     */
    private static final class UIMetadataOverrides {
        private final Map<String, Map<String, Object>> overrides;

        UIMetadataOverrides(Map<String, Map<String, Object>> overrides) {
            this.overrides = Map.copyOf(overrides);
        }

        boolean hasOverride(String pluginId) {
            return overrides.containsKey(pluginId);
        }

        Map<String, Object> getOverride(String pluginId) {
            return overrides.get(pluginId);
        }
    }

    private static final class CatalogBundle {
        private final Map<String, PluginDescriptor> descriptors;

        CatalogBundle(Map<String, PluginDescriptor> descriptors) {
            this.descriptors = Map.copyOf(descriptors);
        }

        Map<String, PluginDescriptor> descriptors() {
            return descriptors;
        }
    }
}

