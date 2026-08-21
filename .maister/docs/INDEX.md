# Documentation Index

**IMPORTANT**: Read this file at the beginning of any development task to understand available documentation and standards.

## Quick Reference

### Project Documentation
Project-level documentation. Available: **tech-stack**, **architecture** (both in Polish), plus two profile design documents — **higson-life-profile** and **higson-property-profile**. Not created: vision, roadmap.

### Technical Standards
Coding standards, conventions, and best practices organized by domain. Six categories: **global** (9 files), **higson** (3 files), **backend** (6 files), **frontend** (8 files), **testing** (2 files), **deployment** (2 files).

> **`higson/` is the category that makes this codebase what it is.** All pricing logic lives in Higson rules rather than Java, and the rules snapshot is not in version control. Read `higson/snapshot.md` before reasoning about where the business logic actually is.

> **The frontend was rewritten in August 2026.** The SPA is now **Angular 22 in `frontend/`**, replacing the AngularJS 1.8.3 application that lived in `src/main/webapp/`. Every `frontend/` standard was rewritten or newly written for that stack; two are new (`typescript.md`, `state.md`). Migration record: `.maister/tasks/migrations/2026-08-06-angularjs-to-angular/`.

---

## Project Documentation

Located in `.maister/docs/project/`

*Generated from analysis of this codebase. **Written in Polish** (team language preference). Standards and this index are in English.*

### Tech Stack (`project/tech-stack.md`)
Technology choices with rationale for the **Business Rules for Motor Insurance** demo (`pl.decerto:motor-insurance` 4.3.0-SNAPSHOT) — a reference application for the **Higson BRMS** rules engine. Covers: Java 17 backend and a **TypeScript ~6.0.3 / Angular 22.1.0** frontend in `frontend/` plus LESS (with the single documented `theme.scss` Sass exception); Spring Boot 3.5.16 with `spring-boot-starter-higson-runtime` 4.3.1 and the rules snapshot layout under `src/main/higson/` (`@domains`, `@params`, `@functions`, `@tests`, `@profiles`, `@flows`) — including the warning that the snapshot is **not tracked in git** and that Higson Studio's database is the system of record; the UI library set (**Angular Material 22.1.1 + CDK**, with Bootstrap 3 / Font Awesome 4 / angular-tooltips / spin.js removed rather than replaced, and Manrope self-hosted); PostgreSQL as default datastore and H2 2.1.212 for demo/tests, with no ORM; Maven build producing `motor-insurance.war` with **`npm ci && ng build`** wired into `prepare-package` and the deliberately unpinned `maven-war-plugin`, plus the `higson-cli-maven-plugin` pull/push snapshot sync; the **Node version floor** and the fact that nothing in the repository enforces it; Docker / docker-compose topology (`db`, `studio`, `runtime-rest`, `web`) and the continuing absence of CI and linting; the three test suites and their honest limits; the key dependency table, version-pinning policy, and a migration path in which **frontend modernization is now marked done** and CI, the bundle budget and API docs are what remain.
**Read before** adding dependencies, bumping Higson or Spring Boot versions, touching the build, or making any technology decision.

### Architecture (`project/architecture.md`)
System architecture of the single-module Spring Boot WAR application. Covers: the layered MVC monolith with the **Adapter pattern** as the rules-engine boundary (API → Converter → Service → Domain → Adapter → Context → Higson Engine → JDBC), and the core principle that all pricing logic lives in Higson rules rather than Java; per-layer package breakdown under `pl.decerto.higson.demo.motor` (REST controllers `QuoteApi`/`DriverApi`/`VehicleApi`/`DictionaryApi`/`ProfilersApi`, DTOs, hand-written converters, `@SessionScope QuoteService` holding the `Quote` aggregate between requests, the domain model, `*Adapter` classes as the only code aware of Higson context paths, `MotorContext`, the `Motor` calculation orchestrator, and `GlobalExceptionHandler` — now `@RestControllerAdvice` returning `ErrorDto`, with the history of why); the **Angular 22 SPA inventory** — standalone components, the four HTTP services, the two HTTP interceptors, and the **`QuoteStore` signal store** that replaced the `$emit`/`$broadcast` event bus, including the `RELOADS_QUOTE` table that encodes the vehicle-cascade reload asymmetry; the end-to-end recalculation data flow with its two deliberate exceptions (blur-commit, model-only reload); external integrations; the deliberate absence of an application-owned DB schema and migrations, with the warning that snapshot-based versioning is aspirational because the snapshot is never committed; configuration layering; and deployment (WAR artifact with the frontend build wired in, `all_in_one` profile, Docker image, compose topology, limited observability, no CI).
**Read before** adding endpoints, changing the domain model, touching adapters or context paths, altering how the application talks to the rules engine, or changing how the SPA triggers recalculation.

