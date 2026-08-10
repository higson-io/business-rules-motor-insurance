# Rollback Plan — AngularJS 1.8.3 → Angular 22

**Task**: `.maister/tasks/migrations/2026-08-06-angularjs-to-angular`
**Companion to**: `implementation/spec.md`
**Date**: 2026-08-06

---

## 0. Why this plan is short

Rollback here is **deliberately minimal**, and that is a decision, not an omission
(`requirements.md` §5 "Rollback", C-11):

| Usual migration concern | Applies here? | Why not |
|---|---|---|
| Production traffic to protect | ❌ | Demo / reference application. `tech-stack.md`: not production, no SLA, 1–2 person team |
| Downtime window | ❌ | Nothing is continuously deployed |
| Dual-run / traffic shifting | ❌ | **Explicitly rejected.** The pre-migration commit is a git artefact, so replaying captured flows against its WAR gives the same A/B value for free (`target-state-plan.md` §4.3) |
| Feature flag | ❌ | There is no seam to flag. One screen, one bundle, one WAR |
| Data migration / schema rollback | ❌ | No client state; the server holds everything in `@SessionScope` (`service/QuoteService.java:10-12`). The database belongs entirely to Higson and no migration tool exists by design (`backend/migrations.md`) |
| Higson snapshot rollback | ❌ | The snapshot is untouched (`spec.md` §11). Studio remains the system of record (`higson/snapshot.md`) |
| Docker / registry rollback | ❌ | `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh` and `docker/app.properties` are unchanged (AC-B7). Any previously published image tag remains pullable and runnable |

**What remains is therefore a single mechanism: git.** Reversibility is rated **high**
(`target-state-plan.md` §4.1) because nothing in this migration touches the database, the Higson
snapshot, the Docker chain, or any Java source beyond one file.

---

## 1. Branch strategy

```
master ──────────────────────────────────────────────────────────────►
   │
   └── techflash/5.8 (current working branch)
          │
          ├── tag: pre-angular-migration    ← the replay anchor (§3)
          │
          └── migration/angularjs-to-angular-22   ← ALL migration work
```

Rules:

1. **All migration work happens on `migration/angularjs-to-angular-22`.** No commit lands on `master`
   or on the base branch until Phase 6 verification passes.
2. **The tag `pre-angular-migration` is created before the first deletion**, on the commit that
   contains the golden captures (see §3). This is the single anchor everything else references.
3. **One task group per commit, each independently buildable** (§5). A half-finished task group is
   never committed.
4. **Rollback = revert or abandon the branch.** There is no runtime rollback procedure, no feature
   flag, no downtime window.
5. `git commit` / `git push` happen only when the user asks (`global/conventions.md` records commit
   practice as historically weak; this migration does not fix that, but it does not worsen it either).

---

## 2. The revert procedure

### 2.1 Abandon before merge (the normal case)

```bash
git checkout techflash/5.8
git branch -D migration/angularjs-to-angular-22        # or leave it for reference
```

Nothing to undo. `src/main/webapp/`, `less/`, `gulpfile.js`, `package.json`, `package-lock.json` and
`pom.xml` are exactly as they were on the base branch.

### 2.2 Revert after merge

```bash
git revert -m 1 <merge-commit-sha>      # if merged as a merge commit
# or
git revert --no-commit <first-sha>..<last-sha> && git commit
```

Then **§4 — recover the untracked-by-design artefacts**, because a revert restores *tracked file
content* and nothing else.

### 2.3 Roll back to the tag (the blunt instrument)

```bash
git checkout -b rollback/pre-angular pre-angular-migration
```

Use this when the migration is being abandoned wholesale rather than surgically reverted. The tag
points at a commit that is **known-buildable and known-captured** — that is its entire purpose.

---

## 3. Rebuilding and verifying the pre-migration state

This is the same procedure used to capture goldens (`spec.md` §8.4), which is why it is known to work:

```bash
# 1. get the old tree
git checkout pre-angular-migration

# 2. the snapshot is NOT in git — a fresh checkout has no rules (P-2)
mvn higson-cli:pull

# 3. build the AngularJS WAR
TZ=UTC mvn clean package -Pall_in_one

# 4. run it
TZ=UTC java -jar target/motor-insurance.war
#    -> Tomcat starts on 48080, context path /demo

# 5. verify
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:48080/demo/       # expect 200
curl -s http://localhost:48080/demo/driver | head -c 200                    # expect DriverDto JSON
cd e2e && TZ=UTC npx playwright test                                        # goldens replay green
```

**Verification checklist for a restored pre-migration state:**

