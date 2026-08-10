import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { DateAdapter, MAT_DATE_FORMATS } from '@angular/material/core';
import { MatCalendar } from '@angular/material/datepicker';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { IsoDateAdapter, MOTOR_DATE_FORMATS } from './iso-date.adapter';

/**
 * The birth-date picker is the single highest-risk item in the migration (G-C7/G-F2), and this
 * adapter is where every one of its risks lands: the wire format, the alternative typed format,
 * the Monday-first week, and the timezone.
 *
 * ⚠️ These tests assume `TZ=UTC` (P-4). `dateOfBirth` arrives as an ISO-8601 string carrying the
 * SERVER's offset while `MatDatepicker` works in BROWSER-LOCAL time, so the displayed day can move
 * by one across machines. Pinning the timezone for capture and replay is what makes that
 * deterministic; it is not something the adapter can fix.
 */
describe('ISO date adapter', () => {
	let adapter: IsoDateAdapter;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				provideZonelessChangeDetection(),
				provideNoopAnimations(),
				IsoDateAdapter,
				{ provide: DateAdapter, useExisting: IsoDateAdapter },
				{ provide: MAT_DATE_FORMATS, useValue: MOTOR_DATE_FORMATS },
			],
		});
		adapter = TestBed.inject(IsoDateAdapter);
	});

	// `uib-datepicker-popup="yyyy-MM-dd"` plus `alt-input-formats="['yyyy/MM/dd']"`
	// (`driver.html:25,35`). Both formats are user-facing: the first is what the field renders and
	// what the E2E golden asserts back (`toHaveValue('1990-05-12')`), the second is only ever typed.
	it('formats and parses yyyy-MM-dd and alt-parses yyyy/MM/dd', () => {
		const display = MOTOR_DATE_FORMATS.display.dateInput;

		expect(
			adapter.format(new Date(1990, 4, 12), display),
			'the field renders the wire-shaped yyyy-MM-dd, never a locale date',
		).toBe('1990-05-12');
		expect(
			adapter.format(new Date(2001, 0, 2), display),
			'single-digit months and days are zero padded',
		).toBe('2001-01-02');

		const typed = adapter.parse('1990-05-12');
		expect(typed, 'a yyyy-MM-dd string parses').not.toBeNull();
		expect(adapter.format(typed!, display), 'yyyy-MM-dd round-trips unchanged').toBe('1990-05-12');

		const alt = adapter.parse('1990/05/12');
		expect(alt, 'the alternative yyyy/MM/dd input format still parses').not.toBeNull();
		expect(
			adapter.format(alt!, display),
			'the alternative format normalises back to yyyy-MM-dd',
		).toBe('1990-05-12');

		const loose = adapter.parse('1990-5-1');
		expect(loose, 'unpadded components parse, as they did in the AngularJS build').not.toBeNull();
		expect(adapter.format(loose!, display), 'and normalise on the way out').toBe('1990-05-01');

		const garbage = adapter.parse('not a date');
		expect(
			garbage !== null && adapter.isValid(garbage),
			'an unparseable string never yields a valid date',
		).toBe(false);

		// ⚠️ The DriverDto wire shape: an ISO-8601 instant with the SERVER's offset, not epoch
		// millis. Under TZ=UTC (P-4) it must land on the same calendar day it names.
		const fromWire = adapter.deserialize('1986-08-06T00:00:00.000+00:00');
		expect(fromWire, 'the seeded dateOfBirth deserialises').not.toBeNull();
		expect(
			adapter.format(fromWire!, display),
			'the server ISO-8601 instant renders as the day it names',
		).toBe('1986-08-06');
	});

	// `$scope.dateOptions = { formatYear: 'yyyy', startingDay: 1, showWeeks: false }`
	// (`driver.js:35-39`). The calendar is rendered rather than the adapter merely queried, because
	// a correct `getFirstDayOfWeek()` that the calendar never consults would be worthless.
	it('the week starts on Monday and week numbers are hidden', () => {
		expect(adapter.getFirstDayOfWeek(), 'startingDay: 1 — Monday, not Sunday').toBe(1);

		const fixture = TestBed.createComponent(MatCalendar<Date>);
		fixture.componentInstance.startAt = new Date(1990, 4, 12);
		TestBed.tick();

		const calendar = fixture.nativeElement as HTMLElement;
		const headers = Array.from(
			calendar.querySelectorAll<HTMLElement>('.mat-calendar-table-header th'),
		)
			.map((cell) => (cell.textContent ?? '').trim())
			.filter((text) => text.length > 0);

		// Each header cell carries a visually-hidden long name for screen readers plus the narrow
		// label, so the cell text is asserted by containment rather than equality.
		const longNames = adapter.getDayOfWeekNames('long');
		expect(
			headers.length,
			'exactly seven columns — showWeeks: false, so no week-number column',
		).toBe(7);
		expect(headers[0], 'the first column of the calendar is Monday').toContain(longNames[1]);
		expect(headers[6], 'the last column is Sunday').toContain(longNames[0]);
	});
});
