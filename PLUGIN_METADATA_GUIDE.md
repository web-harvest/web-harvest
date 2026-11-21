# Plugin Metadata — Current Implementation Status

This page is a short status pointer for the repository's internal IDE palette metadata. It is **not** a stable third-party Plugin SPI or an external plugin-authoring contract.

## Current source contract

- `VisualPluginMetadata` is an internal Core interface that extends `PluginMetadata`. Its exact methods and Java types are defined in [VisualPluginMetadata.java](webharvest-core/src/main/java/org/webharvest/plugin/VisualPluginMetadata.java).
- A Core processor is eligible for the visual palette only when its `@CorePlugin` annotation has `systemComponent() == false` and `paletteComponent() == true`. The annotation's element-name attribute is `elementName`, not `name`; see [CorePlugin.java](webharvest-core/src/main/java/org/webharvest/plugin/annotations/CorePlugin.java).
- [VisualPluginMetadataCompletenessTest.java](webharvest-core/src/test/java/org/webharvest/plugin/metadata/VisualPluginMetadataCompletenessTest.java) enumerates only those eligible processors. It checks metadata presence, selected field completeness and formats, ports, accessibility data and parent/child consistency. It does not prove that every metadata field has a complete user-facing interaction in the IDE.
- [PluginMetadataJsonGenerator.java](webharvest-core/src/main/java/org/webharvest/plugin/metadata/PluginMetadataJsonGenerator.java) serializes eligible Core metadata. The IDE Maven module runs it in `process-classes` and writes `webapp/static/metadata/plugins.json` under its build output directory; see [webharvest-ide/pom.xml](webharvest-ide/pom.xml).
- The current frontend palette consumes a subset of this metadata, including grouping, icon, color, dimensions, ports and tooltip. Do not infer support for every design field or behavior solely from the Java interface or generated JSON.

## Compatibility and stability

The current metadata classes and annotation-driven palette filter are implementation details of this monorepo. The public third-party Plugin SPI, independent plugin loading and compatibility/versioning guarantees remain separate roadmap gates. Do not build external compatibility promises on these internal interfaces.

Do not use old examples that declare `@CorePlugin(name = ...)`: the current annotation requires `elementName = ...`. Do not assume the historical examples' palette fields, keyboard shortcuts, linked services, accessibility claims or canvas behavior are a supported product contract.

## Historical design reference

The former long-form guide is preserved verbatim at [PLUGIN_METADATA_GUIDE-legacy-2026-11-17.md](docs/archive/PLUGIN_METADATA_GUIDE-legacy-2026-11-17.md). It contains design-era examples and claims and is not current implementation guidance.

For the current extension direction, use the [Plugin Author Guide](docs/PLUGIN_AUTHOR_GUIDE.md) and [Plugins and Extensions](PLUGINS.md).