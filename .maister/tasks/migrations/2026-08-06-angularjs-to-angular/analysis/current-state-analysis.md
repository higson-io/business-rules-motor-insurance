# Codebase Analysis Report — Current State (AngularJS → Angular Migration)

**Date**: 2026-08-06
**Task**: Rewrite the AngularJS 1.8.3 SPA (`src/main/webapp/`) to the latest Angular, preserving the single-repository layout and Maven WAR packaging (`motor-insurance.war`) with the frontend build wired into Maven `prepare-package`.
**Description**: First step must preserve current functionality 1:1 (functional parity, zero new features). Scope includes replacing the npm+Gulp pipeline with Angular CLI while keeping the Maven `prepare-package` hook, and reimplementing UI built on angular-ui-bootstrap / angular-tooltips / angular-spinner / Bootstrap 3.
**Analyzer**: maister:codebase-analyzer (4 Explore agents: File Discovery, Code Analysis, Context Discovery, Migration Target)

---

## Summary

The AngularJS application is **small and unusually uniform**: 920 lines of application JS+HTML across 13 JS files and 11 HTML templates, comprising 8 element directives, 4 `$http` service factories, one module definition, and a single non-routed screen. Every directive uses the identical shape (`restrict: 'E'` + `templateUrl` + isolate scope + `bindToController` + `controllerAs: 'ctrl'`), which maps almost mechanically onto standalone Angular components with `input()` signals. None of the genuinely hard AngularJS idioms are present — no router, no `$watch`, no `$rootScope`, no two-way `=` bindings, no link/compile/transclude/require, no DOM manipulation in application code, no jQuery, and no Bootstrap 3 JavaScript behaviours.

The risk is **not** in the component rewrite. It is concentrated in five places: (1) the four AngularJS-only third-party libraries with no drop-in successors (`angular-ui-bootstrap` datepicker being the largest single item), (2) the `<base href>` versus `server.servlet.context-path=/demo` coupling — which today works only by the accident that `index.html` has no `<base>` tag and all 21 API URLs are relative, and which is **contradicted between the packaged properties and `docker/app.properties`**, (3) an HTTP body-serialization difference where Angular's `HttpClient` would JSON-quote the six raw-string PUT bodies that AngularJS sends unquoted, (4) roughly a dozen latent defects and quirks that force explicit product rulings rather than mechanical porting, and (5) the total absence of any test or CI safety net, meaning a "1:1 parity" claim can only be evidence-based if the migration itself builds the harness first.

A toolchain blocker exists today: the machine runs **Node 23.6.1, which Angular 22 rejects outright**.

**Recommendation: straight rewrite, no ngUpgrade.** ngUpgrade remains supported in v22 but requires the AngularJS `.component()` API; all 8 components here use `.directive()`, so hybrid mode would demand a rewrite as a precondition to the rewrite.

---

## Files Identified

### Primary Files — AngularJS application source (all must be rewritten)

**Shell / bootstrap**

| File | Lines | Role |
|---|---|---|
| `src/main/webapp/index.html` | 38 | The only HTML page. `<html lang="pl" ng-app="higsonDemoApp">` (line 2), 5 `<link>` tags, 6 vendor `<script>` tags, 13 application `<script>` tags, `<body>` containing literally only `<main></main>` (line 14). **Has no `<base href>`** — see the context-path trap. |
| `src/main/webapp/app.js` | 5 | The entire module definition: `angular.module('higsonDemoApp', ['ui.bootstrap', '720kb.tooltips', 'ngLoadingSpinner'])`. No `.config()`, no `.run()`. |

**Directives (8) — all `restrict: 'E'`, all `templateUrl`, all `controllerAs: 'ctrl'`**

| JS file | Lines | Template | Lines | Scope | Behaviour |
|---|---|---|---|---|---|
| `components/main/main.js` | 26 | `main.html` | 23 | `{}` | Event bridge only. `$on('recalculate')` → `$broadcast('reloadQuote', data)`. Zero other logic. |
| `components/sidebar/sidebar.js` | 30 | `sidebar.html` | 42 | `{}` | Layout only. Holds `ctrl.sidebarHidden` + `toggleSidebar()` — **neither is bound in the template; dead code**. |
| `components/driver/driver.js` | 101 | `driver.html` | 89 | `{}` | Largest component. Loads driver on init; 7 independent field handlers, each calling `DriverService.*` then `$scope.$emit('recalculate')`. Uses `uib-datepicker-popup` + `uib-btn-radio`. |
| `components/driverHistory/driverHistory.js` | 58 | `driverHistory.html` | 27 | `{}` | Independently re-fetches the same `GET driver` (duplicate startup call). 3 numeric fields → 3 handlers → emit. |
| `components/vehicle/vehicle.js` | 117 | `vehicle.html` | 42 | `{}` | Most complex logic. 4-level cascading dropdown with dependent-field clearing and conditional dictionary preloading. |
| `components/variants/variants.js` | 42 | `variants.html` | 32 | `{}` | Listens for `reloadQuote` → `VariantService.getQuote()`. The only call in the app with an error branch. |
| `components/variants/coverage/coverage.js` | 28 | `coverage.html` | 18 | 6 × `'<'` | Presentational only, empty controller. Tooltip + `number:2` filter. |
| `components/variants/discount/discount.js` | 25 | `discount.html` | 15 | 3 × `'<'` | Presentational only, empty controller. One-time bindings. |

**Service factories (4) — all `$http`, all relative URLs, no leading slash**

| File | Lines | Contents |
|---|---|---|
| `components/service/driverService.js` | 66 | 1 GET + 10 PUT methods |
| `components/service/vehicleService.js` | 37 | 1 GET + 4 PUT methods |
| `components/service/dictionaryService.js` | 43 | 4 GET methods |
| `components/service/variantService.js` | 16 | 1 GET method |

**Verified total: 920 lines of JS + HTML** (`wc -l` over all 13 JS files and 11 HTML templates). Note for the record: an earlier ~700 figure counted JS only; the directive count is **8**, not 7 — an earlier count omitted one leaf presentational directive.

### Primary Files — Build & packaging

| File | Lines | Role |
|---|---|---|
| `pom.xml` | 181 | `<packaging>war</packaging>`, `<finalName>motor-insurance</finalName>` (line 84), `exec-maven-plugin` frontend hook (lines 95–114), `maven-clean-plugin` filesets (lines 115–130), `all_in_one` profile (lines 158–178). **No `maven-war-plugin` block at all.** |
| `package.json` | 31 | Single script: `"build": "npm ci && gulp"` |
| `gulpfile.js` | 45 | `less` task + `prepare:libs` copy task; `watch` is broken (gulp 3 array syntax under gulp 4) |
| `package-lock.json` | ~298 KB | Committed; `npm ci` depends on it; regenerated wholesale |

### Primary Files — Java-side serving and context path

| File | Lines | Role |
|---|---|---|
| `src/main/java/pl/decerto/ApplicationConfig.java` | 20 | `WebMvcConfigurer`; only override is `addViewController("/").setViewName("forward:/index.html")` (lines 17–19). **No `addResourceHandlers`.** |
| `src/main/java/pl/decerto/Application.java` | 11 | Plain `SpringApplication.run(...)`. **Does not extend `SpringBootServletInitializer`.** |
| `src/main/resources/higson-demo-app.properties` | — | `server.servlet.context-path=/demo` (line 24), `server.port=48080` |
| `docker/app.properties` | 12 | `server.port=48080`, **no context-path** |

### Related Files — Styles (`less/`, at repo root, outside `src/main/webapp`)

| File | Lines | Notes |
|---|---|---|
| `less/styles.less` | 10 | Import manifest / build entry point |
| `less/variables.less` | 14 | 12 colour vars + `@border-radius: 8px` |
| `less/config.less` | 42 | **Highest-risk LESS file.** Imports `../node_modules/npm-font-open-sans/open-sans` and `../node_modules/font-awesome/less/font-awesome` by relative path into `node_modules`; sets `@FontPathOpenSans: "../fonts/open-sans"`; pulls Manrope from the Google Fonts CDN (line 4); styles the angular-tooltips DOM (`tooltip`/`tip`/`tip-arrow`, lines 32–55) |
| `less/common/common.less` | 143 | `.common-input-*`, `.common-btn-group`, `.common-tooltip`; `url('../imgs/arrow.svg')` line 56 |
| `less/main/main.less` | 69 | `.main`, `.spinner` (lines 6–12), webkit scrollbars (lines 14–32), `url('../imgs/logo-insurance-app.svg')` line 51 |
| `less/sidebar/sidebar.less` | 100 | Sidebar BEM + 4 icon backgrounds |
| `less/variants/variants.less` | 83 | Variant card styling incl. hover (lines 11–27) |
| `less/variants/coverage.less` | 34 | `.coverage__divider` unused |
| `less/variants/discounts.less` | 32 | |
| `less/dropdown/dropdown-menu.less` | 72 | Full re-skin of Bootstrap 3 `.dropdown-menu` used by the uib datepicker popup |
| `src/main/webapp/styles/styles.min.css` | 2946 | **Generated by Gulp but committed to git**, and not actually minified. Contradicts `css.md`. |

All `url()` paths are `../imgs/...` and `../fonts/...`, relative to the `styles/` output directory. Angular CLI's `stylePreprocessorOptions` and asset hashing change this — every path must be re-anchored.

### Related Files — Static assets

- `src/main/webapp/imgs/`: `logo-insurance-app.svg` (53 KB), `favicon.ico` (19 KB), `person.svg`, `vehicle.svg`, `doc.svg`, `address.svg`, `arrow-tooltip.svg`, `arrow.svg`. **`logo.png` and `select-arrow.png` are unreferenced legacy** — candidates to drop.
- `src/main/webapp/fonts/`: **96 git-tracked files.** Font Awesome 4 (6 files) + Open Sans (10 weight/style folders × 5 formats = 90 files). These are vendored copies duplicating what `less/config.less` imports from `node_modules`.

### Related Files — Deployment & docs

`Dockerfile` (14), `docker-compose.yml` (58), `push-to-docker.sh` (10), `.gitignore` (11), `Readme.md`, and 6 frontend standards files under `.maister/docs/standards/frontend/`.

---

## Inventory of Files to Migrate / Replace / Delete

### REWRITE — becomes an Angular equivalent

| Current | Becomes |
|---|---|
| `src/main/webapp/index.html` | Angular `index.html` + `<app-root>`; resolve `<base href>` first |
| `src/main/webapp/app.js` | `bootstrapApplication()` + `app.config.ts` |
| `components/main/main.js` + `.html` | Root component — **must be renamed away from the `main` tag** (real HTML5 element); the event-relay logic disappears entirely |
| `components/sidebar/*` | Standalone component; **do not port `sidebarHidden`/`toggleSidebar()`** |
| `components/driver/*` | Standalone component (datepicker is the hard part) |
| `components/driverHistory/*` | Standalone component |
| `components/vehicle/*` | Standalone component (cascade is the hard part) |
| `components/variants/*` | Standalone component |
| `components/variants/coverage/*` | Standalone component with `input()` signals |
| `components/variants/discount/*` | Standalone component with `input()` signals |
| `components/service/driverService.js` | `@Injectable({providedIn:'root'}) DriverService` |
| `components/service/vehicleService.js` | `VehicleService` |
| `components/service/dictionaryService.js` | `DictionaryService` |
| `components/service/variantService.js` | `VariantService` |
| `less/**` (10 files) | Carry over near-mechanically; **`config.less` node_modules imports and all `url()` paths must be re-anchored** |

