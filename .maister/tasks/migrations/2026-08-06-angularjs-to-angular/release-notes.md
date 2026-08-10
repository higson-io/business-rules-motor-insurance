# Release notes — AngularJS 1.8.3 → Angular 22

**Date**: 2026-08-10
**Branch**: `migration/angularjs-to-angular-22`
**Parity verdict**: PASS (`verification/parity-verdict.md`) — 206 golden keys compared, zero untraceable differences
**Scope**: frontend rewrite. The 21-endpoint REST surface is unchanged; exactly one backend behaviour changed (below).

---

## What users will notice

### 1. The quote no longer updates while you type ⚠️ the most noticeable change

**What changed.** The three claim-history fields — *number of accidents*, *traffic tickets* and *licence obtained at age* — and the seven driver text fields now commit when you leave the field, not on every keystroke.

**Before**: typing `12` into *number of accidents* sent two `PUT`s and two quote recalculations — one for `1`, one for `12`. The quote visibly flickered through a wrong intermediate value.
**After**: one `PUT`, one recalculation, on the value you actually meant.

**Why it may feel different.** The quote no longer moves as you type. Tab out of the field, or click elsewhere, and it updates. This halves request volume on every numeric edit and removes the intermediate wrong figure.

*Register: DR-03. Evidence: `driver-history.json` golden, 2 PUT + 2 GET → 1 + 1.*

A related fix rides along: clearing a numeric field, or entering a value below its minimum, used to send an empty body and fail with an HTTP 500 that the UI swallowed silently. It now sends nothing at all, and the server keeps the last committed value (**DR-02**).

### 2. Failed quote calculations now say what is wrong

**Before**: a validation failure — say a blank first name — produced HTTP 404 with no message, and the red banner appeared **empty**. The validator's message was being interpreted by Spring as a *view name* rather than as content.
**After**: HTTP 400 with a real body, and the banner reads e.g. *"Driver first name not supplied."*

*Register: DR-14 / C-6. The only production Java change in the migration: `GlobalExceptionHandler` moved from `@ControllerAdvice` to `@RestControllerAdvice` and returns a new `ErrorDto`.*

Relatedly, a failed **mutation** (a `PUT` that fails) is no longer swallowed: it now surfaces in the same banner, with no retry (**DR-06**). See the open copy-quality item below.

### 3. The interface looks different — by design

The entire UI moved to **Angular Material** (decision C-9). Every input, select, date picker, toggle and card on the screen is a different control from before. Bootstrap 3 and Font Awesome 4 are gone rather than upgraded.

This was an explicit, user-approved scope decision, not a side effect. **Visual parity was abandoned deliberately**, and screenshots were kept as documentation of the redesign rather than as a regression gate — with the whole UI redesigned, a before/after image diff cannot tell an intended redesign from a regression. Regression protection rests on the behavioural and contract test suites instead.

The brand still reads as itself: the `#4C4CDE` primary, the 8px corner radius, the page gradient and Manrope all carry through Material's theme tokens.

Smaller consequences worth knowing:
- Form controls now have **real accessible names** (`<mat-label>`), which the old sibling-`div` labels never provided.
- The coverage description popover is now reachable **by keyboard**, not hover-only.
- The startup spinner flash is gone: there are no longer eight template XHRs to wait for.
- Money formatting on the four outer figures gained thousands separators (`1401.19` → `1,401.19`) and two decimals on the discount (`-20` → `-20.00`). Coverage limits and premiums are byte-identical (**DR-10**, **DR-11**).

---

## What deliberately did **not** change

- **The REST contract.** 21 endpoints, none added, none abandoned, verified live during a full wizard flow.
- **Session semantics.** One `JSESSIONID`, no mid-flow reset, `@SessionScope` quote unchanged.
- **The Higson rules snapshot.** Untouched. 105 raw `GET quote` response keys came back byte-identical.
- **⛔ The vehicle-cascade asymmetry.** Only a **model** change reloads the quote; production year, make and type do not. This is intended behaviour, and it is the one assertion required to be byte-identical before and after (**ND-1 / DR-13**). Seven further "looks like a bug, is not" behaviours were likewise preserved intact.
- **Ports, context path, packaging, Docker.** `48080`, `/demo`, `motor-insurance.war`, `-Pall_in_one` — all unchanged.

---

## For developers

