# @webharvest/xml-roundtrip

Self-contained XML parse/generate helpers for WebHarvest visual tooling.

Owns `XMLParser`, `XMLGenerator`, legacy tag mappings, and XML sanitizers.
Depends on `@webharvest/metadata-client` for the `PluginLookup` contract only.
Requires a DOM `DOMParser` / `Node` (browser or jsdom).
