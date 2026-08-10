import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';

/**
 * One discount line inside a variant card. Purely presentational (`discount.js:12-16`).
 *
 * DR-18: the AngularJS directive also declared a `price-before-discount` input, bound at
 * `variants.html:24` to a `DiscountDto` field that does not exist — so the binding has always been
 * `undefined` and no template ever read it. The input is dropped, not carried.
 */
@Component({
	selector: 'app-discount',
	imports: [DecimalPipe],
	templateUrl: './discount.html',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Discount {
	readonly name = input.required<string>();
	readonly value = input.required<number>();
}
