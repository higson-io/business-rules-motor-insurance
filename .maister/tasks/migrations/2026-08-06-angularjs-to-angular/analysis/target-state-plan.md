# Target-State Plan & Gap Analysis — AngularJS 1.8.3 → Angular 22

**Date**: 2026-08-06
**Phase**: 2 (target-state planning / gap analysis)
**Inputs**: `analysis/current-state-analysis.md`, `analysis/clarifications.md` (C-1…C-4, OD-A…OD-F),
`.maister/docs/INDEX.md`, `project/tech-stack.md`, `project/architecture.md`,
`.maister/docs/standards/frontend/*.md`, `standards/deployment/*.md`, `standards/global/conventions.md`
**Constraint**: no source file was modified by this phase. The only write is this document.

---

## 0. Executive summary

The Phase 1 current-state record stands on structure, inventory and the REST contract. On **five
factual points it is wrong or unresolved, and all five change the plan.** Every correction below is
backed by evidence captured against a **running build of this codebase** (image
`decerto/higson-motor-insurance-demo:4.3.0`, Tomcat 10.1.55 == `spring-boot-dependencies` 3.5.16
`<tomcat.version>` — i.e. the image matches the working tree's Spring Boot version).

| # | Phase 1 claim | Verdict | Evidence |
|---|---|---|---|
| 1 | *"`HttpClient` JSON-encodes strings … would send `"Warszawa"` including the quote characters, silently corrupting city/street/zipCode/firstName/lastName/gender"* (`current-state-analysis.md:212`) | **FALSE** | `@angular/common@22.1.0` `HttpRequest.serializeBody()` line 541–543 returns a **string body verbatim**. The real divergence is only the `Content-Type` header. See §5.1. |
| 2 | *"docker/app.properties does not set context-path, so the containerised app **may serve at `/`**"* (`:491`, OD-18/OD-B) | **FALSE** | Throwaway container with no env override logs `Tomcat started on port 48080 (http) with context path '/demo'`. See §5.2. |
| 3 | *"`dateOfBirth` is a **NUMBER** (epoch millis)"* (`:194`) | **FALSE** | `GET driver` returns `"dateOfBirth": "1986-08-06T00:00:00.000+00:00"` — an ISO-8601 **string**. See §5.3. |
| 4 | *"`QuoteApi` … returns `QuoteDto` **or 400**"* (`:188`), *"the message probably never arrives"* (OD-7) | **PARTLY RIGHT, WRONG CODE** | A failed `GET quote` returns **HTTP 404**, body `{"timestamp":…,"status":404,"error":"Not Found","path":"/app/quote"}`. No `message` key ⇒ the red banner renders **empty**. See §5.4. |
| 5 | *"confirm whether the server already returns coverages/discounts sorted by position"* (C-3 mitigation, open) | **RESOLVED: it does** | All 3 options return coverage positions `[1,2,3]`, `[1,2,4,5,8]`, `[1,2,4,5,6,7,8,9]` and discount positions `[2]`, all ascending. See §5.5. |

The net effect is **risk down, not up**. The single scariest item in Phase 1 (silent address/gender
corruption changing Higson pricing) does not exist. The second scariest (base-href/context-path) has a
cleaner answer than Phase 1 proposed — and Phase 1's own recommendation (`--base-href=/demo/`) would
have **actively broken a container that is running on this machine right now** at context path `/app`.

**Recommended strategy**: **big-bang cutover on a branch, behind a mandatory golden-capture gate**
(§7). Dual-run is rejected on cost/benefit; phased-by-component is impossible without ngUpgrade, which
Phase 1 correctly ruled out and that ruling stands.

**Overall risk: MEDIUM** (Phase 1 said medium-high; corrections 1 and 2 remove the two silent-breakage
traps that justified the "-high").

---

## 1. Target system definition

### 1.1 Version pins — verified live against the npm registry (2026-08-06)

Every number below was read from the registry in this session, not carried over from Phase 1.

| Item | Pin | Registry evidence |
|---|---|---|
| `@angular/*` (core, common, compiler, compiler-cli, forms, platform-browser) | **22.1.0** | `npm view @angular/core dist-tags` → `"latest": "22.1.0"` |
| `@angular/cli`, `@angular/build` | **22.1.3** | `npm view @angular/cli@latest version` → `22.1.3` |
| `@angular/material`, `@angular/cdk` | **22.1.1** | `npm view @angular/material@latest` → `22.1.1`; peer `@angular/cdk: 22.1.1` (exact), `@angular/core: ^22.0.0 \|\| ^23.0.0` |
| **TypeScript** | **`~6.0.3`** ⚠️ | `@angular/compiler-cli@22.1.0` peer = `typescript: ">=6.0 <6.1"`. **`typescript@latest` is 7.0.2 — installing latest breaks the build.** Newest in-range release is `6.0.3`. |
| **Node** | **`^22.22.3 \|\| ^24.15.0 \|\| >=26.0.0`**; pin **24.19.0** in `.nvmrc` | `npm view @angular/core@latest engines`. Node 24.19.0 is the newest v24 (LTS) release. |
| npm | `^6.11.0 \|\| ^7.5.6 \|\| >=8.0.0` — local 10.9.2 already conforms | `npm view @angular/cli@latest engines` |
| `rxjs` | **`~7.8.2`** | `@angular/core@22.1.0` peer `rxjs: ^6.5.3 \|\| ^7.4.0`; newest 7.x is 7.8.2 |
| `zone.js` | **omit — go zoneless** | `@angular/core@22.1.0` `peerDependenciesMeta.zone.js.optional = true` |
| **`less`** | **`^4.2.0` — must be installed explicitly** ⚠️ | `@angular/build@22.1.3` `peerDependencies.less = "^4.2.0"`, `peerDependenciesMeta.less.optional = true`. **`@angular/build` bundles `sass` but NOT `less`** (`dependencies` contains `sass: 1.101.0`, `vite: 8.1.5`, `esbuild: 0.28.1` — no `less`). Newest is `4.8.1`; `^4.2.0` resolves there. |
| Test runner | **`vitest ^4.0.8`** (peer of `@angular/build`) — *not* Karma | `@angular/build@22.1.3` peers list both `karma ^6.4.0` and `vitest ^4.0.8`; Vitest is the v22 default |
| `@playwright/test` | **1.62.1** (engines `node >=20` — satisfied) | `npm view @playwright/test@latest` |
| `maven-war-plugin` | **do not pin** — managed at **3.4.0** by `spring-boot-starter-parent:3.5.16` | `spring-boot-dependencies-3.5.16.pom:144` `<maven-war-plugin.version>3.4.0</maven-war-plugin.version>`; parent `pluginManagement` also supplies an `<archive>` block (line 122–124) that **must not be clobbered** |
| `frontend-maven-plugin` (optional, §1.6) | **1.15.1** | Maven Central search |

> **Toolchain blocker is real and unchanged.** Local Node is **23.6.1** (`/opt/homebrew/bin/node`),
> which is outside every accepted range — Node 23 is an odd-numbered non-LTS line Angular excludes
> deliberately. There is **no nvm, no fnm, no volta** on this machine (`~/.nvm` absent,
> `command -v fnm` and `command -v volta` both empty). Java is Temurin **21.0.5** while
> `maven.compiler.release=17`; Maven is **3.8.2**. See OD-E in §8.

### 1.2 `frontend/` workspace tree (C-2)

Angular CLI defaults throughout — no `sourceRoot` override, so `ng generate` / `ng update` and the
Angular MCP `list_projects` work unmodified.

```
frontend/
├── .nvmrc                          # "24.19.0"
├── .gitignore                      # node_modules/, dist/, .angular/
├── angular.json
├── package.json                    # "build": "npm ci && ng build"
├── package-lock.json               # committed; regenerated wholesale
├── tsconfig.json / tsconfig.app.json / tsconfig.spec.json
├── proxy.conf.json                 # dev only; see §1.5
├── public/                         # copied verbatim into the bundle root
│   ├── favicon.ico
│   ├── imgs/{logo-insurance-app,person,vehicle,doc,address,arrow,arrow-tooltip}.svg
│   └── fonts/**                    # subject to OD-F (§8)
└── src/
    ├── index.html                  # NO <base> element — see §1.4
    ├── main.ts                     # bootstrapApplication(App, appConfig)
    ├── styles.less                 # global entry; replaces less/styles.less
    ├── styles/                     # the migrated LESS partials
    │   ├── _variables.less  _config.less
    │   ├── common/common.less
    │   ├── main/main.less
    │   ├── sidebar/sidebar.less
    │   └── variants/{variants,coverage,discounts}.less
    └── app/
        ├── app.ts / app.html / app.less          # root, selector "app-root"
        ├── app.config.ts                         # provideHttpClient(withInterceptors([...])),
        │                                         # provideZonelessChangeDetection(),
        │                                         # provideAnimationsAsync(), LOCALE_ID (§ DR-11)
        ├── core/
        │   ├── quote-store.ts                    # replaces $emit/$broadcast (signals)
        │   └── pending-requests.interceptor.ts   # replaces ngLoadingSpinner
        ├── models/
        │   ├── driver.ts  vehicle.ts  quote.ts  dictionary-entry.ts
        ├── services/                             # services.md: <Domain>Service, HTTP lives ONLY here
        │   ├── driver-service.ts      (class DriverService)
        │   ├── vehicle-service.ts     (class VehicleService)
        │   ├── dictionary-service.ts  (class DictionaryService)
        │   └── variant-service.ts     (class VariantService)
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

Notes:
- **File-naming follows the Angular v20+ CLI default (no `.component`/`.service` suffix on filenames)**,
  which preserves `components.md`'s "Component Files Co-Located and Same-Named" convention and the
  `variants/coverage`, `variants/discount` nesting. **Class names keep the `<Domain>Service` suffix**
  required by `services.md:8`.
- `main` is **renamed to `app-root`** (idiom #6). `<main>` is a real HTML5 element; reusing it as a
  component selector is the one naming change that is non-negotiable.
- All components: `changeDetection: OnPush`, `input()`/`output()` signals, `inject()`, `@if`/`@for`
  with mandatory `track`, `host` object instead of `@HostBinding`/`@HostListener` — per
  `mcp__angular__get_best_practices` (retrieved this session).

### 1.3 LESS wiring, build output, and where things land

- **LESS survives** (satisfies `css.md`'s "Consistent Methodology" and avoids a gratuitous SCSS
  rewrite). `angular.json` → `"styles": ["src/styles.less"]`, `"inlineStyleLanguage": "less"`, and
  `"stylePreprocessorOptions": { "includePaths": ["src/styles"] }`.
- **`less` must be an explicit `devDependency`** — see §1.1. Forgetting it is a build failure, not a
  silent one, so it is low-risk but easy to miss.
- `less/config.less:1,5` currently `@import`s **into `../node_modules/`** for `npm-font-open-sans` and
  `font-awesome`. Those become plain package imports resolved by the CLI (`~` prefix is no longer
  needed in v22; bare specifiers resolve via `includePaths`/node resolution).
- Every `url('../imgs/…')` / `../fonts/…` must be re-anchored: assets live in `frontend/public/`, are
  copied to the bundle root, and the CLI rewrites stylesheet `url()`s to hashed `media/*` references
  **relative to the emitted stylesheet**, which sits at the bundle root. Net: `url('imgs/arrow.svg')`.
- **Output**: `frontend/dist/motor-insurance/browser/` (CLI default `application` builder layout).
  This is **outside `src/main/webapp/`** — mandatory, for the three reasons Phase 1 gives at
  `current-state-analysis.md:497-499` (git-tracked dir, hash accumulation, `index.html` clobber). All
  three are correct and I endorse them.
- `src/main/webapp/` stays the `warSourceDirectory` but is **emptied** (keep the directory with a
  `.gitkeep` so the inherited WAR lifecycle sees a valid, if empty, source dir).

### 1.4 `<base href>` — the resolution (supersedes Phase 1's recommendation)

**Ship `index.html` with no `<base>` element at all.**

Rationale, from the evidence in §5.2: the context path is a **runtime** value that is already being
overridden in practice (`SERVER_SERVLET_CONTEXT_PATH=/app` on the container running on this machine).
Baking `/demo/` into the bundle at build time — Phase 1's "Recommendation: option 1"
(`current-state-analysis.md:489`) — would produce a bundle that 404s under that override, silently.

Mechanics: `ng new` scaffolds `<base href="/">` into `src/index.html`. **Delete that line and set no
`baseHref` in `angular.json`.** The CLI only rewrites/injects a `<base>` when `baseHref` is
configured; with it unset the authored `index.html` passes through untouched. The emitted
`<script src="main-HASH.js">` / `<link href="styles-HASH.css">` are already relative with no leading
slash, so they resolve against the document URL — **exactly the semantics that work today**
(`index.html:2` has no `<base>`, verified in Phase 1 and re-verified in §5.2).

This also means **`services.md`'s binding "relative URLs with no leading slash" rule survives verbatim**
and needs no exception: `http.get('quote')` resolves to `<contextPath>/quote` at any context path.
`--base-href=./` is an acceptable equivalent; `/demo/` is not. There is no router, so `APP_BASE_HREF`
is not needed.

### 1.5 Dev server

`ng serve` (port 4200) + `frontend/proxy.conf.json`. Two viable shapes; **option A is recommended**:

- **A — run the dev backend at the root context.** `SERVER_SERVLET_CONTEXT_PATH=/ mvn spring-boot:run`,
  then proxy `driver`, `vehicle`, `dictionaries`, `quote` 1:1 to `http://localhost:48080`. `JSESSIONID`
  gets `Path=/`, so cookies match with no rewriting.
- **B — keep `/demo` in dev.** `{"target": "http://localhost:48080/demo", "cookiePathRewrite": {"/demo": "/"}}`.
  ⚠️ **Without `cookiePathRewrite` the session silently breaks**: Tomcat sets `JSESSIONID; Path=/demo`,
  the browser is at `localhost:4200`, and the cookie is never sent back on `/driver` — every request
  gets a fresh session and the quote resets to defaults on every keystroke. Angular CLI passes proxy
  options straight to `http-proxy-middleware`, so `cookiePathRewrite` is available.

Either way the proxy must be **same-origin** — there is no CORS configuration anywhere in
`src/main/java` (Phase 1 verified; `@CrossOrigin`/`CorsRegistry` absent), and no Spring Security.
`ng serve` is **dev only**; it never participates in `mvn package`.

### 1.6 Exact `pom.xml` changes

Four edits. `<finalName>motor-insurance</finalName>` (line 84), the `higson-cli-maven-plugin` block
(lines 143–153) and the `all_in_one` profile (lines 158–178) are **untouched** — `packaging.md`
("Do not add the version to the artifact name") and the Higson snapshot standards both bind here.

**(1) `exec-maven-plugin` (lines 95–114) — add `<workingDirectory>`, one line:**

```xml
<configuration>
    <executable>npm</executable>
    <workingDirectory>${project.basedir}/frontend</workingDirectory>  <!-- NEW -->
    <arguments>
        <argument>run</argument>
        <argument>build</argument>
    </arguments>
</configuration>
```

The `prepare-package` binding (line 111) is **unchanged** — it already runs after `process-classes`
and `test` and before `package`, which is exactly what `maven-war-plugin` needs.

**(2) NEW `maven-war-plugin` block — does not exist today (100 % inherited defaults):**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-war-plugin</artifactId>
    <!-- version deliberately omitted: managed at 3.4.0 by spring-boot-starter-parent:3.5.16.
         The parent's pluginManagement <archive> config merges in; do not restate it. -->
    <configuration>
        <webResources>
            <resource>
                <directory>${project.basedir}/frontend/dist/motor-insurance/browser</directory>
            </resource>
        </webResources>
    </configuration>
</plugin>
```

Output lands at the **WAR root**, matching today's layout exactly — proven correct by the running
container's log line `WelcomePageHandlerMapping : Adding welcome page: ServletContext resource
[/index.html]` and by `GET /app/` returning 200 with the SPA `index.html`.
⚠️ `ApplicationConfig.java:17-19` (`addViewController("/") → forward:/index.html`) needs **no change**
and must not be "cleaned up" — it and the Spring Boot welcome-page mapping both target the WAR root.

**(3) `maven-clean-plugin` filesets (lines 115–135) — retarget both, add one:**

| Current | Becomes |
|---|---|
| `node_modules` | `frontend/node_modules` |
| `src/main/webapp/vendors` | `frontend/dist` |
| — | `frontend/.angular` (CLI build cache) |

**(4) Nothing else.** `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh`, `docker/app.properties`
are verified unaffected — they only ever reference `target/motor-insurance.war` and port 48080.
`docker.md`'s "JRE, not JDK" rule means **no Node in the runtime image**; the Angular build stays on
the Maven host, which is what `prepare-package` already does.

⚠️ **Ordering under `-Pall_in_one`**: `maven-war-plugin:war` runs in `package`; `spring-boot-maven-plugin:repackage`
runs after it on the produced WAR. So `webResources` lands *before* repackage and the executable WAR
ships the UI. This must be an explicit acceptance check (`packaging.md:13`), not an assumption.

### 1.7 `.gitignore`

Current is 11 lines (`target`, `.idea`, `.classpath`, `.vim`, `.project`, `.DS_Store`, `.settings/`,
`node_modules/`, `src/main/webapp/vendors/`, `*.iml`, `higson-cli*.log`).

| Action | Entry |
|---|---|
| REMOVE | `src/main/webapp/vendors/` (the directory ceases to exist) |
| KEEP | `node_modules/` — no leading slash, so it already matches `frontend/node_modules/` |
| ADD | `frontend/dist/` |
| ADD | `frontend/.angular/` |
| UNTRACK | `src/main/webapp/styles/styles.min.css` — generated-but-committed today, contradicting `css.md`; deleted outright with the rest of `src/main/webapp/` |
| CONSIDER | a `.dockerignore` (none exists; the build context currently ships `node_modules` when populated) |

`conventions.md` "Build Output, IDE Files and Tool Logs Stay Out of Version Control" is the binding
rule; `build.md` "Generated and Vendored Assets Are Never Committed" is reinforced by Angular's
content-hashed output, where an accidental commit accumulates forever.

### 1.8 Toolchain answer (OD-E preview — full proposal in §8)

Commit **`frontend/.nvmrc` = `24.19.0`** *and* add **`engines`** to `frontend/package.json`
(`"node": "^22.22.3 || ^24.15.0 || >=26.0.0"`). `.nvmrc` alone is advisory — the Maven build shells out
to whatever `npm` is on `PATH` and would otherwise **fail opaquely inside `prepare-package`**.
`engines` + `.npmrc` `engine-strict=true` converts that into a loud, early failure with the required
range printed. `frontend-maven-plugin` 1.15.1 is the stronger option (pins and downloads Node, fixes
the pre-existing Windows `npm` vs `npm.cmd` wart, and makes CI reproducible) but it **replaces**
`exec-maven-plugin` and therefore touches a rule in `build.md`; see OD-E.

---

## 2. Gap inventory

Effort: S ≤ 0.5 d · M ≈ 0.5–2 d · L > 2 d. Risk is residual risk *after* the stated mitigation.

### (a) Scaffolding & build wiring

| ID | Gap | Current → Target | Affected files | Eff | Risk | Depends |
|---|---|---|---|---|---|---|
| **G-A1** | Node toolchain non-conforming | Node 23.6.1 (rejected) → 24.19.0 + `.nvmrc` + `engines` | new `frontend/.nvmrc`, `frontend/package.json`, `standards/global/conventions.md` | S | **high** (hard blocker; fails opaquely inside `prepare-package`) | — |
| **G-A2** | No Angular workspace | none → `frontend/` on CLI defaults | `frontend/**` (new) | M | low | G-A1 |
| **G-A3** | No `maven-war-plugin` config | 100 % inherited → `<webResources>` at WAR root | `pom.xml` (new block) | S | med (silent: WAR builds fine but ships no UI) | G-A2 |
| **G-A4** | `exec-maven-plugin` runs at `${project.basedir}` | → `<workingDirectory>…/frontend` | `pom.xml:99-105` | S | low | G-A2 |
| **G-A5** | Clean filesets point at dead paths | `node_modules`, `src/main/webapp/vendors` → `frontend/{node_modules,dist,.angular}` | `pom.xml:115-135` | S | low | G-A2 |
| **G-A6** | `.gitignore` stale + generated CSS tracked | see §1.7 | `.gitignore`, `src/main/webapp/styles/styles.min.css` | S | low | G-A2 |
| **G-A7** | Gulp pipeline | `gulpfile.js` + 4 gulp deps + `merge-stream` + `vendors/` concept → deleted | `gulpfile.js`, `package.json`, `package-lock.json` (root, all deleted) | S | low | G-A2, **G-G1** |
| **G-A8** | No dev server (`gulp watch` broken, gulpfile.js:43-45) | → `ng serve` + `proxy.conf.json` | `frontend/proxy.conf.json` | S | med (session cookie path, §1.5) | G-A2 |
| **G-A9** | `all_in_one` not proven with the new asset path | → explicit `mvn clean package -Pall_in_one` + container check | acceptance only | S | med | G-A3 |
| **G-A10** | `less` not a declared dependency | → `less@^4.2.0` devDependency (`@angular/build` does **not** bundle it) | `frontend/package.json` | S | low (loud failure) | G-A2 |
| **G-A11** | Angular CLI scaffolds `^` ranges; `build.md` mandates exact pins for framework-critical packages | → exact-pin all `@angular/*`, `typescript`, `rxjs` | `frontend/package.json` | S | low, but **easy to miss** | G-A2 |

### (b) HTTP / service layer

| ID | Gap | Current → Target | Affected files | Eff | Risk | Depends |
|---|---|---|---|---|---|---|
| **G-B1** | 4 `$http` factories → 4 `@Injectable({providedIn:'root'})` | revealing-module literals → `inject(HttpClient)` | `services/*.ts` (4 new) | M | low | G-A2 |
| **G-B2** | Callers unwrap `response.data` (11 sites) | → `HttpClient` already unwraps the body | all components | S | **med — every `.then(r => r.data)` must be stripped; a missed one is `undefined`, not an error** | G-B1 |
| **G-B3** | String PUT bodies change `Content-Type` `application/json` → `text/plain` | Angular sends the string **verbatim** but relabels the header | `driver-service.ts` (6 endpoints) | S | **low — verified accepted, values uncorrupted (§5.1)** | G-B1 |
| **G-B4** | No typed models | untyped JS → `DriverDto`/`VehicleDto`/`QuoteDto`/`DictionaryEntryDto` interfaces; **`dateOfBirth: string`** (ISO-8601, §5.3), `DictionaryEntryDto.code: string` | `models/*.ts` (4 new) | S | low | G-A2 |
| **G-B5** | Relative-URL convention must survive | 21 URLs, no leading slash — **keep exactly** | `services/*.ts` | S | **high if violated** (§1.4) | G-B1 |
| **G-B6** | Query params by convention | `{params: {...}}` → `HttpParams` / `{params}` | `dictionary-service.ts` | S | low | G-B1 |

### (c) Components

| ID | Gap | Current → Target | Affected files | Eff | Risk | Depends |
|---|---|---|---|---|---|---|
| **G-C1** | `coverage` (6 × `'<'`) | → `input()` signals, `OnPush` | `components/variants/coverage/*` | S | low | G-A2 |
| **G-C2** | `discount` (3 × `'<'`, `::` bindings) | → `input()` signals; drop `::` | `components/variants/discount/*` | S | low | G-A2 |
| **G-C3** | `variants` — the only error branch in the app | `$on('reloadQuote')` → store `effect()`; `ng-show`→`@if`; `ng-repeat`→`@for … track` | `components/variants/*` | M | med (error contract, §5.4) | G-D1, G-B1 |
| **G-C4** | `driverHistory` (3 numeric fields) | → component; **DR-03/DR-04 apply** | `components/driver-history/*` | M | med (request volume changes) | G-D1, G-B1 |
| **G-C5** | `vehicle` — 4-level cascade, `ng-options` with `+()` coercion, placeholder `<option value=''>` | → `@for` + `[ngValue]`, explicit `Number()`; **DR-01/DR-02/DR-12 apply** | `components/vehicle/*` | **L** | med | G-D1, G-B1 |
| **G-C6** | `driver` minus the datepicker (7 fields, blur commit, gender group) | `ng-model-options {updateOn:'blur'}` → `(blur)`; `uib-btn-radio` → `MatButtonToggle` or 2 buttons + `[class.active]` | `components/driver/*` | M | med (DR-08 ordering) | G-D1, G-B1 |
| **G-C7** | **Datepicker** — `uib-datepicker-popup` | → `MatDatepicker`: `yyyy-MM-dd` display, `yyyy/MM/dd` alt-parse, Monday-first, no week numbers, no button bar, icon-triggered | `components/driver/*`, `app.config.ts` | **L** | **high — largest single item** | G-C6, G-F1 |
| **G-C8** | `main` shell (event relay) | → `app-root`, pure layout; **relay logic deleted entirely** | `app.{ts,html}` | S | low | G-D1 |
| **G-C9** | `sidebar` | → layout-only component; **do not port `sidebarHidden`/`toggleSidebar`** (DR-16) | `components/sidebar/*` | S | low | G-A2 |
| **G-C10** | Global spinner | `us-spinner` + spin.js → `MatProgressSpinner` driven by G-D2 | `components/spinner/*` | S | med (implicit behaviour, easy to lose) | G-D2, G-F1 |

### (d) State & event bus

| ID | Gap | Current → Target | Affected files | Eff | Risk | Depends |
|---|---|---|---|---|---|---|
| **G-D1** | `$emit('recalculate')` → `main` → `$broadcast('reloadQuote')`, 2 events / 11 emit sites / 2 listeners | → `QuoteStore` signal service; `store.recalculate()` bumps a trigger signal, `variants` reacts | `core/quote-store.ts` (new) + all emitters | M | **high — the one genuine architectural decision, not a mechanical port** | G-A2 |
| **G-D2** | `ngLoadingSpinner` `$httpProvider` interceptor reference-counting in-flight requests | → `HttpInterceptorFn` incrementing/decrementing a `pendingRequests` signal | `core/pending-requests.interceptor.ts` (new) | S | med (**implicit today; its absence is silent behaviour loss**) | G-A2 |
| **G-D3** | Emit asymmetry: only `vehicle.js:76` (model) triggers a reload | → **preserve exactly** unless DR-11 is accepted | `components/vehicle/*`, `core/quote-store.ts` | S | **high if lost — user-visible via spinner + `GET quote` count** | G-D1 |
| **G-D4** | No client state model (server holds everything in `@SessionScope`) | → keep it that way; **do not introduce a client-side quote model** | design constraint | S | med (scope creep) | — |

### (e) Styling & assets

| ID | Gap | Current → Target | Affected files | Eff | Risk | Depends |
|---|---|---|---|---|---|---|
| **G-E1** | `less/**` (10 files, 599 lines) at repo root | → `frontend/src/styles/**` + `src/styles.less` | all `less/*` | M | low | G-A2 |
| **G-E2** | `less/config.less:1,5` `@import '../node_modules/…'` | → package imports resolved by the CLI | `_config.less` | S | med (silent 404 on fonts) | G-E1 |
| **G-E3** | Every `url('../imgs/…')` / `../fonts/…` | → re-anchored to the bundle root | `common.less:56`, `main.less:51`, `_config.less:35`, `sidebar.less` | M | med | G-E1 |
| **G-E4** | `less/dropdown/dropdown-menu.less` (72 lines re-skinning BS3 `.dropdown-menu` for the uib datepicker) | → **obsolete, delete** (C-1) | delete | S | low | G-C7 |
| **G-E5** | `less/config.less:22-42` styles `tooltip`/`tip`/`tip-arrow` (angular-tooltips' internal DOM) | → **obsolete, delete**; restyle whatever OD-A picks | `_config.less` | S | med | G-C1, OD-A |
| **G-E6** | Bootstrap 3 CSS for `.container`/`.row`/`.col-md-3`/`.col-md-9`/`.alert-danger`/`.glyphicon-calendar` | → OD-C | `frontend/package.json`, `styles.less` | M | med | OD-C |
| **G-E7** | 96 committed font files + FA4 + Manrope from the Google Fonts CDN (`_config.less:4`) | → OD-F | `public/fonts/**` | M | low | OD-F |
| **G-E8** | `styles.min.css` generated-but-committed, not actually minified | → CLI-emitted, hashed, git-ignored | delete | S | low | G-A6 |
| **G-E9** | Material theming vs LESS tokens (`#4C4CDE`, radius `8px`, notched-outline 56 px inputs) | → OD-D | `styles.less` | M | med | OD-D |

### (f) Third-party UI replacement (C-1: Angular Material)

| ID | Gap | Current → Target | Eff | Risk | Depends |
|---|---|---|---|---|---|
| **G-F1** | No Material | → `@angular/material` + `@angular/cdk` 22.1.1, `provideAnimationsAsync()`, theme | M | med | G-A2 |
| **G-F2** | `uib-datepicker-popup` | → `MatDatepicker` (+ custom `DateAdapter` for `yyyy-MM-dd` / `yyyy/MM/dd`, `firstDayOfWeek = 1`) | **L** | **high** | G-F1, G-C7 |
| **G-F3** | `uib-btn-radio` (implicit `.active`, alphabetical directive ordering vs `ng-click`) | → `MatButtonToggleGroup`, or 2 plain buttons + `[class.active]` keeping the pill styling | M | med | G-F1 |
| **G-F4** | `angular-tooltips` with **HTML** `tooltip-template` | → OD-A (`MatTooltip` is **text-only**) | M | med | G-F1, OD-A |
| **G-F5** | `angular-spinner`/`spin.js` + `angular-loading-spinner` | → `MatProgressSpinner` + G-D2 | S | low | G-F1, G-D2 |
| **G-F6** | 10 AngularJS-era npm deps | → removed wholesale | S | low | G-A7 |

### (g) Test infrastructure (C-4) — **built from nothing**

| ID | Gap | Current → Target | Eff | Risk | Depends |
|---|---|---|---|---|---|
| **G-G1** | **Golden captures do not exist** | → MockMvc + Playwright + screenshots recorded **against the current AngularJS build, before any deletion** | **L** | **high — irreversible if skipped** | G-G2, G-G3 |
| **G-G2** | `pom.xml` declares `junit:junit:4.13.2`, no `spring-boot-starter-test`; the untracked `src/test/` uses JUnit 5 | → drop JUnit 4, add `spring-boot-starter-test`; keep `SnapshotDataSourceDemoTest` compiling | S | med | — |
| **G-G3** | No API contract tests | → MockMvc/`@SpringBootTest` pinning all 21 shapes **incl. the `text/plain` string bodies, the ISO-8601 `dateOfBirth`, and the 404 error contract** | **L** | med | G-G2 |
| **G-G4** | No E2E | → Playwright 1.62.1 over the real stack; **needs `mvn higson-cli:pull`** (a fresh clone has no rules) | **L** | **high** (env + snapshot dependency) | G-G1 |
| **G-G5** | No screenshot baseline | → captured now; downgraded to a **reviewed diff**, not a pass/fail gate (C-4) | M | med | G-G1 |
| **G-G6** | No frontend unit tests, no standard for them | → Vitest via `@angular/build`; **`mvn package` must NOT depend on a browser test runner** | M | med | G-A2 |
| **G-G7** | No CI anywhere | → out of scope for step 1; **state it explicitly** rather than implying a safety net exists | S | med | — |

### (h) Documentation & standards

| ID | Gap | Eff | Risk | Depends |
|---|---|---|---|---|
| **G-H1** | `build.md` — `prepare:libs` vendor procedure and the gulp entry point are obsoleted; the exact-pin rule now applies to `@angular/*` | S | low | G-A11 |
| **G-H2** | `css.md:8` — "Gulp compiles `less/styles.less` → `src/main/webapp/styles/styles.min.css`; never hand-edit `src/main/webapp/styles/`" is factually dead | S | low | G-E1 |
| **G-H3** | `components.md` — all four project rules (IIFE, `controllerAs:'ctrl'`, `templateUrl`, `$emit` relay) replaced. Note `components.md:3` already scopes them to legacy code, so this is an update, not a conflict | S | low | G-C* |
| **G-H4** | `services.md` — keep the two rules; **delete the "callers resolve `response.data` themselves" clause or it will generate wrong code** (G-B2) | S | **med** | G-B2 |
| **G-H5** | `conventions.md` "Required Local Toolchain Baseline" says only *"NodeJS"* — the vagueness is precisely what let Node 23.6.1 onto this machine | S | med | G-A1 |
| **G-H6** | `tech-stack.md` frontend sections + migration path (currently "Angular 17+ / React 18+ / Vue 3+", "Gulp → Vite/esbuild") → supersede with the actual outcome | M | low | all |
| **G-H7** | `architecture.md` — SPA component/service inventory and the `$emit`/`$broadcast` data-flow description | M | low | G-D1 |
| **G-H8** | `Readme.md` — build prerequisites (Node floor), commands | S | low | G-A1 |
| **G-H9** | NEW `standards/frontend/typescript.md` (strict, no `any`) | S | low | — |
| **G-H10** | NEW `standards/frontend/state.md` — where the `QuoteStore` replacement of the event bus is documented | S | low | G-D1 |
| **G-H11** | Extend `build.md` with the **base-href ↔ context-path** rule: *ship no `<base>`; the context path is a runtime value* (§1.4, §5.2) | S | **med — highest-value doc change** | G-A3 |
| **G-H12** | NEW frontend test standard (none exists; `test-writing.md` is Java/JUnit-only) | S | low | G-G6 |

**Totals**: 11 + 6 + 10 + 4 + 9 + 6 + 7 + 12 = **65 gaps**. Large items: G-C5, G-C7, G-F2, G-G1, G-G3, G-G4.

---

## 3. DEFECT REGISTER (required by C-3)

**This register is a deliverable, not a summary.** It is the *complete* list of sanctioned deviations
from the Phase 1 behavioural parity checklist. **Anything not on this register is a regression.**
Phase 6 verifies against `current-state-analysis.md`'s checklist **plus** this register.

Column **Δreq** = does the fix change backend request volume? **Δorder** = does it change rendering order?

### 3.1 Behavioural defects (DR-01 … DR-19)

| ID | Location | Current behaviour | New behaviour | Rationale | Δreq | Δorder |
|---|---|---|---|---|---|---|
| **DR-01** | `vehicle.js:61-63` (`})[0].name;`) | Selecting the blank placeholder in **Make** sets `makeId` to null; `filter(...)` returns `[]`; `[0].name` throws an uncaught `TypeError`. No PUT, no recalculation, nothing visible to the user. | Guard the lookup. On the blank option: clear `make`, `typeId`, `modelId` and their dictionaries, and **do not** issue `PUT vehicle/make`. | An uncaught exception mid-cascade leaves the UI in a state the user cannot see or recover from. | **YES** — removes a would-be PUT that never happened anyway (the throw preceded it); net 0 | no |
| **DR-02** | `driverHistory.html:8,18,27` + `driverService.js:55-65` | Clearing a numeric field or violating `min` makes `ng-model` `undefined` → `$http.put(url, undefined)` → **no body** → Spring binds null → `setNumberOfAccidents(null)` unboxes to `int` → **HTTP 500**, silently swallowed. **Verified live: `PUT /driver/accidentCount` with an empty body returns `{"status":500,"error":"Internal Server Error"}`.** | Do not send the request when the value is null/NaN/out-of-range. Leave the last valid server value in place. | A guaranteed HTTP 500 on a trivial user action, with zero feedback. | **YES** — eliminates the failing PUT and its swallowed 500 | no |
| **DR-03** | `driverHistory.html:8,18,27` (`ng-change`, no `updateOn:'blur'`) | Every keystroke in the 3 numeric fields fires a PUT **and** a full `GET quote` (a complete Higson rules evaluation). Typing "12" = 2 PUTs + 2 recalculations. | Commit on **blur** (matching the 7 text fields in `driver.html`), or debounce ~400 ms. **Recommend blur** — it reuses the pattern already present in the codebase rather than inventing a timing constant. | Keystroke-level rule evaluation is a defect, not a feature. | **YES — large reduction. Visible in the spinner and in `GET quote` counts. Playwright per-scenario request-count expectations must be rewritten for these flows.** | no |
| **DR-04** | `driver.html:23-32` (datepicker has no `updateOn:'blur'`) | The date field fires `ng-change` on **every valid keystroke** while typing a date, unlike the 6 sibling text fields which are blur-committed. | Commit on close/blur/valid-complete-date only. | Inconsistent with every other field in the same component; same rule-evaluation storm as DR-03. | **YES — reduction** | no |
| **DR-05** | `variants.html:8`, `variants.html:21` | `\| orderBy: position` — **unquoted**, so `position` evaluates against the scope, is `undefined`, and AngularJS treats it as an identity predicate. **Coverages and discounts are not sorted client-side at all today.** | Render in array order; **do not introduce a client-side sort.** | ⚠️ **Verified live**: the backend already returns them sorted by `position` — options BRONZE `[1,2,3]`, SILVER `[1,2,4,5,8]`, GOLD `[1,2,4,5,6,7,8,9]`, discounts `[2]`, all ascending. A client sort would be a no-op today and a *divergence* the day the backend order changes. Removing dead syntax is the fix. | no | **NO — verified identical output** |
| **DR-06** | all 14 mutation call sites (`driver.js`, `driverHistory.js`, `vehicle.js`) | Every mutation has a **success callback only**. A failed PUT is silently swallowed: no message, no rollback, no recalculation. | Add a single `HttpInterceptorFn` that surfaces mutation failures through the existing `.alert.alert-danger` banner. No rollback, no retry. | Silent failure is the root cause of DR-02's invisibility. ⚠️ **Scope flag**: this is arguably a *new capability*, which brushes `minimal-implementation.md` (YAGNI) and "zero new features". Reusing the banner that already exists keeps it minimal. **Needs an explicit ruling.** | no | no |
| **DR-07** | `variants.js:35-38` | On a failed `GET quote`, `ctrl.variants` is **not** cleared — stale variant cards stay visible *underneath* the red banner, indistinguishable from live prices. | Clear the variants when the quote fails. | Showing stale prices as if current is the most user-damaging defect in the list. | no | **YES — cards disappear on error instead of persisting** |
| **DR-08** | `driver.html:38-53` | Both `ng-click` and `uib-btn-radio` sit on the same element. AngularJS orders equal-priority directives **alphabetically**, so `btnRadio` writes the model *first*, then `ngClick` reads it. `uncheckable` is unset, so **re-clicking the already-active button fires a redundant PUT + full recalculation.** | Explicit handler: set the value, **then** PUT — **and skip entirely when the value is unchanged.** | Depending on alphabetical directive ordering is implicit behaviour that must be made explicit; the redundant round-trip is waste. | **YES — reduction (re-clicks stop generating traffic)** | no |
| **DR-09** | `driver.js:53-59` + `driverHistory.js:30-36` | `driver` and `driverHistory` each issue their **own** `GET driver` on init — **two identical requests at every page load**. | One fetch, shared through `QuoteStore`. | The idiomatic Angular rewrite deduplicates this *by accident*; it must be a recorded decision, not an accident. | **YES — 2 `GET driver` → 1 at startup** | no |
| **DR-10** | `variants.html:19,28`, `discount.html:11` vs `coverage.html:12,13,16` | Coverage limits/premium use `\| number: 2` (→ `1,234.50`); variant `premiumBeforeDiscounts` and `premium` are **raw** (→ `1234.5`); discount value is **raw** with a hard-coded `-` prefix; **no currency symbol anywhere**. | Apply `DecimalPipe '1.2-2'` **consistently** to all four money fields. Keep the `-` prefix on discounts; keep no currency symbol. | Two money values formatted and two not, in the same card, is a defect. Adding a currency symbol would be a new feature — explicitly out. | no | no |
| **DR-11** | `coverage.html:12,13,16` | `\| number: 2` uses AngularJS's built-in **en-US** locale despite `<html lang="pl">` → `100000` → `100,000.00`. | Register **en-US explicitly** (`LOCALE_ID` / `registerLocaleData`) so `DecimalPipe` reproduces the same output. | Angular's `DecimalPipe` defaults to the app locale; without an explicit en-US registration the same data renders `100 000,00`. This is a **no-op fix that prevents an accidental regression** — it belongs on the register because it looks like a change but is not. | no | no |
| **DR-12** | `vehicle.js:102-107` (`clearDependantsVehicleFields`) | After a Production-Year change, `ctrl.typeDictionary` and `ctrl.modelDictionary` are **not** cleared. The Type and Model dropdowns keep the *previous* vehicle's options and remain selectable. | Clear both dictionaries alongside the field values. | Offering options belonging to a make the user has just discarded is a correctness bug. | no | **YES — the two dropdowns become empty instead of stale** |
| **DR-13** | `vehicle.js:76` | Only `updateVehicleModel` emits `recalculate`. Production Year, Make and Type updates deliberately do **not**, so the premium panel goes **visibly stale** from the first vehicle edit until a Model is re-picked. | ⚠️ **KEEP AS-IS — this is NOT fixed.** | `components.md` documents the relay as intended behaviour, and the clarifications make preserving recalculation semantics binding. Emitting on every vehicle change would multiply `GET quote` volume (each one a full Higson evaluation). **Listed here so its retention is a recorded decision rather than an oversight.** | **no (deliberately unchanged)** | no |
| **DR-14** | `variants.js:35-38` + `exceptions/GlobalExceptionHandler.java` | `GlobalExceptionHandler` is a plain `@ControllerAdvice` whose handlers return `String` **without `@ResponseBody`** → Spring MVC treats the message as a **view name** → view resolution fails. **Verified live: `GET quote` with a missing required field returns HTTP 404** (not 400) with body `{"timestamp":…,"status":404,"error":"Not Found","path":"/quote"}`. There is no `message` key, so `error.data.message` is `undefined` and **the red banner renders empty.** | ⚠️ **Needs a scope ruling** (§8, OD-G). Options: **(a)** frontend-only — show a fixed fallback message when no `message` is present (keeps "no backend change"); **(b)** backend fix — `@RestControllerAdvice` returning `{message: "..."}` with 400, restoring the intended `QuoteValidator` messages ("Driver birth date not supplied.", "Vehicle make not supplied.", …). **Recommend (b)** — it is a ~5-line change that makes the feature work as designed; (a) permanently hides real validation feedback. | no | **YES under (b)** — the banner gains text; status changes 404 → 400 |
| **DR-15** | `less/variants/variants.less:11-27` | Variant cards have `cursor:pointer` + a hover shadow/background, implying selectability, but there is **no click handler**. | Remove `cursor:pointer` and the hover treatment. | A false affordance. Adding real selection would be a new feature — explicitly out of scope. | no | no (visual only) |
| **DR-16** | `sidebar.js:21-27` + `less/sidebar/sidebar.less:33-36` | `ctrl.sidebarHidden` and `ctrl.toggleSidebar()` are never referenced in `sidebar.html`; the `transition:0.5s` / `opacity:1` rules are leftovers of a removed collapse feature. | Do not port; delete the CSS leftovers. | Dead code (`minimal-implementation.md`: "unused code is debt"). | no | no |
| **DR-17** | `driver.js:33` | `$scope.selectedDate = new Date()` is never bound to anything. | Do not port. | Dead code. | no | no |
| **DR-18** | `variants.html:24` + `discount.js:13` | `<discount price-before-discount="discount.priceBeforeDiscount">` — **the field does not exist on `DiscountDto`** and is never used in `discount.html`. Binds `undefined` on every render. | Drop the binding and the input. | Dead binding to a non-existent field. | no | no |
| **DR-19** | `driver.html:26` + `driver.html:31` | `show-button-bar="false"` disables the button bar, yet `close-text="Close"` (which only labels a button in that bar) is still set. | Do not port `close-text`. | Dead attribute. | no | no |

### 3.2 Structural / hygiene defects (DR-20 … DR-27) — no user-visible behaviour change

| ID | Location | Current | New | Rationale |
|---|---|---|---|---|
| **DR-20** | `index.html:14` + `main.js:4` | The root component is registered on the tag **`main`** — a real HTML5 element. | Rename to `app-root`. | **Mandatory.** Angular would not compile a real HTML element as a selector, and it is invalid custom-element naming. |
| **DR-21** | `src/main/webapp/styles/styles.min.css` (2946 lines) | Generated by Gulp, **committed to git**, and not actually minified. Contradicts `css.md` and `build.md`. | Deleted; CLI output is hashed and git-ignored. | Generated assets are never committed. |
| **DR-22** | `package.json:17` | `merge-stream` is declared as a **runtime** `dependency` but is used only by `gulpfile.js:5`. | Deleted with Gulp. | Misfiled dependency. |
| **DR-23** | `gulpfile.js:43-45` | `gulp.watch(paths.less, ['less'])` — gulp 3 array syntax under gulp 4. **The watch task is broken**; there is no dev watch loop today. | Replaced by `ng serve` (HMR). | Broken task. |
| **DR-24** | `src/main/webapp/imgs/` | `logo.png` and `select-arrow.png` are referenced nowhere. | Not carried over. | Dead assets. |
| **DR-25** | `less/variants/coverage.less`, `less/common/common.less` | `.coverage__divider`, `.common-checkbox`, `.common-subsection-heading` are unused. | Not carried over. | Dead CSS. |
| **DR-26** | all 13 `*.js` | No `$inject` / array-form DI annotations — **the current bundle is not minification-safe**; it only works because nothing minifies it. | Moot: TypeScript + AOT + `inject()`. | Latent build-safety defect, resolved by construction. |
| **DR-27** | `driver.html:29` + `driver.js:42,45-47` | `is-open="popup.opened"` binds to `$scope` while every other binding in the component is on `ctrl` — an inconsistency that only works because AngularJS templates see both. | A single component field/signal. | Removed by construction; recorded so the popup-open state is not mistaken for lost behaviour. |

### 3.3 Explicitly NOT fixed (recorded non-deviations)

Listing these prevents a well-meaning implementer from "fixing" them and creating an unsanctioned diff.

| Item | Location | Why it stays |
|---|---|---|
| Vehicle emit asymmetry | `vehicle.js:76` | DR-13 — deliberate; changes `GET quote` volume substantially |
| Sidebar section labelled **"Documents"** while containing claim history | `sidebar.html` | Copy change, not a defect; **out of scope** (OD-15 → deferred; renaming is a product decision) |
| The "Address" divider emitted from **inside** `driver.html:55-60` rather than from the sidebar | `driver.html:55-60` | Odd structure, zero user impact; restructuring risks layout drift for no gain |
| No `<form>` element, no validation display anywhere | all templates | Adding validation is a **new feature** |
| Zip code has no mask or pattern | `driver.html:64-69` | Same — new feature |
| Startup spinner flash from 8 `templateUrl` XHRs | `main.html:2` | Disappears by construction (compiled templates). Cannot be reproduced without artificial delay; **accepted as a sanctioned disappearance** (OD-13) |
| No router / no deep linking | — | `minimal-implementation.md` (YAGNI); introducing one would require a catch-all forward in `ApplicationConfig.java` |
| WAR is not deployable to an external Tomcat (`Application` does not extend `SpringBootServletInitializer`) | `Application.java` | Current, intended behaviour — **do not "accidentally fix"** |

**Register totals: 19 behavioural + 8 structural = 27 sanctioned deviations**, plus 8 recorded
non-deviations. Two rows (**DR-06**, **DR-14**) carry open scope questions and are escalated in §8.

---

## 4. Migration type classification & strategy

### 4.1 Classification

| Dimension | Value |
|---|---|
| Type | **Technology migration — full framework replacement (rewrite), single tier (frontend only)** |
| Sub-type | Big-bang rewrite with a functional-parity constraint |
| Backend impact | **None required.** One optional 5-line change under DR-14/OD-G |
| Data migration | **None.** No client state; the server holds everything in `@SessionScope` |
| Contract impact | **None.** All 21 endpoints, URLs, relative-path convention and session semantics unchanged; one `Content-Type` header shifts `application/json` → `text/plain` on 6 string PUTs (verified accepted) |
| Reversibility | **High** — a single branch; revert restores `src/main/webapp/` + `gulpfile.js` + `less/` |
| Blast radius | 24 files under `src/main/webapp/`, 10 under `less/`, `pom.xml`, `package.json`, `gulpfile.js`, `.gitignore`, 8 doc files |

### 4.2 Strategy options

| | Big-bang cutover | Phased by component | Dual-run (both frontends behind different paths) |
|---|---|---|---|
| Feasible here? | **Yes** | **No** | Yes, but expensive |
| Blocker | — | Requires ngUpgrade for coexistence. **All 8 components use `.directive()`, and ngUpgrade requires the `.component()` API** — every component would need rewriting *as a precondition to the rewrite*. Phase 1's ruling stands. | — |
| Cost | 920 lines, 8 components, 4 services, 1 screen, no router, no client state | n/a | Two `webResources` roots + two context sub-paths + duplicate asset pipelines + a way to keep both LESS builds alive |
| Risk profile | One cutover moment, gated by golden captures | n/a | Lower cutover risk, but **doubles the build surface for the duration** and adds an `ApplicationConfig` change (a second static root) that step 1 otherwise does not need |
| Verdict | ✅ **RECOMMENDED** | ❌ Rejected | ⚠️ Rejected — see below |

### 4.3 Recommendation: **big-bang cutover on a branch, behind a mandatory golden-capture gate**

Justification:

1. **The app is one screen.** There is no route to migrate incrementally to, no client state model to
   split, and no user-visible seam where a partial migration could live. Phased migration has nothing
   to phase.
2. **ngUpgrade is genuinely unavailable** (`.directive()` vs `.component()`), and Phase 1's cost
   analysis is sound: shipping AngularJS 1.8.3 *plus* Angular 22 in one bundle to migrate 920 lines is
   strictly more work than rewriting them.
3. **Dual-run does not buy what it appears to.** Its usual value is *incremental production traffic
   shifting*. This is a **demo/reference application** (`tech-stack.md`: not production, no SLA, 1–2
   person team) — there is no traffic to shift. And the side-by-side golden replay it is meant to
   enable is achievable far more cheaply: **the AngularJS build is a git artefact.** Checking out the
   pre-migration commit, running `mvn clean package`, and replaying the captured Playwright flows
   against that WAR gives an identical A/B comparison with **zero** permanent build complexity.
   ⚠️ **This is the substitute for dual-run and it must be written into the plan**, because it is what
   makes C-4's "golden before/after" work without a parallel deployment.
4. **The cutover risk is concentrated in packaging, not in components** (G-A3/G-A9 — a WAR that builds
   cleanly but ships no UI). That risk is retired by a single acceptance check, not by phasing.
5. **The revert path is one `git revert`.** Nothing in the migration touches the database, the Higson
   snapshot, the Docker chain, or any Java source (modulo the optional DR-14 fix).

**Non-negotiable gate**: G-G1 (golden captures) completes **before** a single file under
`src/main/webapp/` is deleted. See §7.

---

## 5. Risk assessment

**Overall risk: MEDIUM.** Phase 1 rated this medium-high on four factors; corrections 1 and 2 (§0)
remove the two silent-breakage traps that carried most of the "-high". What remains is a datepicker,
a decision load, and a test harness that has to be built from nothing.

### 5.1 Trap (i) — `HttpClient` and the six raw-string PUT bodies → **NOT A DEFECT**

**Phase 1 is wrong** (`current-state-analysis.md:212`). Source evidence, `@angular/common@22.1.0`,
`fesm2022/_module-chunk.mjs`:

```js
537:  serializeBody() {
538:    if (this.body === null) { return null; }
541:    if (typeof this.body === 'string' || isArrayBuffer(this.body) || isBlob(this.body)
        || isFormData(this.body) || isUrlSearchParams(this.body)) {
542:      return this.body;                    // ← string bodies pass through VERBATIM
543:    }
547:    if (typeof this.body === 'object' || typeof this.body === 'boolean' || Array.isArray(this.body)) {
548:      return JSON.stringify(this.body);    // ← quoting applies to objects, not strings
549:    }
```

```js
552:  detectContentTypeHeader() {
565:    if (typeof this.body === 'string') { return TEXT_CONTENT_TYPE; }   // 'text/plain'  (line 427)
571:    if (typeof this.body === 'object' || 'number' || 'boolean') { return JSON_CONTENT_TYPE; }
```

So `HttpClient` sends `Warszawa` — **not** `"Warszawa"`. The only divergence is the `Content-Type`
header: `application/json` → `text/plain`.

**Verified end-to-end against the running app** (isolated session cookie jar, so no shared state was
disturbed):

| Wire form | Result |
|---|---|
| `PUT driver/address/city`, body `Gdansk`, `Content-Type: text/plain` | **200**, `GET driver` → `city = 'Gdansk'` ✅ |
| `PUT driver/gender`, body `F`, `text/plain` | **200**, `gender = 'F'` ✅ |
| `PUT driver/firstName`, body `Anna`, `text/plain` | **200**, `firstName = 'Anna'` ✅ |
| `PUT driver/accidentCount`, body `5`, `application/json` | **200**, `numberOfAccidents = 5` ✅ |
| `PUT driver/birthDate`, body `"1990-05-12T00:00:00.000Z"`, `application/json` | **200**, `dateOfBirth = '1990-05-12T00:00:00.000+00:00'` ✅ |
| *(control)* body `"Krakow"` **with quotes**, `application/json` | **200**, `city = '"Krakow"'` ❌ — corruption *is* real, but only if you add the quotes yourself |

Spring's `StringHttpMessageConverter` accepts `text/plain` for `@RequestBody String`; none of the 11
`DriverApi` / 5 `VehicleApi` handlers declare `consumes`.

**Mitigation (downgraded from "critical fix" to "guardrail")**: pass the six values as plain `string`
to `HttpClient` and **do not** hand-roll a serializer, a `text/plain` header, or a `JSON.stringify`.
The failure mode is now *over*-engineering: wrapping a value in `JSON.stringify` "to be safe" is what
would actually corrupt it. **G-G3's MockMvc goldens must pin the body bytes**, so any future
regression here fails a test rather than a pricing run.

### 5.2 Trap (ii) — `<base href>` vs the servlet context path → **RESOLVED EMPIRICALLY (OD-B)**

Phase 1 left this as an "UNRESOLVED CONTRADICTION" (`:491`) and hypothesised the container might serve
at `/`. **That hypothesis is false.** Evidence, in order:

1. **The mechanism.** `HigsonIntegrationConfiguration.java:25`:
   `@PropertySource(value = {"classpath:higson-demo-app.properties", "file:${user.home}/conf/higson-demo-app.properties"}, ignoreResourceNotFound = true)`.
   Spring registers **one property source per location**, and resolution is **per key**, not
   per file. `docker/app.properties` simply *does not mention* `server.servlet.context-path`, so the
   classpath value (`higson-demo-app.properties:24` → `/demo`) still applies. `Dockerfile:8` copies
   `docker/app.properties` to `/root/conf/higson-demo-app.properties`, and `user.home` is `/root`, so
   the external file does load — it just overrides only the keys it declares.
2. **Direct proof.** A throwaway container from `decerto/higson-motor-insurance-demo:4.3.0` with **no**
   context-path environment variable logs:
   `Tomcat started on port 48080 (http) with context path '/demo'`.
   `GET /` → **404**; `GET /demo/` → **200**; `GET /demo/quote` → **200**.
   This also confirms `Readme.md:99` ("available at `[host]:48080/demo`") is accurate.
3. **The *real* risk, which Phase 1 missed.** The container already running on this machine
   (`higson-demo-local-web`) carries `SERVER_SERVLET_CONTEXT_PATH=/app` and logs
   `Tomcat started on port 48080 (http) with context path '/app'`; `GET /app/` returns the SPA and
   `GET /app/quote` returns the `QuoteDto`. **The context path is a runtime value that is actively
   overridden in practice.**

**Conclusion**: Phase 1's recommended fix — `ng build --base-href=/demo/`
(`current-state-analysis.md:489`) — **would have shipped a bundle that 404s on every asset in a
container that is running right now.** It must not be adopted.

**Mitigation**: ship **no `<base>` element** (§1.4). This reproduces today's behaviour exactly, keeps
`services.md`'s relative-URL rule intact, and makes the bundle context-path-agnostic. Acceptance test:
run the built WAR at **three** context paths — `/demo` (default), `/` and `/app` — and confirm the app
loads and `GET quote` succeeds at each.

### 5.3 `dateOfBirth` is an ISO-8601 string, not epoch millis

Phase 1 (`:194`) states `dateOfBirth` is "a NUMBER (epoch millis)" because "no Jackson date
configuration is present". That inference is wrong for Spring Boot 3: `JacksonAutoConfiguration`
disables `WRITE_DATES_AS_TIMESTAMPS` by default. **Live `GET driver`:**

```json
{"firstName":"John","lastName":"Potter",
 "dateOfBirth":"1986-08-06T00:00:00.000+00:00","gender":"M",
 "address":{"zipCode":"77566","city":"Lake Jackson","street":"Allwood St"},
 "numberOfAccidents":0,"numberOfTickets":0,"licenceObtainedAtAge":18}
```

`DriverDto.java:9` is `private Date dateOfBirth;` and `DriverApi.java:34-35` is
`@PutMapping("/birthDate") public void updateBirthDate(@RequestBody Date birthDate)` — **`required` is
not set, so this one is mandatory**, unlike the other ten which are all `required = false`.

Impact:
- `DriverDto.dateOfBirth` types as **`string`**, not `number` (G-B4). `driver.js:57`'s
  `new Date(...)` parses both, so the *current* frontend is accidentally agnostic — but a TS model
  typed `number` would be wrong and would break `MatDatepicker` binding.
- **G-G3's MockMvc golden must pin the ISO-8601 shape**, including the `+00:00` offset.
- ⚠️ **Timezone sensitivity (new risk, not in Phase 1).** The server emits an *offset-bearing* instant.
  `Motor.createDriver` seeds "today − 40 years, start-of-day, **system TZ**", so the offset follows the
  server's zone (`+00:00` in the container, `+02:00` on a Europe/Warsaw dev box).
  `MatDatepicker`'s default `NativeDateAdapter` formats and parses in **local browser time**. If the
  browser's zone is behind the server's, the displayed day can be **off by one**. This is
  parity-neutral (today's `uib-datepicker` has the identical flaw) but it **will make screenshot and
  E2E goldens non-reproducible across machines**. *Mitigation*: pin `TZ=UTC` for all golden capture
  and replay runs, and record the timezone in the capture metadata.

### 5.4 The error contract returns 404, not 400 — and the banner is empty today

`GlobalExceptionHandler.java` (verified in full):

```java
@ControllerAdvice                                  // ← not @RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidCarDataException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public String handleInvalidCarDataException(InvalidCarDataException e) {
        return e.getMessage();                     // ← no @ResponseBody ⇒ treated as a VIEW NAME
    }
    // …same for InvalidDriverDataException
}
```

**Live**: blank the driver's first name, then `GET quote` →

```
status=404   content-type=application/json;charset=UTF-8
{"timestamp":"…","status":404,"error":"Not Found","path":"/app/quote"}
```

The `@ResponseStatus(BAD_REQUEST)` is overridden when view resolution fails. There is **no `message`
key** (`server.error.include-message` defaults to `never`), so `variants.js:37`'s
`error.data.message` is `undefined` and **today's red banner renders with no text at all**.

This corrects Phase 1's REST table (`:188`, "`QuoteDto` or 400") and confirms its OD-7 hypothesis about
the mechanism. It is captured as **DR-14** with an escalated scope question (§8, OD-G).

### 5.5 Backend sort order — the C-3 mitigation question, answered

C-3 required: *"confirm against the running backend whether the server already returns coverages and
discounts sorted by `position` before introducing a client-side sort."*

**It does.** Live `GET quote`:

| Option | `order` | coverage `position`s | ascending? | discount `position`s | ascending? |
|---|---|---|---|---|---|
| BRONZE | 1 | `[1, 2, 3]` | ✅ | `[2]` | ✅ |
| SILVER | 2 | `[1, 2, 4, 5, 8]` | ✅ | `[2]` | ✅ |
| GOLD | 3 | `[1, 2, 4, 5, 6, 7, 8, 9]` | ✅ | `[2]` | ✅ |

⇒ **DR-05 introduces no client-side sort.** Rendering in array order is verified byte-identical today,
and adding a sort would create a divergence the day the backend order changes.
(`variants.html:5`'s `| orderBy: 'order'` is quoted and *does* work; the backend also returns options
in `order` 1/2/3 — so sorting in TS by `order` is likewise a verified no-op, and array order suffices.)

### 5.6 Remaining breaking changes and mitigations

| # | Breaking change | Severity | Mitigation |
|---|---|---|---|
| R-1 | **Node 23.6.1 rejected by Angular 22.** Fails **inside `prepare-package`**, so the error surfaces as a Maven build failure with an opaque npm message | **High** | G-A1: install Node 24.19.0, commit `.nvmrc`, add `engines` + `engine-strict=true` so the failure names the required range. Consider `frontend-maven-plugin` (OD-E) |
| R-2 | **WAR builds green but ships no UI** if `<webResources>` path or the CLI `outputPath` drift | **High** (silent) | G-A3/G-A9: acceptance check asserting `index.html` and `main-*.js` exist inside `target/motor-insurance.war`, run **under `-Pall_in_one`** |
| R-3 | **`MatDatepicker` cannot reproduce `uib-datepicker-popup` behaviour** — `yyyy-MM-dd` display, `yyyy/MM/dd` alt-parse, Monday-first, no week numbers, no button bar, external icon trigger | **High** | G-C7/G-F2: custom `DateAdapter` + `MAT_DATE_FORMATS`; budget 1–2 days. Visual difference is **pre-accepted by C-1** |
| R-4 | **Loss of the implicit global spinner.** `ngLoadingSpinner` is an invisible `$httpProvider` interceptor; nothing in the templates names it | **Med** | G-D2: port it *first*, before any component, so every later component inherits it |
| R-5 | **Emit asymmetry lost** — an idiomatic reactive rewrite naturally recalculates on every change | **Med** | G-D3/DR-13: encode it explicitly in `QuoteStore` (only `updateModel` calls `recalculate()`); assert `GET quote` counts in Playwright |
| R-6 | **`response.data` double-unwrap.** `HttpClient` already unwraps; a leftover `.data` yields `undefined`, not an error | **Med** | G-B2 + **G-H4** — the `services.md` clause must be deleted or it will actively generate wrong code |
| R-7 | **Dev-server session loss** via `JSESSIONID; Path=/demo` not matching `localhost:4200/driver` | **Med** | §1.5: prefer option A (root context in dev); otherwise `cookiePathRewrite` |
| R-8 | **`@for` requires `track`** — none of the 3 `ng-repeat`s has `track by` | **Low** | `track variant.code` / `coverage.code` / `discount.code` |
| R-9 | **`coverage.description` is HTML** from Higson rules, fed to `tooltip-template` | **Med** | OD-A. Treat as **trusted** HTML (`DomSanitizer`) — it originates from the rules engine, not user input |
| R-10 | **Golden captures skipped or taken after deletion** — parity becomes unfalsifiable, permanently | **High** | §7 gate: G-G1 precedes all deletion. `git revert` cannot recreate a *runtime* capture |
| R-11 | **E2E requires a populated Higson snapshot.** `src/main/higson/` is **not in git** (9 years, 91 commits); a fresh clone has no rules | **Med** | `mvn higson-cli:pull` documented as an E2E precondition; note the snapshot standards |
| R-12 | **Timezone-dependent goldens** (§5.3) | **Med** | Pin `TZ=UTC` for capture and replay; record TZ in capture metadata |
| R-13 | **Material theming vs the LESS design system** — 56 px notched-outline inputs, `#4C4CDE`, radius 8 px | **Med** | OD-D. Restyling Material inputs to match is the larger hidden cost of C-1 |
| R-14 | **Bootstrap 3 CSS retained for the grid** carries FA4/BS3 EOL debt forward | **Low** | OD-C — lowest-risk parity option; flag as step-2 debt |
| R-15 | **`engine-strict` / exact pins vs CLI defaults** — `ng new` writes `^` ranges, contradicting `build.md` | **Low** | G-A11: pin after scaffolding, before the first commit |

---

## 6. Resolution proposals for OD-A … OD-F (+ one new item)

Each is decision-ready: a recommendation, the trade-off, and what it costs to reverse.

### OD-A — HTML content of `coverage.description` in a Material tooltip

`MatTooltip` renders **text only**; `coverage.description` is HTML from the Higson rules
(`coverage.html:3`, `tooltip-template="{{ctrl.description}}"`).

| Option | Trade-off |
|---|---|
| **A1 — `CdkOverlay` popover with `[innerHTML]` + `DomSanitizer` (RECOMMENDED)** | Preserves the current capability exactly (HTML body, right side, 340 px min-width, custom `arrow-tooltip.svg`). Costs ~0.5 d and one hand-built overlay; it is CDK (already a Material dependency), not a third library. |
| A2 — `MatTooltip` with HTML stripped to plain text | Cheapest, but **loses user-visible content** — any markup in the rules becomes lost formatting or visible tags. This is a functional regression, not a visual one, so it falls outside C-1's accepted visual change. |
| A3 — `MatTooltip` for short descriptions + overlay for long ones | Two code paths for one feature; rejected. |

**Recommendation: A1.** Note that `less/config.less:22-42` (the `tooltip`/`tip`/`tip-arrow` block) is
coupled to `angular-tooltips`' internal DOM and must be **rewritten**, not ported (G-E5).
**Reversal cost**: low — swapping to A2 later is a template change.

### OD-B — `<base href>` vs `server.servlet.context-path` → **RESOLVED, no decision needed**

**Ship no `<base>` element.** Full evidence in §5.2. The default is `/demo` in Docker *and* locally; the
value is runtime-overridable and **is being overridden** (`/app`) on this machine. Baking any literal
into the bundle is wrong. Record the rule in `build.md` (G-H11) and acceptance-test at `/demo`, `/`
and `/app`. **This closes OD-18 as well.**

### OD-C — Bootstrap 3 CSS retention

Needed for `.container` / `.row` / `.col-md-3` / `.col-md-9`, `.alert.alert-danger`, and
`.glyphicon-calendar`. **No Bootstrap 3 JavaScript is used and jQuery is not loaded.**

| Option | Trade-off |
|---|---|
| **C1 — keep Bootstrap 3 CSS alongside Material (RECOMMENDED for step 1)** | Lowest parity risk; the grid, the 1440 px container override (`config.less:16-20`) and the error alert all keep working untouched. Cost: carries an EOL CSS dependency forward, and BS3 + Material CSS must be checked for cascade collisions (both style `button`, `input`, `.container`). Load Bootstrap **before** the Material theme. |
| C2 — reimplement the grid in CSS Grid/Flexbox, hand-style the alert, swap the glyphicon for a Material icon | Removes the EOL dependency and ~1 MB of CSS. Costs ~1 d and puts **layout** — the one thing currently guaranteed correct — into the diff, weakening the screenshot review that C-4 depends on. |

**Recommendation: C1 for step 1; schedule C2 as step 2.** The layout is a 2-column single screen;
C2 is genuinely easy *later*, when a verified-correct baseline exists to diff against.
⚠️ The `.glyphicon-calendar` trigger disappears anyway once `MatDatepicker`'s own toggle is used
(G-C7), which removes one of C1's three justifications.

### OD-D — Angular Material theme vs the existing LESS tokens

Tokens: primary `#4C4CDE`, radius `8px` (`less/variables.less`), 56 px inputs with a floating 9 px
uppercase `#666` label at `top:-5px; left:10px` on white (a hand-built notched outline).

| Option | Trade-off |
|---|---|
| **D1 — Material theme seeded from the existing tokens; use Material *only* for the four C-1 controls; keep the LESS input chrome for the 7 text inputs and 4 selects (RECOMMENDED)** | Confines the visual delta to exactly the four controls C-1 sanctioned. The existing `.common-input-default` chrome already *is* a notched outline, so `MatFormField` would be a near-duplicate at extra cost. Requires a Material M3 theme with `$primary: #4C4CDE` and `--mat-sys-corner-*` overrides so the datepicker panel and tooltip inherit the palette. |
| D2 — adopt `MatFormField` / `MatSelect` everywhere and delete the custom LESS | More idiomatic and better a11y for free, but **restyles every field on the screen** — far beyond C-1's four controls, and it would swamp the screenshot review. |

**Recommendation: D1.** It is the only option consistent with C-1's scope. **Reversal cost**: low —
D2 remains available in step 2 once a verified baseline exists.

### OD-E — Node toolchain (**BLOCKER**)

Local Node **23.6.1**; required `^22.22.3 || ^24.15.0 || >=26.0.0`. No nvm/fnm/volta installed.

| Option | Trade-off |
|---|---|
| **E1 — install Node 24.19.0 + commit `frontend/.nvmrc` + `engines` in `package.json` + `.npmrc` `engine-strict=true` (RECOMMENDED for step 1)** | Smallest change; `exec-maven-plugin` and the `build.md` rule "frontend build wired into `prepare-package`" stay exactly as documented. `engine-strict` converts a silent mid-build failure into a loud one naming the range. **Weakness**: still depends on whatever `npm` is on `PATH`, and does not fix the pre-existing Windows `npm` vs `npm.cmd` wart. |
| E2 — replace `exec-maven-plugin` with `frontend-maven-plugin` 1.15.1 | Pins **and downloads** Node/npm per build: reproducible on any machine and in future CI, and fixes the Windows wart. **Costs**: a new plugin, a ~50 MB per-machine Node download, and it rewrites the exact mechanism `build.md` documents — so G-H1 grows. |
| E3 — both (E1 now, E2 when CI arrives) | Recommended sequencing. |

**Recommendation: E1 now, E2 when CI is introduced (out of scope for step 1).** Also update
`conventions.md`'s "Required Local Toolchain Baseline", which currently says only *"NodeJS"* — that
vagueness is precisely what allowed a non-conforming Node onto this machine (G-H5).
⚠️ **This must be cleared before any `ng` command is run**, including `ng new`.

### OD-F — Fonts and icons

Today: 96 committed font files (Font Awesome 4 ×6 + Open Sans ×90), **duplicating** what
`less/config.less:1,5` imports from `node_modules`, plus **Manrope from the Google Fonts CDN**
(`config.less:4`) — which is the actual body font.

| Option | Trade-off |
|---|---|
| **F1 — self-host all three via npm packages, let the CLI hash and emit them; delete the 96 committed files (RECOMMENDED)** | Removes a runtime CDN dependency (a third-party availability and privacy exposure in a demo shipped to customers), removes 96 tracked binaries, and eliminates the duplication. Costs: adding a Manrope npm package (e.g. `@fontsource/manrope`) and re-anchoring `@FontPathOpenSans`. |
| F2 — keep the committed fonts in `public/fonts/`, keep the Manrope CDN | Zero risk, zero benefit; perpetuates the duplication and the CDN dependency. |
| F3 — F1 + drop Font Awesome 4 in favour of Material Icons | FA4 is used only via `less/config.less`; Material brings its own icon font, so this avoids shipping two icon fonts. ⚠️ **Audit first** — the sidebar's 4 icons are CSS `background-image` SVGs, not FA glyphs, so FA4's actual usage may be near-zero. |

**Recommendation: F1, plus the F3 audit.** If the audit shows Font Awesome 4 has no remaining call
sites, drop it entirely (that becomes an additional DR-25-class dead-asset entry).
**Reversal cost**: low.

### OD-G — **NEW**: does DR-14 (the broken error contract) authorise a backend change?

**Not previously listed, and it needs a ruling before Phase 3.** C-3 says "fix all defects"; the task
framing says frontend-only and Phase 1 says "no backend change is required for parity". DR-14 is the
one defect that **cannot** be fixed properly on the frontend.

| Option | Trade-off |
|---|---|
| **G1 — fix the backend: `@RestControllerAdvice` returning `{ "message": "…" }` with 400 (RECOMMENDED)** | ~5 lines in one file. Restores the `QuoteValidator` messages the UI was designed to display, and makes the *only* error path in the app actually work. **Costs**: it is a backend change, and it alters the observed contract (**404 → 400**, empty banner → real text), so **G-G3's golden must record the current 404 and then be updated deliberately** as a register entry. |
| G2 — frontend-only: fall back to a fixed generic message when `message` is absent | Keeps the "no backend change" boundary intact and makes the banner non-empty. **But** it permanently hides real validation feedback and leaves a `@ControllerAdvice` that returns view names — a live bug — in place. |
| G3 — leave it broken (bug-for-bug) | Contradicts C-3. |

**Recommendation: G1**, with the status-code change explicitly recorded in the defect register.
Also note **DR-06** (adding mutation error handling) carries a related scope question — it is arguably
a *new feature* under "zero new features". Recommend accepting it **narrowly**: surface failures
through the banner that already exists, with no retry and no rollback.

---

## 7. Sequencing

### The hard gate

> **G-G1 — golden captures (MockMvc + Playwright + screenshots) must be recorded against the
> CURRENT AngularJS build BEFORE a single file under `src/main/webapp/`, `less/`, `gulpfile.js` or the
> root `package.json` is deleted or modified.**
> A `git revert` restores *code*; it cannot recreate a *runtime* capture without rebuilding and
> redeploying the old stack. Skipping this makes C-4 unsatisfiable and the parity claim unfalsifiable.
> Preconditions: Node blocker cleared (G-A1) is **not** required for this step, but
> `mvn higson-cli:pull` **is** — a fresh clone has no rules (R-11). Pin `TZ=UTC` (R-12).

### Ordered stages

| Stage | Gaps | Parallelisable? | Gate to exit |
|---|---|---|---|
| **0 — Unblock & decide** | G-A1 (Node), OD-A/C/D/E/F/G rulings, G-G2 (JUnit 4→5 + `spring-boot-starter-test`) | G-A1 ∥ G-G2 ∥ the rulings | `node -v` in range; every OD closed; `mvn test` compiles |
| **1 — GOLDEN CAPTURE (hard gate)** | **G-G1**, G-G3, G-G4, G-G5 | G-G3 (MockMvc) ∥ G-G4+G-G5 (Playwright/screenshots) | 21 API shapes pinned incl. `text/plain` bodies, ISO-8601 `dateOfBirth`, the **404** error contract; E2E flows recorded; screenshots stored **with TZ metadata** |
| **2 — Scaffold & packaging** | G-A2, G-A10, G-A11, G-A3, G-A4, G-A5, G-A6, G-A8, G-A9 | G-A8 (dev server) ∥ the rest | ⚠️ **`mvn clean package -Pall_in_one` produces a WAR containing `index.html` + `main-*.js`, and the container serves it at `/demo`, `/`, AND `/app`** (§5.2). Prove packaging with a placeholder `app-root` **before** writing any component. |
| **3 — Store, interceptor, services** | G-D1, G-D2, G-D4, G-B1, G-B2, G-B3, G-B4, G-B5, G-B6 | G-D1+G-D2 ∥ G-B1…G-B6 | Store encodes the emit asymmetry (G-D3); spinner interceptor live; all 4 services typed; **zero `.data` unwraps remain** |
| **4 — Components, leaves first** | G-C1, G-C2 → G-C3 → G-C4, G-C5 → G-C6 → G-C10 → **G-C7** | G-C1 ∥ G-C2; G-C4 ∥ G-C5 | Each component's parity-checklist section passes. **G-C7 (datepicker) last — it is the highest-risk item and must not block everything behind it** |
| **5 — Shell & Material** | G-C8, G-C9, G-F1, G-F3, G-F4, G-F5 | G-F1 must precede G-C7/G-C10 (hoist it into stage 4 if the datepicker starts early) | Single screen renders end-to-end |
| **6 — Styles & assets** | G-E1, G-E2, G-E3, G-E4, G-E5, G-E6, G-E7, G-E8, G-E9 | G-E7 (fonts) ∥ the rest | No 404s on fonts/images; screenshot diff reviewed |
| **7 — Parity verification** | replay stage-1 goldens | — | Every diff traceable to **C-1** or a **DR-nn** row. Unexplained diff ⇒ regression |
| **8 — Cutover & cleanup** | G-A7 (delete Gulp, `vendors/`, root `package.json`), delete `src/main/webapp/**` | — | `mvn clean package -Pall_in_one` green from a clean clone |
| **9 — Docs & standards** | G-H1…G-H12 | fully parallel | `INDEX.md` regenerated; **G-H4 and G-H11 are the two that prevent future wrong code** |

### Critical path

`G-A1 → G-G1 → G-A2 → G-A3/G-A9 → G-D1 → G-C5/G-C6 → G-C7 → parity replay → cutover`

### Parallelisable clusters

- Stage 0: Node install ∥ JUnit migration ∥ OD rulings (independent)
- Stage 1: MockMvc ∥ Playwright/screenshots (independent harnesses)
- Stage 3: `QuoteStore` + interceptor ∥ the 4 services (only the components need both)
- Stage 4: `coverage` ∥ `discount`; later `driverHistory` ∥ `vehicle`
- Stage 9: all 12 documentation gaps

### Anti-sequencing (things that must NOT happen early)

- ❌ Deleting `src/main/webapp/` before stage 1 completes
- ❌ Writing components before stage 2 proves the WAR ships the UI (`-Pall_in_one`, three context paths)
- ❌ Starting the datepicker (G-C7) before `QuoteStore` and the driver component exist
- ❌ Running `ng new` before the Node blocker is cleared — it will fail or silently produce a workspace pinned to an unsupported toolchain
- ❌ Regenerating `package-lock.json` before OD-E is settled

---

## 8. `task_characteristics`

```yaml
task_characteristics:
  # --- classification ---
  task_type: migration
  migration_subtype: technology_replacement_frontend
  migration_strategy: big_bang_cutover_branch_with_golden_gate
  ngupgrade_ruled_out: true            # .directive() vs required .component() API — Phase 1 ruling stands
  dual_run_ruled_out: true             # replaced by replaying goldens against the pre-migration commit
  tiers_affected: [frontend, build, packaging, documentation, test_infrastructure]
  backend_change_required: false       # except optional OD-G / DR-14 (~5 lines, needs a ruling)

  # --- detected characteristics ---
  has_reproducible_defect: true        # 27 registered; DR-01/DR-02/DR-14 empirically reproduced
  modifies_existing_code: true
  creates_new_entities: true           # frontend/ workspace, QuoteStore, interceptors, test harness
  involves_data_operations: false      # no client state; server holds all state in @SessionScope
  ui_heavy: true

  # --- scale ---
  current_loc: 920                     # 13 JS + 11 HTML; +599 LESS across 10 files
  components_to_rewrite: 8
  services_to_rewrite: 4
  rest_endpoints_to_preserve: 21
  screens: 1
  routes: 0
  gaps_identified: 65
  defect_register_entries: 27          # 19 behavioural + 8 structural
  recorded_non_deviations: 8

  # --- risk ---
  overall_risk: medium                 # revised DOWN from Phase 1 "medium-high"
  risk_revision_reason: >-
    Both silent-breakage traps were disproved empirically: HttpClient does NOT JSON-quote string
    bodies (Angular source + live wire test), and the Docker context path IS /demo by default
    (throwaway container log). What remains is the datepicker rework, the decision load, and a
    test harness that must be built from nothing.
  complexity: moderate
  effort_estimate: high
  reversibility: high                  # single branch; no data, schema, snapshot or Docker change
  regression_safety_net_exists: false  # zero tests, zero CI today

  highest_risk_items:
    - id: G-G1
      what: golden captures must precede any deletion; a revert cannot recreate a runtime capture
    - id: G-C7
      what: MatDatepicker cannot reproduce uib-datepicker-popup; largest single rework (1-2 d)
    - id: G-D1
      what: $emit/$broadcast bus -> signal store; genuine architectural decision, not a port
    - id: G-A3
      what: new maven-war-plugin webResources; silent failure mode (WAR builds green, ships no UI)
    - id: G-A1
      what: Node 23.6.1 rejected by Angular 22; fails opaquely inside prepare-package

  # --- Phase 1 corrections (all empirically verified this session) ---
  phase1_corrections:
    - id: PC-1
      claim: "HttpClient JSON-quotes the six raw-string PUT bodies, corrupting values"
      verdict: FALSE
      evidence: "@angular/common@22.1.0 serializeBody() L541-543 returns string bodies verbatim;
                 live PUT text/plain -> 200, values uncorrupted"
      impact: "removes the highest-severity Phase 1 risk; guardrail replaces the planned fix"
    - id: PC-2
      claim: "docker/app.properties omits context-path so the container may serve at /"
      verdict: FALSE
      evidence: "throwaway container, no env override: 'Tomcat started ... with context path /demo';
                 GET / -> 404, GET /demo/ -> 200. @PropertySource resolves per key, not per file"
      impact: "OD-B / OD-18 CLOSED; Phase 1's --base-href=/demo/ recommendation REJECTED"
    - id: PC-3
      claim: "dateOfBirth is a NUMBER (epoch millis)"
      verdict: FALSE
      evidence: "GET driver -> 'dateOfBirth': '1986-08-06T00:00:00.000+00:00' (ISO-8601 string)"
      impact: "DriverDto.dateOfBirth types as string; new timezone risk for goldens (R-12)"
    - id: PC-4
      claim: "GET quote returns QuoteDto or 400"
      verdict: WRONG_STATUS
      evidence: "live: HTTP 404 {'status':404,'error':'Not Found'}; no message key => banner is EMPTY"
      impact: "DR-14 + new open decision OD-G (backend fix authorised?)"
    - id: PC-5
      claim: "unverified whether the backend sorts coverages/discounts by position"
      verdict: RESOLVED_SORTED
      evidence: "all 3 options ascending: [1,2,3] / [1,2,4,5,8] / [1,2,4,5,6,7,8,9]; discounts [2]"
      impact: "DR-05 introduces no client-side sort; C-3 mitigation question answered"

  # --- decisions ---
  binding_decisions: [C-1, C-2, C-3, C-4]
  open_decisions_resolved_in_this_phase: [OD-B]
  open_decisions_with_recommendations: [OD-A, OD-C, OD-D, OD-E, OD-F]
  new_open_decisions: [OD-G]           # does DR-14 authorise a backend change?
  blocking_before_phase_3: [OD-E, OD-G]

  # --- target stack (verified live against the npm registry, 2026-08-06) ---
  target_stack:
    angular: "22.1.0"
    angular_cli: "22.1.3"
    angular_build: "22.1.3"
    angular_material: "22.1.1"
    angular_cdk: "22.1.1"
    typescript: "~6.0.3"               # peer >=6.0 <6.1 -- typescript@latest (7.0.2) BREAKS the build
    node: "24.19.0"                    # engines ^22.22.3 || ^24.15.0 || >=26.0.0
    rxjs: "~7.8.2"
    less: "^4.2.0"                     # optional peer of @angular/build; NOT bundled -- declare it
    zoneless: true                     # zone.js is an optional peer in v22
    builder: "@angular/build:application"
    test_runner_unit: "vitest ^4.0.8"
    test_runner_e2e: "@playwright/test 1.62.1"
    workspace_dir: "frontend/"
    output_path: "frontend/dist/motor-insurance/browser"
    base_href: null                    # NO <base> element -- context path is a RUNTIME value

  # --- invariants (breaking any of these is a failed migration) ---
  invariants:
    - "target/motor-insurance.war -- finalName unchanged (packaging.md)"
    - "frontend build stays bound to Maven prepare-package (build.md)"
    - "npm ci, never npm install (build.md)"
    - "all_in_one profile still produces a working executable WAR"
    - "port 48080 across properties, EXPOSE and compose (docker.md)"
    - "21 relative API URLs, no leading slash (services.md)"
    - "HTTP only in <Domain>Service classes (services.md)"
    - "session-scoped incremental recalculation preserved (architecture.md)"
    - "vehicle emit asymmetry preserved -- only model triggers reload (DR-13)"
    - "Dockerfile / docker-compose.yml / push-to-docker.sh / docker/app.properties untouched"
    - "no Java change except the optional OD-G fix"
    - "no router introduced (YAGNI; would require a catch-all forward in ApplicationConfig)"
    - "Higson snapshot tooling untouched"
```

---

## 9. What Phase 3 must decide before requirements can be written

1. **OD-E (Node)** — blocking. No Angular command can run until it is settled.
2. **OD-G (backend fix for DR-14)** — blocking. It determines whether the golden error contract is
   pinned at **404-with-empty-banner** or migrated to **400-with-message**.
3. **DR-06 scope** — is adding mutation error handling a sanctioned fix or a prohibited new feature?
4. **OD-A / OD-C / OD-D / OD-F** — each has a recommendation above; each is reversible at low cost, so
   they can be accepted as-recommended without further analysis if the user prefers.
5. **Confirm the register.** DR-01…DR-27 is the sanctioned deviation list. Once approved, anything not
   on it is a regression — that is the whole mechanism that makes C-3 reviewable.
