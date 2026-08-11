import { CoverageDto, OptionDto } from '../../models/quote';

/**
 * The quote, pivoted: coverages become rows and tiers become columns.
 *
 * The response is shaped the other way — each option carries its own list — which is why the old
 * screen could only render three parallel lists. Reading one coverage across three tiers meant
 * scanning back and forth between columns and matching names by eye, and that comparison is the
 * single thing the screen exists to support. A row of the matrix answers it in one glance.
 *
 * ⛔ Still no sort. The backend returns coverages and discounts by ascending `position` and the
 * G1 contract test pins that; the row order below is the order they arrive in, merged. What the
 * merge adds is completeness, not sequence: a coverage the richest tier does not carry still has to
 * appear, or the reader cannot tell "not included" from "not shown".
 */

export interface MatrixCell {
	/** `null` where the tier does not include this coverage — rendered as an explicit absence. */
	readonly coverage: CoverageDto | null;
	/** The tier's pre-discount premium, the denominator of the cell's composition bar. */
	readonly total: number;
}

export interface CoverageRow {
	readonly code: string;
	readonly name: string;
	/**
	 * The rules may describe a coverage differently per tier, or not at all. The row label carries
	 * one popover, so it takes the first description any tier supplies.
	 */
	readonly description: string | null;
	/** One cell per option, in the same order as `options`. */
	readonly cells: readonly MatrixCell[];
}

export interface DiscountRow {
	readonly code: string;
	readonly name: string;
	/** `null` where the tier does not earn this discount. */
	readonly values: readonly (number | null)[];
}

export interface QuoteMatrix {
	readonly options: readonly OptionDto[];
	readonly coverageRows: readonly CoverageRow[];
	readonly discountRows: readonly DiscountRow[];
	/** The richest tier's coverage count — the denominator the tier gauge is read against. */
	readonly maxCoverages: number;
}

/**
 * Merges the per-option lists into one ordered set of row keys, by `position`.
 *
 * ⚠️ First-seen-wins does NOT work here, and the failure is subtle enough to be worth recording.
 * Walking the options in order and appending each unseen key looks like it preserves the response's
 * ordering, and it does for the first tier — but the tiers interleave. BRONZE has no collision
 * cover; SILVER's list runs …UMBI, COLL while GOLD's runs …UMBI, UMPD, COMP, COLL. Taking SILVER's
 * order first puts COLL ahead of UMPD, so GOLD's own cells then appear out of ITS order. Only a
 * total order over the union fixes that, and `position` is exactly that: it is the field the engine
 * orders by, and the G1 contract test pins that every tier returns it ascending.
 *
 * ⛔ This is not the client-side sort DR-05 forbids. That one re-sorted a list the backend had
 * already ordered, hiding whatever order the engine actually meant. This orders a UNION, which has
 * no array order of its own to preserve — and it orders it by the engine's own key, so each tier's
 * cells still come out in exactly the order that tier returned them. `quote-panel.spec.ts` asserts
 * precisely that, per option.
 */
function mergeKeys<T extends { position: number }>(
	lists: readonly (readonly T[])[],
	keyOf: (item: T) => string,
): string[] {
	const positions = new Map<string, number>();

	for (const list of lists) {
		for (const item of list) {
			const key = keyOf(item);
			// Tiers agree on a coverage's position in the recorded data; `min` makes the merge
			// deterministic rather than last-writer-wins if one ever disagrees.
			positions.set(key, Math.min(positions.get(key) ?? item.position, item.position));
		}
	}

	return [...positions.entries()].sort((a, b) => a[1] - b[1]).map(([key]) => key);
}

export function buildQuoteMatrix(options: readonly OptionDto[]): QuoteMatrix {
	const coverageKeys = mergeKeys(
		options.map((option) => option.coverages),
		(coverage) => coverage.code,
	);
	const discountKeys = mergeKeys(
		options.map((option) => option.discounts),
		(discount) => discount.code,
	);

	const coverageRows = coverageKeys.map((code) => {
		const found = options.map((option) => option.coverages.find((c) => c.code === code) ?? null);
		const anywhere = found.find((coverage) => coverage !== null) ?? null;

		return {
			code,
			// Every tier that carries the coverage names it identically in the recorded data, so the
			// first one found is the label. `anywhere` cannot be null: the key came from some list.
			name: anywhere?.name ?? code,
			description: found.find((coverage) => coverage?.description)?.description ?? null,
			cells: found.map((coverage, index) => ({
				coverage,
				total: options[index].premiumBeforeDiscounts,
			})),
		} satisfies CoverageRow;
	});

	const discountRows = discountKeys.map((code) => {
		const found = options.map((option) => option.discounts.find((d) => d.code === code) ?? null);

		return {
			code,
			name: found.find((discount) => discount !== null)?.name ?? code,
			values: found.map((discount) => discount?.value ?? null),
		} satisfies DiscountRow;
	});

	return {
		options,
		coverageRows,
		discountRows,
		maxCoverages: Math.max(0, ...options.map((option) => option.coverages.length)),
	};
}
