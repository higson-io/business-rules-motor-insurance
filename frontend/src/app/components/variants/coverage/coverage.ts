import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';

/**
 * One cell of the comparison matrix: what a tier charges for one coverage, and on what limits.
 *
 * ⛔ The name, code and description moved OUT of here and onto the row label, where they belong once
 * the layout is a matrix — they are properties of the coverage, identical down the row, and
 * repeating them in every cell was what made three parallel lists so hard to compare. The limits
 * stayed: those genuinely differ per tier (bodily injury is 30,000/50,000 in BRONZE and
 * 50,000/100,000 in SILVER), which is half of what a tier upgrade buys.
 */
@Component({
	selector: 'app-coverage',
	imports: [DecimalPipe],
	templateUrl: './coverage.html',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Coverage {
	readonly limitLeft = input.required<number>();
	readonly limitRight = input.required<number | null>();
	readonly premium = input.required<number>();

	/**
	 * The tier's pre-discount premium — the sum this cell is one part of.
	 *
	 * Passed in rather than derived here because the denominator belongs to the column, not to the
	 * cell: a cell cannot know what it is a fraction of.
	 */
	readonly total = input.required<number>();

	/**
	 * This coverage's share of its tier's pre-discount premium, 0–1.
	 *
	 * The bar it drives is the one piece of interpretation on the screen: everything else states a
	 * number the engine returned, this shows how the numbers relate. Down a column it reads as where
	 * the money goes; across a row, as how a tier redistributes it.
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
