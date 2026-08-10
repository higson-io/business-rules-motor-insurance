import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { mutationErrorInterceptor } from './mutation-error.interceptor';
import { QuoteStore } from './quote-store';

describe('mutationErrorInterceptor', () => {
	// The store's resources only load once change detection runs, so this suite never calls
	// TestBed.tick(): the only traffic it sees is the PUT under test.
	it('writes a message into the store for a failed PUT and issues no retry', () => {
		TestBed.configureTestingModule({
			providers: [
				provideZonelessChangeDetection(),
				provideHttpClient(withInterceptors([mutationErrorInterceptor])),
				provideHttpClientTesting(),
			],
		});
		const httpTesting = TestBed.inject(HttpTestingController);
		const http = TestBed.inject(HttpClient);
		const store = TestBed.inject(QuoteStore);

		expect(store.mutationError(), 'no mutation has failed yet').toBeNull();

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

		expect(store.mutationError(), 'the failed PUT surfaces its message through the store').toBe(
			'Driver address city not supplied.',
		);
		expect(
			httpTesting.match('driver/address/city').length,
			'DR-06 is narrow: the failure is surfaced, never retried',
		).toBe(0);
		httpTesting.verify();
	});
});
