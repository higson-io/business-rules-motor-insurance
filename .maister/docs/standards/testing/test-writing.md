## Test Writing

### Test Behavior
Focus on what code does, not how it does it, to allow safe refactoring.

### Clear Names
Use descriptive names explaining what's tested and expected (`shouldReturnErrorWhenUserNotFound`).

### Mock External Dependencies
Isolate tests by mocking databases, APIs, and external services.

### Fast Execution
Keep unit tests fast (milliseconds) so developers run them frequently.

### Risk-Based Testing
Prioritize testing based on business criticality and likelihood of bugs.

### Balance Coverage and Velocity
Adjust test coverage based on project needs and team workflow.

### Critical Path Focus
Ensure core user workflows and critical business logic are well-tested.

### Appropriate Depth
Match edge case testing to the risk profile of the code.

---

## Project-Specific Standards

> **Read the first standard before making any statement about tests in this repository.** Automated tests are new here, arrived with the AngularJS → Angular migration, and are not gated by anything. Most meaningful coverage of this application's *business* behaviour still lives in Higson rule tests; see `standards/testing/rule-tests.md`.

### Current State: Young Suites, and No CI to Gate Them
Three suites now exist and are tracked in git, all added during the frontend migration:

| Suite | Location | Size | Runner |
|---|---|---|---|
| Java — MockMvc contract + snapshot engine | `src/test/java/` | 6 files, 11 tests | `mvn test` |
| Angular unit | `frontend/src/**/*.spec.ts` | 14 files, 27 tests | `npx ng test --watch=false` |
| Playwright behavioural goldens | `e2e/` | 28 tracked files, 6 flows | `npx playwright test` |

Calibrate claims accordingly:

- **No CI exists.** No test run has ever gated a merge, and none does now. "The tests pass" means someone ran them locally.
- **Only the Java suite runs during a build.** `mvn package` does not invoke Vitest or Playwright; `npm run build` is `npm ci && ng build` and nothing else.
- **The E2E harness needs a running application** and a populated Higson snapshot. It is not a self-contained suite.
- Before the migration there were **zero** tracked test files: the 7 that once existed were deleted in commit `6a8b20c` (2025-04-28) and no replacement arrived for over a year. There is no long-standing testing culture here to lean on.
- The MockMvc contract classes pin **18 of the 21 REST endpoints**; the three claim-history PUTs are covered at runtime by `e2e/specs/driver-history.spec.ts` instead. Worth closing if the contract suite ever becomes a CI gate.

*Evidence:* `git ls-files 'src/test/*' 'e2e/*'`; parity verdict §4 and §6.4; no CI configuration of any kind exists in the repository.

### Java Tests Use JUnit 5 via `spring-boot-starter-test`
The former JUnit 4 / JUnit 5 conflict is **resolved**: the explicit `junit:junit:4.13.2` dependency was removed and `spring-boot-starter-test` added, which supplies JUnit Jupiter, AssertJ and `spring-test`/MockMvc through the Spring Boot 3.5.16 BOM. Do not pin its version, and do not add JUnit 4 tests.

`maven-surefire-plugin` is configured with `-Duser.timezone=UTC`. This is not optional decoration: `Motor.createDriver` seeds `dateOfBirth` as "today − 40 years, start of day, system timezone", so an unpinned zone makes every date-carrying assertion machine-dependent.

*Evidence:* `pom.xml` Testing dependency block and surefire configuration.

### Test Classes Are Package-Private With Static Assertions
Tests use JUnit Jupiter with package-private test classes and methods, statically imported assertions, behaviour-describing method names without a `test` prefix, and an explanatory message on every assertion. Shared fixture setup lives in a static `@BeforeAll`.

```java
class SnapshotDataSourceDemoTest {

	@BeforeAll
	static void bootEngineFromSnapshot() { ... }

	@Test
	void readsParameterWithContext() {
		assertFalse(rows.isEmpty(), "expected available-makes rows for the given production year");
	}
}
```

*Evidence:* 3 of 3 test methods are package-private with descriptive names; 6 of 6 assertions statically imported, 4 of 6 carry a message. Confidence 60 — n=1, treat as a starting convention.

### Test Layout Mirrors the Main Source Package
Tests live under `src/test/java` mirroring the exact main package path, one test class per production class, named `<ClassUnderTest>Test` — never `Tests`, never Should/When style. The test tree followed the `pl.decerto.hyperon.*` → `pl.decerto.higson.*` package rename in lockstep, confirming the mirroring is deliberate.

