import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DateAdapter, MAT_DATE_FORMATS } from '@angular/material/core';
import { MatDatepicker } from '@angular/material/datepicker';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { Driver } from './driver';
import { IsoDateAdapter, MOTOR_DATE_FORMATS } from '../../core/iso-date.adapter';
import { APK_ABSENT } from '../../testing/shell-harness';
import { DriverDto } from '../../models/driver';
import { QuoteDto } from '../../models/quote';

/**
 * ⚠️ `dateOfBirth` is an ISO-8601 instant with the SERVER's offset — never epoch millis. Under
 * `TZ=UTC` (P-4) it names 6 August 1986 in browser-local time too.
 */
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

const QUOTE: QuoteDto = {
	...APK_ABSENT, options: [] };

describe('Driver form', () => {
	let httpTesting: HttpTestingController;
	let fixture: ComponentFixture<Driver>;
	let component: Driver;

	beforeEach(async () => {
		TestBed.configureTestingModule({
			providers: [
				provideZonelessChangeDetection(),
				provideNoopAnimations(),
				provideHttpClient(),
				provideHttpClientTesting(),
				// The same two providers `app.config.ts` installs application-wide.
				{ provide: DateAdapter, useClass: IsoDateAdapter },
				{ provide: MAT_DATE_FORMATS, useValue: MOTOR_DATE_FORMATS },
			],
		});
		httpTesting = TestBed.inject(HttpTestingController);

		fixture = TestBed.createComponent(Driver);
		component = fixture.componentInstance;
		TestBed.tick();

		// DR-09: the driver is fetched once, by the store, and shared. The component adds no
		// `GET driver` of its own.
		httpTesting.match('driver').forEach((request) => request.flush(DRIVER));
		httpTesting.match('quote').forEach((request) => request.flush(QUOTE));
		await TestBed.inject(ApplicationRef).whenStable();
	});

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

	/** One `GET quote` per accepted mutation — every driver field recalculates (`driver.js:96-98`). */
	function flushRecalculation(expected: number, because: string): void {
		TestBed.tick();
		const reloads = httpTesting.match('quote');
		expect(reloads.length, because).toBe(expected);
		reloads.forEach((request) => request.flush(QUOTE));
	}

	// `ng-model-options="{updateOn: 'blur'}"` on all five text fields
	// (`driver.html:7,17,68,78,88`), plus the guard `ng-change` gave for free: it fires only when
	// the model actually CHANGES, so blurring a field nobody touched costs nothing. A bare `(blur)`
	// handler would PUT on every blur — a request-volume increase traceable to no register row.
	it('text fields commit on blur, not on keystroke', () => {
		const firstName = input('driver-first-name');
		expect(firstName.value, 'the seeded first name is rendered').toBe('John');

		type(firstName, 'J');
		type(firstName, 'Ja');
		type(firstName, 'Jane');
		httpTesting.expectNone('driver/firstName');
		flushRecalculation(0, 'typing alone triggers no recalculation');

		firstName.dispatchEvent(new Event('blur'));
		const put = httpTesting.expectOne('driver/firstName');
		expect(put.request.body, 'the raw string goes on the wire, never JSON-quoted').toBe('Jane');
		expect(typeof put.request.body, 'a string body goes out as text/plain').toBe('string');
		put.flush(null);
		flushRecalculation(1, 'an accepted text edit drags exactly one recalculation with it');

		// The unchanged guard: `ng-change` never fired for a blur that changed nothing.
		firstName.dispatchEvent(new Event('blur'));
		httpTesting.expectNone('driver/firstName');
		flushRecalculation(0, 'blurring an unchanged field spends no rules evaluation');

		// The address subtree is wired through the same guard and the same string branch.
		const city = input('driver-city');
		expect(city.value, 'the seeded city is rendered').toBe('Lake Jackson');
		city.dispatchEvent(new Event('blur'));
		httpTesting.expectNone('driver/address/city');

		type(city, 'Krakow');
		city.dispatchEvent(new Event('blur'));
		const cityPut = httpTesting.expectOne('driver/address/city');
		expect(
			cityPut.request.body,
			'a JSON-quoted body would be stored WITH its quotes: `"Krakow"`',
		).toBe('Krakow');
		cityPut.flush(null);
		flushRecalculation(1, 'the address fields recalculate like every other driver field');

		httpTesting.verify();
	});

	// ⛔ DR-04. `driver.html:23-35` carries no `updateOn: 'blur'`, so every keystroke that parses as
	// a valid date fires its own `PUT driver/birthDate` AND its own full Higson recalculation — the
	// golden records THREE PUTs for one typed date. Here the commit happens on `(closed)` and
	// `(blur)` only.
	it('the birth date commits once, on datepicker close or input blur, never on keystroke', () => {
		const birthDate = input('driver-date-of-birth');
		expect(
			birthDate.value,
			'the server ISO-8601 instant is displayed as the day it names (P-4: TZ=UTC)',
		).toBe('1986-08-06');

		// `1990-05-1` and `1990-05-12` both parse — exactly the intermediate states that cost a
		// PUT each in the AngularJS build.
		type(birthDate, '1990-0');
		type(birthDate, '1990-05-1');
		type(birthDate, '1990-05-12');
		httpTesting.expectNone('driver/birthDate');
		flushRecalculation(0, 'DR-04: typing a date triggers no recalculation');

		birthDate.dispatchEvent(new Event('blur'));
		const put = httpTesting.expectOne('driver/birthDate');
		expect(
			put.request.body instanceof Date,
			'⛔ the body is a Date OBJECT — a string arrives as text/plain and Jackson answers 415',
		).toBe(true);
		expect(
			(put.request.body as Date).toISOString(),
			'the committed instant is the typed day',
		).toBe('1990-05-12T00:00:00.000Z');
		put.flush(null);
		flushRecalculation(1, 'DR-04: one date entry, exactly one recalculation');

		// Blur fires again when the calendar hands focus back; the unchanged guard is what keeps
		// the pair of commit paths from costing two PUTs.
		birthDate.dispatchEvent(new Event('blur'));
		httpTesting.expectNone('driver/birthDate');

		// The other sanctioned commit path: the calendar closing.
		const picker = fixture.debugElement.query(By.directive(MatDatepicker));
		expect(picker, 'the birth date is backed by a MatDatepicker').not.toBeNull();

		picker.triggerEventHandler('closed', undefined);
		httpTesting.expectNone('driver/birthDate');

		type(birthDate, '1991-06-13');
		picker.triggerEventHandler('closed', undefined);
		const onClose = httpTesting.expectOne('driver/birthDate');
		expect((onClose.request.body as Date).toISOString(), 'closing the calendar commits').toBe(
			'1991-06-13T00:00:00.000Z',
		);
		onClose.flush(null);
		flushRecalculation(1, 'DR-04: closing the calendar costs one recalculation, not three');

		birthDate.dispatchEvent(new Event('blur'));
		httpTesting.expectNone('driver/birthDate');
		flushRecalculation(0, 'DR-04: the trailing blur after a calendar pick commits nothing');

		httpTesting.verify();
	});

	// ⛔ DR-08. `driver.html:38-53` puts `ng-click` and `uib-btn-radio` on the same button with no
	// `uncheckable`, so re-clicking the active choice leaves the model unchanged yet still fires
	// `ng-click` — a redundant PUT and a full Higson evaluation for a no-op. The golden records
	// 1 PUT + 1 GET quote for that click; `MatButtonToggleGroup` does not emit at all.
	it('re-clicking the already-selected gender toggle issues no PUT', () => {
		expect(component.gender(), 'the seeded driver is male').toBe('M');

		const female = fixture.debugElement.query(By.css('[data-testid="driver-gender-female"]'));
		expect(female, 'the Female toggle must be rendered').not.toBeNull();
		const femaleButton = female.nativeElement.querySelector('button') as HTMLButtonElement;

		femaleButton.click();
		const put = httpTesting.expectOne('driver/gender');
		expect(put.request.body, 'the gender code goes on the wire as a bare string').toBe('F');
		put.flush(null);
		flushRecalculation(1, 'a real gender change recalculates once');

		femaleButton.click();
		httpTesting.expectNone('driver/gender');
		flushRecalculation(0, 'DR-08: the redundant recalculation disappears with the redundant PUT');
		expect(component.gender(), 'the selection is unchanged by the re-click').toBe('F');

		httpTesting.verify();
	});

	// ⛔ DR-08 ordering. In AngularJS this was implicit and fragile: `btnRadio` and `ngClick` sit on
	// the same element at equal priority, so alphabetical ordering alone decided that the model was
	// written BEFORE the click handler read it. Reversing it would have PUT the previous gender.
	it('the gender value is set before the PUT is issued', () => {
		const female = fixture.debugElement.query(By.css('[data-testid="driver-gender-female"]'));
		const femaleButton = female.nativeElement.querySelector('button') as HTMLButtonElement;

		femaleButton.click();

		// Asserted BEFORE the response is flushed: the value must already be the new one at the
		// moment the request leaves, not once the server has answered.
		const put = httpTesting.expectOne('driver/gender');
		expect(
			component.gender(),
			'DR-08: the value is set first, then the PUT carries it',
		).toBe('F');
		expect(put.request.body, 'and the PUT carries the value that was just set').toBe('F');

		put.flush(null);
		flushRecalculation(1, 'the accepted change recalculates');
		httpTesting.verify();
	});
});
