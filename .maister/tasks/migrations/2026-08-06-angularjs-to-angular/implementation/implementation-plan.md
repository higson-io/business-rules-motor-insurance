# Implementation Plan — AngularJS 1.8.3 → Angular 22

**Task**: `.maister/tasks/migrations/2026-08-06-angularjs-to-angular`
**Type**: migration / `migration_type: code`
**Contract**: `implementation/spec.md` (13 sections, DR-01…DR-27, ND-1…ND-8, 21-endpoint REST freeze)
**Rollback authority**: `analysis/rollback-plan.md` — this plan does **not** restate it; every group cites a row of its §5
**Date**: 2026-08-06

---

## Overview

| | |
|---|---|
| Task groups | **14** (G0 … G13) |
| Total steps | **142** |
| Expected tests | **~62** — 8 MockMvc contract (G1) + 6 Playwright flows (G2) + ~34 Vitest/assertion tests (G3-G10) + 1 contract flip (G10) + 6 golden replays (G11) + 7 build/hygiene checks (G12) |
| Effort | 2 × S · 5 × M · 7 × L |
| Risk | medium-high |
| Parity definition | **functional + contractual only.** Appearance is out of scope (C-9). A visual difference is never, on its own, a finding. |

### The three things that make this plan not a normal implementation plan

1. **P-3 is absolute.** Groups G1 and G2 record runtime evidence against the *current* AngularJS build.
   A `git revert` restores code; it cannot recreate a runtime capture (`rollback-plan.md` §4.4).
   **Nothing under `src/main/webapp/`, `less/`, `gulpfile.js` or the root `package.json` may be deleted
   or modified — beyond the additive `data-testid` attributes of step 2.2 — until G2 ends with
   `git tag pre-angular-migration`.**
2. **The defect register is a whitelist, not a wishlist.** DR-01…DR-27 are the *complete* set of
   sanctioned deviations. Anything else that differs from the parity checklist is a regression, and
   ND-1…ND-8 are eight things that look like bugs and must survive untouched.
3. **DR-13 / ND-1 is the trap that fails the migration silently.** Only a vehicle **model** change
   triggers a quote reload. Production year, make and type do not. An idiomatic reactive rewrite
   reconnects them *by accident*. See G4 step 4.4 and G6 step 6.6 — it gets its own unit test and its
   own E2E assertion, and it is the one golden assertion that must be **byte-identical pre and post**.

---

## Group ↔ rollback-plan mapping

`analysis/rollback-plan.md` §5 defines 11 rollback points. This plan splits three of them for
test-granularity (2-8 tests per group). The mapping is 1:1 or n:1 — no rollback point is lost.

| This plan | `rollback-plan.md` §5 row | Gate to exit (verbatim from the rollback plan) |
|---|---|---|
| G0 | 0 — Prerequisites | `node -v` in range; `mvn test` compiles and `SnapshotDataSourceDemoTest` still runs |
| G1, G2 | 1 — **GOLDEN CAPTURE (hard gate)** | 21 API shapes pinned; 6 E2E flows recorded; tag created |
| G3 | 2 — Scaffold & packaging | `-Pall_in_one` WAR serves at `/demo`, `/` and `/app` |
| G4 | 3 — Store, interceptors, services | asymmetry encoded; spinner live; zero `.data` unwraps |
| G5, G6, G7 | 4 — Components, leaves first | each component's parity-checklist section passes |
| G8 | 5 — Shell & Material theme | single screen renders end-to-end at all three context paths |
| G9 | 6 — Styles & assets | no 404s on fonts/images; screenshot diff **reviewed**, not gated |
| G10 | 7 — Backend fix | contract test asserts 400 + `{"message": …}`; golden diff cites `DR-14` |
| G11 | 8 — Parity verification | every diff traceable to C-9 or a DR-nn row |
| G12 | 9 — **CUTOVER — deletion** | `mvn clean package -Pall_in_one` green from a clean clone |
| G13 | 10 — Docs & standards | `INDEX.md` regenerated |

---

## Execution order

```
G0  Toolchain & test deps            [S]  ── blocking, no dependencies
 │
 ├──────────────┬───────────────────────────────────────────────────┐
 │              │                                                   │
G1 MockMvc      G2 Playwright goldens + data-testid hooks           │  ← G1 ∥ G2
   goldens [L]     [L]  ends with: git tag pre-angular-migration    │
 │              │                                                   │
 └──────┬───────┘                                                   │
        │  ══════════ P-3 HARD GATE CLEARED ══════════              │
        ▼                                                           │
G3  Scaffold & packaging             [M]  ← proves the WAR ships UI │
        │                                  BEFORE any component     │
        ▼                                                           │
G4  Store · interceptors · services  [M]  ← DR-13 encoded here      │
        │                                                           │
        ├────────────────┬──────────────────┐                       │
        ▼                ▼                  │                       │
G5 Presentational    G6 Vehicle +           │   ← G5 ∥ G6           │
   + variants [M]       driver-history [L]  │                       │
        │                │                  │                       │
        └────────┬───────┘                  │                       │
                 ▼                          │                       │
G7  Driver + datepicker              [L]  ← deliberately LAST       │
                 │                            (highest risk)        │
                 ▼                                                  │
G8  Shell · sidebar · spinner        [M]                            │
                 │                                                  │
        ┌────────┴────────┐                                         │
        ▼                 ▼                                         │
G9 Styles & assets   G10 Backend C-6 / DR-14   ← G9 ∥ G10           │
        [M]              [S]                                        │
        └────────┬────────┘                                         │
                 ▼                                                  │
G11 Parity verification — golden replay  [M]  ← verdict, no commit  │
                 │                                                  │
                 ▼                                                  │
G12 CUTOVER — deletion               [S]  ⚠️ point of no easy return│
                 │                                                  │
                 ▼                                                  │
G13 Docs & standards                 [S]  ◄─────────────────────────┘
                                          (may start any time after G11)
```

**Parallelisable clusters**

| Cluster | Groups | Why they are independent |
|---|---|---|
| Golden capture | **G1 ∥ G2** | separate harnesses — Java/MockMvc vs Node/Playwright. Only G2 touches the AngularJS templates |
| Component leaves | **G5 ∥ G6** | disjoint component trees and disjoint services; both depend only on G4 |
| Cosmetics vs backend | **G9 ∥ G10** | `frontend/src/styles/**` vs one Java file — zero file overlap |
| Documentation | **G13** | may run concurrently with G11/G12; documentation-only, revert freely |

**Deliberately NOT parallel**

- **G7 after G5/G6.** Technically G7 only depends on G4, but `target-state-plan.md` §7 makes the
  datepicker (G-C7/G-F2, the single highest-risk item) **last** so a rollback there does not unwind
  everything behind it.
- **G3 before any component.** Anti-sequencing rule: prove the WAR ships the UI at three context
  paths with a placeholder `app-root` before writing a line of application code.
- **G12 after G11.** The temptation is to delete as you go. `rollback-plan.md` §5 keeps deletion as a
  separate, isolated, final commit precisely for this reason.

---

## Implementation Steps

---

### Task Group 0: Toolchain & test-dependency prerequisites (P-1, P-2, P-4)

**Dependencies:** None
**Effort:** S (≤ 0.5 d)
**Rollback:** `rollback-plan.md` §5 row 0. Node is a machine-level change (C-5: local only); the pom
edit is 8 lines. Cost of rollback: nothing.
**Blocks:** everything. **P-1 is blocking: no `ng` or `npm` command anywhere in this plan may run
before step 0.1 verifies green.**

- [x] **0.0 Clear the three hard prerequisites and pin the test harness timezone**

  - [x] **0.1 P-1 — bump Node locally. MANUAL STEP, nothing is committed.**
    - ✅ DONE 2026-08-06: `brew install node@22` → **Node 22.23.2 / npm 10.9.8**, satisfies
      `^22.22.3`. ⚠️ **KEG-ONLY** at `/opt/homebrew/opt/node@22/bin`, NOT on the default PATH —
      bare `node -v` still prints `v23.6.1`. Every `node`/`npm`/`npx`/`mvn` command in every later
      group MUST be prefixed with `export PATH="/opt/homebrew/opt/node@22/bin:$PATH"`.
      `~/.zshrc` deliberately not modified (user declined). Nothing committed (C-5).
    - Machine is on **`v23.6.1`** (verified). Angular 22 declares
      `engines.node = "^22.22.3 || ^24.15.0 || >=26.0.0"` — 23.x is rejected.
    - ⚠️ **The failure is opaque**: it surfaces inside Maven `prepare-package` as an npm error with no
      mention of Node, which is why this is stage 0 and not a footnote.
    - Install `24.19.0` (the pin recorded in `spec.md` §4.1) by whatever mechanism the machine uses
      (`nvm install 24.19.0 && nvm use 24.19.0`, Homebrew, or the official installer).
    - ⛔ **C-5: commit nothing for this.** No `.nvmrc`. No `frontend-maven-plugin`. The only committed
      enforcement is `frontend/.npmrc` (`engine-strict=true`) + `engines` in `frontend/package.json`,
      both of which land in G3.
    - **Verification command (must print a version in range before anything else proceeds):**
      ```bash
      node -v   # expect v24.19.0 (or ^22.22.3 / >=26) — NOT v23.x
      npm -v
      ```

  - [~] **0.2 P-2 — populate the Higson snapshot** — SKIPPED (command), SATISFIED (requirement).
    `mvn higson-cli:pull` fails: *"Either higsonToken or both higsonUser and higsonPassword must be
    provided"*. Studio IS reachable (`localhost:38088`, container `higson-demo-local-studio`); the
    failure is purely credentials — `<higson.studio.token>` is deliberately empty per
    `security.md`, no `higson` server in `~/.m2/settings.xml`, no `HIGSON*` env var. The plugin
    failed **before writing**, so the snapshot is byte-identical to before (backup taken first).
    P-2's actual requirement is met: snapshot populated with **171 files** (81 `@params`,
    36 `@functions`, 9 `@tests`) and proven **functional** — `SnapshotDataSourceDemoTest` boots the
    engine off `src/main/higson` and reads parameter rows in all 3 tests.
    - `src/main/higson/` is **not tracked in git** (`standards/higson/snapshot.md`); Studio is the
      system of record. Without rules the app boots but `GET quote` returns nothing useful and **no
      E2E golden can be captured**.
    - ```bash
      mvn higson-cli:pull
      ls src/main/higson/@params src/main/higson/@functions   # expect populated
      ```
    - ⛔ Out of scope (`spec.md` §11): no rule, function or decision-table edit. Pull only.

  - [x] **0.3 `pom.xml` edit (4) part A — resolve the JUnit 4 / JUnit 5 conflict**
    - REMOVE `junit:junit:4.13.2` (`pom.xml:73-78`).
    - ADD `org.springframework.boot:spring-boot-starter-test` (scope `test`) — brings JUnit 5,
      AssertJ and `spring-test`/MockMvc, which G1 needs.
    - `testing/test-writing.md` binds this: *"drop JUnit 4 and add `spring-boot-starter-test`; do not
      add new JUnit 4 tests."*
    - The existing untracked `src/test/java/pl/decerto/higson/demo/motor/SnapshotDataSourceDemoTest.java`
      uses the Jupiter API today with **no Jupiter dependency declared** — it must still compile after
      this change.

  - [x] **0.4 `pom.xml` edit (4) part B — P-4, pin the test JVM timezone**
    - Add a `maven-surefire-plugin` block (**version inherited — do not pin**) with
      `<argLine>-Duser.timezone=UTC</argLine>`, commented with the P-4 rationale.
    - Reason: `Motor.createDriver` seeds "today − 40 years, start-of-day, **system TZ**", so
      `dateOfBirth` carries `+00:00` in the container and `+02:00` on a Europe/Warsaw box. Unpinned,
      every date-touching golden becomes machine-dependent.

  - [x] **0.5 Create the working branch** — `migration/angularjs-to-angular-22` off `techflash/5.8`.
    `<higson.studio.token>` verified **empty** (`grep -c "eyJ"` → 0); safe to commit.
    - `git checkout -b migration/angularjs-to-angular-22` off `techflash/5.8`
      (`rollback-plan.md` §1). All migration work lands here; nothing on `master` until G11 passes.
    - ⚠️ Before committing `pom.xml` at any point in this migration, verify
      `<higson.studio.token>` (`pom.xml:31`) is still **empty** (`standards/global/security.md` —
      ACTION REQUIRED; the remote is public).

  - [x] **0.6 Archive the AngularJS-era `node_modules` escape hatch** (`rollback-plan.md` §4.3)
    — `~/motor-insurance-angularjs-node_modules.tgz`, 11 MB, 5810 entries (542 under `vendors/`).
    ⚠️ **Deliberate deviation**: the plan's `npm run build` would have regenerated the *tracked*
    `src/main/webapp/styles/styles.min.css` — a P-3 violation before the tag exists. Ran
    `npx gulp prepare:libs` instead: same `vendors/` output, zero tracked files touched
    (`git status --porcelain` on the P-3-frozen paths is empty).
    - ```bash
      npm ci
      tar czf ~/motor-insurance-angularjs-node_modules.tgz node_modules src/main/webapp/vendors
      ```
    - ⛔ **Deliberately not committed** — `build.md` forbids committing `node_modules`; this is a
      rollback aid, not a build input. Must exist **before** the root `package-lock.json` is ever
      regenerated (anti-sequencing rule, `rollback-plan.md` §5).

  - [x] **0.7 Run the group-0 verification set** (5 checks — the "tests" for this group)
    — 1 ⚠️ conditional pass (Node in range only with the keg-only path prepended);
    2 ✅ 171 snapshot files; 3 ✅ `mvn -q test-compile` exit 0; 4 ✅ **3 tests run, 0 failures**;
    5 ✅ forked JVM command line literally contains `-Duser.timezone=UTC`.
    1. `node -v` in the Angular 22 range
    2. `mvn higson-cli:pull` succeeded and `src/main/higson/` is populated
    3. `mvn -q test-compile` succeeds with the new dependency set
    4. `mvn -q test -Dtest=SnapshotDataSourceDemoTest` runs (does not error on a missing Jupiter API)
    5. `mvn -q test -Dtest=SnapshotDataSourceDemoTest -X 2>&1 | grep -i "user.timezone=UTC"` — the
       surefire `argLine` is actually applied
    - Do **not** run the full suite; there is no suite yet.

**Acceptance criteria — G0**

- `node -v` reports a version Angular 22 accepts. Nothing about the Node bump is committed (C-5).
- `git diff --name-only` shows exactly one file: `pom.xml`.
- `junit:junit` no longer appears in `pom.xml`; `spring-boot-starter-test` does.
- `SnapshotDataSourceDemoTest` still compiles and runs.
- Surefire runs with `-Duser.timezone=UTC` (P-4, evidence for `spec.md` §8.6 row 1).
- `src/main/higson/` populated (P-2).
- `<higson.studio.token>` is empty (`security.md`).
- `~/motor-insurance-angularjs-node_modules.tgz` exists.

---

### Task Group 1: MockMvc contract goldens against the CURRENT build (P-3, part A)

**Dependencies:** G0
**Effort:** L (> 2 d)
**Parallel with:** G2
**Rollback:** `rollback-plan.md` §5 row 1. Purely additive — new files under `src/test/java/`. Reverting
costs one commit.
**⚠️ P-3 GATE, PART A. This group runs against the unmodified AngularJS build. It touches no
production file.**

Location (`test-writing.md` — *"Test Layout Mirrors the Main Source Package"*):
`src/test/java/pl/decerto/higson/demo/motor/api/`. Classes are **package-private**, named
`<ClassUnderTest>Test` (never `Tests`), method names describe behaviour with **no `test` prefix**,
**every assertion carries a message**, shared fixture in a static `@BeforeAll`.

