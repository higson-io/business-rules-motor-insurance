import {
	ChangeDetectionStrategy,
	Component,
	provideZonelessChangeDetection,
	signal,
} from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { HtmlPopover } from './html-popover';

/**
 * ⛔ THIS SUITE IS THE ONLY PROTECTION C-8 HAS.
 *
 * `e2e/specs/quote-panel.spec.ts` asserts that the description text appears on hover, but no
 * description in the current 171-file Higson snapshot contains any markup (six are plain prose,
 * three are null) — so the E2E flow cannot tell "rendered as HTML" from "escaped as text", and the
 * golden records that limitation honestly (`descriptionContainsMarkup: false`). Feeding real markup
 * here is what makes the MatTooltip failure mode — silently dropping or escaping the markup —
 * detectable at all.
 */
@Component({
	selector: 'app-popover-host',
	template: `<span data-testid="host" [appHtmlPopover]="description()"
		>Bodily Injury Liability</span
	>`,
	imports: [HtmlPopover],
	changeDetection: ChangeDetectionStrategy.OnPush,
})
class PopoverHost {
	readonly description = signal<string | null>(null);
}

describe('HtmlPopover', () => {
	function open(description: string | null): HTMLElement {
		TestBed.resetTestingModule();
		TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] });
		const fixture = TestBed.createComponent(PopoverHost);
		fixture.componentInstance.description.set(description);
		fixture.detectChanges();

		const host = (fixture.nativeElement as HTMLElement).querySelector('[data-testid="host"]');
		host?.dispatchEvent(new MouseEvent('mouseenter'));
		fixture.detectChanges();
		return document.body;
	}

	afterEach(() => TestBed.resetTestingModule());

	// G5 test 4 — C-8 / AC-F4.
	it('renders the description as HTML elements, not as escaped text', () => {
		const body = open('Covers <strong>bodily injury</strong> you cause.<br/>Excludes your own.');

		const panel = body.querySelector('[data-testid="html-popover-panel"]');
		expect(panel, 'hovering the host opens the popover panel').not.toBeNull();

		expect(
			panel?.querySelector('strong')?.textContent,
			'C-8: the markup becomes a real element — MatTooltip would drop it silently',
		).toBe('bodily injury');
		expect(
			panel?.querySelectorAll('br').length,
			'C-8: every tag survives, not just the first',
		).toBe(1);
		expect(
			panel?.textContent ?? '',
			'AC-F4: nothing is escaped — a literal `<strong>` in the text means the markup was escaped away',
		).not.toContain('<strong>');
		expect(panel?.textContent ?? '', 'the prose itself is preserved verbatim').toContain(
			'Covers bodily injury you cause.',
		);
	});

	// G5 test 5 — three of the nine recorded coverage descriptions are null.
	it('opens no popover at all when the description is null', () => {
		const body = open(null);

		expect(
			body.querySelector('[data-testid="html-popover-panel"]'),
			'a coverage with no description must not open an empty popover',
		).toBeNull();
	});
});