*Evidence:* every test path ever tracked mirrors its main counterpart; naming uniform across all 7 historical files plus the 5 added by the migration. Confidence 65 — inferred largely from deleted history.

---

## Frontend Standards — Angular Unit Tests

The Angular workspace tests with **Vitest**, run through the Angular CLI's `@angular/build:unit-test` builder. Specs sit beside the file under test as `<name>.spec.ts`.

### Run Unit Tests Through the Angular CLI, Never Through Vitest Directly

```bash
cd frontend
npx ng test --watch=false        # or: npm test
```

⛔ **`npx vitest run` fails with `ReferenceError: describe is not defined`.** The Angular CLI — not a `vitest.config.ts` — supplies the test environment, the globals and the Angular compiler plugin. Invoking Vitest directly bypasses all of it. There is no Vitest config file in this workspace, and adding one to "fix" the failure would fork the configuration.

### Select Tests With `--filter`, Not `--include`
⛔ **`ng test --include=<dir>` is broken in this workspace**: it drops the Angular compiler plugin and fails with *"No loader is configured for .less files"* — a message that looks like a styling problem and is not. Select by suite name instead:

```bash
npx ng test --watch=false --filter="^QuoteStore"
```

### `TestBed.tick()` + `httpTesting.match()`, Not `ApplicationRef.whenStable()`
⛔ **`ApplicationRef.whenStable()` deadlocks here.** After flushing a mutation, `QuoteStore.recalculate()` leaves a `GET quote` outstanding in the testing backend, so stability is never reached and the test times out. Drive change detection explicitly and then handle the outstanding request:

```typescript
TestBed.tick();
const quoteRequests = httpTesting.match('quote');
```

⚠️ **`HttpTestingController.match()` consumes the requests it returns.** A test that asserts "these requests are in flight" by calling `match()` and checking the length never flushes them — and passes whether or not the code under test behaved correctly. If you match, you own what you matched: flush it or assert on it deliberately.

### Assert That Failures Are Handled, Not Merely That Nothing Rendered
A test asserting only the *absence* of output cannot distinguish graceful degradation from a thrown exception — a template that throws also renders nothing. This produced a green test over genuinely broken code during the migration (see the `hasValue()` rule in `standards/frontend/state.md`).

When testing an error path, also assert on a recording `ErrorHandler` provided into the `TestBed`. Absence of output plus absence of a handled error is the real assertion.

### Prove a Test Fails Before Trusting It
Every headline behavioural test in this workspace was verified to fail under the mutation it exists to catch — reintroducing a client-side sort, swapping `(blur)` for `(input)`, routing an extra vehicle field into `recalculate()`, replacing `[innerHTML]` with interpolation. Restore the source byte-identically afterwards.

Where a fixture can make a test vacuous, scramble it deliberately: the DR-05 ordering fixture arrives with `GOLD` before `BRONZE` and coverage positions `[9, 1]`, because a fixture already in the expected order would pass with or without a sort and prove nothing.

### Probe Code Must Not Live Under `frontend/src/`
A temporary, non-compiling spec left under `src/` breaks the shared build for anyone else working in the workspace. Keep exploratory probes outside `src/**`, or write them compile-clean.

---

## Frontend Standards — End-to-End Goldens

`e2e/` at the repository root holds six Playwright flows that capture and replay the SPA's **behaviour and request volume**. It lives outside `frontend/` deliberately: the same spec files must run unmodified against both frontends, and the harness had to exist before `frontend/` did. Do not move it into the workspace.

### Select by Test Id and Accessible Role, Never by Markup
Selectors keyed to CSS classes or DOM structure do not survive a UI redesign. Use `data-testid` attributes and accessible roles and labels, so one specification runs against two entirely different renderings.

### Goldens Are Committed Evidence; Screenshots Are Never a Gate
`e2e/goldens/` is committed. It is captured runtime evidence, not build output — a `git revert` restores code, it cannot recreate a capture.

Screenshots under `goldens/screenshots/` are **documentation only**. Playwright's screenshot-comparison assertion appears nowhere in `e2e/` and must never be added: with the whole UI redesigned, an image diff cannot distinguish an intended redesign from a regression. **A visual difference is never, on its own, a finding.**

### A Golden Is Never Edited to Make a Replay Pass
When a replay differs from the capture, the difference is either traceable to an enumerated, pre-agreed deviation — or it is a regression. Editing the golden defeats the purpose of having captured it. The harness enforces the discipline mechanically: its stage-branching helper rejects a call that does not name the deviation authorising the branch.

*Evidence:* `e2e/README.md`; the G11 parity verdict adjudicated all 35 substantive deltas without editing a golden.
