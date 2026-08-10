import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { QuoteStore } from './quote-store';
import { DriverDto } from '../models/driver';
import { QuoteDto } from '../models/quote';

const QUOTE: QuoteDto = {
	options: [
		{
			code: 'BRONZE',
			order: 1,
			premium: 1000,
			premiumBeforeDiscounts: 1200,
			coverages: [],
			discounts: [],
		},
	],
};

const DRIVER: DriverDto = {
	firstName: 'John',
	lastName: 'Potter',
	dateOfBirth: '1986-08-06T00:00:00.000+00:00',
	gender: 'M',
	address: { zipCode: '77566', city: 'Lake Jackson', street: 'Allwood St' },
	numberOfAccidents: 0,
	numberOfTickets: 0,
	licenceObtainedAtAge: 18,
};

describe('QuoteStore', () => {
	let httpTesting: HttpTestingController;
	let store: QuoteStore;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				provideZonelessChangeDetection(),
				provideHttpClient(),
				provideHttpClientTesting(),
			],
		});
		httpTesting = TestBed.inject(HttpTestingController);
		store = TestBed.inject(QuoteStore);
	});

	/** Loads the two startup resources and settles them, leaving no outstanding request. */
	async function startup(): Promise<void> {
		TestBed.tick();
		httpTesting.expectOne('quote').flush(QUOTE);
		httpTesting.expectOne('driver').flush(DRIVER);
		await TestBed.inject(ApplicationRef).whenStable();
	}

	it('bumps the reload trigger on recalculate and refetches the quote exactly once', async () => {
		await startup();

		store.recalculate();
		TestBed.tick();

		const reloads = httpTesting.match('quote');
		expect(reloads.length, 'one recalculate must produce exactly one GET quote').toBe(1);
		reloads[0].flush(QUOTE);

		TestBed.tick();
		httpTesting.expectNone('quote');
		httpTesting.verify();
	});

	// ⚠️ DR-13 / ND-1 — the single most important unit test in the migration.
	it('reloads the quote only for a model change — year, make and type do not', async () => {
		await startup();

		store.vehicleFieldChanged('productionYear');
		store.vehicleFieldChanged('make');
		store.vehicleFieldChanged('type');
		TestBed.tick();

		expect(
			httpTesting.match('quote').length,
			'production year, make and type must NOT reload the quote (DR-13 / ND-1)',
		).toBe(0);

		store.vehicleFieldChanged('model');
		TestBed.tick();

		const reloads = httpTesting.match('quote');
		expect(reloads.length, 'a model change must reload the quote exactly once').toBe(1);
		reloads[0].flush(QUOTE);
		httpTesting.verify();
	});

	it('fetches the driver exactly once and shares it between consumers', async () => {
		const otherConsumer = TestBed.inject(QuoteStore);
		expect(otherConsumer, 'the store is a singleton, so both consumers share one resource').toBe(
			store,
		);

		TestBed.tick();
		const driverRequests = httpTesting.match('driver');
		expect(driverRequests.length, 'two consumers must produce one GET driver (DR-09)').toBe(1);
		driverRequests[0].flush(DRIVER);
		httpTesting.expectOne('quote').flush(QUOTE);

		await TestBed.inject(ApplicationRef).whenStable();
		expect(store.driver.value()?.firstName, 'the shared driver value is readable').toBe('John');
		expect(otherConsumer.driver.value(), 'both consumers read the identical value').toBe(
			store.driver.value(),
		);
		httpTesting.expectNone('driver');
		httpTesting.verify();
	});
});
