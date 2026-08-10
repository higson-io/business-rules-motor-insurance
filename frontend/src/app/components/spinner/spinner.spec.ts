import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { App } from '../../app';
import { QuoteStore } from '../../core/quote-store';
import { openMoto, QUOTE, settleShell, SHELL_PROVIDERS } from '../../testing/shell-harness';

/**
 * AC-F5 — the spinner is global and interceptor-driven.
 *
 * It is asserted through the assembled shell rather than through the component in isolation,
 * because the behaviour under test is the WIRING: `@if (pendingRequests.count() > 0)` in
 * `app.html`. `<app-spinner>` itself renders a `<mat-progress-spinner>` and knows nothing.
 *
 * ⛔ ND-6 is deliberately NOT asserted. The AngularJS build flashed this spinner at bootstrap
 * because eight `templateUrl` XHRs passed through the `$http` interceptor; compiled templates
 * remove those requests by construction. That disappearance is sanctioned (OD-13) — reproducing
 * it would need an artificial delay.
 */
describe('Global spinner', () => {
	let httpTesting: HttpTestingController;
	let fixture: ComponentFixture<App>;

	beforeEach(() => {
		TestBed.configureTestingModule({ providers: SHELL_PROVIDERS });
		httpTesting = TestBed.inject(HttpTestingController);
		fixture = TestBed.createComponent(App);
	});

	function spinner(): Element | null {
		return (fixture.nativeElement as HTMLElement).querySelector('[data-testid="app-spinner"]');
	}

	function ariaBusy(): string | null {
		return (fixture.nativeElement as HTMLElement).getAttribute('aria-busy');
	}

	it('is visible while any request is in flight and hidden at zero, on success and on error', () => {
		// ⚠️ Opening the demo is what mounts the quote screen and therefore what issues any request
		// at all. The landing screen makes none — that is asserted in `demo-picker.spec.ts`.
		openMoto();

		// Startup: the screen mounts and its GETs go out before anything can answer them.
		//
		// ⚠️ Nothing may call `httpTesting.match()` here. It REMOVES what it matches from the open
		// list, so merely counting the in-flight requests would leave them permanently unanswered
		// and pin the counter above zero — the spinner would then look correct for the wrong
		// reason and every later assertion would be meaningless.
		TestBed.tick();
		expect(spinner(), 'in-flight startup traffic shows the spinner').not.toBeNull();
		expect(ariaBusy(), 'the busy state is exposed to assistive technology').toBe('true');

		// …and nothing is left in flight once every one of them has been answered.
		const startup = settleShell(httpTesting);
		expect(startup.length, 'the screen really did issue startup requests').toBeGreaterThan(0);
		TestBed.tick();
		expect(spinner(), 'the spinner disappears when the counter returns to zero').toBeNull();
		expect(ariaBusy(), 'the screen is no longer announced as busy').toBe('false');

		// A successful reload raises it again and lowers it again.
		TestBed.inject(QuoteStore).recalculate();
		TestBed.tick();
		expect(spinner(), 'a reload puts the spinner back').not.toBeNull();
		httpTesting.expectOne('quote').flush(QUOTE);
		TestBed.tick();
		expect(spinner(), 'a successful response clears the spinner').toBeNull();

		// ⚠️ And so does a FAILING one. `finalize` in `pending-requests.interceptor.ts` is what
		// makes that true; a `tap`-based counter would leave the spinner stuck over the error
		// banner forever, which is the failure mode AC-F5 exists to catch.
		TestBed.inject(QuoteStore).recalculate();
		TestBed.tick();
		expect(spinner(), 'the failing reload also shows the spinner').not.toBeNull();
		httpTesting
			.expectOne('quote')
			.flush(
				{ message: 'Driver first name not supplied.' },
				{ status: 400, statusText: 'Bad Request' },
			);
		TestBed.tick();
		expect(spinner(), 'AC-F5: the spinner clears on error too, never sticking').toBeNull();
		expect(ariaBusy(), 'and the busy state is cleared with it').toBe('false');

		httpTesting.verify();
	});
});
