# Release and distribution readiness assessment

**Issue:** [#88](https://github.com/web-harvest/web-harvest/issues/88) (parent [#84](https://github.com/web-harvest/web-harvest/issues/84))  
**Inspected against:** `main` @ `436cd50b159a5a22fc9e040663ebd323c5cdfa52`  
**Recommendation:** **NO-GO** for publishing a 2.3.1 GitHub Release or Maven Central deployment without explicit maintainer approval and the blockers below cleared.

No release tag, GitHub Release, or Maven deploy was performed as part of this assessment.

## Evidence matrix

| Area | Finding | Classification |
|---|---|---|
| Source / default branch | Public repo; default branch `main`; baseline SHA above | FACT |
| CI on baseline | Workflow `Semantic Core Contract CI` run [`43578560301`](https://github.com/web-harvest/web-harvest/actions/runs/43578560301) succeeded on that SHA (`workflow_dispatch`) | FACT |
| CI default triggers | Workflow historically targeted recovery/implementation branches and `2.2.x` PRs; `main` was not the only/natural PR base | FACT (addressed in this iteration: `push` includes `main` and `project/**`; `pull_request.branches` includes `main` as the PR base filter — not the head branch) |
| POM version | Root and modules declare `2.3.1`, `groupId` `org.webharvest` | FACT |
| GitHub Releases | `0` releases via GitHub API | FACT |
| Git tags | Historical/archive tags exist (including `2.2.0`, `2.1.0rc1`); no verified `2.3.1` release tag identified as a published product release | FACT / INFERENCE |
| Maven Central (`org.webharvest:webharvest`) | Solr search returned no `org.webharvest` artifacts for current coordinates; older `net.sourceforge.web-harvest` coordinates appear in public indexes | FACT |
| Java baseline | Root compiler `source`/`target` = 11 | FACT |
| Changelog / notes | Root `CHANGELOG.md` and older release-note docs exist; they are not by themselves proof of a published 2.3.1 | FACT |
| Release automation | Root POM contains `release` profile pieces (Sonatype URLs, gpg plugin config). Presence of config ≠ configured credentials or a completed publish | FACT / UNKNOWN |
| License / attribution | BSD-style license texts and `NOTICE` present; this assessment does not change license terms | FACT |

## What “2.3.1” means today

**FACT:** `2.3.1` is the Maven reactor version string.  
**NOT demonstrated:** that users can download a blessed 2.3.1 GitHub Release asset or resolve `org.webharvest:*:2.3.1` from Maven Central.

## Smallest honest next actions (after approval)

1. Decide the first public artifact set (Core-only vs full reactor) and compatibility notes for XML/`set-var` behavior.
2. Ensure `main` CI is green for the intended release commit without relying solely on ad-hoc `workflow_dispatch`.
3. Produce a changelog section that only claims verified changes.
4. Only then, with explicit approval: tag, GitHub Release, and/or Maven publish.

## Blockers for GO

- No GitHub Release / verified 2.3.1 distribution exists yet
- Maven Central publication for current coordinates not demonstrated
- Release credential/signing readiness in Actions (or maintainer-operated publish) is UNKNOWN and unexercised here
- Broader product docs still contain historical claims that must not be copy-pasted into release notes without verification

## Explicit non-actions

- No version bump
- No tag creation
- No GitHub Release publication
- No Maven deploy
- No secret or signing-key changes
