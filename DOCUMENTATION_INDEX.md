# WebHarvest Documentation Index

This index distinguishes current project guidance from historical and version-specific material. Repository-facing documentation is maintained in English.

**Canonical source of truth:** [GitHub repository](https://github.com/web-harvest/web-harvest)  
**Current integration branch:** `main`  
**Maven project version:** `2.3.1` (POM metadata only; not evidence of a published release)

## Start here

1. [README](README.md) — project overview, module map, current direction and contributor quick start.
2. [Contributing Guide](CONTRIBUTING.md) — development prerequisites, deterministic test commands, CI evidence and branch policy.
3. [Project Direction and Architecture Roadmap](docs/PROJECT_DIRECTION.md) — accepted architecture direction, staged delivery and decision gates.
4. [Monorepo and Module Boundary Policy](docs/MONOREPO.md) — current Maven modules and dependency boundaries.
5. [Plugin Author Guide](docs/PLUGIN_AUTHOR_GUIDE.md) — target extension contract and explicit stability limitations.
6. [Release Readiness](docs/RELEASE_READINESS.md) — evidence-based distribution status; do not infer publication from POM versions.
7. [Semantic Metadata](docs/semantic-metadata/) — current semantic metadata and catalog material.

## Core engineering references

- [Plugin Inventory](docs/PLUGIN_INVENTORY.md) — preliminary source-backed registration/discovery inventory. It is not an approved Core membership list; see [Issue #93](https://github.com/web-harvest/web-harvest/issues/93).
- [Function Execution](docs/FUNCTION_EXECUTION.md) — execution behavior and current implementation notes; verify claims against source and tests.
- [Validation Rules](docs/VALIDATION_RULES.md) — structural validation behavior and constraints.
- [Documentation README](docs/README.md) — documentation structure, status and contribution standards.
- Module-specific README and Markdown files — consult the owning module and verify version-specific claims before relying on them.

## Historical and version-specific documents

The following root-level files are retained for historical, migration, compatibility or research purposes. Their presence does not imply that they describe the current supported product or runtime. Verify every operational claim against current source, tests and CI before use.

- [User Guide](USER_GUIDE.md) — current user-documentation status and links; the former mixed-language v2.3 manual is preserved as a labeled historical archive.
- [Developer Guide](DEVELOPER_GUIDE.md) — older development and IDE guidance; use this index, README and CONTRIBUTING for current contributor workflow.
- [Plugins and Extensions](PLUGINS.md) — current status, source-of-truth links and explicit extension stability boundaries.
- [Visual Editor Guide](VISUAL_EDITOR_GUIDE.md) — version-specific IDE behavior; verify against the current IDE before use.
- [Plugin Metadata Guide](PLUGIN_METADATA_GUIDE.md) — historical metadata/API guidance; verify against current Core source.
- [Plugin Documentation Generation](PLUGIN_DOCS_GENERATION.md) — historical generator instructions and plugin counts require revalidation.
- [Working XML Syntax Reference](WORKING_XML_SYNTAX_REFERENCE.md) and [XPath Best Practices](XPATH_BEST_PRACTICES.md) — compatibility references; validate examples against current integration tests.
- [HttpClient 5 Migration Guide](HTTPCLIENT5_MIGRATION_GUIDE.md), [Pause/Resume Guide](PAUSE_RESUME_GUIDE.md), and [Enhanced Metrics Guide](ENHANCED_METRICS_GUIDE.md) — version-specific migration/feature material; current availability must be verified.
- [October 2025 Release Notes](OCTOBER_2025_RELEASE_NOTES.md) — historical release record.
- [Changelog](CHANGELOG.md) — release history; distinguish released changes from unreleased work.
- [Fixed Bugs Archive](BUGS_FIXED.md) — historical defect record, not a current defect/status tracker.
- [Component Library](COMPONENT_LIBRARY.md) — design/component reference; confirm its current owner and consumers before changing related assets.
- [Build and Test Guide](BUILD_AND_TEST.md) — current Core, reactor and opt-in network validation commands.

## Legacy publishing material

- [SourceForge Setup](SOURCEFORGE-SETUP.md) and [SFTP Upload Instructions](SFTP-UPLOAD-INSTRUCTIONS.md) are legacy operational documents. They are **not** current contribution, publishing or release instructions.
- GitHub is the sole canonical source for code, issues, pull requests, reviews, CI and project documentation. Do not use SourceForge as an upstream, fetch source, synchronization target or authority.
- These legacy files remain pending a separate preservation/removal decision tracked in [documentation audit Issue #152](https://github.com/web-harvest/web-harvest/issues/152).

## Documentation policy

- Write current project documentation, source comments, Javadoc, issues and pull requests in English.
- Label historical/versioned content clearly; do not silently rewrite historical records as current product guidance.
- Separate verified current facts, accepted decisions and future proposals.
- Verify commands against Maven configuration and CI workflows; verify XML examples against runtime tests where practical.
- Record exact commit SHAs and test/CI evidence for claims that depend on implementation state.
- Preserve copyright, license and third-party attribution notices.
- Update this index when current entry points or documentation ownership changes.

## Audit tracking

The repository-wide Markdown review is tracked in [Issue #152](https://github.com/web-harvest/web-harvest/issues/152). At the audit baseline, the repository tree contained 21 root-level Markdown files and 211 Markdown files under `docs/`; these are inventory counts, not a claim that every document is current or authoritative.
