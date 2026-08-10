# Migration Specification — AngularJS 1.8.3 → Angular 22

**Task**: `.maister/tasks/migrations/2026-08-06-angularjs-to-angular`
**Type**: migration / `migration_type: code` — technology replacement, frontend tier
**Strategy**: big-bang cutover on a dedicated branch, gated by golden captures recorded against the
current build. Dual-run rejected; ngUpgrade ruled out.
**Risk**: medium-high
**Date**: 2026-08-06

**Binding inputs** (do not re-derive; this document builds on them):

| Artefact | Role here |
|---|---|
| `analysis/current-state-analysis.md` | Behavioural parity checklist (§"BEHAVIOURAL PARITY CHECKLIST"), 21-row REST table, 27-row idiom table. **The acceptance baseline.** |
| `analysis/target-state-plan.md` | Version pins (§1.1), 65 gaps (§2), defect register DR-01…DR-27 (§3), Phase-1 corrections PC-1…PC-5 (§5) |
| `analysis/clarifications.md` | C-1…C-4 |
| `analysis/requirements.md` | **Binding**: C-1…C-11, scope boundaries, P-1…P-4, the non-negotiable constraints table |

---

## 1. Goal

Replace the AngularJS 1.8.3 SPA under `src/main/webapp/` with an Angular 22 application in a new
`frontend/` workspace, preserving the single-module Maven WAR (`target/motor-insurance.war`) and the
21-endpoint REST contract exactly, while applying an enumerated register of 27 sanctioned defect fixes
and moving the entire UI to Angular Material.

---

## 2. Scope statement — the parity definition (binding)

Per `requirements.md` §3, C-9 revises the parity definition. **This section supersedes every earlier
phrasing of "1:1", including `clarifications.md`'s "Revised definition of 1:1" table.**

### Parity is FUNCTIONAL and CONTRACTUAL ONLY

| Dimension | Requirement | Verified by |
|---|---|---|
| **Feature set** | **Identical.** No new screens, no new fields, no router, no new endpoints. The parity checklist in `current-state-analysis.md` is the acceptance list. | Playwright flows |
| **Backend contract** | **Byte-identical** on all 21 endpoints — same URLs, same relative-path convention, same body bytes, same session semantics. One sanctioned change: C-6 (§6.4). | MockMvc contract tests |
| **Recalculation semantics** | **Preserved exactly**, including the vehicle emit asymmetry (DR-13), except where the defect register sanctions a change. | Playwright request-count assertions |
| **Defects** | Fixed — but **only** those on the DR-01…DR-27 register (§7). | Register walk-through, §7 |
| **Appearance** | ⛔ **EXPLICITLY OUT OF PARITY SCOPE.** | — see below |
| **Packaging** | **Unchanged**: one repository, one Maven module, `finalName=motor-insurance`, frontend build wired into `prepare-package`, `all_in_one` profile, port 48080. | §5 acceptance checks |

### Appearance is out of scope — state this unambiguously

C-9 moves the **whole** UI to Angular Material, not just the four controls of C-1. Consequently:

- **Visual parity is abandoned entirely.** The custom LESS input chrome (56 px height, 8 px radius,
  floating notched-outline label — `less/common/common.less`), the Bootstrap 3 grid
  (`.container` / `.row` / `.col-md-3` / `.col-md-9`, `main.html:1-22`) and `.alert.alert-danger`
  (`variants.html:2`) are all replaced.
- **Bootstrap 3 is dropped from the dependency set.** Nothing in the new frontend depends on it.
  This overrides the target-state plan's OD-C recommendation ("C1 — keep Bootstrap 3 CSS"), which was
  written before C-9.
- **Angular Material is used for every control**, not just four. This overrides the target-state plan's
  OD-D recommendation (D1); the accepted option is **D2**.
- **Screenshot comparison is NOT a regression gate.** With the whole UI redesigned, a before/after
  screenshot cannot distinguish an intended redesign from a regression. Screenshots are retained as
  **documentation of the redesign** only (§8.5).
- **Downstream phases (implementation-planner, executor, Phase 6 verification) MUST NOT verify
  appearance.** A visual difference is never, on its own, a finding. Regression protection rests
  entirely on the MockMvc contract tests and the Playwright behavioural flows.
- **Playwright selectors must be semantic** — accessible roles, accessible names, and `data-testid` —
  never Bootstrap classes, never DOM structure. The same spec file must run unmodified against both
  frontends. This is what makes the golden replay possible at all.

**Material theming must still carry the brand tokens** from `less/variables.less:1-14` — primary
`#4C4CDE`, corner radius `8px`, and the page gradient at `less/config.less:12` — so the application
remains recognisable. This is a *recognisability* requirement, not a parity requirement.

---

## 3. Prerequisites — BLOCKING

From `requirements.md` §6. **No implementation task group may start until P-1 is cleared, and no
deletion may occur until P-3 is complete.**

| # | Prerequisite | Blocks | Status |
|---|---|---|---|
| **P-1** | Node upgraded to `>= 22.22.3` / `24.15.0+` / `26+`. Machine is on **23.6.1**, which Angular 22 rejects (`engines.node = "^22.22.3 \|\| ^24.15.0 \|\| >=26.0.0"`). Failure surfaces **opaquely inside Maven `prepare-package`**. | Every `ng` command, including `ng new`. Task group ordering: **stage 0**. | ⛔ Not done |
| **P-2** | Higson snapshot populated (`mvn higson-cli:pull`). `src/main/higson/` is not in git (`standards/higson/snapshot.md`); a fresh clone has no rules and `GET quote` returns nothing useful. | Both MockMvc contract tests (they boot the engine) and every Playwright flow. | ⚠️ Verify |
| **P-3** | **Golden captures recorded against the CURRENT build**, before any AngularJS source is removed or modified. A `git revert` restores *code*; it cannot recreate a *runtime* capture. | All of §7 and §8. **Must be the first task group.** | ⚠️ Not done |
| **P-4** | `TZ=UTC` pinned for golden capture and replay. `dateOfBirth` carries a server-timezone offset (`+00:00` in the container, `+02:00` on a Europe/Warsaw dev box) and `MatDatepicker`'s adapter parses in browser-local time — the displayed day can be off by one across machines. | Reproducibility of every date-touching golden. | ⚠️ Encode in the harness (§8.6) |

---

## 4. Target architecture

### 4.1 Stack (verified live against the npm registry, `target-state-plan.md` §1.1)

| Item | Pin | Note |
|---|---|---|
| `@angular/*` (core, common, compiler, compiler-cli, forms, platform-browser) | **22.1.0** exact | `build.md` mandates exact pins for framework-critical packages |
| `@angular/cli`, `@angular/build` | **22.1.3** exact | esbuild `application` builder |
| `@angular/material`, `@angular/cdk` | **22.1.1** exact | CDK peer is exact |
| `typescript` | **`~6.0.3`** | ⚠️ peer is `>=6.0 <6.1`. **`typescript@latest` is 7.0.2 and breaks the build.** |
| `rxjs` | `~7.8.2` | |
| `less` | `^4.2.0` **devDependency** | ⚠️ `@angular/build` bundles `sass`, **not** `less`. Omitting it is a build failure. |
| `zone.js` | **omitted** | zoneless; `provideZonelessChangeDetection()` |
| `vitest` | `^4.0.8` | unit runner (v22 default, not Karma) |
| `@playwright/test` | `1.62.1` | E2E; lives in `e2e/`, not in `frontend/` (§8.3) |
| Node | `24.19.0` locally | **C-5: bump locally only. No `.nvmrc`, no `frontend-maven-plugin`.** |

### 4.2 Repository tree after migration

```
business-rules-motor-insurance/
├── pom.xml                              # 4 edits, §5.1
├── .gitignore                           # §5.4
├── e2e/                                 # NEW — standalone Playwright project, §8.3
│   ├── package.json                     #   frontend-agnostic by design
│   ├── playwright.config.ts
│   ├── goldens/                         #   captured against the AngularJS build (P-3)
│   └── specs/*.spec.ts
├── frontend/                            # NEW — the Angular workspace (C-2)
├── src/main/webapp/                      # EMPTIED, directory KEPT as warSourceDirectory
│   └── .gitkeep
├── src/main/java/…                       # one file changes: GlobalExceptionHandler (C-6)
├── src/test/java/…                       # MockMvc contract tests, §8.2
└── (deleted) package.json, package-lock.json, gulpfile.js, less/
```

### 4.3 `frontend/` workspace — file level

Angular CLI defaults throughout: no `sourceRoot` override, so `ng generate` / `ng update` and the
Angular MCP `list_projects` work unmodified.

```
frontend/
├── .npmrc                               # engine-strict=true          (§5.5)
├── .gitignore                           # node_modules/ dist/ .angular/
├── angular.json                         # §5.3
├── package.json                         # scripts + engines           (§5.2)
├── package-lock.json                    # COMMITTED; regenerated wholesale
├── tsconfig.json / tsconfig.app.json / tsconfig.spec.json
├── proxy.conf.json                      # dev only                    (§4.9)
├── public/                              # copied verbatim to the bundle root
│   ├── favicon.ico
│   ├── imgs/logo-insurance-app.svg
│   ├── imgs/{person,vehicle,doc,address}.svg
│   ├── imgs/arrow-tooltip.svg           # kept — used by the CDK popover (§4.7)
│   └── fonts/manrope/Manrope[wght].woff2   # self-hosted variable font (C-10)
└── src/
    ├── index.html                       # ⛔ NO <base> element        (§5.6)
    ├── main.ts                          # bootstrapApplication(App, appConfig)
    ├── theme.scss                       # ⚠️ the ONLY Sass file — Material theming API is
    │                                    #    Sass-only. Seeds #4C4CDE / 8px. §4.8
    ├── styles.less                      # global LESS entry point
    ├── styles/
    │   ├── _variables.less              # from less/variables.less
    │   ├── _typography.less             # @font-face Manrope; body gradient
    │   ├── _layout.less                 # CSS Grid — replaces the BS3 grid (C-9)
    │   ├── main.less  sidebar.less
    │   └── variants/{variants,coverage,discounts}.less
    └── app/
        ├── app.ts / app.html / app.less           # selector "app-root"   (DR-20)
        ├── app.config.ts
        ├── core/
        │   ├── quote-store.ts                     # replaces $emit/$broadcast (G-D1)
        │   ├── pending-requests.ts                # signal holder for the spinner
        │   ├── pending-requests.interceptor.ts    # replaces ngLoadingSpinner (G-D2)
        │   ├── mutation-error.interceptor.ts      # DR-06 / C-7
        │   └── iso-date.adapter.ts                # DateAdapter + MAT_DATE_FORMATS (G-C7)
        ├── models/
        │   ├── driver.ts        # DriverDto, AddressDto
        │   ├── vehicle.ts       # VehicleDto, UpdateMakeDto
        │   ├── quote.ts         # QuoteDto, OptionDto, CoverageDto, DiscountDto
        │   ├── dictionary-entry.ts
        │   └── error.ts         # ErrorDto  { message: string }        (C-6)
        ├── services/                              # HTTP lives ONLY here (services.md)
        │   ├── driver-service.ts        class DriverService
        │   ├── vehicle-service.ts       class VehicleService
        │   ├── dictionary-service.ts    class DictionaryService
        │   └── variant-service.ts       class VariantService
        ├── shared/
        │   └── html-popover/
        │       ├── html-popover.ts            # attribute directive, CDK Overlay
        │       └── html-popover-panel.ts      # panel component, [innerHTML] + DomSanitizer
        └── components/
            ├── sidebar/sidebar.{ts,html,less}
            ├── driver/driver.{ts,html,less}
            ├── driver-history/driver-history.{ts,html,less}
            ├── vehicle/vehicle.{ts,html,less}
            ├── spinner/spinner.{ts,html,less}
            └── variants/
                ├── variants.{ts,html,less}
                ├── coverage/coverage.{ts,html,less}
                └── discount/discount.{ts,html,less}
```

