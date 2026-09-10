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
Technology choices with rationale for the **Business Rules for Motor Insurance** demo (`pl.decerto:motor-insurance` 4.3.0-SNAPSHOT) — a reference application for the **Higson BRMS** rules engine. Covers: Java 17 backend and a **TypeScript ~6.0.3 / Angular 22.1.0** frontend in `frontend/` plus LESS (with the single documented `theme.scss` Sass exception); Spring Boot 3.5.16 with `spring-boot-starter-higson-runtime` 4.3.1 and the rules snapshot layout under `src/main/higson/` (`@domains`, `@params`, `@functions`, `@flows`, `@profiles`, `@tags`) — including the warning that Studio's database is where rules are changed, so the committed tree drifts the moment someone edits without exporting; the UI library set (**Angular Material 22.1.1 + CDK**, with Bootstrap 3 / Font Awesome 4 / angular-tooltips / spin.js removed rather than replaced, and Manrope self-hosted); PostgreSQL as default datastore and H2 2.1.212 for demo/tests, with no ORM; Maven build producing `motor-insurance.war` with **`npm ci && ng build`** wired into `prepare-package` and the deliberately unpinned `maven-war-plugin`, plus the `higson-cli-maven-plugin` pull/push snapshot sync; the **Node version floor** and the fact that nothing in the repository enforces it; Docker / docker-compose topology (`db`, `studio`, `runtime-rest`, `web`) and the continuing absence of CI and linting; the three test suites and their honest limits; the key dependency table, version-pinning policy, and a migration path in which **frontend modernization is now marked done** and CI, the bundle budget and API docs are what remain.
**Read before** adding dependencies, bumping Higson or Spring Boot versions, touching the build, or making any technology decision.

### Architecture (`project/architecture.md`)
System architecture of the single-module Spring Boot WAR application. Covers: the layered MVC monolith with the **Adapter pattern** as the rules-engine boundary (API → Converter → Service → Domain → Adapter → Context → Higson Engine → JDBC), and the core principle that all pricing logic lives in Higson rules rather than Java; per-layer package breakdown under `pl.decerto.higson.demo.motor` (REST controllers `QuoteApi`/`DriverApi`/`VehicleApi`/`DictionaryApi`/`ProfilersApi`, DTOs, hand-written converters, `@SessionScope QuoteService` holding the `Quote` aggregate between requests, the domain model, `*Adapter` classes as the only code aware of Higson context paths, `MotorContext`, the `Motor` calculation orchestrator, and `GlobalExceptionHandler` — now `@RestControllerAdvice` returning `ErrorDto`, with the history of why); the **shared APK layer** (`pl.decerto.higson.demo.apk`) whose `ApkAnalysis` runs the needs analysis for all three lines and **knows no rule at all**; the **Angular 22 SPA inventory** — standalone components, the shared `step-bar` / `apk-questionnaire` / `apk-recommendation`, the **three-step flow (needs analysis → recommendation → premium)** with every step always reachable, the policy date pinned above the bar, and the **proposed cover's premium shown on the recommendation screen** — one figure for one proposal, which rec. 8.2.d requires rather than forbids, with variant comparison kept to step three; the four HTTP services, the two HTTP interceptors, and the **`QuoteStore` signal store** that replaced the `$emit`/`$broadcast` event bus, including the `RELOADS_QUOTE` table that encodes the vehicle-cascade reload asymmetry; the end-to-end recalculation data flow with its two deliberate exceptions (blur-commit, model-only reload); external integrations; the deliberate absence of an application-owned DB schema and migrations, with the warning that snapshot-based versioning is aspirational because the snapshot is never committed; configuration layering; and deployment (WAR artifact with the frontend build wired in, `all_in_one` profile, Docker image, compose topology, limited observability, no CI).
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
⚙️ **Zbudowane i działające**, łącznie z obiema fazami D-2. Profil `LIFE` istnieje w Studio wraz
z regionem o dwóch wersjach; §13 spisuje trzy rzeczy dołożone po dacie dokumentu (`demo.life.tariff.version` —
nazwa obowiązującej wersji taryfy, dzięki której ekran nie zna daty progu; oraz
`demo.life.individual.rider.premium` — składka JEDNEJ umowy dodatkowej, bez której podsumowanie
wypisywało każdą wybraną umowę za 0,00 zł) oraz **fazę przystąpień (§13.3)** — rozkład wyborów
pracowników, z którego biorą się `insuredCount` i `programCost`, a przez `factor.variantMix` także
druga oś antyselekcji: ten sam skład osobowy przy wszystkich na najbogatszym wariancie kosztuje
o 30% więcej. Projekt
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
**§14 (2026-08-27)** takes the needs analysis out of Groovy — six functions and the
`apk.recommend` flow deleted, `suggestSum` kept — and adds **`demo.life.apk.cover.equivalent`**,
a two-row table translating the individual dictionary into the group one (`MAIN_COVER` → `DEATH`).
Without it a need met by the base group benefit reported as unmet and the raw code reached the
client in a customer-facing sentence.
**Read before** touching anything related to the LIFE demo's rules, and read **§13 and §14** before
assuming a given piece exists in Studio — the body of the document is the design, §13 and §14 are
what was built afterwards, and the two are close but not identical.

### Profil PROPERTY — projekt konfiguracji (`project/higson-property-profile.md`)
> ⚙️ **Zbudowane i działające** w profilu `PROPERTY`, zweryfikowane przeglądarką.
>
> ⛔ **STAN NA 2026-09-08 — czytaj go zamiast akapitu poniżej.** Dokument jest pisany
> chronologicznie i akapit otwierający opisuje projekt z sierpnia, nie dzisiejszą konfigurację.
> Dziś w profilu `PROPERTY` jest: **33 tabele decyzyjne, 19 funkcji Groovy, 3 flow, ZERO regionów**,
> a drzewo domeny to `/PRODUCTS[HOME]` z trzema gałęziami — `APK[NEEDS]` (9 atrybutów reguł +
> **7 pytań** jako elementy), `COVERS` (7 pokryć, każde z własnym kompletem tabel) i `VARIANTS`
> (BASIC/STANDARD/COMPLETE, a skład pakietu to **referencje** do pokryć, nie wiersze tabeli).
> **Trzynaście pól przedmiotu ubezpieczenia nie jest elementami domeny** — siedzą na sztywno
> w `Property.SUBJECT_FIELDS`, świadomie, bo to nie jest analiza potrzeb. Gałęzie `SECTIONS`,
> `SUBJECT`, `FACTORS`, `ZONES` i `USES` **nie istnieją**; tabele czynników i stref żyją dalej
> i są używane w taryfikacji, tyle że nie są punktem startu w drzewie. Region `PROPERTY`
> **został usunięty 8 września 2026** (§33.1) — wersjonowanie datą polisy demonstrują MOTOR i LIFE.
> §16…§33 opisują drogę tutaj; §2/§4/§6/§7/§8/§9 są nieaktualne.