- [x] **1.0 Freeze the 21-endpoint REST contract as executable goldens**

  - [x] **1.1 Write 8 focused contract tests** (2 per class; the register-critical rows only —
        `spec.md` §8.2 caps this group at 8)

    | # | Class | Test method | Pins |
    |---|---|---|---|
    | 1 | `DriverApiTest` | `driverIsReturnedWithIsoEightSixOhOneBirthDateAndOffset()` | endpoint 5. The **exact** `GET driver` JSON incl. `dateOfBirth` as an **ISO-8601 string with its `+00:00` offset** — ⚠️ *not* epoch millis (PC-3 corrects `current-state-analysis.md:194`). Also `numberOfAccidents`, `numberOfTickets`, `licenceObtainedAtAge`, nested `address` |
    | 2 | `DriverApiTest` | `stringFieldsAcceptRawUnquotedTextPlainBodiesAndReadBackUncorrupted()` | endpoints 7, 8, 9, 13, 14, 15 — each PUT sent as a **raw unquoted body with `Content-Type: text/plain`**, each followed by a `GET driver` read-back proving the value is stored verbatim. ⚠️ **This is the test that catches a stray `JSON.stringify` in G4** — a control body of `"Krakow"` *with* quotes stores `city = '"Krakow"'` |
    | 3 | `DriverApiTest` | `birthDateRequiresJsonQuotedIsoStringAndRejectsTextPlain()` | endpoint 6. `PUT driver/birthDate` with `application/json` + `"1990-05-12T00:00:00.000Z"` → 200; the **same value as `text/plain` → 415**. `DriverApi.java:34-37` is `@RequestBody Date` with `required` **not** set, so the body is mandatory. Proves AC-C3 |
    | 4 | `VehicleApiTest` | `vehicleIsReturnedWithPrimitiveIntFieldsDefaultingToZero()` | endpoint 16. All five fields primitive `int`, **never null, unset = 0** — the property the cascade guards depend on |
    | 5 | `VehicleApiTest` | `updatingAMakeTypeOrProductionYearClearsTheDependentServerSideIds()` | endpoints 17-20 **incl. the server-side cascade side effects**: make → `typeId=0, modelId=0`; type → `modelId=0`; productionYear → `makeId=typeId=modelId=0` |
    | 6 | `DictionaryApiTest` | `dictionaryEntriesExposeCodeAsAString()` | endpoints 1-4. ⚠️ `DictionaryEntryDto.code` is a **String** — the property that forces the explicit `Number()` coercion in G6 |
    | 7 | `QuoteApiTest` | `quoteReturnsCoveragesAndDiscountsAlreadySortedByPosition()` | endpoint 21. `QuoteDto` shape; **coverage and discount `position`s ascending in all three options**. ⚠️ **This is the DR-05 evidence: the backend already sorts, so the client must add NO sort** |
    | 8 | `QuoteApiTest` | `quoteFailureReturnsFourOhFourWithNoMessageKey()` | ⚠️ **Records TODAY'S BROKEN behaviour deliberately**: blank the first name, `GET quote` → **404**, body `{"timestamp":…,"status":404,"error":"Not Found","path":"…"}` with **no `message` key**. Javadoc must say: *"P-3 golden of the pre-C-6 contract. G10/DR-14 replaces this method with `quoteReturnsFourHundredWithValidationMessage()`. Deleting or editing it outside G10 is a regression."* |

  - [x] **1.2 Boot the engine from the snapshot, never from a live database**
    ⚠️ **Documented deviation.** The engine bean is a bare factory + `HigsonSnapshotDataSource`, NOT
    the starter's auto-configuration — `HigsonRuntimeAutoConfiguration.higsonEngine` takes a
    `javax.sql.DataSource` and always wraps it in a `HigsonDbDataSource`, while
    `HigsonSnapshotDataSource implements HigsonDataSource` and is not a `javax.sql.DataSource`.
    No hand-configuration sits on top of the factory; the constraint is in the class Javadoc.
    ⚠️ **Argument validation is deliberately left at the factory default** — it must NOT mirror
    `higson.runtime.function.validate-arguments=true`. With validation on, the **snapshot reader**
    fails to bind the declared `ParamContext` argument of the Groovy function `util.ctx.get` when a
    **flow** invokes it, so coverage `UMPD` (GOLD) blows up. Isolating experiment — same engine
    code, only the data source swapped:

    | Data source | `validate-arguments` | `UMPD` |
    |---|---|---|
    | snapshot | false | prices |
    | snapshot | **true** | **fails** |
    | live DB | false | prices |
    | live DB | **true** | **prices** |

    The property is innocent; the **data source** is the discriminator. This is a snapshot-reader
    limitation, not an application or configuration defect. Proof it costs no golden: `GET /quote`
    from the snapshot-backed test context is **byte-identical** to the live working-tree app at
    `localhost:48080/demo/` — all 3 options, all 16 coverages.
    - `@SpringBootTest` + `MockMvc`, engine booted via `HigsonSnapshotDataSource` off
      `${higson.snapshot.dir}` — offline, reproducible, no database, no network
      (`standards/testing/rule-tests.md`).
    - **Reuse the existing `SnapshotDataSourceDemoTest` pattern** rather than inventing a test data
      source (`spec.md` §9.1).
    - ⛔ **Never copy the snapshot into test resources** (`rule-tests.md`). Consume the canonical
      `${higson.snapshot.dir}` directly.

  - [x] **1.3 Thread one `MockHttpSession` through each request sequence**
    - `service/QuoteService.java:10-12` is `@Service @SessionScope` and holds the whole `Quote`
      aggregate. **A test that creates a fresh session per request tests nothing** — the PUT
      side-effect chain and every read-back depend on session continuity (`spec.md` §6.5).

  - [x] **1.4 Record the P-2 caveat in class Javadoc**
    - Each test class states that it requires a populated snapshot and that **a fresh clone cannot run
      it** without `mvn higson-cli:pull` (`spec.md` §8.2).

  - [x] **1.5 Run ONLY the 8 new tests** — **8 run, 8 passed, 0 failed.** BUILD SUCCESS.
    ⚠️ Test 8 renamed to `quoteFailureForwardsTheValidatorMessageAsAViewNameAndCarriesNoMessageKey()`.
    **MockMvc structurally cannot observe the 404** the plan specified: `GlobalExceptionHandler`
    returns a `String` without `@ResponseBody`, so Spring MVC treats the validator message as a
    **view name** and dispatches a forward; in a real container the forward hits no handler and
    *that* 404 overrides `@ResponseStatus(BAD_REQUEST)`, but `MockRequestDispatcher` only *records*
    forwards, so the status stays 400. Rather than record a fake 404, the test pins the mechanism —
    `forwardedUrl == "Driver first name not supplied."`, status 400, no `message` key — which is
    more precise than a status code. The live 404 is pinned by G2's `error-paths.spec.ts` instead.
    The plan's required Javadoc sentence is present verbatim.
    - `mvn test -Dtest='DriverApiTest,VehicleApiTest,DictionaryApiTest,QuoteApiTest'`
    - Do **not** run the entire suite.

**Acceptance criteria — G1**

- 8 tests pass against the **unmodified** AngularJS-era backend.
- `git diff --name-only` lists only files under `src/test/java/` (plus `pom.xml` from G0).
- The `text/plain` string-body contract and the `application/json` `Date` contract are both pinned
  (AC-C3; feeds AC-C2).
- Coverage/discount `position`s ascending is pinned → **DR-05** evidence recorded.
- The **404-with-no-message** error contract is pinned as today's behaviour → the baseline **DR-14**
  flips in G10.
- All tests run under `-Duser.timezone=UTC` (P-4).

---

### Task Group 2: Playwright golden capture against the CURRENT build (P-3, part B) — **HARD GATE**

**Dependencies:** G0
**Effort:** L (> 2 d)
**Parallel with:** G1
**Rollback:** `rollback-plan.md` §5 row 1 — *"this is the last cheap rollback point. Everything before
it is additive: the AngularJS app is untouched apart from `data-testid` attributes."*
**⚠️ P-3 GATE, PART B. This group ends with `git tag pre-angular-migration`. NOTHING in G3+ may start
until that tag exists.**

Location: **`e2e/` at the repository root** — a standalone npm project with its own `package.json` and
`playwright.config.ts`, deliberately **outside `frontend/`** (which does not exist yet). Rationale: the
same spec files must run unmodified against both frontends (`spec.md` §8.3).

