# Phase 1 — Clarifications

Date: 2026-08-06
Source: AskUserQuestion after current-state analysis.

---

## C-1 — UI library strategy → **Angular Material**

**Question**: How to handle the AngularJS-locked UI libraries (angular-ui-bootstrap datepicker +
btn-radio, angular-tooltips with HTML content, angular-spinner/spin.js), none of which has a 1:1
successor?

**Decision**: Adopt **Angular Material** — `MatDatepicker`, `MatTooltip`, `MatProgressSpinner`.

**Consequences (accepted by the user):**
- This is a **deliberate visual change**. The datepicker popup, the gender radio group, the coverage
  tooltip and the global spinner will not look identical to today's `uib-*` / `720kb.tooltips` /
  `spin.js` rendering.
- `less/dropdown/dropdown-menu.less` (72 lines re-skinning the Bootstrap 3 `.dropdown-menu` for the
  uib datepicker) and the `tooltip` / `tip` / `tip-arrow` block in `less/config.less:32-55` become
  obsolete.
- `MatTooltip` renders **text only**. `coverage.description` is HTML coming from the Higson rules.
  Either a Material-styled `CdkOverlay` popover is used for that one case, or the description is
  rendered as plain text — this is an open item for the specification phase (see OD-A below).
- Angular Material brings its own theming layer, which must be reconciled with the existing LESS
  design tokens (`less/variables.less`: primary `#4C4CDE`, radius `8px`).
- Bootstrap 3 CSS is still needed for the `.container` / `.row` / `.col-md-3` / `.col-md-9` grid and
  the `.alert.alert-danger` banner unless those are also re-implemented.

**Effect on the parity definition**: strict *pixel* parity is off the table for the four affected
controls. See "Revised definition of 1:1" below.

---

## C-2 — Angular workspace layout → **`frontend/` sibling directory**

**Question**: Where should the Angular workspace live? Today `package.json`, `gulpfile.js` and
`less/` sit at the repository root.

**Decision**: A **`frontend/`** directory at the repository root.

**Consequences:**
- `frontend/angular.json`, `frontend/package.json`, `frontend/node_modules`, `frontend/src` — the
  Angular CLI runs entirely on its defaults (no `sourceRoot` override, `ng generate` / `ng update`
  work unmodified).
- `less/`, `package.json`, `package-lock.json` and `gulpfile.js` move out of the repository root.
- `pom.xml` requires two changes:
  - `<workingDirectory>${project.basedir}/frontend</workingDirectory>` added to the existing
    `exec-maven-plugin` configuration (`pom.xml:99-105`) — it currently defaults to
    `${project.basedir}`.
  - A **new** `maven-war-plugin` block with `<webResources>` pointing at the Angular build output.
    There is no `maven-war-plugin` configuration in `pom.xml` today; it is fully inherited from
    `spring-boot-starter-parent`.
- `maven-clean-plugin` filesets must be retargeted: `node_modules` → `frontend/node_modules`,
  `src/main/webapp/vendors` → the new build output / `.angular/cache`.
- `.gitignore`: add `frontend/node_modules/`, `frontend/dist/`, `frontend/.angular/`; remove the
  now-meaningless `src/main/webapp/vendors/` entry.
- `Dockerfile`, `docker-compose.yml` and `push-to-docker.sh` need **no change** — they only ever
  touch `target/motor-insurance.war`.
- Single-repository requirement is satisfied: still one Maven module, still one git repository, still
  one `motor-insurance.war`.

---

## C-3 — Latent defects → **fix all of them during the migration**

**Question**: The analysis found ~19 defects/quirks (make-dropdown `TypeError` on the blank option,
HTTP 500 on a cleared numeric field, a PUT per keystroke, coverages/discounts unsorted because of an
unquoted `orderBy` predicate, unformatted prices in the summary, silently swallowed mutation errors,
duplicate `GET driver` on load, dead code). What happens to them in step 1?

**Decision**: **Fix everything along the way** — migration plus sanitisation.

**Concern raised and overruled by the user**: mixing a technology migration with functional changes
makes an unambiguous parity verdict impossible, because every behavioural difference has two possible
causes. The user confirmed this choice; work proceeds under it, with the mitigation below.

**Mitigation applied to the plan** (so the decision stays reviewable rather than untraceable):
- Every fix must be an **explicitly enumerated, individually documented change** in the specification
  — a numbered defect register with "current behaviour → new behaviour → rationale".
- Anything *not* on that register is a **regression**, not a fix. The parity checklist from
  `current-state-analysis.md` remains the acceptance baseline; the defect register is the list of
  sanctioned deviations from it.
