/**
 * ⚠️ The suite is timezone-relative, so the timezone is pinned here.
 *
 * `driver.spec.ts` asserts the exact instant committed for a typed birth date, which is midnight
 * LOCAL time. Run from Europe/Warsaw that is 22:00Z the previous day and the assertion fails on a
 * correct application. Maven does the same thing for the Java suite (`-Duser.timezone=UTC` in
 * surefire); this is the frontend half of that decision, and without it the suite passes or fails
 * depending on where the machine is.
 *
 * ⚠️ Reached through `globalThis` rather than the bare `process`, for the same reason
 * `testing/shell-harness.ts` stays compile-clean: `tsconfig.app.json` type-checks every `.ts` under
 * `src/` that is not a spec, and the application has no Node types. Nothing imports this file
 * outside the test runner, so none of it reaches the bundle.
 */
const node = globalThis as unknown as { process?: { env: Record<string, string> } };

if (node.process) {
	node.process.env['TZ'] = 'UTC';
}
