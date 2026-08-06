import { expect, test } from '@playwright/test';
import { chooseOption, openApp, optionLabels, settle, TESTID } from '../support/app';
import { Golden } from '../support/golden';
import { byStage } from '../support/stage';

/**
 * FLOW 4 - the four-level vehicle cascade.
 *
 * ⚠️ THIS IS THE MOST IMPORTANT SPEC IN THE SUITE.
 *
 * ND-1 / DR-13: only a vehicle MODEL change triggers a quote reload (`vehicle.js:76`).
 * Production year, make and type deliberately do NOT - and the consequence is user-visible:
 * from the moment the user touches Production Year until they re-pick a Model, the whole
 * premium panel shows the previous vehicle's prices.
 *
 * An idiomatic reactive rewrite reconnects those three by accident, multiplying `GET quote`
 * volume - and every full Higson evaluation behind it - while every other assertion in this
 * suite still passes. That is why the four `GET quote` assertions below carry NO stage
 * branch: their expected values must be byte-identical pre and post. If a later group makes
 * one of them fail, the migration has failed, not the test.
 */

const golden = new Golden('vehicle-cascade');

/** The literal placeholder entries (`vehicle.html:8,19,31,42`), excluded from dictionary reads. */
const PLACEHOLDERS = ['Production Year', 'Make', 'Type', 'Model'];

const realOptions = (labels: string[]): string[] =>
	labels.filter((l) => l !== '' && !PLACEHOLDERS.includes(l));

test.afterAll(() => golden.write());