**File naming** follows the Angular v20+ CLI default (no `.component` / `.service` suffix on
*filenames*), which preserves `components.md`'s "Component Files Co-Located and Same-Named" rule and
the `variants/coverage`, `variants/discount` nesting. **Class names keep the `<Domain>Service`
suffix** required by `services.md`.

### 4.4 Angular idiom rules — apply to every component

Per `mcp__angular__get_best_practices`, recorded in `target-state-plan.md` §1.2:

- `changeDetection: ChangeDetectionStrategy.OnPush` on **every** component.
- **Never** write `standalone: true` — it is the default since v20 and setting it is an error of style.
- Inputs via `input<T>()` / `input.required<T>()`; outputs via `output<T>()`. **No `@Input` / `@Output`
  decorators.**
- Dependencies via `inject()`, not constructor parameters.
- Local state via `signal()`; derived state via `computed()`. **Never** `BehaviorSubject`.
- Control flow via `@if` / `@for` / `@switch`. `*ngIf`, `*ngFor`, `ngClass`, `ngStyle` are forbidden.
  **`@for` requires `track`** — none of the three current `ng-repeat`s has one:
  `track variant.code` (`variants.html:5`), `track coverage.code` (`variants.html:8`),
  `track discount.code` (`variants.html:21`).
- Host bindings via the `host` object, not `@HostBinding` / `@HostListener`.
- `providedIn: 'root'` for all services and the store.
- Zoneless: `provideZonelessChangeDetection()` in `app.config.ts`.

### 4.5 `app.config.ts`

```
providers: [
  provideZonelessChangeDetection(),
  provideBrowserGlobalErrorListeners(),
  provideHttpClient(withInterceptors([pendingRequestsInterceptor, mutationErrorInterceptor])),
  provideAnimationsAsync(),                        // required by Angular Material
  { provide: DateAdapter, useClass: IsoDateAdapter },
  { provide: MAT_DATE_FORMATS, useValue: MOTOR_DATE_FORMATS },
]
```

⚠️ **Do NOT set `LOCALE_ID` and do NOT call `registerLocaleData(localePl)`.** Angular's default
`LOCALE_ID` is `en-US` with built-in locale data, which is exactly what today's `| number: 2` produces
(`100000` → `100,000.00`) despite `<html lang="pl">`. This is DR-11: a no-op that prevents an
accidental regression. `index.html` keeps `lang="pl"` — changing it is not on the register (§11).

### 4.6 `QuoteStore` — replaces the `$emit`/`$broadcast` event bus

This is the one genuine architectural decision in the migration (G-D1, `target-state-plan.md` §2(d)),
not a mechanical port. The current bus is 2 events / 11 emit sites / 2 listeners:
`$scope.$emit('recalculate')` (`driver.js:97`, `driverHistory.js:54`, `vehicle.js:76`) →
`main.js:21-23` relay → `$scope.$on('reloadQuote')` (`variants.js:24-26`).

`core/quote-store.ts`, `@Injectable({ providedIn: 'root' })`:

| Member | Shape | Replaces / satisfies |
|---|---|---|
| `private reloadTrigger = signal(0)` | write-only counter | the `recalculate` event |
| `recalculate(): void` | `reloadTrigger.update(n => n + 1)` | `$scope.$emit('recalculate')` at all 11 sites |
| `quote = resource({ params: () => this.reloadTrigger(), loader: () => firstValueFrom(this.variantService.getQuote()) })` | Angular `resource` | `variants.js:23-39`; **DR-07 by construction** — on error `value()` is `undefined`, so stale cards cannot persist under the banner |
| `driver = resource({ loader: () => firstValueFrom(this.driverService.getDriver()) })` | loaded once | **DR-09** — deduplicates the two startup `GET driver` calls (`driver.js:53-59` + `driverHistory.js:30-36`) |
| `mutationError = signal<string \| null>(null)` | written by the mutation-error interceptor | **DR-06 / C-7** |

Rules:

- The store injects `VariantService` and `DriverService` — **never `HttpClient`**. `services.md`'s
  "HTTP only in `*Service` classes" survives verbatim.
