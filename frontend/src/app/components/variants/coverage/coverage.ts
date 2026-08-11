import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';

import { HtmlPopover } from '../../../shared/html-popover/html-popover';

/** One coverage row inside a variant card. Purely presentational (`coverage.js:12-19`). */
@Component({
	selector: 'app-coverage',
	imports: [DecimalPipe, HtmlPopover],
	templateUrl: './coverage.html',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Coverage {
	readonly code = input.required<string>();
	readonly name = input.required<string>();
	/** HTML from the Higson rules, shown in the popover. Null for coverages that have none. */
	readonly description = input.required<string | null>();
	readonly limitLeft = input.required<number>();
	readonly limitRight = input.required<number | null>();
	readonly premium = input.required<number>();

	/**
	 * The variant's pre-discount premium — the sum this row is one part of.
	 *
	 * Used only to size the composition bar. It is passed in rather than derived here because the
	 * denominator belongs to the card, not to the row: a row cannot know what it is a fraction of.
	 */
	readonly total = input.required<number>();

	/**
	 * This coverage's share of the pre-discount premium, 0–1.
	 *
	 * The bar it drives is the one piece of interpretation on the screen: everything else states a
	 * number the engine returned, this shows how the numbers relate. It is why the row is worth
	 * looking at — change the driver's age and the bars redistribute, which is the whole argument
	 * for pricing in rules rather than in code, made visible instead of asserted.
	 *
	 * Guarded rather than trusted: `premiumBeforeDiscounts` is a `BigDecimal` on the wire and a
	 * zero-premium quote is a legitimate rules outcome, not an error.
	 */
	readonly share = computed(() => {
		const total = this.total();
		if (!Number.isFinite(total) || total <= 0) {
			return 0;
		}
		return Math.min(1, this.premium() / total);
	});
}