### Profil LIFE — projekt konfiguracji (`project/higson-life-profile.md`)
> Wersja z 2026-08-13: wszystkie kwestie otwarte rozstrzygnięte jako **D-1…D-7**; zostało sześć
> punktów **W-1…W-6** do zweryfikowania przed implementacją. Po przeglądzie pod kątem polskiego
> rynku i demonstrowalności: kody są **pełnymi angielskimi nazwami** (`CRITICAL_ILLNESS`,
> `HOSPITAL_STAY`, warianty `BASIC`/`EXTENDED`/`COMPLETE`) zamiast skrótów, `MAIN_COVER` stoi obok
> `RIDERS` zamiast w nich, karencja ma poziom `cause` odwzorowujący zapis OWU „nie obowiązuje przy
> nieszczęśliwym wypadku", a `PER_PERCENT` odróżnia świadczenie za 1% uszczerbku od sumy. §11 wprowadza
> **budżet złożoności** (funkcja ≤80 linii, tabela ≤30 wierszy, flow ≤15 nodów) — wszystko mieści się
> z zapasem, dwa artefakty są blisko sufitu i mają opisaną drogę wyjścia.
⚠️ **Propozycja, nie stan faktyczny — w Higsonie nic z tego jeszcze nie istnieje.** Projekt
konfiguracji nowego profilu `LIFE` pokrywającego obie ścieżki demo życiowego. Zawiera: drzewo domeny
dzielące się na pierwszym poziomie na `INDIVIDUAL` (ochronne — `MAIN_COVER` plus siedem umów
dodatkowych w `RIDERS`) i `GROUP` (pracownicze — trzy warianty i trzynaście świadczeń), wraz z
uzasadnieniem, dlaczego świadczenia wiszą pod gałęzią, a nie pod wariantami; typy domenowe
`PRODUCT_LINE`/`RIDER`/`VARIANT`/`BENEFIT` z atrybutami i źródłami; rozdzielone konteksty
wejściowe, w których gałąź grupowa **nie ma dostępu** do danych osobowych, bo polisa grupowa nie
prowadzi indywidualnej oceny ryzyka; 29 tabel decyzyjnych pod prefiksem `demo.life.individual.*` / `demo.life.group.*` / `demo.life.underwriting.*` (taryfa,
czynniki ryzyka, dostępność, sumy, underwriting, antyselekcja) z pełnymi matrycami; sześć flow
modelujące algorytmy wyceny i underwritingu; osiem funkcji Groovy, każda z uzasadnieniem, dlaczego
tabela nie wystarcza; oraz **wersjonowanie datą przez Region `LIFE` z dwoma timeline'ami**
(`2026-01-01` i `2026-07-01`) obejmujące sześć tabel i jeden element domeny, ze scenariuszami demonstracyjnymi —
w tym takim, który pokazuje, że wersjonowanie jest selektywne. Osobno: zestaw testów regułowych,
siedem zapisanych decyzji i **zero otwartych kwestii produktowych**. **D-1** — `REFER` zwraca składkę
oznaczoną `binding = false`, `DECLINE` nie zwraca ceny, a uzasadnienie rozdziela się na
`reasonInternal` dla underwritera i `underwriting.customerMessage` dla klienta. **D-2** — wariant grupowy
wybiera pracownik, a `variant.code` pozostaje **iteracją w obu fazach**: rozkład wyborów
(`quote.group.enrolment`) wchodzi dopiero przy agregacji, faza programu nie ma własnej taryfy, wybór per
pracownik otwiera drugą oś antyselekcji wycenianą przez `group.factor.variantMix`, a `monthlyTotal`
zmienia nazwę na `monthlyTotalIfAll`, bo rzeczywisty koszt niesie nowe `programCost`.
**D-3** — suma umowy dodatkowej ma pułap względem głównej, ale **trójrodzajowy** (`RATIO` /
`ABSOLUTE` / `NONE`) sterowany tym samym `benefitKind`, który wybiera wzór na składkę; katalog sum
zostaje nietknięty, a naruszenie pułapu jest **błędem, nie cichym przycięciem**. **D-4** — region
`LIFE` obejmuje **oba** wymiary, ale rozłącznie: parametry odpowiadają na „ile wynosi", a
`@conf/.../@regions/` na „co istnieje", więc wersja 2 dokłada świadczenie `MENTAL_HEALTH_SUPPORT` jako **element
domeny**, zostawiając jego wiersze w tabelach wartości globalnymi i bezczynnymi w wersji 1 —
wzorzec wzięty z działającego MOTO (`DISCOUNTS/@regions/MOTOR/1/DE`). **D-5** — poprzednia ochrona
**skraca** karencję (nie znosi), pod trzema warunkami naraz: przerwa ≤ 30 dni, długość ≥ 1/6 miesięcy
i rodzaj umowy, przy czym PZ i NDP zawsze zatrzymują połowę. **D-6** — waluta nie jest wymiarem
(jedna, PLN), taryfa zostaje miesięczna, a **składka roczna jest wielokrotnością zaokrąglonej raty**,
nie liczbą liczoną osobno — konstrukcja usuwa całą klasę rozjazdów zamiast ją obsługiwać. **D-7** —
dochód wchodzi jako **drugi** pułap dla SZP i NDP, obowiązujący łącznie z produktowym przez `min`.
Z D-3/D-4/D-5/D-7 wyłania się nazwany wzorzec **„reguła bazowa × reguła modyfikująca"**: nowy wymiar
dokłada się drugą tabelą i złożeniem we flow, zamiast rozsadzać macierz bazową o kolejny poziom.
⚠️ D-2 unieważnia etykietę „Koszt programu miesięcznie" na obecnym ekranie grupowym SPA (W-4).
**Read before** touching anything related to the LIFE demo's rules, or before assuming any of this
exists in Studio — the frontend LIFE demo currently runs on TypeScript mocks, not on Higson.

