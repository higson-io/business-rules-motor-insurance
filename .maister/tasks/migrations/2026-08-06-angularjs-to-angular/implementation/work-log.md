# Work Log

## 2026-08-06 — Implementation Started

**Plan**: `implementation/implementation-plan.md`
**Total Steps**: 142
**Task Groups**: 14 (G0 … G13)
**Expected tests**: ~62
**Parity definition**: functional + contractual only. Appearance is out of scope (C-9).
A visual difference is never, on its own, a finding.

### Environment note (carried into every group)

Node 22.23.2 is installed via Homebrew but is **keg-only** at `/opt/homebrew/opt/node@22/bin`
and is **not** on the default PATH — the machine's default `node` is still `v23.6.1`, which
Angular 22 rejects. Every `npm` / `npx` / `ng` / `mvn` invocation must be prefixed with:

```bash
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"
```

`~/.zshrc` is **not** to be modified (user declined). Per C-5 nothing is committed for the Node
bump. This must be documented in `Readme.md` during G13.

### Hard sequencing rules

1. **P-3 is absolute** — G1 and G2 record runtime evidence against the *current* AngularJS build.
   Nothing under `src/main/webapp/`, `less/`, `gulpfile.js` or the root `package.json` may be
   deleted or modified — beyond the additive `data-testid` attributes of step 2.2 — until G2 ends
   with `git tag pre-angular-migration`.
2. **The defect register is a whitelist** — DR-01…DR-27 are the complete set of sanctioned
   deviations. ND-1…ND-8 must survive untouched.
3. **DR-13 / ND-1** — only a vehicle **model** change triggers a quote reload. An idiomatic
   reactive rewrite reconnects production year / make / type *by accident*. This is the one golden
   assertion that must be **byte-identical pre and post**.

---

## Standards Reading Log

### Loaded Per Group

#### Group 0: Toolchain & test-dependency prerequisites

**From Implementation Plan**:
- [x] `standards/global/security.md` — ACTION REQUIRED; gated step 0.5
- [x] `standards/global/commenting.md` — drove a mid-execution self-correction
- [x] `standards/global/minimal-implementation.md` — justified writing no extra Java tests
- [x] `standards/global/coding-style.md` — hard tabs, matching surrounding pom blocks
- [x] `standards/higson/snapshot.md` — `src/main/higson/` treated as read-only
- [x] `standards/testing/test-writing.md` — the binding "drop JUnit 4" clause
- [x] `standards/testing/rule-tests.md` — snapshot-booted, no DB/network
- [x] `standards/backend/java.md` — inherit versions the Boot parent manages

**From INDEX.md**:
- [x] `standards/global/conventions.md` — local toolchain baseline (PATH)
- [x] `standards/frontend/build.md` — `npm ci` never `npm install`; `node_modules` never committed

**Discovered During Execution**:
- [x] `standards/global/conventions.md` §build-output — step 0.6, checking whether `vendors/` is
      ignored; confirms the archive must stay uncommitted
- [x] `standards/deployment/packaging.md` — step 0.4, confirming a surefire block cannot disturb
      `finalName` / `all_in_one` (surefire runs in `test`, `repackage` in `package`)

---

## 2026-08-06 — Group 0 Complete (PARTIAL)

**Steps**: 0.1 through 0.7. Step 0.2 marked `[~]` — command skipped, requirement satisfied.

**Standards Applied**: 8 from plan · 2 from INDEX.md · 2 discovered (see log above)

**Tests**: 5 verification checks — 4 pass, 1 conditional pass.
`SnapshotDataSourceDemoTest`: **3 run, 0 failures, 0 errors.**

**Files Modified**:
- `pom.xml` — JUnit 4 removed, `spring-boot-starter-test` added, `maven-surefire-plugin` block with
  `-Duser.timezone=UTC` (version inherited, P-4 rationale commented)
- `~/motor-insurance-angularjs-node_modules.tgz` — created, uncommitted
- `node_modules/`, `src/main/webapp/vendors/` — regenerated, both gitignored
- Branch `migration/angularjs-to-angular-22` created off `techflash/5.8`

Nothing committed. Nothing pushed.

**Notes / decisions**:

