import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { Coverage } from './coverage';

/**
 * DR-10 / DR-11, and the composition bar.
 *
 * ⛔ The name, code and description assertions moved to `variants.spec.ts` with the elements
 * themselves: in the comparison matrix those are properties of the ROW, identical down it, and only
 * the limits and the premium vary per tier.
 *
 * DR-11 is a NO BRANCH row: `| number: 2` on the AngularJS side already renders en-US despite the
 * page being served with `lang="pl"`, so `100000` must stay `100,000.00` and must never become
 * `100 000,00`. That only holds while the application registers no locale (`app.config.ts`), which
 * is why this suite deliberately provides no `LOCALE_ID`.
 */
describe('Coverage', () => {
	function render(inputs: {
		limitLeft: number;
		limitRight: number | null;
		premium: number;
		/** Only the composition-bar tests care; everything else takes a plausible default. */
		total?: number;
	}): HTMLElement {
		TestBed.resetTestingModule();
		TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] });
		const fixture = TestBed.createComponent(Coverage);
		fixture.componentRef.setInput('limitLeft', inputs.limitLeft);
		fixture.componentRef.setInput('limitRight', inputs.limitRight);
		fixture.componentRef.setInput('premium', inputs.premium);
		// The denominator of the composition bar. Most of this suite is about number formatting, so
		// a plausible total is enough; the bar's own arithmetic is covered by the last two cases.
		fixture.componentRef.setInput('total', inputs.total ?? 1000);
		fixture.detectChanges();
		return fixture.nativeElement as HTMLElement;
	}

	function textOf(host: HTMLElement, testid: string): string {
		const element = host.querySelector(`[data-testid="${testid}"]`);
		return (element?.textContent ?? '').replace(/\s+/g, ' ').trim();
	}

	it('renders all money fields with two decimals, en-US separators and no currency symbol', () => {
		const bothLimits = render({ limitLeft: 100000, limitRight: 50000, premium: 100 });

		expect(
			textOf(bothLimits, 'coverage-limit'),
			'DR-11: 100000 renders as 100,000.00 — comma groups and a dot decimal, never 100 000,00',
		).toBe('100,000.00 / 50,000.00');
		expect(
			textOf(bothLimits, 'coverage-premium'),
			'DR-10: a whole-number premium is padded to two decimals',
		).toBe('100.00');
		for (const symbol of ['$', 'zł', 'PLN', '€']) {
			expect(
				bothLimits.textContent ?? '',
				`DR-10: no currency symbol is added anywhere — found '${symbol}'`,
			).not.toContain(symbol);
		}

		// `limitRight` is nullable on the wire (`CoverageDto.java:11` is a BigDecimal; six of the
		// nine recorded coverages send null), so the `@if` branch is part of the money rendering.
		const leftOnly = render({ limitLeft: 25000, limitRight: null, premium: 129.78 });

		expect(
			textOf(leftOnly, 'coverage-limit'),
			'a null `limitRight` renders the left limit alone, with no separator (coverage.html:14)',
		).toBe('25,000.00');
		expect(
			textOf(leftOnly, 'coverage-premium'),
			'DR-10: a fractional premium keeps exactly two decimals',
		).toBe('129.78');
	});

	it('sizes the composition bar to the share of the total and adds no text', () => {
		const host = render({ limitLeft: 25000, limitRight: null, premium: 250, total: 1000 });
		const bar = host.querySelector<HTMLElement>('.coverage__composition__fill');

		expect(bar?.style.width, 'a quarter of the premium fills a quarter of the bar').toBe('25%');

		// ⛔ The bar must contribute NOTHING to `innerText`. The Playwright goldens compare rendered
		// text, so this is what makes the design change provably not a behaviour change — and it is
		// the assertion that fails first if someone later "helpfully" prints the percentage.
		expect(
			bar?.textContent ?? '',
			'the bar renders no text, so the behaviour goldens stay byte-identical',
		).toBe('');
		expect(
			host.querySelector('.coverage__composition')?.getAttribute('aria-label'),
			'the share is named for assistive technology instead of being drawn as text',
		).toBe('25% of the tier premium');
	});

	it('draws an empty bar rather than dividing by zero', () => {
		// A zero-premium quote is a legitimate rules outcome, not an error state — the engine is
		// free to price everything at nothing — so the bar has to survive it.
		const host = render({ limitLeft: 0, limitRight: null, premium: 0, total: 0 });

		expect(
			host.querySelector<HTMLElement>('.coverage__composition__fill')?.style.width,
			'a zero total yields an empty bar, never NaN',
		).toBe('0%');
	});
});
