## @webharvest/xml-roundtrip

Temporary package that re-exports the IDE’s `XMLParser`, `XMLGenerator`, and sanitizer utilities so external consumers (VS Code harness, CLI tools) can share the same round-trip implementation.

> **Note:** Until the code is physically moved, this package proxies files from `webharvest-ide/frontend/src`. Consumers must install the main repo or replace these re-exports once we split the codebase into true packages.


