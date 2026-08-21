import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { App } from '../../app';
import { DEMOS } from '../demo-catalog';
import { settleShell, SHELL_PROVIDERS } from '../../testing/shell-harness';

/**
 * The landing screen and the switch between demos.
 *
 * Asserted through the assembled shell rather than the picker in isolation, because the behaviour
 * that matters is the wiring — which demo the `@switch` in `app.html` mounts, and, above all, that
 * nothing is mounted until one is chosen.
 */
describe('Demo picker', () => {
	let httpTesting: HttpTestingController;
	let fixture: ComponentFixture<App>;

	beforeEach(() => {
		TestBed.configureTestingModule({ providers: SHELL_PROVIDERS });
		httpTesting = TestBed.inject(HttpTestingController);
		fixture = TestBed.createComponent(App);
	});

	function el(selector: string): Element | null {
		return (fixture.nativeElement as HTMLElement).querySelector(selector);
	}

	function elements(selector: string): Element[] {
		return [...(fixture.nativeElement as HTMLElement).querySelectorAll(selector)];
	}

	function click(selector: string): void {
		(el(selector) as HTMLElement).click();
		TestBed.tick();
	}

	it('opens on the landing screen and issues no request at all', () => {
		TestBed.tick();

		expect(el('app-demo-picker'), 'the shell starts on the landing screen').not.toBeNull();
		expect(el('app-moto'), 'no demo is mounted before one is chosen').toBeNull();
		expect(el('app-life'), 'no demo is mounted before one is chosen').toBeNull();

		// ⛔ The point of mounting demos behind an `@if`. `QuoteStore` is `providedIn: 'root'` and
		// its resources load the moment it is first injected — which must not happen here. A
		// regression that mounts the motor components eagerly would show up as startup traffic on
		// a screen that has nothing to fetch.
		httpTesting.verify();

		expect(
			elements('.menubar button, .menubar a, .menubar input').length,
			'parity checklist §1: on the landing screen the menubar carries no interactive element',
		).toBe(0);
	});

	it('renders one tile per catalogue entry', () => {
		TestBed.tick();

		expect(
			elements('[data-testid^="demo-tile-"]').map((e) => e.getAttribute('data-testid')),
			'every catalogue entry gets a tile, in catalogue order',
		).toEqual(DEMOS.map((demo) => `demo-tile-${demo.code}`));

		// ⛔ The order is the argument, not a detail. Each demo hands the rules engine something
		// bigger than the last, so a catalogue reshuffle turns a build-up into three unrelated
		// screens — which still works and stops meaning anything.
		expect(
			elements('.demo-picker__step').map((e) => e.textContent?.trim()),
			'the tiles carry their position in the story',
		).toEqual(['01', '02', '03']);

		// Every tile carries a glyph. A missing `@switch` case renders an empty hole, and the
		// landing screen is the first thing an audience sees.
		expect(
			elements('.demo-picker__icon svg').length,
			'every demo has an icon; a missing case reads as an unfinished product',
		).toBe(DEMOS.length);
	});

	it('opens the chosen demo and comes back to the picker', () => {
		TestBed.tick();

		click('[data-testid="demo-tile-moto"]');
		settleShell(httpTesting);
		expect(el('app-moto'), 'choosing the motor tile mounts the motor demo').not.toBeNull();
		expect(el('app-demo-picker'), 'the picker gives way to the demo').toBeNull();

		click('[data-testid="back-to-demos"]');
		expect(el('app-demo-picker'), 'the back control returns to the picker').not.toBeNull();
		expect(el('app-moto'), 'leaving a demo unmounts it').toBeNull();

		click('[data-testid="demo-tile-life"]');
		expect(el('app-life'), 'the life tile mounts its own screen, not the motor one').not.toBeNull();
		expect(el('app-moto'), 'the two demos are never mounted together').toBeNull();

		// The life screen loads its own quote and nothing else. In particular it must not re-issue
		// the motor demo's requests: the two stores are separate, and leaving a demo has to leave
		// its traffic behind it.
		expect(settleShell(httpTesting), 'the life screen fetches its quote, and only that').toEqual([
			'life',
		]);
		httpTesting.verify();
	});
});