### Profil PROPERTY — projekt konfiguracji (`project/higson-property-profile.md`)
> ⚙️ **Zbudowane i działające** w profilu `PROPERTY` (2026-08-21), zweryfikowane przeglądarką.
Trzecie demo aplikacji: **ubezpieczenie domu i mieszkania (detal)**, w którym osią nie jest taryfa,
tylko **analiza potrzeb klienta (APK) jako konfiguracja wersjonowana datą polisy**. Zawiera: drzewo
domeny `/PRODUCTS[HOME]/{COVERS,VARIANTS,QUESTIONS}`, gdzie **katalog 18 pytań APK to elementy
domeny**, a nie formularz; 32 tabele decyzyjne pod prefiksem `demo.property.*` (widoczność pytania,
mapowanie odpowiedzi na potrzeby, konfrontacja wyłączeń z sytuacją klienta, wyliczenie sumy
ubezpieczenia, taryfa, strefy, limity); 18 funkcji Groovy i dwa flow; oraz **region `PROPERTY`
z dwoma wersjami — `2026-01-01` i `2027-07-01`**, czyli terminem dostosowania z §2 uchwały KNF
nr 190/2026. Przesunięcie daty polisy przez ten próg zmienia **zestaw pytań** (9 → 16 widocznych),
**reżim APK** i **treść rekomendacji** — bez wdrożenia. Osiem decyzji **D-1…D-8**: granica
mury/elementy stałe jako **trzy niezależne parametry** (a nie jedna decyzja), suma liczona
z metrażu i stawki zł/m², strefa powodziowa jako **przełącznik dostępności** obok strefy
kradzieżowej jako czynnika, pytanie jako element domeny, wersjonowanie obejmujące **oba** wymiary
naraz, odmowa odpowiedzi jako trzeci stan, przekroczony budżet z **nazwaną luką**, franszyza
w dwóch rolach. §13 dokumentuje odstępstwa wymuszone przez API i przez pierwsze uruchomienie.
⚠️ Wszystkie stawki są **demonstracyjne** (W-2); jedyne liczby przepisane z OWU to limity z PZU §46.
**Read before** touching the property demo's rules, the APK question catalogue, or anything that
depends on the region version — a question added without `CONTEXT_PATH` and `VALUE_TYPE` will not
reach the engine.

### Vision (`project/vision.md`)
*Not created.* Add it to capture project mission, goals, target users, and long-term direction.

### Roadmap (`project/roadmap.md`)
*Not created.* Add it to capture development milestones, planned features, and priorities.

---

## Technical Standards

### Global Standards

Located in `.maister/docs/standards/global/`

These standards apply across the entire codebase, regardless of frontend/backend context.

**Read before** writing or reviewing any code in this repository, and before committing anything that might carry a secret.