### REPLACE — build tooling

| Current | Replacement |
|---|---|
| `gulpfile.js` | `angular.json` (delete gulpfile) |
| `package.json` `"build": "npm ci && gulp"` | `"build": "npm ci && ng build --base-href=…"` |
| `package-lock.json` | Regenerated wholesale |
| `pom.xml` `exec-maven-plugin` | Same plugin, same `prepare-package` phase; add `<workingDirectory>` if the workspace moves |
| `pom.xml` `maven-clean-plugin` filesets | Replace `src/main/webapp/vendors/**` with `dist/`, `.angular/cache` |
| `pom.xml` (new) | **Add a `maven-war-plugin` block with `<webResources>` — does not exist today** |
| `.gitignore` | Remove `src/main/webapp/vendors/`; add `dist/`, `.angular/`, relocated `node_modules/` |

### DELETE

- `src/main/webapp/vendors/` concept entirely (`prepare:libs` Gulp task, all 7 vendored library copies)
- `src/main/webapp/styles/styles.min.css` (generated-but-committed; must stop being committed)
- `merge-stream`, `gulp`, `gulp-less`, `gulp-plumber`, `gulp-rename` npm dependencies
- `src/main/webapp/imgs/logo.png`, `src/main/webapp/imgs/select-arrow.png` (unreferenced)
- `sidebar.js` dead collapse feature + the `transition: 0.5s` / `opacity: 1` leftovers in `less/sidebar/sidebar.less:33-36`
- `$scope.selectedDate = new Date()` (`driver.js:33`, never bound)
- `priceBeforeDiscount` binding on `<discount>` (`variants.html:24`) — the field does not exist on `DiscountDto` and is never used in the template

### CARRY OVER unchanged

- `src/main/webapp/imgs/*.svg`, `favicon.ico` (relocate to `public/` or `src/assets/`)
- `src/main/webapp/fonts/**` (96 files) — pending an asset-pipeline decision
- `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh`, `docker/app.properties` — **verified: no changes required**
- `pom.xml` `<finalName>motor-insurance</finalName>`, `all_in_one` profile, `higson-cli-maven-plugin` — do not touch
- All Java under `src/main/java/` — **no backend change is required for parity**

### UPDATE — documentation

`Readme.md`, and 6 standards files: `standards/frontend/{build,components,services,css,responsive,accessibility}.md`, plus the frontend sections of `project/tech-stack.md` and `project/architecture.md`.

---

## REST Contract Table (frontend call → backend handler → DTO)

All SPA HTTP traffic lives in the 4 factories under `src/main/webapp/components/service/`. **21 calls across 5 controllers** (one controller never called).

| # | FE call (file:line) | Method + exact URL as written | Params / body | Backend handler (file:line) | Response |
|---|---|---|---|---|---|
| 1 | `dictionaryService.js:25` | GET `dictionaries/make` | query `productionYear` (number) | `api/DictionaryApi.java:27-30` `@GetMapping("/make")`, `@RequestParam("productionYear") Integer` | `List<DictionaryEntryDto>` `{code:String, name:String}` — code is make_id as String |
| 2 | `dictionaryService.js:17` | GET `dictionaries/model` | query `typeId` | `DictionaryApi.java:32-35` | `List<DictionaryEntryDto>` (code=model_id, name=model_label) |
| 3 | `dictionaryService.js:33` | GET `dictionaries/productionYear` | none | `DictionaryApi.java:37-40` | `List<DictionaryEntryDto>` (code=name=year) |
| 4 | `dictionaryService.js:37` | GET `dictionaries/type` | query `makeId` | `DictionaryApi.java:42-45` | `List<DictionaryEntryDto>` (code=type_id, name=type) |
| 5 | `driverService.js:24` | GET `driver` | none | `api/DriverApi.java:28-32` `@GetMapping` | `DriverDto` |
| 6 | `driverService.js:40` | PUT `driver/birthDate` | JS `Date` → JSON string | `DriverApi.java:34-37` `@RequestBody Date` (**required**) | 200, empty body |
| 7 | `driverService.js:44` | PUT `driver/firstName` | **raw unquoted string** | `DriverApi.java:39-42` `@RequestBody(required=false) String` | 200 |
| 8 | `driverService.js:48` | PUT `driver/gender` | **raw string** `"M"`/`"F"` | `DriverApi.java:44-47` | 200 |
| 9 | `driverService.js:52` | PUT `driver/lastName` | **raw string** | `DriverApi.java:49-52` | 200 |
| 10 | `driverService.js:56` | PUT `driver/accidentCount` | number | `DriverApi.java:54-57` `@RequestBody(required=false) Integer` | 200 |
| 11 | `driverService.js:60` | PUT `driver/trafficTicketsCount` | number | `DriverApi.java:59-62` | 200 |
| 12 | `driverService.js:64` | PUT `driver/licenceObtainedAtAge` | number | `DriverApi.java:64-67` | 200 |
| 13 | `driverService.js:28` | PUT `driver/address/zipCode` | **raw string** | `DriverApi.java:69-72` | 200 |
| 14 | `driverService.js:32` | PUT `driver/address/city` | **raw string** | `DriverApi.java:74-77` | 200 |
| 15 | `driverService.js:36` | PUT `driver/address/street` | **raw string** | `DriverApi.java:79-82` | 200 |
| 16 | `vehicleService.js:18` | GET `vehicle` | none | `api/VehicleApi.java:29-32` | `VehicleDto` |
| 17 | `vehicleService.js:23` | PUT `vehicle/make` | JSON `{makeId:int, make:String}` | `VehicleApi.java:43-50` `@RequestBody UpdateMakeDto` | 200; **server-side side effect: typeId=0, modelId=0** |
| 18 | `vehicleService.js:27` | PUT `vehicle/model` | number | `VehicleApi.java:52-56` `@RequestBody Integer` | 200 |
| 19 | `vehicleService.js:31` | PUT `vehicle/type` | number | `VehicleApi.java:58-63` | 200; side effect modelId=0 |
| 20 | `vehicleService.js:35` | PUT `vehicle/productionYear` | number | `VehicleApi.java:34-41` | 200; side effect makeId=typeId=modelId=0 |
| 21 | `variantService.js:14` | GET `quote` | none | `api/QuoteApi.java:30-35` — **runs `motor.calculate(quote)` (full rules evaluation + validation) on every GET** | `QuoteDto` or 400 |

**Never called by the SPA**: `api/ProfilersApi.java` — GET `/profilers/parameters`, `/functions`, `/jdbc` (return HTML strings), GET `/profilers/clear`. Out of migration scope.

### DTO shapes

- **`DriverDto`**: `{firstName:"John", lastName:"Potter", dateOfBirth:1123804800000, gender:"M", address:{zipCode:"77566", city:"Lake Jackson", street:"Allwood St"}, numberOfAccidents:0, numberOfTickets:0, licenceObtainedAtAge:18}` — ⚠️ **`dateOfBirth` is a NUMBER (epoch millis)**; no Jackson date configuration is present. `driver.js:53-59` re-wraps it with `new Date(...)`.
- **`VehicleDto`**: `{makeId, make, modelId, typeId, productionYear}` — all `int`, never null; **unset = 0**, which is what makes the client-side cascade guards work.
- **`DictionaryEntryDto[]`**: `[{code:"217", name:"TOYOTA"}]` — `code` is a **String**, which is why templates use `+(element.code)`. Backed by Higson decision tables `demo.motor.dict.*`.
- **`QuoteDto`**: `{options:[{code, order:1, premium, premiumBeforeDiscounts, coverages:[{position, code, name, limitLeft, limitRight, premium, description:"<html>"}], discounts:[{code, name, value, position}]}]}`. ⚠️ `coverage.description` is fed straight into `tooltip-template` as HTML — treat as **trusted HTML** on migration (`DomSanitizer` / `[innerHTML]`).

### ⚠️ CRITICAL: relative URLs and the context path

All 21 URLs are relative **with no leading slash**, and `.maister/docs/standards/frontend/services.md` makes this a **binding rule**. They resolve against the document URL. `index.html` is served at the context root `/demo/` (`higson-demo-app.properties:24` + `ApplicationConfig.java:17-19`); Tomcat 302-redirects `/demo` → `/demo/` so the trailing slash is present.

Consequences:
- Any client-side route deeper than one segment (e.g. `/demo/quote/summary`) **silently rewrites every API URL**.
- With a correct `<base href="/demo/">`, `document.baseURI` resolution means **the entire existing relative-URL convention survives untouched** — a strong argument for keeping it.
- A dev-server proxy must forward `dictionaries`, `driver`, `vehicle`, `quote` and preserve the `/demo` prefix (or serve the app from `/demo/` in dev).

### ⚠️ CRITICAL: body-serialization trap (verify before claiming parity)