Trzecie demo aplikacji: **ubezpieczenie domu i mieszkania (detal)**, w którym osią nie jest taryfa,
tylko **analiza potrzeb klienta (APK) jako konfiguracja wersjonowana datą polisy**. Zawiera: drzewo
domeny `/PRODUCTS[HOME]/{COVERS,VARIANTS,QUESTIONS}`, gdzie **katalog 18 pytań APK to elementy
domeny**, a nie formularz; 32 tabele decyzyjne pod prefiksem `demo.property.*` (widoczność pytania,
mapowanie odpowiedzi na potrzeby, konfrontacja wyłączeń z sytuacją klienta, wyliczenie sumy
ubezpieczenia, taryfa, strefy, limity); 18 funkcji Groovy i dwa flow; oraz **region `PROPERTY`
z dwoma wersjami — `2026-01-01` i `2027-07-01`**, czyli terminem dostosowania z §2 uchwały KNF
nr 190/2026. Przesunięcie daty polisy przez ten próg zmienia **zestaw pytań** (5 → 12 widocznych po §16; przed nim 9 → 16),
**reżim APK** i **treść rekomendacji** — bez wdrożenia. Osiem decyzji **D-1…D-8**: granica
mury/elementy stałe jako **trzy niezależne parametry** (a nie jedna decyzja), suma liczona
z metrażu i stawki zł/m², strefa powodziowa jako **przełącznik dostępności** obok strefy
kradzieżowej jako czynnika, pytanie jako element domeny, wersjonowanie obejmujące **oba** wymiary
naraz, odmowa odpowiedzi jako trzeci stan, przekroczony budżet z **nazwaną luką**, franszyza
w dwóch rolach. §13 dokumentuje odstępstwa wymuszone przez API i przez pierwsze uruchomienie, **§14 — zdjęcie analizy potrzeb z Groovy'ego (2026-08-27)**: siedem funkcji i flow `apk.recommend` usunięte, wszystkie tabele nietknięte, a **§15 — domknięcie procesu ofertą i polisą (2026-08-28)**: czwarty krok tylko w tym profilu, nowa tabela `demo.property.offer.terms` **kluczowana kodem reżimu**, dzięki czemu wersjonuje się bez własnego progu — po 1 lipca 2027 oferta niesie załącznik z analizą potrzeb (rek. 8.7). Numer dokumentu celowo NIE jest regułą; wystawienie polisy zamraża wariant i składkę.
⚠️ Wszystkie stawki są **demonstracyjne** (W-2); jedyne liczby przepisane z OWU to limity z PZU §46.
**§16 (2026-09-03)** rebuilds the APK questionnaire to the analyst's notes: the multi-select
`CIRCUMSTANCES` becomes five yes/no need questions in a **new, unversioned section `NEEDS_BASE`**
(what insurers ask today), `NEEDS` keeps only what the KNF threshold adds; the object
characteristics move out of the questionnaire into a collapsed, still-editable "data we already
have" strip driven by a new `SECTION.KIND` attribute; and — the load-bearing correction —
**the client's expected premium stops filtering the recommendation**. It used to downgrade the
proposal to the richest variant inside budget, making the scope depend on price; now the best-fit
variant is always proposed and the cheaper one rides alongside as an alternative.
Three new tables: `apk.need.conflict`, `sum.valueGap`, `apk.question.blocker`.
**§17 (2026-09-03)** cuts the configuration down to what a client can take in at a glance, and
finishes moving the recommendation out of Java. The `QUESTION` type goes from **12 attributes to
5** — the seven that were an identical `$p` pointer on every question become one row of a new
catalogue table, and `KIND` absorbs `VALUE_TYPE` (`NUMBER` splits into `INTEGER`/`DECIMAL`);
`COVER` goes 7→3 and `VARIANT` 4→2 by dropping attributes Java never read. Thirteen APK tables
become eight. And **`demo.property.apk.answer` now names the package** each answer calls for:
Java walks the answers, the table decides, and the richest indicated package wins — verified live,
one cell edited in Studio changes the recommendation with no restart. The table is the only
authority; an under-covering package is reported as a gap, never silently upgraded.
**§18 (2026-09-03)** reshapes the flow: **needs → subject → recommendation-with-premium → offer**.
The recommendation and premium steps merge into one view (rec. 8.2.d is one decision, not two
screens), the object characteristics become their own step 2 rendered by the same questionnaire
component under a `kind` input, and step 1 becomes a **dependent questionnaire** — a new
`revealedBy` column chains each question to the one whose *settlement* reveals it. Settlement, not
answer: `Answer.isSettled()` counts a refusal too, or one skipped optional question would stall
the rest forever. Demo seed answers are gone from the questionnaire and kept only on the subject
step, where they are genuinely prior-policy data.
**§19 (2026-09-03)** aligns the demo with the PM's spec `higson_apk_demo_spec_1.7.docx`, which
**outranks working agreements**. Three gaps closed: step 1 becomes **one question per screen** with
a counter and its own navigation (the spec asks for a survey, not a form); step 3 is reordered to
**packages-with-premiums → recommendation → justification**; and the recommendation component
reappears in the closing summary beside the automatic note on whether the client took it. `HELP`
leaves the domain type — a question now carries **four attributes**; the deleted Polish texts are
archived in §19.4. ⚠️ **W-26**: `@if` has no key, so Angular reused the `mat-button-toggle-group`
node between questions and two consecutive identical answers silently never reached the server —
`@for … track question.code` over a one-element list is the fix, guarded by a mutation-proven test.
**§20 (2026-09-03)** takes the technical band off the front page. Policy date, regime, legal basis
and the counters move into a right-hand **drawer** (closed by default, Esc/scrim/✕ to close) behind
a quiet trigger carrying the date and regime name; the front page gets a **client card** built from
the `policyholder` the quote already carried, with its CRM source rendered as part of the card
rather than a footnote. ⛔ The regime-changed marker stays **on the trigger**, because crossing the
KNF threshold has to be noticeable while the drawer is shut.
**§21 (2026-09-03)** strips the screen's commentary about itself — the lead under the title, the
drawer's lead, and the "not typed in" half of the sum note. The claim they made is true and is
demonstrated by the demo itself; printed beside the title it spoiled the point and read as a tool
advert inside a sales process. Provenance copy stays (CRM source, "prefilled from the previous
policy") — that is rec. 8.3, not a boast. ⚠️ The demo picker still narrates the engine in its
subtitle and in the moto/life tiles; only the property tile was rewritten.
**§22 (2026-09-04)** drops the "prefer not to say" control and centres the single question.
Refusal was load-bearing in a way that is easy to miss — it *settled* a question without answering
it, which is what let an optional one (expected premium, property value) pass the reveal chain;
its role moves to **optionality itself**, so removing the button does not dead-end those two.
`Answer.refused` and its endpoint stay — the motor and life demos still use them. ⚠️ **W-27**:
changing a table's columns renumbers its rows, and a later update with a stale id silently creates
a **duplicate row** rather than failing — that is how one questionnaire question ended up
pre-answered. Re-read the matrix after any column change. Step 2 is centred like the questionnaire, and
**D-15** puts the recommendation ABOVE the packages — a deliberate departure from the letter of
spec §3's ordering in favour of the priority sentence in the same paragraph ("do not show the
client packages to choose from before they have seen the recommendation and its justification").
**§23 (2026-09-04)** makes the questionnaire's sections visible — a section name above the counter
and a progress bar **split into one segment per section**, so crossing the KNF threshold visibly
adds two whole areas rather than seven questions from nowhere. And **D-16 reverses §17.1**: a
domain attribute pointing at a rule is the tree's *table of contents*, not dead config, so the
attributes removed there for "Java does not read them" come back and more join them — most
importantly `VARIANT.PREMIUM` → the pricing flow, and a `PRODUCT` node that had **no attributes at
all**. The criterion is now "remove attributes that point at no rule", not "that Java does not read".
**§24 (2026-09-04)** narrows the demo twice. The questionnaire becomes exactly spec §2's seven
questions: the cover period moves to step 2 (it feeds the period factor, so deleting it would
leave the pricing without a basis) and the whole EXCLUSIONS section goes — which costs rec. 8.2.c
half its material. And **D-17 removes region version 1**: one regime, always KNF, so the settings
drawer and the property `effective-date` component are gone. ⛔ That retires §12 — the date
threshold that changed the questionnaire without a deployment is no longer demonstrable here
(motor and life still version by date). ⚠️ There is no MCP call to delete a region version;
"removing version 1" is `detach_from_region` on everything attached, and detaching keeps the
ACTIVE version's content — **verify which one survived** rather than assuming.
**§25 (2026-09-04)** removes the "before you decide" warnings block from **all three demos**, and
its whole path with it — `ApkWarningDto`, the `warnings` field on model and DTO, Java's `Warning`,
`ApkAnalysis.checkExclusions`, the per-profile exclusion tables and the reasons that pointed at the
block. Rec. 8.2.c leaves the screen; the client's valuation vs. the computed sum survives as a
sentence in the justification. ⚠️ **W-28**: `target/motor-insurance/` is never cleaned — the war
plugin adds to it — so old `main-*.js` bundles keep shipping and a cached browser tab silently runs
weeks-old code instead of 404-ing. `mvn clean package`, and check the **served** bundle, not the WAR.
**§26 (2026-09-04)** drops the recommendation component from the offer step. §19.2 had added it to
satisfy spec §5.4 ("the same component in the summary"), which was reading the requirement
literally instead of by substance — the offer already carries the chosen premium in its header,
the agreement-with-recommendation note, and the needs and justification in its "client needs
analysis" attachment. The component beside it repeated all of that a second time on one screen.
**§27–§28 (2026-09-04)** drop the legal-basis line from the justification note, and stop the
browser caching `index.html` — a servlet filter now sends `no-cache` for `/` and `*.html`, without
which a tab kept replaying an old hashed bundle and every change looked like it had never shipped.
**§29 (2026-09-04)** flattens the domain tree and moves package composition into it. `SECTIONS`
is gone: the seven APK questions and the twelve subject fields now hang directly under
`/PRODUCTS[HOME]` as `QUESTIONS` and `SUBJECT` — **two steps under two legal regimes**, not two
sections of one form — which reverses §23.1's section name and segmented progress bar. And
**D-18**: a package's covers are now child references under `VARIANTS[...]/COVERS` in the tree
rather than rows of a decision table, so what a package contains is visible by expanding a branch.
**§30 (2026-09-04)** is an ergonomics pass over the whole configuration. §29's `USES` collection
is **reversed** — Higson renders a collection even when empty, so 22 of 29 rules carried an empty
"uses" branch; the rules go flat into six thematic branches and what-calls-what moves into element
descriptions. **D-19**: `demo.property.apk.answer` loses its "package" column — it duplicated the
package composition D-18 had put in the tree, so rebuilding a package meant hand-editing a dozen
rows to stop the configuration lying. The answer now names only the **cover** it needs, and the
proposal is the cheapest package covering every need (largest coverage, cheapest on a tie, when
none covers all). Three artifacts nobody read are deleted — `cover.sumBasis`, `fixtures.model` and
the flow `flood.decision`, which looked like a live decision diagram and had never run — and the
`refusal` column leaves the question catalogue (§22 took the control off the screen). ⚠️ The unused
`SECTION` type is still in the schema.
**§31 (2026-09-04, corrected 2026-09-07)** merges the needs analysis into one branch. A
`NEEDS_ANALYSIS` element holds `QUESTIONS` and `RULES` side by side, so the 7 APK questions and the
4 APK rules stop being two unrelated branches at the same level. ⛔ The first version grouped the
whole tree into `STEPS` named "Krok 1"/"Krok 2" — mirroring the app's step bar in the domain — and
was **reversed**: that couples configuration to presentation, and the screens have already been
reordered once (§18). The grouping criterion is the subject matter, never the screen. **D-20**: a
question's text is now the **element name** — it used to sit in a `TEXT` attribute as well, and 4
of 19 had already diverged, invisibly. §31.3 records a regression this pass caused and fixed: the
`cover.sumBasis` table deleted in §30.3 was **not** dead — Java reached it through the `SUM_BASIS`
domain attribute, and the offer lost the liability guarantee sum; the fix fills `sums.liability`
from the chosen variant instead of restoring the table. ⚠️ **W-29**: `update_domain_type` defaults
to `action: SET_TO_ALL`, which overwrites every element's value with the type default — editing
one attribute wiped `KIND` and `CONTEXT_PATH` on all 19 questions and returned HTTP 500.
§31.5 lists what the analysis found and this pass did not do — chief among it that Higson's real
element references (`reference.txt`) are unused, so a package's covers are hand-copied stubs.
**§32 (2026-09-07/08)** makes the tree the way in: everything the application asks Higson for is
reachable by walking it. `SUBJECT`, `FACTORS`, `ZONES` and `USES` leave the tree (their tables stay
and are still used — they are tariff inputs, not a place anyone starts reading); **D-21** gives an
element behaviour instead of one `RULE` pointer (`COVER` 12 attributes, `APK[NEEDS]` 9);
**D-22** hangs a package's covers as **real references** to `COVERS[...]` rather than copies —
⚠️ a reference path needs a **trailing slash**; **D-23** puts a per-element constant on the element,
so a table survives only when it has a second dimension. Answers become configuration (yes/no by
default); the risk factor and the package choice become **flows** with util functions for the
arithmetic — ⛔ **`EXPRESSION` is forbidden**, newer Higson removed it; and every cover gets its own
table keyed by what actually decides it, which is the **opposite** of §31.5's merge proposal and the
reason those tables can be rich. Five parallel reviews then found two defects: `sum.valueGap` asked
for a context path Java had renamed, so its sentence could never fire; and the package rules counted
covers the client cannot buy, so "no security" chose COMPLETE and reported the theft it had just
credited as a gap. ⚠️ Fixing that **deliberately changes one outcome** — no security now proposes
STANDARD. §32.11 lists nine open items that are decisions, not litter.
**§33 (2026-09-08)** closes all nine open items from §32.11, three of them differently than the list
predicted. The **region `PROPERTY` is deleted** — and it was never empty: 17 live parameter rows and
55 domain-element rows still hung off it in Studio's database, which is trap W-24 armed rather than
dormant; no MCP tool deletes a region and the UI marks the card `no-permissions`, so it went through
the product's own `DELETE /studio/api/regions/{code}` (MOTOR and LIFE regions untouched, LIFE's date
versioning re-verified after). The regime's "three dead columns" turned out to be **two**:
`requiresBudget` and `requiresJustification` gate real behaviour in MOTO and LIFE, and cutting all
four — the first attempt — would have broken both demos. And the last hardcoded Polish sentence sat
in a branch that **cannot execute** (the proposal is by construction the cheapest package covering
every need, so a cheaper alternative always leaves a gap); it moved to the rules anyway, as a guard.
Also: a three-step weight ladder replaces three silent ties decided by question order; the flood
waiting period gains prior cover as a real second axis and a **13th subject field**, with continuity
producing a *different sentence* rather than "after 0 days"; the two-constant `quote.recalculation`
table becomes two `LITERAL`s on the product (D-23); `COVER`'s attribute defaults are emptied so a new
cover has no rule rather than someone else's tariff; and a blocking answer now actually stops the
questionnaire, with the server naming which question blocked. ⚠️ **W-30**: a range with an open bound
(`* - 1`) matches **null** and beats the `*` row — a missing budget came back as `BUDGET_FIT`.
⛔ Twelve rows equal to their `*` row were examined and **deliberately kept**: every one is a real
dictionary value that happens to be neutral, and deleting them would leave each table an incomplete
list. §33.9 records the ordering rule this round produced — **decision table → flow → function** —
now a standard in `higson/rules-authoring.md`, together with using a function as a level `source`
(`$f` / `$u`) so a null check becomes a column instead of an `if`.
**§34 (2026-09-09)** adds the **first rule tests this repository has ever had**, one artifact per
Higson testing mode: a single case in the Tester (`src/main/higson/@tests/*.json`) and a ten-row
batch package for the Batch Tester (`src/main/higson-batch/skladka-pakietu-10-przypadkow.xlsx`), both
over `demo.property.calculateQuote` — the flow that prices one package and therefore walks
availability, seven per-cover tables, the period factor and the instalment split. ⛔ **The `Mark`
sheet carries the mode** and is the only difference between the two files: `TEST` for one case,
`BATCH_TESTS` for a batch — getting it wrong returns HTTP 422 `EXCEL_VALIDATION_INCORRECT_LABEL`.
⚠️ `higson_import_tests` is not the way in: it reads **only the first row** of a CASES sheet, because
it converts the file into one test case; a batch goes through `POST /api/test-packages/convert`
("Load test package"). Each case pins the flow's **whole return structure** — annual, instalment and
the per-cover breakdown — so a changed rate shows up as a difference *in its own line*; expecting a
single number reports FAILURE on a correct result. Three of the ten rows guard behaviour rather than
an amount: an unavailable cover costs `THEFT=0` and stays visible in the breakdown, an unavailable
package comes back with a reason and no price, and BASIC in the same flood zone still prices.
⚠️ §34.4 is explicit about what was measured and what was inferred: the package converts (verified
twice, and it converts with only one stored test case, so it is self-contained), but the ten rows
were last seen green when they were ten stored cases, not when run from the file.
**§35 (2026-09-09)** implements the UX redesign's two configuration asks with **two columns of one
table**. `shortLabel` gives the new question outline a short name per question — recorded as a
deliberate exception to D-23, because `demo.property.apk.question` already owns per-question
constants (§17 moved seven there) and an attribute would have cost a run-in with W-29 for one line
of text; it does **not** repeat D-20's deleted `TEXT`, because a table-of-contents entry and the
question itself are different things and both are on screen at once, so a drift is visible.
`partOf` makes a row a **field of another question** rather than a question — the first design
(a `FIELDS` collection under the element) was dropped once it was clear that **only APK questions
are tree elements** while the thirteen subject fields are table rows, so a collection would have
served one branch of two. Counters count parents, the reveal chain runs between parents, a parent is
settled when its required fields are — or, when none is required, when any is, or a question of only
optional fields dead-ends the survey. ⛔ Still not screen grouping (§31): `partOf` is a statement
about the subject matter, `SCREEN`/`GROUP` would not be. Built: `QUESTIONS[EXPECTATIONS]`, which
takes the survey from seven questions to **six**. ⚠️ Two defects surfaced: `PRIOR_FLOOD_COVER`
showed the client raw `true`/`false`, because **an empty value in a question's own row overrides
the `*` row rather than inheriting it**; and the mockup's outline labels ("Lokalizacja", "Historia
szkód") describe step 2 — the designer invented plausible topics instead of reading the catalogue,
which is the general caution about mockups here: **the drawing does not know the question catalogue**.
**§35 continued.** Two further mechanisms arrived the same day. **Question groups became a level of
the domain tree**: a group holds one question or several, and the six groups match the
outline beside the survey — adding a question or a whole group is a branch in Studio. A group's
fields nest only at the wire boundary, so Java's question list stays flat. **A field may be
conditional on an answer**: the `revealedBy` chain fires on *settlement* (a refusal and a skipped
optional question count, deliberately), so it could never express "show this only after yes"; a
second catalogue column `revealedWhen` names the answer that reveals — empty meaning any — and the
reveal table gains one boolean input. ⛔ The condition is configuration, not code: Java resolves the
reference and compares strings, the table decides. The live dependent question hangs under
"Wynajem" and moves the proposal from STANDARD to COMPLETE. ⚙️ **`TEXT` returns to the `QUESTION`
type**, a qualified reversal of D-20 — allowed only because element names stopped carrying the same
text, and only because the reader has **no fallback** to the element name. Also: two single-row
tables (`offer.terms`, `apk.regime`) replaced by grouped domain attributes on `PRODUCT`, and
attribute groups are now **named** rather than merely numbered. ⚠️ W-27 fired again (adding a column
renumbers every row) and `delete_domain_element` records as `UPDATE`, so an export still carries the
deleted element — verify the tree, not the status. ⛔ And an answer to a question the client can no
longer see **must stop feeding the rules** — kept, but filtered out — or a proposal keeps resting on
a hidden question; the filter belongs to the property profile, never to the shared APK layer, whose
other two demos never set `revealed`.
**§36 (2026-09-09)** applies a narrower criterion for what belongs in the domain tree: **an attribute
stays when the application reads it through the attribute, or when it is the one thing a business
user opens on that element.** A scan found that of 41 attributes, Java reads 16 through the
attribute, six are read only by Groovy, and **19 are read by nobody** — Java holds twenty constants
with rule codes and calls them directly, past the attribute beside them. First pass: `COVER` 13 → 9,
dropping `RATE` / `RATE_BASIS` / `CAP_RATIO` / `CAP_ABSOLUTE` — four pointers into columns of the
cover's **own** table, twenty-eight addresses restating the naming convention. The rule of thumb is
**result stays, ingredient goes**; ⛔ `PREMIUM` was on the first cut list and that was wrong — a
cover's premium never even reaches the client DTO, but "what does this cover cost and how is it
priced" is what the *insurer* opens. ⚠️ `SUM_BASIS`/`SUM_REFERENCE` look like ingredients and stay:
they are literals, and in a table they would be a column with one value per row (D-23).
⚠️ **Order matters**: the rule must stop reading the attribute *before* the attribute disappears —
removing one deletes its value on every element at once. ⛔ **`higson_update_function` called without
`arguments` wipes a function's extra arguments** — it took the whole quote down with HTTP 500
(`Expected: 1, Passed: 2`), confirming that `calculate()` has no `catch` and offers no partial
degradation. The other three types followed with no rule changes at all — `VARIANT` 8→5,
`NEEDS_ANALYSIS` 9→7, `PRODUCT` 8→7, all six being pure navigation past which Java already called the
rule code directly. **Final state: 41 → 31 attributes** (`COVER` 9, `NEEDS_ANALYSIS` 7, `PRODUCT` 7,
`VARIANT` 5, `QUESTION` 3), verified by a scenario exercising the value gap, an exceeded budget, a
package chosen against the recommendation and an issued policy — zero differences after each step.
**§37 (2026-09-09)** simplifies `sum.suggest` (25 lines → 12) and fixes two defects. Two thirds of
the "error handling" was **dead code pretending to be error handling**: a `throw` guarding a table
that has a wildcard row and `nullable = false` (it cannot return null), and a fourth returned value
nobody read. `createContext` disappeared with a table dimension — `sum.derivedRatio` was keyed by
`(cover, standard)`, which is why the function had to fabricate a `cover.code` context twice at a
point where **no cover is known yet**; re-keyed on `standard` with two columns it is one call and
4 rows instead of 7. ⚠️ The remaining guard does **not** become "assign 0": a zero area yields a zero
sum and a whole quote priced at zero, presented as real. ⚠️ Table and function changed in **one
session, published once**, so the engine never saw a half-migrated pair. Defects: the offer promised
a flood deductible it never applied (two different deductibles sharing one name on one screen), and
`PUT /variant` accepted any string with 200 then poisoned every later `GET` with a 500. ⛔ The first
fix for the second defect was itself wrong — validating against `quote.getVariants()`, which is
**empty right after a reset** — and its only symptom was four fields in the offer's choice note;
only a field-by-field comparison against a captured baseline caught it.
**§38 (2026-09-09)** runs the same review over all 31 decision tables: **thirty of thirty-one are
tight** — no dead output column, no unreachable row. The exceptions: `shortLabel` was filled for 13
of 28 catalogue rows and **never rendered anywhere** (the outline exists only in step 1, whose items
are groups, which have no catalogue row), so it is gone from table, model, DTO and frontend; and
`factor.deductible` held a real 200/500/1000 ladder that nothing could reach. ⚙️ Rather than cut it
to a `*` row — the "artificial one-liner" the owner forbade — the deductible became a **step-2
question** replacing `RENOVATED`, a field no rule ever read. Zero new rules: a catalogue row with
`kind = MONEY` and `contextPath = quote.policy.deductible` was enough, and a 1000 zł deductible now
takes 481 zł off the COMPLETE premium. ⛔ The trap on the way: answer and model share that one path,
and reversing `collectAnswers`/`collectComputed` to fix it **broke the zones**, because
`collectComputed` derives the postcode prefix *from the answers* — `putIfAbsent` on the single path
is the fix. Also: `apk.question.visibility` loses a whole input dimension (`age` served only the dead
question), two duplicate `ZX` rows in `flood.waitingPeriod` collapse into one, and `offer.choiceKind`
closes both open range bounds (W-30 — an open bound matches `null` and beats the wildcard row).
⛔ `basis` in the seven `cover.*` tables never varies **within** a table but stays: by §36's rule it is
an *ingredient* of the premium and belongs in the table.
**§39–§40 (2026-09-09/10)** apply two more owner rules. **The context carries facts, not
derivations**: `PropertyContext` went from fifteen pushed values to nine — five paths nobody read
(three backed by model fields with no setter, so the engine received `null` on every call), and the
building age, whose whole chain collapsed once `factor.buildingAge` took `$f` as its level source.
The two-digit postcode area is gone entirely: the zone and rate tables key on the **full postcode**,
the client's own answer, because a demo holds a dozen of them. ⚠️ `quote.sums.suggestedBuilding`
looked dead among the rules and is read back **by Java** — removing it silently added a sentence to
the justification, so search the application as well as the rules. The risk factor and the flood zone
stay as deliberate memoisation points. And **package availability lost its table**: a package is
sellable exactly when all its covers are, with the reason composed from the covers' own sentences.
⛔ An intermediate version added a `BLOCKS_PACKAGE` flag per cover; the owner cut it — all covers
must be available, full stop. ⚠️ That withdraws COMPLETE when there is no security, where it used to
price with theft at zero. ⛔ The narrow context in `apk.indicated`/`alternative` must carry **every**
field a cover's availability keys on (zone and security today) or the recommendation and the screen
disagree silently.
**§41 (2026-09-10)** removes the unavailability reason from the demo **entirely** rather than moving
it again: there is no case here where the client is told why something is not for sale, so the
`variant.blocked` function, the `reason` column on `cover.theft`, the `FLOOD_UNAVAILABLE` values and
rows, the `unavailableReason` wire field and its stylesheet rule all go, and `calculateQuote` becomes
linear. ⛔ The `reason` column on `cover.flood` **stays** — `FLOOD_DEDUCTIBLE` is a condition of sale,
not a refusal. Unavailability itself still works; the variant simply comes back without a sentence.
⚠️ **W-27 a third time, and silently**: clearing two cells produced two *extra* rows rather than
editing the existing ones, and the table still behaved correctly because the first matching row won —
a scan of all 30 tables for duplicate input keys is what found it, and it is cheap enough to run after
every column change. ⛔ Two self-inflicted lessons: **never delete a block of text with a `DOTALL`
regex** (a non-greedy `.*?` jumped an interface boundary and ate ~30 lines of `property.ts`, and left
a dangling `}` in a LESS file that only failed two steps later), and **`git checkout HEAD -- <file>`
is not a recovery** for a file carrying same-day uncommitted work — it turned one broken interface
into a set of missing fields.
**§42 (2026-09-10)** closes the gap §36 measured: after it `Property.java` holds **zero rule codes
and zero `engine.get`/`call`/`flow`** — everything the application asks Higson for goes through an
element's attribute, leaving only `getDomain` (the way into the tree) and `setEffectiveDate`. Nine
rules had nowhere to hang and got attributes; two helpers carry every call, and one of them hides
whether the rule is a flow or a function, because that is configuration. ⚠️ **An attribute backed by
a FLOW has no named levels** — `getBigDecimal("factor")` on one throws `UnknownLevelNameException`,
which is what the first attempt did. ⛔ `COVERS[FLOOD]` now has two attributes on the same table
(`IS_AVAILABLE`, `DEDUCTIBLE`) and that is a **recorded exception**: §36 removed such pairs where the
second was an *ingredient* of an answer already on the element, while an imposed deductible is a
different answer that availability cannot yield. `ApkAnalysis.justify` stops taking a table code and
takes a row supplier instead, so the shared layer finally knows no rule address (⚠️ `deriveNeeds`
still does, for the two unconverted demos). All three demos verified field by field: identical.
**§43 (2026-09-10)** x-rays `LIABILITY_SUM` and removes three traces around it. The attribute earns
its place on the **variant** because the OC cover is one element while the sum differs per package;
it is the cover's sum (`SUM_BASIS = CATALOGUE`), the tariff key that makes the rate **degressive**
(0.09‰ under 500k, 0.07‰ over — COMPLETE pays 170.10 instead of 218.70), and the figure the offer
prints. What went: the `0` on BASIC, which has no OC at all — ⛔ **not** the §33.8 case of a neutral
dictionary row, because here the question does not apply, and zero reads as "cover for zero złoty";
the `liabilitySum` wire field, which **no template ever rendered** (the sum reaches the screen on the
cover itself); and `Sums.liability`, a model field **with no setter** whose only effect was pushing
`null` into the context — §39's pattern, missed then because the package pricing overwrote it.
⚠️ **`higson_update_domain_type` with `defaultValue` omitted DELETES the default**, even under
`action: NO_CHANGES` — same family as `update_function` without `arguments`; clearing one default
silently cleared two others. Pass `defaultValue` for every attribute you list.
**§44 (2026-09-10)** makes a question describe itself. The trigger was an owner's objection to the
`APK[NEEDS]` screen, and counting the catalogue's columns found the real fault: **five of them were
filled only for subject fields, two only for tree elements** — one table doing two jobs under one
name, hanging under the needs analysis while 13 of its 27 rows described step two. So `QUESTION`
goes 3 → **9** attributes and `QUESTION_GROUP` 0 → **3** (position, required, options, labels and
the reveal chain move onto the element), and the catalogue becomes
**`demo.property.subject.field`** — 28 rows → 14, 11 output columns → 9 — moving to the product as
`SUBJECT_FIELDS`. ⛔ Only ONE attribute left the screen (9 remain, not the 6 the analysis predicted):
`VISIBILITY`'s two real rows are both **subject fields with no element to hang on**, `ANSWER_MAP` has
a second dimension, and `REVEAL` has no `question` column at all — it keys on the chain's state. The
boundary is exactly where the tree's elements end, and closing it fully would mean reversing §29/§32.
⚙️ Proof that the attribute now decides, not the table: `POSITION` on `BUDGET` changed in Studio from
251 to 205 moved the survey **without a restart**, with the table still saying 251.
⚠️ Fourteen elements were filled by **one XML import**, not 47 MCP calls; the import reported
"Deleted: 5" and only a before/after diff showed those were empty-valued attributes — the import
report is not evidence, the diff is.
**§45 (2026-09-10)** takes the next step the owner asked for — a question should know **how it
behaves**, not just what it says. The mechanism is that an attribute's value can be either: `true`
written literally where the answer never varies, or a pointer to a rule where it does, **in the same
cell**. `QUESTION` goes 9 → **12** attributes, `QUESTION_GROUP` 3 → **4**. Fourteen questions and
groups now carry `IS_VISIBLE = true` instead of each asking a table that returned the wildcard row;
⛔ this is **not** a return to the pre-§17 state, where `IS_VISIBLE` was the same pointer to the same
table on every question — the criterion is not "how many pointers" but "does the value say anything
about this element". `BLOCKS_WHEN`/`BLOCK_MESSAGE` move onto the question and
**`demo.property.apk.question.blocker` is deleted** (it had one real row); a new
`questionElement(Question)` returns **null for a subject field** and callers must bear it.
⛔ The owner chose **not** to make the 13 subject fields elements, so three tables stay for named
reasons: `VISIBILITY`'s only two rows are subject fields with nothing to hang on, `ANSWER_MAP` is
read by a Groovy function that would need an element-or-table branch per answer (a complication
of the kind §37 removed), and `REVEAL` has no `question` column at all. The boundary now runs
**across** the principle rather than beside it — closing it means reversing §29/§32, which is
deferred, not rejected.
**§46 (2026-09-10)** withdraws §45's `IS_VISIBLE` the same day it was added. The owner pointed out
that visibility and the reveal chain are one thing wearing two names — **both answer "is this
question asked or not"** — and checking proved it: the **intersection is empty**, visibility was used
by two subject fields and the chain by six APK questions, never both on one question. "The floor does
not apply to a house" needs no concept of its own: `revealedBy = PROPERTY_KIND, revealedWhen = FLAT`.
⚙️ The `revealedBy`/`revealedWhen` columns §44 had dropped from the catalogue as **empty for every
subject field** come back with content — unplanned confirmation that catalogue and tree describe the
same kind of thing. Gone: `IS_VISIBLE` from `QUESTION`/`QUESTION_GROUP`, `VISIBILITY` from
`NEEDS_ANALYSIS` (9 → **8** attributes), the visibility table (**28** tables left), and `visible` from
the property DTO, the TS model and two frontend filters. ⛔ `Question.visible` **stays in the shared
APK layer** — motor and life read their own visibility tables. ⚠️ The distinction had one real
consequence (the progress counter counted by `visible`, the chain decided rendering); step 1 is
unaffected because no APK question was ever invisible, and step 2 now renders `reachable()` and
completes on revealed fields. Two frontend tests encoded exactly that distinction and were rewritten
to what remains — a test whose subject disappears goes with it, rather than being bent to pass.
**§47 (2026-09-10)** answers the owner's next objection — *"you are adding configuration for things
that can be derived from what is already there"* — and it was right twice over. `revealedBy` held
**two roles**: on five groups it merely restated the order `POSITION` already gives (200…250), on
three questions it carried a real condition. The five go; **question order comes from `POSITION` and
the end of the survey is the absence of a next position**, neither configured twice. Also gone: the
whole gate — `BLOCKS_WHEN`/`BLOCK_MESSAGE` (**added the same day in §45**), `applyBlocker`, the
`blocked`/`blockedBy`/`blockMessage` fields through model, DTO, converter, TS model, two signals, a
template block and a stylesheet rule. ⛔ The gate is **not** relocated — it is simply not shown in the
demo, and a mechanism nobody demonstrates is configuration maintained for no reason. Navigation needed
no change: the cursor already walked a position-sorted list. ⚠️ Five groups are now revealed from the
start and the outline is clickable throughout — intended. **The lesson, arriving three times in one
day: ask whether a value can be derived from what the element already carries BEFORE adding an
attribute** — three attributes and one table were created and removed within the same day.
**§48 (2026-09-10)** removes the cause the three previous sections had only been treating symptoms
of. The owner's point: *the answers are already in the context, so make the next question's
visibility depend on them — the dependency is then done, without any extra logic.* One table with
three inputs (`question.code` plus the **context paths of two answers**) and five rows replaces: the
`reveal` table with **five inputs**, `applyReveal` (45 lines), `sourceMatches` (10), `withReveal`
plus five context fields (20), the `REVEALED_BY`/`REVEALED_WHEN` attributes (`QUESTION` 9 → **7**),
two catalogue columns, and `revealed`/`revealedBy`/`revealedWhen` in the **shared** APK model, which
only property ever used. ⚙️ The engine resolves by **row specificity, not order**, so a missing
answer lands on the general row and `true` on the more specific one — verified across all three
states. ⛔ The warning sign in hindsight: the old table **had no `question` column at all** — it keyed
solely on state Java had just computed. **A rule whose inputs come from the application's own
calculations rather than from the quote usually describes a mechanism invented alongside the engine.**
The wire field is called `visible` again, because that is what it is; the only diff across the three
demos is that rename.
**§49 (2026-09-10)** applies three owner corrections to §48. **A visibility rule belongs to ONE
question**: §48's single shared table with a `question` column (and a pointer on `APK[NEEDS]`, mixing
subject fields with the survey) splits into `demo.property.question.assistance.visible` — hanging
under that question's own `IS_VISIBLE`, **with no question column at all**, because the tree says who
it is about — and `demo.property.subject.field.visible` on the product, where the code column IS
needed because subject fields are not elements. Same pattern as D-21 for covers. **Options follow
from `KIND`**: `OPTIONS`/`OPTION_LABELS` leave `QUESTION` since a survey question is yes-or-no and
nothing else in this demo, while subject fields keep their own lists in the catalogue where they
genuinely differ. **A step follows from its questions**: `REQUIRED` leaves `QUESTION_GROUP` — it
matched the questions 6 times out of 6, i.e. the same rule written twice. ⛔ But **visibility of a
step is not derived the same way — a step is ALWAYS listed**; the first attempt computed it like
required and that was wrong, because the outline must be stable while conditionality works *inside*
a step. ⚠️ The regression showed up in the baseline diff (six groups at `visible: false`), not in the
scenarios, which watched questions rather than steps. `QUESTION` ends at **6** attributes,
`QUESTION_GROUP` at **1**.
**Read before** touching the property demo's rules, the APK question catalogue, or the domain
tree — a PARAMETER `defaultValue` written through the API doubles its `$p` prefix, which only
breaks the *next* element someone adds (**W-25**), and the region-inheritance trap (**W-24**) is
dormant rather than fixed: PROPERTY carries no region any more (D-17), so it returns the moment
anyone attaches one.

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
Five standards: *Snapshot Directory Is the Single Source of Truth* (`src/main/higson` addressed everywhere via `${higson.snapshot.dir}`; never a second copy, zip fixture, or hard-coded path); ***The Snapshot IS Version-Controlled Since 2026-09-04*** — ⛔ **reversed**: it was untracked for nine years and is now committed in full for all three profiles, because the entire configuration existed only inside a local Studio container. **Studio is still where rules are changed**, so the committed tree drifts the moment someone edits without exporting — treat a stale working tree as normal, not as a fault; *Higson CLI Runs On Demand With Safe Defaults* (no `<executions>`, nothing deleted, no auto-commit without explicit opt-in); *Version Business Logic Through the Snapshot, Not SQL Migrations* (no Flyway/Liquibase, ever — subject to the not-committed caveat); *Bump the Higson Runtime and CLI Plugin in Lockstep* (runtime + CLI plugin move together with `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh`, `Readme.md`).

