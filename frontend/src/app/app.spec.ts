import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { App } from './app';
import { appConfig } from './app.config';
import { settleShell, SHELL_PROVIDERS } from './testing/shell-harness';

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

	// G8 test 1 — `main.js:21-23` is DELETED, not ported.
	it('renders layout only and contains no event-relay logic', () => {
		const startup = settleShell(httpTesting);

		expect(elements('app-sidebar').length, 'the shell composes exactly one sidebar').toBe(1);
		expect(elements('app-variants').length, 'the shell composes exactly one variants panel').toBe(
			1,
		);
		expect(
			elements('.menubar .logo').length,
			'the menubar carries the logo (parity checklist §1: no nav, no buttons, no interactivity)',
		).toBe(1);
		expect(
			elements('.menubar button, .menubar a, .menubar input').length,
			'the menubar has no interactive element — it never had one',
		).toBe(0);

		// The AngularJS shell existed ONLY to relay `recalculate` → `reloadQuote` between siblings
		// (`main.js:21-23`). `QuoteStore` replaced that bus in G4, so the shell has no behaviour
		// left at all: no methods, and no state beyond the counter the spinner reads.
		expect(
			Object.getOwnPropertyNames(App.prototype),
			'the shell declares no methods — a ported relay handler would show up here',
		).toEqual(['constructor']);
		expect(
			// `__ngContext__` is stamped onto every component instance by the framework itself.
			Object.getOwnPropertyNames(fixture.componentInstance).filter(
				(name) => !name.startsWith('__'),
			),
			'the shell holds no state of its own — only the injected pending-request counter',
		).toEqual(['pendingRequests']);

		// DR-09, asserted from the ASSEMBLED screen rather than from one component in isolation:
		// the shell mounts driver, driver-history and variants together and the shared `QuoteStore`
		// resource still yields one fetch. The AngularJS build issued two (`driver.js:24-28` and
		// `driverHistory.js:30-36`), which is what `e2e/goldens/pre/startup.json` records.
		expect(
			startup.filter((url) => url === 'driver').length,
			'DR-09: the assembled screen issues exactly one GET driver, not one per consumer',
		).toBe(1);
	});

	// G8 test 4 — ND-7. The screen is single and non-routed, by design.
	it('declares no router provider and renders no router-outlet', () => {
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
