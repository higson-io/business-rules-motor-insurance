import { defineConfig, devices } from '@playwright/test';

/**
 * Golden capture / replay harness for the AngularJS -> Angular 22 migration.
 *
 * The same spec files run against BOTH frontends. Only the environment differs
 * (`MIGRATION_STAGE=pre|post`) - see `support/stage.ts` and step 2.5 of the
 * implementation plan.
 *
 * P-4 (spec.md 8.6): the timezone is pinned in three places. Two of them are here
 * (`timezoneId` below, and `TZ=UTC` in the npm scripts); the third is the
 * application under test, which must be launched with `TZ=UTC -Duser.timezone=UTC`.
 * `Motor.createDriver` seeds "today - 40 years, start-of-day, SYSTEM TZ", so an
 * unpinned zone makes every date-carrying golden machine-dependent.
 */
export default defineConfig({
	testDir: './specs',

	// Request counting is a first-class assertion (plan 2.4). Parallel workers would
	// not corrupt each other's counts - each browser context gets its own JSESSIONID and
	// therefore its own @SessionScope Quote - but serial execution keeps the recorded
	// goldens deterministic and keeps Higson evaluation load off the critical path.
	fullyParallel: false,
	workers: 1,

	// A golden capture must never be "fixed" by a retry.
	retries: 0,
	forbidOnly: true,

	reporter: [['list'], ['json', { outputFile: 'test-results/results.json' }]],

	use: {
		baseURL: process.env.APP_BASE_URL ?? 'http://localhost:48080/demo/',
		timezoneId: 'UTC',
		locale: 'en-US',
		trace: 'retain-on-failure',
		// Screenshots are DOCUMENTATION ONLY (spec.md 8.5, C-9). Playwright's
		// screenshot-comparison assertion is BANNED from every spec - the acceptance
		// criterion for G2 is that grepping this directory for its name finds nothing.
		// A visual difference is never, on its own, a finding.
		screenshot: 'off',
		video: 'off'
	},

	projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }]
});
