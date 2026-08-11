import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { App } from './app';
import { appConfig } from './app.config';
import { openMoto, settleShell, SHELL_PROVIDERS } from './testing/shell-harness';

describe('App shell', () => {
	let httpTesting: HttpTestingController;
	let fixture: ComponentFixture<App>;

	beforeEach(() => {
		TestBed.configureTestingModule({ providers: SHELL_PROVIDERS });
		httpTesting = TestBed.inject(HttpTestingController);
		fixture = TestBed.createComponent(App);
	});

	function elements(selector: string): Element[] {
		return [...(fixture.nativeElement as HTMLElement).querySelectorAll(selector)];
	}

	function el(selector: string): Element | null {
		return (fixture.nativeElement as HTMLElement).querySelector(selector);
	}

	// G8 test 1 — `main.js:21-23` is DELETED, not ported.
	it('renders layout only and contains no event-relay logic', () => {
		// ⚠️ The quote screen does not exist until a demo is opened; without this the assertions
		// below would be run against the landing screen.
		openMoto();
		const startup = settleShell(httpTesting);

		expect(elements('app-inputs').length, 'the shell composes exactly one sidebar').toBe(1);
		expect(elements('app-variants').length, 'the shell composes exactly one variants panel').toBe(
			1,
		);
		expect(
			el('.menubar [data-testid="app-wordmark"]')?.textContent?.trim(),
			'the menubar carries the product wordmark (parity checklist §1, now industry-neutral)',
		).toBe('Higson Demos');

		// ⛔ The shell is the PRODUCT, not a line of business. It carried an "Insurance App"
		// wordmark while this was one insurance calculator; that became wrong the moment it started
		// hosting several industries, and would be wrong again for whichever one comes next.
		//
		// Asserted from inside the motor demo deliberately — that is where the pull to brand the
		// chrome is strongest, and where a stray "Insurance" in the menubar would look plausible.
		// What the DEMO says about itself is its own business; this is only about the shell.
		expect(
			el('.menubar')?.textContent ?? '',
			'the menubar names no industry — the demo identifies itself, the shell does not',
		).not.toMatch(/insurance|motor|vehicle|life|health|travel/i);

		// ⚠️ The parity checklist says the menubar has "no nav, no buttons, no interactivity", and
		// that still holds on the landing screen — see `demo-picker.spec.ts`. Inside a demo it now
		// carries exactly one control, the way back out, because without a router the browser
		// offers none. That is a deliberate addition and is pinned here so a second one cannot
		// arrive unnoticed.
		expect(
			elements('.menubar button, .menubar a, .menubar input').map((el) =>
				el.getAttribute('data-testid'),
			),
			'inside a demo the menubar carries exactly one control: the way back to the picker',
		).toEqual(['back-to-demos']);

		// The AngularJS shell existed ONLY to relay `recalculate` → `reloadQuote` between siblings
		// (`main.js:21-23`). `QuoteStore` replaced that bus in G4, so no relay survives. The one
		// method the shell now declares closes the demo — it touches `DemoSelection` and nothing
		// else, and in particular nothing quote-shaped.
		expect(
			Object.getOwnPropertyNames(App.prototype),
			'the shell declares no methods beyond closing a demo — a ported relay handler would show up here',
		).toEqual(['constructor', 'backToDemos']);
		expect(
			// `__ngContext__` is stamped onto every component instance by the framework itself.
			Object.getOwnPropertyNames(fixture.componentInstance).filter(
				(name) => !name.startsWith('__'),
			),
			'the shell holds no state of its own — the counter, the selection service and the signal it exposes',
		).toEqual(['pendingRequests', 'selection', 'demo']);

		// DR-09, asserted from the ASSEMBLED screen rather than from one component in isolation:
		// the shell mounts driver, driver-history and variants together and the shared `QuoteStore`
		// resource still yields one fetch. The AngularJS build issued two (`driver.js:24-28` and
		// `driverHistory.js:30-36`), which is what `e2e/goldens/pre/startup.json` records.
		expect(
			startup.filter((url) => url === 'driver').length,
			'DR-09: the assembled screen issues exactly one GET driver, not one per consumer',
		).toBe(1);
	});

	// G8 test 4 — ND-7. There are several screens now, but still no router: the switch is a signal.
	it('declares no router provider and renders no router-outlet', () => {
		openMoto();
		settleShell(httpTesting);

		expect(
			elements('router-outlet').length,
			'ND-7: no router-outlet — `ApplicationConfig.addViewController("/")` stays sufficient',
		).toBe(0);
		expect(
			document.querySelectorAll('router-outlet').length,
			'ND-7: no router-outlet is rendered into an overlay container either',
		).toBe(0);

		// The provider list is PINNED rather than searched: `provideRouter()` returns opaque
		// `EnvironmentProviders` that carry nothing inspectable at runtime, so the only honest
		// runtime signal is that nothing new was added. The six are zoneless change detection,
		// the global error listeners, HttpClient + its two interceptors, async animations, the
		// `DateAdapter` and `MAT_DATE_FORMATS`.
		expect(
			appConfig.providers.length,
			'ND-7: `appConfig` carries exactly the six known providers — a seventh (provideRouter, a locale, …) is a deliberate scope change and must be argued for here first',
		).toBe(6);
	});
});
