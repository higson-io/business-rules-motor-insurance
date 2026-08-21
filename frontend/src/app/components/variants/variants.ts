import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatCard } from '@angular/material/card';

import { HtmlPopover } from '../../shared/html-popover/html-popover';
import { MutationErrors } from '../../core/mutation-errors';
import { QuoteStore } from '../../core/quote-store';
import { bodyMessageOf } from '../../core/http-error-message';
import { buildQuoteMatrix } from './quote-matrix';
import { Coverage } from './coverage/coverage';

/**
 * Shown when a request fails without telling us why. Today's failing `GET quote` answers 404 with a
 * body that has no `message` key, which is exactly why the AngularJS banner renders EMPTY
 * (`e2e/goldens/pre/error-paths.json`: `bannerText: ""`). The C-6 backend fix (G10) will start
 * sending a real message; until then, and for any other message-less failure, the banner says
 * something rather than nothing.
 */
const GENERIC_QUOTE_ERROR = 'The quote could not be calculated.';

/**
 * The tier comparison matrix, and the only error branch in the application (`variants.js:35-38`).
 *
 * ⛔ Replaced three parallel per-tier lists. Those could show what a tier contained but never what
 * it lacked, and comparing one coverage across tiers meant matching names by eye between columns —
 * the one question the screen exists to answer.
 */
@Component({
	selector: 'app-variants',
	imports: [DecimalPipe, MatCard, Coverage, HtmlPopover],
	templateUrl: './variants.html',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Variants {
	private readonly store = inject(QuoteStore);
	private readonly mutationErrors = inject(MutationErrors);

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
	/** The quote pivoted into rows and columns. See `quote-matrix.ts` for why. */
	readonly matrix = computed(() => buildQuoteMatrix(this.options()));

	/**
	 * What a tier costs over the cheapest one, or null for the cheapest itself.
	 *
	 * ⛔ Arithmetic over the returned figures, NOT a recommendation. The distinction matters on this
	 * screen more than anywhere: `OptionDto` carries no recommendation flag, so a "best value" badge
	 * would be a judgement invented in TypeScript, on the one screen whose entire claim is that
	 * judgements live in configuration. A subtraction is checkable against the two numbers above it.
	 *
	 * ⚠️ The cheapest is found by price, not by position. The rules decide the order of the options
	 * and are free to return a cheaper tier second.
	 */
	deltaOf(premium: number): { readonly amount: number; readonly base: string } | null {
		const options = this.options();
		if (options.length < 2) {
			return null;
		}
		const cheapest = options.reduce((low, option) => (option.premium < low.premium ? option : low));
		if (premium <= cheapest.premium) {
			return null;
		}
		return { amount: premium - cheapest.premium, base: cheapest.code };
	}

	/**
	 * ⚠️ Derived from the response, never from the column's position. The obvious shortcut is
	 * `nth-child(1|2|3)` in CSS with the widths written by hand, which is a lie the moment the rules
	 * return a fourth option or return them in another order — and the rules are exactly the thing
	 * this application exists to let people change.
	 */
	breadthOf(coverageCount: number): number {
		const richest = this.matrix().maxCoverages;
		return richest === 0 ? 0 : coverageCount / richest;
	}

	/**
	 * One banner, two sources (C-7: the DR-06 fix adds no new UI surface, no per-field messages, no
	 * retry and no rollback). The failed mutation wins when both are live because it is the more
	 * recent thing the user did; the interceptor clears it as soon as the next mutation starts, so
	 * it never becomes a tombstone.
	 */
	readonly errorMessage = computed<string | null>(() => {
		const mutationFailure = this.mutationErrors.message();
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
