# Semantic Metadata Catalog — Current Implementation

This page describes the implementation present in the repository. It is not a declaration that the metadata endpoint is a stable third-party Plugin SPI.

## Runtime catalog

The catalog is built from plugin classes at runtime:

1. `PluginClassScanner` scans the Core and optional extension packages for concrete classes annotated with `@CorePlugin`.
2. Discovered classes are instantiated and their `PluginMetadata` is projected into `PluginDescriptor` entries by `PluginMetadataCatalog`.
3. Structural semantics are resolved through `PluginSemanticsResolver`; UI hints are inferred from plugin metadata and category.
4. The scanner contains an embedded compatibility fallback class list. A failure of an individual package scan is logged and that package is skipped; it does not activate the fallback list. The current implementation can therefore produce a partial or empty catalog after package-level scan failures.
5. The JSON resource `webharvest-core/src/main/resources/metadata/plugin-metadata-schema.json` describes the response shape. It is not a curated plugin list or a source of per-plugin metadata.

The active scanner packages are Core, FTP, Mail, Database, ZIP and WebBrowser. Optional extension classes are loaded reflectively and are included only when present on the runtime classpath.

There is no active `plugin-semantics.json` override catalog or static plugin inventory used to build the current descriptors. The old curated inventory is preserved as a historical snapshot (see below).

## Version fields — decision still open

The current implementation exposes distinct values that must not be treated as interchangeable:

| Field | Current source value | Meaning/status |
| --- | --- | --- |
| Metadata `schemaVersion` | `2.3.0` on the normal catalog path | Returned by `PluginMetadataCatalog.getSchemaVersion()`; asserted by `PluginMetadataCatalogTest` |
| Metadata API `version` | `2.2.0` | Hardcoded in both metadata and version endpoint responses; intended contract is unresolved in [Issue #296](https://github.com/web-harvest/web-harvest/issues/296) |
| Fallback `schemaVersion` | `2.2.0` | Used by the servlet when catalog loading fails |
| Maven reactor version | `2.3.1` | Build/project version; not proof of publication |

Do not infer a product/API version contract from these values. Issue #296 owns the decision and endpoint-level tests. This page intentionally does not choose between a Maven-derived product version, a retained legacy API version, or deprecation of the field.

## API endpoints

The IDE servlet exposes:

- `GET /api/plugins/metadata` — plugin descriptors, count, schema version and JSON schema document when the catalog is available.
- `GET /api/plugins/metadata/version` — version metadata.
- `GET /api/plugins/ontology` — ontology projection.
- `GET /api/plugins/xsd` and `GET /api/plugins/xsd/{version}` — generated XML schemas.

Each descriptor contains a documentation summary. The current catalog constructs `PluginDocumentation` with a null documentation URL; the IDE separately supplies a `helpUrl` using the verified GitHub documentation resolver. Known Core and extension plugin names map to existing files under `docs/web/documentation/2.2.0/`; unknown names map to its index. See [Issue #294](https://github.com/web-harvest/web-harvest/issues/294).

## Contributor workflow

1. Implement the plugin as a concrete class annotated with `@CorePlugin` in one of the scanned packages.
2. Implement `Plugin.getMetadata()` and provide the configuration metadata used by the catalog.
3. Express structural behavior through the plugin metadata and `PluginSemanticsResolver` conventions; add tests for any new semantic rule.
4. If a new extension package is introduced, update `PluginClassScanner.PLUGIN_PACKAGES` and its fallback class list.
5. Change the JSON schema resource only when the API response shape changes; do not use it as a substitute for plugin metadata.
6. Run the focused catalog tests:

   ```bash
   mvn -pl webharvest-core test -Dtest=PluginMetadataCatalogTest
   ```

## Related documentation

- [Stage 1 semantic contract](./STAGE1_CONTRACT.md) — consumer-facing structural rules and limitations.
- [Current plugin inventory status](./PLUGIN_INVENTORY.md) — where to obtain the runtime inventory.
- [Legacy user documentation](../web/documentation/2.2.0/index.html) — preserved 2.2.0 documentation; it is not claimed to cover every 2.3.1 behavior.

## Historical snapshots

The previous v1.1.0 README and curated plugin table are retained for historical context only:

- [README v1.1.0 historical snapshot](../archive/SEMANTIC_METADATA_README_V1.1.0.md)
- [Plugin inventory v1.1.0 historical snapshot](../archive/SEMANTIC_METADATA_PLUGIN_INVENTORY_V1.1.0.md)

Neither snapshot is authoritative for the current runtime catalog.
