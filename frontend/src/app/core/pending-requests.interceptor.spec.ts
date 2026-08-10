import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { PendingRequests } from './pending-requests';
import { pendingRequestsInterceptor } from './pending-requests.interceptor';

describe('pendingRequestsInterceptor', () => {
	let httpTesting: HttpTestingController;
	let http: HttpClient;
	let pending: PendingRequests;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				provideZonelessChangeDetection(),
				provideHttpClient(withInterceptors([pendingRequestsInterceptor])),
				provideHttpClientTesting(),
			],
		});
		httpTesting = TestBed.inject(HttpTestingController);
		http = TestBed.inject(HttpClient);
		pending = TestBed.inject(PendingRequests);
	});

	it('returns the counter to zero on success and on error', () => {
		expect(pending.count(), 'nothing is in flight before the first request').toBe(0);

		http.get('quote').subscribe({ next: () => undefined, error: () => undefined });
		expect(pending.count(), 'an in-flight request raises the counter').toBe(1);
		httpTesting.expectOne('quote').flush({ options: [] });
		expect(pending.count(), 'a successful response returns the counter to zero').toBe(0);

		http.get('quote').subscribe({ next: () => undefined, error: () => undefined });
		expect(pending.count(), 'a second in-flight request raises the counter again').toBe(1);
		httpTesting
			.expectOne('quote')
			.flush('boom', { status: 500, statusText: 'Internal Server Error' });
		expect(pending.count(), 'a failed response also returns the counter to zero').toBe(0);

		httpTesting.verify();
	});
});