#### Java ↔ Higson Integration Boundary (`standards/higson/integration.md`)
Six standards (one added 2026-09-10: ⛔ ***Reach a Rule Through the Domain Tree, Not by Its Code*** —
the application asks the tree for an element and reads the rule from its **attribute**; naming the
rule breaks three things silently, because repointing the attribute in Studio then changes nothing,
deleting the element breaks nothing visible, and the call site encodes whether the rule is a table or
a flow. Read the **whole row** through `DomainAttribute.getValue(ctx)` rather than adding a pointer
per column, hide the FlowResult-vs-map difference behind one helper — ⚠️ an attribute backed by a
FLOW has no named levels and `getBigDecimal("name")` on one throws — and give shared layers a **row
supplier, never an address**. PROPERTY is fully converted; MOTOR and LIFE are not, and the older
*Reference Rule Codes Through Java Constants* now applies only where a rule genuinely is not in the
tree). The rest: *Higson Runtime API Confined to adapter / context / service* (`io.higson.*` may appear only in `adapter`, `context`, `service`, and the `example` orchestrator — `api`, `dto`, `converter`, `domain` stay Higson-free); *One Adapter Per Domain Class, Exposing Canonical Paths* (each aggregate gets one `Adapter` with a single-argument constructor and a fluent `getMapping()`; child aggregates nest, collections use `CollectionAdapter`; canonical path segments are defined here and nowhere else); *Wire Only the DataSourceProxy Manually* (everything else comes from starter auto-configuration; the `BeanPostProcessor` `@Bean` must be `static`); *Configure the Engine Only Through `higson.*` Properties* (keep `dev-mode-enabled=false`, `function.validate-arguments=true`, `diagnostic.profiler.enabled=false`; never set dialect or schema explicitly); *Reference Rule Codes Through Java Constants* (constants holder or enum, plus the varargs `HigsonContext(path, value, ...)` constructor).