- **`app.ts` contains no relay logic at all.** `main.js:21-23` is deleted, not ported (idiom #2).
  `app.html` is pure layout.
- **DR-13 / G-D3 — the emit asymmetry is encoded here, explicitly.** Only
  `VehicleComponent.updateModel()` calls `store.recalculate()`. Production year, make and type do
  **not**. Add a code comment naming DR-13 so a future reader cannot mistake it for an oversight.
  A "tidy" reactive rewrite that recalculates on every change is a **failed migration**, not an
  improvement (invariant, `target-state-plan.md` §8).
- No client-side quote model is introduced (G-D4). The server keeps all state in `@SessionScope`
  (`service/QuoteService.java:10-12`); the SPA stays a thin remote-controlled view.

### 4.7 Component inventory — target specification

`main` **must be renamed**: `main` is a real HTML5 element and an invalid custom-element selector
(`index.html:14`, `main.js:8`; DR-20). Selector becomes `app-root`.

| Component | Selector | Inputs | Material / CDK used | Endpoints | Register entries applied |
|---|---|---|---|---|---|
| `app` | `app-root` | — | — | — | DR-20 |
| `sidebar` | `app-sidebar` | — | — | — | DR-16 (do **not** port `sidebarHidden`/`toggleSidebar`, `sidebar.js:21-27`) |
| `driver` | `app-driver` | — | `MatFormField`, `MatInput`, `MatDatepicker` + toggle, `MatButtonToggleGroup` | 1 GET + 7 PUT | DR-04, DR-08, DR-17, DR-19, DR-27 |
| `driver-history` | `app-driver-history` | — | `MatFormField`, `MatInput[type=number]` | 3 PUT | DR-02, DR-03, DR-09 |
| `vehicle` | `app-vehicle` | — | `MatFormField`, `MatSelect`, `MatOption` | 1 GET + 4 PUT + 4 dictionary GETs | DR-01, DR-12, DR-13 |
| `variants` | `app-variants` | — | `MatCard` (error surface) | reads `store.quote` | DR-05, DR-07, DR-10, DR-14 |
| `coverage` | `app-coverage` | 6 × `input()`: `code`, `name`, `description`, `limitLeft`, `limitRight`, `premium` | `HtmlPopover` directive (CDK Overlay) | — | DR-10, DR-11 |
| `discount` | `app-discount` | 2 × `input()`: `name`, `value` | — | — | DR-10, DR-18 (drop `priceBeforeDiscount`) |
| `spinner` | `app-spinner` | — | `MatProgressSpinner` | — | — |

Per-component detail where it is not mechanical:

**`driver`** — 7 fields (`driver.html:1-90`).
- Text fields (firstName, lastName, zipCode, city, street) commit on **`(blur)`**, reproducing
  `ng-model-options="{updateOn: 'blur'}"` (`driver.html:7,17,68,78,88`).
- Date of birth: `<input matInput [matDatepicker]>` + `<mat-datepicker-toggle matSuffix>` replacing the
  `.glyphicon-calendar` trigger (`driver.html:34`). Commit on **`(closed)` and `(blur)` only** — never
  on keystroke (**DR-04**). The component holds a `Date`; the wire model is an ISO-8601 string (§6.3).
- Gender: `<mat-button-toggle-group [value]="gender()" (change)="onGender($event)">` with values
  `'M'` / `'F'`. `MatButtonToggleGroup.change` does not fire when the already-selected toggle is
  re-clicked, which satisfies **DR-08** (no redundant PUT) by construction — and the handler sets the
  value **before** issuing the PUT, making explicit what AngularJS did implicitly through alphabetical
  directive ordering (`driver.html:38-53`).
- Do **not** port `$scope.selectedDate` (`driver.js:33`, DR-17) or `close-text="Close"`
  (`driver.html:31`, DR-19). The `is-open="popup.opened"` `$scope` binding becomes a single component
  signal (DR-27).
- The "Address" divider stays inside the driver template (`driver.html:55-60`) — recorded
  non-deviation.

**`driver-history`** — 3 numeric fields (`driverHistory.html:8,18,27`).
- Commit on **`(blur)`**, not on keystroke (**DR-03** — the largest request-volume change in the
  migration).
- **Guard before sending** (**DR-02**): if the value is `null`, `NaN`, or violates `min`
  (`min=0`, `min=0`, `min=16`), do not issue the PUT and leave the last valid server value in place.
  Sending it today produces a verified HTTP 500.
- Reads the driver from `store.driver` — no second `GET driver` (**DR-09**).

**`vehicle`** — 4-level cascade (`vehicle.html:6,17,28,39`; `vehicle.js:36-107`).
- `<mat-select>` with `<mat-option [value]="null">` for the placeholder and
  `@for (e of dict(); track e.code)` `<mat-option [value]="Number(e.code)">`. `DictionaryEntryDto.code`
  is a **String** on the wire; coerce with an explicit `Number()`, replacing the implicit unary `+`
  in `ng-options` (idiom #10).
- **DR-01**: guard the make lookup. On the blank option, clear `make`, `typeId`, `modelId` and their
  dictionaries and issue **no** `PUT vehicle/make`. Today `vehicle.js:61-63` (`})[0].name;`) throws an
  uncaught `TypeError`.
- **DR-12**: `clearDependantsVehicleFields` (`vehicle.js:102-107`) must clear `typeDictionary` and
  `modelDictionary` too, not just the field values.
- The falsy-`0` guards (`if (vehicle.productionYear)` etc.) work only because `VehicleDto` uses
  primitive `int` so unset arrives as `0`. Preserve that logic; type the model `number`, not
  `number | null`.
- **DR-13**: only `updateModel()` calls `store.recalculate()`.

**`variants`** — the only error branch in the application (`variants.js:35-38`).
- Renders `@for (v of store.quote.value()?.options ?? []; track v.code)` in **array order**. The
  backend already sorts (`target-state-plan.md` §5.5: options `order` 1/2/3, coverage positions
  ascending in all three options). **DR-05: introduce no client-side sort.**
- Error surface: a `<mat-card role="alert" data-testid="error-banner">` replacing
  `.alert.alert-danger` (`variants.html:2`). Text = `ErrorDto.message` from the C-6 backend fix
  (§6.4), falling back to a generic message if `message` is absent.
- **DR-07**: when `store.quote` is in error, no variant cards render.
- **DR-15**: drop `cursor: pointer` and the hover shadow/background
  (`less/variants/variants.less:11-27`) — a false affordance; the cards have no click handler.

**`coverage`** — presentational, `input()` signals only (`coverage.js:12-19`).
- **DR-10**: `limitLeft`, `limitRight` and `premium` all use `DecimalPipe` `'1.2-2'`, and so do
  `variant.premiumBeforeDiscounts` and `variant.premium` in `variants` and `value` in `discount`.
  All four money fields formatted consistently. The literal `-` prefix on discounts stays; **no
  currency symbol is added anywhere** — that would be a new feature.
- `@if (limitRight())` replaces `ng-if` (`coverage.html:13`).

**`discount`** — presentational (`discount.js:12-16`). Drop the `::` one-time-binding syntax
(default Angular behaviour) and the `priceBeforeDiscount` input entirely (**DR-18** — the field does
not exist on `DiscountDto`; `variants.html:24`).

**`spinner`** — `<mat-progress-spinner mode="indeterminate" [diameter]="60" [strokeWidth]="8">`,
mapping `us-spinner="{radius:30, width:8, length:16}"` (`main.html:2`). Rendered by `app.html` under
`@if (pendingRequests.count() > 0)`. Positioning carried over from `less/main/main.less:6-12`
(`top:50%; left:56%; transform:translate(-50%,-50%); z-index:9999`).

### 4.8 The HTML coverage-description popover (C-8)

`coverage.description` arrives from the Higson rules as **HTML** and is rendered today through
`angular-tooltips` (`coverage.html:1-6`, `tooltip-template="{{ctrl.description}}"`). `MatTooltip` is
**text-only** and would silently drop the markup — a *functional* regression, not a visual one, and
therefore outside the appearance exemption of §2.

`app/shared/html-popover/`:

| File | Contents |
|---|---|
| `html-popover.ts` | Attribute directive `[appHtmlPopover]`. `inject(Overlay)`, `inject(ViewContainerRef)`. Opens on `mouseenter` of the host, closes on `mouseleave`. `host: { '(mouseenter)': …, '(mouseleave)': … }`. |
| `html-popover-panel.ts` | Panel component rendering `<div [innerHTML]="safeHtml()">`. `safeHtml = computed(() => this.sanitizer.bypassSecurityTrustHtml(this.html()))`. |

Behaviour that must match today's:

| Today (`coverage.html:1-6`, `less/config.less:22-42`) | Target |
|---|---|
| trigger = hover over the **whole coverage row** | directive applied to the `.coverage` host element |
| `tooltip-side="right"` | `FlexibleConnectedPositionStrategy`, `originX: 'end'`, `overlayX: 'start'`, `originY/overlayY: 'center'` |
| `tooltip-size="large"` + `.common-tooltip` `min-width: 340px` | panel `min-width: 340px` |
| white body, 1 px `rgba(76,76,222,1)` border, 15 px padding, 4 px radius, left-aligned | reproduced in the panel's LESS |
| border-arrow replaced by `imgs/arrow-tooltip.svg` (19×19) | same asset, re-anchored (`public/imgs/arrow-tooltip.svg`) |
| `withScrollStrategy` | `Overlay.scrollStrategies.reposition()` |

**Sanitization**: `coverage.description` originates from the Higson rules engine
(`adapter/CoverageAdapter.java` → `DESCRIPTION`), not from user input. Treat it as **trusted** and use
`bypassSecurityTrustHtml`. Record this in a code comment — it is the only `bypassSecurity*` call in the
application and a reviewer must be able to see why.

⚠️ `less/config.less:22-42` styles `angular-tooltips`' internal `tooltip` / `tip` / `tip-arrow` DOM.
It must be **rewritten** for the CDK panel, not ported (G-E5).

### 4.9 Styling — where LESS lives, and the one Sass exception

**LESS is retained as the styling language** (`css.md` "Consistent Methodology"). Concretely:

- `angular.json` → `"styles": ["src/theme.scss", "src/styles.less"]`, `"inlineStyleLanguage": "less"`,
  `"stylePreprocessorOptions": { "includePaths": ["src/styles"] }`.
- Component styles are `.less` files referenced via `styleUrl`.
- ⚠️ **`src/theme.scss` is the single, deliberate Sass file.** Angular Material's theming API
  (`@use '@angular/material' as mat; @include mat.theme(...)`) is **Sass-only** — there is no LESS
  equivalent, and this is a constraint neither analysis phase recorded. `@angular/build` already
  bundles `sass`, so it costs no new dependency. The alternative — shipping a prebuilt Material theme
  and hand-overriding `--mat-sys-*` custom properties from LESS — was rejected as more fragile and
  more code.
- `theme.scss` is the **single seed** for the brand tokens: `$primary` derived from `#4C4CDE`,
  `--mat-sys-corner-*` set to `8px`. `_variables.less` keeps only the values Material has no token for
  (`@color-border-block: #ECECEC`, `@color-gray-light: #f2f2f2`, the tooltip border colour). Where a
  Material token exists, LESS references `var(--mat-sys-primary)` rather than re-declaring the hex.
  The theme file is listed first so LESS overrides win the cascade.

**LESS files: carried, rewritten, deleted**

| Source | Fate |
|---|---|
| `less/variables.less` (14) | → `src/styles/_variables.less`, trimmed per above |
| `less/config.less:1,5` (`@import '../node_modules/…'`) | **deleted** — Open Sans and Font Awesome both go (C-10) |
| `less/config.less:4` (Manrope from the Google Fonts CDN) | → `@font-face` over the self-hosted variable font in `_typography.less` |
| `less/config.less:8-14` (body gradient, Manrope 13px) | → `_typography.less`, kept verbatim |
| `less/config.less:16-20` (`.container` 1440px @≥1200px) | → `_layout.less` as a CSS Grid container |
| `less/config.less:22-42` (angular-tooltips DOM) | **rewritten** for the CDK panel (G-E5) |
| `less/common/common.less` (143) | **mostly deleted** — `.common-input-*`, `.common-btn-group` are superseded by `mat-form-field` / `mat-button-toggle` (C-9). `.common-tooltip`'s `min-width: 340px` survives into the popover panel. `.common-checkbox` and `.common-subsection-heading` are dead (DR-25). |
| `less/main/main.less` (69) | → `styles/main.less`; `.spinner` positioning (6-12) and the webkit scrollbars (14-32) kept; logo at `:51` re-anchored |
| `less/sidebar/sidebar.less` (100) | → `styles/sidebar.less`; delete the `transition: 0.5s` / `opacity: 1` collapse leftovers at `:33-36` (DR-16); 4 icon backgrounds re-anchored |
| `less/variants/{variants,coverage,discounts}.less` | → `styles/variants/*`; drop `.coverage__divider` (DR-25) and the `cursor:pointer`/hover block (DR-15) |
| `less/dropdown/dropdown-menu.less` (72) | **deleted** — it re-skins the Bootstrap 3 `.dropdown-menu` for the uib datepicker (G-E4) |

**Layout without Bootstrap 3** (C-9). `main.html:1-22`'s `.container` / `.row` / `.col-md-3` /
`.col-md-9` becomes CSS Grid in `_layout.less`: a `max-width: 1440px` container and
`grid-template-columns: 25% 75%` collapsing to a single column below `992px` (Bootstrap's `md`
breakpoint, preserved so the collapse point does not move).

> **Reading of C-9's "CDK Layout for the grid"**: `@angular/cdk/layout` provides `BreakpointObserver`,
> a **TypeScript** API for components that need to *react* to viewport changes. No component here does
> — the layout is static two-column-or-one. Introducing `BreakpointObserver` to do what a media query
> already does would violate `minimal-implementation.md` ("no speculative abstractions"). The grid is
> therefore implemented in CSS, and `BreakpointObserver` is **not** introduced. This deviation from the
> literal wording of C-9 is recorded here so it is reviewable rather than silent.

**Assets** (C-10):

| Asset | Fate |
|---|---|
| `imgs/logo-insurance-app.svg`, `person.svg`, `vehicle.svg`, `doc.svg`, `address.svg`, `arrow-tooltip.svg`, `favicon.ico` | → `frontend/public/`, `url()` re-anchored to `imgs/…` (assets land at the bundle root, and the CLI rewrites `url()` relative to the emitted stylesheet, which also sits at the bundle root) |
| `imgs/arrow.svg` | **dropped** — its only call site is the custom select chevron at `less/common/common.less:56`, which `mat-select`'s own indicator replaces (C-9) |
| `imgs/logo.png`, `imgs/select-arrow.png` | **dropped** — unreferenced (DR-24) |
| `src/main/webapp/fonts/open-sans/**` (90 files) | **deleted** — imported at `less/config.less:1` but overridden by Manrope in the body rule at `:9-16`; dead weight |
| `src/main/webapp/fonts/font-awesome/**` (6 files) | **deleted** — audited: Font Awesome has **zero call sites**; the only reference is the LESS import at `config.less:5` |
| Manrope | **self-hosted** at `public/fonts/manrope/Manrope[wght].woff2` (one variable font file covering `wght 200..800`, matching the current CDN query at `config.less:4`). Removes a runtime CDN dependency at application start. ⚠️ This commits one binary; `build.md`'s "vendored assets are never committed" targets *build output*, and C-10 pins this location. Net change: −96 tracked binaries, +1. |
| Material Icons font | **NOT added.** The only glyph in the application (`.glyphicon-calendar`, `driver.html:34`) is replaced by `<mat-datepicker-toggle>`'s built-in SVG icon. If a future call site needs `<mat-icon>`, self-host the font in `public/fonts/` then — not now (`minimal-implementation.md`). |

### 4.10 Interceptors

**`pending-requests.interceptor.ts` + `pending-requests.ts`** (G-D2). Replaces `ngLoadingSpinner`
(`app.js:4`), which registers an `$httpProvider` interceptor that reference-counts in-flight requests.
This is **implicit behaviour named nowhere in the templates** — its loss is silent. Port it **first**,
before any component, so every later component inherits it.

```
PendingRequests  (providedIn: 'root')
  count = signal(0)   ·   increment()   ·   decrement()

pendingRequestsInterceptor: HttpInterceptorFn
  increments on subscribe, decrements in finalize() — on success AND on error
```

⚠️ The current spinner **flashes on load** because eight `templateUrl` XHRs pass through the
interceptor (`current-state-analysis.md` §10). Compiled templates remove those requests. The flash
disappearing is a **recorded non-deviation** (OD-13) — accepted, not a regression.

**`mutation-error.interceptor.ts`** (DR-06 / C-7). All 14 mutation call sites have a success callback
only; a failed PUT is silently swallowed. The interceptor catches non-2xx responses on `PUT` requests
and writes into `QuoteStore.mutationError`, which the existing error banner renders. **No retry, no
rollback, no new UI surface** — C-7 accepts DR-06 narrowly precisely to keep it from becoming a new
feature under `minimal-implementation.md`.

### 4.11 Dev server (not part of `mvn package`)

`ng serve` on port 4200 + `frontend/proxy.conf.json`. Recommended shape (option A,
`target-state-plan.md` §1.5): run the dev backend at the root context
(`SERVER_SERVLET_CONTEXT_PATH=/ mvn spring-boot:run`) and proxy `driver`, `vehicle`, `dictionaries`,
`quote` 1:1 to `http://localhost:48080`. `JSESSIONID` then gets `Path=/` and cookies match with no
rewriting.

⚠️ If `/demo` is kept in dev instead, `cookiePathRewrite: {"/demo": "/"}` is **mandatory** — without
it Tomcat sets `JSESSIONID; Path=/demo`, the browser at `localhost:4200` never sends it back, every
request gets a fresh session, and the quote resets to defaults on every keystroke.

The proxy must be **same-origin**: there is no CORS configuration anywhere in `src/main/java`
(`@CrossOrigin` / `CorsRegistry` both absent) and no Spring Security. `ng serve` never participates in
`mvn package`.

---

## 5. Build wiring — exact changes

### 5.1 `pom.xml` — four edits

Untouched: `<finalName>motor-insurance</finalName>` (line 84), the `higson-cli-maven-plugin` block
(lines 143-153), the `all_in_one` profile (lines 157-179), `<packaging>war</packaging>` (line 11).

**(1) `exec-maven-plugin` (lines 95-114) — add one line to `<configuration>` (lines 99-105):**

```xml
<configuration>
    <executable>npm</executable>
    <!-- The Angular workspace lives in frontend/; without this the plugin runs at
         ${project.basedir} and finds no package.json. -->
    <workingDirectory>${project.basedir}/frontend</workingDirectory>   <!-- NEW -->
    <arguments>
        <argument>run</argument>
        <argument>build</argument>
    </arguments>
</configuration>
```

The `prepare-package` binding (line 111) is **unchanged** — it already runs after `process-classes`
and `test` and before `package`, which is exactly what `maven-war-plugin` needs.

**(2) NEW `maven-war-plugin` block — none exists today (100 % inherited defaults):**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-war-plugin</artifactId>
    <!-- Version deliberately omitted: managed at 3.4.0 by spring-boot-starter-parent:3.5.16,
         whose pluginManagement also supplies an <archive> block that an explicit <version>
         here would clobber. Do NOT pin this. -->
    <configuration>
        <webResources>
            <resource>
                <directory>${project.basedir}/frontend/dist/motor-insurance/browser</directory>
            </resource>
        </webResources>
    </configuration>
</plugin>
```

⚠️ **Do not pin `maven-war-plugin`** (non-negotiable constraint, `requirements.md` §7).
Output lands at the **WAR root**, matching today's layout exactly. `ApplicationConfig.java:17-19`
(`addViewController("/") → forward:/index.html`) needs **no change** and must not be "cleaned up".

⚠️ **Ordering under `-Pall_in_one`**: `maven-war-plugin:war` runs in `package`;
`spring-boot-maven-plugin:repackage` runs after it on the produced WAR. So `webResources` lands
*before* repackage and the executable WAR ships the UI. This must be an explicit acceptance check
(§10, AC-B4), not an assumption.

**(3) `maven-clean-plugin` filesets (lines 115-136) — retarget both, add one:**

| Current directory | Becomes |
|---|---|
| `node_modules` (line 121) | `frontend/node_modules` |
| `src/main/webapp/vendors` (line 128) | `frontend/dist` |
| — | `frontend/.angular` (CLI build cache) |

**(4) Test dependencies (lines 72-78) — resolve the JUnit 4 / JUnit 5 conflict:**

```xml
<!-- REMOVE -->
<dependency>
    <groupId>junit</groupId><artifactId>junit</artifactId>
    <version>4.13.2</version><scope>test</scope>
</dependency>

<!-- ADD -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

`pom.xml:73-78` declares `junit:junit:4.13.2` while the only existing test
(`src/test/java/pl/decerto/higson/demo/motor/SnapshotDataSourceDemoTest.java`, untracked) uses the
JUnit 5 Jupiter API with no Jupiter dependency declared. `spring-boot-starter-test` brings
JUnit 5 + AssertJ + `spring-test` (MockMvc). `test-writing.md` binds: *"drop JUnit 4 and add
`spring-boot-starter-test`; do not add new JUnit 4 tests."* `SnapshotDataSourceDemoTest` must still
compile afterwards.

Plus, for P-4, pin the test JVM timezone (`maven-surefire-plugin`, version inherited):

```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <!-- P-4: dateOfBirth carries a server-timezone offset; goldens must be
             reproducible across machines. -->
        <argLine>-Duser.timezone=UTC</argLine>
    </configuration>
</plugin>
```

**Nothing else changes.** `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh` and
`docker/app.properties` are verified unaffected — they only ever reference `target/motor-insurance.war`
and port 48080. `docker.md`'s "JRE, not JDK" rule means **no Node in the runtime image**; the Angular
build stays on the Maven host, which is what `prepare-package` already does.

### 5.2 `frontend/package.json`

```json
{
  "name": "motor-insurance-frontend",
  "private": true,
  "engines": { "node": "^22.22.3 || ^24.15.0 || >=26.0.0" },
  "scripts": {
    "build": "npm ci && ng build",
    "start": "ng serve --proxy-config proxy.conf.json",
    "test":  "ng test"
  }
}
```

- `"build": "npm ci && ng build"` preserves the exact self-bootstrapping pattern of today's
  `"build": "npm ci && gulp"` — `npm run build` works from an empty `node_modules` because npm puts
  `node_modules/.bin` on `PATH` before the script runs. **`npm ci`, never `npm install`** (`build.md`).
- `mvn package` runs **only** `npm run build`. It must **not** depend on Vitest or Playwright
  (`test` and the E2E project are separate entry points, G-G6).
- **All `@angular/*`, `typescript` and `rxjs` are exact-pinned** (no `^`). `ng new` scaffolds caret
  ranges; pin them after scaffolding and **before the first commit** (`build.md` "Exact Pins for
  Framework-Critical npm Packages"; G-A11 — low risk, easy to miss).
- `less@^4.2.0` in `devDependencies`. A caret range is correct here — `build.md` reserves exact pins
  for framework-critical packages.

### 5.3 `frontend/angular.json`

| Key | Value | Why |
|---|---|---|
| `projects.motor-insurance.architect.build.builder` | `@angular/build:application` | esbuild builder, v22 default |
| `options.outputPath` | `dist/motor-insurance` → emits `dist/motor-insurance/browser/` | matches the `<webResources>` directory in §5.1(2) |
| `options.index` | `src/index.html` | |
| `options.browser` | `src/main.ts` | |
| **`options.baseHref`** | ⛔ **NOT SET** | §5.6 |
| `options.styles` | `["src/theme.scss", "src/styles.less"]` | theme first so LESS wins the cascade (§4.9) |
| `options.inlineStyleLanguage` | `"less"` | |
| `options.stylePreprocessorOptions.includePaths` | `["src/styles"]` | lets partials import each other by bare name |
| `options.assets` | `[{ "glob": "**/*", "input": "public" }]` | CLI default; copies to the bundle root |
| `configurations.production.outputHashing` | `"all"` | |
| `serve.options.proxyConfig` | `"proxy.conf.json"` | dev only |

⚠️ **`less` must be an explicit dependency** — `@angular/build@22.1.3` declares it as an *optional*
peer (`peerDependenciesMeta.less.optional = true`) and bundles `sass`, not `less`. Omitting it is a
loud build failure, not a silent one, but it is easy to miss (G-A10).

⚠️ Emit **outside** `src/main/webapp/`. Emitting into it would break three ways: the directory is
git-tracked so hashed output would be committed; hashed names are never overwritten so stale
`main-*.js` would accumulate forever and all of it would be packed into the WAR (`mvn clean` does not
touch `src/main/webapp`); and Angular's `index.html` would clobber the source one.

### 5.4 `.gitignore`

Current (11 lines): `target`, `.idea`, `.classpath`, `.vim`, `.project`, `.DS_Store`, `.settings/`,
`node_modules/`, `src/main/webapp/vendors/`, `*.iml`, `higson-cli*.log`.

| Action | Entry | Why |
|---|---|---|
| REMOVE | `src/main/webapp/vendors/` | the directory ceases to exist |
| KEEP | `node_modules/` | no leading slash, so it already matches `frontend/node_modules/` and `e2e/node_modules/` |
| ADD | `frontend/dist/` | build output |
| ADD | `frontend/.angular/` | CLI cache |
| ADD | `e2e/test-results/`, `e2e/playwright-report/` | Playwright run output |
| UNTRACK + DELETE | `src/main/webapp/styles/styles.min.css` | 2946 lines, generated by Gulp but **committed**, and not actually minified — contradicts `css.md` and `build.md` (DR-21) |

`e2e/goldens/` **is** committed — it is captured evidence, not build output.

### 5.5 `frontend/.npmrc`

```
engine-strict=true
```

C-5 forbids `.nvmrc` and `frontend-maven-plugin`. It does not forbid `engines` enforcement, and R-1
rates the Node blocker **High** precisely because it fails opaquely inside `prepare-package`.
`engine-strict=true` plus the `engines` field in `package.json` converts that into an early failure
that prints the required range. Two lines total; no toolchain manager, no new plugin.

### 5.6 `<base href>` — ship none

**`frontend/src/index.html` must contain no `<base>` element**, and `angular.json` must set no
`baseHref`. `ng new` scaffolds `<base href="/">`; delete that line.

Rationale (`target-state-plan.md` §5.2, verified empirically): the servlet context path is a
**runtime** value and is actively overridden in practice — a throwaway
`decerto/higson-motor-insurance-demo:4.3.0` container with no env override logs `context path '/demo'`,
while the container running on this machine carries `SERVER_SERVLET_CONTEXT_PATH=/app` and logs
`context path '/app'`. Phase 1's recommendation (`ng build --base-href=/demo/`) would have shipped a
bundle that 404s on every asset under that override.

With no `<base>`, the emitted `<script src="main-HASH.js">` / `<link href="styles-HASH.css">` are
already relative with no leading slash and resolve against the document URL — exactly today's
semantics (`index.html:2` has no `<base>`). **This is also what keeps `services.md`'s "relative URLs
with no leading slash" rule intact with no exception**: `http.get('quote')` resolves to
`<contextPath>/quote` at any context path. There is no router, so `APP_BASE_HREF` is not needed.

---

## 6. The REST contract as a frozen interface

All SPA HTTP traffic lives in 4 service classes. **21 calls across 5 controllers.** No endpoint is
added, removed, or renamed. `api/ProfilersApi.java` is never called by the SPA and stays out of scope.

### 6.1 Body-encoding rules — the load-bearing part

`HttpClient.serializeBody()` (`@angular/common@22.1.0`, lines 537-549) branches on the **JavaScript
type** of the body:

| JS type passed | Bytes on the wire | `Content-Type` |
|---|---|---|
| `string` | **verbatim**, unquoted | `text/plain` |
| `number`, `boolean`, `object`, `Array` | `JSON.stringify(body)` | `application/json` |
| `null` / `undefined` | no body | — |

Three consequences, all binding:

1. ⛔ **NEVER `JSON.stringify` a string body, and never hand-set a `Content-Type` for one.**
   Phase 1 claimed `HttpClient` would JSON-quote these and corrupt them; that claim is **FALSE**
   (`target-state-plan.md` §5.1, verified against the Angular source *and* live against the running
   backend). The failure mode is now the opposite: wrapping a value in `JSON.stringify` "to be safe"
   is what would actually corrupt it — a control test sending `"Krakow"` with quotes produced
   `city = '"Krakow"'`. Spring's `StringHttpMessageConverter` accepts `text/plain` for
   `@RequestBody String`, and none of the 16 `DriverApi` / `VehicleApi` handlers declares `consumes`.

2. **The `Content-Type` header changes** on the six string PUTs: `application/json` → `text/plain`.
   This is the only wire divergence in the entire contract. It is **verified accepted** end-to-end
   (`PUT driver/address/city` body `Gdansk` with `text/plain` → 200, `GET driver` → `city = 'Gdansk'`).
   The MockMvc goldens must pin it (§8.2).

3. ⚠️ **`PUT driver/birthDate` must be passed a `Date` instance, never an ISO string.**
   `DriverApi.java:34-37` is `@RequestBody Date` — and unlike the other ten handlers, `required` is
   **not** set, so a body is mandatory. Passing a `Date` makes `serializeBody` take the *object*
   branch → `JSON.stringify(date)` → `"1990-05-12T00:00:00.000Z"` with `application/json`, which is
   byte-identical to what AngularJS sends today. Passing a **string** would take the string branch →
   unquoted body with `text/plain` → Jackson cannot bind `Date` from `text/plain` → **HTTP 415**.
   This hazard is recorded in neither analysis document; it is a genuine migration trap. The component
   therefore holds a `Date` (which is also what `MatDatepicker` binds), while `DriverDto.dateOfBirth`
   types as `string` on the wire (§6.3).

### 6.2 The 21 endpoints

| # | Service method (target) | Method + exact URL | Body / params | JS type passed | Wire encoding | Backend |
|---|---|---|---|---|---|---|
| 1 | `DictionaryService.getMakeDictionary(productionYear)` | GET `dictionaries/make` | `{ params: { productionYear } }` | — | — | `DictionaryApi.java:27-30` |
| 2 | `DictionaryService.getModelDictionary(typeId)` | GET `dictionaries/model` | `{ params: { typeId } }` | — | — | `DictionaryApi.java:32-35` |
| 3 | `DictionaryService.getProductionYearDictionary()` | GET `dictionaries/productionYear` | none | — | — | `DictionaryApi.java:37-40` |
| 4 | `DictionaryService.getTypeDictionary(makeId)` | GET `dictionaries/type` | `{ params: { makeId } }` | — | — | `DictionaryApi.java:42-45` |
| 5 | `DriverService.getDriver()` | GET `driver` | none | — | — | `DriverApi.java:28-32` |
| 6 | `DriverService.updateBirthDate(d)` | PUT `driver/birthDate` | date | **`Date`** ⚠️ | `"…ISO…"` · `application/json` | `DriverApi.java:34-37` (**required**) |
| 7 | `DriverService.updateFirstName(s)` | PUT `driver/firstName` | name | `string` | verbatim · `text/plain` | `DriverApi.java:39-42` |
| 8 | `DriverService.updateGender(s)` | PUT `driver/gender` | `'M'` / `'F'` | `string` | verbatim · `text/plain` | `DriverApi.java:44-47` |
| 9 | `DriverService.updateLastName(s)` | PUT `driver/lastName` | name | `string` | verbatim · `text/plain` | `DriverApi.java:49-52` |
| 10 | `DriverService.updateAccidentCount(n)` | PUT `driver/accidentCount` | count | `number` | `5` · `application/json` | `DriverApi.java:54-57` |
| 11 | `DriverService.updateTrafficTicketsCount(n)` | PUT `driver/trafficTicketsCount` | count | `number` | `5` · `application/json` | `DriverApi.java:59-62` |
| 12 | `DriverService.updateLicenceObtainedAtAge(n)` | PUT `driver/licenceObtainedAtAge` | age | `number` | `18` · `application/json` | `DriverApi.java:64-67` |
| 13 | `DriverService.updateZipCode(s)` | PUT `driver/address/zipCode` | zip | `string` | verbatim · `text/plain` | `DriverApi.java:69-72` |
| 14 | `DriverService.updateCity(s)` | PUT `driver/address/city` | city | `string` | verbatim · `text/plain` | `DriverApi.java:74-77` |
| 15 | `DriverService.updateStreet(s)` | PUT `driver/address/street` | street | `string` | verbatim · `text/plain` | `DriverApi.java:79-82` |
| 16 | `VehicleService.getVehicle()` | GET `vehicle` | none | — | — | `VehicleApi.java:29-32` |
| 17 | `VehicleService.updateMake(makeId, make)` | PUT `vehicle/make` | `{ makeId, make }` | `object` | JSON · `application/json` | `VehicleApi.java:43-50`; **side effect: typeId=0, modelId=0** |
| 18 | `VehicleService.updateModel(n)` | PUT `vehicle/model` | modelId | `number` | JSON · `application/json` | `VehicleApi.java:52-56` |
| 19 | `VehicleService.updateType(n)` | PUT `vehicle/type` | typeId | `number` | JSON · `application/json` | `VehicleApi.java:58-63`; side effect modelId=0 |
| 20 | `VehicleService.updateProductionYear(n)` | PUT `vehicle/productionYear` | year | `number` | JSON · `application/json` | `VehicleApi.java:34-41`; side effect makeId=typeId=modelId=0 |
| 21 | `VariantService.getQuote()` | GET `quote` | none | — | — | `QuoteApi.java:30-35` — runs `motor.calculate(quote)` (full rules evaluation + validation) on **every** GET |

Binding rules for the service layer:

- **URLs stay relative with no leading slash**, exactly as written above (`services.md`, 21 of 21
  today). ⛔ Do not "modernize" to absolute paths — that breaks the context path (§5.6).
- **`HttpClient` already unwraps the response body.** `services.md`'s clause *"callers resolve
  `response.data` themselves"* describes `$http` and **must be deleted from the standard**, or it will
  actively generate wrong code. A leftover `.data` yields `undefined`, not an error — a silent bug
  across 11 call sites (G-B2, R-6).
- Query parameters go in the `params` option object, never string concatenation.
- Components never inject `HttpClient` (`services.md`). `QuoteStore` injects the *services*, not
  `HttpClient`.

### 6.3 DTO shapes (typed models in `frontend/src/app/models/`)

- **`DriverDto`**: `{ firstName: string; lastName: string; dateOfBirth: string; gender: string;
  address: { zipCode: string; city: string; street: string }; numberOfAccidents: number;
  numberOfTickets: number; licenceObtainedAtAge: number }`
  ⚠️ **`dateOfBirth` is an ISO-8601 STRING**, e.g. `"1986-08-06T00:00:00.000+00:00"` — not epoch
  millis. Phase 1 stated the opposite (`current-state-analysis.md:194`); corrected empirically
  (PC-3). Typing it `number` would break `MatDatepicker` binding.
- **`VehicleDto`**: `{ makeId: number; make: string; modelId: number; typeId: number;
  productionYear: number }` — all primitive `int` server-side, **never null; unset = 0**, which is
  what makes the cascade guards work.
- **`DictionaryEntryDto`**: `{ code: string; name: string }` — ⚠️ `code` is a **String**; coerce with
  `Number()` at the binding site.
- **`QuoteDto`**: `{ options: { code, order, premium, premiumBeforeDiscounts,
  coverages: { position, code, name, limitLeft, limitRight, premium, description }[],
  discounts: { code, name, value, position }[] }[] }`. `coverage.description` is **HTML** (§4.8).
- **`ErrorDto`**: `{ message: string }` — new, introduced by C-6 (§6.4).

### 6.4 The one sanctioned contract change — C-6 / DR-14

**Today** (verified live): a failed `GET quote` returns **HTTP 404** with body
`{"timestamp":…,"status":404,"error":"Not Found","path":"/app/quote"}`. There is no `message` key, so
`variants.js:37`'s `error.data.message` is `undefined` and **the red banner renders with no text at
all.**

Cause: `exceptions/GlobalExceptionHandler.java:8` is a plain `@ControllerAdvice` whose handlers return
`String` **without `@ResponseBody`** (lines 13-15, 19-21). Spring MVC treats the return value as a
**view name**; view resolution fails, and the resulting 404 overrides the `@ResponseStatus(BAD_REQUEST)`.

**After** (C-6, the only backend change in the migration):

| | Before | After |
|---|---|---|
| Status | `404 Not Found` | `400 Bad Request` |
| Body | `{"timestamp":…,"status":404,"error":"Not Found","path":"…"}` | `{"message":"Driver first name not supplied."}` |
| Banner | empty | the actual `QuoteValidator` message |

Change: `@ControllerAdvice` → `@RestControllerAdvice` on
`pl.decerto.higson.demo.motor.exceptions.GlobalExceptionHandler`, and both handlers return a new
`pl.decerto.higson.demo.motor.api.dto.ErrorDto` (a mutable POJO with a single `message` field, per
`models.md` "no records, builders, or Lombok" and `api.md` "DTOs cross the API boundary"). The
`@ResponseStatus(BAD_REQUEST)` annotations already present (lines 12, 18) then take effect.

This restores the messages `example/QuoteValidator.java` was written to produce — "Driver birth date
not supplied.", "Driver first name not supplied.", "Driver gender not supplied.", "Driver address
not supplied.", "Driver address zip code / city / street not supplied.", "Vehicle production year /
make / type / model not supplied."

⚠️ **Golden handling**: the MockMvc golden must first record the **current 404-with-no-message**
(during P-3), and then be updated **deliberately** as part of implementing DR-14. An undocumented
golden edit is a regression; a documented one is the register working as designed.

`spring.error.include-message` stays at its default (`never`) — the message now comes from the
handler, not from Spring's `/error` machinery, so no property change is needed.

### 6.5 Session semantics — preserve exactly

`service/QuoteService.java:10-12` is `@Service @SessionScope`, holding the whole `Quote` aggregate,
created once in `@PostConstruct initializeQuote()`. There is **no request-carried state at all** — the
wizard is a sequence of side-effecting PUTs against a `JSESSIONID` cookie.

Binding: losing or splitting the session resets the quote to defaults; two tabs share one quote; a hard
reload does **not** reset it. No CORS, no auth, no CSRF, no Spring Security. The dev proxy must pass
cookies through and must not introduce a second origin (§4.11).

---

## 7. Defect register as an acceptance artefact

**This register is the complete list of sanctioned deviations from the parity checklist in
`current-state-analysis.md`. Anything not on it is a regression.** Phase 6 verifies against the
checklist **plus** this register. Every entry carries an acceptance criterion below.

`Δreq` = changes backend request volume · `Δord` = changes rendering order.

### 7.1 Behavioural defects DR-01 … DR-19

| ID | Location | Fix | Acceptance criterion | Δreq | Δord |
|---|---|---|---|---|---|
| **DR-01** | `vehicle.js:61-63` | Guard the make lookup; on the blank option clear `make`, `typeId`, `modelId` + their dictionaries and issue **no** `PUT vehicle/make` | Playwright: select the blank Make option → no console error, **zero** `PUT vehicle/make`, Type and Model dropdowns empty | yes (net 0) | no |
| **DR-02** | `driverHistory.html:8,18,27` | Do not send when the value is null / NaN / below `min`; keep the last valid server value | Playwright: clear "Number of accidents" → **zero** `PUT driver/accidentCount`, **zero** 5xx; the previous quote stays rendered | yes | no |
| **DR-03** | `driverHistory.html:8,18,27` | Commit on **blur**, matching the 7 text fields in `driver.html` | Playwright: type `12` into a numeric field then blur → exactly **1** PUT and **1** `GET quote` (today: 2 + 2) | **yes — large reduction** | no |
| **DR-04** | `driver.html:23-32` | Commit on datepicker close / input blur only | Playwright: type a full date then blur → exactly **1** `PUT driver/birthDate` | yes | no |
| **DR-05** | `variants.html:8,21` | Remove the dead `\| orderBy: position`; render in array order, **introduce no client-side sort** | Contract test: coverage `position`s ascending in all 3 options. E2E: rendered order identical to the golden | no | **no** |
| **DR-06** | all 14 mutation sites | Single `HttpInterceptorFn` surfacing PUT failures through the existing banner. No retry, no rollback (**C-7: narrow**) | Playwright with a forced 500 on `PUT driver/city` → banner visible with text; no retry request | no | no |
| **DR-07** | `variants.js:35-38` | Clear the variants when the quote fails | Playwright: force a failed `GET quote` → **zero** variant cards, banner visible | no | **yes** |
| **DR-08** | `driver.html:38-53` | Set value **then** PUT; **skip entirely when unchanged** | Playwright: click the already-active gender toggle → **zero** `PUT driver/gender`, **zero** `GET quote` | **yes — reduction** | no |
| **DR-09** | `driver.js:53-59` + `driverHistory.js:30-36` | One `GET driver` shared through `QuoteStore` | Playwright: on load exactly **1** `GET driver` (today: 2) | **yes — 2 → 1** | no |
| **DR-10** | `variants.html:19,28`, `discount.html:11` vs `coverage.html:12,13,16` | `DecimalPipe '1.2-2'` on **all four** money fields; keep the `-` prefix on discounts; **no currency symbol** | Rendered `premiumBeforeDiscounts`, `premium`, coverage limits/premium and discount value all match `/^-?[\d,]+\.\d{2}$/`; no `$`/`zł`/`PLN` anywhere | no | no |
| **DR-11** | `coverage.html:12,13,16` | Leave `LOCALE_ID` at Angular's default `en-US`; **do not** register `pl` | `100000` renders `100,000.00` (not `100 000,00`); `app.config.ts` contains no `LOCALE_ID` provider and no `registerLocaleData` | no | no |
| **DR-12** | `vehicle.js:102-107` | Clear `typeDictionary` and `modelDictionary` alongside the field values | Playwright: change Production Year → Type and Model dropdowns are **empty**, not stale | no | **yes** |
| **DR-13** | `vehicle.js:76` | ⛔ **KEEP AS-IS — not fixed.** Only Model triggers a reload | Playwright: change Production Year / Make / Type → **zero** `GET quote`; change Model → exactly **1** | **no (deliberate)** | no |
| **DR-14** | `GlobalExceptionHandler.java` + `variants.js:35-38` | Backend fix (C-6): `@RestControllerAdvice` + `ErrorDto`, 400 with `{message}` — §6.4 | Contract test: `GET quote` with a blanked first name → **400**, body `{"message":"Driver first name not supplied."}`. E2E: banner shows that text. The golden is updated **deliberately**, with this row cited | no | **yes** |
| **DR-15** | `less/variants/variants.less:11-27` | Remove `cursor:pointer` and the hover shadow/background | Computed `cursor` on a variant card is `default`; no hover-state rule targets it | no | no (visual) |
| **DR-16** | `sidebar.js:21-27`, `less/sidebar/sidebar.less:33-36` | Do not port `sidebarHidden` / `toggleSidebar`; delete the CSS leftovers | `grep -r "sidebarHidden\|toggleSidebar" frontend/src` → empty; no `transition`/`opacity` collapse rules | no | no |
| **DR-17** | `driver.js:33` | Do not port `$scope.selectedDate` | `grep -r "selectedDate" frontend/src` → empty | no | no |
| **DR-18** | `variants.html:24`, `discount.js:13` | Drop the `priceBeforeDiscount` binding and input | `grep -ri "priceBeforeDiscount" frontend/src` → empty | no | no |
| **DR-19** | `driver.html:26,31` | Do not port `close-text` (the button bar is disabled) | `grep -r "close-text\|closeText" frontend/src` → empty | no | no |

### 7.2 Structural / hygiene defects DR-20 … DR-27 — no user-visible behaviour change

| ID | Location | Fix | Acceptance criterion |
|---|---|---|---|
| **DR-20** | `index.html:14`, `main.js:4` | Root selector `main` → **`app-root`** | `frontend/src/index.html` body contains `<app-root>`; no component declares selector `main` |
| **DR-21** | `src/main/webapp/styles/styles.min.css` | Deleted and untracked; CLI output is hashed and git-ignored | `git ls-files \| grep styles.min.css` → empty; `frontend/dist/` is ignored |
| **DR-22** | root `package.json:17` | `merge-stream` deleted with Gulp | root `package.json` no longer exists |
| **DR-23** | `gulpfile.js:43-45` | Broken `gulp.watch` replaced by `ng serve` | `gulpfile.js` deleted; `npm start` in `frontend/` serves with HMR |
| **DR-24** | `src/main/webapp/imgs/` | `logo.png`, `select-arrow.png` not carried over | absent from `frontend/public/` |
| **DR-25** | `less/variants/coverage.less`, `less/common/common.less` | `.coverage__divider`, `.common-checkbox`, `.common-subsection-heading` not carried over | absent from `frontend/src/styles/` |
| **DR-26** | all 13 `*.js` | Minification-safety resolved by construction (TS + AOT + `inject()`) | production build succeeds with default optimization |
| **DR-27** | `driver.html:29`, `driver.js:42,45-47` | `is-open="popup.opened"` `$scope` binding → a single component signal | datepicker open state is one signal on the component; no dual-scope binding |

### 7.3 Recorded NON-deviations — these must NOT change

Listing these prevents a well-meaning implementer from "fixing" them and creating an unsanctioned diff.
**Changing any of these is a regression, even though each looks like an improvement.**

| # | Item | Location | Why it stays | How Phase 6 checks it |
|---|---|---|---|---|
| **ND-1** | Vehicle emit asymmetry — only Model triggers a reload | `vehicle.js:76` | Changing it multiplies `GET quote` volume, each a full Higson evaluation. Documented as intended in `components.md` | DR-13 acceptance criterion |
| **ND-2** | Sidebar section labelled **"Documents"** while containing claim history | `sidebar.html:33` | Copy change, i.e. a product decision — out of scope | Rendered label is exactly `Documents` |
| **ND-3** | The "Address" divider emitted from **inside** `driver.html:55-60`, not from the sidebar | `driver.html:55-60` | Odd structure, zero user impact; restructuring risks layout drift for no gain | Divider markup lives in the driver component |
| **ND-4** | No `<form>` element and no validation display anywhere | all templates | Adding validation is a **new feature** | No `mat-error`, no `required`, no `Validators` in the app |
| **ND-5** | Zip code has no mask or pattern | `driver.html:64-69` | Same — new feature | Free-text input, no pattern |
| **ND-6** | Startup spinner flash from 8 `templateUrl` XHRs | `main.html:2` | Disappears by construction with compiled templates; cannot be reproduced without an artificial delay. **Sanctioned disappearance** (OD-13) | Not asserted; noted in the E2E readme |
| **ND-7** | No router, no deep linking | — | YAGNI (`minimal-implementation.md`); a router would require a catch-all forward in `ApplicationConfig.java` | No `provideRouter`, no `<router-outlet>`; `ApplicationConfig.java` unchanged |
| **ND-8** | WAR not deployable to an external Tomcat (`Application` does not extend `SpringBootServletInitializer`) | `Application.java` | Current, intended behaviour — **do not "accidentally fix"** | `Application.java` unchanged |

**Totals: 19 behavioural + 8 structural = 27 sanctioned deviations, plus 8 recorded non-deviations.**

---

## 8. Test strategy (C-4)

C-4 requires three kinds of evidence. C-9 **downgrades the third** from a gate to documentation.

### 8.1 What proves what

| Evidence | Proves | Gate? |
|---|---|---|
| MockMvc contract tests | the backend contract is byte-frozen (§6) | ✅ **yes** |
| Playwright behavioural flows | feature set, recalculation semantics, request volume | ✅ **yes** |
| Screenshots | that the redesign happened, and what it looks like | ❌ **no** — documentation only (§8.5) |

### 8.2 MockMvc contract tests — the frozen interface

Location: `src/test/java/pl/decerto/higson/demo/motor/api/` — `test-writing.md` binds *"Test Layout
Mirrors the Main Source Package"*, `<ClassUnderTest>Test`, never `Tests`.

| Class | Pins |
|---|---|
| `DriverApiTest` | endpoints 5-15: exact response JSON of `GET driver` incl. the **ISO-8601 `dateOfBirth` with its `+00:00` offset**; for each of the 6 string PUTs the **raw unquoted body with `Content-Type: text/plain`** followed by a `GET driver` read-back proving the value is uncorrupted; the 3 numeric PUTs with `application/json`; `PUT driver/birthDate` with a JSON-quoted ISO string |
| `VehicleApiTest` | endpoints 16-20 incl. the **server-side cascade side effects** (make → typeId=0, modelId=0; type → modelId=0; productionYear → makeId=typeId=modelId=0) |
| `DictionaryApiTest` | endpoints 1-4; `DictionaryEntryDto.code` is a **String** |
| `QuoteApiTest` | endpoint 21: the `QuoteDto` shape; **coverage and discount `position`s ascending** (the DR-05 evidence); and the **error contract** — recorded as today's **404 with no `message`** during P-3, updated to **400 with `{"message": …}`** when DR-14 lands |

Rules:

- `@SpringBootTest` + `MockMvc`, booting the Higson engine **from the snapshot** via
  `HigsonSnapshotDataSource` — offline, reproducible, no database, no network
  (`rule-tests.md` "Run Rule Tests Off the Snapshot, Not a Live Database"). Consume
  `${higson.snapshot.dir}` directly; **never copy the snapshot into test resources**.
- **Session semantics**: a single `MockHttpSession` threaded through each request sequence, so
  `@SessionScope QuoteService` behaves as it does in the browser. A test that creates a fresh session
  per request tests nothing.
- Test classes are **package-private**, method names describe behaviour without a `test` prefix, every
  assertion carries a message, shared fixture in a static `@BeforeAll` (`test-writing.md`).
- **2-8 focused tests per implementation step group.** Verification runs only the new tests, not the
  entire suite.
- ⚠️ These tests require **P-2** (a populated snapshot). Document that in the class Javadoc — a fresh
  clone cannot run them.

### 8.3 Playwright flows — written against semantics, not markup

Location: **`e2e/` at the repository root**, a standalone npm project with its own `package.json` and
`playwright.config.ts` — deliberately **outside `frontend/`**. Rationale: the same spec files must run
unmodified against the AngularJS build (for capture) and the Angular build (for replay). Coupling them
to the Angular workspace would make that impossible, and the harness must exist **before** `frontend/`
does (P-3 precedes scaffolding).

**Selector policy — binding.** Locate elements only by:

1. accessible role + accessible name — `getByRole('textbox', { name: 'First name' })`
2. label text — `getByLabel('City')`
3. `data-testid` — for anything with no accessible name (the variant cards, the error banner,
   the spinner)

⛔ Forbidden: Bootstrap class names, `.common-input-default__input`, `mat-*` class names, `nth-child`,
DOM-structure paths. A selector that survives only one of the two frontends defeats the entire golden
strategy.

**Consequence for the AngularJS side**: capturing against the current build requires adding
`data-testid` attributes to the *existing* templates for the handful of elements with no accessible
name. This is a **capture-only, additive** change to files that are about to be deleted; it must be
committed **before** the golden run so the capture and the replay use identical selectors. Everything
with a visible label is reachable by role/label in both frontends without touching anything.

**Flows to capture** (each a spec file; each asserts *behaviour + request counts*, never appearance):

| Flow | Asserts |
|---|---|
| `startup.spec.ts` | initial render; server defaults (John / Potter / M / today−40y / Lake Jackson / Allwood St / 77566; TOYOTA 2010); 3 variant cards BRONZE/SILVER/GOLD; **`GET driver` count** (2 → 1, DR-09); all 4 dictionaries loaded |
| `driver-fields.spec.ts` | 7 field edits, each blur-committed → 1 PUT + 1 `GET quote`; gender toggle incl. the re-click case (DR-08) |
| `driver-history.spec.ts` | blur commit (DR-03), the cleared-field guard (DR-02), the `min` guard |
| `vehicle-cascade.spec.ts` | the 4-level cascade; dependent-field clearing; **the emit asymmetry** (DR-13/ND-1: 0 `GET quote` for year/make/type, 1 for model); the blank-option case (DR-01); stale dictionaries (DR-12) |
| `quote-panel.spec.ts` | option order, coverage/discount order (DR-05), money formatting (DR-10/DR-11), the coverage description popover renders **HTML** (C-8) |
| `error-paths.spec.ts` | failed `GET quote` → banner + **no** stale cards (DR-07); failed PUT → banner (DR-06); the DR-14 message |

**Request counting** is a first-class assertion, not a nicety: DR-02, DR-03, DR-04, DR-08, DR-09 and
DR-13 are all only observable as changes in request volume. Use `page.on('request')` filtered by
method + URL, and assert exact counts per interaction.

### 8.4 Capturing goldens against the current build (P-3)

The substitute for dual-run. **The pre-migration commit is a git artefact** — that is what makes an
A/B comparison free:

```
1. mvn higson-cli:pull                       # P-2
2. TZ=UTC mvn clean package -Pall_in_one     # the AngularJS WAR
3. TZ=UTC java -jar target/motor-insurance.war
4. cd e2e && TZ=UTC npx playwright test --update-snapshots
   → e2e/goldens/*.json  (request logs, response bodies, rendered text)
   → e2e/goldens/screenshots/*.png  (documentation only)
5. mvn test                                   # MockMvc goldens
6. git commit -m "golden captures against the pre-migration build"
7. git tag pre-angular-migration               # the replay anchor
```

**Replay after the migration** is the same commands against the new WAR. Any diff must be traceable to
**C-9** (appearance — ignored by construction, since no spec asserts appearance) or to a **DR-nn** row.
An untraceable diff is a regression.

⚠️ Goldens must be captured **before** a single file under `src/main/webapp/`, `less/`, `gulpfile.js`
or the root `package.json` is deleted or modified (beyond the additive `data-testid` attributes of
§8.3). A `git revert` restores *code*; it cannot recreate a *runtime* capture.

### 8.5 Screenshots — documentation, not a gate

C-9 removes the whole basis for screenshot comparison: with the entire UI redesigned, a before/after
diff cannot distinguish an intended redesign from a regression.

- Screenshots **are** captured before and after, and stored under `e2e/goldens/screenshots/`.
- They are **never** a pass/fail assertion. `toHaveScreenshot()` must not appear in any spec.
- Their purpose is a reviewable record of the redesign for the Phase 8 documentation.
- **Downstream phases must not report a visual difference as a finding.**

### 8.6 `TZ=UTC` (P-4)

Three places, all mandatory:

| Where | How |
|---|---|
| MockMvc / Maven | `maven-surefire-plugin` `<argLine>-Duser.timezone=UTC</argLine>` (§5.1(4)) |
| The application under E2E test | launch with `TZ=UTC` / `-Duser.timezone=UTC` |
| Playwright | `use: { timezoneId: 'UTC' }` in `playwright.config.ts`, plus `TZ=UTC` in the run command |

Reason: `Motor.createDriver` seeds "today − 40 years, start-of-day, **system TZ**", so the emitted
offset follows the server's zone (`+00:00` in the container, `+02:00` on a Europe/Warsaw box), while
the `MatDatepicker` adapter formats and parses in **browser-local** time. Unpinned, the displayed day
can be off by one and every date-touching golden becomes machine-dependent. Record the timezone in the
capture metadata.

### 8.7 Frontend unit tests

Vitest via `@angular/build` (`ng test`). Scope for step 1 — 2-8 focused tests per group, on the logic
that is genuinely non-trivial and not covered end-to-end:

- `QuoteStore` — the emit asymmetry (DR-13) and the reload trigger
- `IsoDateAdapter` — `yyyy-MM-dd` format, `yyyy/MM/dd` alt-parse, Monday-first
- `pendingRequestsInterceptor` — counter goes to zero on both success and error
- the vehicle cascade's clear-dependants logic (DR-01, DR-12)

⛔ **`mvn package` must not depend on a browser test runner.** `npm run build` is `npm ci && ng build`
only (§5.2).

### 8.8 CI

**Out of scope for step 1** — stated explicitly rather than implying a safety net exists. There has
never been CI in this repository (`test-writing.md`: no CI has ever gated a merge). The tests above are
run manually per the commands in §8.4.

---

## 9. Reusability analysis

### 9.1 Carried over unchanged

| Asset | Location | How it is reused |
|---|---|---|
| **The REST contract** | `services.md`; `src/main/java/**/api/*.java` | All 21 URLs, the relative-no-leading-slash convention, session semantics. **Zero backend endpoints change.** Frozen in §6. |
| **`<Domain>Service` naming + one-method-per-endpoint shape** | `components/service/*.js` (4 files) | The 4 target classes mirror the existing method names 1:1 (`getMakeDictionary`, `updateVehicleProductionYear`, …), so the diff is readable and `services.md`'s naming rule survives verbatim |
| **LESS partials** | `less/main/main.less`, `less/sidebar/sidebar.less`, `less/variants/*.less` | Layout, spinner positioning (`main.less:6-12`), webkit scrollbars (`main.less:14-32`), sidebar BEM + 4 icon backgrounds, variant card structure — carried near-mechanically with `url()` re-anchoring |
| **Brand tokens** | `less/variables.less:1-14` | Seed the Material M3 theme (`#4C4CDE`, `8px`) and the page gradient (`config.less:12`) |
| **SVG assets** | `src/main/webapp/imgs/` (6 of 8) + `favicon.ico` | Relocated to `frontend/public/imgs/`, byte-identical |
| **Maven packaging chain** | `pom.xml:84`, the `all_in_one` profile, `prepare-package` binding | `finalName`, the profile and the phase binding are untouched; only `<workingDirectory>` and the new `<webResources>` are added |
| **Docker chain** | `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh`, `docker/app.properties` | **Zero changes.** They only ever reference `target/motor-insurance.war` and port 48080 |
| **Higson snapshot tooling** | `higson-cli-maven-plugin` block, `${higson.snapshot.dir}` | Untouched. The MockMvc tests **reuse** the existing `SnapshotDataSourceDemoTest` pattern (`HigsonSnapshotDataSource` off the canonical snapshot directory) rather than inventing a test data source |
| **All backend Java** | `src/main/java/**` | One file changes (`GlobalExceptionHandler`, C-6). 47 of 48 files untouched |

### 9.2 Rewritten (existing behaviour, new implementation)

| Current | Becomes | Why rewriting rather than reusing |
|---|---|---|
| 8 element directives | 8 standalone components | `restrict:'E'` + `bindToController` + `controllerAs` has no Angular equivalent; the shape is textbook-uniform so the rewrite is near-mechanical |
| 4 `$http` factories | 4 `@Injectable` services | `$http` → `HttpClient`; method names and URLs preserved |
| `$emit`/`$broadcast` relay (`main.js:21-23`) | `QuoteStore` signals | The `$scope` event bus has no Angular equivalent (G-D1) |
| `ngLoadingSpinner` interceptor | `HttpInterceptorFn` + signal | AngularJS-locked package, unmaintained, currently not even installed |
| `uib-datepicker-popup` | `MatDatepicker` + custom `DateAdapter` | AngularJS-locked, no drop-in successor — the largest single item |
| `angular-tooltips` HTML tooltip | CDK Overlay popover | AngularJS-locked; `MatTooltip` is text-only (§4.8) |
| Bootstrap 3 grid | CSS Grid (`_layout.less`) | C-9 drops Bootstrap 3 |
| Gulp pipeline | `angular.json` | The CLI compiles LESS natively; Gulp buys nothing |

### 9.3 Deleted outright

| Deleted | Count / size | Why |
|---|---|---|
| `src/main/webapp/{index.html, app.js, components/**}` | 24 files, 920 lines | Rewritten in `frontend/src/` |
| `src/main/webapp/styles/styles.min.css` | 2946 lines | Generated-but-committed (DR-21) |
| `src/main/webapp/fonts/**` | **96 files** | Open Sans (90) is dead — overridden by Manrope at `config.less:9-16`. Font Awesome (6) has **zero call sites** (verified: the only reference is the import at `config.less:5`). C-10 |
| `src/main/webapp/imgs/{logo.png, select-arrow.png}` | 2 | Unreferenced (DR-24) |
| `src/main/webapp/imgs/arrow.svg` | 1 | Its only call site (`common.less:56`, the custom select chevron) dies with `mat-select` (C-9) |
| `src/main/webapp/vendors/` | concept | The bundler resolves imports; `prepare:libs` deleted |
| `less/` | 10 files, 599 lines | Moved to `frontend/src/styles/`; `dropdown/dropdown-menu.less` (72 lines) and most of `common/common.less` (143 lines) **deleted rather than ported** (C-9) |
| `gulpfile.js`, root `package.json`, root `package-lock.json` | 3 | Replaced by `frontend/` |
| 10 AngularJS-era npm dependencies | — | `angular`, `angular-ui-bootstrap`, `angular-tooltips`, `angular-loading-spinner`, `angular-spinner`, `spin.js`, `bootstrap`, `font-awesome`, `npm-font-open-sans`, `merge-stream` (DR-22) |
| `junit:junit:4.13.2` | `pom.xml:73-78` | Replaced by `spring-boot-starter-test` |

**`src/main/webapp/` is emptied but the directory is KEPT** (with a `.gitkeep`) as the
`warSourceDirectory`, so nothing else in the inherited WAR lifecycle has to change.

---

## 10. Acceptance criteria

Each is testable and traceable. **AC-D\* are the defect register criteria in §7 — all 27 plus the
8 non-deviations of §7.3 — and are not restated here.**

### Build & packaging

| # | Criterion |
|---|---|
| **AC-B1** | `mvn clean package` from a clean clone (Node ≥ 22.22.3) produces `target/motor-insurance.war` containing `index.html` and at least one `main-*.js` at the WAR root |
| **AC-B2** | `mvn clean package -Pall_in_one` produces an **executable** WAR that still contains the UI — repackage runs after `war`, and the assets survive it |
| **AC-B3** | `finalName` is `motor-insurance`; the WAR path scripts reference is unchanged |
| **AC-B4** | The built WAR serves the SPA and `GET quote` succeeds at **three** context paths: `/demo` (default), `/`, and `/app`. Proves the no-`<base>` decision (§5.6) |
| **AC-B5** | `mvn clean` removes `frontend/node_modules`, `frontend/dist` and `frontend/.angular` |
| **AC-B6** | `git status` after a full build is clean — no generated asset is tracked; `git ls-files \| grep -E "dist/\|styles.min.css\|node_modules"` is empty |
| **AC-B7** | `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh`, `docker/app.properties` are byte-identical to `HEAD` before the migration |
| **AC-B8** | `frontend/package.json` pins every `@angular/*`, `typescript` and `rxjs` to an exact version; `typescript` is in `~6.0.x` |
| **AC-B9** | Port 48080 appears unchanged in `higson-demo-app.properties`, `docker/app.properties`, `EXPOSE` and the compose mapping |

### Contract

| # | Criterion |
|---|---|
| **AC-C1** | All 21 endpoints respond as pinned by the MockMvc goldens; only the DR-14 row differs from the P-3 capture, and that difference is annotated with `DR-14` |
| **AC-C2** | The 6 string PUTs send **unquoted** bodies. `grep -rn "JSON.stringify" frontend/src` returns **zero** hits |
| **AC-C3** | `PUT driver/birthDate` sends a JSON-quoted ISO-8601 string with `Content-Type: application/json` — verified by a contract test, not by inspection |
| **AC-C4** | All 21 request URLs in `frontend/src/app/services/` are relative with **no leading slash**; `grep -n "http.\(get\|put\)('/" frontend/src` returns zero hits |
| **AC-C5** | `grep -rn "HttpClient" frontend/src/app/components` returns **zero** hits (`services.md`) |
| **AC-C6** | `grep -rn "\.data" frontend/src/app` finds no `response.data` unwrapping (R-6) |
| **AC-C7** | The session survives a full wizard flow — one `JSESSIONID`, no quote reset mid-flow |
| **AC-C8** | `GlobalExceptionHandler` is the **only** changed Java file; `git diff --name-only src/main/java` lists exactly one path |

### Behaviour

| # | Criterion |
|---|---|
| **AC-F1** | Every row of the `current-state-analysis.md` parity checklist passes, **or** is traceable to a DR-nn row or to C-9 (appearance) |
| **AC-F2** | The 6 Playwright flow specs (§8.3) pass against the new build using the **same spec files** that were used for capture |
| **AC-F3** | Request counts match §7's per-DR expectations exactly |
| **AC-F4** | The coverage description renders as **HTML** in the popover (an element, not escaped text) |
| **AC-F5** | The global spinner appears for **every** in-flight request and disappears at zero, on both success and error |
| **AC-F6** | No console error occurs during any of the 6 flows |

### Code shape

| # | Criterion |
|---|---|
| **AC-S1** | Every component sets `ChangeDetectionStrategy.OnPush`; none writes `standalone: true` |
| **AC-S2** | Zero `@Input` / `@Output` decorators; zero `*ngIf` / `*ngFor` / `ngClass` / `ngStyle`; every `@for` has a `track` |
| **AC-S3** | Zero `BehaviorSubject`; state is `signal()` / `computed()` |
| **AC-S4** | `tsconfig.json` has `strict: true`; `grep -rn ": any" frontend/src` returns zero hits |
| **AC-S5** | `app.config.ts` contains no `LOCALE_ID` provider and no `registerLocaleData` (DR-11) |
| **AC-S6** | `frontend/src/index.html` contains no `<base>` element and `angular.json` sets no `baseHref` |
| **AC-S7** | `frontend/src/theme.scss` is the only `.scss` file in the workspace |
| **AC-S8** | `bypassSecurityTrustHtml` appears exactly once, in `html-popover-panel.ts`, with a comment naming the Higson rules engine as the trusted source |

---

## 11. Out of scope

Restated from `requirements.md` §5. Anything here appearing in the diff is scope creep.

- ⛔ **No changes to the Higson snapshot** (`src/main/higson/`) — no rule, function or decision-table
  edits. All pricing logic stays where it is (`standards/higson/rules-authoring.md`).
- ⛔ **No changes to `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh`, `docker/app.properties`.**
  The image continues to `COPY target/motor-insurance.war` built on the host; the frontend is never
  built inside the image.
- ⛔ **No router.** Single non-routed screen; `ApplicationConfig.java`'s single `addViewController("/")`
  stays sufficient and unchanged (ND-7).
- ⛔ **No new REST endpoints and no changes to the existing 21** beyond what §6.4 sanctions.
- ⛔ No `SpringBootServletInitializer`, no external-Tomcat deployability (ND-8).
- ⛔ No CI pipeline (§8.8).
- ⛔ No changes to ports, context-path configuration, or the `all_in_one` profile.
- ⛔ No `.nvmrc`, no `frontend-maven-plugin` (C-5).
- ⛔ No currency symbol, no field validation, no zip-code mask, no variant selection, no copy changes
  (ND-2, ND-4, ND-5, DR-15).

**Deferred to step 2** (record, do not do now): removing the remaining EOL debt; `<html lang="pl">` vs
English UI copy (§13); `frontend-maven-plugin` when CI arrives; `@angular/cdk/layout` if a component
ever needs to react to breakpoints in TypeScript.

---

## 12. Standards compliance

Applicable standards from `.maister/docs/INDEX.md`, and how this specification satisfies each.

| Standard | How this spec complies |
|---|---|
| `frontend/build.md` — build wired into `prepare-package` | §5.1(1): the `prepare-package` binding is unchanged; only `<workingDirectory>` is added |
| `frontend/build.md` — `npm ci`, never `npm install` | §5.2: `"build": "npm ci && ng build"` |
| `frontend/build.md` — generated/vendored assets never committed | §5.4: `frontend/dist/`, `frontend/.angular/` ignored; `styles.min.css` untracked (DR-21). Documented exception: the one self-hosted Manrope font file (C-10) |
| `frontend/build.md` — exact pins for framework-critical packages | §5.2, AC-B8 |
| `frontend/services.md` — HTTP only in `*Service` | §4.6, §6.2, AC-C5. ⚠️ The *"callers resolve `response.data` themselves"* clause must be **deleted** from the standard (§6.2) |
| `frontend/services.md` — relative URLs, no leading slash | §5.6, §6.2, AC-C4 |
| `frontend/css.md` — LESS, single entry point, never hand-edit generated CSS | §4.9. Documented exception: `theme.scss` (Material's theming API is Sass-only) |
| `frontend/components.md` | Its four project rules (IIFE, `controllerAs`, `templateUrl`, `$emit` relay) are all superseded. `components.md:3` already scopes them to legacy code, so this is an update, not a conflict |
| `frontend/accessibility.md` | Material gives semantic roles and labels for free; the Playwright selector policy (§8.3) *depends* on accessible names, so a11y regressions surface as failing tests. Open item: `lang="pl"` (§13) |
| `frontend/responsive.md` | §4.9: the `992px` collapse point is preserved |
| `global/minimal-implementation.md` | §4.9 (no `BreakpointObserver`), §4.9 (no Material Icons font), §4.10 (DR-06 narrow), §11 |
| `global/coding-style.md` | Tab indentation, UTF-8; Java kept deliberately simple (only one backend file changes) |
| `global/conventions.md` | ⚠️ "Required Local Toolchain Baseline" says only *"NodeJS"* — that vagueness is what let Node 23.6.1 onto this machine. Must name a Node floor (Phase 8 doc task) |
| `global/security.md` | No secret is introduced. ⚠️ **`pom.xml` is edited by this migration** — before committing it, verify the Studio token property (`pom.xml:31`) is still empty. It is empty in the working tree today |
| `backend/api.md`, `backend/models.md` | §6.4: `ErrorDto` is a mutable POJO in `api/dto/`; controllers keep the `*Api` suffix; no records, no Lombok |
| `backend/spring.md` | `@RestControllerAdvice` centralises error handling at the boundary, which is exactly what the standard prescribes |
| `testing/test-writing.md` | §8.2: JUnit 4 dropped, `spring-boot-starter-test` added; package-private classes, behaviour-describing names, assertion messages, static `@BeforeAll`, `<ClassUnderTest>Test` layout |
| `testing/rule-tests.md` | §8.2: tests boot off `${higson.snapshot.dir}` via `HigsonSnapshotDataSource`; the snapshot is never copied into test resources. No rule test is added — no business rule changes |
| `higson/snapshot.md` | The snapshot is untouched (§11). P-2 records that a fresh clone needs `mvn higson-cli:pull` |
| `higson/integration.md` | No adapter, context or Higson-property change |
| `deployment/packaging.md` | §5.1: `finalName` and the `all_in_one` profile untouched; AC-B2/AC-B3 |
| `deployment/docker.md` | §5.1(4), AC-B7, AC-B9: no Docker change, port 48080 preserved, no Node in the runtime image |
| `global/documentation.md` | Phase 8 updates `INDEX.md`, `tech-stack.md` and `architecture.md` (Polish), plus the 6 frontend standards (English) |

---

## 13. Known limitations and open items

1. **`<html lang="pl">` with English UI copy.** Current behaviour (`index.html:2`); all visible copy is
   English. It is an accessibility defect but it is **not on the defect register**, so changing it
   would be an unsanctioned deviation. Carried forward as-is; recommend adding it to the register in
   step 2. Note that DR-11 depends on `LOCALE_ID` staying at Angular's `en-US` default regardless of
   the `lang` attribute — the two are independent.
2. **`theme.scss` splits the styling language.** Material's theming API is Sass-only, so the brand
   tokens live in Sass while everything else is LESS. Mitigation: `theme.scss` is the single seed and
   LESS consumes `var(--mat-sys-*)` where a token exists. Documented in §4.9 and §12 as a deliberate,
   scoped exception to `css.md`.
3. **`data-testid` attributes must be added to the AngularJS templates before capture** (§8.3) for the
   elements with no accessible name. This is an additive, capture-only change to files that are about
   to be deleted, but it does mean the "golden captured against an untouched build" claim is precisely
   "against a build touched only by additive test hooks". Record the commit hash.
4. **The Material Icons font is deliberately not added** (§4.9). If a later step introduces `<mat-icon>`
   the font must be self-hosted then, per C-10 — not pulled from a CDN.
5. **CI does not exist and is not being created** (§8.8). Every acceptance check in §10 is run manually.
   Treat any future claim that "the tests pass" with the scepticism `test-writing.md` prescribes.
6. **DR-03 changes user-perceived responsiveness.** Moving the three numeric fields from keystroke to
   blur means the quote no longer updates while typing. This is the intended fix, but it is the most
   noticeable behavioural change in the migration and should be called out in the Phase 8 release note.
