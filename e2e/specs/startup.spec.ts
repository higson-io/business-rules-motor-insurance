import { expect, test } from '@playwright/test';
import { defaultDateOfBirth, openApp, selectedLabel, TESTID, textsOf } from '../support/app';
import { Golden } from '../support/golden';
import { byStage } from '../support/stage';

/**
 * FLOW 1 - initial render, server-seeded defaults, and the startup request volume.
 *
 * ND-6 is deliberately NOT asserted here. The startup spinner flash comes from the eight
 * AngularJS `templateUrl` XHRs and disappears by construction once templates are compiled;
 * it cannot be reproduced without an artificial delay, and is a sanctioned disappearance
 * (OD-13). Asserting it would fail the migration for doing the right thing.
 */

const golden = new Golden('startup');

test.afterAll(() => golden.write());

test('startup renders the seeded quote and issues the expected requests', async ({ page }) => {
	const log = await openApp(page);

	await test.step('the server-seeded driver is rendered', async () => {
		const driver = {
			firstName: await page.getByTestId(TESTID.firstName).inputValue(),
			lastName: await page.getByTestId(TESTID.lastName).inputValue(),
			dateOfBirth: await page.getByTestId(TESTID.dateOfBirth).inputValue(),
			zipCode: await page.getByTestId(TESTID.zipCode).inputValue(),
			city: await page.getByTestId(TESTID.city).inputValue(),
			street: await page.getByTestId(TESTID.street).inputValue(),
			accidents: await page.getByTestId(TESTID.accidents).inputValue(),
			tickets: await page.getByTestId(TESTID.tickets).inputValue(),
			licenceAge: await page.getByTestId(TESTID.licenceAge).inputValue()
		};

		expect(driver.firstName, 'first name is seeded by Motor.createDriver').toBe('John');
		expect(driver.lastName, 'last name is seeded by Motor.createDriver').toBe('Potter');
		expect(driver.city, 'city is seeded by Motor.createAddress').toBe('Lake Jackson');
		expect(driver.street, 'street is seeded by Motor.createAddress').toBe('Allwood St');
		expect(driver.zipCode, 'zip code is seeded by Motor.createAddress').toBe('77566');
		expect(driver.licenceAge, 'licence age is seeded at 18').toBe('18');
		expect(
			driver.dateOfBirth,
			'date of birth is "today - 40 years, start of day" - deterministic only because P-4 pins the zone to UTC on both the server and the browser'
		).toBe(defaultDateOfBirth());

		// The visual "which gender is selected" state is unassertable under the selector
		// policy: the AngularJS build signals it only through the Bootstrap `.active` class
		// and sets no `aria-pressed`. The gender VALUE is pinned by the G1 MockMvc golden,
		// and the gender BEHAVIOUR by driver-fields.spec.ts (DR-08).
		golden.record('driver', { ...driver, dateOfBirth: '<today-40y>' });
	});

	await test.step('the server-seeded vehicle is rendered across all four levels', async () => {
		const vehicle = {
			productionYear: await selectedLabel(page, TESTID.productionYear),
			make: await selectedLabel(page, TESTID.make),
			type: await selectedLabel(page, TESTID.type),
			model: await selectedLabel(page, TESTID.model)
		};
		expect(vehicle.productionYear, 'production year is seeded at 2010').toBe('2010');
		expect(vehicle.make, 'make is seeded at TOYOTA (makeId 217)').toBe('TOYOTA');
		expect(vehicle.type, 'type is seeded at typeId 28654').toBe('Avensis Wagon MR/09');
		expect(vehicle.model, 'model is seeded at modelId 218915').toBe(
			'Avensis Wagon MR/09 (Sol, cb, 112hp)'
		);
		golden.record('vehicle', vehicle);
	});

	await test.step('three variant cards render in backend order', async () => {
		const codes = await textsOf(page, TESTID.variantCode);
		expect(codes, 'the three rating options render in backend `order`, BRONZE/SILVER/GOLD').toEqual(
			['BRONZE', 'SILVER', 'GOLD']
		);
		golden.record('variantCodes', codes);
	});

	await test.step('the startup request volume is pinned', async () => {
		// A tally, not an ordered list: the startup calls race with each other, so their
		// relative order is not a behavioural property and would flap between runs.
		golden.record('startupRequests', log.tally());

		expect(
			log.count('GET', 'driver'),
			'DR-09: today the driver and driver-history components each fetch the driver independently; after the migration one shared QuoteStore fetch serves both'
		).toBe(byStage('DR-09', { pre: 2, post: 1 }));

		expect(log.count('GET', 'vehicle'), 'the vehicle is fetched exactly once').toBe(1);
		expect(log.count('GET', 'quote'), 'the quote is fetched exactly once at startup').toBe(1);

		expect(
			log.count('GET', 'dictionaries/productionYear'),
			'the production-year dictionary always loads'
		).toBe(1);
		expect(
			log.count('GET', 'dictionaries/make'),
			'the seeded productionYear=2010 is truthy, so the make dictionary loads'
		).toBe(1);
		expect(
			log.count('GET', 'dictionaries/type'),
			'the seeded makeId=217 is truthy, so the type dictionary loads'
		).toBe(1);
		expect(
			log.count('GET', 'dictionaries/model'),
			'the seeded typeId=28654 is truthy, so the model dictionary loads'
		).toBe(1);

		expect(
			log.offContract(),
			'the 21-endpoint REST contract is frozen (spec.md 6) - no other application request may appear'
		).toEqual([]);
	});

	await golden.screenshot(page, 'initial');
});
