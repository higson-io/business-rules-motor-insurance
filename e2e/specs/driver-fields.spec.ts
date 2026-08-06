import { expect, test } from '@playwright/test';
import { openApp, settle, TESTID, typeAndBlur } from '../support/app';
import { Golden } from '../support/golden';
import { byStage } from '../support/stage';

/**
 * FLOW 2 - the seven driver fields, the datepicker (DR-04) and the gender toggle (DR-08).
 *
 * Every edit in this component is a blur-committed round trip: one PUT, then one `GET quote`
 * driven by the recalculate relay. That 1 + 1 shape carries NO stage branch - it is
 * current behaviour that must survive the migration untouched.
 */

const golden = new Golden('driver-fields');

test.afterAll(() => golden.write());

/** The five plain text fields, with a value that differs from the server-seeded default. */
const TEXT_FIELDS = [
	{ testid: TESTID.firstName, endpoint: 'driver/firstName', value: 'Jane' },
	{ testid: TESTID.lastName, endpoint: 'driver/lastName', value: 'Weasley' },
	{ testid: TESTID.zipCode, endpoint: 'driver/address/zipCode', value: '30001' },
	{ testid: TESTID.city, endpoint: 'driver/address/city', value: 'Krakow' },
	{ testid: TESTID.street, endpoint: 'driver/address/street', value: 'Dluga 5' }
] as const;

test('each driver field commits on blur and triggers exactly one recalculation', async ({
	page
}) => {
	const log = await openApp(page);
	const observed: Record<string, unknown> = {};

	for (const field of TEXT_FIELDS) {
		await test.step(`editing ${field.endpoint} sends one PUT and one GET quote`, async () => {
			log.reset();
			await typeAndBlur(page.getByTestId(field.testid), field.value);
			await settle(page);

			expect(
				log.count('PUT', field.endpoint),
				`${field.endpoint} commits once, on blur - not on every keystroke`
			).toBe(1);
			expect(
				log.count('GET', 'quote'),
				`${field.endpoint} triggers exactly one full recalculation`
			).toBe(1);
			expect(log.summary(), `${field.endpoint} issues no other request`).toHaveLength(2);

			observed[field.endpoint] = log.summary();
		});
	}

	await test.step('typing a full date then blurring (DR-04)', async () => {
		log.reset();
		const input = page.getByTestId(TESTID.dateOfBirth);
		await input.click();
		await input.fill('');
		await input.pressSequentially('1990-05-12');
		await input.blur();
		await settle(page);

		const puts = log.count('PUT', 'driver/birthDate');
		observed['driver/birthDate'] = { puts, requests: log.summary() };

		// DR-04: `driver.html:23-32` has no `updateOn:'blur'`, so `ng-change` fires on every
		// keystroke that parses as a valid date. The Angular component commits on
		// `(closed)`/`(blur)` only.
		if (byStage('DR-04', { pre: 'many', post: 'one' }) === 'one') {
			expect(puts, 'DR-04: the datepicker commits exactly once, on close or blur').toBe(1);
		} else {
			expect(
				puts,
				'DR-04: today every keystroke that parses as a date fires its own PUT driver/birthDate'
			).toBeGreaterThan(1);
		}

		expect(
			page.getByTestId(TESTID.dateOfBirth),
			'the typed date is displayed back in yyyy-MM-dd'
		).toHaveValue('1990-05-12');
	});

	await test.step('switching the gender toggle sends one PUT and one GET quote', async () => {
		log.reset();
		await page.getByTestId(TESTID.genderFemale).click();
		await settle(page);

		// No stage branch: a genuine change must behave identically on both frontends.
		expect(log.count('PUT', 'driver/gender'), 'a real gender change commits once').toBe(1);
		expect(log.count('GET', 'quote'), 'a real gender change triggers one recalculation').toBe(1);
		observed['driver/gender:change'] = log.summary();
	});

	await test.step('re-clicking the already-active gender toggle (DR-08)', async () => {
		log.reset();
		await page.getByTestId(TESTID.genderFemale).click();
		await settle(page);

		// DR-08: today `ng-click` and `uib-btn-radio` sit on the same element; `uncheckable` is
		// not set, so re-clicking leaves the model unchanged but still fires `ng-click`, costing
		// a redundant PUT and a full Higson recalculation. `MatButtonToggleGroup.change` does not
		// fire at all in that case.
		expect(
			log.count('PUT', 'driver/gender'),
			'DR-08: re-clicking the active toggle currently still issues a redundant PUT'
		).toBe(byStage('DR-08', { pre: 1, post: 0 }));
		expect(
			log.count('GET', 'quote'),
			'DR-08: the redundant PUT currently drags a full recalculation with it'
		).toBe(byStage('DR-08', { pre: 1, post: 0 }));

		observed['driver/gender:reclick'] = log.summary();
	});

	golden.record('perFieldRequests', observed);
	await golden.screenshot(page, 'after-edits');
});
