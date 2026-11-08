# WebHarvest Semantic Metadata

This directory documents the semantic metadata catalog that powers the visual
designer, IDE tooling, and LLM integrations. The catalog is built during server
startup by `PluginMetadataCatalog` and exposed via
`GET /api/plugins/metadata`.

## Schema

- **Version:** `1.1.0`
- **Canonical file:** `webharvest-core/src/main/resources/metadata/plugin-metadata-schema.json`
- **API contract:** `/api/plugins/metadata` now returns the `schemaVersion` and
  an embedded `schema` object that mirrors the JSON schema file.

### Top-Level Fields

| Field | Description |
| --- | --- |
| `id` | Stable identifier used in XML (`<http>` ⇒ `http`). |
| `elementName` | Canonical element name, usually matches `id`. |
| `displayName` | Human friendly title for palette and docs. |
| `origin` | Source module (`core`, `ftp`, `mail`, `database`, `zip`, `webbrowser`). |
| `category` | High level grouping used by the palette. |
| `description` | Short description suitable for tooltips. |
| `documentation` | Summary, canonical documentation URL, and example snippets. |
| `semantics` | Structural rules (role, allowed children, parent requirements, body behaviour). |
| `ui` | Palette hints such as icon, palette group, search tags. |
| `extensions` | Reserved for future per-plugin metadata blocks. |

### Semantics Roles

| Role | Meaning |
| --- | --- |
| `ROOT_CONTAINER` | Only the `<config>` node; accepts arbitrary children and anchors the workflow. |
| `CONTEXT_CONTAINER` | Provides a new execution scope (loops, try/catch, ftp, database, etc.). |
| `BODY_EVALUATOR` | Evaluates body/text content and returns a value (xpath, script, template). |
| `DEPENDENT` | Helper element that must live inside a specific parent (`http-header`, `catch`, `zip-entry`). |
| `LEAF` | Standalone processor without body or children (`sleep`, `exit`, `log`). |

## Stage-1 structural contract

The Stage-1 semantic model uses a stricter contract than the legacy visual metadata
summary. See [`STAGE1_CONTRACT.md`](./STAGE1_CONTRACT.md) for child-policy states,
namespace-qualified relationship matching, provenance, and the scope of runtime
availability diagnostics. In particular, body evaluation does not by itself declare
that arbitrary plugin children are structurally valid.

The catalog preserves both `acceptsAnyChild` (the legacy effective value, which may
include BODY_EVALUATOR inference) and `explicitlyAcceptsAnyChild` (an explicit
metadata declaration only). Stage 1 uses only the explicit field as authority for
`ANY_CHILD`; the JSON schema documents both fields.

## Contributor Workflow

1. **Register plugin classes** in `PluginClassIndex` so the runtime catalog can
   discover them.
2. **Add or update curated overrides** in
   `webharvest-core/src/main/resources/metadata/plugin-semantics.json`. Include
   documentation summaries, canonical URLs, and structural rules that cannot be
   derived from the Java metadata.
3. **Validate:** run `mvn -pl webharvest-core test -Dtest=PluginMetadataCatalogTest`
   to ensure every plugin has documentation, semantics, and UI hints.
4. **Expose through the IDE:** the frontend consumes the API response to drive
   the palette, property panel, and node inspector. Additional metadata is
   automatically available to `window.app.getPluginContext()` for LLM helpers.

## API Response Snapshot

```json
{
  "count": 52,
  "schemaVersion": "1.1.0",
  "schema": { ... },
  "plugins": [
    {
      "id": "http",
      "elementName": "http",
      "category": "Network",
      "documentation": {
        "summary": "Performs HTTP/HTTPS requests with headers, parameters and binary support.",
        "url": "https://web-harvest.sourceforge.io/documentation/2.2.0/http.html"
      },
      "semantics": {
        "role": "BODY_EVALUATOR",
        "allowsChildren": true,
        "allowedChildTypes": ["http-header", "http-param"]
      },
      "ui": {
        "paletteGroup": "Network",
        "icon": "fa-globe",
        "tags": ["http", "request", "web"]
      },
      "schema": {
        "attributes": [ ... ]
      }
    }
  ]
}
```

See [`PLUGIN_INVENTORY.md`](./PLUGIN_INVENTORY.md) for the full curated list of
plugins, their roles, and coverage status.


