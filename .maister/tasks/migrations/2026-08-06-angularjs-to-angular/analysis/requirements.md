# Phase 3 — Migration Requirements

Date: 2026-08-06
Source: AskUserQuestion rounds after Phase 1 (clarifications) and Phase 2 (gap analysis).
Binding for the specification, the implementation plan and the Phase 6 verification.

---

## 1. Confirmed decisions

| ID | Decision | Phase |
|---|---|---|
| C-1 | **Angular Material** replaces the AngularJS-locked UI libraries (datepicker, tooltip, spinner) | 1 |
| C-2 | Angular workspace lives in **`frontend/`** at the repository root | 1 |
| C-3 | **Fix all latent defects** during the migration, under an enumerated defect register | 1 |
| C-4 | Parity evidence = MockMvc contract tests + Playwright E2E goldens + screenshot comparison | 1 |
| C-5 | **Node: bump locally only.** No `.nvmrc`, no `frontend-maven-plugin` | 2 |
| C-6 | **`GlobalExceptionHandler` fix is in scope** (`@ControllerAdvice` → `@RestControllerAdvice`) | 2 |
| C-7 | DR-06 accepted narrowly: failed mutations surface in the **existing** red banner | 2 |
| C-8 | **`CdkOverlay` + `DomSanitizer`** for the HTML coverage description (OD-A) | 3 |
| C-9 | **Full migration to Angular Material** for the whole UI (OD-C + OD-D) | 3 |
| C-10 | **Asset cleanup**: self-host Manrope, Material icons, delete Open Sans + Font Awesome 4 (OD-F) | 3 |
| C-11 | Scope boundaries and rollback confirmed as stated below | 3 |

---

## 2. C-8 — Coverage description tooltip

`coverage.description` arrives from the Higson rules as **HTML** and is rendered today through
`angular-tooltips` (`coverage.html:1-6`, `tooltip-template="{{ctrl.description}}"`).
`MatTooltip` is text-only and would silently drop the markup.

**Requirement**: implement a custom popover on **Angular CDK Overlay**, rendering the description via
`[innerHTML]` with `DomSanitizer`. Full HTML content is preserved — this is a functional-parity
requirement, not a styling preference.

Positioning and trigger behaviour must match today's: opens on hover over the whole coverage row,
positioned to the right (`tooltip-side="right"`), wide variant (`tooltip-size="large"`,
`common-tooltip` = `min-width: 340px`).

---

## 3. C-9 — Full migration to Angular Material  ⚠️ SCOPE EXPANSION

**Decision**: the entire UI moves to Angular Material — `mat-form-field` for the inputs and selects,
CDK Layout for the grid, a Material surface for the error banner. Not just the four controls agreed
in C-1.

**Concern raised before the decision and overruled by the user**: this goes well beyond the four
controls of C-1 and turns step 1 into a technology migration *plus* a full UI redesign.

**Consequences that are now binding:**

- **Visual parity is abandoned entirely.** The custom LESS input chrome (56px height, 8px radius,
  floating notched-outline label, `#4C4CDE` primary) is replaced by Material's form-field appearance.
  The Bootstrap 3 grid (`container` / `row` / `col-md-3` / `col-md-9`) and `.alert.alert-danger` are
  replaced.
- **Bootstrap 3 is dropped from the dependency set.** Nothing in the new frontend depends on it.
- **`less/dropdown/dropdown-menu.less` (72 lines), the `tooltip` block in `less/config.less:32-55`,
  and most of `less/common/common.less` (143 lines) become dead** and must be deleted rather than
  ported.
- **The C-4 screenshot comparison loses its role as a regression gate.** With the whole UI
  redesigned, before/after screenshots cannot distinguish an intended redesign from a regression.
  Screenshots are retained as **documentation of the redesign**, not as pass/fail evidence.
  Regression protection therefore rests entirely on the MockMvc contract tests and the Playwright
  E2E flows — both of which assert *behaviour and contract*, not appearance.
- **The Playwright goldens must be written against semantics, not markup.** Selectors keyed to
  Bootstrap classes or the current DOM structure will not survive. Use accessible roles, labels and
  test ids so the same specification runs against both frontends.
- **Material theming must still carry the brand tokens** from `less/variables.less` — primary
  `#4C4CDE`, radius `8px`, the page gradient — so the application remains recognisable even though
  the controls change.

**Revised parity statement for step 1**: *functional and contractual parity only*. Identical feature
set, identical backend contract, identical recalculation semantics. Appearance is explicitly out of
the parity scope.

---

## 4. C-10 — Fonts and icons

- **Delete** the 90 vendored Open Sans files under `src/main/webapp/fonts/open-sans/` — the font is
  imported in `less/config.less:1` but effectively overridden by Manrope in the body rule
  (`less/config.less:9-16`), so it is dead weight.
- **Delete** Font Awesome 4 (6 files) and its LESS import (`less/config.less:5`). Icons come from
  Angular Material.
- **Self-host Manrope** in `frontend/public/fonts/` instead of the Google Fonts CDN
  (`less/config.less:4`). Removes a runtime network dependency at application start.
