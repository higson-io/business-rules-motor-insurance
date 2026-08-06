import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import type { Page } from '@playwright/test';
import { STAGE } from './stage';

/**
 * The recorded evidence (plan steps 2.4 and 2.7).
 *
 * The in-spec assertions are the hard gate; these files are the artefact G11 diffs to
 * adjudicate. They are written per stage - `goldens/pre/` from the AngularJS build, then
 * `goldens/post/` from the Angular build - so the comparison G11 performs is a plain
 * directory diff, and every line of it must map to a step-2.5 delta row or to C-9.
 *
 * `e2e/goldens/` IS committed. It is captured evidence, not build output (spec.md 5.4):
 * a `git revert` restores code, it cannot recreate a runtime capture.
 */

const ROOT = join(__dirname, '..');

export class Golden {
	private readonly entries: Record<string, unknown> = {};

	constructor(private readonly flow: string) {}

	/** Records one observation. Keys are stable across stages so the G11 diff lines up. */
	record(key: string, value: unknown): void {
		this.entries[key] = value;
	}

	/**
	 * Documentation only (spec.md 8.5, C-9). Playwright's screenshot-comparison assertion is
	 * BANNED from every spec: with the whole UI redesigned, a before/after image diff cannot
	 * tell an intended redesign from a regression, so downstream phases must not report one as
	 * a finding. Capturing an image, as here, is fine; asserting on one is not.
	 */
	async screenshot(page: Page, name: string): Promise<void> {
		const file = join(ROOT, 'goldens', 'screenshots', STAGE, `${this.flow}-${name}.png`);
		mkdirSync(dirname(file), { recursive: true });
		await page.screenshot({ path: file, fullPage: true });
	}

	write(): void {
		const file = join(ROOT, 'goldens', STAGE, `${this.flow}.json`);
		mkdirSync(dirname(file), { recursive: true });
		const body = {
			flow: this.flow,
			stage: STAGE,
			timezone: 'UTC',
			observations: this.entries
		};
		writeFileSync(file, `${JSON.stringify(body, null, '\t')}\n`, 'utf8');
	}
}