> ⛔ **The `function.validate-arguments=true` clause is correct — do not "fix" it.** It was challenged during the frontend migration and the challenge was **disproven empirically**: the running application with the property enabled returns a complete quote. The observed failure came from a hand-built test engine reading the snapshot directly, not from the configuration. See `.maister/tasks/migrations/2026-08-06-angularjs-to-angular/implementation/work-log.md` (Group 1) for the full differential.

#### Authoring Rules in the Snapshot (`standards/higson/rules-authoring.md`)
Eleven standards (three added 2026-09-09: *Group Domain Attributes by NAME, Not by Number* — `group` on the type / `groupName` on the element is what Studio renders as a section header, `groupOrder` alone gives anonymous blocks, `0` means ungrouped, the name is a **label** in the register of the attribute names beside it (*Podstawy wyceny* / *Warunki oferty* / *Notatka o wyborze klienta*, ordered to follow the process) rather than a code like `OFFER`, and ⚠️ the MCP schema omits `group` although it round-trips, while a domain-TYPE change is not session-scoped and cannot be rejected; *A Per-Element Constant Goes on the Element — Unless a Catalogue Table Already Owns Its Kind*, the recorded exception to D-23 with the "is there already a home for facts of this kind" criterion; and *A Question May Be Composed of Fields, and That Is a Column Too*, the `partOf` contract with its counter, reveal-chain and settlement rules) governing `.def`/`.mat` decision tables and Groovy functions: *Pricing Logic Belongs in Higson Rules, Not Java* (a tariff change must never require recompiling); *Parameter Code Mirrors the Snapshot File Path* (path with `/` → `.`, under a `demo.motor.*` business prefix); *Input Levels Bind `property == source` to Canonical Model Paths* (58 of 58 levels; paths always start at an adapter-exposed root); *Decision Tables Are Cacheable With a Wildcard Default Row* (`cacheable = true`, semicolon matrix with `#matrix for parameter` header, `nullable = false` tables open with a `*` row); *Groovy Function Header Contract* (generated TOML header, `#Full code` matching the path, `ctx` always the first argument, explicit `return`); *Rule Bodies Read Only `ctx` and the `higson` Helpers* (zero imports, zero references to application Java classes); ⛔ *Flows Use Only DECISION_TABLE, FUNCTION and FLOW — Never `EXPRESSION`* (the MCP tool description still lists it, but that documentation is stale and the capability is being removed; a flow cannot do arithmetic, so small utilities like `demo.util.multiply` carry it, and a `FLOW` action returns a `FlowResult` rather than a number); *Known Drift: Legacy ParamContext Binding* (29 of 36 functions still on `org.smartparam.*` — migrate opportunistically, no dedicated sweep).

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
⛔ **A shared component that must look different in one demo** (2026-09-09): an `appearance` **input** when the DOM differs (`app-step-bar`'s circle-vs-inline number), **CSS custom properties** when only the skin does (`app-apk-recommendation`'s panel-vs-border) — never a silent restyle of all three demos, and never `::ng-deep` to dodge the input.
Since August 2026 the shared demo chrome lives in `frontend/src/app/shared/`: `app-step-bar`,
`app-apk-questionnaire` and `app-apk-recommendation`. The recommendation component knows no product —
it renders a proposal with a variant and one without from the same file.
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
⚠️ ***Ship no HTML `<base>` element and set no `baseHref`*** — the servlet context path is a runtime value, and the CLI's default `<base href="/">` would 404 every asset under `/demo` and `/app`; verified across all three context paths from one WAR. Stylesheet assets live under `frontend/src/styles/`, **not** `public/`, because `url()` resolves relative to the partial that wrote it. The build is `npm ci && ng build` in `frontend/`, wired into Maven `prepare-package` via `exec-maven-plugin` with `<workingDirectory>`, with the output pulled into the WAR by a **deliberately unpinned** `maven-war-plugin` `<webResources>` block (Spring Boot manages it at 3.4.0 and its `pluginManagement` carries an `<archive>` block an explicit version would clobber). Generated assets are never committed; exact pins for every `@angular/*`, `rxjs` and `typescript` (`typescript@latest` is 7.0.2 and breaks the build; `less` must be declared explicitly because `@angular/build` ships `sass`); the Node range is enforced by `engines` + `engine-strict`, and by nothing else. ⛔ **The `ng serve` proxy** (fixed 2026-09-09, previously unusable for every demo): each target must carry the `/demo` context path, each entry needs `"cookiePathRewrite": "/"` — without it Tomcat's `Path=/demo` cookie never comes back, **every request opens a new session**, and answers vanish while `PUT`s return 200 — and every `*Api` root must be listed.
**Read before** touching the build, adding a dependency, moving an asset, or debugging a control that "does not save" under `ng serve`.