1. **P-1 was already satisfied** before the group ran and was not re-done. Node 22.23.2 (not the
   plan's suggested 24.19.0 — both are in range). Keg-only; the PATH prefix is now a standing
   requirement for G1, G2, G3 and G12, all of which shell out to `npm` via Maven. This is the single
   most likely cause of a confusing failure in G3.
2. **Step 0.2 blocker**: `mvn higson-cli:pull` needs a Studio token that `security.md` deliberately
   keeps empty. Snapshot accepted as-is — see the escalation below.
3. **Deliberate deviation at 0.6**: used `npx gulp prepare:libs` rather than `npm run build`, because
   the latter regenerates the *tracked* `styles.min.css` and would have violated P-3 before the
   `pre-angular-migration` tag exists.
4. **Self-corrected defect**: the first surefire insertion orphaned the `higson-cli` comment block
   from its plugin (`commenting.md`); caught on diff review and moved.
5. **Acceptance criterion "exactly one file changed" not literally met** — `git diff --name-only`
   shows `pom.xml` *and* `.gitignore`. The `.gitignore` entry (`+higson-cli*.log`) predates this
   group and is unrelated. `pom.xml` also carries pre-existing uncommitted work (Higson CLI
   properties, runtime 4.3.0→4.3.1, the `higson-cli` plugin block); G0's own contribution is the
   8-line dependency swap plus the surefire block.
6. **JDK/target skew, harmless**: surefire forks under Java 21 while `maven.compiler.release=17`.
   Worth remembering if a G1 MockMvc test ever behaves oddly.

**Escalation resolved**: user accepted the existing Higson snapshot as satisfying P-2 rather than
re-pulling. Rationale: it is proven functional, and re-pulling mid-capture would inject uncontrolled
variance into exactly the goldens P-3 exists to protect.

---

#### Group 2: Playwright golden capture (P-3 part B)

**From Implementation Plan**:
- [x] `standards/frontend/build.md` — `npm ci` only; exact pins on all three devDependencies
      (`@playwright/test` 1.62.1, `@types/node` 22.19.1, `typescript` 5.9.3), zero carets;
      `node_modules/`, `test-results/`, `playwright-report/` gitignored
- [x] `standards/testing/test-writing.md` — message on every assertion; behaviour-describing names
- [x] `standards/global/minimal-implementation.md` — four support modules, no speculative framework
- [x] `standards/global/commenting.md` — rationale only, no change-log narration
- [x] `standards/global/coding-style.md` — tabs, UTF-8

**From INDEX.md**:
- [x] `standards/frontend/accessibility.md` — drove the selector-policy investigation
- [x] `standards/frontend/services.md` — source of the 21 relative URLs in `API_PATHS`
- [x] `standards/higson/snapshot.md` — honoured P-2 without re-pulling
- [x] `standards/deployment/docker.md`, `packaging.md` — port 48080, `/demo`, `-Pall_in_one`

**Discovered During Execution**:
- [x] `standards/global/documentation.md` — step 2.1, README in English per the standards convention
- [x] `standards/frontend/css.md` — step 2.7, `styles.min.css` is generated-but-tracked; confirmed
      the "never hand-edit generated stylesheets" rule and restored the file

---

## 2026-08-06 — Group 2 Complete (SUCCESS)

**Steps**: 2.1 through 2.9. **2.8 deliberately NOT done** — commit + `git tag pre-angular-migration`
deferred to the main agent. ⚠️ **G3 is blocked until that tag exists.**

**Tests**: `MIGRATION_STAGE=pre TZ=UTC npx playwright test` → **6 passed, 0 failed** (17.1s).
`npx tsc --noEmit` clean. Goldens **byte-identical across two consecutive full runs** — one ordering
flap in the concurrent startup burst was found and fixed by recording a key-sorted tally rather than
an ordered list.

**Files**: `e2e/` created (config, 4 support modules, 6 specs, 6 JSON goldens, 6 screenshots);
`data-testid` added to 7 AngularJS templates.

**Expected-delta table**: 15 plan rows implemented as **17 assertions**, 5 of them **NO BRANCH**.
Two deliberate refinements: the plan's single "year/make/type" row became **three separate**
NO-BRANCH assertions (so a failure names the level), and DR-14 additionally branches the HTTP status
404 → 400, which `spec.md` §6.4 places under that row.

`byStage(defect, {pre, post})` **rejects a call without a DR/ND id** — the branching mechanism cannot
be used without citing its authority.

**Key golden values recorded**:
- startup: `GET driver` = **2**, `GET vehicle` = 1, `GET quote` = 1, 4 dictionaries = 1 each
- vehicle cascade: year → **0** `GET quote`; make → **0**; type → **0**; model → **1** ← the ND-1
  evidence, byte-identical pre and post
- driver-history: typing `12` = **2 PUT + 2 GET quote**; clearing = 1 PUT → **HTTP 500**, silent;
  below-`min` = 2 PUT → 2× HTTP 500
- driver-fields: one date entry = **3** `PUT driver/birthDate`; gender re-click = 1+1 redundant
- quote-panel: coverages `1,234.50`-style, variant premiums **raw** (`1401.19`), discount `-20`
- error-paths: failed PUT → 1 request, no retry, no banner; failed quote → **404**, 3 stale cards,
  banner visible with **empty text**

**P-3 freeze verified mechanically**: 29 testids added; stripping them reproduces the removed lines
byte-for-byte; 0 deletions in the frozen scope; `less/`, `gulpfile.js`, root `package.json` and
`src/main/higson/` untouched.

⚠️ **`styles.min.css` was dirtied by the build and restored** (`git checkout --`, md5 back to
`114d65ad…`). Cause worth carrying forward: the LESS build **inlines Google Fonts CSS over the
network at build time**, and Manrope has moved `v15` → `v20` since the file was last committed. Any
`mvn package` on this repo re-dirties it. G3 and G12 must expect this; DR-21 (untrack it) removes
the problem permanently.

**Three findings accepted as decisions (main agent):**

1. **`spec.md` §8.3's selector assumption is wrong** — no form control has an accessible name.
   The extended `data-testid` set is accepted; see the annotation on step 2.2.
2. **`spec.md` §4.7 names the error surface `data-testid="error-banner"`; the captured id is
   `quote-error-banner`.** ✅ **G5 must adopt the captured id.** A golden cannot be edited to match
   the target — that would defeat the purpose of capturing it.
3. **C-8 is not exercised by the current snapshot data.** No `coverage.description` in the 171-file
   snapshot contains markup (six plain prose, three `null`). `quote-panel.spec.ts` would catch
   dropping *or* escaping if markup existed, but today it catches neither. ✅ **The `HtmlPopover`
   directive needs its own unit test in G5** — the E2E suite will not protect it.

**Not assertable under the selector policy**, both documented in `e2e/README.md`: the gender toggle's
visual selected state (AngularJS signals it only via the Bootstrap `.active` class, no
`aria-pressed`), and ND-6 (sanctioned disappearance, deliberately not asserted).

---

#### Group 1: MockMvc contract goldens (P-3 part A)

**From Implementation Plan**:
- [x] `standards/testing/test-writing.md` — package-private classes, `<ClassUnderTest>Test`, no
      `test` prefix, a message on every assertion, shared fixture
- [x] `standards/testing/rule-tests.md` — snapshot-backed engine, offline, no DB, no network,
      canonical directory consumed directly
- [x] `standards/higson/snapshot.md` — `src/main/higson/` read-only, `higson-cli:pull` never run
- [x] `standards/backend/api.md`, `standards/backend/models.md` — goldens assert DTO field sets
- [x] `standards/global/coding-style.md` — tab indentation (verified: 0 space-indented lines)
- [x] `standards/global/commenting.md`, `standards/global/minimal-implementation.md`

**From INDEX.md**:
- [x] `standards/higson/integration.md` — ⚠️ misapplied at first; see the correction below
- [x] `standards/backend/spring.md`

**Discovered During Execution**:
- [x] `standards/deployment/docker.md` — identifying the running 4.3.0 container as a reference build

---

## 2026-08-06 — Group 1 Complete (SUCCESS, after one corrected false blocker)

**Steps**: 1.1 through 1.5.
**Tests**: `mvn test -Dtest='DriverApiTest,VehicleApiTest,DictionaryApiTest,QuoteApiTest'` →
**8 run, 8 passed, 0 failed.** BUILD SUCCESS.

**Files**: `ApiContractTestSupport.java`, `DriverApiTest.java`, `VehicleApiTest.java`,
`DictionaryApiTest.java`, `QuoteApiTest.java` — all new, all under `src/test/java/`.
**No production file touched.** Nothing committed, tagged or pushed.

### ⚠️ A false blocker was raised and withdrawn — worth recording, because the failure mode recurs

G1 initially reported that `GET /quote` returns HTTP 500 on this branch, root-caused it to
`higson.runtime.function.validate-arguments=true` (`src/main/resources/higson-demo-app.properties:15`),
called it a regression shipped by the starter migration, and recommended a production config change
plus a correction to `standards/higson/integration.md`.

**All of that was wrong.** Verified by the main agent: the working-tree app booted via
`mvn spring-boot:run` against H2, with that property unchanged, returns **HTTP 200** and a complete
three-option quote including the `UMPD` coverage reported as failing. G2 independently reached the
same conclusion — it captured six goldens against that same build.

The real cause was G1's **own test harness**: it read `integration.md`'s *"keep
`function.validate-arguments=true`"*, reasoned the test engine should mirror production, and added
`setValidateFunctionArgumentsDataTypes(true)` to a hand-built engine. That line broke it.

The isolating experiment G1 should have run before reporting — same engine code, only the data
source swapped — shows the **data source** is the discriminator, not the property:

| Data source | `validate-arguments` | `UMPD` |
|---|---|---|
| snapshot | false | prices |
| snapshot | **true** | **fails** |
| live DB | false | prices |
| live DB | **true** | **prices** |

It is a **snapshot-reader** limitation: with validation on, the reader fails to bind the declared
`ParamContext` argument of the Groovy function `util.ctx.get` when a **flow** invokes it. Confirmed
not to be stale snapshot content — the Studio DB's `util.ctx.get` metadata is byte-identical to the
snapshot's TOML header.

Two supporting claims were also unfounded: the 4.3.0 container was offered as evidence the property
was "never enabled before", but that image was built 2026-08-03 and simply predates the working
tree; and `docker/app.properties:11` **does** set the property, contrary to the report.

**Consequences:**
- `src/main/resources/higson-demo-app.properties` and `docker/app.properties` — **untouched**.
- The properties comment *"kept enabled as before the starter migration"* and `integration.md`'s
  `validate-arguments=true` clause are **NOT** wrong. **Removed from the G13 corrections list.**
  `services.md`'s `response.data` clause is now the only standards correction that stands.
- Argument validation stays at the factory default in the test engine, with the reasoning in a
  comment. Proof this costs no golden: `GET /quote` from the snapshot-backed test context is
  **byte-identical** to the live app.

**Lesson for later groups**: a subagent generalising from a two-row probe table produced a confident,
well-written recommendation to change production config and rewrite a standard. Differential probes
must vary one factor at a time and cover the full matrix before a production change is proposed.

### Golden values recorded (the evidence G11 replays against)

- `GET /driver`: `dateOfBirth` is an **ISO-8601 string with `+00:00`**, never epoch millis
  (confirms PC-3, corrects `current-state-analysis.md:194`). 8 top-level + 3 address fields.
- String PUTs: raw **unquoted `text/plain`** bodies → 200, stored verbatim. Control: a body of
  `"Krakow"` *with quotes* stores `city == "\"Krakow\""` — exactly the corruption a stray
  `JSON.stringify` in G4 would cause.
- `PUT driver/birthDate`: `application/json` + quoted ISO string → **200**; same value as
  `text/plain` → **415**. The rule that pulls opposite to the one above, for an adjacent endpoint.
- `GET /vehicle`: 5 primitive `int` fields, never null, cleared reads back as `0`.
- Cascade side effects: make → `typeId=0, modelId=0`; type → `modelId=0`; model → clears nothing;
  productionYear → `makeId=typeId=modelId=0`.
- Dictionaries: `code` is **always a JSON string**.
- **DR-05 evidence**: positions ascending in all three options — `BRONZE [1,2,3]`,
  `SILVER [1,2,4,5,8]`, `GOLD [1,2,4,5,6,7,8,9]`, discounts `[2]`. **The client must add NO sort.**

---

## 2026-08-06 — P-3 GATE CLEARED

Commit **`d09c201`**, tag **`pre-angular-migration`**. Staged explicitly, never `git add .`:
`pom.xml`, `.gitignore`, the 7 `data-testid` templates, `e2e/` (28 files, 1.4 MB) and `src/test/`
(6 Java files). **Excluded**: `src/main/higson/` (25 MB — the snapshot is never version-controlled),
`.maister/` and `CLAUDE.md` (pre-existing untracked work).
Pre-commit check: `<higson.studio.token>` empty, 0 JWTs in `pom.xml` — the remote is public.
`e2e/README.md` capture-commit row filled in afterwards, with rebuild instructions and the two
reproduction caveats (snapshot not in git; `npm run build` at the repo root re-dirties
`styles.min.css`).

---

#### Group 3: Scaffold the Angular workspace and prove the packaging chain

**From Implementation Plan**:
- [x] `standards/frontend/build.md` — `npm ci && ng build`; exact pins on every `@angular/*`, `rxjs`,
      `typescript`; carets only for peripheral `less`/`prettier`/`jsdom`/`vitest`; nothing generated
      committed
- [x] `standards/frontend/services.md` — the no-base-element decision is what keeps the relative-URL
      rule intact with zero exceptions; `proxy.conf.json` mirrors the four backend roots
- [x] `standards/frontend/css.md` — LESS is the styling language; `theme.scss` is the single
      recorded Sass exception, listed first so LESS wins the cascade
- [x] `standards/deployment/packaging.md` — `finalName` untouched; `all_in_one` still ships the UI
      after `repackage`
- [x] `standards/deployment/docker.md` — `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh`,
      `docker/app.properties` **byte-identical**; port 48080 in all four places
- [x] `standards/frontend/components.md` — deliberately **not** used as a template (legacy-scoped)
- [x] `standards/global/security.md` — token still empty, 0 JWTs

**From INDEX.md**:
- [x] `standards/global/commenting.md` — every non-obvious build/config entry says *why*
- [x] `standards/global/minimal-implementation.md` — dropped the scaffolded `@angular/router`
      (ND-7), the `watch`/`ng` scripts and the welcome-page template
- [x] `standards/global/conventions.md` — scaffolded `frontend/.vscode/` removed
- [x] `standards/global/coding-style.md` — tabs everywhere; `.editorconfig` and `.prettierrc`
      switched to tabs so generated code follows suit
- [x] `standards/global/documentation.md` — `frontend/README.md` rewritten in English

**Discovered During Execution**:
- [x] `standards/testing/test-writing.md` — step 3.6, deciding what to do with the scaffolded
      `app.spec.ts`; trimmed to one behaviour-describing test rather than deleted
- [x] `standards/higson/snapshot.md` — before running the WAR; `src/main/higson/` untracked and
      unmodified

---

## 2026-08-07 — Group 3 Complete (SUCCESS)

**Steps**: 3.1 through 3.9. **Tests**: 7/7 packaging checks pass, plus every acceptance grep.
`ng test` 1/1 green.

**Files**: 23 new files under `frontend/`; `pom.xml` and `.gitignore` modified. Nothing under
`src/main/webapp/`, `less/`, `gulpfile.js` or the root `package.json` was touched.

**Packaging chain proven**: one WAR, three context paths (`/demo`, `/`, `/app`), no rebuild between
them — index and `quote` both 200, every asset referenced by the *served* page fetches 200, and a
real browser at `/app` renders with **0 console messages**. The AngularJS app was independently
re-verified as still building and serving from a `HEAD` `pom.xml`.

**Notes / decisions**:

1. **`@angular/animations` was needed and added — with a full 2×2 matrix rather than a guess.**
   The first `ng build` failed on `Could not resolve "@angular/animations/browser"`. Probing with a
   temporary animation-heavy component and varying two factors independently:

   | `provideAnimationsAsync()` | `@angular/animations` | `ng build` |
   |---|---|---|
   | present | absent | **FAIL** |
   | present | present | OK |
   | absent | present | OK |
   | absent | absent | **OK** |

   `@angular/material@22.1.1` does **not** peer-depend on `@angular/animations`, so `spec.md` §4.5's
   *"required by Angular Material"* no longer holds for v22, and the bottom row shows the provider
   could be dropped entirely. **The plan was implemented as written** (provider kept, animations
   pinned at 22.1.0, splitting into a lazy 67.75 kB chunk). ⏭️ **Decision deferred to G8/G9**, when
   real Material components exist and runtime animation behaviour can actually be judged.
   This is the differential-experiment discipline the G1 incident called for.

2. **`maven-clean-plugin` `<includes>` deviation** — see the annotation on step 3.7.

3. **`.gitignore` friction window, open until G12.** Step 3.8 removes `src/main/webapp/vendors/`
   from the ignore list — a G12 fact applied in G3. The directory still exists in the source tree
   and `mvn clean` no longer wipes it, so anyone running the legacy root build now leaves ~570
   untracked files. The proof-run's generated `vendors/` was moved out of the tree so the clean-tree
   check is genuine, but the window stays open.

4. **Plan step 3.8's `e2e/test-results/` and `e2e/playwright-report/` entries were not added** —
   G2 already committed `e2e/.gitignore` containing both; verified ignored. Duplicating them would
   violate `minimal-implementation.md`.

5. **Scaffold facts worth recording**: Angular CLI 22.1.3 does **not** emit `"strict": true` despite
   `--strict` defaulting to true — only the individual `noImplicit*` flags — so `"strict"` and
   `"strictTemplates"` were added explicitly. The CLI also scaffolds `@angular/router` even with
   `--routing=false` (removed, ND-7) and a `.vscode/` directory (removed).

6. Two checks initially failed on the subagent's **own comment text** — `grep "<base"` and
   `grep "LOCALE_ID\|registerLocaleData"` are literal in the plan and the explanatory comments
   contained those exact strings. Comments reworded; the plan's greps now work verbatim.

---

## 2026-08-07 — Group 4 Complete (SUCCESS)

⚠️ **The G4 subagent was terminated by a session limit** immediately before its acceptance-criteria
greps. The implementation was already complete on disk; the main agent finished the verification
directly rather than re-running the group.

**Steps**: 4.1 through 4.10.
**Tests**: **7 passed / 5 files** — 6 for G4 plus the G3 scaffold test.

| Test | Covers |
|---|---|
| writes a message into the store for a failed PUT and issues no retry | DR-06 |
| bumps the reload trigger on recalculate and refetches the quote exactly once | recalculate path |
| **reloads the quote only for a model change — year, make and type do not** | **DR-13 / ND-1** |
| fetches the driver exactly once and shares it between consumers | DR-09 |
| returns the counter to zero on success and on error | spinner interceptor |
| sends string PUTs verbatim and sends birthDate as a Date instance | AC-C2 / AC-C3 |

**Acceptance greps, all verified independently by the main agent** — AC-C2 (no `JSON.stringify`) 0,
AC-C4 (no leading-slash URLs) 0, AC-C6 (no `response.data` unwrap) 0, AC-C5 (`HttpClient` confined
to `services/`) 0, AC-S3 (no `BehaviorSubject`) 0, plus an extra DR-05 check for any client-side sort
0. AC-C3 confirmed by signature: `updateBirthDate(birthDate: Date)`.

**Files**: 19 new files under `frontend/src/app/` — 5 models, 4 services, `quote-store.ts`,
2 interceptors, `pending-requests.ts`, `iso-date.adapter.ts`, 4 spec files; plus `proxy.conf.json`.

### DR-13 / ND-1 encoding — the thing this group existed to get right

Encoded **as data, not as component discipline**, exactly as the plan demanded:

```ts
const RELOADS_QUOTE: Record<VehicleField, boolean> = {
    productionYear: false, make: false, type: false, model: true,
};
```

The record is **exhaustive by construction** — adding a member to the `VehicleField` union is a
compile error until someone decides, explicitly and in that one place, whether it reloads.
`vehicleFieldChanged(field)` is the only path, so a component that routes all four fields through a
single handler cannot reintroduce the loss. The comment names DR-13 and states that a "tidy" rewrite
recalculating on every vehicle change is a **failed migration, not an improvement**.

**DR-09** and **DR-07** are satisfied by construction too: one shared `driver` resource replaces the
two startup fetches, and on a failed load `quote.value()` is `undefined`, so stale variant cards
cannot survive underneath the error banner. No client-side quote model exists (G-D4) — the backend's
session-scoped bean remains the only one.

**Notes**:

1. ⚠️ **The plan's test command is wrong and must be corrected in later groups.** Raw
   `npx vitest run` fails with `ReferenceError: describe is not defined` — the Angular CLI supplies
   the Vitest environment. Use **`npx ng test --watch=false`** (or `npm test`).
2. `proxy.conf.json` proxies the four backend roots to `localhost:48080` same-origin. The dev backend
   must run at the **root** context so `JSESSIONID` gets `Path=/`; otherwise `cookiePathRewrite` is
   mandatory or the quote resets on every keystroke.

---

## 2026-08-07 — Groups 5 and 6 Complete (SUCCESS, run in parallel)

Both groups verified their own work by **mutation testing** rather than trusting a first-run pass.

### Group 5 — presentational components (5 tests; 10 green including re-run core)

**Standards** — from plan: `frontend/services.md` (no component injects `HttpClient`),
`frontend/css.md` (no second Sass file), `frontend/accessibility.md` (`role="alert"`; popover
reachable by keyboard, not hover-only), `frontend/components.md` (behaviour only). From INDEX.md:
`global/coding-style.md`, `commenting.md`, `minimal-implementation.md`, `security.md`,
`validation.md`. Discovered: `backend/models.md` + `CoverageDto.java` (nullability),
`testing/test-writing.md`.

**Falsifiability** — each headline test was verified to fail under the mutation it exists to catch:
reintroducing a sort → DR-05 test fails; `hasValue()` → `value()?...` → DR-07 test fails;
`[innerHTML]` → interpolation → popover test fails.

**DR-05 proof is genuinely adversarial**: the fixture is **deliberately scrambled** (`GOLD` before
`BRONZE`, coverage positions `[9,1]`, discount positions `[5,2]`). A golden-shaped fixture would
have passed with or without a sort and proven nothing.

Rows landed: DR-05, DR-06, DR-07, DR-10, DR-11, DR-14 (client half), DR-15, DR-18, C-8/AC-S8.

### ⛔ Group 5's most important finding — the spec is wrong about Angular 22 resources

`spec.md` §4.7 and plan step 5.4 both specify `store.quote.value()?.options ?? []`, and my own
briefing repeated the claim that "on failure `quote.value()` is `undefined`". **All three are
wrong.** `value()` **throws `ResourceValueError`** in the error state — proven by a full 2×4
differential over `defaultValue` × reader API, and confirmed in the framework source. DR-07 still
holds, but via `hasValue()`.

The dangerous part: **the DR-07 test passed anyway.** A template that throws renders zero cards, and
"zero cards" was all the test asserted. A recording `ErrorHandler` assertion now makes that
accidental pass a failure. This is the second time a test has looked green for the wrong reason —
the first was G1's snapshot engine. Correct `spec.md` §4.7 in G13.

### Group 6 — vehicle cascade and driver history (6 tests, all green)

**Standards** — from plan: `frontend/services.md`, `css.md`, `accessibility.md` (**every one of the
7 controls now has a real accessible name via `<mat-label>`**, which the AngularJS sibling-`div`
labels never provided; `data-testid` hooks kept byte-identical to the goldens),
`components.md` (behaviour only). From INDEX.md: `global/coding-style.md`, `commenting.md`,
`minimal-implementation.md` (no `<form>`, no `Validators`, no `mat-error` — ND-4), `validation.md`,
`testing/test-writing.md`. Discovered: `backend/api.md` (single-field PUT), `backend/models.md`
(primitive `int` is what makes the falsy-`0` cascade guards safe).

**5-mutant differential, one factor at a time, restored between each:**

| Mutation | Caught by |
|---|---|
| `vehicleFieldChanged('type')` → `recalculate()` | DR-13 test |
| DR-01 guard removed | **compile time** — under `strict: true` the unguarded `[0].name` port does not typecheck |
| dictionaries left stale | DR-01 + DR-12 assertions |
| `(blur)` → `(input)` | DR-03 test |
| `Number.isFinite`/`min` guard removed | DR-02 test |

Rows landed: DR-01, DR-02, DR-03, DR-09, DR-12, DR-13.

### Decisions taken on the flagged items

1. **G6 cleared the model dictionary on a *make* change too** — one step beyond DR-12's literal
   text, making "clearing a level clears its dictionary" a uniform invariant. **Accepted.** No
   golden observes the state between the make and type steps, so `vehicle-cascade.spec.ts` is
   unaffected either way, and the alternative is a special case that exists only to preserve a bug
   the register already sanctions fixing.
2. **G6 applied the blank-placeholder guard to all four cascade levels**, not just Make (DR-01).
   **Accepted** — blanking Production Year in the current build sends an empty PUT, which is the
   DR-02 500 in another shape. All four null branches stay local-only: clear the level and its
   dependents, no PUT, no reload.
3. **G5 removed the empty `<thead><tr></tr></thead>` from `discount.html`** — dead markup. Costs one
   blank line in the `renderedVariantCards` golden; **traceable to C-9**, so it is an explained diff
   in G11, not a regression. G5 deliberately **kept** the single-row `<table>` rather than a `<dl>`
   or flex row, so the golden's `Good Driver\t-20` tab separator survives — a more semantic
   construct would have produced a larger, less explicable diff.
4. **G4's `models/quote.ts` nullability was wrong** and G5 widened it. `limitRight` and `description`
   are nullable `BigDecimal`/`String` on the wire (`CoverageDto.java:11,13`); the golden records six
   `limitRight: null`, and `e2e/specs/quote-panel.spec.ts` already typed them nullable. This aligns
   the app with the E2E contract.
5. **OnPush best-practice conflict, resolved toward the project.** The Angular MCP best-practices for
   this workspace say *"do NOT set `changeDetection: OnPush` explicitly — it is the default in v22+"*,
   while AC-S1 requires every component to set it. AC-S1 followed (documentation priority beats
   general best practice). G13 candidate: restate AC-S1 as "no component opts *out* of OnPush".

### ⚠️ Carry-forward for G7 — a request-volume trap the register does not name

`ng-change` fires **only when the model actually changes**, so blurring an untouched field costs
nothing today. A bare `(blur)` handler would PUT on every blur — a request-volume **increase**
traceable to no DR row, which would surface in G11 as an unexplainable golden diff. G6 added a
`value === field()` check in `commit()`. **G7's seven text fields have the identical exposure and
need the same guard.**

### Tooling corrections discovered

- `ng test --include=<dir>` is **broken in this workspace** — it drops the Angular compiler plugin
  and fails with *"No loader is configured for .less files"*. Use `--filter="^<Suite name>"`.
- Two acceptance greps are written too loosely to pass as stated: **AC-C5** (`HttpClient` under
  `components/`) also matches `provideHttpClient`/`HttpTestingController` in *spec* files — needs
  `--exclude="*.spec.ts"`; **DR-10**'s `grep '\$\|zł\|PLN'` matches every TS template literal and
  `$event` — needs narrowing to production templates. Restricted properly, both are empty.
- **Parallel-agent hazard**: G5 briefly left a non-compiling probe spec under `src/`, which broke the
  shared build for G6. Probes must live outside `src/**` or be written compile-clean.

### ⚠️ Known-unfinished, for G9

**G5 created no `.less` files at all** (the plan defers panel and card styling to G9, and empty stubs
would violate `minimal-implementation.md`). Consequence: **the popover panel currently has no
background, border or `min-width: 340px`** — it renders as unstyled text over the page. Functionally
correct, visually unfinished. `less/config.less:22-42` must be **rewritten** for the CDK panel rather
than ported (G-E5).

---

## 2026-08-07 — Group 7 Complete (SUCCESS)

**Steps**: 7.1 through 7.7, plus a check that step 4.8's `IsoDateAdapter` / `MOTOR_DATE_FORMATS`
provision was already in `app.config.ts` from G4 — it was, so nothing was added.

**Tests**: 6 passed (2 adapter, 4 component). Full workspace suite **24 passed / 12 files**.
`ng build` production green.

**9-mutant differential, every one caught**, sources restored byte-identical afterwards: adding a
`(dateInput)` commit, dropping the `(closed)` binding, dropping the `sameDate` guard, passing
`toISOString()` instead of the `Date`, moving `(change)` onto each toggle, setting `gender` inside
the subscribe callback, dropping the `value === field()` guard, `JSON.stringify`ing the body, and
`getFirstDayOfWeek() → 0`.

**Rows landed**: DR-04 (3 PUTs → exactly 1 per date entry), DR-08 (re-click → 0 + 0, satisfied by
construction — `MatButtonToggleGroup` only emits on an actual change), DR-08 ordering (value set
*before* the PUT), DR-17, DR-19, DR-27, ND-3, ND-4, ND-5, DR-09, plus G6's carry-forward unchanged
guard on all seven fields.

**Datepicker parity**: `yyyy-MM-dd` display/parse, alternative `yyyy/MM/dd` (backreferenced, so
`1990-05/12` is rejected), **Monday-first asserted on a rendered `<mat-calendar>`** rather than only
on the adapter, exactly 7 header columns so no week-number column exists, no button bar,
`<mat-datepicker-toggle>` replacing the glyphicon. Timezone conversion goes through
`dateAdapter.deserialize()` — never hand-rolled — asserted end-to-end:
`'1986-08-06T00:00:00.000+00:00'` renders as `1986-08-06`.

**Decisions on the flagged items**:

1. **DR-27 deviation in letter, satisfied in spirit — accepted.** The plan wanted the
   `is-open="popup.opened"` binding to become "a single component signal". With
   `<mat-datepicker-toggle>` the open state lives entirely inside `MatDatepicker`; the component
   holds none, and a signal bound to nothing would be dead code. The criterion that matters — no
   dual-scope binding, one owner of the open state — is met more strongly.
2. **An extra guard not on the register — accepted.** `commitBirthDate` returns early on `null` or
   an invalid `Date`, because `Date.prototype.toJSON` renders an invalid date as `null` and an
   unguarded commit would PUT a null birth date — the DR-02 failure in another shape, equally
   silent. Changes request volume only when typing garbage into the date field, which no golden
   covers.

### ⚠️ Defect found in G6's work and fixed immediately by the main agent

G7 noticed `driver-history.ts` still read `store.driver.value()?.…` on three lines. Given G5's
finding — a resource in the error state **throws** `ResourceValueError` rather than returning
`undefined` — those lines would have crashed the component the moment `GET driver` failed, instead
of degrading.

Fixed directly: `private readonly driver = computed(() => hasValue() ? value() : undefined)`, with
the three `linkedSignal`s reading through it and a comment explaining why a bare `value()` is wrong.
Verified: **24 tests / 12 files pass**, `ng build` green.

Third time a plausible-looking construct proved wrong in a way the tests did not catch.
**`hasValue()` should become an explicit frontend standard in G13.**

**Tooling notes**: `ApplicationRef.whenStable()` **deadlocks** here — after flushing a mutation,
`store.recalculate()` leaves a `GET quote` outstanding in the testing backend, so it never resolves
(three tests timed out at 5s). Use `TestBed.tick()` + `httpTesting.match('quote')`, the pattern G6
established. Add to the frontend test standard in G13.

**⚠️ For G11**: the plan's code-shape greps need `--include='*.ts' --exclude='*.spec.ts'` scoping, or
they read as failed for reasons that are test infrastructure rather than production code —
`provideHttpClient` appears in four spec files, and explanatory comments legitimately contain tokens
like `<form>` and `Validators`.

---

## 2026-08-07 — Group 8 Complete (SUCCESS)

**Tests**: **27 passed / 14 files** (baseline 24/12; +4 new, −1 replaced placeholder).
**11 mutants, 11 caught**, every one by a test assertion rather than a build failure — including
`finalize` → `tap` on the interceptor (proves the "and on error" half), the ND-2 "Documents" label,
sidebar widget order, DR-16's `toggleSidebar()` being ported, the `main.js` relay being ported, and
a second independent `GET driver` being reintroduced.

**End-to-end, the first group where the whole screen exists**: one WAR, three context paths
(`/demo`, `/`, `/app`), only `--server.servlet.context-path` varied. Real Chromium at all three —
**0 console messages, 0 failed requests**. Startup tally matches the golden's `post` branch exactly:
`GET driver` = **1** (pre records 2 — DR-09 delivered, not reintroduced), vehicle 1, quote 1, four
dictionaries 1 each, nothing off-contract. Spinner proven live with `**/quote` held back 2.5 s:
visible while in flight with `role="status"` / `aria-busy`, gone once the counter returns to zero.
`git status src/main/webapp/` empty — `styles.min.css` was **not** re-dirtied, confirming G3's note.

**Rows**: DR-20 (`app-root`), DR-16 (dead collapse code not ported), ND-6 (spinner flash **not**
reproduced, no artificial delay), ND-2, ND-3, ND-7, ND-8, AC-C8.

**Notes**: the shell gained a real heading structure (`h1`/`h2`) where the AngularJS build had **no
headings at all**. A new bundle-budget warning appeared — 671 kB initial vs the 500 kB warn line —
quantified by a one-factor differential against the G3 placeholder; `angular.json` deliberately not
touched. Two traps recorded: `HttpTestingController.match()` **consumes**, so a test asserting
"requests are in flight" that way never flushes them and passes for the wrong reason; and the port
needs `lsof -ti :48080 | xargs kill -9` between server restarts or `curl` answers from a shutting-down
instance.

---

## 2026-08-10 — Group 9 Complete (SUCCESS)

**Tests**: 3 checks — static script 12/12, 2 Playwright — plus the full suite **27/14 unchanged**.
`mvn clean package -Pall_in_one` green.

**Visual gates met at all three context paths**: 18 responses, 10 assets, **zero 4xx/5xx, zero
network failures, zero console errors/warnings**. Live computed-style verification: body Manrope
13px with the diagonal gradient, grid `346px 1038px` at 1440 (exactly 25/75), card radius 8px,
popover `min-width 340px` with the 1px `rgb(76,76,222)` border and the SVG arrow.
`document.fonts` shows latin **loaded** and latin-ext **unloaded** — `unicode-range` working.

**⚠️ Plan step 9.5's asset location is wrong, proven by a 7-row differential.** Assets in
`frontend/public/` do **not** get their `url()` rewritten — it is a hard build error, and the only
`public/` variant that builds emits a **leading-slash** URL that breaks every non-root context path.
`url()` resolves relative to **the partial that wrote it**, so the assets live under
`frontend/src/styles/` and get content-hashed into `media/` with relative references. **Zero config
change.**

**Assets**: 6 SVGs + `favicon.ico` carried (the favicon *replaced* the CLI's scaffolded default);
Manrope self-hosted as **two** woff2 files, not one — `index.html` declares `lang="pl"` and the
Polish diacritics live in latin-ext, so a latin-only file would silently fall back to a system font
for the language the document claims. `unicode-range` means the second downloads only when needed.
Net **−96 tracked binaries, +2**. Dropped: `arrow.svg` (its only call site was the custom select
chevron; `mat-select` draws its own), `logo.png`, `select-arrow.png` (DR-24).

**Dead CSS deleted rather than ported** — 13 blocks, each recorded with its source line. The find
worth keeping: `less/sidebar/sidebar.less:45-47` compiles to
`.sidebar-container__divideraddress` (`&address` instead of `&.address`) and **has never matched an
element** — which is *why* `.address`'s `width: 24px` was beating the divider's `width: 100%` and
squashing the whole Address divider to a 24px box. Fixed at the cause. Also: the `outline: none`
block was **not** ported (it would violate `accessibility.md`), and the coverage row — now a tab stop
because C-8 made the popover keyboard-reachable — gained a `:focus-visible` ring.

**Contrast checked, not assumed**: `#4C4CDE` on white ≈ 6.2:1, `#333` ≈ 12.6:1, both past AA;
nothing renders directly on the blue gradient.

**Bundle budget: warning accepted, `angular.json` untouched.** G9 added 9.47 kB — about **5%** of the
180.86 kB overrun; the rest is Material + CDK + datepicker. Raising `maximumWarning` here would erase
the signal without anyone examining the 95%. **G13 decides with these numbers.**

**Two checks corrected rather than the code**: the `cursor: default` criterion asserts a string the
browser reports as `auto` (no declaration → initial value, identical arrow) — adding a declaration
that changes not one pixel, in the group whose job is deleting exactly that, would be absurd; and
the dead-CSS greps flagged this group's own documentation of what it deleted, so the script blanks
comments before matching.

---

## 2026-08-10 — Group 10 Complete (SUCCESS)

⚠️ A first attempt was terminated by an API error before it changed anything; re-run clean.

**The only Java change in the migration.** `@ControllerAdvice` → `@RestControllerAdvice`, both
handlers returning a new `ErrorDto`.

**Red-then-green, the proof the test is load-bearing rather than tautological:**

- **Red** — `expected: <null> but was: <Driver first name not supplied.>` on the `forwardedUrl`
  assertion. MockMvc's dump confirms the mechanism: `View name = Driver first name not supplied.`,
  `Body = ` empty, `Status = 400`.
- **Green** — 2 run, 0 failures. Full Java suite **11 run, 0 failures**.

**G1's MockMvc warning proved exactly right and it mattered**: the status was **400 in both states**,
so a test asserting only the status would have passed *before* the fix. The assertions that actually
flipped are `forwardedUrl` (message → `null`) and the body.

**Live contract, captured empirically at both ends:**

| | status | body |
|---|---|---|
| before | `404` | `{"timestamp":…,"status":404,"error":"Not Found","path":"/demo/quote"}` — **no `message` key** |
| after | `400` | `{"message":"Driver first name not supplied."}` |

The second handler (no test covers it) was exercised too, to confirm the fix is not first-name
specific: `PUT vehicle/productionYear 0` → `400 {"message":"Vehicle production year not supplied."}`.
`server.error.include-message` stayed at its `never` default — no property change was needed or made.

**Golden update**: `QuoteApiTest`'s pre-fix method replaced by
`quoteReturnsFourHundredWithValidationMessage()`, citing DR-14 / C-6 / spec §6.4. The `e2e/`
DR-14 branch **already encoded the post behaviour correctly** and was left byte-identical — which is
precisely the point of the `MIGRATION_STAGE` mechanism. `goldens/pre/` untouched;
`goldens/post/error-paths.json` captured.

**Scope discrepancy resolved in favour of the plan**: my briefing said "exactly one file"; plan step
10.2 and AC-C8 say **two paths** — the handler *and* a new `api/dto/ErrorDto.java`. The plan wins,
because the mandated contract is unreachable without the DTO: `@RestControllerAdvice` returning
`String` yields a bare `text/plain` body, not `{"message": …}`, and returning a `Map` would violate
`api.md`. Both statements are literally true of the tree, since `git diff` does not list an untracked
file.

---