- The SPA now lives in **`frontend/`** (Angular 22, TypeScript strict, LESS). The `less/` tree, `gulpfile.js`, the root `package.json`/`package-lock.json` and every file under `src/main/webapp/` were deleted at cutover; `src/main/webapp/` survives as an empty `warSourceDirectory`.
- **Node version now matters**: `^22.22.3 || ^24.15.0 || >=26.0.0`. Nothing in the repository installs or selects it, and the wrong version fails opaquely inside `mvn package`. See `Readme.md`.
- `mvn clean package` still does everything — it just runs `npm ci && ng build` in `frontend/` instead of Gulp.
- Three test suites now exist (Java/MockMvc, Angular/Vitest, Playwright E2E). **There is still no CI**; nothing gates a merge.

---

## Open decisions — recorded, not resolved

### A. Production bundle budget

Measured on the current build:

| | |
|---|---|
| Initial bundle, raw | **680.86 kB** |
| Initial bundle, estimated transfer | **~143.7 kB** |
| `maximumWarning` (initial) | 500 kB |
| `maximumError` (initial) | 1 MB |
| Overrun against the warning | 180.86 kB |

This is a **warning, not an error** — `ng build` exits 0 and the WAR packages normally.

Attribution, measured rather than assumed: Angular Material + CDK + the datepicker account for roughly **95%** of the overrun; the styles group added 9.47 kB, about 5%.

`angular.json` was deliberately **left untouched** so the signal is not erased before anyone looks at it. The choice:

1. **Accept and raise `maximumWarning`** to a number the team is willing to defend — with a comment recording *why* that number.
2. **Reduce the bundle** — the realistic lever is trimming Material surface area, since the datepicker and its locale machinery are the largest single contributor.
3. **Leave it warning.** Honest, but a permanently-yellow build teaches people to ignore build output.

⚠️ Not decided unilaterally. Someone should pick one.

### B. `AC-S1` versus current Angular best practice

`AC-S1` requires every component to set `changeDetection: ChangeDetectionStrategy.OnPush`. The Angular best-practices guidance served for this workspace says the opposite: *"do **not** set `OnPush` explicitly — it is the default in v22+."*

The project rule was followed (documentation priority beats general best practice), so all 11 components and directives declare it.

**Suggested restatement**: *"No component opts **out** of OnPush."* That keeps the guarantee that matters without requiring redundant metadata. Recorded in `standards/frontend/components.md` as an open item; settle it before the next component is written so the workspace stays uniform either way.

### C. Failed-mutation banner copy

When a failed `PUT` carries no message in its response body, the banner falls back to `HttpErrorResponse.message`, which embeds an absolute URL and reads as a developer diagnostic rather than user-facing copy.

This is inside the sanctioned envelope — DR-06 pins *"banner with text"*, not the wording — and is strictly more information than the pre-migration behaviour of showing nothing. Improving the copy is a **new requirement**, not a migration defect.

---

## Deferred — recorded, deliberately not implemented

| Item | Note |
|---|---|
| **CI** | Three test suites exist and none gates anything. Everything below is cheaper once this lands. |
| **`frontend-maven-plugin`** | Decision C-5 deliberately did not add it. Without CI it buys nothing; with CI it removes the "wrong Node" failure class entirely. |
| **The three unpinned endpoints** | MockMvc pins 18 of 21; the claim-history PUTs are covered at runtime by Playwright instead. Worth closing if the contract suite becomes a gate. |
| **`<html lang="pl">` with English UI copy** | `frontend/src/index.html` declares Polish while every string on screen is English. Carried over unchanged, deliberately out of scope. `lang` drives font-subset selection and screen-reader pronunciation, so it is not cosmetic. **Not** in the defect register — adding it there is itself a decision to take. |
| **`@angular/cdk/layout`** | Not used. Add it only if a component ever needs to react to a breakpoint in TypeScript; the responsive layout is CSS today. |
| **`@angular/animations` provider** | A 2×2 differential showed `provideAnimationsAsync()` could be dropped entirely — `@angular/material@22.1.1` does not peer-depend on animations in v22. The plan was implemented as written and the provider kept; revisit if the 67.75 kB lazy chunk ever matters. |
| **Remaining EOL debt** | Out of frontend scope: Higson runtime version line, Spring Boot upgrades, H2 2.1.212. |
| **A code comment left stale** | `frontend/src/app/core/quote-store.ts` still says *"On a failed load `value()` is `undefined`"* on the `quote` resource. That is the misconception `standards/frontend/state.md` now corrects — the code around it is right, only the comment is wrong. |