- [ ] `mvn clean package` succeeds — note this requires **the old Node toolchain expectations**, i.e.
      Gulp 4 under whatever Node is installed. Node 24 (installed for P-1) runs the old Gulp pipeline
      fine; this has no known incompatibility, but it is the one thing to check first if the build
      fails after a rollback.
- [ ] `target/motor-insurance.war` contains `index.html`, `app.js`, `components/**`, `styles/styles.min.css`
- [ ] The application serves at `http://localhost:48080/demo/` and renders 3 variant cards
- [ ] `GET /demo/quote` returns a `QuoteDto`
- [ ] The Playwright goldens replay green against it (this is the strongest single check — it is the
      same evidence that was captured *from* this build)

⚠️ **The `mvn higson-cli:pull` step is mandatory and easy to forget.** `src/main/higson/` has never been
tracked in git (91 commits, 9 years) and is absent from `.gitignore`
(`standards/higson/snapshot.md`). Without it the application boots but returns nothing useful, and a
rollback will look broken when it is not.

---

## 4. What is irreversible — and how to recover it

A `git revert` restores tracked file *content*. Three things need a moment's thought.

### 4.1 The deleted `src/main/webapp/styles/styles.min.css` (2946 lines)

**Status**: fully recoverable from git history. It is a tracked file today; the migration deletes it
and adds no ignore rule that would prevent its restoration (DR-21, `spec.md` §5.4).

```bash
git checkout pre-angular-migration -- src/main/webapp/styles/styles.min.css
```

If it was untracked *and* deleted in separate commits, recover it explicitly:

```bash
git log --oneline --all -- src/main/webapp/styles/styles.min.css
git show <sha>:src/main/webapp/styles/styles.min.css > src/main/webapp/styles/styles.min.css
```

**It is also regenerable**: it is Gulp output from `less/styles.less`. `mvn clean package` on the
restored tree recreates it. Recovering it from history is only needed if you want the exact prior bytes.

### 4.2 The 96 deleted font files

`src/main/webapp/fonts/` — Font Awesome 4 (6 files) + Open Sans (10 weight/style folders × 5 formats
= 90 files), all binary, all tracked.

**Status**: fully recoverable. Git stores binaries as well as text.

```bash
git checkout pre-angular-migration -- src/main/webapp/fonts/
```

**Caveat that makes this look scarier than it is**: `less/config.less:1,5` imports Open Sans and Font
Awesome from `node_modules` (`npm-font-open-sans`, `font-awesome`), while the 96 committed files are a
**duplicate vendored copy** referenced via `@FontPathOpenSans` / `@FontFontAwesome`
(`config.less:3,6`). Restoring `less/config.less` and `package.json` restores the npm side too, and
`npm ci` re-fetches those packages. So even total loss of the 96 binaries is recoverable from npm.

**Same treatment for**: `src/main/webapp/imgs/{logo.png, select-arrow.png}` (DR-24) and
`imgs/arrow.svg` — recover with `git checkout pre-angular-migration -- src/main/webapp/imgs/`.

### 4.3 `package-lock.json` regeneration

**Status**: this is the one genuinely lossy item, and it is lossy in a specific and limited way.

The root `package-lock.json` (~298 KB) is **deleted**, and a new `frontend/package-lock.json` is
generated from scratch. The old file itself is trivially recoverable
(`git checkout pre-angular-migration -- package.json package-lock.json`), but:

- ⚠️ **What is NOT guaranteed is that the old lockfile still resolves.** It pins AngularJS-era packages
  (`angular@1.8.3`, `angular-ui-bootstrap@^2.5.0`, `angular-tooltips@1.1.10`,
  `angular-loading-spinner@0.0.3`, `angular-spinner@1.0.1`, `spin.js@2.3.2`, `bootstrap@^3.3.7`,
  `font-awesome@^4.7.0`, `npm-font-open-sans@^1.0.3`, the Gulp 4 toolchain). All are EOL. `npm ci`
  reproduces exactly what the lockfile names, so it works as long as those tarballs remain on the
  registry — but nobody controls that, and several packages are unmaintained.
- **Mitigation, and it is cheap**: before the first deletion, archive a populated `node_modules` and
  the `vendors/` output.

  ```bash
  # run once, at the pre-angular-migration commit, alongside the golden capture
  npm ci
  tar czf ~/motor-insurance-angularjs-node_modules.tgz node_modules src/main/webapp/vendors
  ```

  This is a **local escape hatch, deliberately not committed** — `build.md` forbids committing
  `node_modules`, and the archive is a rollback aid, not a build input.
- The `frontend/package-lock.json` is regenerated wholesale by design (`current-state-analysis.md`
  §"REPLACE — build tooling"). There is nothing to preserve on the new side.