#### CSS (`standards/frontend/css.md`)
LESS is the styling language, with `frontend/src/styles.less` as the sole global entry point, compiled by the Angular CLI as part of `ng build` — the Gulp `less` task and the tracked `src/main/webapp/styles/styles.min.css` are gone. **One scoped Sass exception**: `frontend/src/theme.scss`, the only `.scss` file, because Angular Material's `mat.theme()` API is Sass-only and has no LESS equivalent; it is listed **first** in `angular.json` so LESS wins the cascade. Appearance is not a parity gate — the UI moved wholesale to Angular Material by decision, and a visual difference is never on its own a finding. ⛔ **The action colour is teal, never red**: `--accent-500` (#FF0000) is an accent used in the brand gradient and nowhere else — reading that backwards is what once put red buttons on every screen. ⚠️ **Contrast is measured on every background**, not just white: muted copy has to clear both the white panel and the grey page ground. ⛔ **Recorded departure (2026-09-09)**: the palette now follows the property demo's UX mockups rather than the portal, and the action colour `#2AA399` measures **3.09:1** on white — under the 4.5:1 floor, on copy the floor applies to. Two passing alternatives were offered and declined; the owner chose the mockup as drawn. It is **not a bug to fix back**, it covers the action colour **only**, and it is **global** (one token, all three demos).
**Read before** adding a stylesheet, picking a colour, or changing a design token.

