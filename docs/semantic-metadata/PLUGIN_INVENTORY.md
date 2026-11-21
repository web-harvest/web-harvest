# Plugin Inventory — Current Status

The current plugin catalog is generated at runtime. This file is a status and navigation page, not a hand-maintained list of plugin rows.

## Canonical source

- Runtime discovery: `webharvest-core/src/main/java/org/webharvest/plugin/metadata/PluginClassScanner.java`
- Descriptor construction: `webharvest-core/src/main/java/org/webharvest/plugin/metadata/PluginMetadataCatalog.java`
- Semantic projection: `webharvest-core/src/main/java/org/webharvest/plugin/semantic/PluginSemanticsResolver.java`
- Runtime API: `GET /api/plugins/metadata`
- Focused catalog tests: `webharvest-core/src/test/java/org/webharvest/plugin/metadata/PluginMetadataCatalogTest.java`

The scanner discovers concrete `@CorePlugin` classes in the Core, FTP, Mail, Database, ZIP and WebBrowser packages. Successful discovery results are cached. An empty result for an absent optional package is accepted; a package scan exception or empty required Core scan uses the complete compatibility fallback for that call. Fallback results are not cached, so a later call retries. See [Issue #309](https://github.com/web-harvest/web-harvest/issues/309), resolved by [PR #311](https://github.com/web-harvest/web-harvest/pull/311). Optional extension modules appear only when their classes are available at runtime.

## What this file does not claim

- It does not publish a fixed plugin count; the runtime catalog is the source for the current count.
- It does not maintain a curated per-plugin role/category table.
- It does not claim that a static `plugin-semantics.json` file drives descriptor construction.
- It does not establish a stable third-party Plugin SPI.
- It does not resolve the difference between API `version`, `schemaVersion` and Maven project version; see [Issue #296](https://github.com/web-harvest/web-harvest/issues/296).

For semantic role definitions and Stage 1 consumer guarantees, use [STAGE1_CONTRACT.md](./STAGE1_CONTRACT.md).

## Historical inventory

The previous curated v1.1.0 table is preserved at [docs/archive/SEMANTIC_METADATA_PLUGIN_INVENTORY_V1.1.0.md](../archive/SEMANTIC_METADATA_PLUGIN_INVENTORY_V1.1.0.md). Its rows, plugin count, role labels, categories and metadata-source labels must not be treated as current runtime facts.
