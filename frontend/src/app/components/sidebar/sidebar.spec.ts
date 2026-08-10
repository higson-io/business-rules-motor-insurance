import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { Sidebar } from './sidebar';
import { settleShell, SHELL_PROVIDERS } from '../../testing/shell-harness';

describe('Sidebar', () => {
	let httpTesting: HttpTestingController;
	let fixture: ComponentFixture<Sidebar>;

	beforeEach(() => {
		TestBed.configureTestingModule({ providers: SHELL_PROVIDERS });
		httpTesting = TestBed.inject(HttpTestingController);
		fixture = TestBed.createComponent(Sidebar);
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
	it('renders the three section labels, including the literal "Documents"', () => {
		expect(textsOf('h1'), 'the panel keeps its heading copy verbatim (`sidebar.html:3`)').toEqual([
			'Get a Quote',
		]);

		// ⛔ ND-2 — the third section is labelled "Documents" while it contains CLAIM HISTORY
		// (accidents, tickets, licence age). That is wrong copy, and it is a RECORDED
		// NON-DEVIATION: rewording it is a product decision, out of scope for a migration
		// (`spec.md` §7.3). Fixing it here would show up as an unexplained golden diff in G11.
		expect(
			textsOf('h2'),
			'ND-2: the three sidebar section labels, in order and verbatim — the third says "Documents"',
		).toEqual(['Personal information', 'Vehicle', 'Documents']);

		// Each label introduces its own widget, in the order `sidebar.html:8-42` had them.
		expect(
			tagsOf('app-driver, app-vehicle, app-driver-history'),
			'each section is followed by its widget, in the original order',
		).toEqual(['app-driver', 'app-vehicle', 'app-driver-history']);

		// ⛔ DR-16 — `ctrl.sidebarHidden` / `ctrl.toggleSidebar()` (`sidebar.js:21-27`) are dead
		// code: declared, never referenced in `sidebar.html`, leftovers of a removed collapse
		// feature. Nothing here can hide the panel, and there is no control that would try.
		expect(
			Object.getOwnPropertyNames(Sidebar.prototype),
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
