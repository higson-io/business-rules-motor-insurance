import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { Coverage } from './coverage';

/**
 * G5 test 1 — DR-10 / DR-11.
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
	}): HTMLElement {
		TestBed.resetTestingModule();
		TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] });
		const fixture = TestBed.createComponent(Coverage);
		fixture.componentRef.setInput('code', 'BI');
		fixture.componentRef.setInput('name', 'Bodily Injury Liability');
		fixture.componentRef.setInput('description', null);
		fixture.componentRef.setInput('limitLeft', inputs.limitLeft);
		fixture.componentRef.setInput('limitRight', inputs.limitRight);
		fixture.componentRef.setInput('premium', inputs.premium);
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
		expect(
			textOf(bothLimits, 'coverage-header'),
			'the header keeps the `name (code)` shape the goldens recorded',
		).toBe('Bodily Injury Liability (BI)');

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
});