#### Accessibility (`standards/frontend/accessibility.md`)
Semantic HTML first, full keyboard navigation, sufficient color contrast, alt text and form labels, screen reader testing, ARIA only when native semantics are insufficient, logical heading structure, deliberate focus management.

#### Responsive Design (`standards/frontend/responsive.md`)
Mobile-first approach, standard breakpoints, fluid layouts, relative units, cross-device testing, touch-friendly targets, mobile performance budgets, readable typography, content prioritization on small screens.

---

### Testing Standards

Located in `.maister/docs/standards/testing/`

> ⚠️ **Automated tests are new here and nothing gates them.** The three code suites arrived with the August 2026 frontend migration, the first ten rule tests on 2026-09-09, and **there is still no CI** — no test run has ever blocked a merge. Before the migration, zero test files were tracked in git. Treat "the tests pass" as meaning someone ran them locally.

**Read before** writing any test, and before making any statement about this repository's test coverage.

#### Test Writing (`standards/testing/test-writing.md`)
General: test behavior not implementation, clear descriptive names, mock external dependencies, fast execution, risk-based selection, balance coverage against velocity, critical-path focus, appropriate depth per layer.

Project-specific, **Java**: *Current State — Young Suites, and No CI to Gate Them* (a table of all three suites with their sizes and run commands, plus the honest limits: only the Java suite runs during a build, the E2E harness needs a running application and a populated snapshot, and MockMvc pins 18 of 21 endpoints); *Java Tests Use JUnit 5 via `spring-boot-starter-test`* (the former JUnit 4/5 conflict is **resolved**; surefire pins `-Duser.timezone=UTC` because the seeded `dateOfBirth` is timezone-relative); *Test Classes Are Package-Private With Static Assertions*; *Test Layout Mirrors the Main Source Package*.

