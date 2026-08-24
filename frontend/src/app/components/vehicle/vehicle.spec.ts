import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { Vehicle } from './vehicle';
import { DictionaryEntryDto } from '../../models/dictionary-entry';
import { APK_ABSENT } from '../../testing/shell-harness';
import { DriverDto } from '../../models/driver';
import { QuoteDto } from '../../models/quote';
import { VehicleDto } from '../../models/vehicle';

/** The seeded vehicle: every level of the cascade already populated (`Motor.createVehicle`). */
const SEED_VEHICLE: VehicleDto = {
	productionYear: 2011,
	makeId: 110,
	make: 'AUDI',
	typeId: 12360,
	modelId: 44001,
};

// `code` is a String on the wire even though every dictionary here is keyed by a number.
const YEARS: DictionaryEntryDto[] = [
	{ code: '2011', name: '2011' },
	{ code: '2012', name: '2012' },
];
const MAKES: DictionaryEntryDto[] = [
	{ code: '110', name: 'AUDI' },
	{ code: '120', name: 'BMW' },
];
const TYPES: DictionaryEntryDto[] = [{ code: '12360', name: '100 1.8 44' }];
const MODELS: DictionaryEntryDto[] = [{ code: '44001', name: '100 1.8 44 (sed, 66hp)' }];

const QUOTE: QuoteDto = {
	...APK_ABSENT, options: [] };

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

describe('Vehicle cascade', () => {
	let httpTesting: HttpTestingController;
	let fixture: ComponentFixture<Vehicle>;
	let component: Vehicle;

	beforeEach(async () => {
		TestBed.configureTestingModule({
			providers: [
				provideZonelessChangeDetection(),
				provideNoopAnimations(),
				provideHttpClient(),
				provideHttpClientTesting(),
			],
		});
		httpTesting = TestBed.inject(HttpTestingController);

		fixture = TestBed.createComponent(Vehicle);
		component = fixture.componentInstance;

		// The constructor loads the vehicle, and the vehicle drives which dictionaries load.
		httpTesting.expectOne('vehicle').flush(SEED_VEHICLE);
		httpTesting.expectOne('dictionaries/productionYear').flush(YEARS);
		httpTesting.expectOne('dictionaries/make?productionYear=2011').flush(MAKES);
		httpTesting.expectOne('dictionaries/type?makeId=110').flush(TYPES);
		httpTesting.expectOne('dictionaries/model?typeId=12360').flush(MODELS);

		// The store's two startup resources are settled here so that later `match('quote')`
		// counts only what the interaction under test caused.
		TestBed.tick();
		httpTesting.match('quote').forEach((request) => request.flush(QUOTE));
		httpTesting.match('driver').forEach((request) => request.flush(DRIVER));
		await TestBed.inject(ApplicationRef).whenStable();
	});

	/**
	 * Fires a `<mat-select>` selection through the TEMPLATE BINDING rather than by calling the
	 * handler directly. That distinction is the whole point of the DR-13 test below: a component
	 * whose handlers are correct but whose template is wired to something else would still pass a
	 * method-level test.
	 */
	function select(testid: string, value: number | null): void {
		const control = fixture.debugElement.query(By.css(`[data-testid="${testid}"]`));
		expect(control, `the ${testid} dropdown must be rendered`).not.toBeNull();
		control.triggerEventHandler('selectionChange', { value });
	}

	// DR-01. `vehicle.js:61-63` reads `…filter(…)[0].name` unguarded, so the blank placeholder
	// makes `filter` return `[]` and throws an uncaught TypeError before any request is issued.
	it('clears make, type, model and their dictionaries on the blank make option and issues no PUT', () => {
		select('vehicle-make', null);

		expect(component.vehicle().makeId, 'DR-01: the make id is cleared').toBe(0);
		expect(component.vehicle().make, 'DR-01: the make name is cleared').toBe('');
		expect(component.vehicle().typeId, 'DR-01: the dependent type is cleared').toBe(0);
		expect(component.vehicle().modelId, 'DR-01: the dependent model is cleared').toBe(0);
		expect(
			component.typeDictionary(),
			'DR-01: the Type dropdown is emptied, not left stale',
		).toEqual([]);
		expect(
			component.modelDictionary(),
			'DR-01: the Model dropdown is emptied, not left stale',
		).toEqual([]);

		httpTesting.expectNone('vehicle/make');
		httpTesting.verify();
	});

	// DR-12. `clearDependantsVehicleFields` (`vehicle.js:102-107`) nulls the field values only,
	// leaving both dependent dropdowns offering the previous vehicle's entries (the golden records
	// 305 types and 17 models surviving a year change).
	it('clears the type and model DICTIONARIES on a production-year change, not just the values', () => {
		expect(
			component.typeDictionary().length,
			'precondition: the seeded vehicle populates the dependent dictionaries',
		).toBe(1);

		select('vehicle-production-year', 2012);
		httpTesting.expectOne('vehicle/productionYear').flush(null);

		expect(component.typeDictionary(), 'DR-12: the type dictionary is cleared').toEqual([]);
		expect(component.modelDictionary(), 'DR-12: the model dictionary is cleared').toEqual([]);
		expect(component.vehicle().typeId, 'the server clears typeId too; mirror it locally').toBe(0);
		expect(component.vehicle().modelId, 'the server clears modelId too; mirror it locally').toBe(0);

		httpTesting.expectOne('dictionaries/make?productionYear=2012').flush(MAKES);
		httpTesting.verify();
	});

	// ⛔ DR-13 / ND-1 — THE trap of the whole migration, at the component level.
	//
	// An `effect()` over vehicle state, a `switchMap` off `valueChanges`, or one shared handler
	// that recalculates for every field all reconnect production year / make / type to the quote
	// BY ACCIDENT, multiplying `GET quote` volume — and every full Higson rules evaluation behind
	// it — while every other assertion in the suite still passes.
	it('reloads the quote only for a model change — year, make and type reload nothing', () => {
		select('vehicle-production-year', 2012);
		httpTesting.expectOne('vehicle/productionYear').flush(null);
		httpTesting.expectOne('dictionaries/make?productionYear=2012').flush(MAKES);

		select('vehicle-make', 110);
		httpTesting.expectOne('vehicle/make').flush(null);
		httpTesting.expectOne('dictionaries/type?makeId=110').flush(TYPES);

		select('vehicle-type', 12360);
		httpTesting.expectOne('vehicle/type').flush(null);
		httpTesting.expectOne('dictionaries/model?typeId=12360').flush(MODELS);

		TestBed.tick();
		expect(
			httpTesting.match('quote').length,
			'DR-13 / ND-1: production year, make and type must NOT reload the quote',
		).toBe(0);

		select('vehicle-model', 44001);
		httpTesting.expectOne('vehicle/model').flush(null);

		TestBed.tick();
		const reloads = httpTesting.match('quote');
		expect(
			reloads.length,
			'DR-13 / ND-1: a model change is the ONLY vehicle change that reloads the quote',
		).toBe(1);
		reloads[0].flush(QUOTE);

		httpTesting.verify();
	});
});
