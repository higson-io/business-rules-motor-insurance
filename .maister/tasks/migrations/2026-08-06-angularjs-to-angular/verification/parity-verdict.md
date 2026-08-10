# Parity verdict — AngularJS 1.8.3 → Angular 22

**Task group**: G11 — parity verification (`implementation/implementation-plan.md` §Task Group 11)
**Date**: 2026-08-10
**Replay anchor**: tag `pre-angular-migration` = commit `d09c201`
**Rollback authority**: `analysis/rollback-plan.md` §5 row 8

---

## VERDICT: **PASS**

**Zero untraceable differences.** 206 golden leaf keys were compared pre → post. 165 are
byte-identical. 41 differ, of which **6 are the `stage` metadata field** (one per file, `"pre"` →
`"post"` — the harness records which run produced the file) and the remaining **35 all map to a
defect-register row or to C-9**. No difference required a golden to be edited, and no golden was
edited.

**G12 is unblocked.**

| Gate | Result |
|---|---|
| AC-F2 — same spec files as capture | ✅ `git diff pre-angular-migration -- e2e/specs/` is **empty** |
| 6 Playwright flows green in `post` mode | ✅ 8 tests passed (6 flows + G9's 2 style checks) |
| AC-C1 — 8 MockMvc contract tests, only DR-14 differs | ✅ 8/8 green; exactly one test method changed |
| **ND-1 / DR-13 byte-identical** | ✅ **all four NO-BRANCH rows identical, including dictionary ids** |
| AC-F1 — parity checklist | ✅ every section passes or traces to DR-nn / C-9 |
| AC-F3 — request counts match per-DR expectations | ✅ exactly |
| 13 code-shape greps | ✅ all pass (4 required rescoping — see §7) |
| AC-F6 — zero console errors | ✅ zero across all flows; see §6.1 for the one qualified line |
| AC-C7 — session semantics | ✅ one `JSESSIONID`, no mid-flow reset |
| Off-contract requests | ✅ **21 distinct paths hit, 21 on contract, 0 off** |

---

## 1. The four NO-BRANCH DR-13 / ND-1 rows — checked first

> *"Only a vehicle **Model** change reloads the quote. Production year, make and type deliberately
> do not. If one of those four assertions ever fails, the migration has failed — not the test."*

Extracted from both goldens rather than inferred from the absence of a diff:

| Cascade level | `pre` requests | `post` requests | `GET quote` pre → post |
|---|---|---|---|
| `productionYear` | `PUT vehicle/productionYear`, `GET dictionaries/make?productionYear=2012` | *identical* | **0 → 0** ✅ |
| `make` | `PUT vehicle/make`, `GET dictionaries/type?makeId=110` | *identical* | **0 → 0** ✅ |
| `type` | `PUT vehicle/type`, `GET dictionaries/model?typeId=12360` | *identical* | **0 → 0** ✅ |
| `model` | `PUT vehicle/model`, `GET quote` | *identical* | **1 → 1** ✅ |

**Byte-identical**, down to the dictionary ids (`makeId=110`, `typeId=12360`) and the chosen option
labels (`100 1.8 44`, `100 1.8 44 (sed, 66hp)`).

The asymmetry is encoded structurally rather than incidentally, which is what makes it durable:

- `frontend/src/app/core/quote-store.ts` — `vehicleFieldChanged(field)` consults a
  `RELOADS_QUOTE[field]` table and calls `recalculate()` for `model` only.
- `frontend/src/app/components/vehicle/vehicle.ts` — all four handlers call
  `store.vehicleFieldChanged(…)`; the component **never** calls `recalculate()` itself
  (verified: 4 call sites, 0 direct `recalculate()` calls).

The single-handler shape is what defeats the failure mode the plan warns about: a component that
routes all four fields through one handler cannot accidentally reconnect three of them, because the
decision lives in the store's table, not at the call site.

---

## 2. Golden diff adjudication — every differing key

206 leaf keys compared. `stage` is excluded below as harness metadata.

### 2.1 `vehicle-cascade.json` — 16 keys, 12 identical, 3 substantive diffs

| Key | pre | post | Authority | Verdict |
|---|---|---|---|---|
| `cascade.dictionariesAfterYearChange.typeCount` | `305` | `0` | **DR-12** | ✅ expected — dependent dictionaries clear instead of going stale |
| `cascade.dictionariesAfterYearChange.modelCount` | `17` | `0` | **DR-12** | ✅ expected |
| `cascade.blankMake.consoleErrors[0]` | `TypeError: Cannot read properties of undefined (reading 'name')` at `vehicle.js:63` | *absent* | **DR-01** | ✅ expected — the make lookup is guarded |

All four `GET quote` counts unchanged — see §1.

### 2.2 `startup.json` — 26 keys, 24 identical, 1 substantive diff

| Key | pre | post | Authority | Verdict |
|---|---|---|---|---|
| `startupRequests.GET driver` | `2` | `1` | **DR-09** | ✅ expected — the duplicate bootstrap fetch is gone |

Everything else identical: all four dictionary loads, `GET vehicle`, `GET quote`, the seeded driver
(John / Potter / M / `<today-40y>` / Lake Jackson / Allwood St / 77566), TOYOTA 2010, and the three
variant codes.

### 2.3 `driver-fields.json` — 22 keys, 15 identical, 6 substantive diffs

| Key | pre | post | Authority | Verdict |
|---|---|---|---|---|
| `perFieldRequests.driver/birthDate.puts` | `3` | `1` | **DR-04** | ✅ expected — the datepicker commits once, not per keystroke |
| `…driver/birthDate.requests[1..3]` | 3 × `PUT`, then `GET quote` | `PUT`, `GET quote` | **DR-04** | ✅ same delta, expressed as the request list |
| `perFieldRequests.driver/gender:reclick[0..1]` | `PUT driver/gender`, `GET quote` | *empty* | **DR-08** | ✅ expected — re-clicking the active toggle is a no-op |

The other five driver fields (first name, last name, zip, city, street) are **byte-identical**:
1 PUT + 1 `GET quote` each, unchanged.

### 2.4 `driver-history.json` — 21 keys, 3 identical, 17 substantive diffs

Three interactions, three delta rows. The high diff count is one row expanding into many keys
(a request array and a response array both collapsing to empty), not many independent changes.

| Interaction | pre | post | Authority | Verdict |
|---|---|---|---|---|
| `accidentCount:type-12` | 2 × PUT + 2 × `GET quote` | 1 + 1 | **DR-03** | ✅ expected — commit on blur, not on keystroke |
| `accidentCount:cleared` | 1 PUT → **HTTP 500** | 0 PUT, 0 responses | **DR-02** | ✅ expected — the cleared field is guarded |
| `licenceObtainedAtAge:below-min` | 2 PUT → **2 × HTTP 500** | 0 PUT, 0 responses | **DR-02** | ✅ expected — see note |

> **Note on the `below-min` row.** The step-2.5 delta table words DR-02 as *"clearing a numeric
> field"*, and the `min` violation is a third interaction not spelled out in that row. It is
> nevertheless the same defect and the same mechanism: in the AngularJS build a `min` violation
> empties the model and the empty body is still sent, which is exactly what "clearing" does. The
> spec binds it explicitly — `specs/driver-history.spec.ts:118` branches on
> `byStage('DR-02', { pre: 'sends-and-fails', post: 'guarded' })` — so the authority is named in the
> code, per the harness rule that every branch must name its DR. **Adjudicated to DR-02.**

### 2.5 `error-paths.json` — 12 keys, 6 identical, 5 substantive diffs

| Key | pre | post | Authority | Verdict |
|---|---|---|---|---|
| `failedPut.bannerVisible` | `false` | `true` | **DR-06** | ✅ expected — a failed mutation is no longer swallowed |
| `failedPut.bannerText` | `null` | `Http failure response for …/driver/address/city: 500 Internal Server Error` | **DR-06** | ✅ expected — see note |
| `failedQuote.responses[0].status` | `404` | `400` | **DR-14 / C-6** | ✅ expected — the view-name dispatch is gone |
| `failedQuote.bannerText` | `""` | `Driver first name not supplied.` | **DR-14 / C-6** | ✅ expected — the validator message reaches the user |
| `failedQuote.cardsAfter` | `3` | `0` | **DR-07** | ✅ expected — stale cards no longer survive the failure |

Request volume unchanged: exactly 1 `PUT driver/address/city` (**no retry**, which C-7 requires to
keep DR-06 narrow) and 0 `GET quote` on the failed mutation.

> **Note on `failedPut.bannerText`.** DR-06's delta row specifies *"banner with text, no retry"* and
> does not pin the wording, so this is inside the sanctioned envelope. The interceptor
> (`frontend/src/app/core/mutation-error.interceptor.ts`) prefers the backend's own message via
> `bodyMessageOf(error)` and falls back to `HttpErrorResponse.message` only when the body carries
> none — which is the case here **because the spec forces the 500 with a mocked empty body `{}`**.
> Against a real backend failure the user-facing message would be the server's. This is a
> deliberate, commented fallback ("so the banner never renders as an empty box") and is strictly
> more information than the pre-migration behaviour of showing nothing at all. **Not a finding.**
> Recorded as a copy-quality observation for G13 in §9.

### 2.6 `quote-panel.json` — 109 keys, 105 identical, 3 substantive diffs

**The entire `quote` observation block — 105 keys of raw `GET quote` response data — is
byte-identical.** Every option code and `order`, every coverage `position` / `code` / `limitLeft` /
`limitRight` / `premium`, every discount `position` / `code` / `value`. The rules engine produced
identical output, and rendering follows the response array with no client-side sort.

The three differing keys are the three rendered card strings. Line-level breakdown:

| # | Change | Cards | Authority | Verdict |
|---|---|---|---|---|
| 1 | `Good Driver\t-20` → `Good Driver\t-20.00` | all 3 | **DR-10** | ✅ expected — discount value gains `DecimalPipe '1.2-2'`, keeps the literal `-`, gains no currency symbol |
| 2 | `1401.19` → `1,401.19` and `1381.19` → `1,381.19` | GOLD only | **DR-10** | ✅ expected — `premiumBeforeDiscounts` / `premium` formatted. BRONZE (284.78/264.78) and SILVER (694.02/674.02) are < 1000, so the separator is invisible there and those values are unchanged |
| 3 | one **blank line** removed before `Good Driver` | all 3 | **C-9** | ✅ expected and **pre-adjudicated** — G5 removed the empty `<thead><tr></tr></thead>` from the discount table |
| 4 | the BI description line removed | BRONZE only | **C-9 / C-8** | ✅ expected — see §3, investigated by experiment |

Coverage limits and premiums are unchanged in every card (`30,000.00 / 50,000.00`, `129.78`,
`0.00`, …) — the **DR-11 NO-BRANCH** row holds: `LOCALE_ID` stays at Angular's `en-US` default and
`pl` is never registered, so `100000` stays `100,000.00` and never becomes `100 000,00`.

Option order `BRONZE, SILVER, GOLD` and coverage/discount order unchanged — the **DR-05 NO-BRANCH**
row holds.

---

## 3. The one diff that required an experiment — the BRONZE description line

`renderedVariantCards[0]` lost the line *"Bodily injury liability provides protection if you injure
or kill someone while operating your car."* This is a **content** difference in a behavioural
golden, so it was not accepted on reasoning. A differential matrix was run against the live post
build, varying **one factor at a time** (hover state), measuring three things (presence in page
text, presence in the card's own text, and the DOM ancestry of the node):

| Factor: hover state | in page text | in card text | DOM host of the node |
|---|---|---|---|
| **A.** no hover | `false` | `false` | absent |
| **B.** hovering the BI row | **`true`** | `false` | `body > div.cdk-overlay-container > … > div[data-testid=html-popover-panel]` — **OUTSIDE the card** |
| **C.** hover moved away | `false` | `false` | absent |
| **D.** hovering the BI row again | **`true`** | `false` | *same as B* — **OUTSIDE the card** |

**Conclusion.** The description is neither dropped nor escaped. It is revealed on hover and hidden
otherwise — the same on/off behaviour the pre build had, and the specs prove that independently:
`quote-panel.spec.ts` asserts *unconditionally, with no stage branch* that the description is absent
from the page before hover and present after, and **both assertions passed in the pre capture and in
the post replay**. Had the pre build shown the description permanently, the pre capture would have
failed at the first of those assertions.

What changed is **where the node lives**. Angular CDK's Overlay attaches its pane to a
`.cdk-overlay-container` that is a direct child of `<body>`, so the text is no longer inside
`[data-testid="variant-card"]` and `textsOf(card)` cannot see it. `renderedVariantCards` is captured
immediately after the C-8 hover step, which is why the pre golden happens to contain exactly one of
the three available descriptions — a hover-state capture artefact, not a content inventory.

The CDK Overlay is not an incidental choice: it is what the plan's trap index **prescribes** —
*"`MatTooltip` silently drops the HTML description → CDK Overlay popover + `bypassSecurityTrustHtml`,
once, commented"*. `bypassSecurityTrustHtml` appears exactly once, in
`frontend/src/app/shared/html-popover/html-popover-panel.ts:31`.

**Authority: C-9** (the Material redesign moved the popover into an overlay container), with the
functional contract preserved and independently asserted. **Not a regression.**

> **Limitation carried forward, unchanged from G2.** No coverage description in the current Higson
> snapshot contains markup (`descriptionContainsMarkup: false`, identical pre and post), so the
> escaping half of C-8 is not exercised by the data in either build. The G5 unit test covers it.

---

## 4. MockMvc contract replay

```
mvn test -Dtest='DriverApiTest,VehicleApiTest,DictionaryApiTest,QuoteApiTest'
→ Tests run: 8, Failures: 0, Errors: 0, Skipped: 0    BUILD SUCCESS
```

Full suite, for completeness: **`Tests run: 11, Failures: 0, Errors: 0`** (the 8 above plus
`SnapshotDataSourceDemoTest`'s 3).

**AC-C1 — only the DR-14 row differs.** `git diff pre-angular-migration -- src/test/` touches exactly
one file and exactly one test method:

| | |
|---|---|
| Removed | `quoteFailureForwardsTheValidatorMessageAsAViewNameAndCarriesNoMessageKey()` |
| Added | `quoteReturnsFourHundredWithValidationMessage()` |
| Annotated | Javadoc names **DR-14 / C-6** and cites G10 as the only sanctioned authority for the replacement |

The other seven test methods across the four classes are untouched. The new method's load-bearing
assertions are the ones MockMvc can actually see: the body carries `message`, `ErrorDto` exposes
exactly one field, and `forwardedUrl` is **null** — direct evidence that the view-name dispatch is
gone. The 404 → 400 half is pinned live by `error-paths.spec.ts` (§2.5).

Production Java changed in exactly two places, both G10 / DR-14:
`GlobalExceptionHandler.java` (+15/−6) and the new `api/dto/ErrorDto.java`. **No other Java file
differs from the tag** — which is also the ND-7 and ND-8 evidence (`ApplicationConfig.java` and
`Application.java` untouched).

> **Coverage note, stated honestly.** The four contract classes pin **18 of the 21 endpoints**
> directly (Javadoc: 1–4, 5–9, 13–15, 16–20, 21). Endpoints 10–12 —
> `PUT driver/accidentCount`, `PUT driver/trafficTicketsCount`, `PUT driver/licenceObtainedAtAge` —
> are pinned by `driver-history.spec.ts` at runtime instead. This is a G1 scoping decision that
> predates the tag, not a G11 regression. It is fully closed by the live check in §6.2: a complete
> wizard flow hits **21 distinct paths, all on contract**.

---

## 5. Parity checklist walk (AC-F1) and request counts (AC-F3)

| Checklist section | Result | Traceability |
|---|---|---|
| 1. Startup / bootstrap | pass with delta | `GET driver` 2 → 1 (**DR-09**); all four dictionaries, `GET vehicle`, `GET quote` unchanged |
| 2. Driver text fields | **byte-identical** | 5 fields × (1 PUT + 1 `GET quote`) |
| 3. Driver date of birth | pass with delta | 3 PUT → 1 (**DR-04**) |
| 4. Driver gender toggle | pass with delta | first click unchanged; re-click 1+1 → 0+0 (**DR-08**) |
| 5. Driver claim history | pass with delta | blur commit (**DR-03**); cleared and below-`min` guarded (**DR-02**) |
| 6. Vehicle cascade | pass with delta | dictionary reloads **byte-identical**; dependent lists clear (**DR-12**); blank option guarded (**DR-01**) |
| 7. **Vehicle → quote asymmetry** | **byte-identical** | **ND-1 / DR-13**, four NO-BRANCH rows — §1 |
| 8. Quote panel ordering | **byte-identical** | **DR-05** NO BRANCH; 105 raw response keys identical |
| 9. Money formatting | pass with delta | coverage rows **byte-identical** (**DR-11** NO BRANCH); the four outer fields formatted (**DR-10**) |
| 10. Coverage description popover | pass, mechanism changed | **C-8** behaviour preserved; **C-9** DOM location — §3 |
| 11. Error paths | pass with delta | **DR-06**, **DR-07**, **DR-14** |
| 12. Session / context path | **byte-identical** | §6.2, §6.3 |

**AC-F3 — request counts match the per-DR expectations exactly.** Every count in the step-2.5 delta
table was observed at its expected value; no count changed that the table does not sanction.

---

## 6. Runtime checks

### 6.1 AC-F6 — console errors

The specs only track console errors inside `vehicle-cascade.spec.ts` (for DR-01), so AC-F6 was
verified independently by an instrumented run covering all six flows' interaction surface in one
session:

| After flow segment | `console.error` | `pageerror` |
|---|---|---|
| startup | 0 | 0 |
| driver fields (7 fields + gender re-click + datepicker) | 0 | 0 |
| driver history (3 fields + cleared field) | 0 | 0 |
| vehicle cascade (all 4 levels) | 0 | 0 |
| quote panel (popover open + close) | 0 | 0 |
| error path (blank first name) | **1** | 0 |

The single line is **`Failed to load resource: the server responded with a status of 400 ()`** —
Chromium's own network log entry for the deliberately provoked `GET quote → 400`, i.e. for the
DR-14 status itself. It is not an application error: `pageerror` is 0 and no application code threw.
The pre-migration build emitted the equivalent line for its `404`. **AC-F6 holds.**

Independently, `styles-assets.spec.ts` CHECK 1 passed in `post` mode: **zero 4xx on any same-origin
font or image** across a flow that deliberately exercises the deferred `url()` references
(popover arrow, logo, four sidebar icons).

### 6.2 AC-C7 — session semantics, and the REST surface

| Check | Result |
|---|---|
| `JSESSIONID` cookies in the jar after a full wizard flow | **1** |
| Distinct `JSESSIONID` values seen | **1** |
| `Set-Cookie` responses observed | **1** (the initial one) |
| Cookie path | `/demo` |

No mid-flow quote reset: a `GET driver` and `GET vehicle` issued mid-flow returned the **complete
accumulated state** — `Jane / Doe / 1990-05-17 / F / 77001 / Houston / Main St / 12 accidents /
3 tickets / licence age 25`, and `AUDI / 2012 / typeId 12360` — i.e. every mutation from every flow
segment survived in one session.

> `numberOfAccidents` correctly remained `12` after the field was cleared: under **DR-02** clearing
> sends no PUT, so the server keeps the last committed value. That is the sanctioned behaviour.

**Off-contract request check.** Every application request during the full flow was matched against
the frozen 21-endpoint contract: **21 distinct paths hit, 0 off-contract.** The Angular SPA uses
exactly the frozen surface — no endpoint added, none abandoned.

### 6.3 Context paths — a claim that needed the experiment, not the assumption

A first probe showed `/demo/` → 200 but `/` → 404 and `/app/` → 404, which looks like a base-href
regression. It is not: `server.servlet.context-path` is a **single-valued** setting
(`higson-demo-app.properties:24`), so one running instance can only ever serve one of the three.
Re-run varying **only** that factor, restarting the same WAR for each:

| Run | `server.servlet.context-path` | index | `<app-root>` | `<base>` | `main-*.js` | `styles-*.css` | `GET quote` |
|---|---|---|---|---|---|---|---|
| A | `/demo` | 200 | 1 | **0** | 200 | 200 | 200 |
| B | *(empty — root)* | 200 | 1 | **0** | 200 | 200 | 200 |
| C | `/app` | 200 | 1 | **0** | 200 | 200 | 200 |

A browser render at the non-default path `/app/` confirmed it is a real render, not just a 200:
**3 variant cards `BRONZE`/`SILVER`/`GOLD`, seeded driver `John`, 0 console errors, 0 failed
requests.** DR-20 and the no-`<base>` rule hold at all three context paths. **No finding.**

### 6.4 Angular unit suite

`npx ng test --watch=false` → **14 test files, 27 tests, all passing.**

---

## 7. Code-shape greps (AC-S1…AC-S8, AC-C2…AC-C6)

Re-runnable as **`bash frontend/scripts/g11-parity-greps.sh`** → *ALL CHECKS PASSED* (22 checks).
Written as a script for the reason G9 gave: a grep that only ever ran once in a transcript proves
nothing later, and G12 needs to re-prove these after it deletes files.

| Check | Result |
|---|---|
| AC-S1 `standalone: true` | ✅ empty |
| AC-S2 `@Input` / `@Output` | ✅ empty |
| AC-S2 `*ngIf` / `*ngFor` / `ngClass` / `ngStyle` | ✅ empty |
| AC-S3 `BehaviorSubject` | ✅ empty |
| AC-S4 `: any` | ✅ empty |
| AC-S5 / DR-11 `LOCALE_ID` / `registerLocaleData` | ✅ empty **(rescoped)** |
| AC-S6 / DR-20 `<base` in `index.html` | ✅ empty |
| AC-S7 `*.scss` | ✅ only `frontend/src/theme.scss` |
| AC-S8 / C-8 `bypassSecurityTrustHtml` | ✅ **exactly 1** |
| AC-C2 `JSON.stringify` | ✅ empty |
| AC-C4 `http.get\|put('/…` | ✅ empty |
| AC-C5 `HttpClient` in components | ✅ empty **(rescoped)** |
| AC-C6 `.data` | ✅ empty |
| DR-05 `.sort(` / `orderBy` | ✅ empty |
| DR-16 `sidebarHidden` / `toggleSidebar` | ✅ empty **(rescoped)** |
| DR-17 `selectedDate` | ✅ empty |
| DR-18 `priceBeforeDiscount` | ✅ empty |
| DR-19 `close-text` / `closeText` | ✅ empty |
| ND-4 `Validators` / `mat-error` / `required=` | ✅ empty |
| ND-7 `provideRouter` / `router-outlet` | ✅ empty **(rescoped)** |
| DR-20 `<app-root>` present in `index.html` | ✅ present |
| DR-13 `vehicleFieldChanged` = 4, `.recalculate(` = 0 | ✅ **(restated — see below)** |

### 7.1 Rescoping — four greps, each with its reason

Every rescope narrows *where* the grep looks. **No pattern was weakened and no check was dropped.**

| # | Check | Literal grep's false positive | Rescope | Why the criterion still holds |
|---|---|---|---|---|
| 1 | **AC-S5** | `coverage.spec.ts:12` — a **comment** reading *"…is why this suite deliberately provides no `LOCALE_ID`"* | blank `/* */`, `<!-- -->` and leading `//` before matching, reusing `strip_comments` from `frontend/scripts/g9-style-checks.sh` | The criterion is whether a locale is **registered**, not whether the word appears in prose. G5–G9 deliberately document *why* constructs are absent, using the very token being grepped |
| 2 | **AC-C5** | 16 hits, all `provideHttpClient` / `HttpTestingController` in `*.spec.ts` | `frontend/src/app/components/**/*.ts` excluding `*.spec.ts` | The criterion is *no **component** injects `HttpClient`* — HTTP belongs in `*Service`. Test doubles must provide the client; that is how they test it |
| 3 | **DR-16** | `sidebar.spec.ts:56` — the **assertion message** of the test that proves it: *"a ported `toggleSidebar` would show up here"* | production `*.ts` / `*.html` only | The test proving the absence cannot itself be the evidence of presence |
| 4 | **ND-7** | `app.spec.ts:65–84` — the test must **query** for `router-outlet` in order to assert zero of them exist, and names `provideRouter` in a message | production `*.ts` / `*.html` only | Same class as #3 |

### 7.2 G6 step 6.6 — the criterion restated so it is satisfiable

The plan's literal criterion — *"`grep -rn "recalculate" …/components/vehicle` returns exactly one
call site"* — returns **zero**, because G4 introduced `vehicleFieldChanged()` and the component never
calls `recalculate()` directly. The criterion as written cannot be satisfied by the design the plan
itself mandates.

The **intent** — at most one path from the vehicle component to a quote reload — is met *more
strongly* than the literal wording asks, and is verified as:

```
vehicleFieldChanged  → 4   (one per cascade level, all routed through the store)
\.recalculate\(      → 0   (the component never triggers a reload itself)
```

Zero direct call sites is a stronger guarantee than one: the decision lives in the store's
`RELOADS_QUOTE` table, where it is covered by a unit test, rather than in a component branch. See §1.

### 7.3 Not re-run as a grep: DR-10's currency-symbol check

The plan's `grep -rn '\$\|zł\|PLN' frontend/src` matches every TypeScript template literal
(`` `[data-testid="${x}"]` ``) and every `$event`, so it cannot discriminate. It is unnecessary here:
`quote-panel.spec.ts` asserts the same property **at runtime, with no stage branch** — *"no currency
symbol appears anywhere (DR-10)"*, checking `$`, `zł`, `PLN` and `€` against the fully rendered
page — and it passed in `post`. Runtime evidence on the rendered output is strictly better than a
source grep.

---

## 8. Register and non-deviation walk

### 8.1 DR-01 … DR-27

| ID | Status | Evidence |
|---|---|---|
| **DR-01** | ✅ | `vehicle-cascade` `blankMake.consoleErrors` `[TypeError]` → `[]` |
| **DR-02** | ✅ | `driver-history` cleared **and** below-`min`: 1–2 PUT/500 → 0 PUT, 0 responses |
| **DR-03** | ✅ | `driver-history` `accidentCount:type-12` 2 PUT + 2 GET → 1 + 1 |
| **DR-04** | ✅ | `driver-fields` `birthDate.puts` 3 → 1 |
| **DR-05** | ✅ | 105 raw response keys identical; render follows array order; `.sort(`/`orderBy` grep empty |
| **DR-06** | ✅ | `failedPut.bannerVisible` false → true, 1 PUT (no retry), 0 `GET quote` |
| **DR-07** | ✅ | `failedQuote.cardsAfter` 3 → 0 |
| **DR-08** | ✅ | `gender:reclick` `[PUT, GET quote]` → `[]` |
| **DR-09** | ✅ | `startup` `GET driver` 2 → 1 |
| **DR-10** | ✅ | `-20` → `-20.00`; `1401.19` → `1,401.19`; `1381.19` → `1,381.19`; no currency symbol at runtime |
| **DR-11** | ✅ **NO BRANCH** | coverage limits/premiums byte-identical; `LOCALE_ID` grep empty |
| **DR-12** | ✅ | `typeCount` 305 → 0, `modelCount` 17 → 0 |
| **DR-13** | ✅ **NO BRANCH ×4** | §1 — byte-identical |
| **DR-14** | ✅ | 404 → 400; `""` → `"Driver first name not supplied."`; `QuoteApiTest.quoteReturnsFourHundredWithValidationMessage()` |
| **DR-15** | ✅ | `styles-assets` CHECK 2 passed (computed `cursor` is `default`, no hover affordance). The only `cursor: pointer` left in `frontend/src` is on `::-webkit-scrollbar-thumb` (`main.less:56`) — a scrollbar, not a card |
| **DR-16** | ✅ | grep empty (rescoped, §7.1) |
| **DR-17** | ✅ | `selectedDate` grep empty |
| **DR-18** | ✅ | `priceBeforeDiscount` grep empty |
| **DR-19** | ✅ | `close-text` / `closeText` grep empty |
| **DR-20** | ✅ | `<app-root>` in `index.html`, no `<base>`, verified at all three context paths (§6.3) |
| **DR-21** | ⏳ **G12** | `styles.min.css` still tracked — deletion is G12 step 12.2 by design |
| **DR-22** | ⏳ **G12** | root `package.json` still present — G12 step 12.4 |
| **DR-23** | ⏳ **G12** | `gulpfile.js` still present — G12 step 12.4 |
| **DR-24** | ✅ | `frontend/public/` holds only `favicon.ico`; `logo.png` / `select-arrow.png` absent from the Angular tree. The 6 SVGs are carried in `frontend/src/styles/imgs/`. (Removal from `src/main/webapp/imgs/` is G12 step 12.3) |
| **DR-25** | ✅ | `.coverage__divider`, `.common-checkbox`, `.common-subsection-heading` absent from `frontend/src` (comments blanked) |
| **DR-26** | ✅ | `mvn clean package -Pall_in_one` succeeded with default optimization; bundle 680.86 kB (a budget **warning**, not an error) |
| **DR-27** | ✅ | `selectedDate` grep empty — one signal on the component, no dual-scope binding |

**DR-21, DR-22 and DR-23 are the only outstanding rows, and all three are G12's own deliverable.**
They are not G11 failures; the plan sequences deletion into a separate isolated commit precisely so
this verdict can be rendered against a tree that still contains both frontends.

### 8.2 ND-1 … ND-8 — the eight things that had to survive

| ID | Status | Evidence |
|---|---|---|
| **ND-1** | ✅ | §1 — the four NO-BRANCH rows, byte-identical. **The strongest evidence in the suite** |
| **ND-2** | ✅ | `sidebar.html:54` renders the literal `Documents` over a section holding claim history; `sidebar.spec.ts` asserts `['Personal information', 'Vehicle', 'Documents']` verbatim |
| **ND-3** | ✅ | The "Address" divider is still emitted from **inside** the driver component (`driver.html:88–91`), not from the sidebar that owns every other divider |
| **ND-4** | ✅ | `Validators` / `mat-error` / `required=` grep empty — no validation was introduced |
| **ND-5** | ✅ | `driver-zip-code` is a free-text input with no `pattern` and no mask |
| **ND-6** | ✅ *(deliberately not asserted)* | The startup spinner flash disappears by construction — 8 `templateUrl` XHRs replaced by compiled templates. Sanctioned as OD-13, documented in `e2e/README.md`, asserted by no spec. Asserting it would fail the migration for doing the right thing |
| **ND-7** | ✅ | `provideRouter` / `router-outlet` grep empty (rescoped); `ApplicationConfig.java` byte-identical to the tag |
| **ND-8** | ✅ | `Application.java` byte-identical to the tag — the only production Java changes are `GlobalExceptionHandler.java` and the new `ErrorDto.java`, both DR-14 |

---

## 9. Observations — non-blocking, for G13

Neither is a parity finding. Both are recorded so they are not rediscovered later as surprises.

1. **`failedPut` banner copy.** When a failed mutation carries no message in its body, the banner
   falls back to `HttpErrorResponse.message`, which embeds an absolute URL and reads as a developer
   diagnostic. Sanctioned by DR-06 (which pins *"banner with text"*, not the wording) and strictly
   better than the pre-migration silence. If the team wants user-facing copy, that is a **new
   requirement**, not a migration defect — G13 release notes are the right place to raise it.
2. **MockMvc pins 18 of 21 endpoints** (§4). The three claim-history PUTs are covered at runtime
   instead. Worth closing if the contract suite ever becomes a CI gate.

---

## 10. Provenance and method

| | |
|---|---|
| Build under test | `target/motor-insurance.war`, `mvn clean package -Pall_in_one`, `TZ=UTC` |
| Runtime | `TZ=UTC java -Duser.timezone=UTC -jar …`, context path `/demo`, port 48080, H2 at `tcp://localhost:9192` |
| Node | 22.23.2 (keg-only at `/opt/homebrew/opt/node@22/bin`) |
| Replay command | `cd e2e && MIGRATION_STAGE=post TZ=UTC npx playwright test` |
| Rules snapshot | `src/main/higson`, **171 files** — matches the capture inventory in `e2e/README.md` |

**Deliberate deviation from step 11.1: `mvn higson-cli:pull` was not run.** The step's purpose is
P-2 — guarantee the rules exist before replaying. They do: 171 files, and **every one of them
predates the golden capture** (newest mtime `2026-08-06 13:47`, capture `16:11`; zero files modified
since). Pulling a fresh snapshot would have introduced the one variable this entire group exists to
control — a rules change between capture and replay would surface as premium differences
indistinguishable from a frontend regression. Not pulling is the conservative choice, and it is
corroborated by the outcome: **105 raw `GET quote` response keys came back byte-identical**, which
is only possible if the rules are unchanged.

### Files created by this group

| Path | Purpose |
|---|---|
| `.maister/tasks/.../verification/parity-verdict.md` | this document |
| `frontend/scripts/g11-parity-greps.sh` | the 22 code-shape checks, re-runnable by G12 |
| `e2e/goldens/post/*.json` | 6 replay goldens (`error-paths.json` refreshed from G10's capture) |
| `e2e/goldens/screenshots/post/*.png` | 8 screenshots — **documentation only, never gated** |

**No production code, no spec file and no `pre` golden was modified.** `toHaveScreenshot()` appears
nowhere in `e2e/`.

---

## 11. Sign-off

**Zero untraceable differences. All 35 substantive golden deltas map to a DR row or to C-9. The four
DR-13 / ND-1 NO-BRANCH rows are byte-identical. All eight non-deviations survived.**

**G11 is green. G12 (deletion of the AngularJS frontend) is authorised.**

⚠️ G12 is `rollback-plan.md` §5 row 9 — *the point of no easy return*. Keep it as a single isolated
commit, and re-run `bash frontend/scripts/g11-parity-greps.sh` afterwards.