Project-specific, **frontend unit tests** (Vitest through the Angular CLI): ⛔ run them with `ng test --watch=false` — raw `npx vitest run` fails with `ReferenceError: describe is not defined` because the CLI supplies the environment; ⛔ select with `--filter`, because `--include` drops the Angular compiler plugin and fails on `.less`; ⛔ use `TestBed.tick()` + `httpTesting.match()` rather than `ApplicationRef.whenStable()`, which **deadlocks** on the outstanding `GET quote` a recalculation leaves behind; ⚠️ `HttpTestingController.match()` **consumes**, so asserting "requests are in flight" that way passes for the wrong reason; assert that failures are *handled*, not merely that nothing rendered; prove a test fails under the mutation it exists to catch, and scramble fixtures that could make it vacuous; keep probe code out of `frontend/src/`.

Project-specific, **end-to-end goldens** (`e2e/`, Playwright): select by test id and accessible role rather than markup; goldens are committed evidence while screenshots are documentation only and never a gate; **a golden is never edited to make a replay pass**.

#### Rule Tests (`standards/testing/rule-tests.md`)
Testing the part of this system that actually holds the business logic. Three standards: ⚙️ *Business Logic Is Covered by Higson Rule Tests* — **real since 2026-09-09**, though partial: `src/main/higson/@tests` holds one case over the property tariff flow and `src/main/higson-batch/` a ten-row batch package for the same flow, and nothing yet covers package selection, justification text, the reveal chain or offer terms; a new business rule's test belongs in the snapshot, which is committed, so the tests are visible in the repository; *Run Rule Tests Off the Snapshot, Not a Live Database* (boot the engine from the snapshot directory via `HigsonSnapshotDataSource` for offline, reproducible runs with no database and no network); *Tests Read the Canonical Snapshot Directory Directly* (consume the same `${higson.snapshot.dir}` the CLI syncs — never copy the snapshot into test resources).

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
**Last regenerated**: 2026-09-10 (§49 — a rule belongs to one question; the rest is derived)
