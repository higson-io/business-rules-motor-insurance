import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { DriverService } from './driver-service';

describe('DriverService body encoding', () => {
	let httpTesting: HttpTestingController;
	let service: DriverService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				provideZonelessChangeDetection(),
				provideHttpClient(),
				provideHttpClientTesting(),
			],
		});
		httpTesting = TestBed.inject(HttpTestingController);
		service = TestBed.inject(DriverService);
	});

	// ⚠️ The two traps of this group, pinned against each other in one test: the six string PUTs
	// must travel verbatim as text/plain, while the adjacent birthDate PUT must travel as a
	// serialised object with application/json. A string there yields HTTP 415 (AC-C2 + AC-C3).
	it('sends string PUTs verbatim and sends birthDate as a Date instance', () => {
		const stringEndpoints: readonly { readonly url: string; readonly send: () => void }[] = [
			{ url: 'driver/firstName', send: () => service.updateFirstName('Jan').subscribe() },
			{ url: 'driver/lastName', send: () => service.updateLastName('Kowalski').subscribe() },
			{ url: 'driver/gender', send: () => service.updateGender('F').subscribe() },
			{ url: 'driver/address/zipCode', send: () => service.updateZipCode('30-001').subscribe() },
			{ url: 'driver/address/city', send: () => service.updateCity('Krakow').subscribe() },
			{ url: 'driver/address/street', send: () => service.updateStreet('Dluga 1').subscribe() },
		];
		const expectedBodies = ['Jan', 'Kowalski', 'F', '30-001', 'Krakow', 'Dluga 1'];

		stringEndpoints.forEach((endpoint, index) => {
			endpoint.send();
			const request = httpTesting.expectOne(endpoint.url).request;
			const expected = expectedBodies[index];

			expect(typeof request.body, `${endpoint.url} must be given a raw string body`).toBe('string');
			expect(
				request.serializeBody(),
				`${endpoint.url} must go on the wire unquoted — a quoted body corrupts the stored value`,
			).toBe(expected);
			expect(
				request.detectContentTypeHeader(),
				`${endpoint.url} must be sent as text/plain, which Spring accepts for @RequestBody String`,
			).toBe('text/plain');
		});

		service.updateBirthDate(new Date(Date.UTC(1990, 4, 12))).subscribe();
		const birthDate = httpTesting.expectOne('driver/birthDate').request;

		expect(
			birthDate.body instanceof Date,
			'driver/birthDate takes a Date OBJECT — a string body yields HTTP 415',
		).toBe(true);
		expect(
			birthDate.serializeBody(),
			'the Date is serialised to a quoted ISO-8601 string, byte-identical to the AngularJS body',
		).toBe('"1990-05-12T00:00:00.000Z"');
		expect(
			birthDate.detectContentTypeHeader(),
			'driver/birthDate must be sent as application/json so Jackson can bind java.util.Date',
		).toBe('application/json');

		httpTesting.verify();
	});
});