AngularJS `$http` only `JSON.stringify`s **objects**. For the six plain-string PUTs (#7, #8, #9, #13, #14, #15) it sends the **raw unquoted text** (`Warszawa`) with `Content-Type: application/json`; Spring accepts it because `StringHttpMessageConverter` wins over Jackson for a `String` target.

**Angular's `HttpClient` JSON-encodes strings by default, so it would send `"Warszawa"` including the quote characters**, and `StringHttpMessageConverter` would hand the controller a value containing the quotes — silently corrupting `city`, `street`, `zipCode`, `firstName`, `lastName`, and `gender` (a gender of `"M"` with quotes would stop matching the Higson rules). Fix by sending text bodies (`{headers:{'Content-Type':'text/plain'}}`) or a custom serializer. Numbers, the `Date`, and the `UpdateMakeDto` payloads are unaffected.

### ⚠️ Error contract is probably broken today

`variants.js:35-38` reads `error.data.message` on a failed `GET quote`. Server-side, `exceptions/GlobalExceptionHandler.java` is a plain `@ControllerAdvice` (**not** `@RestControllerAdvice`) whose handlers return `String` **without `@ResponseBody`** — Spring MVC interprets that as a **view name**, not a body. The messages from `example/QuoteValidator.java` ("Driver birth date not supplied.", "Driver first name not supplied.", "Driver gender not supplied.", "Driver address not supplied.", "Driver address zip code / city / street not supplied.", "Vehicle production year / make / type / model not supplied.") almost certainly never reach the client as `data.message`; what the browser gets is Spring Boot's default `/error` JSON, where `message` is suppressed by default (`server.error.include-message=never`). **Capture the actual bytes from the running app before asserting parity on the error banner.**

### Session semantics — the SPA is fully stateful

`service/QuoteService.java:10-12` is `@Service @SessionScope`, holding the whole `Quote` aggregate in `sessionQuote`, created once in `@PostConstruct initializeQuote()`. `DriverService` and `VehicleService` are singletons reading through it; every `@PutMapping` mutates that in-memory session object and returns nothing.

Implications: there is **no request-carried state at all** — the wizard is a sequence of side-effecting PUTs against a `JSESSIONID` cookie. Losing or splitting the session resets the quote to defaults. Two browser tabs share one quote; a hard reload does **not** reset it. Any Angular CLI `proxy.conf.json` must pass cookies through and must not introduce a second origin: **there is no CORS configuration anywhere** in `src/main/java` (grep for `@CrossOrigin`/`CorsRegistry` returns nothing), so same-origin proxying is the only zero-backend-change option. No auth, no CSRF, no Spring Security on the classpath.

---

## BEHAVIOURAL PARITY CHECKLIST

**This is the artefact Phase 6 verifies 1:1 against.** Every row is current, observed behaviour.

### 1. Main shell (`components/main/main.js`, `main.html`)

- [ ] `index.html:2` — `ng-app="higsonDemoApp"` on `<html lang="pl">`; `<body>` contains only `<main></main>` (`index.html:14`). ⚠️ The root component is registered as element `main`, an actual HTML5 tag — **in Angular this MUST be renamed** (`<app-root>`).
- [ ] Layout: Bootstrap 3 grid — `.container` (fixed 1440px at ≥1200px, `less/config.less:26-30`) → `.row` → `.col-md-3` (sidebar) + `.col-md-9` (menubar + variants).
- [ ] Menubar: 65px tall white bar, right-aligned, containing only the logo SVG (`imgs/logo-insurance-app.svg`, 206×38). No nav, no buttons, no interactivity.
- [ ] Global spinner: `<span class="spinner" us-spinner="{radius:30, width:8, length: 16}">` (`main.html:2`). Absolutely positioned `top:50%; left:56%; transform:translate(-50%,-50%); z-index:9999` (`less/main/main.less:6-12`). Shown whenever **any** `$http` request is in flight.
- [ ] Page background: diagonal blue gradient `rgba(76,76,222,1)` → `rgba(41,41,120,1)`, `min-height:100vh`, font Manrope 13px (`less/config.less:9-16`).
- [ ] The only logic in `mainCtrl`: relay `recalculate` (bubbling up) into `reloadQuote` (broadcasting down) — `main.js:21-23`.
- [ ] No routing, no URL state, no deep-linking, no page transitions. **Single screen.**

### 2. Sidebar (`components/sidebar/`)

- [ ] Header: title "Get a Quote" (24px, `#4C4CDE`, bold), subtitle "fill in the details to get a quote" (14px). Drop shadow underneath.
- [ ] Content area scrollable: `overflow:auto; height:83vh` (`less/sidebar/sidebar.less:28-31`), custom webkit scrollbar (6px, rounded, `#b5b5b5` thumb) from `less/main/main.less:14-32`.
- [ ] Three sections, each an icon+label divider followed by a widget: "Personal information" (`imgs/person.svg`) → `<driver>`; "Vehicle" (`imgs/vehicle.svg`) → `<vehicle>`; "Documents" (`imgs/doc.svg`) → `<driver-history>`. ⚠️ Note the label is "Documents" but the content is claim history.
- [ ] A fourth divider, "Address" (`imgs/address.svg`), is emitted from **inside `driver.html:55-60`**, not from the sidebar.
- [ ] ⚠️ **Dead code, do not port**: `ctrl.sidebarHidden = false` and `ctrl.toggleSidebar()` (`sidebar.js:21-27`) are never referenced in `sidebar.html`; the `transition:0.5s` / `opacity:1` rules (`less/sidebar/sidebar.less:33-36`) are leftovers of a removed collapse feature.

### 3. Driver data (`components/driver/`)

Loaded on init via `GET driver` (`driver.js:53-59`); `dateOfBirth` re-wrapped with `new Date(...)` because the backend serialises `java.util.Date` as epoch millis.

| Field | Control | Update trigger | Endpoint | Notes |
|---|---|---|---|---|
| First name | `input type=text` | `ng-model-options {updateOn:'blur'}` + `ng-change` | PUT `driver/firstName` | body = raw JSON string |
| Last name | `input type=text` | blur | PUT `driver/lastName` | |
| Date of birth | `uib-datepicker-popup="yyyy-MM-dd"` | ⚠️ **every valid keystroke** (no `updateOn:'blur'`) | PUT `driver/birthDate` | body = ISO-8601 string |
| Gender | two `uib-btn-radio` buttons | click | PUT `driver/gender` | values `'M'`/`'F'` |
| Zip code | `input type=text` | blur | PUT `driver/address/zipCode` | ⚠️ no mask, no pattern |
| City | `input type=text` | blur | PUT `driver/address/city` | |
| Street | `input type=text` | blur | PUT `driver/address/street` | |

- [ ] Every handler calls `.then(emitRecalculate)` → `$scope.$emit('recalculate')` (`driver.js:96-98`).
- [ ] Server-seeded defaults (`example/Motor.java` `createDriver`/`createAddress`): firstName **John**, lastName **Potter**, gender **M**, dateOfBirth **today − 40 years** (start-of-day, system TZ), licenceObtainedAtAge **18**, city **Lake Jackson**, street **Allwood St**, zipCode **77566**.
- [ ] Datepicker specifics (`driver.html:23-35`): display/parse format `yyyy-MM-dd`; alt typed format `['yyyy/MM/dd']` (`alt-input-formats`); `show-button-bar="false"` — **no Today/Clear/Close row**; `datepicker-options = {formatYear:'yyyy', startingDay:1 (Monday-first), showWeeks:false}` (`driver.js:35-39`); opened by clicking the calendar glyphicon → `ctrl.openPopup()` sets `$scope.popup.opened = true`; `is-open="popup.opened"` is a **two-way binding into `$scope`, not `ctrl`**; icon `.glyphicon.glyphicon-calendar` positioned `top:19px; right:13px` (`less/common/common.less` `.common-input-icon`).
- [ ] ⚠️ `$scope.selectedDate = new Date()` (`driver.js:33`) is **dead** — never bound.
- [ ] Gender button group (`driver.html:38-53`): both `ng-click` and `uib-btn-radio` on the same element. AngularJS orders directives **alphabetically at equal priority**, so `btnRadio` writes the model **first**, then `ngClick` reads it. **Angular must replicate this ordering explicitly** (set value, THEN fire the PUT). `uncheckable` is not set → re-clicking the already-active button leaves the model unchanged but **still fires `ng-click`** → redundant PUT + full recalculate. **PARITY DECISION NEEDED.** Active state = Bootstrap `.active` applied by `btnRadio`, styled `background:#f2f2f2` inside a pill-shaped `.common-btn-group`.
- [ ] Input chrome (all text inputs): 56px tall, `border-radius: 8px`, 1px `rgba(0,0,0,.38)` border, 20px bottom margin, floating 9px uppercase `#666` label absolutely positioned at `top:-5px; left:10px` on a white background (notched-outline look).
- [ ] ⚠️ **No validation is displayed anywhere.** No `required`, no `ng-pattern`, no error messages, no touched/dirty styling, **and no `<form>` element at all**.

### 4. Driver claim history (`components/driverHistory/`)

- [ ] ⚠️ Independently issues its **own** `GET driver` on init (`driverHistory.js:30-36`) — duplicating the driver component's fetch. **Two identical requests at startup.**

| Label | Control | Constraint | Trigger | Endpoint |
|---|---|---|---|---|
| "Number of accidents in the last 5 years" | `input type=number` | `min=0` | `ng-change`, **every keystroke** | PUT `driver/accidentCount` |
| "Number of traffic tickets in the last 5 years" | `input type=number` | `min=0` | every keystroke | PUT `driver/trafficTicketsCount` |
| "Driver's age when driver licence was obtained" | `input type=number` | `min=16` | every keystroke | PUT `driver/licenceObtainedAtAge` |

- [ ] Each calls `.then(emitRecalculate)`.
- [ ] ⚠️ **Behavioural landmine**: no debounce, no `updateOn:'blur'`. Typing "12" fires **two PUTs + two full quote recalculations**. Worse: when the value violates `min` or the field is cleared, `ng-model` becomes `undefined` → `$http.put(url, undefined)` sends **no body** → Spring binds null → `setNumberOfAccidents(null)` unboxes to `int` → **NPE / HTTP 500**. Because the failure path has no error handler, **the user sees nothing** and the quote silently does not refresh.

### 5. Vehicle data (`components/vehicle/`)

Four cascading `<select>`. Each uses `ng-options="+(element.code) as element.name for element in <dict>"` — the string `code` from the server is **coerced to a number with unary `+`** — plus a literal placeholder `<option value=''>` labelled with the field name.

| Change | Local state cleared | Dictionary reloaded | Server call | Emits recalculate? |
|---|---|---|---|---|
| Production year | makeId, make, typeId, modelId | make | PUT `vehicle/productionYear` (int) | ⚠️ **No** |
| Make | typeId, modelId | type | PUT `vehicle/make` (`{makeId, make}`) | ⚠️ **No** |
| Type | modelId | model | PUT `vehicle/type` (int) | ⚠️ **No** |
| Model | — | — | PUT `vehicle/model` (int) | ✅ **Yes** (`vehicle.js:76`) |

- [ ] Dictionary bootstrapping on load (`vehicle.js:36-50`): always `GET dictionaries/productionYear`; then conditionally `GET dictionaries/make?productionYear=`, `/type?makeId=`, `/model?typeId=`. The guards are `if (vehicle.productionYear)`, `if (vehicle.makeId)`, `if (vehicle.typeId)` — **they work only because the DTO uses primitive `int`, so unset values arrive as `0` (falsy)**.
- [ ] Server defaults (`Motor.createVehicle()`): productionYear **2010**, makeId **217**, make **TOYOTA**, typeId **28654**, modelId **218915**. The app opens fully populated; **all four dictionaries load at startup**.
- [ ] ⚠️ **Defect 1**: `vehicle.js:61-63` — `ctrl.makeDictionary.filter(...)[0].name` throws `TypeError` if the user selects the blank placeholder in Make (`makeId` becomes null, `filter` returns `[]`). No PUT, no recalc, uncaught exception. (Verified: `vehicle.js:63` is `})[0].name;`.)
- [ ] ⚠️ **Defect 2**: after a production-year change, `ctrl.typeDictionary` and `ctrl.modelDictionary` are **not** cleared — the Type/Model dropdowns keep stale option lists until the user picks a new Make.
- [ ] ⚠️ **Consequence of the emit asymmetry**: since only Model emits `recalculate`, the whole premium panel goes **stale** (still showing the previous vehicle's prices) from the moment the user touches Production Year until they re-pick a Model.
- [ ] Select chrome: same 56px / 8px-radius shell, `appearance:none`, custom `imgs/arrow.svg` chevron at `right .8rem bottom 1.4rem`, `cursor:pointer`.

### 6. Variants / options panel (`components/variants/`)

- [ ] Fetches `GET quote` on init and on every `reloadQuote` broadcast (`variants.js:23-39`).
- [ ] Binds `response.data.options` → `ctrl.variants`, rendered with `ng-repeat ... | orderBy: 'order'` (**verified `variants.html:5`** — valid string predicate, this one works).
- [ ] Each variant is a flex column card: 329px wide, white, 8px radius, 13px side margins, 1px `#ECECEC` border, `cursor:pointer`, on hover `box-shadow 3px 3px 5px rgba(0,0,0,.25)` + background `#EBEBFB` (`less/variants/variants.less:11-27`). ⚠️ **The hover/pointer styling implies selectability but there is no click handler — cards are not selectable.**
- [ ] Header = `{{variant.code}}` (26px bold, `#4C4CDE`, 50px bottom padding).
- [ ] Footer (pinned to bottom via `margin-top:auto`): 1px divider, then "Price before discounts" / `{{variant.premiumBeforeDiscounts}}` (**raw, unformatted**), then discount rows, then "FINAL PRICE" / `{{variant.premium}}` (24px bold blue, **raw, unformatted**).
- [ ] Error state: `ng-show="ctrl.errorOccured"` renders Bootstrap `.alert.alert-danger` with `{{ctrl.errorMessage}}` = `error.data.message` (`variants.js:35-38`). ⚠️ **On error `ctrl.variants` is NOT cleared, so stale variant cards remain visible underneath the red banner.**
- [ ] ⚠️ See the error-contract section — the message probably never arrives. Capture real bytes.

### 7. Coverage row (`components/variants/coverage/`)

- [ ] Pure presentational, isolate scope, all `'<'` one-way bindings: `code`, `name`, `description`, `limitLeft`, `limitRight`, `premium`.
- [ ] Renders: header `{{name}} ({{code}})` (14px, weight 600); left `{{limitLeft | number: 2}}` (12px); optional `<span ng-if="limitRight">/ {{limitRight | number: 2}}</span>`; right `{{premium | number: 2}}` (16px bold).
- [ ] ⚠️ **`| number: 2` uses AngularJS's default en-US locale despite `<html lang="pl">`** → `100000` renders as `100,000.00`. The Angular equivalent is `DecimalPipe '1.2-2'` with **en-US explicitly registered**, otherwise output differs.
- [ ] Tooltip (`coverage.html:1-6`): the whole row is `tooltips` with `tooltip-template="{{ctrl.description}}"` (interpolated **HTML** content), `tooltip-class="common-tooltip"` (min-width 340px), `tooltip-side="right"`, `tooltip-size="large"`. Styled in `less/config.less:32-55`: white body, 1px `rgba(76,76,222,1)` border, 15px padding, 4px radius, left-aligned, default border-arrow replaced by `imgs/arrow-tooltip.svg` (19×19).

### 8. Discount row (`components/variants/discount/`)

- [ ] Isolate scope `'<'` bindings: `name`, `value`, `priceBeforeDiscount`.
- [ ] ⚠️ `priceBeforeDiscount` is bound from `discount.priceBeforeDiscount` (`variants.html:24`) — **that field does not exist on `DiscountDto`** and is never used in the template. Dead binding; drop it.
- [ ] Renders **one `<table>` per discount** (repeated), each with an empty `<thead><tr></tr></thead>` and a single row: `{{::ctrl.name}}` | `-{{::ctrl.value}}`.
- [ ] ⚠️ The minus sign is a **hard-coded literal prefix**, and the value is **unformatted** (no `number` filter). Second column right-aligned, 16px, bold.
- [ ] Uses `::` one-time bindings — safe here because the directive is destroyed/recreated on each `ng-repeat` refresh.

### 9. Formatting summary (critical for pixel parity)

| Where | Formatting |
|---|---|
| Coverage limits & premium | `\| number: 2` → `1,234.50` (en-US) |
| Variant `premiumBeforeDiscounts` | **RAW** → `1234.5` |
| Variant `premium` (FINAL PRICE) | **RAW** → `1234.5` |
| Discount value | **RAW**, prefixed with literal `-` → `-100` |
| Date of birth | `yyyy-MM-dd` in the input; sent as ISO-8601; received as epoch millis |
| Currency symbol | **NONE ANYWHERE.** No currency filter is used. |

⚠️ The inconsistency (some values formatted, some not) is real and must be **reproduced deliberately or fixed deliberately** — see Open Decisions.

### 10. Loading / spinner behaviour

- [ ] **No per-component loading state anywhere** — no `ctrl.loading`, no disabled-while-saving, no optimistic UI.
- [ ] Single global mechanism: `ngLoadingSpinner` registers an `$httpProvider` interceptor that reference-counts in-flight requests and calls `usSpinnerService.spin('spinner-1')` / `.stop('spinner-1')`. `us-spinner` (`main.html:2`) is the spin.js render target.
- [ ] ⚠️ **Template fetches count too** — eight `templateUrl` XHRs at bootstrap make the spinner **flash on load**. This disappears in a compiled Angular build. Decide whether that flash is parity-relevant.
- [ ] ⚠️ Every keystroke in a number field toggles the spinner.
- [ ] Note: **`angular-loading-bar` is NOT used** despite appearing in some task descriptions.

### 11. Error handling

- [ ] ⚠️ **Exactly one error handler exists in the entire application** — `variants.js:35-38`. All 14 mutation calls have a **success callback only**. A failed PUT is **silently swallowed** — no message, no rollback, no recalculation. No `$exceptionHandler`, no global interceptor, no retry, no timeout, no cancellation.

### 12. Recalculation flow end-to-end (preserve exactly)

```
1. User blurs an input / picks a date / clicks a gender button / picks a Model
2. ng-change → ctrl.updateXxx()
3. XxxService.updateXxx(value) → $http.put(...)          [spinner ON via interceptor]
4. .then(emitRecalculate)                                 [ONLY ON 2xx — errors are swallowed]
5. $scope.$emit('recalculate')   driver/driverHistory/vehicle → sidebar → main
6. main: $scope.$broadcast('reloadQuote', undefined)      main → all descendants
7. variants: $on('reloadQuote') → reloadQuote()
8. VariantService.getQuote() → GET quote                  [spinner ON again]
9.   Backend: QuoteService.getQuote() (session-scoped) → Motor.calculate(quote)
     → QuoteValidator throws 400 on any missing field
     → per option: rebuildCoverages (IS_AVAILABLE / POSITION / DESCRIPTION),
       per coverage: LIMIT_1, LIMIT_2, PREMIUM from Higson rules,
       then DISCOUNTS (available / value / position), sorted by position
     → QuoteConverter: premium = Σ coverage premiums; premiumBeforeDiscounts = same;
       then premium −= each discount value
10. success → ctrl.variants = data.options; ctrl.errorOccured = false
    failure → ctrl.errorOccured = true; ctrl.errorMessage = error.data.message  (variants NOT cleared)
```

- [ ] ⚠️ **Preserve the emit asymmetry exactly**: `driver.js` and `driverHistory.js` emit after **every** field update; `vehicle.js` emits **only** after `updateVehicleModel` (`vehicle.js:76`, verified). Production year, make and type updates deliberately do **not** trigger a recalculation. Getting this wrong changes the number of `GET quote` calls and is **user-visible via the spinner**.
- [ ] The event payload `data` is threaded through (`main.js:21-22`) but is **always `undefined`** — every emitter calls `$emit('recalculate')` with no argument.

---

## AngularJS Idioms Requiring Rework

| # | Idiom | File:line | Angular replacement |
|---|---|---|---|
| 1 | `$scope.$emit('recalculate')` | `driver.js:97`, `driverHistory.js:54`, `vehicle.js:76` | Inject a `QuoteStore` service; `store.recalculate()` |
| 2 | `$scope.$on('recalculate')` + `$broadcast('reloadQuote', data)` relay | `main.js:21-23` | **DELETE.** Replaced by the store; `<main>` becomes pure layout |
| 3 | `$scope.$on('reloadQuote')` | `variants.js:24-26` | `effect()` / `httpResource()` reacting to a reload-trigger signal |
| 4 | `bindToController:true` + `controllerAs:'ctrl'` (×8) | all `*.js` | Component class; drop the `ctrl.` prefix in templates |
| 5 | `scope:{x:'<'}` one-way bindings | `coverage.js:12-19`, `discount.js:12-16` | `input<T>()` signals |
| 6 | `restrict:'E'` on the tag name `main` | `main.js:8` + `index.html:14` | ⚠️ **MUST RENAME** — `main` is a real HTML element. Use `app-root` |
| 7 | `templateUrl` (8 runtime XHRs) | all `*.js:9` | Compiled at build time — removes 8 network requests **and the startup spinner flash** |
| 8 | `ng-model-options="{updateOn:'blur'}"` | `driver.html:7,17,68,78,88` | `(blur)` handler, `ngModelOptions`, or Signal Forms blur commit |
| 9 | `ng-change` firing on **every keystroke** | `driverHistory.html:8,18,27`; `driver.html:28`; `vehicle.html:6,17,28,39` | `valueChanges` + `debounceTime`, or `(change)` — ⚠️ decide per field; **this changes request volume** |
| 10 | `ng-options="+(element.code) as element.name for element in ..."` (implicit string→number coercion) | `vehicle.html:7,18,29,40` | `@for` over options with `[value]`; coerce with `Number()` explicitly |
| 11 | Placeholder `<option value=''>` semantics (selectable, clears model) | `vehicle.html:8,19,30,41` | `<option [ngValue]="null">`; ⚠️ **must guard the `filter()[0].name` crash** (`vehicle.js:61-63`) |
| 12 | `uib-datepicker-popup="yyyy-MM-dd"` + `datepicker-options` + `is-open` + `alt-input-formats` + `show-button-bar` | `driver.html:23-32` | ⚠️ **LARGEST SINGLE REWORK.** No Angular equivalent ships in the framework — Angular Material Datepicker or NG Bootstrap `NgbDatepicker`; Monday-first, no week numbers, no button bar, `yyyy-MM-dd` + `yyyy/MM/dd` parse |
| 13 | `uib-btn-radio="'M'"/"'F'"` with implicit `.active` class + directive execution ordering vs `ng-click` | `driver.html:40-51` | Two `<button>`s with `[class.active]` and a click handler that **sets the value THEN PUTs** |
| 14 | `is-open="popup.opened"` — binding to `$scope` while everything else is on `ctrl` | `driver.html:29` + `driver.js:42,45-47` | A single component field / signal |
| 15 | `tooltips` + `tooltip-template="{{ctrl.description}}"` + `tooltip-side/size/class` (HTML content) | `coverage.html:1-6` | CDK Overlay / Material Tooltip (text only) or NG Bootstrap popover; description is HTML → needs `DomSanitizer` |
| 16 | `us-spinner="{radius:30,width:8,length:16}"` (spin.js DOM injection) | `main.html:2` | CSS spinner or Material `mat-progress-spinner` |
| 17 | `ngLoadingSpinner` global `$http` interceptor auto-counting requests | `app.js:4` | `HttpInterceptorFn` incrementing a `pendingRequests` signal. ⚠️ **Implicit behaviour — easy to miss; its absence is visible behaviour loss** |
| 18 | `\| number: 2` filter | `coverage.html:12,13,16` | `DecimalPipe '1.2-2'` — ⚠️ **must register en-US locale explicitly** |
| 19 | `\| orderBy: 'order'` (quoted → works) | `variants.html:5` (verified) | Sort in TS (`toSorted((a,b)=>a.order-b.order)`) — `orderBy` was removed from Angular |
| 20 | `\| orderBy: position` — **UNQUOTED → undefined predicate → identity → NO-OP**; the backend already sorts | `variants.html:8`, `variants.html:21` (verified) | ⚠️ Just render in array order. **Do NOT "fix" it into a real sort without an explicit ruling** |
| 21 | `::` one-time bindings | `discount.html:10,11` | Default Angular behaviour; drop the syntax |
| 22 | `ng-show` (keeps element, toggles display) | `variants.html:2` | `@if` **removes** the node — visually equivalent here, but note the semantic difference |
| 23 | `ng-if` | `coverage.html:13` | `@if` |
| 24 | `ng-repeat` (**no `track by` anywhere**) | `variants.html:5,8,21` | `@for (...; track ...)` — `track` is **mandatory**; use `variant.code`, `coverage.code`, `discount.code` |
| 25 | `$http` promise-based service factories | `components/service/*.js` | `HttpClient` + `firstValueFrom`, or `httpResource()`. ⚠️ HttpClient already unwraps the body — **every `.then(function(response){...response.data...})` needs unwrapping** |
| 26 | Global `ng-app` auto-bootstrap on `<html>` | `index.html:2` (verified) | `bootstrapApplication()` |
| 27 | No DI annotations (`$inject` / array form) — **not minification-safe today** | all `*.js` | `inject()` |

**Idioms explicitly searched for and confirmed ABSENT (good news):** `$rootScope`, `$scope.$watch`, `$timeout`, `$apply`, `ng-include`, `=` two-way bindings, custom `$parsers`/`$formatters`, link functions, `require`, `transclude`, `$compile`, ngRoute/ui-router, `angular-loading-bar`, any input-masking library, any `.config()`/`.run()`/`.constant()`/`.value()`/`.filter()`/`.component()`/`.provider()`/`.service()` block.

---

## Third-Party Replacement Map

| Current | Version | Used by | Replacement decision needed |
|---|---|---|---|
| `angular` | 1.8.3 exact | everything | Angular 22 |
| `angular-ui-bootstrap` (`ui.bootstrap`) | ^2.5.0 | `uib-datepicker-popup` (+ `show-button-bar`, `is-open`, `datepicker-options`, `close-text`, `alt-input-formats`), `uib-btn-radio` | ⚠️ **Largest single UI-parity risk.** Datepicker + radio-button-group must be rebuilt |
| `angular-tooltips` (`720kb.tooltips`) | 1.1.10 exact | `coverage.html`: `tooltips`, `tooltip-template`, `tooltip-class`, `tooltip-side="right"`, `tooltip-size="large"` | Tooltip directive / CDK overlay; `less/config.less` styles its internal `tooltip`/`tip`/`tip-arrow` elements — **DOM structure is coupled to the CSS** |
| `angular-loading-spinner` (`ngLoadingSpinner`) | 0.0.3 exact | global `$http` interceptor driving `us-spinner` | `HttpInterceptor` + signal-driven spinner. ⚠️ **Implicit behaviour.** Currently **not installed** in `node_modules` |
| `angular-spinner` / `spin.js` | 1.0.1 / 2.3.2 exact | `main.html:2` `us-spinner="{radius:30,width:8,length:16}"` | Spinner rendering; drop spin.js with it |
| `bootstrap` | ^3.3.7 | `.container`, `.row`, `.col-md-3`/`.col-md-9`, `.alert.alert-danger`, `.glyphicon-calendar`, `.dropdown-menu` | Grid + glyphicon + alert. ⚠️ **Glyphicons do not exist past BS3.** **Keeping BS3 CSS is the lowest-risk parity option** |
| `font-awesome` | ^4.7.0 | via `less/config.less` | FA4 class-name syntax |
| `npm-font-open-sans` | ^1.0.3 | via `less/config.less` | Self-hosted Open Sans (90 committed files) |
| Manrope (Google Fonts CDN) | — | `less/config.less:4`, the actual body font | ⚠️ Runtime CDN dependency — consider self-hosting |
| `merge-stream` | ^2.0.0 | only `gulpfile.js:5`; **misfiled as a runtime dependency** | Delete |
| `gulp` / `gulp-less` / `gulp-plumber` / `gulp-rename` | dev | build | Delete — Angular CLI has native LESS support |

**Bootstrap 3 JS behaviours used: NONE.** No modal, no collapse, no tabs, no carousel, no native BS dropdown JS. **jQuery is not loaded at all** — `ui-bootstrap-tpls` is pure AngularJS. This significantly de-risks the migration: only the CSS is needed, and only for the grid, one alert, one glyph, and the datepicker chrome.

---

## OPEN DECISIONS for the Orchestrator

Each of these is a latent defect or quirk in current behaviour requiring an explicit **"reproduce bug-for-bug" or "fix"** ruling. A bug-for-bug port and a clean port are different projects; these cannot be decided by the implementer.

| # | Issue | File:line | Impact if reproduced | Impact if fixed |
|---|---|---|---|---|
| **OD-1** | `orderBy: position` unquoted → undefined predicate → **coverages and discounts are NOT actually sorted today**; the backend already sorts by position | `variants.html:8`, `variants.html:21` | Render in raw array order (correct today because the backend sorts) | A "real" sort could **change rendering order** if backend order ever differs. **Recommendation: render in array order; do not introduce a sort.** |
| **OD-2** | Placeholder-option crash: `filter(...)[0].name` with no guard throws `TypeError` when Make is set to the blank option | `vehicle.js:61-63` | Uncaught exception, no PUT, no recalc | Guard + graceful clear. Changes observable behaviour (currently: nothing happens + console error) |
| **OD-3** | Empty/invalid number field → `$http.put(url, undefined)` → no body → Spring null → **NPE / HTTP 500**, silently swallowed | `driverHistory.html:8,18,27` | User sees nothing; quote silently stale | Client-side guard or backend null-tolerance. **This is a 500 in production today.** |
| **OD-4** | Keystroke-level PUT storms on the three numeric fields — no debounce, no blur | `driverHistory.js`, `driverHistory.html` | Reproducing means N PUTs + N full rules recalculations per typed number | Debouncing **reduces `GET quote` volume** — user-visible via the spinner, and a measurable deviation from parity |
| **OD-5** | All 14 mutation calls have **no error handler** — failed PUTs silently swallowed | all `*Service` consumers | No feedback, no rollback | Adding error handling is a **new feature** (violates "zero new features") |
| **OD-6** | On a failed `GET quote`, `ctrl.variants` is not cleared — stale cards remain **underneath** the red error banner | `variants.js:35-38` | Reproduce the stale-under-banner look | Clearing is arguably correct but changes the screen |
| **OD-7** | The error banner's `error.data.message` probably **never populates** because `GlobalExceptionHandler` returns a bare `String` from a plain `@ControllerAdvice` (view name, not body) | `exceptions/GlobalExceptionHandler.java`, `variants.js:35-38` | Reproduce an empty/default banner | Fixing requires a **backend change** — out of the stated frontend-only scope. ⚠️ **Capture the actual response bytes before deciding.** |
| **OD-8** | Gender button: `uncheckable` not set → re-clicking the active button fires a **redundant PUT + full recalculate** | `driver.html:40-51` | Must explicitly replicate AngularJS's alphabetical directive ordering (set value THEN PUT) | Suppressing the redundant call reduces request volume |
| **OD-9** | Duplicate `GET driver` at startup (driver + driverHistory each fetch independently) | `driver.js:53-59`, `driverHistory.js:30-36` | Two identical requests on every load | A shared store naturally deduplicates — **the idiomatic Angular rewrite fixes this by accident**. Must be an explicit ruling |
| **OD-10** | Formatting inconsistency: coverage values use `number:2`, variant premiums and discount values are raw; the minus sign is a hard-coded literal; no currency symbol anywhere | `coverage.html:12,13,16`; `variants.html`; `discount.html:11` | Pixel parity requires reproducing the inconsistency exactly, **including registering the en-US locale despite `lang="pl"`** | Consistent formatting is a visible UI change |
| **OD-11** | Vehicle emit asymmetry — production year / make / type do **not** trigger recalculation, leaving the premium panel visibly stale | `vehicle.js:76` | Preserve exactly (documented as intended in `components.md`) | Emitting on every change increases `GET quote` volume substantially |
| **OD-12** | Stale `typeDictionary` / `modelDictionary` after a production-year change | `vehicle.js:102-107` | Stale option lists remain selectable | Clearing them is arguably correct but changes the dropdown contents |
| **OD-13** | Spinner **flashes on load** because 8 `templateUrl` XHRs pass through the interceptor. A compiled Angular build has no template fetches | `main.html:2`, all `*.js:9` | Cannot be reproduced without artificial delay | Accept the flash disappearing (recommended — it is an artefact of the old loader) |
| **OD-14** | Variant cards have `cursor:pointer` + hover styling but **no click handler** | `less/variants/variants.less:11-27` | Keep the misleading affordance | Removing it is a visual change |
| **OD-15** | Sidebar label reads "Documents" while the content is claim history | `sidebar.html` | Keep the wrong label | Renaming is a copy change |
| **OD-16** | 96 committed font files + `src/main/webapp/styles/styles.min.css` (generated but committed, contradicting `css.md`) | `src/main/webapp/fonts/`, `styles/styles.min.css` | — | Asset-pipeline decision needed: self-host via `public/`, or npm + Angular CLI `assets` |
| **OD-17** | UI library strategy: **Bootstrap 3 → what?** `angular-ui-bootstrap`, `angular-tooltips`, `angular-spinner`/`spin.js` and `angular-loading-spinner` all have hard AngularJS 1.x dependencies and **no drop-in successor** | — | Hand-reimplement the tooltip, spinner and datepicker to match pixel-for-pixel | Adopting ng-bootstrap or Angular Material is a **visual change** and contradicts "zero new features / parity 1:1". ⚠️ **The single highest-impact open decision.** |
| **OD-18** | `<base href>` value: build-time `/demo/` versus `docker/app.properties` omitting the context path | `higson-demo-app.properties:24`, `docker/app.properties` | — | ⚠️ **Must be resolved empirically before the scaffold** — see Build Constraints |
| **OD-19** | Parity evidence strategy — there is no test suite and no CI | — | — | Decide whether golden-capture harnesses (MockMvc + Playwright + screenshot diff) are in scope **before** implementation, since they must be recorded against the current build first |

---

## Build / Packaging Constraints

### Maven

| Constraint | Detail | Action |
|---|---|---|
| `<packaging>war</packaging>` | Inherited lifecycle from `spring-boot-starter-parent:3.5.16` (pom lines 13–17) | Unchanged |
| `<finalName>motor-insurance</finalName>` | pom.xml line 84. Version-free and **load-bearing** — `Dockerfile:7`, `push-to-docker.sh` and `standards/deployment/packaging.md` depend on it | ⚠️ **DO NOT TOUCH** |
| **No `maven-war-plugin` block exists** | 100% inherited defaults: `warSourceDirectory = src/main/webapp`, no `warSourceExcludes`, no `webResources`, no `packagingExcludes` | ⚠️ **Must be ADDED** with `<webResources>` pointing at the Angular output |
| Frontend hook | `exec-maven-plugin` 3.0.0 (pom lines 95–114): `<executable>npm</executable>`, `arguments run build`, phase `prepare-package`. **No `<workingDirectory>`** → defaults to `${project.basedir}` | Phase **survives unchanged** (runs after `process-classes`/`test`, before `package`). Add `<workingDirectory>` if the workspace moves. ⚠️ `npm` is not Windows-safe (`npm.cmd`) — a pre-existing wart |
| Clean filesets | `maven-clean-plugin` 3.2.0 (pom lines 115–130) deletes `node_modules/**/*` and `src/main/webapp/vendors/**/*` (`followSymlinks=false`) | ⚠️ **Goes stale** — `vendors/` ceases to exist. Replace with `dist/`, `.angular/cache`; follow the relocated `node_modules` |
| `all_in_one` profile | pom lines 158–178: `spring-boot-maven-plugin` repackage with `<executable>true</executable>`. **The Dockerfile's `java -jar motor-insurance.war` requires it** | Orthogonal — unchanged. Release command stays `mvn clean package -Pall_in_one` |
| `higson-cli-maven-plugin` 4.3.1 | pom lines 143–153 | Entirely unaffected |
| Toolchain note | `java` on PATH is Temurin 21 while `maven.compiler.release=17`; Maven 3.8.2 | Not a blocker; recorded |

### ⚠️ base href vs the servlet context path — the single biggest hidden risk

`src/main/resources/higson-demo-app.properties:24` sets `server.servlet.context-path=/demo`, so the app is served at `http://host:48080/demo/`. **Verified: `index.html:2` has no `<base>` tag at all**, and every asset reference and all 21 API URLs are relative. It works today purely by that convention.

**Angular CLI emits `<base href="/">` by default**, which would resolve every asset to `http://host:48080/main-ABC123.js` — a 404.

Three viable resolutions:

1. **`ng build --base-href=/demo/`** (or `"baseHref":"/demo/"` in `angular.json`). Simplest. ⚠️ Hardcodes the servlet context path into the frontend build — if `server.servlet.context-path` is ever overridden via `${user.home}/conf/higson-demo-app.properties` (the documented two-tier override), the SPA silently breaks.
2. **`--base-href=./` plus `{provide: APP_BASE_HREF, useValue:'/demo/'}`**. Assets resolve context-relative; only the router needs the value.
3. **Server-side rewrite of `index.html`** — a filter or Thymeleaf injecting `<base href="${contextPath}/">` at request time. Most robust, most machinery.

**Recommendation: option 1 for the parity phase**, with a comment in `pom.xml`/`angular.json` tying the two together. Crucially, with `<base href="/demo/">` the relative `HttpClient` URLs resolve against `document.baseURI` → `/demo/quote`, so **the entire existing `services.md` URL convention survives untouched**.

⚠️ **UNRESOLVED CONTRADICTION (OD-18)**: `docker/app.properties` does **not** set `context-path`, so the containerised app may serve at `/`, and a build-time-baked `<base href="/demo/">` would then be wrong in Docker. `Readme.md:99` claims the docker app is at `[host]:48080/demo`. `HigsonIntegrationConfiguration.java:25` loads classpath first, then `${user.home}/conf/`. **Determine empirically which path wins in the container before choosing.**

### ⚠️ Output location — do NOT emit into `src/main/webapp/`

Angular 22's `application` builder emits `main-<hash>.js`, `styles-<hash>.css`, `chunk-<hash>.js` plus `index.html`. Emitting into `src/main/webapp/` would break three ways:

1. `src/main/webapp/` is **git-tracked** (`.gitignore` only excludes `src/main/webapp/vendors/`), so hashed output would be **committed** — a direct violation of `build.md`.
2. Hashed names are **never overwritten**, so stale `main-*.js` from every prior build **accumulates forever** and all of them get packed into the WAR — `mvn clean` does not touch `src/main/webapp`.
3. Angular's `index.html` would **clobber the source `index.html`**.

**Correct target: emit outside the source tree** (into `target/` or a git-ignored `dist/`) and pull it in via `maven-war-plugin` `<webResources>`. `target/` is already git-ignored and wiped by `mvn clean`, so hash accumulation solves itself.

### Recommended workspace layout: `frontend/` sibling

| | (a) Root workspace | **(b) `frontend/` sibling** | (c) `src/main/frontend/` |
|---|---|---|---|
| `angular.json` at | `/angular.json` | `/frontend/angular.json` | `/src/main/frontend/angular.json` |
| Angular `sourceRoot` | ⚠️ must be overridden — default `src/` **collides head-on with Maven's `src/main/java`** | `frontend/src` (CLI default) | `src/main/frontend/src` |
| `outputPath` | `target/frontend` | `frontend/dist/motor-insurance` | `target/frontend` |
| `exec-maven-plugin` change | none | add `<workingDirectory>${project.basedir}/frontend</workingDirectory>` | add `<workingDirectory>${project.basedir}/src/main/frontend</workingDirectory>` |
| IDE ergonomics | worst | **best** | good, but `node_modules` under `src/main/` upsets Maven resource scanners and IDE indexers |
| Alignment with existing docs | poor | moderate (needs a note in `architecture.md`) | strong |

**Recommendation: (b) `frontend/`.** Rationale: (1) the repo root **already** hosts the frontend today — `package.json`, `package-lock.json`, `gulpfile.js`, `less/`, `node_modules/` are all at root, **not** under `src/` — so moving them into `frontend/` is a tidying move, not a convention break; (2) option (a) is disqualified by the `src/` collision; (3) option (c) is the most Maven-pure and a defensible second choice, but burying a large `node_modules` and `.angular/cache` under `src/main/` invites accidental inclusion by resource filters; (4) (b) keeps the CLI on all defaults so `ng update`, `ng generate` and the Angular MCP `list_projects` work without special-casing.

Concrete wiring:

```xml
<plugin>
  <artifactId>maven-war-plugin</artifactId>
  <webResources>
    <resource><directory>${project.basedir}/frontend/dist/motor-insurance/browser</directory></resource>
  </webResources>
</plugin>
```

plus `<workingDirectory>${project.basedir}/frontend</workingDirectory>` inside the existing `exec-maven-plugin` `<configuration>` (pom.xml lines 99–105), and `ng build --base-href=/demo/`.

`src/main/webapp/` should be **emptied** (its `index.html`, `app.js`, `components/`, `styles/` all become Angular sources; `imgs/` and `fonts/` move to `frontend/public/`) but **kept** as the `warSourceDirectory` so nothing else in the inherited WAR lifecycle has to change.

### `.gitignore`

Current (11 lines): `node_modules/`, `src/main/webapp/vendors/`, `target`. ⚠️ `src/main/webapp/styles/styles.min.css` is **not ignored and IS committed** despite being generated.

Required changes: remove `src/main/webapp/vendors/`; add `frontend/node_modules/`, `frontend/dist/`, `frontend/.angular/`; stop tracking `src/main/webapp/styles/styles.min.css`. ⚠️ **No `.dockerignore` exists** — the build context currently ships `node_modules` when populated.

### Deep-link routing — a NEW concern

`ApplicationConfig.java:17-19` forwards **only** the exact root path. The current app has no router, so this is sufficient. If the rewrite introduces routed URLs, a browser refresh would **404**, requiring a catch-all forward or `HashLocationStrategy`. **For strict 1:1 parity, do not introduce a router** — `standards/global/minimal-implementation.md` (YAGNI) also argues against it — and this file then needs no change.

⚠️ Also do not "accidentally fix" the missing `SpringBootServletInitializer`: despite `packaging=war`, the artifact only runs via `java -jar` with `all_in_one` and is **not** deployable to an external Tomcat. That is current, intended behaviour.

### Verified non-issues

`Dockerfile` needs **no change** (only `COPY ./target/motor-insurance.war`; never sees Node). `all_in_one` / `spring-boot-maven-plugin` repackage is orthogonal. Higson snapshot and `higson-cli-maven-plugin` entirely unaffected. Port **48080** is consistent across properties, `EXPOSE`, and compose — `standards/deployment/docker.md` makes that a binding rule; keep it. LESS compiles natively under Angular CLI (`less/styles.less` becomes a `"styles"` entry in `angular.json`); Gulp is not needed for it.

---

## Toolchain Readiness Blockers

### ⚠️ BLOCKER: Node version rejected by Angular 22

```
node -v  →  v23.6.1     (/opt/homebrew/bin/node)
npm  -v  →  10.9.2
java     →  Temurin 21.0.5 LTS
mvn      →  3.8.2
```

Angular 22 engine range (live from the npm registry):

```
@angular/core   engines.node = "^22.22.3 || ^24.15.0 || >=26.0.0"
@angular/cli    engines.node = "^22.22.3 || ^24.15.0 || >=26.0.0"
                engines.npm  = "^6.11.0 || ^7.5.6 || >=8.0.0"
```

`npx semver -r "^22.22.3 || ^24.15.0 || >=26.0.0" 23.6.1` returns **empty** — **Node 23.6.1 is rejected.** Node 23 is an odd-numbered, non-LTS line that Angular deliberately excludes. npm 10.9.2 is fine.

**ACTION REQUIRED before any `ng` command**: install Node 22.22.3+ (LTS), 24.15.0+, or 26+. Node is installed via Homebrew, not nvm (`~/.nvm/versions/node` does not exist). Installing nvm/fnm **plus committing an `.nvmrc`** is the right fix, because the Maven build shells out to whatever `npm` is on `PATH` and will otherwise **fail opaquely inside `prepare-package`**. Consider `frontend-maven-plugin`, which pins and downloads Node — solving this for CI too, and fixing the Windows `npm.cmd` wart.

### Missing tooling — confirmed absent repo-wide

No `.ts` file anywhere (0 repo-wide). `mcp__angular__list_projects` returns `{"workspaces":[]}`. No `angular.json`, `tsconfig*.json`, `.editorconfig`, `.nvmrc`/`.node-version`, `engines` in `package.json`, `.npmrc`, `.browserslistrc`, `.babelrc`, webpack config, ESLint, Prettier, `karma.conf.js`, `protractor.conf.js`, `.dockerignore`. **This is a greenfield target — nothing to reconcile on the tooling side.**

### No partial migration exists

No ngUpgrade (`@angular/upgrade`, `downgradeComponent`, `UpgradeModule` all absent). No bundler at all. No ESM — `index.html` loads 13 classic `<script src=...>` tags (lines 17–36), all files IIFE + `'use strict'` ES5. No second frontend directory. `node_modules/` at root exists but is **empty**; `src/main/webapp/vendors/` is **empty** (gitignored + wiped by `mvn clean`). **A fresh checkout has no vendor JS until `npm run build` runs.** No branch contains Angular work (`master`, `techflash/5.8`, `bump/*`, `demo/starter`, `higson-4.3.0-springboot-3.5`, two dependabot branches).

### No dev server today

`gulp watch` is **broken** (gulp 3 array syntax under gulp 4). There is effectively **no dev server and no HMR today** — the dev loop is `mvn package` + full reload. Angular CLI's dev server is therefore a strict improvement, but it needs a `proxy.conf.json` that preserves the `/demo` prefix and passes the `JSESSIONID` cookie (see Session Semantics).

### Target platform requirements

| Item | Value | Source |
|---|---|---|
| Latest stable | **Angular 22.1.0** (`@angular/cli` 22.x, `@angular/build` 22.1.3); released 2026-06-03 | `npm view @angular/core dist-tags` (live registry) |
| Node.js | `^22.22.3 \|\| ^24.15.0 \|\| >=26.0.0` — **Node 20 support dropped** | `npm view @angular/core engines`; angular.dev/reference/versions |
| TypeScript | `>=6.0 <6.1` — **TS 6 required**, 5.9 and below unsupported | `npm view @angular/compiler-cli peerDependencies` |
| Standalone components | **Default and mandatory.** `standalone: true` must NOT be set (default since v20); NgModules are legacy | `mcp__angular__get_best_practices` |
| Signals | The prescribed state primitive. `input()`/`output()` functions replace decorators; `computed()` for derived state; OnPush-by-default in v22 | `mcp__angular__get_best_practices` |
| Control flow | `@if` / `@for` / `@switch` required; `*ngIf`/`*ngFor`/`ngClass`/`ngStyle` forbidden | `mcp__angular__get_best_practices` |
| DI | `inject()` function, not constructor injection; `providedIn: 'root'` | `mcp__angular__get_best_practices` |

### ngUpgrade assessment — supported, but wrong here

`@angular/upgrade@22.1.0` is still published, still shipped in lockstep, and **not deprecated** (`npm view @angular/upgrade deprecated` → empty); `angular.dev/api/upgrade/static/UpgradeModule` is live and documented for v22. **But it is the wrong tool for this codebase.** ngUpgrade exists to avoid a big-bang rewrite of a *large* app and carries a documented cost ("running both frameworks may result in more change detection cycles… performance degrades significantly in large hybrid apps"). This SPA is 8 components + 4 services + 1 module, ~13 JS files, no router, no forms library, no state library. The hybrid scaffolding (UpgradeModule, downgradeComponent, downgradeInjectable, dual bootstrap, shipping AngularJS 1.8.3 **plus** Angular 22 in the bundle) would cost more than rewriting all 12 artefacts. Decisively: **ngUpgrade requires the AngularJS `.component()` API, and all 8 components here use `.directive()` with `restrict:'E'`** — each would need converting to `.component()` first, i.e. a rewrite as a precondition to the rewrite.

**RECOMMENDATION: straight rewrite, no ngUpgrade.**

---

## Dependencies

### Imports (what the frontend depends on)

- **AngularJS runtime**: `angular` 1.8.3 + 3 module dependencies (`ui.bootstrap`, `720kb.tooltips`, `ngLoadingSpinner`) + transitive `angularSpinner`/`spin.js`
- **CSS**: `bootstrap` 3, `font-awesome` 4, `npm-font-open-sans`, Manrope from the Google Fonts CDN
- **Backend**: 5 REST roots (`dictionaries`, `driver`, `vehicle`, `quote`; `profilers` unused) served from the same origin under `/demo`
- **Build**: `npm` on `PATH` (unpinned), `gulp` 4 + 3 plugins, Maven `exec-maven-plugin`

### Consumers (what depends on the frontend)

- **`maven-war-plugin`** (inherited defaults) — packs everything under `src/main/webapp/` into the WAR root verbatim
- **`Dockerfile:7`** — `COPY ./target/motor-insurance.war`; depends on `<finalName>`
- **`push-to-docker.sh`** — hardcoded `VERSION=4.3.0`, multi-arch buildx
- **`docker-compose.yml`** — `web` service on 48080
- **`Readme.md`** — build/run instructions users follow
- **6 frontend standards files** + `project/tech-stack.md` + `project/architecture.md`

**Consumer count**: 6 build/deploy artefacts + 8 documentation files.
**Impact scope**: **Medium** — no other application code depends on the SPA's internals, but the packaging chain is tightly coupled through `<finalName>`, the `prepare-package` hook, and `src/main/webapp` being the `warSourceDirectory`.

---

## Test Coverage

### Test files

**There are none for the frontend, and effectively none overall.**

| Category | Status |
|---|---|
| Frontend tests | ⚠️ **ZERO.** No karma, jasmine, protractor, jest, vitest, playwright, cypress, `.spec.js`, `.test.js` anywhere (verified by repo-wide `find` excluding `node_modules`/`target`) |
| Tests tracked in git | ⚠️ **ZERO.** `HEAD` contains no test files. 7 test classes were deleted in commit `6a8b20c` (2025-04-28) |
| Untracked test | `src/test/java/pl/decerto/higson/demo/motor/SnapshotDataSourceDemoTest.java` — boots the Higson engine from the snapshot and asserts dictionary parameters load. Tests **the rules-engine data source** — not a single HTTP endpoint, not a single DTO, not a single line of frontend code. Imports JUnit 5 while `pom.xml` declares only `junit:junit:4.13.2` and no `spring-boot-starter-test` (documented conflict) |
| CI | ⚠️ **NONE.** No `.github/`, no `.gitlab-ci.yml`, no Jenkinsfile |
| Rule tests | 9 files in `src/main/higson/@tests/` — test pricing rules, not the API or UI, and live in a directory that is **not version-controlled** |

### Coverage assessment

- **Test count**: 0 relevant
- **Gaps**: everything
- **⚠️ Honest assessment for a 1:1 parity claim**: **there is no regression safety net whatsoever.** Nothing today would detect a changed URL, a changed request-body encoding, a lost session cookie, a dropped `updateOn:'blur'` debounce, a missing spinner, or a number rendered as `1 234,00` instead of `1,234.00`.

A parity claim can only be evidence-based if **the migration itself produces the harness**, and crucially **recorded against the current build first**:

1. **MockMvc / `@SpringBootTest` tests pinning all 21 request/response shapes** — including the raw-string body quirk — captured against the AngularJS build as golden.
2. **Playwright flows over the real stack** (`docker compose up` + the WAR) captured against AngularJS as golden, then replayed against the new app.
3. **Screenshot diffs**, since much of the "parity" here is visual.

⚠️ Any end-to-end run needs a **populated Higson snapshot** (`mvn higson-cli:pull`) — a fresh clone has no rules and the app returns nothing useful.

---

## Coding Patterns

### Naming conventions

- **Directives**: camelCase JS registration → kebab-case element (`driverHistory` → `<driver-history>`)
- **Files**: co-located, same-named `.js` + `.html` per component directory
- **Services**: `<Domain>Service` factories in a flat `components/service/` directory, no HTML
- **CSS**: consistent BEM (`.sidebar-container__widget`, `.coverage__divider`)
- **Backend**: `*Api` (never `*Controller`), `*Dto`, `*Adapter`, `*Service`

### Architecture patterns

- **Style**: ES5 IIFE + `'use strict'` per file; element directives with isolate scope; revealing-module object literals for services
- **State management**: **none on the client.** Server state is `@SessionScope`; the SPA is a thin remote-controlled view over a server-held quote. Every field edit is a discrete round-trip.
- **Cross-component communication**: a 2-hop `$emit`/`$broadcast` event bus with exactly 2 events, 11 emit sites, and 2 listeners
- **Palette**: primary `#4C4CDE`; borders `rgba(0,0,0,.38)` / `#ECECEC`; greys `#666` / `#f2f2f2` / `#333`; radius 8px

### Binding standards that constrain the target design

**Survive the migration (enforce them):**
- Frontend build wired into Maven `prepare-package` (`build.md`)
- **`npm ci`, never `npm install`** (`build.md`)
- Generated/vendored assets never committed (`build.md`) — ⚠️ **reinforced**: Angular's hashed output makes accidental commits far worse
- Exact pins for framework-critical packages (`build.md`) — Angular **requires** it: all `@angular/*` at the same version, `typescript` pinned `~6.0.x`
- HTTP only in `*Service` classes; **components never call HTTP directly** (`services.md` → `@Injectable({providedIn:'root'})` + `inject(HttpClient)`)
- **Relative URLs with no leading slash mirroring the `@RequestMapping` roots** (`services.md`) — ⚠️ **critical and load-bearing**; works identically with `HttpClient` given a correct base href. **Do NOT "modernize" to absolute paths**
- Never hand-edit generated stylesheets (`css.md`)
- All of `accessibility.md` — ⚠️ **strengthened** by Angular's official rule: "It MUST pass all AXE checks. It MUST follow all WCAG AA minimums"
- All of `responsive.md`
- WAR packaging with stable `finalName` (`packaging.md`); `all_in_one` before `docker build` (`conventions.md`); port 48080 consistency (`docker.md`)

**Must be rewritten (AngularJS/Gulp-specific):**
- IIFE + strict mode per file (`components.md`) → ES modules + TS strict; **delete**
- Element directives with `controllerAs`/`bindToController`/isolate scope → standalone `@Component` with `input()`/`output()` signals, OnPush, `host` object
- `angular.module` retrieval per file → `imports: [...]` on the standalone component
- ⚠️ **`recalculate` events relayed through `main` via `$emit`/`$broadcast` (`components.md`) — HIGHEST-RISK REWRITE ITEM.** The `$scope` event bus has no Angular equivalent; replace with a signal-based store service. **This is a genuine architectural decision, not a mechanical port.**
- `angular.factory` revealing-module literal + callers unwrap `response.data` (`services.md`) → ⚠️ **`HttpClient` already unwraps the body, so the "callers resolve `response.data` themselves" clause MUST be deleted from the standard or it will produce wrong code**
- LESS compiled by Gulp to `src/main/webapp/styles/styles.min.css` (`css.md`) → LESS survives; the Gulp `less` task, `gulp-plumber`, `gulp-rename` and the output path all die
- `prepare:libs` copying `node_modules/<lib>/**` into `vendors/` (`build.md`) → **deleted entirely**; the bundler resolves imports
- "Required Local Toolchain Baseline: Java 17, recent Maven, NodeJS" (`conventions.md`) → ⚠️ must name a **specific Node floor >= 22.22.3** backed by a committed `.nvmrc` — the current vague "NodeJS" is exactly what let a non-conforming Node 23.6.1 onto this machine
- Frontend sections of `tech-stack.md` (currently proposes "Angular 17+ / React 18+ / Vue 3+" and "Gulp → Vite/esbuild") → **supersede** with Angular 22 + the CLI's esbuild-based `application` builder
- The AngularJS SPA component/service inventory and `$emit`/`$broadcast` data-flow description in `architecture.md`

**New standards that should be created:**
- `standards/frontend/typescript.md` (strict: true, no `any`, prefer inference)
- `standards/frontend/state.md` (signals over `BehaviorSubject`; `computed()` for derived; never mutate — where the recalculate event-bus replacement gets documented)
- Extend `standards/frontend/build.md` with the **base href ↔ `server.servlet.context-path=/demo` coupling** — the single most likely thing to silently break a deployment

**Explicitly NOT binding**: `components.md` opens by stating the AngularJS standards are "NOT an endorsement of AngularJS for new work… do not use them as a template for greenfield frontends."

---

## Complexity Assessment

| Factor | Value | Level |
|---|---|---|
| File size | 920 lines JS+HTML across 13 JS + 11 HTML files; +10 LESS files (599 lines) | Low |
| Component count | 8 directives, 4 services, 1 module, 1 screen | Low |
| Dependencies | 10 runtime npm packages, 4 of them AngularJS-locked with **no drop-in successor** | High |
| Consumers | 6 build/deploy artefacts + 8 docs files | Medium |
| REST surface | 21 calls, 5 controllers, 4 DTO shapes | Low–Medium |
| Test coverage | **0 tests, 0 CI** | **High risk** |
| Toolchain readiness | Greenfield target + **an active Node-version blocker** | Medium |

### Overall: **Moderate** (leaning simple in code, high in decision load)

**Why not Simple:** (1) `uib-datepicker-popup` has no drop-in successor — reproducing `yyyy-MM-dd` display, `yyyy/MM/dd` alt-parse, Monday-first weeks, hidden week numbers, hidden button bar, external open-trigger via icon, and the fully re-skinned `.dropdown-menu` (72 lines) is the largest chunk. Budget 1–2 days. (2) `angular-tooltips` with HTML `tooltip-template` — Material/CDK tooltips are text-only; rendering `coverage.description` as HTML needs an overlay + sanitizer + custom SVG arrow. (3) **19 open decisions** force product rulings, not mechanical porting. (4) The formatting inconsistency is load-bearing for pixel parity. (5) Directive execution order on the gender buttons is implicit AngularJS behaviour that must be made explicit. (6) The base-href/context-path contradiction is unresolved. (7) Zero regression safety net.

**Why not Complex:** (1) 920 lines total; 8 components, 4 services, 21 endpoints, 1 screen. (2) Textbook-uniform directive shape mapping 1:1 onto standalone components with `input()` signals. No `link`, no `require`, no `transclude`, no `compile`, **no DOM manipulation in application code**. (3) Almost none of the hard AngularJS idioms are present. (4) The entire event graph is 2 events / 11 emit sites / 2 listeners — collapsible into one small store. (5) **No Bootstrap JS behaviours, no jQuery.** (6) The server holds all state — there is no client model to migrate. (7) LESS is well-organised BEM and ports near-mechanically. (8) No backend change is required for parity.

---

## Key Findings

### Strengths

- **Textbook-uniform directive shape** — the single biggest asset for the migration; every directive maps 1:1 onto a standalone component
- **Zero DOM manipulation in application code**; all DOM mutation is third-party (spin.js, angular-tooltips, uib-datepicker)
- **No jQuery, no Bootstrap 3 JavaScript** — only the CSS is needed, and only for the grid, one alert, one glyph, and the datepicker chrome
- **No router, no client state model** — two entire categories of migration risk simply do not exist
- **No backend change required** for functional parity
- Well-organised BEM LESS with a consistent palette
- `Dockerfile`, compose topology, `finalName`, port 48080 and the Higson plugin chain are **all verified unaffected**
- The `prepare-package` Maven hook and the relative-URL convention **both survive** the migration unchanged

### Concerns

- ⚠️ **Node 23.6.1 is rejected by Angular 22** — a hard blocker before any `ng` command, and it will fail **opaquely inside `prepare-package`**
- ⚠️ **base href vs context path** — build-time `/demo/` contradicts `docker/app.properties`, which omits the context path. Unresolved.
- ⚠️ **HttpClient would JSON-quote the six raw-string PUT bodies**, silently corrupting name/address/gender values and breaking Higson rule matching
- ⚠️ **No test suite, no CI** — a parity claim is unfalsifiable without building the harness first, against the *current* build
- ⚠️ **Four AngularJS-locked libraries with no drop-in successors**; adopting Material/ng-bootstrap is a visual change that contradicts "zero new features"
- ⚠️ **19 open decisions**, several of which (OD-17, OD-18, OD-19) must be settled *before* scaffolding
- ⚠️ Angular's hashed output would be **committed and accumulate forever** if emitted into `src/main/webapp/`
- ⚠️ `styles.min.css` is generated-but-committed today, already contradicting `css.md`
- ⚠️ Three genuine production defects exist today (OD-2 crash, OD-3 HTTP 500, OD-7 broken error contract) that a faithful port would preserve
- ⚠️ `maven-war-plugin` has **no configuration block at all** — one must be created from scratch

### Opportunities

- The idiomatic Angular rewrite **naturally eliminates** the duplicate `GET driver` (OD-9), the 8 `templateUrl` XHRs, and the startup spinner flash
- Compiled bundles remove the minification-safety problem (no `$inject` annotations exist today)
- `frontend/` relocation finally gets `less/`, `package.json` and `gulpfile.js` out of the repo root
- Angular CLI's dev server + proxy replaces a dev loop that currently has **no watch and no HMR** (`gulp watch` is broken)
- `.nvmrc` / `frontend-maven-plugin` fixes both the Node-version blocker and the Windows `npm.cmd` wart
- Dead code can be dropped: `sidebarHidden`/`toggleSidebar`, `selectedDate`, the `priceBeforeDiscount` binding, `logo.png`, `select-arrow.png`, `.common-checkbox`, `.coverage__divider`, `.common-subsection-heading`

---

## Impact Assessment

- **Primary changes**: all 24 files under `src/main/webapp/` (13 JS + 11 HTML), all 10 files under `less/`, `pom.xml`, `package.json`, `package-lock.json`, `gulpfile.js` (deleted), `.gitignore`
- **Related changes**: `Readme.md`, 6 frontend standards files, `project/tech-stack.md`, `project/architecture.md`
- **No change required**: all Java under `src/main/java/`, `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh`, `docker/app.properties`, all Higson snapshot tooling
- **Test updates**: the entire frontend test infrastructure must be **created from nothing**; golden captures must be recorded against the AngularJS build **before** it is removed

### Risk Level: **Medium-High**

The code volume is genuinely small and the component shape is unusually favourable, which caps implementation risk. Risk is elevated by four independent factors that are **not** proportional to code size: (1) the total absence of any regression safety net, making "1:1 parity" unverifiable by default; (2) two silent-breakage traps (base href / context path, and string body JSON-quoting) that produce **no error message** when wrong — the address and gender corruption in particular would surface only as changed Higson pricing; (3) four third-party libraries with no drop-in successors, one of which (the datepicker) is genuinely hard to reproduce exactly; and (4) a decision load of 19 open items that, if left to the implementer, would produce an arbitrary mixture of bug-for-bug and clean porting.

---

## Recommendations

This is a **rewrite of existing code with a hard functional-parity constraint**, not a new capability.

### Implementation strategy

**Phase 0 — Unblock and decide (must complete before any code):**
1. Install Node 22.22.3+ / 24.15.0+ / 26+; commit `.nvmrc`. Consider `frontend-maven-plugin` to pin Node for CI and fix Windows.
2. **Resolve OD-18 empirically**: run the container and determine whether the app serves at `/` or `/demo`. Everything about the build configuration depends on this.
3. **Settle OD-17** (UI library strategy) — this determines whether the datepicker/tooltip/spinner are hand-built or adopted, and it is the largest scope lever in the project.
4. **Settle OD-19** (parity evidence strategy) and, if in scope, **record golden captures against the current AngularJS build before touching it**.
5. Obtain rulings on the remaining open decisions, at minimum OD-1, OD-2, OD-3, OD-4, OD-8, OD-10, OD-11.

**Phase 1 — Scaffold and wiring (resolve packaging before writing components):**
- Create the `frontend/` workspace; `angular.json`, `tsconfig`, `package.json` `build` → `ng build`
- Add the `maven-war-plugin` block with `<webResources>`; add `<workingDirectory>` to `exec-maven-plugin`; update `maven-clean-plugin` filesets and `.gitignore`
- Emit **outside** `src/main/webapp/`; verify `mvn clean package -Pall_in_one` produces a working WAR and that the container serves correctly at the resolved context path

**Phase 2 — Services and store:**
- 4 factories → 4 `@Injectable`s; **unwrap `response.data` everywhere**
- ⚠️ **Handle the raw-string body encoding explicitly** and verify against the running backend
- Build the `QuoteStore` replacing the `$emit`/`$broadcast` relay — **preserving the emit asymmetry exactly**

**Phase 3 — Components, leaves first:** `coverage` and `discount` (pure presentational) → `variants` (error contract + OD-1) → `driverHistory` and `vehicle` (cascade) → `driver` minus the datepicker → the global spinner interceptor → **the datepicker (the real risk)** → the coverage tooltip with HTML content.

**Phase 4 — Shell:** `main` (renamed from the `main` tag) and `sidebar` become pure layout.

**Phase 5 — Styles:** LESS compiles as-is under Angular CLI, but `less/config.less`'s `../node_modules/...` imports and every `url('../imgs/...')` path must be re-anchored.

**Phase 6 — Verification:** replay the parity checklist above, section by section.

**Phase 7 — Docs:** `Readme.md` + 6 standards files + `tech-stack.md` + `architecture.md`.

### Backward compatibility

- **The REST contract must not change** — all 21 calls, their exact relative URLs, their body encodings, and the session-cookie behaviour
- **The WAR contract must not change** — `finalName`, port 48080, the `all_in_one` profile, the `prepare-package` hook
- **The Docker chain must not change**

### Testing requirements

Given zero existing coverage, the migration must produce its own evidence:
1. MockMvc/`@SpringBootTest` tests pinning all 21 request/response shapes — **including the raw-string body quirk** — recorded against the current build first
2. Playwright flows over the real stack, captured against AngularJS as golden, replayed against Angular
3. Screenshot diffs for the visual parity that dominates this checklist
4. ⚠️ Any end-to-end run needs `mvn higson-cli:pull` — a fresh clone has no rules

---

## Next Steps

1. **Orchestrator: resolve the 19 OPEN DECISIONS with the user**, prioritising OD-17 (UI library strategy), OD-18 (base href / context path), and OD-19 (parity evidence strategy) — these three gate the scaffold and materially change scope.
2. **Clear the Node blocker** before any Angular tooling is invoked.
3. **Invoke gap-analyzer** with this report plus the decision rulings to produce the current-vs-desired gap analysis.
4. Carry the **behavioural parity checklist** forward verbatim — Phase 6 verifies against it 1:1.
