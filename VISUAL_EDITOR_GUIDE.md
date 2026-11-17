# Visual Editor Guide — historical notice

The former Visual Editor User Guide described a broad v2.3.0 product experience, including specific palette contents, node dimensions, connection semantics, keyboard shortcuts, accessibility guarantees, templates, minimap/grid controls and real-time validation. Those claims were not all supported by current implementation and test evidence.

The original guide is preserved verbatim at [docs/archive/VISUAL_EDITOR_GUIDE-2.3.0.md](docs/archive/VISUAL_EDITOR_GUIDE-2.3.0.md) for historical reference. **Do not use it as a current feature specification or assume every described interaction is implemented.**

## What current source confirms

At the current documentation review baseline (`main`, commit `52c9bcbf6103d24d2b1beed45e33df2bd168aadf)):

- The IDE has a canvas-oriented `VisualDesigner` implementation in `webharvest-ide/frontend/src/visual/VisualDesigner.ts`, with node rendering, drag handling, zoom/pan state, grid snapping, minimap and connection-management components.
- `PluginPalette` is implemented in `webharvest-ide/frontend/src/visual/PluginPalette.ts`; it requests `/api/plugins/metadata` and retains a local-template fallback when that request fails.
- `App.openFileInVisual()` has a focused test in `webharvest-ide/frontend/src/core/__tests__/App.openVisual.test.ts` that verifies XML file selection, parsing, loading into the designer and filename/dirty-state updates.

These source observations do **not** establish that every historical guide interaction is currently available, that all plugin types are palette-eligible, or that the visual model defines runtime execution semantics. Treat exported XML and Core runtime contracts as authoritative; inspect the XML before relying on a visual arrangement.

## Current references

- [Project direction](docs/PROJECT_DIRECTION.md)
- [Plugin author guide](docs/PLUGIN_AUTHOR_GUIDE.md)
- [Plugin metadata guide](PLUGIN_METADATA_GUIDE.md)
- [Documentation index](DOCUMENTATION_INDEX.md)

For current IDE behavior, verify the relevant frontend implementation and tests for the exact interaction. This notice intentionally makes no blanket accessibility, keyboard-shortcut, performance or feature-completeness claim.

