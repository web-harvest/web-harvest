# Phase 5 — Security and public-release preparation

## Secrets scan (verified)

Tool: `gitleaks` (`/opt/homebrew/bin/gitleaks`)

| Scope | Result | Exit / note |
| --- | --- | --- |
| Working tree (`gitleaks detect --no-git`) | **0 findings** | exit 0; `evidence/gitleaks-workdir.json` |
| Working tree re-scan (session continuation) | **0 findings** | exit 0; `evidence/gitleaks-workdir-session2.summary` |
| Reachable history (`gitleaks git --log-opts="--all"`) | **2 findings** | same rule, same path |

### History findings (confirmed pattern; remediated in current tips)

| Rule | Path | Commit (short) | Classification |
| --- | --- | --- | --- |
| `stripe-access-token` | `webharvest-core/.../HttpRedirectIntegrationBug36Test.java` | `7cd4bea5` (and duplicate hit) | **Confirmed historical fixture** shaped like `sk_live_…` — **not a live credential**. Remediated on `2.2.x` via PR **#81** (`87ec535`) and re-applied on this stabilization tip. |

**Do not rewrite history** without owner approval. Recommend: keep remediation commits; optionally document the historical false-credential fixture in Issue #77/#79; no rotation needed if never a real key.

No complete secret values are reproduced in this report.

## License and attribution

| Artifact | Status |
| --- | --- |
| Root `LICENSE` | Present (BSD text; aligned with `2.2.x`) |
| `NOTICE` | Present |
| `licences/` | Present (third-party texts) |
| Maven `<licenses>` | BSD License URL |
| Provenance questions | Unresolved: completeness of dependency license inventory vs modern SBOM expectations — track under Issue #79 |

**Not changed:** license terms, copyright notices, contributor attribution.

## Public exposure checklist (visibility change **not** performed)

When the repository becomes public, all of the following become visible:

- [ ] All remote branches (today: `2.2.x`, `master` only after prune)
- [ ] All PRs + discussion (including closed unmerged stacks)
- [ ] Issues + comments
- [ ] Actions workflow definitions historically registered (even if missing from default-branch tree)
- [ ] Full commit history (includes historical Stripe-shaped fixture until rewritten — not recommended)
- [ ] Tags including `archive/*`
- [ ] Release metadata (if any)

### Publication blockers (owner)

1. Decide default/integration branch strategy (`01-branch-pr-inventory.md`) and land Core tip.
2. Confirm Issue #79 website/SourceForge script disposition.
3. Confirm acceptance of historical fixture commit without history rewrite.
4. Ensure billing/Actions spending limit does not block required checks after going public (separate from workflow correctness).
5. Review closed PR commentary for private paths / personal data (manual owner review recommended).
