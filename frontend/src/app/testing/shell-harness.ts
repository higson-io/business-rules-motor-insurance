import { EnvironmentProviders, Provider, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DateAdapter, MAT_DATE_FORMATS } from '@angular/material/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { IsoDateAdapter, MOTOR_DATE_FORMATS } from '../core/iso-date.adapter';
import { mutationErrorInterceptor } from '../core/mutation-error.interceptor';
import { pendingRequestsInterceptor } from '../core/pending-requests.interceptor';
import { DictionaryEntryDto } from '../models/dictionary-entry';
import { DriverDto } from '../models/driver';
import { QuoteDto } from '../models/quote';
import { VehicleDto } from '../models/vehicle';

/**
 * Test-only helper, shared by the three G8 shell specs.
 *
 * ⚠️ It is not a `.spec.ts`, so `tsconfig.app.json` type-checks it with the application — it is
 * compile-clean and unreachable from `main.ts`, so nothing of it reaches the bundle. It lives here
 * rather than being copied into three spec files because all three mount the WHOLE screen and so
 * all three have to answer the same startup traffic.
 */

/**
 * The application's own providers, minus the ones a test cannot use.
 *
 * ⚠️ The interceptor chain is the same one `app.config.ts` installs, and that is load-bearing:
 * without `pendingRequestsInterceptor` the counter never moves and the spinner assertions would
 * pass against a spinner that is simply always absent.
 */
export const SHELL_PROVIDERS: (Provider | EnvironmentProviders)[] = [
	provideZonelessChangeDetection(),
	provideNoopAnimations(),
	provideHttpClient(withInterceptors([pendingRequestsInterceptor, mutationErrorInterceptor])),
	provideHttpClientTesting(),
	{ provide: DateAdapter, useClass: IsoDateAdapter },
	{ provide: MAT_DATE_FORMATS, useValue: MOTOR_DATE_FORMATS },
];

/**
 * The server-seeded session the goldens were captured against
 * (`e2e/goldens/pre/startup.json`).
 */
export const DRIVER: DriverDto = {
	firstName: 'John',
	lastName: 'Potter',
	dateOfBirth: '1986-08-06T00:00:00.000+00:00',
	gender: 'M',
	address: { zipCode: '77566', city: 'Lake Jackson', street: 'Allwood St' },
	numberOfAccidents: 0,
	numberOfTickets: 0,
	licenceObtainedAtAge: 18,
};

export const VEHICLE: VehicleDto = {
	productionYear: 2010,
	makeId: 217,
	make: 'TOYOTA',
	typeId: 28654,
	modelId: 218915,
};

export const QUOTE: QuoteDto = {
	options: [
		{
			code: 'BRONZE',
			order: 1,
			premium: 264.78,
			premiumBeforeDiscounts: 284.78,
			coverages: [],
			discounts: [],
		},
	],
};

const DICTIONARY: DictionaryEntryDto[] = [{ code: '1', name: 'Entry' }];

/**
 * Answers every request the mounted screen has in flight, repeatedly, until it goes quiet — the
 * vehicle cascade issues its dependent dictionary loads only once the vehicle itself has landed,
 * so one round of flushing is never enough.
 *
 * ⚠️ `TestBed.tick()`, never `ApplicationRef.whenStable()`: a settled screen still holds an open
 * `GET quote` after any mutation, and `whenStable()` waits for it forever.
 *
 * @returns the URL of every request answered, in the order they were answered.
 */
export function settleShell(httpTesting: HttpTestingController): string[] {
	const answered: string[] = [];

	for (let round = 0; round < 10; round++) {
		TestBed.tick();
		const open = httpTesting.match(() => true);
		if (open.length === 0) {
			return answered;
		}
		for (const request of open) {
			answered.push(request.request.url);
			request.flush(bodyFor(request.request.url));
		}
	}

	throw new Error(`the screen never went quiet; answered so far: ${answered.join(', ')}`);
}

function bodyFor(url: string): object {
	if (url === 'quote') {
		return QUOTE;
	}
	if (url === 'driver') {
		return DRIVER;
	}
	if (url === 'vehicle') {
		return VEHICLE;
	}
	if (url.startsWith('dictionaries/')) {
		return DICTIONARY;
	}
	throw new Error(`unexpected request to "${url}" — the 21-endpoint contract is frozen`);
}