### 4.4 What is genuinely irreversible — the runtime capture

**The golden captures cannot be recreated by `git revert`.** They are runtime evidence: recorded
requests, response bodies, rendered text and screenshots taken against a *running* AngularJS build.
Reverting the code restores the ability to rebuild and re-capture, but it does not restore the capture
itself — and re-capturing after the fact costs a full snapshot pull, build and run cycle.

This is why **P-3 is a hard gate**: goldens are recorded and committed *before* a single file under
`src/main/webapp/`, `less/`, `gulpfile.js` or the root `package.json` is deleted or modified
(`target-state-plan.md` §7, R-10; `spec.md` §3).

The captures live in `e2e/goldens/` and **are committed** — they are evidence, not build output
(`spec.md` §5.4).

### 4.5 Not irreversible, despite appearances

| Item | Why it is fine |
|---|---|
| `pom.xml` edits | Four localised edits (§5.1 of the spec); `git checkout pre-angular-migration -- pom.xml` restores it. ⚠️ On restore, re-check that `<higson.studio.token>` (`pom.xml:31`) is still **empty** before committing — `standards/global/security.md` |
| `GlobalExceptionHandler.java` (C-6) | One file, ~10 lines. Reverting it restores the 404-with-empty-banner behaviour, which is what the P-3 golden already records |
| `.gitignore` | Text file, revertible |
| Deleted `less/**` (599 lines) | Tracked; recoverable and, in any case, largely re-authored under `frontend/src/styles/` |
| The Higson snapshot | **Never touched.** No rollback needed. Studio is the system of record |
| Published Docker images | **Never rebuilt** by this migration. Any existing tag still runs the AngularJS build |

---

## 5. Per-task-group rollback points during execution

Each task group is a commit; each commit is a rollback point. The gate column states what must be true
*before* the commit is made — a group that fails its gate is reverted, not patched forward.

| # | Task group | Rollback point / gate | What a rollback at this point costs |
|---|---|---|---|
| **0** | **Prerequisites** — Node 24.19.0 (P-1), `mvn higson-cli:pull` (P-2), JUnit 4 → `spring-boot-starter-test` (`pom.xml:72-78`), surefire `TZ=UTC` | `node -v` in range; `mvn test` compiles and `SnapshotDataSourceDemoTest` still runs | Nothing. Node is a machine-level change (C-5: local only, no `.nvmrc`); the pom edit is 8 lines |
| **1** | **GOLDEN CAPTURE (hard gate)** — `e2e/` harness, `data-testid` hooks added to the AngularJS templates, MockMvc contract tests, `git tag pre-angular-migration` | 21 API shapes pinned incl. `text/plain` bodies, ISO-8601 `dateOfBirth`, the **404** error contract; 6 E2E flows recorded; screenshots stored with TZ metadata | ⚠️ **This is the last cheap rollback point.** Everything before it is additive: the AngularJS app is untouched apart from `data-testid` attributes. Reverting costs one commit |
| **2** | **Scaffold & packaging** — `frontend/` workspace, `pom.xml` `<workingDirectory>` + `<webResources>` + clean filesets, `.gitignore`, placeholder `app-root` | ⚠️ `mvn clean package -Pall_in_one` produces a WAR containing `index.html` + `main-*.js`, **and the container serves it at `/demo`, `/` and `/app`** (AC-B4) | Revert the commit. `src/main/webapp/` is **still intact** at this point — nothing has been deleted. The old app still builds if `pom.xml` is reverted with it. **This is the last point at which both frontends coexist in the tree** |
| **3** | **Store, interceptors, services** — `QuoteStore`, `pendingRequestsInterceptor`, `mutationErrorInterceptor`, 4 services, typed models | Store encodes the emit asymmetry (DR-13); spinner interceptor live; all 4 services typed; **zero `.data` unwraps** | Revert; group 2's placeholder app still builds and packages |
| **4** | **Components, leaves first** — `coverage`, `discount` → `variants` → `driver-history`, `vehicle` → `driver` → `spinner` → **datepicker last** | Each component's parity-checklist section passes | Revert individual component commits. The datepicker is deliberately **last** (highest risk, `target-state-plan.md` §7) so a rollback there does not unwind everything behind it |
| **5** | **Shell & Material theme** — `app`, `sidebar`, `theme.scss`, Material components | Single screen renders end-to-end at all three context paths | Revert; group 4 components are unaffected in isolation but the app will not render — treat groups 4+5 as a rollback unit if group 5 fails |
| **6** | **Styles & assets** — LESS migration, `url()` re-anchoring, Manrope self-hosting, asset cleanup | No 404s on fonts or images; screenshot diff **reviewed** (not gated) | Revert. Purely cosmetic; a rollback leaves an ugly but functional app |
| **7** | **Backend fix** — `GlobalExceptionHandler` → `@RestControllerAdvice` + `ErrorDto` (C-6 / DR-14), and the **deliberate** golden update | Contract test asserts 400 + `{"message": …}`; the golden diff cites `DR-14` | Revert one Java file + one golden. Isolated by design — it is the only backend change, kept in its own commit so it can be rolled back independently of the frontend |
| **8** | **Parity verification** — replay the group-1 goldens against the new build | Every diff traceable to C-9 (appearance) or a DR-nn row. Unexplained diff ⇒ regression | No code change; this group produces a verdict, not a commit |
| **9** | **CUTOVER — deletion** — delete `src/main/webapp/{index.html,app.js,components,styles,fonts,imgs,vendors}`, `less/`, `gulpfile.js`, root `package.json` + `package-lock.json`; keep `src/main/webapp/` with `.gitkeep` | `mvn clean package -Pall_in_one` green **from a clean clone** | ⚠️ **The point of no easy return.** Recoverable via §4.1–4.3 but no longer a one-line revert. Do this **only after group 8 passes.** Keep it as a single, isolated commit titled so it is obvious in `git log` |
| **10** | **Docs & standards** — `Readme.md`, 6 frontend standards, `tech-stack.md`, `architecture.md`, `INDEX.md` | `INDEX.md` regenerated | Documentation only; revert freely |

