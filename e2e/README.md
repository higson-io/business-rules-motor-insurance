# `e2e/` — golden capture and replay harness

Six Playwright flows that pin the **behaviour** of the motor-insurance SPA across the
AngularJS 1.8.3 → Angular 22 migration. Captured against the pre-migration build, replayed
against the post-migration build, and diffed in G11.

> **This directory lives at the repository root, deliberately outside `frontend/`, and must
> not be moved by any later group** (`spec.md` §8.3). The same spec files have to run
> unmodified against both frontends; coupling them to the Angular workspace would make that
> impossible, and the harness had to exist *before* `frontend/` did.

---

## What this proves — and what it does not

| Evidence | Gate? |
|---|---|
| Behaviour and **request volume** | ✅ yes |
| The frozen 21-endpoint REST surface (no off-contract call appears) | ✅ yes |
| Screenshots under `goldens/screenshots/` | ❌ **no — documentation only** |

Playwright's screenshot-comparison assertion does not appear anywhere in this directory and
must never be added — G2's acceptance criterion is that grepping `e2e/` for its name finds
nothing. With the entire UI redesigned (C-9), a before/after image diff cannot distinguish an
intended redesign from a regression. **A visual difference is never, on its own, a finding.**

---

## Running it

The application under test must already be running, with its timezone pinned:

```bash
# from the repository root, with Node 22.x on PATH
TZ=UTC mvn clean package -Pall_in_one
TZ=UTC java -Duser.timezone=UTC -jar target/motor-insurance.war \
  --higson.database.url='jdbc:h2:tcp://localhost:9192/higson.demo;NON_KEYWORDS=VALUE' \
  --higson.database.username=sa --higson.database.password=sa
```

Then:

```bash
cd e2e
npm ci                                     # never `npm install` (standards/frontend/build.md)
npx playwright install chromium
MIGRATION_STAGE=pre  TZ=UTC npx playwright test   # capture, against the AngularJS build
MIGRATION_STAGE=post TZ=UTC npx playwright test   # replay,  against the Angular build
```

`APP_BASE_URL` overrides the default `http://localhost:48080/demo/` — the app is served under
the `/demo` context path (`higson-demo-app.properties`), and under `/app` in the published
container image.

Each run writes `goldens/<stage>/<flow>.json` and `goldens/screenshots/<stage>/*.png`.
G11 diffs `goldens/pre/` against `goldens/post/`; **every** differing line must map to a row of
the step-2.5 expected-delta table or to C-9, or it is a regression.

`goldens/` **is committed.** It is captured evidence, not build output (`spec.md` §5.4) — a
`git revert` restores code, it cannot recreate a runtime capture.

---

## Capture metadata

| | |
|---|---|
| Captured | 2026-08-06 |
| **Timezone** | **UTC**, pinned in all three places required by P-4 (`spec.md` §8.6): `use.timezoneId` in `playwright.config.ts`, `TZ=UTC` in the run command, and `TZ=UTC -Duser.timezone=UTC` on the application JVM |
| Branch | `migration/angularjs-to-angular-22` |
| Base commit | `31ec808` (`spring boot starter`) |
| Capture commit | **`d09c201`**, tagged **`pre-angular-migration`** |
| Backend | `target/motor-insurance.war`, `-Pall_in_one`, context path `/demo`, port 48080 |
| Rules | Higson snapshot in `src/main/higson` (171 files), served through the H2 demo database. **The snapshot is not in git** — a fresh clone cannot reproduce this capture without `mvn higson-cli:pull` (P-2) |
| Node / Playwright | Node 22.23.2 · `@playwright/test` 1.62.1 · Chromium 151 |

**Provenance, stated honestly.** The goldens were captured against a working tree in which the
`data-testid` hooks and this harness were still uncommitted; commit `d09c201` is the commit that
landed exactly that tree, and the `pre-angular-migration` tag points at it. `spec.md` §13 item 3
requires the golden to be identifiable as *"captured against a build touched only by additive
test hooks"* — `d09c201` is that build, and the paragraph below is the evidence.

To rebuild the captured application from scratch:

```bash
git checkout pre-angular-migration
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"   # Node 22.23.2 is keg-only on this machine
mvn clean package -Pall_in_one
```