#### Security (`standards/global/security.md`)
Secrets and credentials. Two standards: *Never Commit Credentials — Override at Invocation Time* (supply real tokens via `-Dhigson.studio.token=...` or a Maven `settings.xml` server entry; demo credentials acceptable only for disposable resources such as the throwaway H2 `sa/sa`); and *Resolved: The Studio Token Literal Was Removed Before `pom.xml` Was Committed* — the admin-authority JWT that once sat in the working tree's `pom.xml` was replaced with an empty default before the file first entered history, so it never reached the public remote. ⚠️ The property must **stay** empty, and nothing enforces that: no pre-commit hook, no secret scanning. Check `grep -c 'eyJ' pom.xml` before every `pom.xml` commit.
**Read before** committing `pom.xml`, adding any configuration that carries a token, or pushing this repository anywhere.

#### Error Handling (`standards/global/error-handling.md`)
Clear user-facing messages separated from internal detail, fail-fast on invalid state, typed exceptions over generic throws, centralized handling at boundaries, graceful degradation of non-critical paths, retry with exponential backoff, guaranteed resource cleanup.

#### Validation (`standards/global/validation.md`)
Server-side validation always enforced (client-side only for feedback), validate early at system boundaries, specific actionable error messages, allowlists over blocklists, type and format checks, input sanitization, business-rule validation, consistent enforcement across entry points.

#### Conventions (`standards/global/conventions.md`)
General: predictable project structure, documentation kept current with code, clean version control, configuration via environment variables, minimal dependencies, consistent review, testing expectations, feature flags, changelog updates, build only what is needed. Project-specific: the **required local toolchain baseline** — Java 17, Maven, Higson Studio 4.0.2+, and Node in the range Angular 22 accepts (`^22.22.3 || ^24.15.0 || >=26.0.0`), with the ⚠️ warning that nothing in the repository installs or selects a Node version and that the wrong one **fails opaquely inside `prepare-package` with no mention of Node**, plus the keg-only PATH caveat for the current development machine; build the executable WAR with the `all_in_one` profile; build output, IDE files and tool logs stay out of version control; commit and branch practice documented honestly as weak; releases ship as Docker images rather than git tags.

#### Coding Style (`standards/global/coding-style.md`)
General: naming consistency, automatic formatting, descriptive identifiers, small focused functions, uniform indentation, no dead code, no backward-compatibility shims unless required, DRY. Project-specific: tab indentation everywhere, UTF-8 everywhere, and keeping Java deliberately simple because it serves as reference material for Higson customers.

#### Commenting (`standards/global/commenting.md`)
Let code speak for itself, comment sparingly and only for non-obvious rationale, no change-log or history comments in source. Project-specific: explanatory comments are expected on non-obvious build and configuration entries (`pom.xml`, properties files).

#### Minimal Implementation (`standards/global/minimal-implementation.md`)
Build only what is needed now, every artifact has a clear purpose, delete exploration artifacts, no future stubs, no speculative abstractions, review scope before commit, unused code is debt (YAGNI).

#### Documentation (`standards/global/documentation.md`)
Project documentation under `.maister/docs/project/` is written in **Polish** (team preference) while standards and `INDEX.md` are in English; keep the docs index in sync whenever documentation is added, removed, or significantly changed.

#### Licensing (`standards/global/licensing.md`)
Apache-2.0 notice obligations: any redistribution — including derived demos and forks — must ship a copy of the license, retain copyright/patent/trademark/attribution notices, and prominently state which files were changed.

---

### Higson Standards

Located in `.maister/docs/standards/higson/`

**The most project-specific category.** These are the rules-engine conventions that define this codebase: where the business logic actually lives, how Java is allowed to touch the engine, and how the snapshot moves between the working tree and Higson Studio.

**Read before** touching anything under `src/main/higson/`, any `adapter`/`context` class, any pricing behaviour, or before making any claim about where a piece of business logic is implemented.

#### Snapshot & Studio Sync (`standards/higson/snapshot.md`) — read this first
Five standards: *Snapshot Directory Is the Single Source of Truth* (`src/main/higson` addressed everywhere via `${higson.snapshot.dir}`; never a second copy, zip fixture, or hard-coded path); ***The Snapshot Is NOT Version-Controlled — Studio Is the System of Record*** (never tracked in git, untracked in the working tree, and absent from `.gitignore` — so a Java change and its matching rule change are never atomically committed, a fresh clone has no rules until `mvn higson-cli:pull`, and rule-dependent Java must be read assuming the Studio instance holds the matching rules); *Higson CLI Runs On Demand With Safe Defaults* (no `<executions>`, nothing deleted, no auto-commit without explicit opt-in); *Version Business Logic Through the Snapshot, Not SQL Migrations* (no Flyway/Liquibase, ever — subject to the not-committed caveat); *Bump the Higson Runtime and CLI Plugin in Lockstep* (runtime + CLI plugin move together with `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh`, `Readme.md`).

