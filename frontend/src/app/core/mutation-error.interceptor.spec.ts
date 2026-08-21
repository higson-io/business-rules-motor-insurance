import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { mutationErrorInterceptor } from './mutation-error.interceptor';
import { MutationErrors } from './mutation-errors';

describe('mutationErrorInterceptor', () => {
	// ⛔ `httpTesting.verify()` at the end is what pins the second half of this: injecting
	// `MutationErrors` must not start ANY other traffic. Back when the interceptor injected
	// `QuoteStore`, a failed PUT in the life demo also instantiated the motor store and fetched a
	// motor quote — this test would have caught it, and did not exist to.
	it('writes a message for a failed PUT, issues no retry, and fetches nothing', () => {
		TestBed.configureTestingModule({
			providers: [
				provideZonelessChangeDetection(),
				provideHttpClient(withInterceptors([mutationErrorInterceptor])),
				provideHttpClientTesting(),
			],
		});
		const httpTesting = TestBed.inject(HttpTestingController);
		const http = TestBed.inject(HttpClient);
		const errors = TestBed.inject(MutationErrors);

		expect(errors.message(), 'no mutation has failed yet').toBeNull();

		http.put('driver/address/city', 'Krakow').subscribe({
			next: () => undefined,
			error: () => undefined,
		});
		httpTesting.expectOne('driver/address/city').flush(
			{ message: 'Driver address city not supplied.' },
			{
				status: 500,
				statusText: 'Internal Server Error',
			},
		);

		expect(errors.message(), 'the failed PUT surfaces its message').toBe(
			'Driver address city not supplied.',
		);
		expect(
			httpTesting.match('driver/address/city').length,
			'DR-06 is narrow: the failure is surfaced, never retried',
		).toBe(0);
		httpTesting.verify();
	});
});
