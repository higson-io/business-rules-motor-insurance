import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { Inputs } from './inputs';
import { settleShell, SHELL_PROVIDERS } from '../../testing/shell-harness';

describe('Inputs', () => {
	let httpTesting: HttpTestingController;
	let fixture: ComponentFixture<Inputs>;

	beforeEach(() => {
		TestBed.configureTestingModule({ providers: SHELL_PROVIDERS });
		httpTesting = TestBed.inject(HttpTestingController);
		fixture = TestBed.createComponent(Inputs);
		settleShell(httpTesting);
	});

	function textsOf(selector: string): string[] {
		return [...(fixture.nativeElement as HTMLElement).querySelectorAll(selector)].map((element) =>
			(element.textContent ?? '').replace(/\s+/g, ' ').trim(),
		);
	}

	function tagsOf(selector: string): string[] {
		return [...(fixture.nativeElement as HTMLElement).querySelectorAll(selector)].map((element) =>
			element.tagName.toLowerCase(),
		);
	}

	// G8 test 3 — ND-2.
	it('renders the four group labels, including the literal "Documents"', () => {
		expect(textsOf('h1'), 'the panel keeps its heading copy verbatim (`sidebar.html:3`)').toEqual([
			'Get a Quote',
		]);

		// ⛔ ND-2 — the third section is labelled "Documents" while it contains CLAIM HISTORY
		// (accidents, tickets, licence age). That is wrong copy, and it is a RECORDED
		// NON-DEVIATION: rewording it is a product decision, out of scope for a migration
		// (`spec.md` §7.3). Fixing it here would show up as an unexplained golden diff in G11.
		// ⚠️ FOUR groups, not three. "Address" is now a heading of its own: the band lays the inputs
		// out side by side, so what used to be a divider inside the driver panel had to become a
		// group like the others. ND-3 still holds — the element is still EMITTED from inside
		// `<app-driver>`, which is what the record is about; the host is `display: contents` so it
		// lands as a sibling here rather than being moved.
		expect(
			textsOf('h2'),
			'ND-2: the four group labels, in order and verbatim — the last says "Documents"',
		).toEqual(['Personal information', 'Address', 'Vehicle', 'Documents']);

		// Each widget still follows its label, in the original order.
		expect(
			tagsOf('app-driver, app-vehicle, app-driver-history'),
			'each section is followed by its widget, in the original order',
		).toEqual(['app-driver', 'app-vehicle', 'app-driver-history']);

		// ⛔ DR-16 — `ctrl.sidebarHidden` / `ctrl.toggleSidebar()` (`sidebar.js:21-27`) are dead
		// code: declared, never referenced in `sidebar.html`, leftovers of a removed collapse
		// feature. Nothing here can hide the panel, and there is no control that would try.
		expect(
			Object.getOwnPropertyNames(Inputs.prototype),
			'DR-16: the sidebar declares no methods — a ported `toggleSidebar` would show up here',
		).toEqual(['constructor']);
		expect(
			[
				...(fixture.nativeElement as HTMLElement).querySelectorAll(
					'.sidebar-container__header button, .sidebar-container__divider button',
				),
			].length,
			'DR-16: the panel chrome owns no collapse control — every button on screen belongs to a widget',
		).toBe(0);
	});
});
