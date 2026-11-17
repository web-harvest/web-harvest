# Plugins and Extensions

This page is the current entry point for plugin and extension documentation. It deliberately does not publish a definitive plugin catalogue or promise a stable third-party extension API before the corresponding project gates are complete.

## Current source of truth

- The source code and tests on `main` define the behavior that is currently implemented.
- [Plugin Inventory](docs/PLUGIN_INVENTORY.md) is a preliminary, source-backed inventory of registration and discovery paths. It is not an approved Core membership list.
- [Issue #98](https://github.com/web-harvest/web-harvest/issues/98) tracks meaningful runtime contract coverage for the default Core XML mappings.
- [Issue #93](https://github.com/web-harvest/web-harvest/issues/93) tracks the decision and approval of the built-in Core plugin set.
- [Issue #94](https://github.com/web-harvest/web-harvest/issues/94) tracks plugin discovery, runtime registration and extension loading.

## Built-in Core plugins

The default Core registration currently contains 44 XML element mappings. This is a registration baseline, not an approved product catalogue or proof that every mapping has complete behavioral coverage.

Do not infer plugin membership, support level or public stability from:
- a Java package or Maven module location;
- a class annotation or metadata declaration alone;
- a generated documentation page or historical plugin count;
- the existence of scanner/registry classes;
- an example XML file that has not been exercised by a meaningful runtime test.

Use the source-backed inventory and the runtime contract tests to evaluate each mapping. The final approved Core set remains subject to Issue #93.

## Third-party and optional extensions

The [Plugin Author Guide](docs/PLUGIN_AUTHOR_GUIDE.md) describes the intended external plugin contract and explicitly distinguishes implemented behavior from future targets.

The following are not currently promised as stable public capabilities:
- a versioned, independently consumable third-party Plugin SPI;
- arbitrary JAR auto-installation or classpath scanning as a supported deployment contract;
- a separate official extensions repository and release train.

Those capabilities require explicit design, compatibility tests and release gates before they can be documented as supported.

Optional integrations currently exist as Maven modules in this repository, including database, FTP, mail, browser and ZIP/archive integrations. Their presence does not make them members of the built-in Core plugin set.

## Writing and testing plugin documentation

For each plugin claim or example:

1. Identify the exact implementation and registration path on `main`.
2. Identify the configuration contract and relevant source/test files.
3. Add a deterministic runtime test that asserts observable output or context state.
4. Keep network-dependent behavior in an explicitly opt-in network test path.
5. Record the exact commit SHA and CI evidence.
6. Label proposals and planned extension capabilities as future work.

Do not use `assertTrue(true)`, context-non-null checks alone, or catch-and-pass behavior as evidence that a plugin works.

## Historical plugin guide

The former root guide, including its historical plugin examples and migration-era descriptions, is preserved in [PLUGINS-LEGACY-2025.md](docs/archive/PLUGINS-LEGACY-2025.md). It is a historical reference only; its counts, auto-discovery instructions and API descriptions must not be treated as current guarantees.
