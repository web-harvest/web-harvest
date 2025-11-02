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
import org.webharvest.plugin.semantic.PluginSemanticsResolver;

/**
 * Builds a unified plugin metadata catalog by combining curated overrides with
 * runtime metadata discovered from actual plugin classes. The resulting
 * descriptors power the IDE palette, property editors, and semantic
 * validations.
 */
public final class PluginMetadataCatalog {

    private static final Logger LOG = LoggerFactory.getLogger(PluginMetadataCatalog.class);
    private static final String CATALOG_RESOURCE = "/metadata/plugin-semantics.json";
    private static final Gson GSON = new Gson();
    private static final String SCHEMA_RESOURCE = "/metadata/plugin-metadata-schema.json";

    private static final Map<String, PluginDescriptor> DESCRIPTORS;
    private static final String SCHEMA_VERSION;
    private static final Map<String, Object> CATALOG_SCHEMA;

    static {
        CatalogOverrides overrides = loadOverrides();
        CatalogBundle bundle = buildRuntimeCatalog(overrides);
        SCHEMA_VERSION = overrides.schemaVersion();
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

    private static CatalogOverrides loadOverrides() {
        JsonObject root;
        try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(PluginMetadataCatalog.class.getResourceAsStream(CATALOG_RESOURCE),
                        "Missing metadata catalog resource: " + CATALOG_RESOURCE),
                StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ex) {
            LOG.warn("Unable to load plugin metadata overrides ({}): {}", CATALOG_RESOURCE, ex.getMessage());
            return new CatalogOverrides("1.0.0", Map.of());
        }

        String schemaVersion = root.has("schemaVersion")
                ? root.get("schemaVersion").getAsString()
                : "1.0.0";

        Map<String, PluginDescriptor> overrides = new LinkedHashMap<>();
        if (root.has("plugins")) {
            for (JsonElement element : root.getAsJsonArray("plugins")) {
                PluginDescriptor descriptor = toDescriptor(element.getAsJsonObject());
                overrides.put(descriptor.getId(), descriptor);
            }
        }

        LOG.info("Loaded {} curated plugin descriptors from overrides (schema v{})",
                overrides.size(), schemaVersion);
        return new CatalogOverrides(schemaVersion, overrides);
    }

    private static CatalogBundle buildRuntimeCatalog(CatalogOverrides overrides) {
        Map<String, PluginDescriptor> descriptors = new LinkedHashMap<>();
        Map<String, PluginDescriptor> overrideMap = new LinkedHashMap<>(overrides.descriptors());

        for (String className : PluginClassIndex.allPluginClassNames()) {
            Class<? extends Plugin> pluginClass = tryLoadPluginClass(className);
            if (pluginClass == null) {
                continue;
            }
            try {
                PluginDescriptor runtimeDescriptor = createDescriptor(pluginClass);
                if (runtimeDescriptor == null) {
                    continue;
                }

                PluginDescriptor override = overrideMap.remove(runtimeDescriptor.getId());
                PluginDescriptor merged = mergeDescriptors(runtimeDescriptor, override);
                descriptors.put(merged.getId(), merged);
            } catch (Exception ex) {
                LOG.warn("Failed to build metadata for plugin {}: {}",
                        pluginClass.getName(), ex.getMessage());
            }
        }

        // Append remaining overrides for plugins not yet available at runtime (future extensions)
        for (PluginDescriptor descriptor : overrideMap.values()) {
            descriptors.putIfAbsent(descriptor.getId(), descriptor);
        }

        LOG.info("Assembled metadata catalog with {} plugins", descriptors.size());
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

        return new PluginDescriptor(id, elementName, displayName, origin, pluginClass.getName(), category, description,
                documentation, semantics, uiHints, dependencies, Map.of());
    }

    private static PluginSemantics toDescriptorSemantics(org.webharvest.plugin.semantic.PluginSemantics semantics) {
        if (semantics == null) {
            return new PluginSemantics(PluginSemanticRole.LEAF, true, false, false,
                    false, false, List.of(), List.of());
        }

        PluginSemanticRole role = PluginSemanticRole.valueOf(semantics.getRole().name());
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

        return new PluginSemantics(role, canAppearAtRoot, allowsChildren, semantics.allowsAnyChild(),
                allowsBody, allowsText, allowedChildTypes, requiredParents);
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

        PluginSemanticRole role = semanticsObj.has("role")
                ? PluginSemanticRole.valueOf(semanticsObj.get("role").getAsString())
                : PluginSemanticRole.LEAF;

        boolean canAppearAtRoot = semanticsObj.has("canAppearAtRoot")
                && semanticsObj.get("canAppearAtRoot").getAsBoolean();
        boolean allowsChildren = semanticsObj.has("allowsChildren")
                && semanticsObj.get("allowsChildren").getAsBoolean();
        boolean acceptsAnyChild = semanticsObj.has("acceptsAnyChild")
                && semanticsObj.get("acceptsAnyChild").getAsBoolean();
        boolean allowBody = semanticsObj.has("allowBody")
                && semanticsObj.get("allowBody").getAsBoolean();
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

        return new PluginSemantics(role, canAppearAtRoot, allowsChildren, acceptsAnyChild,
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

    private static final class CatalogOverrides {
        private final String schemaVersion;
        private final Map<String, PluginDescriptor> descriptors;

        CatalogOverrides(String schemaVersion, Map<String, PluginDescriptor> descriptors) {
            this.schemaVersion = schemaVersion;
            this.descriptors = Map.copyOf(descriptors);
        }

        String schemaVersion() {
            return schemaVersion;
        }

        Map<String, PluginDescriptor> descriptors() {
            return descriptors;
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

