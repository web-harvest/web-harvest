# Visual Focus Mode Mapping (November 2025)

## Overview

This note captures the current behaviour of the Visual Designer's drill-down (focus) mode when working with deeply nested workflows such as the NYTimes example. It records the cues already available to users, the gaps we observed, and the follow-up items that will drive the **Focus Enhancements** task.

## Current Behaviour

- **Breadcrumbs** show the ancestry stack and allow users to jump back to any level; the root crumb always exits focus mode.
- **Focus summary** displays:
  - Node title (type + friendly name/id)
  - Quick metrics (child count, nested containers, leaf nodes, detail nodes)
  - Top child component types
  - Highlighted attributes (method/url/var/etc.)
- **Detail containers** (HTTP, XQuery, ZIP, etc.) render their inline cards inside the focused node, matching the property panel layout.
- **BPMN pins** appear at the top of the canvas while focused, labelling the first and last visible steps (“Start → … → End”) without reintroducing draggable markers.

## Gaps Identified

- Detail-heavy nodes (HTTP with multiple headers/params) lacked a concise breakdown in the focus summary; users had to scan the canvas to understand the composition.
- Breadcrumb/focus summary gave no hint about the *content* of detail sections (e.g. the first header or param), only the aggregated count.
- When focusing nested lists/loops, distinguishing which parts were dependent detail items vs. regular children required visual inspection.

## Improvements Implemented

- `HierarchyInspector.analyzeContainer()` now returns a metadata-rich `detailBreakdown` that aggregates section title, item count, and a short preview of the first entries.
- `updateFocusSummary()` now renders detail sections as compact preview cards (with inline lists of the first items plus “+N more” indicators), so users can inspect HTTP headers/params or XQuery parameters without leaving focus mode.
- The backend registry logic (`MasterDetailRegistry`) considers both `allowedChildPlugins` and semantic `allowedChildTypes`/`allowedSubElements`, guaranteeing consistent coverage for future master/detail plugins.
- Dedicated tests guard the behaviour:
  - `visual/detail/__tests__/MasterDetailEditor.test.ts`
  - `visual/detail/__tests__/MasterDetailRegistry.test.ts`
  - `visual/__tests__/NYTimesLayout.test.ts`
  - Updated `visual/__tests__/HierarchyInspector.test.ts` verifies the analytical summary.
- `PropertyPanel.updateFocusSummary()` mirrors the same breakdown in the sidebar, keeping detail context visible while editing attributes.
- Root-level containers now render as compact summary tiles listing key attributes and child/detail chips sourced from metadata, which keeps nested workflows scannable before drilling down.
- The breadcrumb trail appends metric badges (children, nested containers, details) for each level so users can judge complexity without opening the node.
- Focused containers display a BPMN-inspired overlay summarising the first and last steps with a step count, so users always know the entry/exit points of the sub-process they are editing.

## Next Steps (Feeds Focus Enhancements Task)

- Explore richer preview rendering for `detailBreakdown` (e.g. bullet list of the first N entries) inside the focus summary rather than tooltips.
- Expose focus summary data through an API so the property panel can display the same breakdown even when the canvas is collapsed.
- Add execution metrics overlay integration to highlight detail sections that produced errors or long runtimes during focused drill-down.
- Review breadcrumbs/focus summary visual hierarchy to ensure the added detail chips remain legible across dark/light modes.

Keeping this mapping up to date will help us triage new UX issues surfaced during NYTimes-style workflows and guide the upcoming **Focus Enhancements** implementation.