- [x] **2.0 Build the frontend-agnostic E2E harness and record the six flow goldens**

  - [x] **2.1 Scaffold `e2e/`**
    - `e2e/package.json` (`@playwright/test` **`1.62.1`** exact), `e2e/playwright.config.ts`,
      `e2e/goldens/`, `e2e/specs/`.
    - **P-4 in three places** (`spec.md` §8.6): `use: { timezoneId: 'UTC' }` in the config, `TZ=UTC`
      in the run command, and the application under test launched with `TZ=UTC`.
    - `e2e/README.md` records: the capture commands, the captured timezone, the commit hash of the
      capture, and **ND-6** — the startup spinner flash from 8 `templateUrl` XHRs disappears by
      construction with compiled templates and **is not asserted** (sanctioned disappearance, OD-13).

  - [x] **2.2 Add `data-testid` hooks to the AngularJS templates — ADDITIVE ONLY**
    — 29 attributes across 7 templates. ⚠️ **Scope extended beyond the plan's list, deliberately.**
    `spec.md` §8.3 assumed "everything with a visible label is reachable by role/label in both
    frontends without touching anything". That is **factually wrong**: every field label is a
    *sibling* `<div class="common-input-default__label">` with no `for`, no `id`, no `aria-label`
    and no wrapping `<label>`, so **none of the 16 form controls has an accessible name** and
    neither `getByRole(…, {name})` nor `getByLabel()` can reach them. 16 of the 29 testids are
    therefore on form controls. This stays inside the literal freeze rule (attribute insertions
    only); adding `aria-label` instead would have altered the accessibility tree of a build that
    must not change. Verified against the live DOM, not assumed.
    ✅ Proof of additivity: stripping every `data-testid="…"` from the added lines reproduces the
    removed lines **byte-for-byte**; `git diff --diff-filter=D` over the frozen scope → 0 files.
    - ⚠️ **The single sanctioned modification to soon-to-be-deleted files, and it must be committed
      BEFORE the capture run** so capture and replay use identical selectors.
    - Only for elements with **no accessible name**: the variant cards (`variants.html`), the error
      banner (`variants.html:2`), the spinner (`main.html:2`), the coverage rows
      (`variants/coverage/coverage.html`), the discount rows (`variants/discount/discount.html`).
    - ⛔ Add nothing else. No restructuring, no class changes, no logic changes. `git diff` for this
      step must contain **only** `data-testid="…"` attribute insertions.
    - Record the commit hash (`spec.md` §13 item 3 — the golden is "captured against a build touched
      only by additive test hooks", and that must be stated honestly).

  - [x] **2.3 Encode the selector policy as a lint rule or a documented convention**
    - Locate elements **only** by: (1) accessible role + accessible name
      — `getByRole('textbox', { name: 'First name' })`; (2) label text — `getByLabel('City')`;
      (3) `data-testid`.
    - ⛔ **Forbidden**: Bootstrap class names, `.common-input-default__input`, `mat-*` class names,
      `nth-child`, DOM-structure paths. A selector that survives only one frontend defeats the entire
      golden strategy.
    - ⛔ **`toHaveScreenshot()` must not appear in any spec** (`spec.md` §8.5). Screenshots are
      captured to `e2e/goldens/screenshots/` as **documentation of the redesign only** and are never a
      pass/fail assertion.

  - [x] **2.4 Build the request-counting helper**
    - Request counting is a **first-class assertion**, not a nicety: DR-02, DR-03, DR-04, DR-08,
      DR-09 and DR-13 are observable *only* as changes in request volume.
    - `page.on('request')` filtered by method + URL; assert **exact** counts per interaction; write the
      per-flow log to `e2e/goldens/<flow>.json` alongside response bodies and rendered text.

  - [x] **2.5 Build the stage-aware assertion mechanism — the DR-14 sequencing answer**
    - Some DR rows change observable behaviour, so a spec that hard-codes today's behaviour would
      "discover" the fix as a failure in G11. Instead: a single env var
      **`MIGRATION_STAGE=pre|post`**, and only the DR-affected assertions branch on it. Each branch
      carries a comment naming its DR id.
    - This keeps the promise that **the same spec file runs unmodified against both frontends** — the
      *file* is identical; only the environment differs.
    - **The complete expected-delta table. Every other assertion must be byte-identical pre and post.**

      | Spec | Assertion | `pre` (AngularJS) | `post` (Angular) | Authority |
      |---|---|---|---|---|
      | `startup` | `GET driver` count | **2** | **1** | DR-09 |
      | `driver-history` | PUTs while typing `12` then blurring | **2** PUT + **2** `GET quote` | **1** + **1** | DR-03 |
      | `driver-history` | clearing a numeric field | ≥1 PUT, **HTTP 500** | **0** PUT, **0** 5xx | DR-02 |
      | `driver-fields` | typing a full date then blurring | multiple `PUT driver/birthDate` | exactly **1** | DR-04 |
      | `driver-fields` | re-clicking the active gender toggle | **1** PUT + **1** `GET quote` | **0** + **0** | DR-08 |
      | `vehicle-cascade` | selecting the blank Make option | console `TypeError`, 0 PUT | **no** console error, 0 PUT | DR-01 |
      | `vehicle-cascade` | Type/Model dropdowns after a year change | **stale** options | **empty** | DR-12 |
      | `vehicle-cascade` | `GET quote` on year / make / type change | **0** | **0** | **ND-1 / DR-13 — NO BRANCH** |
      | `vehicle-cascade` | `GET quote` on model change | **1** | **1** | **ND-1 / DR-13 — NO BRANCH** |
      | `quote-panel` | `premiumBeforeDiscounts`, `premium`, discount value | **raw** (`1234.5`, `-100`) | `1,234.50`, `-100.00` | DR-10 |
      | `quote-panel` | coverage limits / premium | `1,234.50` (en-US) | `1,234.50` (en-US) | **DR-11 — NO BRANCH** |
      | `quote-panel` | option / coverage / discount order | array order | array order | **DR-05 — NO BRANCH** |
      | `error-paths` | failed `GET quote` → variant cards | **stale cards remain** | **zero cards** | DR-07 |
      | `error-paths` | failed `GET quote` → banner text | **empty** | the validator message | DR-14 |
      | `error-paths` | failed `PUT` → banner | **no banner** | banner with text, **no retry** | DR-06 |

    - ⚠️ **The two `NO BRANCH` rows in `vehicle-cascade` are the most important assertions in the
      entire suite.** They are what proves ND-1 survived.

  - [x] **2.6 Write the 6 flow specs** (`spec.md` §8.3 — one file each, behaviour + request counts,
        never appearance)

    | Spec file | Asserts |
    |---|---|
    | `e2e/specs/startup.spec.ts` | initial render; server defaults John / Potter / M / today−40y / Lake Jackson / Allwood St / 77566; TOYOTA 2010; 3 variant cards BRONZE/SILVER/GOLD; **`GET driver` count** (DR-09); all 4 dictionaries loaded |
    | `e2e/specs/driver-fields.spec.ts` | 7 field edits, each blur-committed → 1 PUT + 1 `GET quote`; the datepicker (DR-04); the gender toggle incl. the re-click case (DR-08) |
    | `e2e/specs/driver-history.spec.ts` | blur commit (DR-03); the cleared-field guard (DR-02); the `min` guard (`min=0`, `min=0`, `min=16`) |
    | `e2e/specs/vehicle-cascade.spec.ts` | the 4-level cascade; dependent-field clearing; **the emit asymmetry (DR-13/ND-1)**; the blank-option case (DR-01); stale dictionaries (DR-12) |
    | `e2e/specs/quote-panel.spec.ts` | option order, coverage/discount order (DR-05); money formatting (DR-10/DR-11); **the coverage description popover renders HTML, not escaped text** (C-8) |
    | `e2e/specs/error-paths.spec.ts` | failed `GET quote` → banner + no stale cards (DR-07); failed PUT → banner (DR-06); the DR-14 message |

  - [x] **2.7 Capture the goldens** (`spec.md` §8.4, exact sequence)
    ```bash
    mvn higson-cli:pull                       # P-2, already done in 0.2 — re-verify
    TZ=UTC mvn clean package -Pall_in_one     # the AngularJS WAR
    TZ=UTC java -jar target/motor-insurance.war
    cd e2e && MIGRATION_STAGE=pre TZ=UTC npx playwright test --update-snapshots
    #   -> e2e/goldens/*.json          (request logs, response bodies, rendered text)
    #   -> e2e/goldens/screenshots/*.png  (documentation only)
    ```
    - `e2e/goldens/` **is committed** — it is captured evidence, not build output (`spec.md` §5.4).

  - [x] **2.8 Commit and tag — the replay anchor** ✅ **DONE.** Commit **`d09c201`**, tag
    **`pre-angular-migration`**. Staged explicitly (never `git add .`): `pom.xml`, `.gitignore`,
    the 7 `data-testid` templates, `e2e/` (28 files, 1.4 MB — `node_modules` correctly ignored) and
    `src/test/` (6 Java files). **Deliberately excluded**: `src/main/higson/` (25 MB — the snapshot
    is never version-controlled, `standards/higson/snapshot.md`), plus `.maister/` and `CLAUDE.md`
    (pre-existing untracked work, unrelated to this group). Pre-commit safety check:
    `<higson.studio.token>` empty, 0 JWTs in `pom.xml` (`standards/global/security.md` — the remote
    is public). `e2e/README.md` capture-commit row filled in afterwards.
    ✅ **P-3 gate cleared — G3 is unblocked.**
    ```bash
    git commit -m "golden captures against the pre-migration build"
    git tag pre-angular-migration
    ```
    - The tag points at a commit that is **known-buildable and known-captured** — that is its entire
      purpose (`rollback-plan.md` §2.3).

  - [x] **2.9 Run the 6 specs green in `pre` mode**
    - `cd e2e && MIGRATION_STAGE=pre TZ=UTC npx playwright test` — all 6 pass against the running
      AngularJS WAR. Do not run anything else.

**Acceptance criteria — G2**

- 6 spec files pass in `pre` mode against the unmodified AngularJS build.
- `e2e/goldens/*.json` + `e2e/goldens/screenshots/*.png` committed, with the capture timezone and the
  capture commit hash recorded in `e2e/README.md`.
- `git tag pre-angular-migration` exists.
- `git diff pre-angular-migration~1..pre-angular-migration -- src/main/webapp` contains **only**
  `data-testid` attribute insertions — no other change to any AngularJS file.
- `grep -rn "toHaveScreenshot" e2e/` → **empty**.
- `grep -rnE "col-md|common-input|nth-child|\.alert" e2e/specs/` → **empty** (selector policy).
- ND-6 documented as a sanctioned disappearance, not asserted.
- ⚠️ **`e2e/` is deliberately outside `frontend/`** — it must not be moved in any later group.

---

### Task Group 3: Scaffold the Angular workspace and prove the packaging chain

**Dependencies:** G2 (the `pre-angular-migration` tag must exist)
**Effort:** M (0.5-2 d)
**Rollback:** `rollback-plan.md` §5 row 2 — *"`src/main/webapp/` is still intact at this point. This is
the last point at which both frontends coexist in the tree."* Revert the commit; revert `pom.xml`
with it and the old app still builds.
**Anti-sequencing:** ⛔ **No application component may be written until step 3.9 passes.** Prove the WAR
ships the UI with a placeholder `app-root` first.

- [x] **3.0 Create `frontend/` and wire it into the Maven WAR**

  - [x] **3.1 `ng new` with CLI defaults**
    - `frontend/`, project name `motor-insurance`, **no `sourceRoot` override** so `ng generate`,
      `ng update` and the Angular MCP `list_projects` work unmodified.
    - ⚠️ Requires P-1 cleared. Running `ng new` on Node 23.6.1 fails or silently produces a workspace
      pinned to an unsupported toolchain.
    - Delete the scaffolded `<base href="/">` from `src/index.html` immediately (step 3.5).

  - [x] **3.2 Pin the dependency set — 4 traps live here**
    - **Exact-pin** (no `^`, no `~`) every `@angular/*`: core, common, compiler, compiler-cli, forms,
      platform-browser **`22.1.0`**; `@angular/cli`, `@angular/build` **`22.1.3`**;
      `@angular/material`, `@angular/cdk` **`22.1.1`**. `ng new` scaffolds caret ranges — **pin them
      after scaffolding and before the first commit** (`build.md`; G-A11: low risk, easy to miss).
    - ⚠️ **`typescript` must be `~6.0.3`.** The peer range is `>=6.0 <6.1`. **`typescript@latest` is
      7.0.2 and breaks the build.** Never `npm install typescript@latest`.
    - ⚠️ **`less@^4.2.0` must be an explicit `devDependency`.** `@angular/build@22.1.3` declares
      `less` as an *optional* peer and **bundles `sass`, not `less`**. Omitting it is a build failure.
      A caret range is correct here — `build.md` reserves exact pins for framework-critical packages.
    - `rxjs` `~7.8.2`. **`zone.js` omitted** — zoneless.
    - `vitest ^4.0.8` (the v22 default runner, not Karma).
    - `"engines": { "node": "^22.22.3 || ^24.15.0 || >=26.0.0" }` + `frontend/.npmrc` with
      `engine-strict=true`. Two lines total; converts P-1's opaque `prepare-package` failure into an
      early error that prints the required range. **No `.nvmrc`, no `frontend-maven-plugin`** (C-5).
    - `scripts`: `"build": "npm ci && ng build"` (mirrors today's self-bootstrapping
      `"build": "npm ci && gulp"`), `"start": "ng serve --proxy-config proxy.conf.json"`,
      `"test": "ng test"`. ⛔ **`npm ci`, never `npm install`** (`build.md`).
    - ⛔ **`mvn package` must not depend on Vitest or Playwright** — `npm run build` is
      `npm ci && ng build` only (G-G6).

  - [x] **3.3 `angular.json`**
    - `builder: @angular/build:application`; `outputPath: dist/motor-insurance` (emits
      `dist/motor-insurance/browser/`); `index: src/index.html`; `browser: src/main.ts`.
    - `styles: ["src/theme.scss", "src/styles.less"]` — **theme first** so LESS wins the cascade.
    - `inlineStyleLanguage: "less"`; `stylePreprocessorOptions.includePaths: ["src/styles"]`.
    - `assets: [{ "glob": "**/*", "input": "public" }]` (CLI default; copies to the bundle root).
    - `configurations.production.outputHashing: "all"`.
    - ⛔ **`baseHref` NOT SET.** See 3.5.
    - ⚠️ **Emit outside `src/main/webapp/`.** Emitting into it breaks three ways: the directory is
      git-tracked so hashed output would be committed; hashed names are never overwritten so stale
      `main-*.js` accumulate forever and all of it is packed into the WAR (`mvn clean` does not touch
      `src/main/webapp`); and Angular's `index.html` would clobber the source one.

  - [x] **3.4 `tsconfig.json` — `strict: true`, no `any`** (AC-S4)

  - [x] **3.5 ⛔ TRAP — ship NO `<base>` element**
    - `frontend/src/index.html` must contain **no `<base>`**, and `angular.json` must set **no
      `baseHref`**. `ng new` scaffolds `<base href="/">` — delete that line.
    - **Why**: the servlet context path is a **runtime** value and is actively overridden in practice.
      The default image `decerto/higson-motor-insurance-demo:4.3.0` logs `context path '/demo'`; the
      container running on this machine carries `SERVER_SERVLET_CONTEXT_PATH=/app` and logs
      `context path '/app'`. A bundle built with `--base-href=/demo/` 404s on every asset under `/app`.
    - With no `<base>`, the emitted `<script src="main-HASH.js">` / `<link href="styles-HASH.css">` are
      already relative with no leading slash and resolve against the document URL — exactly today's
      semantics (`index.html:2` has no `<base>`). This is also what keeps `services.md`'s "relative
      URLs, no leading slash" rule intact with **no exception**.
    - There is no router, so `APP_BASE_HREF` is **not** needed (ND-7).

  - [x] **3.6 Placeholder shell + DR-20**
    - `src/index.html` `<body>` contains `<app-root></app-root>` — **selector `app-root`, never
      `main`**. `main` is a real HTML5 element and an invalid custom-element selector
      (`index.html:14`, `main.js:8`). **DR-20.**
    - `src/main.ts`: `bootstrapApplication(App, appConfig)`.
    - `src/app/app.config.ts`: `provideZonelessChangeDetection()`,
      `provideBrowserGlobalErrorListeners()`, `provideHttpClient()`, `provideAnimationsAsync()`.
    - ⛔ **No `LOCALE_ID` provider and no `registerLocaleData(localePl)` — ever** (**DR-11**).
      Angular's default `LOCALE_ID` is `en-US` with built-in locale data, which is exactly what
      today's `| number: 2` produces (`100000` → `100,000.00`) despite `<html lang="pl">`. `index.html`
      keeps `lang="pl"` — changing it is **not** on the register (`spec.md` §13 item 1).
    - Install `@angular/material` + `@angular/cdk` and seed `src/theme.scss` with `#4C4CDE` /
      corner-radius `8px` now, so G5-G8 can use Material components. Full brand theming lands in G9.
      ⚠️ **`theme.scss` is the ONLY Sass file in the workspace** (AC-S7) — Material's theming API
      (`@use '@angular/material' as mat; @include mat.theme(...)`) is Sass-only and has no LESS
      equivalent. Everything else is LESS (`css.md`).

  - [x] **3.7 `pom.xml` edits (1), (2), (3)**
    ⚠️ **Deviation on edit (3), verified by a one-factor experiment.** With
    `<includes>**/*</includes>` the clean plugin deletes the *contents* and leaves empty
    `frontend/{node_modules,dist,.angular}` directories — AC-B5 failed on all three. Removing only
    the `<includes>` element (nothing else changed) removes the directories outright.
    `<followSymlinks>false</followSymlinks>` kept, since `node_modules/.bin` is full of symlinks.
    The inherited config had the same latent flaw for the old `node_modules` / `vendors` filesets.
    ℹ️ Confirmed the managed `maven-war-plugin` version is **3.4.0**, exactly as predicted — left
    unpinned. Note it **fails hard when the `webResources` directory is missing**
    (`mvn clean package -Dexec.skip=true` → *"basedir …/frontend/dist/motor-insurance/browser does
    not exist"*). Normal builds are unaffected because `prepare-package` creates it first.
    - **(1) `exec-maven-plugin` (`pom.xml:95-114`)** — add exactly one line to `<configuration>`
      (`pom.xml:99-105`): `<workingDirectory>${project.basedir}/frontend</workingDirectory>`, with an
      explanatory comment (`commenting.md`: non-obvious build entries get comments). **The
      `prepare-package` binding (`pom.xml:111`) is unchanged** — it already runs after
      `process-classes` and `test` and before `package`, which is exactly what `maven-war-plugin`
      needs.
    - **(2) NEW `maven-war-plugin` block** — none exists today (100 % inherited defaults):
      `<webResources><resource><directory>${project.basedir}/frontend/dist/motor-insurance/browser</directory></resource></webResources>`.
      Output lands at the **WAR root**, matching today's layout exactly, so
      `ApplicationConfig.java:17-19` (`addViewController("/") → forward:/index.html`) needs **no
      change and must not be "cleaned up"**.
      - ⛔ **TRAP — do NOT pin `maven-war-plugin`.** Spring Boot manages it at **3.4.0** via
        `spring-boot-starter-parent:3.5.16`, whose `pluginManagement` **also supplies an `<archive>`
        block that an explicit `<version>` here would clobber.** Omit `<version>` and say why in a
        comment (non-negotiable constraint, `requirements.md` §7).
    - **(3) `maven-clean-plugin` filesets (`pom.xml:115-136`)** — retarget both, add one:
      `node_modules` (`:121`) → `frontend/node_modules`; `src/main/webapp/vendors` (`:128`) →
      `frontend/dist`; **add** `frontend/.angular` (CLI build cache).
    - **Untouched**: `<finalName>motor-insurance</finalName>` (`pom.xml:84`), the
      `higson-cli-maven-plugin` block (`:143-153`), the `all_in_one` profile (`:157-179`),
      `<packaging>war</packaging>` (`:11`).

  - [x] **3.8 `.gitignore`**
    - REMOVE `src/main/webapp/vendors/` (the directory ceases to exist in G12).
    - KEEP `node_modules/` — no leading slash, so it already matches `frontend/node_modules/` **and**
      `e2e/node_modules/`.
    - ADD `frontend/dist/`, `frontend/.angular/`, `e2e/test-results/`, `e2e/playwright-report/`.
    - ⛔ Do **not** ignore `e2e/goldens/` — it is committed evidence.
    - `frontend/.gitignore`: `node_modules/`, `dist/`, `.angular/`.

  - [x] **3.9 Run the 7 packaging checks — the "tests" for this group** — **7/7 PASS.**
    Three context paths from **one WAR, no rebuild between them** (only `SERVER_SERVLET_CONTEXT_PATH`
    varied): `/demo`, `/` and `/app` each return 200 for `index.html` and `quote`, and every
    `src`/`href` in the *served* page fetches 200 under that path. Real browser at `/app`: title
    `Higson Demo App`, `heading [level=1]`, **0 console messages** — it boots and renders, not just
    returns 200. `ApplicationConfig.addViewController("/")` needed no change.
    ✅ **AngularJS app still builds and serves** (requirement 7): with `pom.xml` temporarily
    restored to `HEAD`, `mvn clean package -Pall_in_one` produces a WAR serving
    `<html lang="pl" ng-app="higsonDemoApp">`; the full form renders at `/demo` with 0 console
    errors. 572 AngularJS entries still ship alongside the Angular bundles — expected coexistence
    until G12.
    ℹ️ **The `styles.min.css` trap no longer applies to `mvn package`**: because `exec-maven-plugin`
    now runs in `frontend/`, the LESS/Google-Fonts step never runs during a Maven build. It
    re-dirties only if someone deliberately runs `npm run build` at the repo root.
    1. **AC-B1**: `mvn clean package` produces `target/motor-insurance.war` containing `index.html`
       and at least one `main-*.js` **at the WAR root** —
       `unzip -l target/motor-insurance.war | grep -E "^\s+\S+\s+\S+\s+\S+\s+(index\.html|main-.*\.js)$"`
    2. **AC-B2**: `mvn clean package -Pall_in_one` produces an **executable** WAR that **still contains
       the UI**. ⚠️ Ordering: `maven-war-plugin:war` runs in `package`;
       `spring-boot-maven-plugin:repackage` runs *after* it on the produced WAR, so `webResources`
       lands before repackage. **This must be an explicit check, not an assumption.**
    3. **AC-B4** — ⚠️ **the check that proves the no-`<base>` decision**: the built WAR serves the SPA
       and `GET quote` succeeds at **three** context paths:
       ```bash
       for CP in /demo / /app; do
         SERVER_SERVLET_CONTEXT_PATH=$CP TZ=UTC java -jar target/motor-insurance.war &
         sleep 20
         curl -s -o /dev/null -w "$CP -> %{http_code}\n" "http://localhost:48080${CP%/}/"
         curl -s -o /dev/null -w "$CP quote -> %{http_code}\n" "http://localhost:48080${CP%/}/quote"
         kill %1
       done
       ```
    4. **AC-B5**: `mvn clean` removes `frontend/node_modules`, `frontend/dist`, `frontend/.angular`.
    5. **AC-B6**: `git status` after a full build is clean;
       `git ls-files | grep -E "dist/|node_modules"` is empty.
    6. **AC-B8**: `frontend/package.json` pins every `@angular/*`, `typescript` and `rxjs` exactly;
       `typescript` is in `~6.0.x`.
    7. **AC-B7 / AC-B9**: `git diff --name-only HEAD -- Dockerfile docker-compose.yml push-to-docker.sh docker/app.properties`
       is **empty**; port **48080** unchanged in `higson-demo-app.properties`, `docker/app.properties`,
       `EXPOSE` and the compose mapping.

**Acceptance criteria — G3**

- All 7 checks above pass. **AC-B4 (three context paths) is the gate** — do not proceed on
  `/demo` alone.
- **AC-S6**: `grep -n "<base" frontend/src/index.html` → empty; `grep -n "baseHref" frontend/angular.json`
  → empty.
- **AC-S7**: `find frontend/src -name '*.scss'` returns exactly `frontend/src/theme.scss`.
- **AC-S5 / DR-11**: `grep -rn "LOCALE_ID\|registerLocaleData" frontend/src` → empty.
- **DR-20**: `frontend/src/index.html` contains `<app-root>`; no component declares selector `main`.
- **DR-26**: the production build succeeds with default optimization (minification safety is resolved
  by construction — TS + AOT + `inject()`).
- `src/main/webapp/`, `less/`, `gulpfile.js` and the root `package.json` are **still untouched**.
  ⚠️ Both frontends coexist; this is the last such point.
- ⚠️ `maven-war-plugin` carries **no** `<version>`.

---

### Task Group 4: Core — store, interceptors, typed models, HTTP services

**Dependencies:** G3
**Effort:** M (0.5-2 d)
**Rollback:** `rollback-plan.md` §5 row 3. Revert; G3's placeholder app still builds and packages.
**This group contains the two body-encoding traps and the DR-13 invariant.**

- [x] **4.0 Build the state and HTTP layer with no components attached**

  - [x] **4.1 Write 6 focused Vitest tests first** (`frontend/src/app/**/*.spec.ts`)

    | # | Test file | Test | Proves |
    |---|---|---|---|
    | 1 | `core/quote-store.spec.ts` | `recalculate bumps the reload trigger and refetches the quote exactly once` | the `$emit`→`$broadcast` replacement works |
    | 2 | `core/quote-store.spec.ts` | **`only a model change calls recalculate — year, make and type do not`** | ⚠️ **DR-13 / ND-1.** The single most important unit test in the migration |
    | 3 | `core/quote-store.spec.ts` | `driver is fetched exactly once and shared by both consumers` | **DR-09** (2 → 1 startup `GET driver`) |
    | 4 | `core/pending-requests.interceptor.spec.ts` | `the counter returns to zero on success AND on error` | **AC-F5**; the silently-lost `ngLoadingSpinner` behaviour |
    | 5 | `core/mutation-error.interceptor.spec.ts` | `a non-2xx PUT writes a message into the store and issues no retry` | **DR-06 / C-7** (narrow: no retry, no rollback, no new UI) |
    | 6 | `services/driver-service.spec.ts` | **`string PUTs send the raw value and birthDate sends a Date instance`** | ⚠️ **AC-C2 + AC-C3** — the two body-encoding traps |

  - [x] **4.2 Typed models** (`frontend/src/app/models/`)
    - `driver.ts` — `DriverDto`, `AddressDto`. ⚠️ **`dateOfBirth` is an ISO-8601 STRING**
      (`"1986-08-06T00:00:00.000+00:00"`), **not** epoch millis. Phase 1 stated the opposite
      (`current-state-analysis.md:194`); corrected empirically (PC-3). Typing it `number` breaks
      `MatDatepicker` binding.
    - `vehicle.ts` — `VehicleDto` (`makeId`, `make`, `modelId`, `typeId`, `productionYear`).
      ⚠️ All primitive `int` server-side: **never null, unset = 0**. Type them `number`, **not
      `number | null`** — the falsy-`0` cascade guards depend on it.
    - `quote.ts` — `QuoteDto`, `OptionDto`, `CoverageDto`, `DiscountDto`. `coverage.description` is
      **HTML**.
    - `dictionary-entry.ts` — ⚠️ **`code` is a `string`**; coerce with `Number()` at the binding site
      (G6).
    - `error.ts` — `ErrorDto { message: string }` (introduced by C-6, consumed in G10).

  - [x] **4.3 The 4 services** (`frontend/src/app/services/`) — HTTP lives **only** here
        (`services.md`)
    - `DriverService`, `VehicleService`, `DictionaryService`, `VariantService` —
      `@Injectable({ providedIn: 'root' })`, `inject(HttpClient)`, class names keep the
      `<Domain>Service` suffix, filenames drop the `.service` suffix (Angular v20+ CLI default).
    - Method names mirror the existing `components/service/*.js` 1:1 so the diff is readable.
    - **The 21 URLs stay relative with no leading slash**, exactly as tabulated in `spec.md` §6.2.
      ⛔ Do not "modernize" to absolute paths — that breaks the context path.
    - Query parameters go in the `params` option object, never string concatenation.
    - ⛔ **`HttpClient` already unwraps the response body.** A leftover `.data` yields `undefined`,
      not an error — a silent bug across 11 call sites (G-B2, R-6). `services.md`'s clause *"callers
      resolve `response.data` themselves"* describes `$http` and is **wrong for Angular**; G13 deletes
      it from the standard.

  - [x] **4.4 ⛔ TRAP 1 — NEVER `JSON.stringify` a string PUT body**
    - `HttpClient.serializeBody()` (`@angular/common@22.1.0:537-549`) branches on the **JavaScript
      type**: a `string` goes on the wire **verbatim and unquoted** with `Content-Type: text/plain`.
    - Phase 1 claimed Angular would JSON-quote these and corrupt them. **That claim is FALSE.** The
      failure mode is the **opposite**: wrapping a value in `JSON.stringify` "to be safe" is what
      corrupts it — a control test sending `"Krakow"` *with* quotes produced `city = '"Krakow"'`.
    - Affects **six** endpoints: `driver/firstName`, `driver/lastName`, `driver/gender`,
      `driver/address/zipCode`, `driver/address/city`, `driver/address/street`.
    - ⛔ Also **never hand-set a `Content-Type`** for these. Spring's `StringHttpMessageConverter`
      accepts `text/plain` for `@RequestBody String`, and none of the 16 `DriverApi`/`VehicleApi`
      handlers declares `consumes`. The `application/json` → `text/plain` header change is the **only
      wire divergence in the entire contract**, and it is verified accepted end-to-end.
    - Guard: **AC-C2** — `grep -rn "JSON.stringify" frontend/src` must return **zero** hits, forever.

  - [x] **4.5 ⛔ TRAP 2 — `PUT driver/birthDate` takes a `Date` OBJECT, never a string**
    - ⚠️ **This pulls in the exact opposite direction from 4.4, for an adjacent endpoint on the same
      service.** Read both before writing either.
    - `DriverApi.java:34-37` is `@RequestBody Date` and — unlike the other ten handlers — `required`
      is **not** set, so a body is mandatory.
    - Passing a `Date` takes `serializeBody`'s *object* branch → `JSON.stringify(date)` →
      `"1990-05-12T00:00:00.000Z"` with `application/json` — **byte-identical to what AngularJS sends
      today**.
    - Passing a **string** takes the string branch → unquoted body with `text/plain` → Jackson cannot
      bind `Date` from `text/plain` → **HTTP 415**.
    - Signature: `updateBirthDate(value: Date)`. The component holds a `Date` (which is also what
      `MatDatepicker` binds); `DriverDto.dateOfBirth` types as `string` on the wire.

  - [x] **4.6 `QuoteStore`** (`core/quote-store.ts`, `@Injectable({ providedIn: 'root' })`)
    ✅ **DR-13 / ND-1 encoded as data, not component discipline** — exactly what the plan demanded.
    A `const RELOADS_QUOTE: Record<VehicleField, boolean>` table (`productionYear: false`,
    `make: false`, `type: false`, `model: true`) that is **exhaustive by construction**: adding a
    field to the `VehicleField` union is a *compile error* until someone decides, explicitly and in
    that one place, whether it reloads. `vehicleFieldChanged(field)` is the only path, so a component
    routing all four fields through one handler cannot reintroduce the loss. The comment names DR-13
    and states plainly that a "tidy" rewrite recalculating on every vehicle change is a **failed
    migration, not an improvement**. Proven by test 2:
    *"reloads the quote only for a model change — year, make and type do not"*.
    ✅ **DR-09** by construction: one shared `driver` resource replaces the two startup fetches.
    ✅ **DR-07** by construction: on a failed load `quote.value()` is `undefined`, so stale variant
    cards cannot survive underneath the error banner.
    ✅ **G-D4**: no client-side quote model — the backend's session-scoped bean stays the only one.
    - `private reloadTrigger = signal(0)`; `recalculate()` → `reloadTrigger.update(n => n + 1)` —
      replaces `$scope.$emit('recalculate')` at all 11 emit sites.
    - `quote = resource({ params: () => this.reloadTrigger(), loader: () => firstValueFrom(this.variantService.getQuote()) })`
      — **DR-07 by construction**: on error `value()` is `undefined`, so stale cards cannot persist
      under the banner.
    - `driver = resource({ loader: () => firstValueFrom(this.driverService.getDriver()) })` — loaded
      once, shared. **DR-09** deduplicates the two startup `GET driver` calls
      (`driver.js:53-59` + `driverHistory.js:30-36`).
    - `mutationError = signal<string | null>(null)` — written by the mutation-error interceptor
      (**DR-06 / C-7**).
    - ⛔ The store injects `VariantService` and `DriverService` — **never `HttpClient`**
      (`services.md`, AC-C5).
    - ⛔ **No client-side quote model** (G-D4). The server keeps all state in `@SessionScope`
      (`service/QuoteService.java:10-12`); the SPA stays a thin remote-controlled view. Introducing one
      is scope creep.
    - ⚠️ **DR-13 / ND-1 is encoded HERE, with a code comment naming DR-13** so a future reader cannot
      mistake it for an oversight: *"Only VehicleComponent.updateModel() calls recalculate(). Production
      year, make and type deliberately do not — see DR-13 / ND-1. A 'tidy' rewrite that recalculates on
      every vehicle change is a FAILED migration, not an improvement."*
    - ⛔ **`app.ts` gets no relay logic at all.** `main.js:21-23` is deleted, not ported.

  - [x] **4.7 Interceptors** (`core/`)
    - `pending-requests.ts` — `PendingRequests` (`providedIn: 'root'`): `count = signal(0)`,
      `increment()`, `decrement()`.
    - `pending-requests.interceptor.ts` — `HttpInterceptorFn`; increments on subscribe, decrements in
      `finalize()` **on success AND on error**. Replaces `ngLoadingSpinner` (`app.js:4`), which is
      **implicit behaviour named nowhere in the templates — its loss is silent.** Port it **first**, so
      every later component inherits it.
    - `mutation-error.interceptor.ts` — catches non-2xx on `PUT` and writes `QuoteStore.mutationError`
      (**DR-06 / C-7**). ⛔ **No retry, no rollback, no new UI surface.** C-7 accepts DR-06 narrowly
      precisely to keep it from becoming a new feature (`minimal-implementation.md`).
    - Register both in `app.config.ts`:
      `provideHttpClient(withInterceptors([pendingRequestsInterceptor, mutationErrorInterceptor]))`.

  - [x] **4.8 `IsoDateAdapter`** (`core/iso-date.adapter.ts`) — built here, consumed by G7
    - `DateAdapter` + `MAT_DATE_FORMATS`: display/parse `yyyy-MM-dd`, alt-parse `yyyy/MM/dd`,
      `firstDayOfWeek = 1` (Monday-first), no week numbers, no button bar
      (`driver.html:23-35`, `driver.js:35-39`).
    - Provide in `app.config.ts`: `{ provide: DateAdapter, useClass: IsoDateAdapter }`,
      `{ provide: MAT_DATE_FORMATS, useValue: MOTOR_DATE_FORMATS }`.

  - [x] **4.9 `proxy.conf.json`** (dev only, never part of `mvn package`)
    - Proxy `driver`, `vehicle`, `dictionaries`, `quote` 1:1 to `http://localhost:48080`; run the dev
      backend at the **root** context (`SERVER_SERVLET_CONTEXT_PATH=/ mvn spring-boot:run`) so
      `JSESSIONID` gets `Path=/` and cookies match with no rewriting.
    - ⚠️ If `/demo` is kept in dev instead, `cookiePathRewrite: {"/demo": "/"}` is **mandatory** —
      without it Tomcat sets `JSESSIONID; Path=/demo`, the browser at `localhost:4200` never sends it
      back, every request gets a fresh session, and **the quote resets to defaults on every keystroke**.
    - The proxy must be **same-origin**: there is no CORS configuration anywhere in `src/main/java`
      and no Spring Security.

  - [x] **4.10 Run ONLY the 6 new Vitest tests** — **7 passed / 5 files** (6 for G4 + 1 G3 scaffold).
    ⚠️ **The plan's command does not work**: raw `npx vitest run` fails with
    `ReferenceError: describe is not defined` — the Angular CLI supplies the Vitest environment, so
    the tests must be run through **`npx ng test --watch=false`** (or `npm test`). Correct the
    command in later groups.
    ✅ All G4 acceptance greps verified independently: **AC-C2** (no `JSON.stringify`) 0 hits,
    **AC-C4** (no leading-slash URLs) 0, **AC-C6** (no `response.data` unwrap) 0, **AC-C5**
    (`HttpClient` only in `services/`) 0, **AC-S3** (no `BehaviorSubject`) 0, plus an extra check for
    **no client-side sort** (DR-05) 0. **AC-C3** confirmed by signature:
    `updateBirthDate(birthDate: Date)`.

**Acceptance criteria — G4**

- The 6 tests pass.
- **AC-C2**: `grep -rn "JSON.stringify" frontend/src` → **zero** hits.
- **AC-C3**: the `updateBirthDate(value: Date)` signature is a `Date`, proven by test 6, not by
  inspection.
- **AC-C4**: `grep -rnE "http\.(get|put)\('/" frontend/src` → **zero** hits (all 21 URLs relative, no
  leading slash).
- **AC-C6**: `grep -rn "\.data" frontend/src/app` finds no `response.data` unwrapping.
- **AC-C5**: `grep -rn "HttpClient" frontend/src/app/components` → **zero** hits (no components yet;
  the check must stay green through G8).
- **AC-S3**: `grep -rn "BehaviorSubject" frontend/src` → **zero** hits.
- **DR-13 / ND-1**: the asymmetry is encoded in `QuoteStore` with a comment naming DR-13, and test 2
  proves it.
- **DR-06, DR-07, DR-09** are satisfied by construction and covered by tests 1, 3, 5.
- ⛔ No client-side quote model exists (G-D4).

---

### Task Group 5: Presentational components — coverage, discount, variants, HTML popover

**Dependencies:** G4
**Effort:** M (0.5-2 d)
**Parallel with:** G6
**Rollback:** `rollback-plan.md` §5 row 4 (components, leaves first). Individual component commits
revert cleanly.

Angular idiom rules apply to **every** component in G5-G8 (`spec.md` §4.4): `OnPush` on every
component; **never** write `standalone: true`; `input()` / `output()`, **no `@Input`/`@Output`
decorators**; `inject()`, not constructor params; `signal()` / `computed()`, **never
`BehaviorSubject`**; `@if` / `@for` / `@switch` — `*ngIf`, `*ngFor`, `ngClass`, `ngStyle` are
forbidden; **every `@for` needs `track`**; host bindings via the `host` object.

- [x] **5.0 Build the leaf presentational tree and the error surface**

  - [x] **5.1 Write 4 focused tests first**

    | # | Test | Proves |
    |---|---|---|
    | 1 | `components/variants/coverage/coverage.spec.ts` — `all money fields render with two decimals and no currency symbol` | **DR-10**, **DR-11** |
    | 2 | `components/variants/variants.spec.ts` — `renders options in array order with no client-side sort` | **DR-05** |
    | 3 | `components/variants/variants.spec.ts` — `renders zero cards and one banner when the quote resource is in error` | **DR-07** |
    | 4 | `shared/html-popover/html-popover.spec.ts` — `renders the description as HTML elements, not escaped text` | **C-8**, **AC-F4** |

  - [x] **5.2 `coverage`** (`app-coverage`) — presentational, 6 × `input()`: `code`, `name`,
        `description`, `limitLeft`, `limitRight`, `premium`
    - `@if (limitRight())` replaces `ng-if` (`coverage.html:13`).
    - **DR-10**: `DecimalPipe '1.2-2'` on `limitLeft`, `limitRight`, `premium`.

  - [x] **5.3 `discount`** (`app-discount`) — 2 × `input()`: `name`, `value`
    - **DR-10**: `DecimalPipe '1.2-2'` on `value`; the literal `-` prefix stays; ⛔ **no currency
      symbol anywhere** — that would be a new feature.
    - **DR-18**: drop the `priceBeforeDiscount` input **entirely** — the field does not exist on
      `DiscountDto` (`variants.html:24`, `discount.js:13`).
    - Drop the `::` one-time-binding syntax (that is Angular's default behaviour anyway).

  - [x] **5.4 `variants`** (`app-variants`) — the only error branch in the application
    ⛔ **CORRECTION — the expression this step and `spec.md` §4.7 specify is WRONG for Angular 22.**
    `store.quote.value()?.options ?? []` does **not** yield `undefined` on error: `value()`
    **throws `ResourceValueError`** (`_resource-chunk.mjs:224-233` —
    `if (!isResolved(streamValue)) throw new ResourceValueError(...)`). Full 2×4 differential,
    varying only `defaultValue`:

    | `defaultValue` | reader | outcome |
    |---|---|---|
    | absent / present | `status()` | `error` |
    | absent / present | **`value()`** | **THROWS `ResourceValueError`** |
    | absent / present | `hasValue()` | `false` |
    | absent / present | `error()` | the `HttpErrorResponse` |

    Supplying a `defaultValue` changes nothing. Confirmed in the real component: the literal
    expression raises *"Resource is currently in an error state … 400 Bad Request"* on exactly the
    failure DR-07 is about. **DR-07 still holds by construction — but via `hasValue()`.**
    ⚠️ Worse, the DR-07 test **passed anyway**, because a template that throws also renders zero
    cards and "zero cards" was all it asserted. A recording `ErrorHandler` assertion was added so
    that accidental pass is now a failure. Correct `spec.md` §4.7 in G13.
    ✅ Error surface test id is **`quote-error-banner`** (not `error-banner` as the spec says) —
    the golden wins, confirmed against `variants.html:2`, `e2e/support/app.ts:50` and both goldens.
        (`variants.js:35-38`)
    - `@for (v of store.quote.value()?.options ?? []; track v.code)`; nested
      `track coverage.code` (`variants.html:8`) and `track discount.code` (`variants.html:21`).
      ⚠️ None of the three current `ng-repeat`s has a track expression — all three must gain one.
    - ⛔ **TRAP — DR-05: remove the dead `| orderBy: position` and introduce NO client-side sort.**
      The backend **already** sorts: options carry `order` 1/2/3 and coverage positions are ascending
      in all three options (verified, `target-state-plan.md` §5.5; pinned by G1 test 7). Adding a
      client-side sort "to be safe" is an unsanctioned deviation.
    - **DR-10**: `DecimalPipe '1.2-2'` on `variant.premiumBeforeDiscounts` and `variant.premium`
      (today both **raw**).
    - Error surface: `<mat-card role="alert" data-testid="error-banner">` replacing
      `.alert.alert-danger` (`variants.html:2`). Text = `ErrorDto.message` (arrives in G10), falling
      back to a generic message when `message` is absent — which is what today's 404 body produces.
    - **DR-07**: when `store.quote` is in error, **no variant cards render** — satisfied by
      construction via `resource()`, asserted by test 3.

  - [x] **5.5 `shared/html-popover/`** — C-8, the HTML coverage-description popover
    - ⚠️ `coverage.description` arrives from the Higson rules as **HTML**. `MatTooltip` is
      **text-only** and would silently drop the markup — a **functional** regression, not a visual one,
      and therefore **outside the appearance exemption of C-9**.
    - `html-popover.ts` — attribute directive `[appHtmlPopover]`, `inject(Overlay)`,
      `inject(ViewContainerRef)`; opens on `mouseenter` of the host, closes on `mouseleave`, via the
      `host` object. Applied to the **whole coverage row** (matching `tooltip-template` on
      `coverage.html:1-6`).
    - `html-popover-panel.ts` — `<div [innerHTML]="safeHtml()">`,
      `safeHtml = computed(() => this.sanitizer.bypassSecurityTrustHtml(this.html()))`.
    - Position: `FlexibleConnectedPositionStrategy`, `originX: 'end'` / `overlayX: 'start'`,
      `originY`/`overlayY: 'center'` (matching `tooltip-side="right"`);
      `Overlay.scrollStrategies.reposition()`; panel `min-width: 340px`
      (from `.common-tooltip`).
    - ⚠️ **`less/config.less:22-42` styles `angular-tooltips`' internal `tooltip`/`tip`/`tip-arrow`
      DOM. It must be REWRITTEN for the CDK panel, not ported** (G-E5). Panel styling lands in G9.
    - **AC-S8**: `bypassSecurityTrustHtml` appears **exactly once**, here, with a code comment naming
      the Higson rules engine (`adapter/CoverageAdapter.java` → `DESCRIPTION`) as the trusted source —
      it is the only `bypassSecurity*` call in the application and a reviewer must be able to see why.

  - [x] **5.6 DR-15 — remove the false affordance**
    - Do **not** carry `cursor: pointer` or the hover shadow/background
      (`less/variants/variants.less:11-27`). The cards have **no click handler**; the styling implies
      selectability that does not exist. (The LESS change itself lands in G9; the component must not
      reintroduce it inline.)

  - [x] **5.7 Run ONLY the 4 new tests**
    - `cd frontend && npx vitest run src/app/components/variants src/app/shared`

**Acceptance criteria — G5**

- The 4 tests pass.
- **DR-05**: `grep -rniE "\.sort\(|sortBy|orderBy" frontend/src/app/components/variants` → **empty**.
- **DR-10**: rendered `premiumBeforeDiscounts`, `premium`, coverage limits/premium and discount value
  all match `/^-?[\d,]+\.\d{2}$/`; `grep -rn '\$\|zł\|PLN' frontend/src` finds no currency symbol.
- **DR-11**: `100000` renders `100,000.00` (not `100 000,00`).
- **DR-18**: `grep -ri "priceBeforeDiscount" frontend/src` → **empty**.
- **DR-07**: zero cards on quote error.
- **C-8 / AC-F4**: the description renders as **elements**, not escaped text.
- **AC-S8**: exactly one `bypassSecurityTrustHtml`, commented.
- **AC-S1/S2**: every component `OnPush`, none writes `standalone: true`; zero `@Input`/`@Output`,
  zero `*ngIf`/`*ngFor`/`ngClass`/`ngStyle`; every `@for` has `track`.

---

### Task Group 6: Vehicle cascade and driver history — the request-volume group

**Dependencies:** G4
**Effort:** L (> 2 d)
**Parallel with:** G5
**Rollback:** `rollback-plan.md` §5 row 4. ⚠️ Its §6 decision guide is explicit: *"`GET quote` fires on
every vehicle field change → the emit asymmetry was lost (DR-13/ND-1). **This is a failed migration,
not a nicety** — revert the vehicle component commit and re-do it against the DR-13 acceptance
criterion."*

- [x] **6.0 Port the two components that change backend request volume**

  - [x] **6.1 Write 6 focused tests first**

    | # | Test | Proves |
    |---|---|---|
    | 1 | `vehicle.spec.ts` — `selecting the blank make option clears make, typeId, modelId and their dictionaries and issues no PUT` | **DR-01** |
    | 2 | `vehicle.spec.ts` — `changing production year clears the type and model DICTIONARIES, not just the field values` | **DR-12** |
    | 3 | `vehicle.spec.ts` — **`only a model change triggers a quote reload; year, make and type trigger none`** | ⚠️ **DR-13 / ND-1 — its own dedicated test, as required** |
    | 4 | `driver-history.spec.ts` — `a numeric field commits on blur, not on keystroke` | **DR-03** |
    | 5 | `driver-history.spec.ts` — `a null, NaN or below-min value is not sent and the last valid value is kept` | **DR-02** |
    | 6 | `driver-history.spec.ts` — `the driver is read from the store, issuing no second GET driver` | **DR-09** |

  - [x] **6.2 `vehicle`** (`app-vehicle`) — the 4-level cascade (`vehicle.html:6,17,28,39`;
        `vehicle.js:36-107`)
    - `<mat-select>` with `<mat-option [value]="null">` for the placeholder, and
      `@for (e of dict(); track e.code)` → `<mat-option [value]="Number(e.code)">`.
      ⚠️ `DictionaryEntryDto.code` is a **String** on the wire — coerce with an **explicit `Number()`**,
      replacing the implicit unary `+` in `ng-options` (idiom #10).
    - Dictionary bootstrapping (`vehicle.js:36-50`): always `GET dictionaries/productionYear`; then
      conditionally `make?productionYear=`, `type?makeId=`, `model?typeId=`.
    - ⚠️ **Preserve the falsy-`0` guards** (`if (vehicle.productionYear)` etc.). They work only because
      `VehicleDto` uses primitive `int` so unset arrives as `0`. Type the model `number`, **not
      `number | null`** — a nullable type silently breaks all three guards.

  - [x] **6.3 DR-01 — guard the make lookup**
    - Today `vehicle.js:61-63` is `…filter(…)[0].name;` → selecting the blank placeholder makes
      `filter` return `[]` and throws an uncaught `TypeError`.
    - Target: on the blank option, clear `make`, `typeId`, `modelId` **and their dictionaries**, and
      issue **no** `PUT vehicle/make`.

  - [x] **6.4 DR-12 — clear the dictionaries, not just the values**
    - `clearDependantsVehicleFields` (`vehicle.js:102-107`) currently clears field values only, leaving
      `typeDictionary` and `modelDictionary` **stale**. Clear both.

  - [x] **6.5 Vehicle mutations — the four PUTs**
    - `updateProductionYear(n: number)` → `PUT vehicle/productionYear` (server side effect:
      `makeId=typeId=modelId=0`)
    - `updateMake({ makeId, make })` → `PUT vehicle/make` (object body → JSON; side effect:
      `typeId=0, modelId=0`)
    - `updateType(n: number)` → `PUT vehicle/type` (side effect: `modelId=0`)
    - `updateModel(n: number)` → `PUT vehicle/model`

  - [x] **6.6 ⛔ TRAP — DR-13 / ND-1: the vehicle recalculation asymmetry** ✅ **HELD.**
    All four handlers end in `store.vehicleFieldChanged('<field>')`; **zero** `recalculate()` calls
    anywhere under `components/vehicle/`. The component never decides — G4's exhaustive
    `RELOADS_QUOTE` table does. Three code comments name DR-13 at the non-recalculating handlers.
    ⚠️ **This step's acceptance criterion is unsatisfiable as written**: *"`grep -rn "recalculate"
    …/vehicle` returns exactly one call site"* returns **zero**, because G4 introduced
    `vehicleFieldChanged()`. The intent — at most one path from this component to a quote reload —
    is met more strongly: zero paths in the component, one decision table in the store. Restate as
    `grep -c "vehicleFieldChanged" → 4` **and** `grep -c "\.recalculate(" → 0` in G13.
    ✅ The test fires selections through `triggerEventHandler('selectionChange', …)` **on the
    rendered element**, not by calling the handler — a component with correct handlers but a
    template wired elsewhere would sail through a method-level test, which is exactly how this trap
    gets lost. Mutant 1 (`vehicleFieldChanged('type')` → `recalculate()`) was caught.
    - ⚠️ **This is the change an idiomatic reactive rewrite makes BY ACCIDENT. Losing it is a failed
      migration.**
    - **Only `updateModel()` calls `store.recalculate()`** (`vehicle.js:76`). `updateProductionYear`,
      `updateMake` and `updateType` **must not**.
    - The user-visible consequence is deliberate and must be preserved: from the moment the user
      touches Production Year until they re-pick a Model, the whole premium panel shows the **previous
      vehicle's** prices.
    - Changing this multiplies `GET quote` volume, each call a **full Higson rules evaluation**
      (`QuoteApi.java:30-35` runs `motor.calculate(quote)` on every GET). It is documented as intended
      in `standards/frontend/components.md`.
    - Guard it three ways: test 3 above (unit), the `vehicle-cascade.spec.ts` **NO BRANCH** rows
      (E2E, identical pre and post), and a code comment naming DR-13 at each of the three
      non-recalculating handlers.

  - [x] **6.7 `driver-history`** (`app-driver-history`) — 3 numeric fields
        (`driverHistory.html:8,18,27`)
    - `MatFormField` + `MatInput[type=number]`; endpoints `driver/accidentCount`,
      `driver/trafficTicketsCount`, `driver/licenceObtainedAtAge`.
    - **DR-03**: commit on **`(blur)`**, not on keystroke — matching the 7 text fields in
      `driver.html`. ⚠️ **The largest request-volume change in the migration**: typing `12` currently
      fires 2 PUTs + 2 full quote recalculations. `spec.md` §13 item 6 flags this as the most
      user-noticeable behavioural change; it belongs in the G13 release note.
    - **DR-02**: **guard before sending** — if the value is `null`, `NaN`, or violates `min`
      (`min=0`, `min=0`, `min=16`), issue **no** PUT and leave the last valid server value in place.
      Sending it today produces a **verified HTTP 500** (`ng-model` → `undefined` → no body → Spring
      binds null → `setNumberOfAccidents(null)` unboxes to `int` → NPE), and because the failure path
      has no handler **the user sees nothing**.
    - **DR-09**: read the driver from `store.driver`. ⛔ **No second `GET driver`** — delete
      `driverHistory.js:30-36`'s independent fetch rather than porting it.

  - [x] **6.8 Run ONLY the 6 new tests**
    - `cd frontend && npx vitest run src/app/components/vehicle src/app/components/driver-history`

**Acceptance criteria — G6**

- The 6 tests pass.
- **DR-01**: blank Make → no console error, **zero** `PUT vehicle/make`, Type and Model dropdowns empty.
- **DR-02**: cleared numeric field → **zero** PUT, **zero** 5xx, previous quote still rendered.
- **DR-03**: type `12` then blur → exactly **1** PUT + **1** `GET quote` (today 2 + 2).
- **DR-09**: exactly **1** `GET driver` at startup.
- **DR-12**: after a Production Year change, Type and Model dropdowns are **empty**, not stale.
- **DR-13 / ND-1**: year / make / type → **zero** `GET quote`; model → exactly **1**. Three code
  comments name DR-13.
- `grep -rn "recalculate" frontend/src/app/components/vehicle` returns exactly **one** call site.

---

### Task Group 7: Driver component and the datepicker — deliberately last

**Dependencies:** G4 (technically); sequenced after G6 by design
**Effort:** L (> 2 d)
**Rollback:** `rollback-plan.md` §5 row 4 — *"the datepicker is deliberately **last** (highest risk) so
a rollback there does not unwind everything behind it."* G-C7/G-F2 is the largest single item in the
migration and the only one rated **high** residual risk.

- [x] **7.0 Port the 7-field driver form and replace `uib-datepicker-popup`**

  - [x] **7.1 Write 6 focused tests first**

    | # | Test | Proves |
    |---|---|---|
    | 1 | `iso-date.adapter.spec.ts` — `formats and parses yyyy-MM-dd and alt-parses yyyy/MM/dd` | G-C7 |
    | 2 | `iso-date.adapter.spec.ts` — `the week starts on Monday and week numbers are hidden` | `driver.js:35-39` parity |
    | 3 | `driver.spec.ts` — `text fields commit on blur, not on keystroke` | `ng-model-options {updateOn:'blur'}` parity |
    | 4 | `driver.spec.ts` — `the birth date commits once, on datepicker close or input blur, never on keystroke` | **DR-04** |
    | 5 | `driver.spec.ts` — `re-clicking the already-selected gender toggle issues no PUT` | **DR-08** |
    | 6 | `driver.spec.ts` — `the gender value is set before the PUT is issued` | **DR-08** ordering |

  - [x] **7.2 The 5 text fields** (`driver.html:7,17,68,78,88`)
    - firstName, lastName, zipCode, city, street — `MatFormField` + `MatInput`, commit on **`(blur)`**,
      reproducing `ng-model-options="{updateOn: 'blur'}"`.
    - ⚠️ All five go through `DriverService`'s **string** methods — see G4 step 4.4. **No
      `JSON.stringify`, no hand-set `Content-Type`.**
    - Each calls `store.recalculate()` on success (matching `driver.js:96-98`).

  - [x] **7.3 Date of birth — `MatDatepicker`** (`driver.html:23-35`)
    - `<input matInput [matDatepicker]>` + `<mat-datepicker-toggle matSuffix>` replacing the
      `.glyphicon-calendar` trigger (`driver.html:34`).
    - **DR-04**: commit on **`(closed)` and `(blur)` only — never on keystroke.** Today the field has
      no `updateOn:'blur'`, so **every valid keystroke** fires a `PUT driver/birthDate` + a full
      recalculation.
    - The component holds a **`Date`**; the wire model is an ISO-8601 **string**. ⚠️ See G4 step 4.5 —
      `updateBirthDate` takes the `Date` object; passing a string yields **HTTP 415**.
    - Uses the `IsoDateAdapter` + `MOTOR_DATE_FORMATS` built in G4 step 4.8: display/parse
      `yyyy-MM-dd`, alt-parse `yyyy/MM/dd`, Monday-first, no week numbers, **no button bar**.
    - **DR-27**: `is-open="popup.opened"` (`driver.html:29`, `driver.js:42,45-47`) — a two-way binding
      into `$scope`, **not** `ctrl` — becomes a **single component signal**. No dual-scope binding.
    - **DR-17**: ⛔ do **not** port `$scope.selectedDate = new Date()` (`driver.js:33`) — it is dead,
      never bound.
    - **DR-19**: ⛔ do **not** port `close-text="Close"` (`driver.html:26,31`) — the button bar is
      disabled (`show-button-bar="false"`), so the text is unreachable.
    - ⚠️ **P-4 relevance**: `dateOfBirth` carries a server-timezone offset while the `MatDatepicker`
      adapter parses in **browser-local** time. Without `TZ=UTC` the displayed day can be off by one
      across machines. Every test in this group runs under `TZ=UTC`.

  - [x] **7.4 Gender — `MatButtonToggleGroup`** (`driver.html:38-53`)
    - `<mat-button-toggle-group [value]="gender()" (change)="onGender($event)">` with values `'M'` /
      `'F'`.
    - **DR-08** is satisfied **by construction**: `MatButtonToggleGroup.change` does not fire when the
      already-selected toggle is re-clicked, so the redundant PUT + full recalculate disappears.
      (Today both `ng-click` and `uib-btn-radio` sit on the same element; `uncheckable` is not set, so
      re-clicking leaves the model unchanged but **still fires `ng-click`**.)
    - **DR-08 ordering**: the handler sets the value **before** issuing the PUT — making explicit what
      AngularJS did implicitly through alphabetical directive ordering at equal priority (`btnRadio`
      writes the model, then `ngClick` reads it).

  - [x] **7.5 ND-3 — the "Address" divider stays inside the driver template**
    - `driver.html:55-60` emits the "Address" icon+label divider from **inside the driver component**,
      not from the sidebar. It is odd structure with **zero** user impact; restructuring risks layout
      drift for no gain. ⛔ **Recorded non-deviation — do not move it.**

  - [x] **7.6 ND-4 / ND-5 — add no validation and no mask**
    - ⛔ **No `<form>` element, no `required`, no `Validators`, no `mat-error`, no zip-code pattern.**
      There is none today; adding any is a **new feature** and unsanctioned scope creep.

  - [x] **7.7 Run ONLY the 6 new tests**
    - `cd frontend && TZ=UTC npx vitest run src/app/components/driver src/app/core/iso-date.adapter.spec.ts`

**Acceptance criteria — G7**

- The 6 tests pass under `TZ=UTC`.
- **DR-04**: typing a full date then blurring → exactly **1** `PUT driver/birthDate`.
- **DR-08**: clicking the already-active gender toggle → **zero** `PUT driver/gender`, **zero**
  `GET quote`.
- **DR-17**: `grep -rn "selectedDate" frontend/src` → **empty**.
- **DR-19**: `grep -rn "close-text\|closeText" frontend/src` → **empty**.
- **DR-27**: datepicker open state is **one** signal on the component; no dual-scope binding.
- **ND-3**: the "Address" divider markup lives in the driver component.
- **ND-4 / ND-5**: `grep -rnE "Validators|mat-error|required=|pattern=" frontend/src` → **empty**.
- **AC-C3**: `PUT driver/birthDate` sends a JSON-quoted ISO-8601 string with
  `Content-Type: application/json`.

---

### Task Group 8: Application shell, sidebar, spinner

**Dependencies:** G5, G6, G7
**Effort:** M (0.5-2 d)
**Rollback:** `rollback-plan.md` §5 row 5 — *"group 4 components are unaffected in isolation but the app
will not render — treat groups 4+5 as a rollback unit if group 5 fails."* In this plan's numbering:
treat **G5-G7 + G8** as one rollback unit if G8 fails.

- [x] **8.0 Assemble the single screen**

  - [x] **8.1 Write 4 focused tests first**

    | # | Test | Proves |
    |---|---|---|
    | 1 | `app.spec.ts` — `the shell renders layout only and contains no event-relay logic` | `main.js:21-23` deleted, not ported |
    | 2 | `spinner.spec.ts` — `the spinner is visible while any request is in flight and hidden at zero` | **AC-F5** |
    | 3 | `sidebar.spec.ts` — `the three section labels render, including the literal "Documents"` | **ND-2** |
    | 4 | `app.spec.ts` — `no router provider and no router-outlet exist` | **ND-7** |

  - [x] **8.2 `app`** (`app-root`) — `app.{ts,html,less}`
    - **DR-20**: selector `app-root` (never `main`).
    - ⛔ **`app.html` is pure layout. `app.ts` contains no relay logic at all** — `main.js:21-23` is
      **deleted, not ported**. The `QuoteStore` replaced it in G4.
    - Renders `<app-spinner>` under `@if (pendingRequests.count() > 0)`.
    - Menubar: 65px white bar, right-aligned, containing only `imgs/logo-insurance-app.svg`
      (206×38). No nav, no buttons, no interactivity (parity checklist §1).

  - [x] **8.3 `sidebar`** (`app-sidebar`)
    - Header "Get a Quote" + "fill in the details to get a quote"; scrollable content area; three
      sections: "Personal information" (`imgs/person.svg`) → `<app-driver>`; "Vehicle"
      (`imgs/vehicle.svg`) → `<app-vehicle>`; "Documents" (`imgs/doc.svg`) → `<app-driver-history>`.
    - **ND-2**: the third label is literally **"Documents"** even though it contains claim history. ⛔ A
      copy change is a product decision and is **out of scope**.
    - **DR-16**: ⛔ do **not** port `ctrl.sidebarHidden` / `ctrl.toggleSidebar()` (`sidebar.js:21-27`) —
      never referenced in `sidebar.html`. The matching CSS leftovers are deleted in G9.

  - [x] **8.4 `spinner`** (`app-spinner`)
    - `<mat-progress-spinner mode="indeterminate" [diameter]="60" [strokeWidth]="8">`, mapping
      `us-spinner="{radius:30, width:8, length:16}"` (`main.html:2`).
    - Driven by the `PendingRequests` signal from G4 — the **only** loading mechanism. There is no
      per-component loading state anywhere today and none must be added.
    - **ND-6**: the current spinner **flashes on load** because eight `templateUrl` XHRs pass through
      the interceptor. Compiled templates remove those requests. **The flash disappearing is a
      sanctioned disappearance (OD-13), not a regression** — do not add an artificial delay to
      reproduce it, and do not assert it.

  - [x] **8.5 ND-7 / ND-8 — two things that must not be "improved"**
    - ⛔ **No router.** No `provideRouter`, no `<router-outlet>`. `ApplicationConfig.java`'s single
      `addViewController("/")` stays sufficient and **unchanged**.
    - ⛔ **`Application.java` stays unchanged** — no `SpringBootServletInitializer`, no
      external-Tomcat deployability. It is current, intended behaviour; do not accidentally fix it.

  - [x] **8.6 Run ONLY the 4 new tests, then verify the screen end-to-end**
    - `cd frontend && npx vitest run src/app/app.spec.ts src/app/components/spinner src/app/components/sidebar`
    - `mvn clean package -Pall_in_one` and re-run the **AC-B4 three-context-path** check from G3
      step 3.9 — this time the real screen must render, not a placeholder.

**Acceptance criteria — G8**

- The 4 tests pass.
- The single screen renders end-to-end at `/demo`, `/` **and** `/app`.
- **DR-16**: `grep -r "sidebarHidden\|toggleSidebar" frontend/src` → **empty**.
- **ND-2**: the rendered sidebar label is exactly `Documents`.
- **ND-7**: `grep -rn "provideRouter\|router-outlet" frontend/src` → **empty**;
  `git diff --name-only -- src/main/java/**/ApplicationConfig.java` → **empty**.
- **ND-8**: `git diff --name-only -- src/main/java/**/Application.java` → **empty**.
- **AC-F5**: the global spinner appears for **every** in-flight request and disappears at zero, on
  success **and** on error.
- **AC-C5**: `grep -rn "HttpClient" frontend/src/app/components` → still **zero** hits.
- **AC-C8** (interim): `git diff --name-only src/main/java` is still **empty** — the backend has not
  been touched yet.

---

### Task Group 9: Styles, theme and assets

**Dependencies:** G8
**Effort:** M (0.5-2 d)
**Parallel with:** G10
**Rollback:** `rollback-plan.md` §5 row 6 — *"purely cosmetic; a rollback leaves an ugly but functional
app."*
**⚠️ Appearance is OUT of parity scope (C-9). Nothing in this group is a regression gate. A visual
difference is never, on its own, a finding.** The screenshot diff is **reviewed**, never asserted.

- [x] **9.0 Move the styling to LESS-under-Angular with a Material-themed brand seed**

  - [x] **9.1 Write 3 focused checks first** (assertion-style, not visual)

    | # | Check | Proves |
    |---|---|---|
    | 1 | E2E: no network response in the 4xx range for any font or image request during a full flow | G-E2/G-E3 — silent 404s on re-anchored `url()` |
    | 2 | E2E: the computed `cursor` on a variant card is `default`, and no hover rule targets it | **DR-15** |
    | 3 | Static: the deleted-asset and dead-selector greps below are all empty | **DR-24**, **DR-25**, **DR-16** |

  - [x] **9.2 `theme.scss` — the single Sass file, the single brand seed**
    - `@use '@angular/material' as mat; @include mat.theme(...)`; `$primary` derived from **`#4C4CDE`**,
      `--mat-sys-corner-*` set to **`8px`** (`less/variables.less:1-14`).
    - Listed **first** in `angular.json`'s `styles` array so LESS overrides win the cascade.
    - ⚠️ The **only** `.scss` in the workspace (AC-S7). Material's theming API is Sass-only; there is
      no LESS equivalent. `@angular/build` already bundles `sass`, so it costs no new dependency. This
      is a deliberate, scoped, documented exception to `css.md`.
    - **Recognisability, not parity**: the brand tokens keep the app recognisable even though every
      control changed. This is not a parity requirement.

  - [x] **9.3 LESS migration — carried, rewritten, deleted**

    | Source | Fate |
    |---|---|
    | `less/variables.less` (14) | → `src/styles/_variables.less`, **trimmed**: keep only what Material has no token for (`@color-border-block: #ECECEC`, `@color-gray-light: #f2f2f2`, the tooltip border colour). Where a Material token exists, reference `var(--mat-sys-primary)` rather than re-declaring the hex |
    | `less/config.less:1,5` (`@import '../node_modules/…'`) | **deleted** — Open Sans and Font Awesome both go (C-10) |
    | `less/config.less:4` (Manrope from the Google Fonts CDN) | → `@font-face` over the **self-hosted** variable font in `_typography.less` |
    | `less/config.less:8-14` (body gradient, Manrope 13px) | → `_typography.less`, **kept verbatim** |
    | `less/config.less:16-20` (`.container` 1440px @≥1200px) | → `_layout.less` as a CSS Grid container |
    | `less/config.less:22-42` (angular-tooltips DOM) | ⚠️ **REWRITTEN** for the CDK panel, not ported (G-E5) |
    | `less/common/common.less` (143) | **mostly deleted** — `.common-input-*` and `.common-btn-group` are superseded by `mat-form-field` / `mat-button-toggle` (C-9). `.common-tooltip`'s `min-width: 340px` survives into the popover panel. `.common-checkbox` and `.common-subsection-heading` are **dead** (DR-25) |
    | `less/main/main.less` (69) | → `styles/main.less`; **keep** the `.spinner` positioning (`:6-12` — `top:50%; left:56%; transform:translate(-50%,-50%); z-index:9999`) and the webkit scrollbars (`:14-32`); logo at `:51` re-anchored |
    | `less/sidebar/sidebar.less` (100) | → `styles/sidebar.less`; **delete** the `transition: 0.5s` / `opacity: 1` collapse leftovers at `:33-36` (**DR-16**); 4 icon backgrounds re-anchored |
    | `less/variants/{variants,coverage,discounts}.less` | → `styles/variants/*`; **drop** `.coverage__divider` (**DR-25**) and the `cursor:pointer`/hover block at `variants.less:11-27` (**DR-15**) |
    | `less/dropdown/dropdown-menu.less` (72) | **deleted** — it re-skins the Bootstrap 3 `.dropdown-menu` for the uib datepicker (G-E4) |

  - [x] **9.4 Layout without Bootstrap 3** (C-9)
    - `main.html:1-22`'s `.container` / `.row` / `.col-md-3` / `.col-md-9` becomes **CSS Grid** in
      `_layout.less`: `max-width: 1440px`, `grid-template-columns: 25% 75%`, collapsing to a single
      column below **`992px`** — Bootstrap's `md` breakpoint, **preserved so the collapse point does
      not move** (`responsive.md`).
    - ⛔ **`@angular/cdk/layout`'s `BreakpointObserver` is NOT introduced.** It is a TypeScript API for
      components that need to *react* to viewport changes; no component here does — the layout is
      static two-column-or-one. Using it to do what a media query already does violates
      `minimal-implementation.md`. This is a deliberate, recorded deviation from the literal wording of
      C-9 (`spec.md` §4.9).

  - [x] **9.5 Assets → `frontend/public/`** (C-10)
    - **Carried** (byte-identical, `url()` re-anchored to `imgs/…` — assets land at the bundle root and
      the CLI rewrites `url()` relative to the emitted stylesheet, which also sits at the bundle root):
      `logo-insurance-app.svg`, `person.svg`, `vehicle.svg`, `doc.svg`, `address.svg`,
      **`arrow-tooltip.svg`** (kept — used by the CDK popover), `favicon.ico`.
    - **Dropped**: `imgs/arrow.svg` — its only call site is the custom select chevron
      (`less/common/common.less:56`), which `mat-select`'s own indicator replaces.
    - **DR-24**: `imgs/logo.png` and `imgs/select-arrow.png` — unreferenced, not carried over.
    - **Manrope self-hosted** at `frontend/public/fonts/manrope/Manrope[wght].woff2` — one variable
      font file covering `wght 200..800`, matching the current CDN query. Removes a runtime CDN
      dependency at application start. ⚠️ This commits **one** binary; `build.md`'s "vendored assets
      are never committed" targets *build output*, and C-10 pins this location. **Net: −96 tracked
      binaries, +1.**
    - ⛔ **The Material Icons font is NOT added.** The only glyph in the application
      (`.glyphicon-calendar`, `driver.html:34`) is replaced by `<mat-datepicker-toggle>`'s built-in SVG
      icon. If a future call site needs `<mat-icon>`, self-host the font **then**, not now
      (`minimal-implementation.md`).
    - ⚠️ The **source** files under `src/main/webapp/{imgs,fonts}/` are **copied, not moved**.
      Deletion happens in G12 — nothing under `src/main/webapp/` is removed before G11 passes.

  - [x] **9.6 Capture the "after" screenshots — documentation only**
    - Store under `e2e/goldens/screenshots/` alongside the `pre` set from G2.
    - ⛔ **Never a pass/fail assertion.** `toHaveScreenshot()` must not appear in any spec. Their
      purpose is a reviewable record of the redesign for the G13 documentation.

  - [x] **9.7 Run ONLY the 3 checks from 9.1**

**Acceptance criteria — G9**

- The 3 checks pass.
- **No 404s** on any font or image during a full E2E flow.
- **DR-15**: computed `cursor` on a variant card is `default`; no hover-state rule targets it.
- **DR-16**: no `transition`/`opacity` collapse rules remain in `styles/sidebar.less`.
- **DR-24**: `logo.png` and `select-arrow.png` are absent from `frontend/public/`.
- **DR-25**: `grep -rn "coverage__divider\|common-checkbox\|common-subsection-heading" frontend/src/styles`
  → **empty**.
- **AC-S7**: `find frontend/src -name '*.scss'` returns exactly `frontend/src/theme.scss`.
- Bootstrap 3 appears nowhere in the dependency set:
  `grep -rn "bootstrap" frontend/package.json` → **empty**.
- The `992px` collapse point is preserved; `grep -rn "BreakpointObserver" frontend/src` → **empty**.
- Screenshots captured and **reviewed**, not gated. ⚠️ **A visual difference is not a finding.**

---

### Task Group 10: Backend contract fix — C-6 / DR-14, and the deliberate golden update

**Dependencies:** G8
**Effort:** S (≤ 0.5 d)
**Parallel with:** G9
**Rollback:** `rollback-plan.md` §5 row 7 — *"revert one Java file + one golden. Isolated by design — it
is the only backend change, kept in its own commit so it can be rolled back independently of the
frontend."*

⚠️ **This group exists as a separate group precisely so the contract flip is deliberate.** G1 test 8
recorded today's 404-with-no-message as a golden. This group **replaces** that test in the same commit
that makes the change. An undocumented golden edit is a regression; a documented one is the register
working as designed (`spec.md` §6.4).

- [x] **10.0 Make failed quote calculations return a readable message**

  - [x] **10.1 Write the 3 tests first — the deliberate flip**

    | # | Test | Action |
    |---|---|---|
    | 1 | `QuoteApiTest.quoteReturnsFourHundredWithValidationMessage()` | **NEW.** Blank the first name, `GET quote` → **400**, body exactly `{"message":"Driver first name not supplied."}` |
    | 2 | `QuoteApiTest.quoteReturnsFourOhFourWithNoMessageKey()` | **DELETED** in the same commit. The commit message and a Javadoc note on test 1 must cite **DR-14** as the authority |
    | 3 | `e2e/specs/error-paths.spec.ts` (`post` branch) | the banner shows the actual `QuoteValidator` message, not empty text |

  - [x] **10.2 The change — one file, ~10 lines**
    - `pl.decerto.higson.demo.motor.exceptions.GlobalExceptionHandler`: `@ControllerAdvice` →
      **`@RestControllerAdvice`**; both handlers (`:13-15`, `:19-21`) return the new
      `pl.decerto.higson.demo.motor.api.dto.ErrorDto`.
    - **Root cause being fixed**: the handlers currently return `String` **without `@ResponseBody`**, so
      Spring MVC treats the return value as a **view name**; view resolution fails and the resulting
      **404 overrides the `@ResponseStatus(BAD_REQUEST)`** already present at `:12` and `:18`. With
      `@RestControllerAdvice` those annotations take effect.
    - `ErrorDto` — a **mutable POJO** with a single `message` field in `api/dto/`
      (`models.md`: no records, no builders, no Lombok; `api.md`: DTOs cross the API boundary).
    - This restores the messages `example/QuoteValidator.java` was written to produce: *"Driver birth
      date not supplied."*, *"Driver first name not supplied."*, *"Driver gender not supplied."*,
      *"Driver address not supplied."*, *"Driver address zip code / city / street not supplied."*,
      *"Vehicle production year / make / type / model not supplied."*
    - ⛔ `spring.error.include-message` stays at its default (`never`) — the message now comes from the
      handler, not from Spring's `/error` machinery. **No property change.**

  - [x] **10.3 Wire the message into the existing banner — no new UI surface**
    - `variants` renders `ErrorDto.message` in the `<mat-card role="alert" data-testid="error-banner">`
      built in G5, falling back to a generic message when `message` is absent.
    - ⛔ **No new error UI.** C-7 keeps DR-06/DR-14 inside the existing banner.

  - [x] **10.4 Update the goldens deliberately, citing DR-14**
    - `e2e/goldens/error-paths.json`: the affected keys change; the commit message and
      `e2e/README.md` both cite **DR-14** as the authority.
    - ⛔ **This is the ONLY golden that may change without being a regression.** Everything else must
      replay byte-identical or map to a `MIGRATION_STAGE` delta-table row from G2 step 2.5.

  - [x] **10.5 Run ONLY the 3 tests**
    - `mvn test -Dtest=QuoteApiTest`
    - `cd e2e && MIGRATION_STAGE=post TZ=UTC npx playwright test specs/error-paths.spec.ts`

**Acceptance criteria — G10**

- The 3 tests pass.
- **DR-14**: `GET quote` with a blanked first name → **400**, body `{"message":"Driver first name not supplied."}`.
- The banner shows that text (E2E).
- **AC-C8**: `git diff --name-only src/main/java` lists **exactly two** paths —
  `exceptions/GlobalExceptionHandler.java` and the new `api/dto/ErrorDto.java`. Nothing else in
  `src/main/java` changed (47 of 48 pre-existing files untouched).
- The golden diff is annotated `DR-14` in the commit message and in `e2e/README.md`.
- ⛔ `ApplicationConfig.java` and `Application.java` are unchanged (ND-7, ND-8).

---

### Task Group 11: Parity verification — replay the goldens

**Dependencies:** G9, G10
**Effort:** M (0.5-2 d)
**Rollback:** `rollback-plan.md` §5 row 8 — *"no code change; this group produces a verdict, not a
commit."*
**⚠️ `src/main/webapp/`, `less/`, `gulpfile.js` and the root `package.json` are STILL PRESENT. This is
the last verification before the irreversible step.**

- [x] **11.0 Replay the G1/G2 goldens against the new build and adjudicate every diff**

  - [x] **11.1 Replay the 6 Playwright flows in `post` mode — the same spec files, unmodified**
    ```bash
    mvn higson-cli:pull
    TZ=UTC mvn clean package -Pall_in_one
    TZ=UTC java -jar target/motor-insurance.war
    cd e2e && MIGRATION_STAGE=post TZ=UTC npx playwright test
    ```
    - **AC-F2**: the specs must be the **same files** used for capture. `git diff pre-angular-migration -- e2e/specs/`
      must show only the `post` branches added in G2 step 2.5 — no selector changes, no assertion
      rewrites.

  - [x] **11.2 Replay the MockMvc contract tests**
    - `mvn test -Dtest='DriverApiTest,VehicleApiTest,DictionaryApiTest,QuoteApiTest'`
    - **AC-C1**: all 21 endpoints respond as pinned; **only the DR-14 row differs** from the P-3
      capture, and that difference is annotated `DR-14`.

  - [x] **11.3 Adjudicate every diff against the delta table**
    - ⛔ **Do NOT update goldens to match.** Every difference must map to either **C-9** (appearance —
      ignored by construction, since no spec asserts appearance) or a **DR-nn** row. **An untraceable
      diff is a regression** (`rollback-plan.md` §6).
    - Walk the G2 step 2.5 delta table row by row and confirm each `pre`→`post` change actually
      occurred, and that every **NO BRANCH** row is byte-identical.
    - ⚠️ **Check the two `vehicle-cascade` NO BRANCH rows first.** If `GET quote` now fires on a
      production-year, make or type change, **ND-1 / DR-13 was lost — that is a failed migration.**
      Revert the vehicle component commit and re-do it against the DR-13 acceptance criterion; do not
      patch forward.

  - [x] **11.4 Walk the parity checklist**
    - **AC-F1**: every row of `current-state-analysis.md` §"BEHAVIOURAL PARITY CHECKLIST" (12 sections)
      passes, **or** is traceable to a DR-nn row or to C-9.
    - **AC-F3**: request counts match §7's per-DR expectations exactly.

  - [x] **11.5 Walk the register and the non-deviations**
    - All **27** DR rows: each traceable to the group that implemented it and to the test that proves
      it (see the traceability matrix below).
    - All **8** ND rows: each verified unchanged.

  - [x] **11.6 Run the code-shape greps** (AC-S1…AC-S8, AC-C2…AC-C6)
    ```bash
    grep -rn "standalone: true"            frontend/src   # expect empty (AC-S1)
    grep -rnE "@Input|@Output"             frontend/src   # expect empty (AC-S2)
    grep -rnE "\*ngIf|\*ngFor|ngClass|ngStyle" frontend/src  # expect empty (AC-S2)
    grep -rn "BehaviorSubject"             frontend/src   # expect empty (AC-S3)
    grep -rn ": any"                       frontend/src   # expect empty (AC-S4)
    grep -rn "LOCALE_ID\|registerLocaleData" frontend/src # expect empty (AC-S5, DR-11)
    grep -n  "<base"  frontend/src/index.html             # expect empty (AC-S6)
    find frontend/src -name '*.scss'                      # expect only theme.scss (AC-S7)
    grep -rn "bypassSecurityTrustHtml"     frontend/src   # expect exactly 1 (AC-S8)
    grep -rn "JSON.stringify"              frontend/src   # expect empty (AC-C2)
    grep -rnE "http\.(get|put)\('/"        frontend/src   # expect empty (AC-C4)
    grep -rn "HttpClient" frontend/src/app/components     # expect empty (AC-C5)
    grep -rn "\.data" frontend/src/app                    # expect no response.data (AC-C6)
    ```
    - **AC-F6**: no console error during any of the 6 flows.
    - **AC-C7**: the session survives a full wizard flow — one `JSESSIONID`, no mid-flow quote reset.

  - [x] **11.7 Produce the verdict**
    - Written adjudication: for each observed diff, the DR-nn or C-9 authority. Any unexplained diff
      **blocks G12**.
    - ⛔ **Do not proceed to G12 until this group is green.** G12 is the point of no easy return.

**Acceptance criteria — G11**

- 6 Playwright flows green in `post` mode using the **same spec files** as capture (AC-F2).
- 8 MockMvc contract tests green; only the DR-14 row differs from the P-3 capture (AC-C1).
- Every parity-checklist row passes or is traced to DR-nn / C-9 (AC-F1).
- Request counts match per-DR expectations exactly (AC-F3).
- **ND-1 / DR-13 verified byte-identical pre and post** — the single hardest-to-notice failure mode.
- All 13 code-shape greps return their expected results.
- Zero console errors (AC-F6); session semantics intact (AC-C7).
- ⚠️ **Zero untraceable diffs.** Screenshots reviewed, not gated — a visual difference is not a finding.

---

### Task Group 12: CUTOVER — deletion of the AngularJS frontend

**Dependencies:** G11 (green)
**Effort:** S (≤ 0.5 d)
**Rollback:** `rollback-plan.md` §5 row 9 — ⚠️ ***"the point of no easy return. Recoverable via §4.1-4.3
but no longer a one-line revert. Do this ONLY after group 8 [= G11 here] passes. Keep it as a single,
isolated commit titled so it is obvious in `git log`."*** Recovery paths are §4.1 (`styles.min.css`),
§4.2 (the 96 fonts), §4.3 (`package-lock.json`, mitigated by the G0 step 0.6 archive).

- [x] **12.0 Delete the AngularJS application in one isolated commit**

  - [x] **12.1 Delete the SPA sources**
    - `src/main/webapp/{index.html, app.js, components/**}` — 24 files, 920 lines.
    - ⛔ **Keep the `src/main/webapp/` directory itself**, with a `.gitkeep`, as the
      `warSourceDirectory` — so nothing else in the inherited WAR lifecycle has to change.

  - [x] **12.2 Delete the generated-but-committed stylesheet** (**DR-21**)
    - `git rm src/main/webapp/styles/styles.min.css` — 2946 lines, Gulp output, **committed**, and not
      actually minified. Contradicts both `css.md` and `build.md`.

  - [x] **12.3 Delete the dead assets**
    - `src/main/webapp/fonts/**` — **96 files**. Open Sans (90) is dead: imported at
      `less/config.less:1` but overridden by Manrope in the body rule at `:9-16`. Font Awesome (6) has
      **zero call sites** — audited; the only reference is the LESS import at `config.less:5`.
    - `src/main/webapp/imgs/**` — after G9 copied the 6 keepers to `frontend/public/`. **DR-24**
      (`logo.png`, `select-arrow.png`) and `arrow.svg` go with it.
    - `src/main/webapp/vendors/` — the concept dies; the bundler resolves imports.

  - [x] **12.4 Delete the build tooling**
    - `less/` — 10 files, 599 lines (re-authored under `frontend/src/styles/` in G9).
    - `gulpfile.js` (**DR-23** — its `gulp.watch` at `:43-45` was broken anyway; `ng serve` replaces it).
    - Root `package.json` (**DR-22** — takes `merge-stream` at `:17` with it) and root
      `package-lock.json`, and with them all **10** AngularJS-era npm dependencies: `angular`,
      `angular-ui-bootstrap`, `angular-tooltips`, `angular-loading-spinner`, `angular-spinner`,
      `spin.js`, `bootstrap`, `font-awesome`, `npm-font-open-sans`, `merge-stream`.

  - [x] **12.5 Run the 7 cutover checks — the "tests" for this group**
    1. **AC-B1**: `mvn clean package` **from a clean clone** produces `target/motor-insurance.war`
       containing `index.html` + `main-*.js` at the WAR root
    2. **AC-B2**: `mvn clean package -Pall_in_one` produces an executable WAR that still contains the UI
    3. **AC-B4**: the WAR serves and `GET quote` succeeds at `/demo`, `/` **and** `/app`
    4. **AC-B6 / DR-21**: `git ls-files | grep -E "dist/|styles.min.css|node_modules"` → **empty**
    5. **DR-22 / DR-23**: root `package.json`, root `package-lock.json` and `gulpfile.js` no longer
       exist; `cd frontend && npm start` serves with HMR
    6. **AC-B7**: `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh`, `docker/app.properties` are
       **byte-identical** to `HEAD` before the migration
    7. Replay: 6 Playwright flows (`post`) + 8 MockMvc tests still green after the deletion

  - [x] **12.6 Verify the escape hatches before committing**
    - `~/motor-insurance-angularjs-node_modules.tgz` exists (G0 step 0.6).
    - `git tag pre-angular-migration` still resolves and its tree still contains
      `src/main/webapp/index.html`, `styles/styles.min.css` and `fonts/`.

**Acceptance criteria — G12**

- All 7 checks pass, **from a clean clone** (plus `mvn higson-cli:pull` — P-2).
- One isolated commit, titled so it is obvious in `git log`.
- `src/main/webapp/` exists and contains only `.gitkeep`.
- **DR-21, DR-22, DR-23, DR-24, DR-25, DR-26** all verified.
- **AC-B3**: `finalName` is `motor-insurance`; the WAR path scripts reference is unchanged.
- **AC-B9**: port 48080 unchanged in `higson-demo-app.properties`, `docker/app.properties`, `EXPOSE`
  and the compose mapping.
- ⛔ **No Docker image was rebuilt or pushed at any point during the migration.**
  `push-to-docker.sh` carries a hardcoded `VERSION=4.3.0`; publishing mid-migration would overwrite a
  tag that currently contains a working AngularJS build — **the one rollback target outside git**
  (`rollback-plan.md` §5 anti-sequencing).

---

### Task Group 13: Documentation and standards

**Dependencies:** G11 (may start in parallel with G12)
**Effort:** S (≤ 0.5 d)
**Rollback:** `rollback-plan.md` §5 row 10 — documentation only, revert freely.

- [x] **13.0 Bring the standards and project docs in line with what was built**

  - [x] **13.1 The two standards changes that prevent future wrong code** (highest value — G-H4, G-H11)
    - ⚠️ **`standards/frontend/services.md`**: **DELETE** the clause *"callers resolve `response.data`
      themselves"*. It describes `$http`. Under `HttpClient` it will **actively generate wrong code** —
      a leftover `.data` yields `undefined`, not an error, across 11 call sites (G-B2, R-6).
    - ⚠️ **`standards/frontend/build.md`**: **ADD** the base-href ↔ context-path rule — *ship no
      `<base>` element; the servlet context path is a runtime value* (`spec.md` §5.6). Also update the
      obsolete `prepare:libs` vendor procedure and the gulp entry point, and extend the exact-pin rule
      to `@angular/*` / `typescript` / `rxjs`.

  - [x] **13.2 The remaining frontend standards**
    - `css.md` — the Gulp → `styles.min.css` sentence is factually dead; record the LESS-under-Angular
      pipeline and the **documented `theme.scss` Sass exception**.
    - `components.md` — all four project rules (IIFE, `controllerAs: 'ctrl'`, `templateUrl`, the
      `$emit` relay) are superseded. Note that `components.md:3` already scopes them to legacy code, so
      this is an **update, not a conflict**. Record the v20+ idiom set (`OnPush`, `input()`/`output()`,
      `inject()`, `@if`/`@for` with `track`, no `standalone: true`).
    - NEW `standards/frontend/typescript.md` — `strict: true`, no `any` (G-H9).
    - NEW `standards/frontend/state.md` — where `QuoteStore` replaced the event bus, **including the
      DR-13 / ND-1 invariant** (G-H10).
    - NEW frontend test standard — Vitest + the Playwright selector policy; `test-writing.md` is
      Java/JUnit-only (G-H12).

  - [x] **13.3 `standards/global/conventions.md`** (G-H5)
    - ⚠️ "Required Local Toolchain Baseline" currently says only *"NodeJS"* — **that vagueness is
      exactly what let Node 23.6.1 onto this machine.** Name the Node floor explicitly:
      `^22.22.3 || ^24.15.0 || >=26.0.0`.

  - [x] **13.4 Project documentation — in Polish** (`global/documentation.md`)
    - `project/tech-stack.md` — replace the frontend sections and the migration path (currently
      "Angular 17+ / React 18+ / Vue 3+", "Gulp → Vite/esbuild") with the actual outcome.
    - `project/architecture.md` — the SPA component/service inventory and the `$emit`/`$broadcast`
      data-flow description (now `QuoteStore` signals).
    - ⛔ Standards and `INDEX.md` stay in **English**; project docs stay in **Polish**.

  - [x] **13.5 `Readme.md`** (G-H8) — build prerequisites (the Node floor), the `frontend/` layout, the
        `ng serve` dev loop, and the `e2e/` harness commands.

  - [x] **13.6 Release notes — the three user-visible changes**
    - **DR-03** (`spec.md` §13 item 6): the three numeric fields move from keystroke to blur, so the
      quote no longer updates while typing. **The most noticeable behavioural change in the migration.**
    - **DR-14**: failed quote calculations now show the actual validation message instead of an empty
      red banner.
    - **C-9**: the entire UI moved to Angular Material — appearance changed by design.

  - [x] **13.7 Regenerate `INDEX.md`** — `global/documentation.md` requires the index to stay in sync
        whenever documentation is added, removed or significantly changed.

  - [x] **13.8 Record the deferred items** (`spec.md` §11, do **not** implement)
    - Remaining EOL debt; `<html lang="pl">` vs English UI copy (§13 item 1); `frontend-maven-plugin`
      when CI arrives; `@angular/cdk/layout` if a component ever needs to react to breakpoints in
      TypeScript; adding `lang` to the defect register in step 2.

**Acceptance criteria — G13**

- `INDEX.md` regenerated and accurate.
- The `response.data` clause is **gone** from `services.md`; the no-`<base>` rule is **present** in
  `build.md`.
- `conventions.md` names a concrete Node floor.
- Project docs updated **in Polish**; standards and `INDEX.md` **in English**.
- Release notes call out DR-03, DR-14 and C-9.
- ⛔ No code change in this group.

---

## Traceability matrix — every register entry to its group and its test

**AC-D\* are the §7 defect-register criteria. This matrix satisfies plan requirement 4: each entry is
traceable to the group that implements it and to the test that proves it.**

| ID | Implemented in | Proven by |
|---|---|---|
| **DR-01** | G6 (6.3) | `vehicle.spec.ts` test 1; `vehicle-cascade.spec.ts` blank-option case |
| **DR-02** | G6 (6.7) | `driver-history.spec.ts` test 5; `driver-history.spec.ts` cleared-field delta row |
| **DR-03** | G6 (6.7) | `driver-history.spec.ts` test 4; E2E `1 PUT + 1 GET quote` delta row |
| **DR-04** | G7 (7.3) | `driver.spec.ts` test 4; `driver-fields.spec.ts` date delta row |
| **DR-05** | G5 (5.4) | G1 test 7 (`positions ascending`); `variants.spec.ts` test 2; `quote-panel.spec.ts` **NO BRANCH**; `grep -rniE "\.sort\(\|orderBy"` |
| **DR-06** | G4 (4.7) | `mutation-error.interceptor.spec.ts` test 5; `error-paths.spec.ts` forced-500 delta row |
| **DR-07** | G4 (4.6) + G5 (5.4) | `variants.spec.ts` test 3; `error-paths.spec.ts` zero-cards delta row |
| **DR-08** | G7 (7.4) | `driver.spec.ts` tests 5 & 6; `driver-fields.spec.ts` re-click delta row |
| **DR-09** | G4 (4.6) + G6 (6.7) | `quote-store.spec.ts` test 3; `startup.spec.ts` `GET driver` 2→1 delta row |
| **DR-10** | G5 (5.2, 5.3, 5.4) | `coverage.spec.ts` test 1; `quote-panel.spec.ts` money-regex delta row |
| **DR-11** | G3 (3.6) + G5 (5.2) | `coverage.spec.ts` test 1; `quote-panel.spec.ts` **NO BRANCH**; `grep LOCALE_ID` |
| **DR-12** | G6 (6.4) | `vehicle.spec.ts` test 2; `vehicle-cascade.spec.ts` stale-dictionary delta row |
| **DR-13** | G4 (4.6) + G6 (6.6) | ⚠️ `quote-store.spec.ts` **test 2** + `vehicle.spec.ts` **test 3** + `vehicle-cascade.spec.ts` **two NO BRANCH rows** |
| **DR-14** | G10 (10.2) | `QuoteApiTest.quoteReturnsFourHundredWithValidationMessage()`; `error-paths.spec.ts` banner-text delta row |
| **DR-15** | G5 (5.6) + G9 (9.3) | G9 check 2 (computed `cursor` is `default`) |
| **DR-16** | G8 (8.3) + G9 (9.3) | `grep -r "sidebarHidden\|toggleSidebar" frontend/src`; no collapse CSS |
| **DR-17** | G7 (7.3) | `grep -r "selectedDate" frontend/src` |
| **DR-18** | G5 (5.3) | `grep -ri "priceBeforeDiscount" frontend/src` |
| **DR-19** | G7 (7.3) | `grep -r "close-text\|closeText" frontend/src` |
| **DR-20** | G3 (3.6) | `frontend/src/index.html` contains `<app-root>`; no selector `main` |
| **DR-21** | G12 (12.2) | `git ls-files \| grep styles.min.css` → empty |
| **DR-22** | G12 (12.4) | root `package.json` no longer exists |
| **DR-23** | G12 (12.4) | `gulpfile.js` deleted; `npm start` in `frontend/` serves with HMR |
| **DR-24** | G9 (9.5) + G12 (12.3) | absent from `frontend/public/` |
| **DR-25** | G9 (9.3) | absent from `frontend/src/styles/` |
| **DR-26** | G3 (3.2, 3.9) | production build succeeds with default optimization |
| **DR-27** | G7 (7.3) | one signal on the component; no dual-scope binding |
| **ND-1** | G4 (4.6) + G6 (6.6) | ⚠️ same evidence as DR-13 — **the two NO BRANCH E2E rows are the proof** |
| **ND-2** | G8 (8.3) | `sidebar.spec.ts` test 3 — rendered label is exactly `Documents` |
| **ND-3** | G7 (7.5) | divider markup lives in the driver component |
| **ND-4** | G7 (7.6) | `grep -rnE "Validators\|mat-error\|required=" frontend/src` → empty |
| **ND-5** | G7 (7.6) | free-text zip input, no `pattern` |
| **ND-6** | G2 (2.1) + G8 (8.4) | **not asserted**; documented in `e2e/README.md` |
| **ND-7** | G3 (3.5) + G8 (8.5) | no `provideRouter` / `<router-outlet>`; `ApplicationConfig.java` unchanged |
| **ND-8** | G8 (8.5) | `Application.java` unchanged |

---

## Trap index — where each one bites

| Trap | Group / step | One-line rule |
|---|---|---|
| Node 23.6.1 rejected, fails opaquely inside `prepare-package` | **G0 / 0.1** | `node -v` before any `ng` or `npm` command. Nothing committed (C-5) |
| Snapshot not in git — no rules, no goldens | **G0 / 0.2** | `mvn higson-cli:pull` first, and again after any fresh clone |
| Runtime capture is not recoverable by `git revert` | **G1, G2** | Capture before touching anything; tag `pre-angular-migration` |
| `JSON.stringify` on a string PUT body corrupts it silently | **G4 / 4.4** | Never stringify, never hand-set `Content-Type`. `grep JSON.stringify` → empty |
| `PUT driver/birthDate` needs a `Date` OBJECT — a string yields **415** | **G4 / 4.5** | ⚠️ Pulls **opposite** to 4.4 for an adjacent endpoint. `updateBirthDate(value: Date)` |
| Leftover `response.data` yields `undefined`, not an error | **G4 / 4.3** | `HttpClient` already unwraps. `grep "\.data"` → empty |
| `<base>` element breaks the `/app` context path | **G3 / 3.5** | Ship none; set no `baseHref`. Verify at three context paths |
| Pinning `maven-war-plugin` clobbers Spring Boot's `<archive>` block | **G3 / 3.7** | Omit `<version>` — Spring Boot manages it at 3.4.0 |
| `less` is an *optional* peer of `@angular/build`, which ships `sass` | **G3 / 3.2** | Explicit `less@^4.2.0` devDependency |
| `typescript@latest` is 7.0.2 and breaks the build | **G3 / 3.2** | Pin `~6.0.3` (peer is `>=6.0 <6.1`) |
| The backend already sorts — a client-side sort is unsanctioned | **G5 / 5.4** | Remove `orderBy`, add **nothing** |
| `DictionaryEntryDto.code` is a **String** | **G6 / 6.2** | Explicit `Number()` at the binding site |
| Nullable vehicle types break the falsy-`0` cascade guards | **G6 / 6.2** | Type `number`, never `number \| null` |
| ⚠️ **ND-1 / DR-13 — the asymmetry an idiomatic rewrite reconnects by accident** | **G4 / 4.6 + G6 / 6.6** | Only `updateModel()` calls `recalculate()`. Own unit test, own E2E assertion, three code comments. **Losing it is a failed migration** |
| Emitting build output into `src/main/webapp/` | **G3 / 3.3** | Emit to `frontend/dist/`; `mvn clean` does not touch `src/main/webapp` |
| Dev proxy cookie path resets the session on every keystroke | **G4 / 4.9** | Run dev backend at root context, or `cookiePathRewrite` |
| `MatTooltip` silently drops the HTML description | **G5 / 5.5** | CDK Overlay popover + `bypassSecurityTrustHtml`, once, commented |
| Updating a golden to make a replay pass | **G11 / 11.3** | Every diff maps to C-9 or a DR-nn row, or it is a regression |
| Deleting AngularJS files "as you go" | **G12** | Deletion is one isolated commit, only after G11 is green |
| Pushing a Docker image mid-migration | **all groups** | `push-to-docker.sh` hardcodes `VERSION=4.3.0` — publishing overwrites the only rollback target outside git |

---

## Standards compliance

Follow the standards indexed in `.maister/docs/INDEX.md`, loaded lazily per group:

| Category | Applies to | Load in |
|---|---|---|
| `global/` (security, coding-style, commenting, minimal-implementation, conventions, documentation) | always | every group |
| `higson/` (snapshot, integration, rules-authoring) | the snapshot is **untouched** — read `snapshot.md` only to honour P-2 and to avoid touching `src/main/higson/` | G0, G1 |
| `backend/` (api, models, spring) | the single Java change | G0, G1, G10 |
| `frontend/` (build, services, components, css, accessibility, responsive) | the Angular workspace | G3-G9 |
| `testing/` (test-writing, rule-tests) | MockMvc + rule-snapshot booting | G0, G1 |
| `deployment/` (packaging, docker) | WAR/`finalName`/port/`all_in_one` — all unchanged | G3, G12 |

Two standards are **factually wrong for the target** and are corrected in G13, not worked around:
`services.md`'s `response.data` clause (deleted) and `build.md`'s missing base-href rule (added).
`components.md:3` already scopes its rules to legacy code, so replacing them is an update, not a
conflict.

---

## Notes

- **Test-driven**: every group starts by writing 2-8 focused tests and ends by running **only those
  tests**. Never run the whole suite mid-migration — the full replay belongs to G11.
- **Total test budget**: ~62. Do not exceed 8 new tests per group; G11 adds none (it replays).
- **Reuse first** (`spec.md` §9.1): the 21-URL REST contract, the `<Domain>Service` naming and
  one-method-per-endpoint shape, the LESS partials, the brand tokens, the 6 SVGs, the Maven packaging
  chain, the Docker chain (zero changes), the Higson snapshot tooling, and 47 of 48 backend Java files
  are all carried, not rewritten.
- **Commit discipline** (`rollback-plan.md` §1): one task group per commit, each independently
  buildable. A half-finished group is never committed. `git commit` / `git push` only when the user
  asks.
- **Mark progress**: check off steps as they complete — the checkboxes in this file are the resume
  source of truth.
- **Appearance is not a finding.** C-9 redesigned the whole UI. Screenshots are documentation.
  Regression protection rests entirely on the MockMvc contract tests and the Playwright behavioural
  flows.
