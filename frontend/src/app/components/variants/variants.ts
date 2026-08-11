import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatCard } from '@angular/material/card';

import { QuoteStore } from '../../core/quote-store';
import { bodyMessageOf } from '../../core/http-error-message';
import { Coverage } from './coverage/coverage';
import { Discount } from './discount/discount';

/**
 * Shown when a request fails without telling us why. Today's failing `GET quote` answers 404 with a
 * body that has no `message` key, which is exactly why the AngularJS banner renders EMPTY
 * (`e2e/goldens/pre/error-paths.json`: `bannerText: ""`). The C-6 backend fix (G10) will start
 * sending a real message; until then, and for any other message-less failure, the banner says
 * something rather than nothing.
 */
const GENERIC_QUOTE_ERROR = 'The quote could not be calculated.';

/** The three variant cards, and the only error branch in the application (`variants.js:35-38`). */
@Component({
	selector: 'app-variants',
	imports: [DecimalPipe, MatCard, Coverage, Discount],
	templateUrl: './variants.html',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Variants {
	private readonly store = inject(QuoteStore);

	/**
	 * ⛔ DR-07 — the stale cards must vanish when the quote fails.
	 *
	 * `variants.js:35-38` set `errorOccured` but never cleared `ctrl.variants`, so the previous
	 * vehicle's cards stayed on screen underneath the red banner. Reading straight through the
	 * resource fixes that by construction: nothing here caches the last good quote.
	 *
	 * ⚠️ `hasValue()`, NOT `quote.value()?.options ?? []`. In Angular 22 a resource in the error
	 * state THROWS `ResourceValueError` from `value()` — with or without a `defaultValue` — so the
	 * optional-chaining form would blow the template up on the very failure DR-07 is about.
	 */
	readonly options = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value().options : [],
	);

	/**
	 * How much of the richest tier's cover a given tier includes, 0–1.
	 *
	 * Drives the rule under each tier name, so the three cards read as a ladder before any figure
	 * has been compared. BRONZE carries three coverages against GOLD's eight; that difference is the
	 * substance of the choice, and until now the only way to see it was to count rows.
	 *
	 * ⚠️ Derived from the response rather than from the card's position. The obvious shortcut is
	 * `nth-child(1|2|3)` in CSS with the widths written by hand, which is a lie the moment the rules
	 * return a fourth option or return them in another order — and the rules are exactly the thing
	 * this application exists to let people change.
	 */
	/** The richest tier's coverage count — the denominator the gauge is read against. */
	readonly maxCoverages = computed(() =>
		Math.max(0, ...this.options().map((option) => option.coverages.length)),
	);

	readonly coverBreadth = computed(() => {
		const counts = this.options().map((option) => option.coverages.length);
		const richest = Math.max(0, ...counts);
		if (richest === 0) {
			return new Map<string, number>();
		}
		return new Map(
			this.options().map((option) => [option.code, option.coverages.length / richest]),
		);
	});

	/**
	 * One banner, two sources (C-7: the DR-06 fix adds no new UI surface, no per-field messages, no
	 * retry and no rollback). The failed mutation wins when both are live because it is the more
	 * recent thing the user did; the interceptor clears it as soon as the next mutation starts, so
	 * it never becomes a tombstone.
	 */
	readonly errorMessage = computed<string | null>(() => {
		const mutationFailure = this.store.mutationError();
		if (mutationFailure !== null) {
			return mutationFailure;
		}

		const quoteFailure = this.store.quote.error();
		if (quoteFailure === undefined) {
			return null;
		}
		return bodyMessageOf(quoteFailure) ?? GENERIC_QUOTE_ERROR;
	});
}