⚠️ Two caveats on reproducing it. The Higson snapshot is **not in git** (P-2), so a fresh clone
needs `mvn higson-cli:pull` first. And `mvn package` re-dirties the tracked
`src/main/webapp/styles/styles.min.css`, because the LESS build inlines Google Fonts CSS over the
network at build time and Manrope has moved `v15` → `v20` since that file was last committed —
discard it with `git checkout -- src/main/webapp/styles/styles.min.css`. DR-21 removes the problem
permanently by untracking the file.

**Why the build is trustworthy despite that.** The only modification to any AngularJS
production file is the 29 additive `data-testid` attributes described below. Stripping every
`data-testid="…"` from the diff reproduces the pre-existing lines byte-for-byte; no content, no
class, no structure and no logic changed, and nothing was deleted.

---

## The `data-testid` hooks — the one sanctioned modification

`data-testid` attributes were added to 7 templates under `src/main/webapp/components/`. Nothing
else under `src/main/webapp/`, `less/`, `gulpfile.js` or the root `package.json` was touched.

**These ids are part of the contract between the two frontends.** The Angular components built
in G5–G8 must carry the *same* ids, or the specs stop being frontend-agnostic:

| Template | ids |
|---|---|
| `main/main.html` | `app-spinner` |
| `driver/driver.html` | `driver-first-name`, `driver-last-name`, `driver-date-of-birth`, `driver-date-of-birth-toggle`, `driver-gender-male`, `driver-gender-female`, `driver-zip-code`, `driver-city`, `driver-street` |
| `driverHistory/driverHistory.html` | `driver-accidents`, `driver-tickets`, `driver-licence-age` |
| `vehicle/vehicle.html` | `vehicle-production-year`, `vehicle-make`, `vehicle-type`, `vehicle-model` |
| `variants/variants.html` | `quote-error-banner`, `variant-card`, `variant-code`, `variant-premium-before-discounts`, `variant-premium` |
| `variants/coverage/coverage.html` | `coverage-row`, `coverage-header`, `coverage-limit`, `coverage-premium` |
| `variants/discount/discount.html` | `discount-row`, `discount-name`, `discount-value` |

> ⚠️ `spec.md` §4.7 names the Angular error surface `data-testid="error-banner"`. The captured
> id is **`quote-error-banner`**. G5 must use the captured id — the golden cannot be changed to
> match the target, only the other way round.

### Why the form controls needed ids at all

The selector policy prefers accessible role + name, then label text, and allows `data-testid`
only for elements with **no accessible name**. `spec.md` §8.3 assumed "everything with a visible
label is reachable by role/label in both frontends without touching anything". **That assumption
is false for this codebase.** Every field label is rendered as

```html
<div class="common-input-default__label">First name</div>
```

— a *sibling* `div`, with no `for`, no `id`, no `aria-label`, no wrapping `<label>`. None of the
16 form controls has an accessible name, so neither `getByRole(…, { name })` nor `getByLabel()`
can reach any of them. Adding `aria-label` would have changed the accessibility tree of a frozen
build; `data-testid` changes nothing observable, and is what the policy prescribes for exactly
this case.

---

## Selector policy — binding

Elements may be located **only** by:

1. accessible role + accessible name — `getByRole('option', { name: 'AUDI' })`
2. label text — `getByLabel('City')`
3. `data-testid`
4. visible text content

⛔ **Forbidden**: Bootstrap class names, `.common-input-default__input`, `mat-*` class names,
`nth-child`, DOM-structure paths. A selector that survives only one of the two frontends defeats
the entire golden strategy.

Where the two frontends genuinely differ in *mechanism* — a native `<select>` versus a
`<mat-select>` that opens a CDK overlay — the difference is absorbed by `support/app.ts`
(`chooseOption`, `optionLabels`, `selectedLabel`) through feature detection on the element, never
by naming a framework class. **The spec files themselves stay byte-identical between stages.**

---

## `MIGRATION_STAGE` and the expected-delta table

Some defect-register rows deliberately change observable behaviour. A spec that hard-coded
today's behaviour would "discover" the sanctioned fix as a failure in G11. So the spec *files*
are identical across stages and only the *environment* differs:

```bash
MIGRATION_STAGE=pre|post
```

Only assertions backed by a delta row branch, via `byStage('DR-nn', { pre, post })` — which
**requires** a defect id, so every branch names its authority in the code. Everything else must
be byte-identical pre and post.

Five assertions carry an explicit **NO BRANCH** marker. Four of them are the DR-13 / ND-1
request counts in `vehicle-cascade.spec.ts`:

> ⚠️ **Only a vehicle *Model* change reloads the quote** (`vehicle.js:76`). Production year, make
> and type deliberately do not. An idiomatic reactive rewrite reconnects them *by accident*, and
> nothing else in this suite would notice. If one of those four assertions ever fails, the
> migration has failed — not the test.

---

## DR-14 / C-6 — the one golden that changes deliberately

`GlobalExceptionHandler` was a plain `@ControllerAdvice` whose handlers returned a `String`
**without `@ResponseBody`**, so Spring MVC read the `QuoteValidator` message as a **view name**;
view resolution failed and the resulting 404 overrode the `@ResponseStatus(BAD_REQUEST)`. The body
carried no `message` key at all, which is why the AngularJS banner rendered **empty**.
G10 makes it a `@RestControllerAdvice` returning the new `ErrorDto` (`spec.md` §6.4).

**`DR-14` is the authority for this delta**, and it is the *only* golden change in the migration
that is not a regression. Verified live against `target/motor-insurance.war` at
`http://localhost:48080/demo/`, blanking the driver's first name and re-requesting the quote:

| | Before (`goldens/pre/error-paths.json`) | After (`goldens/post/error-paths.json`) |
|---|---|---|
| Status | `404` | `400` |
| Body | `{"timestamp":…,"status":404,"error":"Not Found","path":"/demo/quote"}` | `{"message":"Driver first name not supplied."}` |
| `bannerText` | `""` | `"Driver first name not supplied."` |

`error-paths.spec.ts` needs **no edit** for this: both halves were already written as
`byStage('DR-14', …)` branches in G2, so the spec file stays byte-identical across stages and the
`post` run simply takes the other branch. `spring.error.include-message` stays at its default
(`never`) — the message comes from the handler, not from Spring's `/error` machinery.

The other two differing keys in that file are also delta rows: `failedPut.bannerVisible`
(**DR-06** — a failed mutation is no longer swallowed) and `failedQuote.cardsAfter`
(**DR-07** — the stale variant cards no longer survive the failure).

---

## ND-6 — a sanctioned disappearance, deliberately not asserted

The startup spinner flash comes from the eight AngularJS `templateUrl` XHRs at bootstrap. With
compiled templates it **disappears by construction**, and it cannot be reproduced without an
artificial delay. It is recorded as a sanctioned disappearance (OD-13) and **is not asserted by
any spec.** Asserting it would fail the migration for doing the right thing.

---

## Known limitations of this capture

1. **C-8 (HTML coverage descriptions) is not exercised by the data.** `spec.md` §4.8 states that
   `coverage.description` arrives from the Higson rules as HTML, and that `MatTooltip` would
   silently drop the markup. In the snapshot actually used for this capture, **no description
   contains any markup** — six are plain prose and three are `null`. `quote-panel.spec.ts`
   asserts that hovering reveals the description byte-for-byte, which would catch both dropping
   and escaping *if* markup were present; it cannot catch either today. The Angular
   `HtmlPopover` still needs its own unit test in G5.
2. **The gender toggle's visual selected-state is unassertable.** The AngularJS build signals it
   only through the Bootstrap `.active` class and sets no `aria-pressed`, so no policy-compliant
   selector can read it. The gender *value* is pinned by the G1 MockMvc golden and the gender
   *behaviour* by `driver-fields.spec.ts` (DR-08).
3. **`dateOfBirth` moves with the calendar.** The server seeds "today − 40 years", so goldens
   store the token `<today-40y>` rather than a literal that would make a replay on any other day
   report a false regression.
4. **A populated Higson snapshot is required** (P-2). A fresh clone has no rules and the app
   returns nothing usable.
