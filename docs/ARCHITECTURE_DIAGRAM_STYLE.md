# WebHarvest Architecture Diagram Visual Language

**Status:** Documentation standard for architecture diagrams; visual reference is evidence-backed but provisional pending the final public-site cutover.
**Applies to:** Mermaid diagrams in architecture, lifecycle, semantic-model, runtime and conformance documentation.
**Owner:** WebHarvest documentation / architecture.

## 1. Intent

Architecture diagrams are product documentation, not generic engineering whiteboards. They should feel like a coherent extension of the WebHarvest website: clear editorial hierarchy, generous whitespace, restrained brand accents, neutral surfaces, readable labels and consistent component treatment.

The visual language must improve comprehension without changing the meaning of the architecture. Color is never the only carrier of state or trust. Every diagram must remain understandable in GitHub's Mermaid renderer, in light/dark contexts where possible, and when printed or viewed without color.

## 2. Evidence and source of truth

The canonical GitHub source for public website content is [web-harvest/website](https://github.com/web-harvest/website), as recorded in [website issue #1](https://github.com/web-harvest/website/issues/1). The repository README and issue #1 explicitly state that the repository is a source migration, not a production deployment; the hosting/canonical-domain cutover is not approved. The initial website snapshot came from application repository `docs/web/**` at `e25296701225f40a0f62b56df0795e5b83290a78` (2026-11-12), as recorded in `SOURCE-SNAPSHOT.md`.

The current available CSS reference is:

- Repository: `web-harvest/website`
- Branch: `main`
- File: `css/enterprise.css`
- Blob: `7ed70c03a9eb548f4e54f5419f27d8a63e112f55`
- Root token declaration: `:root`
- Typeface declaration: `Inter`, followed by system sans-serif fallbacks.

This is an inspected source artifact, **not proof of the exact CSS currently served by the legacy public endpoint**, and not a final approved brand-token package. Keep the distinction explicit until a production website cutover and visual review establish the final live design system. Do not fetch or use SourceForge as a development source.

## 3. Observed visual tokens

The following values are copied from the CSS source above; they are not newly invented or renamed website tokens.

### Brand palette

| CSS token | Value | Intended visual role in diagrams |
|---|---|---|
| `--purple` | `#7B1FA2` | Secondary architecture family, provider/catalog boundaries |
| `--magenta` | `#D81B60` | Primary emphasis, active execution path, selected/authoritative component |
| `--yellow` | `#FFC107` | Notes, warnings, decision points and attention markers |
| `--lime` | `#A4C639` | Healthy/accepted/verified outcomes, only when evidence supports that state |

The CSS also declares `--gradient-primary` from purple to magenta and `--gradient-accent` from yellow to a lighter yellow. Use gradients sparingly in diagrams; prefer flat fills for nodes and lines so text remains legible and diagrams remain visually quiet.

### Neutral palette

| CSS token | Value |
|---|---|
| `--gray-50` | `#F9FAFB` |
| `--gray-100` | `#F3F4F6` |
| `--gray-200` | `#E5E7EB` |
| `--gray-300` | `#D1D5DB` |
| `--gray-500` | `#6B7280` |
| `--gray-600` | `#4B5563` |
| `--gray-700` | `#374151` |
| `--gray-900` | `#111827` |
| `--bg-base` | `#FFFFFF` |
| `--bg-subtle` | `#F9FAFB` |

Use white as the canvas, subtle gray for grouped regions, gray-200/300 for boundaries and gray-500/600 for connectors and secondary labels. Avoid large saturated fills.

### Typography, rhythm and shape

- Typeface: Inter, with system sans-serif fallbacks.
- Main text: gray-900; secondary text: gray-600; tertiary text: gray-500.
- Spacing tokens are 4px multiples (`0.25rem` through `5rem`); use an 8px primary rhythm for diagram gaps and padding, with 4px only for compact internal spacing.
- Radius tokens: 6px (`--radius-sm`), 8px (`--radius-md`), 12px (`--radius-lg`), 16px (`--radius-xl`), 24px (`--radius-2xl`).
- Shadows exist in the website CSS, but architecture diagrams should use borders and surface contrast rather than simulated elevation.

## 4. Diagram composition rules

1. **One diagram, one question.** Split broad architecture maps into ownership, lifecycle, sequence, dataflow and fault-handling views.
2. **Name boundaries explicitly.** Label owner, lifecycle, mutability and trust boundary where they affect behavior.
3. **Use consistent node grammar.** Runtime-owned components, immutable data models, mutable state, external actors and compatibility adapters must be visually distinguishable and labelled.
4. **Use brand color as an accent, not decoration.** Magenta marks the primary/authoritative path; purple marks a distinct secondary domain; yellow marks a decision, warning or note; lime is reserved for a verified healthy/success state. Neutral is the default.
5. **Never encode semantics by color alone.** Add text labels such as `CURRENT`, `TARGET`, `READ-ONLY`, `MUTABLE`, `PINNED GENERATION`, `PROVISIONAL` or `NOT IMPLEMENTED`.
6. **Distinguish evidence from proposal.** Current-source observations use explicit current-state labels. Normative target-state diagrams carry a visible title/caption saying they describe the target architecture, not the current implementation.
7. **Keep flow direction consistent.** Prefer left-to-right for ownership/dataflow and top-to-bottom for lifecycle/state transitions. Use sequence diagrams for temporal interactions.
8. **Avoid crossing edges.** Reorder nodes, introduce subgraphs or split the diagram rather than accepting tangled connectors.
9. **Keep labels concise.** Put detailed constraints and rationale in adjacent prose, not inside oversized nodes.
10. **Show failure and uncertainty honestly.** A dashed edge means optional/conditional/proposed only when its meaning is stated in the legend. Never let a dashed edge imply that an unverified fallback already exists.
11. **Preserve accessibility.** Check contrast, avoid tiny labels and ensure monochrome/printed reading remains possible.
12. **Keep diagrams reviewable.** Mermaid source remains in Markdown, adjacent to the contract it illustrates; no screenshots as the only representation.

## 5. Mermaid theme baseline

Use this base theme for new Mermaid diagrams, then add only diagram-specific overrides. The values mirror the inspected CSS tokens above.

```mermaid
%%{init: {"theme":"base","themeVariables":{
  "fontFamily":"Inter, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif",
  "primaryColor":"#FCE7F3",
  "primaryTextColor":"#111827",
  "primaryBorderColor":"#D81B60",
  "secondaryColor":"#F3E8FF",
  "secondaryTextColor":"#111827",
  "secondaryBorderColor":"#7B1FA2",
  "tertiaryColor":"#F9FAFB",
  "tertiaryTextColor":"#111827",
  "tertiaryBorderColor":"#D1D5DB",
  "lineColor":"#6B7280",
  "textColor":"#111827",
  "mainBkg":"#FFFFFF",
  "nodeBorder":"#D1D5DB",
  "clusterBkg":"#F9FAFB",
  "clusterBorder":"#E5E7EB",
  "edgeLabelBackground":"#FFFFFF",
  "actorBkg":"#F9FAFB",
  "actorBorder":"#7B1FA2",
  "actorTextColor":"#111827",
  "actorLineColor":"#6B7280",
  "signalColor":"#6B7280",
  "signalTextColor":"#111827",
  "labelBoxBkgColor":"#F3F4F6",
  "labelBoxBorderColor":"#D1D5DB",
  "noteBkgColor":"#FFF8DB",
  "noteBorderColor":"#FFC107",
  "noteTextColor":"#374151",
  "activationBkgColor":"#FCE7F3",
  "activationBorderColor":"#D81B60",
  "sequenceNumberColor":"#FFFFFF"
}}}%%
```

Use `classDef` sparingly for explicit semantic classes. Prefer the neutral theme defaults for ordinary components. A class name should describe its meaning (for example `runtimeOwned`, `immutableModel`, `mutableState`, `compatibilityAdapter`, `externalBoundary`), not its color.

## 6. Required caption and legend

Every architecture diagram must have a short caption that states:

- whether it represents **CURRENT IMPLEMENTATION**, **TARGET ARCHITECTURE**, or **PROPOSED / UNDER REVIEW**;
- what question the diagram answers;
- any non-obvious line or boundary semantics.

For a set of diagrams, use a shared legend once and keep the node/edge grammar consistent across the set. Do not repeat a large legend in every diagram.

## 7. Review checklist

- [ ] Colors and type follow the observed WebHarvest website tokens above.
- [ ] The source and provisional status of the visual reference are not misrepresented.
- [ ] One diagram answers one architectural question.
- [ ] Current implementation and target architecture are unmistakably distinguished.
- [ ] Ownership, mutability, lifecycle and trust boundaries are visible where relevant.
- [ ] Color is reinforced by labels/shape/line semantics.
- [ ] No diagram implies unimplemented behavior or an approved SPI.
- [ ] Mermaid source renders in GitHub and remains readable at normal document width.
- [ ] Related architecture issues and conformance tests are linked from surrounding prose.

## 8. Maintenance

When the website's production host and canonical design system are approved, inspect the deployed website and the canonical GitHub source together. Reconcile any differences, update the exact source ref/blob and tokens here, then review every architecture diagram for consistency. Until that happens, this file remains the evidence-backed provisional visual standard and must not be described as a verified live-site design system.