### Anti-sequencing — rollback-relevant

Doing any of these early destroys a rollback point:

- ❌ Deleting anything under `src/main/webapp/` or `less/` before group 8 passes. Group 9 exists as a
  separate group **for this reason** — the temptation is to delete as you go.
- ❌ Merging the branch before group 8.
- ❌ Regenerating `package-lock.json` before the group-1 archive of §4.3 exists.
- ❌ Rebuilding or pushing a Docker image at any point during the migration. `push-to-docker.sh`
  carries a hardcoded `VERSION=4.3.0`; publishing mid-migration would overwrite a tag that currently
  contains a working AngularJS build — the one artefact outside git that constitutes a rollback target.

---

## 6. Rollback decision guide

| Symptom | Likely group | Action |
|---|---|---|
| `mvn package` fails inside `prepare-package` with an opaque npm error | 0 or 2 | Check `node -v` first (P-1, R-1). `engine-strict=true` should have named the range — if it did not, that is itself a finding |
| WAR builds green but the deployed app is blank / 404s on `main-*.js` | 2 | `<webResources>` directory does not match `angular.json`'s `outputPath`, or a `<base>` element crept in (AC-B4, AC-S6). Revert group 2 and re-derive both paths |
| App works at `/demo` but not at `/app` | 2 | A `baseHref` was set. See `spec.md` §5.6 |
| Values arrive at the backend wrapped in quotes | 3 | A `JSON.stringify` was added to a string PUT (AC-C2). This is a code fix, not a rollback |
| `PUT driver/birthDate` returns 415 | 3 or 4 | A string was passed where a `Date` is required (`spec.md` §6.1 point 3). Code fix |
| A component renders `undefined` everywhere | 3 | A leftover `response.data` unwrap (AC-C6, R-6). Code fix |
| `GET quote` fires on every vehicle field change | 4 | The emit asymmetry was lost (DR-13 / ND-1). **This is a failed migration, not a nicety** — revert the vehicle component commit and re-do it against the DR-13 acceptance criterion |
| Spinner never appears | 3 | The interceptor was not registered in `app.config.ts`. Code fix |
| Goldens replay with unexplained diffs | 8 | Do **not** update goldens to match. Every diff must map to C-9 or a DR-nn row; anything else is a regression (`spec.md` §7) |
| Everything is worse than expected and the schedule is gone | any | `git checkout -b rollback/pre-angular pre-angular-migration`, then §3. The AngularJS app is fully functional and nothing outside git was changed |

---

## 7. Summary

- **Mechanism**: git branch + one anchor tag. Nothing else.
- **Cheapest exit point**: end of task group 1 (goldens captured, nothing removed).
- **Point of no easy return**: task group 9 (deletion), deliberately isolated and deliberately last.
- **Genuinely irreversible**: only the *runtime* golden captures — which is exactly why P-3 gates
  everything.
- **Untouched and therefore needing no rollback**: the Higson snapshot, the database, the Docker chain,
  47 of 48 backend Java files, `finalName`, port 48080, the `all_in_one` profile.