- The eight referenced SVGs and `favicon.ico` under `src/main/webapp/imgs/` move to
  `frontend/public/`. The two unreferenced legacy files (`logo.png`, `select-arrow.png`) are dropped.
  Note that with C-9 several of the sidebar/common icons may also become unnecessary — confirm
  reference-by-reference before deleting.

---

## 5. C-11 — Scope boundaries and rollback

### In scope

- Full rewrite of the SPA under a new `frontend/` Angular 22 workspace.
- `pom.xml`: `exec-maven-plugin` `<workingDirectory>`, a **new** `maven-war-plugin`
  `<webResources>` block, retargeted `maven-clean-plugin` filesets.
- `.gitignore` updates for the new build output.
- Emptying `src/main/webapp/` of application sources while **keeping the directory** as the
  `warSourceDirectory`.
- **Deleting the generated `src/main/webapp/styles/styles.min.css` from git** (2946 lines, currently
  committed despite being build output).
- One backend file: `GlobalExceptionHandler` (C-6).
- New test infrastructure: MockMvc contract tests, Playwright E2E, and the JUnit 4/5 resolution in
  `pom.xml` (`junit:junit:4.13.2` → `spring-boot-starter-test`).
- Updating `Readme.md` and the affected `.maister/docs/standards/frontend/*.md` files.

### Out of scope

- **No changes to the Higson snapshot** (`src/main/higson/`) — no rule, function or decision-table
  edits.
- **No changes to `Dockerfile`, `docker-compose.yml` or `push-to-docker.sh`.** The image continues to
  `COPY target/motor-insurance.war` built on the host; the frontend is never built inside the image.
- **No router.** The application stays a single non-routed screen, so `ApplicationConfig.java`'s
  single `addViewController("/")` mapping remains sufficient and unchanged.
- **No new REST endpoints and no changes to the existing 21** beyond what the defect register
  sanctions.
- No `SpringBootServletInitializer`, no external-Tomcat deployability, no CI pipeline.
- No changes to ports, context path configuration, or the `all_in_one` profile.

### Rollback

- Work happens on a **dedicated branch**. Rollback = revert or abandon the branch.
- **No runtime rollback procedure, no dual-run, no feature flag, no downtime window.** This is a demo
  application; there is no production traffic to protect.
- The pre-migration commit remains buildable, which is what makes the golden replay possible: check
  out the pre-migration commit, `mvn clean package -Pall_in_one`, run the captured flows against that
  WAR.

---

## 6. Hard prerequisites before implementation

| # | Prerequisite | Status |
|---|---|---|
| P-1 | Node upgraded to `>= 22.22.3` (LTS), `24.15.0+` or `26+`. Machine is on **23.6.1**, which Angular 22 rejects. Failure surfaces opaquely inside Maven `prepare-package`. | ⛔ **Not done** |
| P-2 | Higson snapshot populated (`mvn higson-cli:pull`) — without it the app returns nothing useful and no E2E golden can be captured. | ⚠️ Verify |
| P-3 | Golden captures (MockMvc contract + Playwright flows) recorded against the **current** build, before any AngularJS code is removed. | ⚠️ Must be the first task group |
| P-4 | `TZ=UTC` pinned for golden capture and replay — `dateOfBirth` carries a server-timezone offset and `MatDatepicker` parses in browser-local time. | ⚠️ Encode in the harness |

---

## 7. Non-negotiable technical constraints carried from Phases 1–2

| Constraint | Source |
|---|---|
| A plain `mvn package` must still produce a WAR with freshly built assets | `standards/frontend/build.md` |
| `npm ci`, never `npm install` | `standards/frontend/build.md` |
| Generated and vendored assets are never committed | `standards/frontend/build.md` |
| `@angular/*` pinned to exact versions; **TypeScript pinned `~6.0.3`** — `typescript@latest` is 7.0.2 and breaks the build | `build.md` + Phase 2 verification |
| `less` must be added explicitly — `@angular/build` ships `sass`, not `less` | Phase 2 verification |
| HTTP confined to `*Service` classes; components never inject `HttpClient` | `standards/frontend/services.md` |
| Request URLs stay relative with no leading slash | `standards/frontend/services.md` |
| **Ship no `<base>` element at all** — context-path-agnostic; the running container uses `/app`, the default image uses `/demo` | Phase 2, OD-B resolved empirically |
| **Never `JSON.stringify` a string PUT body** — `HttpClient` passes strings through verbatim; stringifying would corrupt them | Phase 2, corrects a Phase 1 error |
| Do **not** pin `maven-war-plugin` — Spring Boot manages it at 3.4.0 and its `pluginManagement` carries an `<archive>` block an explicit version would clobber | Phase 2 verification |
| The backend already sorts coverages and discounts by `position` — the `orderBy` no-op fix must add **no** client-side sort | Phase 2 verification |
| `finalName` stays `motor-insurance`; port stays 48080 | `standards/deployment/packaging.md`, `docker.md` |
| Preserve the recalculation asymmetry: only a vehicle **model** change triggers a quote reload | `standards/frontend/components.md`, recorded non-deviation |
| Preserve session semantics — `@SessionScope` quote, `JSESSIONID`, no CORS, same-origin dev proxy only | Phase 1 |
