# GitHub topics, labels, and contribution metadata

**Issue:** [#87](https://github.com/web-harvest/web-harvest/issues/87) (parent [#84](https://github.com/web-harvest/web-harvest/issues/84))  
**Policy:** Prefer a small taxonomy. Reuse existing labels. Do not bulk-delete historical labels or mass-relabel old issues without a separate maintainer-approved migration.

## Repository topics (target set)

Concise discoverability topics for the public repository:

- `java`
- `web-scraping`
- `data-extraction`
- `xml`
- `maven`
- `plugins`
- `automation`
- `webharvest`

Homepage field (when set by maintainers): intended product identity `https://webharvest.org` after hosting cutover; until then leave unset or point at the GitHub README rather than a parked page.

## Label taxonomy

### Type (reuse existing GitHub defaults where present)

| Label | Color | Use |
|---|---|---|
| `bug` | `#d73a4a` | Defect in current behavior |
| `enhancement` | `#a2eeef` | New capability or improvement |
| `documentation` | `#0075ca` | Docs-only or docs-primary work |
| `question` | `#d876e3` | Clarification, not yet an agreed change |
| `duplicate` / `invalid` / `wontfix` | defaults | Triage outcomes |

### Area (create if missing; keep existing `project`)

| Label | Color | Use |
|---|---|---|
| `area:core` | `#1d76db` | `webharvest-core`, runtime, semantic model |
| `area:plugins` | `#5319e7` | Extension modules / plugin SPI |
| `area:ide` | `#d93f0b` | IDE / Visual Designer |
| `area:cli` | `#fbca04` | CLI |
| `area:docs` | `#0075ca` | Documentation / website coordination notes in-app |
| `area:ci` | `#0e8a16` | Workflows, Actions, quality gates |
| `project` | `#ededed` | Cross-cutting project governance (already exists) |

### Workflow / community (reuse; do not invent “good first issue” without a real starter task)

| Label | Use |
|---|---|
| `good first issue` | Only when a newcomer can complete the issue with documented steps |
| `help wanted` | Maintainer wants external help on a bounded task |

### Priority

Do **not** introduce a priority dimension until maintainers actively triage with it. Absent process, priority labels become noise.

## Issue templates

Minimal templates live under `.github/ISSUE_TEMPLATE/`:

- Bug report — reproduction, expected/actual, commit SHA, commands
- Enhancement — problem, proposed contract, non-goals
- Config — shared assignee/label defaults if needed later

Templates must not duplicate the entire CONTRIBUTING guide; they link to it.

## Destructive cleanup deferred

Renaming/deleting legacy labels, bulk retagging historical issues, and deleting unused default labels require explicit maintainer approval and a migration note. This iteration only adds missing area labels and topics and documents rules.