test('the vehicle cascade reloads dictionaries but only Model reloads the quote', async ({
	page
}) => {
	const consoleErrors: string[] = [];
	page.on('pageerror', (error) => consoleErrors.push(`pageerror: ${error.message}`));
	page.on('console', (message) => {
		if (message.type() === 'error') {
			consoleErrors.push(`console.error: ${message.text()}`);
		}
	});

	const log = await openApp(page);
	const observed: Record<string, unknown> = {};

	const typesBeforeYearChange = realOptions(await optionLabels(page, TESTID.type));
	const modelsBeforeYearChange = realOptions(await optionLabels(page, TESTID.model));
	expect(
		typesBeforeYearChange.length,
		'the seeded vehicle populates all four dictionaries at startup'
	).toBeGreaterThan(0);

	await test.step('changing Production Year', async () => {
		log.reset();
		await chooseOption(page, TESTID.productionYear, '2012');
		await settle(page);

		expect(log.count('PUT', 'vehicle/productionYear'), 'the year is committed once').toBe(1);
		expect(
			log.count('GET', 'dictionaries/make'),
			'the make dictionary is reloaded for the new year'
		).toBe(1);

		// ⛔ NO BRANCH - ND-1 / DR-13. Deliberately unfixed behaviour.
		expect(
			log.count('GET', 'quote'),
			'ND-1 / DR-13: a production-year change must NOT reload the quote, on either frontend'
		).toBe(0);

		observed['productionYear'] = log.summary();
	});

	await test.step('the dependent dictionaries after a year change (DR-12)', async () => {
		const types = realOptions(await optionLabels(page, TESTID.type));
		const models = realOptions(await optionLabels(page, TESTID.model));
		observed['dictionariesAfterYearChange'] = {
			typeCount: types.length,
			modelCount: models.length
		};

		// DR-12: `clearDependantsVehicleFields` (`vehicle.js:102-107`) nulls the field values but
		// leaves `typeDictionary` and `modelDictionary` populated, so both dropdowns keep
		// offering the previous vehicle's options until a new Make is picked.
		if (byStage('DR-12', { pre: 'stale', post: 'empty' }) === 'empty') {
			expect(types, 'DR-12: the type dictionary is cleared with its field').toEqual([]);
			expect(models, 'DR-12: the model dictionary is cleared with its field').toEqual([]);
		} else {
			expect(types, 'DR-12: today the type dropdown keeps the previous vehicle options').toEqual(
				typesBeforeYearChange
			);
			expect(models, 'DR-12: today the model dropdown keeps the previous vehicle options').toEqual(
				modelsBeforeYearChange
			);
		}
	});

	await test.step('changing Make', async () => {
		const makes = realOptions(await optionLabels(page, TESTID.make));
		expect(makes, 'the make dictionary reloaded for 2012 offers AUDI').toContain('AUDI');

		log.reset();
		await chooseOption(page, TESTID.make, 'AUDI');
		await settle(page);

		expect(log.count('PUT', 'vehicle/make'), 'the make is committed once').toBe(1);
		expect(
			log.count('GET', 'dictionaries/type'),
			'the type dictionary is reloaded for the new make'
		).toBe(1);

		// ⛔ NO BRANCH - ND-1 / DR-13.
		expect(
			log.count('GET', 'quote'),
			'ND-1 / DR-13: a make change must NOT reload the quote, on either frontend'
		).toBe(0);

		observed['make'] = log.summary();
	});

	let chosenType = '';
	await test.step('changing Type', async () => {
		const types = realOptions(await optionLabels(page, TESTID.type));
		expect(types.length, 'the type dictionary reloaded for AUDI 2012').toBeGreaterThan(0);
		chosenType = types[0];

		log.reset();
		await chooseOption(page, TESTID.type, chosenType);
		await settle(page);

		expect(log.count('PUT', 'vehicle/type'), 'the type is committed once').toBe(1);
		expect(
			log.count('GET', 'dictionaries/model'),
			'the model dictionary is reloaded for the new type'
		).toBe(1);

		// ⛔ NO BRANCH - ND-1 / DR-13.
		expect(
			log.count('GET', 'quote'),
			'ND-1 / DR-13: a type change must NOT reload the quote, on either frontend'
		).toBe(0);

		observed['type'] = { chosen: chosenType, requests: log.summary() };
	});

	await test.step('changing Model - the ONE level that reloads the quote', async () => {
		const models = realOptions(await optionLabels(page, TESTID.model));
		expect(models.length, 'the model dictionary reloaded for the chosen type').toBeGreaterThan(0);
		const chosenModel = models[0];

		log.reset();
		await chooseOption(page, TESTID.model, chosenModel);
		await settle(page);

		expect(log.count('PUT', 'vehicle/model'), 'the model is committed once').toBe(1);

		// ⛔ NO BRANCH - ND-1 / DR-13. This is the positive half of the asymmetry: without it,
		// a migration that simply stopped recalculating altogether would also pass the three
		// zero-assertions above.
		expect(
			log.count('GET', 'quote'),
			'ND-1 / DR-13: a model change is the ONLY vehicle change that reloads the quote'
		).toBe(1);

		observed['model'] = { chosen: chosenModel, requests: log.summary() };
	});

	await test.step('selecting the blank Make option (DR-01)', async () => {
		consoleErrors.length = 0;
		log.reset();
		await chooseOption(page, TESTID.make, 'Make');
		await settle(page);

		observed['blankMake'] = { requests: log.summary(), consoleErrors: [...consoleErrors] };

		// No stage branch on the request count: today the `TypeError` is thrown before the PUT
		// is issued, and after the migration the guard suppresses it deliberately. Zero either
		// way - only the console output differs.
		expect(
			log.count('PUT', 'vehicle/make'),
			'DR-01: selecting the blank Make option issues no PUT vehicle/make'
		).toBe(0);

		// DR-01: `ctrl.makeDictionary.filter(...)[0].name` (`vehicle.js:61-63`) throws when the
		// blank placeholder empties `makeId` and the filter returns an empty array.
		if (byStage('DR-01', { pre: 'throws', post: 'guarded' }) === 'guarded') {
			expect(consoleErrors, 'DR-01: the make lookup is guarded, so nothing is thrown').toEqual([]);
		} else {
			expect(
				consoleErrors.join('\n'),
				'DR-01: today the unguarded make lookup throws an uncaught TypeError'
			).toContain('TypeError');
		}
	});

	golden.record('cascade', observed);
	await golden.screenshot(page, 'after-cascade');
});
