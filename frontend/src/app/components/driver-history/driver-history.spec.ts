import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { DriverHistory } from './driver-history';
import { DriverDto } from '../../models/driver';
import { QuoteDto } from '../../models/quote';

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

const QUOTE: QuoteDto = { options: [] };

describe('Driver history numeric fields', () => {
	let httpTesting: HttpTestingController;
	let fixture: ComponentFixture<DriverHistory>;
	let component: DriverHistory;

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

		fixture = TestBed.createComponent(DriverHistory);
		component = fixture.componentInstance;
		TestBed.tick();
	});

	/** Settles the store's two startup resources and renders the seeded values. */
	async function startup(): Promise<void> {
		httpTesting.match('driver').forEach((request) => request.flush(DRIVER));
		httpTesting.match('quote').forEach((request) => request.flush(QUOTE));
		await TestBed.inject(ApplicationRef).whenStable();
	}

	function input(testid: string): HTMLInputElement {
		const element = fixture.debugElement.query(By.css(`[data-testid="${testid}"]`));
		expect(element, `the ${testid} field must be rendered`).not.toBeNull();
		return element.nativeElement as HTMLInputElement;
	}

	/** Types into a field the way a user does, without committing it. */
	function type(field: HTMLInputElement, value: string): void {
		field.value = value;
		field.dispatchEvent(new Event('input'));
	}

	// DR-03 — the largest request-volume change in the migration. `ng-change` with no
	// `updateOn: 'blur'` and no debounce turns typing `12` into two PUTs and two full Higson
	// recalculations; the seven text fields in `driver.html` already commit on blur.
	it('commits a numeric field on blur, not on every keystroke', async () => {
		await startup();
		const accidents = input('driver-accidents');

		type(accidents, '1');
		type(accidents, '12');
		httpTesting.expectNone('driver/accidentCount');

		accidents.dispatchEvent(new Event('blur'));

		const puts = httpTesting.match('driver/accidentCount');
		expect(puts.length, 'DR-03: typing `12` then blurring must produce exactly one PUT').toBe(1);
		expect(puts[0].request.body, 'the committed value is the number, not a string').toBe(12);
		puts[0].flush(null);

		TestBed.tick();
		expect(
			httpTesting.match('quote').length,
			'DR-03: one commit drags exactly one quote recalculation with it',
		).toBe(1);

		httpTesting.verify();
	});

	// DR-02 — clearing the field makes `ng-model` `undefined`, `$http.put` sends no body, Spring
	// binds null and unboxing into `int` throws: a verified HTTP 500 that no handler surfaces, so
	// the user sees nothing at all.
	it('sends nothing for a cleared or below-min value and keeps the last valid value', async () => {
		await startup();

		const accidents = input('driver-accidents');
		type(accidents, '');
		accidents.dispatchEvent(new Event('blur'));
		httpTesting.expectNone('driver/accidentCount');
		expect(
			component.accidents(),
			'DR-02: the last valid server value survives a cleared field',
		).toBe(0);

		// `min=16` on "age when driver licence was obtained" (`driverHistory.html:24`).
		const licenceAge = input('driver-licence-age');
		type(licenceAge, '9');
		licenceAge.dispatchEvent(new Event('blur'));
		httpTesting.expectNone('driver/licenceObtainedAtAge');
		expect(
			component.licenceAge(),
			'DR-02: a value below `min` is not committed and does not disturb the model',
		).toBe(18);

		// The boundary is inclusive, so the guard must not reject a legitimate edit.
		type(licenceAge, '16');
		licenceAge.dispatchEvent(new Event('blur'));
		httpTesting.expectOne('driver/licenceObtainedAtAge').flush(null);
		expect(component.licenceAge(), 'the `min` value itself is a valid commit').toBe(16);

		TestBed.tick();
		httpTesting.match('quote').forEach((request) => request.flush(QUOTE));
		httpTesting.verify();
	});

	// DR-09 — `driverHistory.js:30-36` fetched the driver a second time, independently of
	// `driver.js:53-59`, for the same session-scoped aggregate.
	it('reads the driver from the store and issues no second GET driver', async () => {
		const driverRequests = httpTesting.match('driver');
		expect(driverRequests.length, 'DR-09: the driver is fetched exactly once, by the store').toBe(
			1,
		);
		driverRequests[0].flush(DRIVER);
		httpTesting.match('quote').forEach((request) => request.flush(QUOTE));
		await TestBed.inject(ApplicationRef).whenStable();

		expect(input('driver-accidents').value, 'the seeded accident count is rendered').toBe('0');
		expect(input('driver-tickets').value, 'the seeded ticket count is rendered').toBe('0');
		expect(input('driver-licence-age').value, 'the seeded licence age is rendered').toBe('18');

		httpTesting.expectNone('driver');
		httpTesting.verify();
	});
});