- Where a fix changes the *request volume* against the backend (e.g. debouncing the per-keystroke
  PUTs, deduplicating the double `GET driver`), the change must be called out separately — it is
  visible in the spinner behaviour and in the number of Higson rule evaluations.
- Where a fix changes *rendering order* (the unquoted `orderBy: position` no-op), confirm against the
  running backend whether the server already returns coverages/discounts sorted by `position` before
  introducing a client-side sort.

---

## C-4 — Parity evidence → **API contract tests + Playwright E2E + screenshot comparison**

**Question**: There are no tests and no CI at all today. On what basis do we prove 1:1 parity?

**Decision**: All three of:
1. **API contract tests (MockMvc / `@SpringBootTest`)** pinning all 21 request/response shapes,
   including the raw-string PUT body quirk. **Recorded against the current build first**, as golden.
2. **Playwright E2E**, golden flows captured against the AngularJS app, replayed against the new one.
   Requires a populated Higson snapshot (`mvn higson-cli:pull`) — a fresh clone has no rules.
3. **Screenshot comparison** of the key views, before vs after.

**Consequences:**
- This creates the project's **first test infrastructure**. `pom.xml` currently declares
  `junit:junit:4.13.2` with no `spring-boot-starter-test`, and the only (untracked) test uses the
  JUnit 5 API — that conflict must be resolved as part of this work.
- Golden captures (1) and (2) must be produced **before** the AngularJS code is removed. This makes
  "capture the baseline" a mandatory first task group, ahead of any Angular scaffolding.
- Because of C-1 and C-3, screenshot comparison **cannot be a pass/fail gate**. It is downgraded to a
  reviewed diff: every visual difference must be traceable to either the Angular Material adoption
  (C-1) or an entry in the defect register (C-3). Unexplained differences are regressions.
- The Playwright assertions likewise need per-scenario expectations for the flows touched by the
  defect register (request counts change once debouncing is introduced).

---

## Revised definition of "1:1" for step 1

C-1 and C-3 together mean the original phrasing — *"zachowanie obecnej funkcjonalności 1 do 1"* —
must be read as **functional parity of user-visible capability and of the backend contract**, not as
bug-for-bug or pixel-for-pixel reproduction.

Binding for step 1:

| Dimension | Requirement |
|---|---|
| Feature set | **Identical.** No new screens, no new fields, no router, no new endpoints. The parity checklist in `current-state-analysis.md` is the acceptance list. |
| Backend contract | **Byte-identical**, with one sanctioned exception class: request bodies that are demonstrably broken today. All 21 endpoints, same URLs, same relative-path convention, same session semantics. |
| Recalculation semantics | **Preserved exactly**, including the asymmetry that only a vehicle *model* change triggers a quote reload — unless listed in the defect register. |
| Defects | Fixed, but **only those on the enumerated register**. |
| Visual appearance | Preserved **except** for the four controls replaced by Angular Material (datepicker, gender radio group, coverage tooltip, global spinner) and any change traceable to the defect register. |
| Packaging | **Unchanged**: one repository, one Maven module, `target/motor-insurance.war`, frontend build wired into `prepare-package`, `all_in_one` profile, port 48080. |

---

## Open items carried into Phase 2/3

| ID | Item | Why it is still open |
|---|---|---|
| OD-A | HTML content of `coverage.description` in a Material tooltip | `MatTooltip` is text-only. Needs a `CdkOverlay` popover, `[innerHTML]` + `DomSanitizer`, or a decision to degrade to plain text. |
| OD-B | `<base href>` vs `server.servlet.context-path` | Packaged `higson-demo-app.properties` sets `/demo`; `docker/app.properties` omits it. Must be resolved **empirically** against a running container before the build is configured. |
| OD-C | Bootstrap 3 CSS retention | Still needed for the grid and the error alert after Material adoption. Keep BS3 alongside Material, or re-implement the grid in CSS Grid/Flexbox? |
| OD-D | Angular Material theme vs existing LESS tokens | `#4C4CDE` primary, `8px` radius, custom notched-outline input chrome. Material theming must be reconciled or the inputs re-styled. |
| OD-E | Node toolchain | **Blocker.** Local Node is 23.6.1; Angular 22 requires `^22.22.3 \|\| ^24.15.0 \|\| >=26.0.0`. Needs `.nvmrc` + either a documented toolchain baseline or `frontend-maven-plugin` pinning Node for the Maven build. |
| OD-F | Fonts and icons | Font Awesome 4 + 90 vendored Open Sans files + Manrope from the Google Fonts CDN. Material brings its own icon font. Asset-pipeline decision needed. |