#### Java ↔ Higson Integration Boundary (`standards/higson/integration.md`)
Five standards: *Higson Runtime API Confined to adapter / context / service* (`io.higson.*` may appear only in `adapter`, `context`, `service`, and the `example` orchestrator — `api`, `dto`, `converter`, `domain` stay Higson-free); *One Adapter Per Domain Class, Exposing Canonical Paths* (each aggregate gets one `Adapter` with a single-argument constructor and a fluent `getMapping()`; child aggregates nest, collections use `CollectionAdapter`; canonical path segments are defined here and nowhere else); *Wire Only the DataSourceProxy Manually* (everything else comes from starter auto-configuration; the `BeanPostProcessor` `@Bean` must be `static`); *Configure the Engine Only Through `higson.*` Properties* (keep `dev-mode-enabled=false`, `function.validate-arguments=true`, `diagnostic.profiler.enabled=false`; never set dialect or schema explicitly); *Reference Rule Codes Through Java Constants* (constants holder or enum, plus the varargs `HigsonContext(path, value, ...)` constructor).

> ⛔ **The `function.validate-arguments=true` clause is correct — do not "fix" it.** It was challenged during the frontend migration and the challenge was **disproven empirically**: the running application with the property enabled returns a complete quote. The observed failure came from a hand-built test engine reading the snapshot directly, not from the configuration. See `.maister/tasks/migrations/2026-08-06-angularjs-to-angular/implementation/work-log.md` (Group 1) for the full differential.

#### Authoring Rules in the Snapshot (`standards/higson/rules-authoring.md`)
Seven standards governing `.def`/`.mat` decision tables and Groovy functions: *Pricing Logic Belongs in Higson Rules, Not Java* (a tariff change must never require recompiling); *Parameter Code Mirrors the Snapshot File Path* (path with `/` → `.`, under a `demo.motor.*` business prefix); *Input Levels Bind `property == source` to Canonical Model Paths* (58 of 58 levels; paths always start at an adapter-exposed root); *Decision Tables Are Cacheable With a Wildcard Default Row* (`cacheable = true`, semicolon matrix with `#matrix for parameter` header, `nullable = false` tables open with a `*` row); *Groovy Function Header Contract* (generated TOML header, `#Full code` matching the path, `ctx` always the first argument, explicit `return`); *Rule Bodies Read Only `ctx` and the `higson` Helpers* (zero imports, zero references to application Java classes); *Known Drift: Legacy ParamContext Binding* (29 of 36 functions still on `org.smartparam.*` — migrate opportunistically, no dedicated sweep).

---

### Backend Standards

Located in `.maister/docs/standards/backend/`

These standards apply to backend code (REST layer, services, domain model, data access).

**Read before** adding or changing a controller, DTO, converter, domain class, Spring bean, or anything that touches the database.

#### Java Language & Build Conventions (`standards/backend/java.md`)
Java 17 via `maven.compiler.release`; explicit version pinning for dependencies the Spring Boot BOM does not manage; layer suffix in class names matching the package (`*Api`, `*Adapter`, `*Service`, `*Dto`); retain method parameter names in bytecode; explicit imports with `java.*` first; SLF4J logger as `private static final`.

#### Spring Framework Conventions (`standards/backend/spring.md`)
Constructor injection with `private final` fields; stereotype annotation matches the layer; prefer starter auto-configuration over manual bean wiring; two-tier external property override (classpath defaults plus optional `${user.home}/conf/` overrides); fail fast with custom runtime exceptions and handle them centrally in `@ControllerAdvice`.

> Note: `GlobalExceptionHandler` is now `@RestControllerAdvice` returning an `ErrorDto` body. A plain `@ControllerAdvice` handler returning `String` makes Spring treat the message as a **view name** — which is exactly the defect that produced empty error banners before the frontend migration.

#### API Design (`standards/backend/api.md`)
Project-specific: REST controllers are named `*Api`, never `*Controller`; class-level `@RequestMapping` root with verb-specific method mappings; every mutation is a `PUT` of a single field; DTOs cross the API boundary, never domain objects. General: RESTful resource modeling, consistent naming, versioning, plural nouns for collections, limited nesting, query parameters for filtering, correct status codes, rate-limit headers.

#### Models (`standards/backend/models.md`)
Project-specific: mutable POJOs — no records, builders, or Lombok; Spring `Converter<S,T>` for domain-to-DTO mapping; `toString` via Commons `ToStringBuilder`. General: clear entity naming, timestamps, database constraints, appropriate column types, indexed foreign keys, multi-layer validation, explicit relationships, practical normalization.

