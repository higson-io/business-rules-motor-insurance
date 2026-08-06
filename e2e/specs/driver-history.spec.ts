import { expect, test } from '@playwright/test';
import { openApp, settle, TESTID } from '../support/app';
import { Golden } from '../support/golden';
import { byStage } from '../support/stage';

/**
 * FLOW 3 - the three numeric claim-history fields: the largest request-volume change in the
 * migration (DR-03) and the verified HTTP 500 (DR-02).
 *
 * These fields have no `updateOn:'blur'` and no debounce today, so `ng-change` fires on every
 * keystroke - and when the field is cleared or violates `min`, `ng-model` becomes `undefined`,
 * `$http.put(url, undefined)` sends no body, Spring binds null, and `setNumberOfAccidents(null)`
 * unboxes into `int`. The failure path has no error handler, so the user sees nothing.
 */

const golden = new Golden('driver-history');

test.afterAll(() => golden.write());

test('the numeric claim-history fields commit, guard and recalculate as recorded', async ({
	page
}) => {
	const log = await openApp(page);
	const observed: Record<string, unknown> = {};

	await test.step('typing "12" into a numeric field then blurring (DR-03)', async () => {
		log.reset();
		const field = page.getByTestId(TESTID.accidents);
		// Select-all-then-type, the way a user replaces a seeded value. Clearing the field
		// first would be a different scenario entirely - that is the DR-02 step below, and
		// its empty commit would show up here as a third PUT.
		await field.click();
		await field.press('ControlOrMeta+a');
		await field.pressSequentially('12');
		await field.blur();
		await settle(page);

		// DR-03: today each digit is its own round trip - two PUTs and two full Higson
		// recalculations for one two-digit number. The Angular component commits on blur,
		// matching the seven text fields in the driver component.
		expect(
			log.count('PUT', 'driver/accidentCount'),
			'DR-03: today every keystroke commits; after the migration only the blur does'
		).toBe(byStage('DR-03', { pre: 2, post: 1 }));
		expect(
			log.count('GET', 'quote'),
			'DR-03: each keystroke commit drags a full quote recalculation with it'
		).toBe(byStage('DR-03', { pre: 2, post: 1 }));

		observed['accidentCount:type-12'] = log.summary();
	});

	await test.step('the quote survives the edit', async () => {
		const cards = await page.getByTestId(TESTID.variantCard).count();
		expect(cards, 'three variant cards still render after a successful edit').toBe(3);
	});

	await test.step('clearing a numeric field (DR-02)', async () => {
		const premiumsBefore = await page.getByTestId(TESTID.variantPremium).allInnerTexts();

		log.reset();
		const field = page.getByTestId(TESTID.accidents);
		await field.click();
		await field.fill('');
		await field.blur();
		await settle(page);

		const puts = log.count('PUT', 'driver/accidentCount');
		const errors = log.serverErrors();
		observed['accidentCount:cleared'] = {
			puts,
			requests: log.summary(),
			responses: log.allResponses()
		};

		// DR-02: an empty numeric field must not be sent at all.
		if (byStage('DR-02', { pre: 'sends-and-fails', post: 'guarded' }) === 'guarded') {
			expect(puts, 'DR-02: a cleared numeric field must not be committed').toBe(0);
			expect(errors, 'DR-02: nothing is sent, so nothing can 5xx').toEqual([]);
		} else {
			expect(puts, 'DR-02: today a cleared numeric field is still committed').toBeGreaterThanOrEqual(
				1
			);
			expect(
				errors.length,
				'DR-02: the empty body unboxes into `int` server-side and produces a verified HTTP 500'
			).toBeGreaterThanOrEqual(1);
		}

		// No stage branch: on both frontends the previously rendered quote stays on screen.
		// Today because the failure is silently swallowed, afterwards because the guard means
		// no request was made at all - the observable outcome is identical either way.
		const premiumsAfter = await page.getByTestId(TESTID.variantPremium).allInnerTexts();
		expect(
			premiumsAfter,
			'the last good quote stays rendered when a numeric field is cleared'
		).toEqual(premiumsBefore);
	});

	await test.step('a value below `min` is guarded the same way', async () => {
		log.reset();
		// `min=16` on "age when licence was obtained" (`driverHistory.html:24`); the other two
		// fields carry `min=0`.
		const field = page.getByTestId(TESTID.licenceAge);
		await field.click();
		await field.fill('');
		await field.pressSequentially('9');
		await field.blur();
		await settle(page);

		const puts = log.count('PUT', 'driver/licenceObtainedAtAge');
		observed['licenceObtainedAtAge:below-min'] = {
			puts,
			requests: log.summary(),
			responses: log.allResponses()
		};

		if (byStage('DR-02', { pre: 'sends-and-fails', post: 'guarded' }) === 'guarded') {
			expect(puts, 'DR-02: a value below `min` must not be committed').toBe(0);
			expect(log.serverErrors(), 'DR-02: nothing is sent, so nothing can 5xx').toEqual([]);
		} else {
			expect(
				puts,
				'DR-02: today the `min` violation empties the model and the empty body is still sent'
			).toBeGreaterThanOrEqual(1);
		}
	});

	golden.record('interactions', observed);
	await golden.screenshot(page, 'after-history-edits');
});
