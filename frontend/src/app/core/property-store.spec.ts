import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { PropertyStore } from './property-store';
import { PropertyQuoteDto } from '../models/property';

const QUOTE: PropertyQuoteDto = {
	startDate: '2026-08-21',
	deductible: 0,
	regime: {
		code: 'STATUTORY',
		name: 'APK — minimum ustawowe',
		legalBasis: 'art. 8 ustawy o dystrybucji ubezpieczeń',
		requiresBudget: false,
		requiresExclusionCheck: false,
		requiresJustification: false,
		requiresVariantDifferences: false,
	},
	questions: [],
	floodZone: 'Z0',
	theftZone: 'T1',
	buildingAge: 20,
	riskFactor: 1,
	sumBuilding: 474000,
	sumFixtures: 104280,
	sumContents: 71100,
	suggestedSumBuilding: 474000,
	covers: [],
	variants: [],
	recommendation: {
		variantCode: null,
		coveredNeeds: 0,
		totalNeeds: 0,
		missingCovers: [],
		budgetOutcome: null,
		justification: [],
		needs: [],
		warnings: [],
	},
	selectedVariant: null,
};

describe('PropertyStore', () => {
	let httpTesting: HttpTestingController;
	let store: PropertyStore;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				provideZonelessChangeDetection(),
				provideHttpClient(),
				provideHttpClientTesting(),
			],
		});
		httpTesting = TestBed.inject(HttpTestingController);
		store = TestBed.inject(PropertyStore);
	});

	/** Loads the single startup resource and settles it, leaving no outstanding request. */
	async function startup(): Promise<void> {
		TestBed.tick();
		httpTesting.expectOne('property').flush(QUOTE);
		await TestBed.inject(ApplicationRef).whenStable();
	}

	it('refetches the quote exactly once per recalculate', async () => {
		await startup();

		store.recalculate();
		TestBed.tick();

		const reloads = httpTesting.match('property');
		expect(reloads.length, 'one recalculate must produce exactly one GET property').toBe(1);
		reloads[0].flush(QUOTE);

		TestBed.tick();
		httpTesting.expectNone('property');
		httpTesting.verify();
	});

	/**
	 * ⛔ The reason `reload()` is used instead of a `params` trigger. A params change drops the
	 * resource to loading WITHOUT a value, so the questionnaire would empty itself between every
	 * answer — on this screen that is not a flicker, it is the content disappearing.
	 */
	it('keeps the previous value readable while the reload is in flight', async () => {
		await startup();
		expect(store.quote.hasValue()).toBe(true);

		store.recalculate();
		TestBed.tick();

		expect(
			store.quote.hasValue(),
			'a reload must not blank the questionnaire while the new answer is being priced',
		).toBe(true);
		expect(store.quote.value()?.sumBuilding).toBe(474000);

		httpTesting.match('property')[0].flush(QUOTE);
		TestBed.tick();
		httpTesting.verify();
	});

	/**
	 * ⚠️ In Angular 22 a resource in the error state THROWS from `value()`. Everything that reads
	 * the quote goes through `hasValue()` first, so a failed load has to leave `hasValue()` false
	 * rather than take the screen down.
	 */
	it('reports a failed load as absence, not as a thrown value', async () => {
		TestBed.tick();
		httpTesting
			.expectOne('property')
			.flush('boom', { status: 500, statusText: 'Internal Server Error' });
		await TestBed.inject(ApplicationRef).whenStable();

		expect(store.quote.hasValue()).toBe(false);
		httpTesting.verify();
	});
});