#### Queries (`standards/backend/queries.md`)
Project-specific: no ORM and no application-owned schema (the database belongs entirely to Higson); dictionary lookups go through the Higson engine rather than SQL. General: parameterized queries, N+1 avoidance, select only needed columns, strategic indexing, transaction boundaries, query timeouts, caching for expensive queries.

#### Migrations (`standards/backend/migrations.md`)
Leads with *There Are No Database Migrations — By Design* (the schema is Higson's; do not add Flyway or Liquibase). Remaining general guidance applies only if that ever changes: reversible migrations, small focused changes, zero-downtime awareness, schema separated from data migrations, careful index creation, descriptive names, migrations in version control.

---

### Frontend Standards

Located in `.maister/docs/standards/frontend/`

These standards apply to the **Angular 22** SPA under `frontend/` and its build. They were rewritten in August 2026 for that stack; nothing in this category describes AngularJS any more.

**Read before** adding a component, service or piece of shared state to the SPA, changing styles, or touching the npm/Angular CLI asset pipeline.

#### Components (`standards/frontend/components.md`)
Standalone components — **never write `standalone: true`**, it is the v22 default; signal inputs via `input()` / `input.required()` and `output()` instead of `@Input`/`@Output`; `inject()` rather than constructor parameters; built-in `@if` / `@for` / `@switch` control flow with a **mandatory stable `track`** and no client-side sort (the backend already orders); `OnPush` on every component and host bindings in the `host` object; co-located same-named files with no `.component` suffix and an `app-root` selector that is never a real HTML tag. ⛔ Contains the load-bearing behavioural rule: **recalculation runs through `QuoteStore`**, and the **vehicle-cascade asymmetry** (only a *model* change reloads the quote) is encoded as an exhaustive `RELOADS_QUOTE` table — losing it is a failed migration. Ends with an open item: `AC-S1`'s explicit-`OnPush` requirement versus current Angular guidance.
**Read before** writing any component or template.

#### Services & HTTP (`standards/frontend/services.md`)
HTTP confined to `@Injectable` `*Service` classes under `frontend/src/app/services/`, with components never injecting `HttpClient`; ⚠️ ***never unwrap a response body*** — `HttpClient` returns the body directly, so a leftover `response.data` yields `undefined` silently rather than erroring; relative URLs with **no leading slash** mirroring the backend `@RequestMapping` roots, with query parameters in the `params` option object; and the ⛔ two-way body-type rule — pass raw strings for `@RequestBody String` endpoints (never `JSON.stringify`, which stores the quotes) but a `Date` **object** for `PUT driver/birthDate` (a string yields HTTP 415).
**Read before** adding any server call.

#### State Management (`standards/frontend/state.md`) — NEW
Signals rather than `BehaviorSubject`; `computed()` for derived state and `linkedSignal()` for local editable state that follows a source; ⛔ ***read a `resource()` through `hasValue()`, never a bare `value()`*** — in Angular 22 a resource in the **error** state **throws `ResourceValueError`** rather than returning `undefined`, a misconception that bit this project three times and once produced a green test over broken code; one root-provided `QuoteStore` (two `resource()`s plus a mutation-error signal) instead of an event bus, with no client-side quote model; cross-cutting request state (pending-request counter, mutation errors, no retry) in functional HTTP interceptors.
**Read before** introducing shared state, reading a resource, or writing a test around a failed load.

#### TypeScript (`standards/frontend/typescript.md`) — NEW
`strict: true` and `strictTemplates: true` — ⚠️ **Angular CLI 22 does not emit `"strict": true`** despite `--strict` defaulting to true, so both must be set by hand and must not be removed; no `any`; model the wire honestly including nullability (`limitRight` and `description` are nullable, the vehicle cascade fields are non-null primitive `int`); prefer inference over annotation, with `DictionaryEntryDto.code` as the counter-case (it arrives as a **String**); and the reminder that strictness is load-bearing — it caught a defect no test did.
**Read before** writing TypeScript or changing the compiler configuration.

#### Build & Assets (`standards/frontend/build.md`)
⚠️ ***Ship no HTML `<base>` element and set no `baseHref`*** — the servlet context path is a runtime value, and the CLI's default `<base href="/">` would 404 every asset under `/demo` and `/app`; verified across all three context paths from one WAR. Stylesheet assets live under `frontend/src/styles/`, **not** `public/`, because `url()` resolves relative to the partial that wrote it. The build is `npm ci && ng build` in `frontend/`, wired into Maven `prepare-package` via `exec-maven-plugin` with `<workingDirectory>`, with the output pulled into the WAR by a **deliberately unpinned** `maven-war-plugin` `<webResources>` block (Spring Boot manages it at 3.4.0 and its `pluginManagement` carries an `<archive>` block an explicit version would clobber). Generated assets are never committed; exact pins for every `@angular/*`, `rxjs` and `typescript` (`typescript@latest` is 7.0.2 and breaks the build; `less` must be declared explicitly because `@angular/build` ships `sass`); the Node range is enforced by `engines` + `engine-strict`, and by nothing else.
**Read before** touching the build, adding a dependency, or moving an asset.

#### CSS (`standards/frontend/css.md`)
LESS is the styling language, with `frontend/src/styles.less` as the sole global entry point, compiled by the Angular CLI as part of `ng build` — the Gulp `less` task and the tracked `src/main/webapp/styles/styles.min.css` are gone. **One scoped Sass exception**: `frontend/src/theme.scss`, the only `.scss` file, because Angular Material's `mat.theme()` API is Sass-only and has no LESS equivalent; it is listed **first** in `angular.json` so LESS wins the cascade. Plus: appearance is not a parity gate — the UI moved wholesale to Angular Material by decision, and a visual difference is never on its own a finding.
**Read before** adding a stylesheet or a design token.

#### Accessibility (`standards/frontend/accessibility.md`)
Semantic HTML first, full keyboard navigation, sufficient color contrast, alt text and form labels, screen reader testing, ARIA only when native semantics are insufficient, logical heading structure, deliberate focus management.

#### Responsive Design (`standards/frontend/responsive.md`)
Mobile-first approach, standard breakpoints, fluid layouts, relative units, cross-device testing, touch-friendly targets, mobile performance budgets, readable typography, content prioritization on small screens.

---

### Testing Standards

Located in `.maister/docs/standards/testing/`

> ⚠️ **Automated tests are new here and nothing gates them.** All three suites arrived with the August 2026 frontend migration, and **there is still no CI** — no test run has ever blocked a merge. Before the migration, zero test files were tracked in git. Treat "the tests pass" as meaning someone ran them locally.

**Read before** writing any test, and before making any statement about this repository's test coverage.

#### Test Writing (`standards/testing/test-writing.md`)
General: test behavior not implementation, clear descriptive names, mock external dependencies, fast execution, risk-based selection, balance coverage against velocity, critical-path focus, appropriate depth per layer.

Project-specific, **Java**: *Current State — Young Suites, and No CI to Gate Them* (a table of all three suites with their sizes and run commands, plus the honest limits: only the Java suite runs during a build, the E2E harness needs a running application and a populated snapshot, and MockMvc pins 18 of 21 endpoints); *Java Tests Use JUnit 5 via `spring-boot-starter-test`* (the former JUnit 4/5 conflict is **resolved**; surefire pins `-Duser.timezone=UTC` because the seeded `dateOfBirth` is timezone-relative); *Test Classes Are Package-Private With Static Assertions*; *Test Layout Mirrors the Main Source Package*.

Project-specific, **frontend unit tests** (Vitest through the Angular CLI): ⛔ run them with `ng test --watch=false` — raw `npx vitest run` fails with `ReferenceError: describe is not defined` because the CLI supplies the environment; ⛔ select with `--filter`, because `--include` drops the Angular compiler plugin and fails on `.less`; ⛔ use `TestBed.tick()` + `httpTesting.match()` rather than `ApplicationRef.whenStable()`, which **deadlocks** on the outstanding `GET quote` a recalculation leaves behind; ⚠️ `HttpTestingController.match()` **consumes**, so asserting "requests are in flight" that way passes for the wrong reason; assert that failures are *handled*, not merely that nothing rendered; prove a test fails under the mutation it exists to catch, and scramble fixtures that could make it vacuous; keep probe code out of `frontend/src/`.

Project-specific, **end-to-end goldens** (`e2e/`, Playwright): select by test id and accessible role rather than markup; goldens are committed evidence while screenshots are documentation only and never a gate; **a golden is never edited to make a replay pass**.

#### Rule Tests (`standards/testing/rule-tests.md`)
Testing the part of this system that actually holds the business logic. Three standards: *Business Logic Is Covered by Higson Rule Tests* (coverage lives in `src/main/higson/@tests` — 9 files run by Studio or the runtime — not in Java unit tests; a new business rule's test belongs in the snapshot, **and because the snapshot is not committed those tests are not visible in the repository either**); *Run Rule Tests Off the Snapshot, Not a Live Database* (boot the engine from the snapshot directory via `HigsonSnapshotDataSource` for offline, reproducible runs with no database and no network); *Tests Read the Canonical Snapshot Directory Directly* (consume the same `${higson.snapshot.dir}` the CLI syncs — never copy the snapshot into test resources).

> ⚠️ A snapshot-booted engine is **not** identical to the live runtime. Enabling function argument validation on a hand-built snapshot engine makes a flow-invoked Groovy function fail to bind its `ParamContext` argument, while the same configuration against a live database works. Do not generalise a snapshot-engine failure into a production configuration change.

---

### Deployment Standards

Located in `.maister/docs/standards/deployment/`

How the deployable artifact is built, containerized, configured, and published.

**Read before** changing the `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh`, packaging configuration, or any port or environment variable used at deploy time.

#### Container Image & Compose (`standards/deployment/docker.md`)
Minimal pinned JRE base image (`eclipse-temurin:17-jre-jammy`, JRE not JDK) with maintainer `LABEL`, explicit `EXPOSE`, and runtime-overridable `JAVA_OPTS`; multi-arch publishing via `docker buildx` for `linux/amd64` and `linux/arm64` with both version and `latest` tags, registry credentials passed as script arguments; pinned third-party service images on the dedicated `higson-net` bridge network with fixed `container_name` and `depends_on` (only the locally built application image may use `latest`); a single canonical HTTP port `48080` consistent across packaged properties, container properties, `EXPOSE`, and the compose mapping.

#### Artifact Packaging & Container Config (`standards/deployment/packaging.md`)
WAR packaging with a stable, version-free `finalName` (`motor-insurance`) so scripts reference `target/motor-insurance.war` unchanged across releases; executable packaging is the opt-in `all_in_one` Maven profile (`spring-boot-maven-plugin` repackage with `<executable>true</executable>`) that must be used before building a Docker image; container config through `${ENV_VAR}` placeholders with inline `${VAR:default}` fallbacks, externalizing only genuinely environment-dependent values.

---

## Migration Record

The AngularJS 1.8.3 → Angular 22 migration is documented in full under
`.maister/tasks/migrations/2026-08-06-angularjs-to-angular/`:

| Document | Contents |
|---|---|
| `analysis/requirements.md` | Decisions C-1…C-11, prerequisites P-1…P-4, scope boundaries |
| `implementation/spec.md` | The contract — defect register DR-01…DR-27, non-deviations ND-1…ND-8, the frozen 21-endpoint REST surface |
| `implementation/implementation-plan.md` | 14 task groups, the trap index, the traceability matrix |
| `implementation/work-log.md` | Complete execution history, including every correction and deviation |
| `verification/parity-verdict.md` | The **PASS** verdict: 206 golden keys compared, zero untraceable differences |
| `release-notes.md` | User-visible changes, open decisions (bundle budget, `AC-S1`), and deferred items |

**Read the defect register before "fixing" any odd-looking frontend behaviour** — eight of them (ND-1…ND-8) are deliberate and must survive.

---

## How to Use This Documentation

1. **Start Here**: Always read this INDEX.md first to understand what documentation exists
2. **Project Context**: Read relevant project documentation before starting work
   - Tech stack for understanding technology constraints
   - Architecture for understanding system design
3. **Standards**: Reference appropriate standards when writing code
   - Global standards apply to all code
   - **Higson standards apply whenever business logic, rules, or the snapshot are in scope** — which in this codebase is most of the time
   - Domain-specific standards (backend/frontend/testing/deployment) apply to relevant code
4. **Keep Updated**: Update documentation when making significant changes
5. **Customize**: Adapt all documentation to this project's specific needs

## Updating Documentation

- Project documentation should be updated when goals, tech stack, or architecture changes
- Technical standards should be updated when team conventions evolve
- Always update INDEX.md when adding, removing, or significantly changing documentation
- Use `/maister:standards-update` to capture new conventions, `/maister:standards-discover` to auto-discover standards from the codebase

---

## Documentation Priority

When making development decisions, follow this priority order:

1. **Project documentation** in `.maister/docs/` (highest priority) — team decisions and project-specific requirements
2. **Code patterns** visible in the codebase — how the team actually implements things
3. **User's direct instructions** — specific guidance for the current task
4. **General best practices** (lowest priority) — industry defaults when no specific guidance exists

---

**Maintained by**: Documentation Manager skill
**Last regenerated**: 2026-08-10 (AngularJS → Angular 22 migration, task group G13)
