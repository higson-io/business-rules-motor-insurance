import { expect, test } from '@playwright/test';
import { openApp, readMatrix, renderedText, TESTID, textsOf } from '../support/app';
import { Golden } from '../support/golden';
import { byStage } from '../support/stage';

/**
 * FLOW 5 - rendering order (DR-05), money formatting (DR-10 / DR-11) and the HTML coverage
 * description popover (C-8).
 *
 * Order is asserted against the `GET quote` response itself rather than a hard-coded list, so
 * the spec proves the real property - "rendered in array order, with no client-side sort" -
 * rather than merely that today's data happens to come out in a particular sequence.
 */

const golden = new Golden('quote-panel');

/** AngularJS interpolates a raw number through `String()`; no separators, no padding. */
const raw = (n: number): string => String(n);

/** `| number: 2` (AngularJS) and `DecimalPipe '1.2-2'` (Angular) both format as en-US. */
const money = (n: number): string =>
	n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

interface CoverageDto {
	position: number;
	code: string;
	name: string;
	limitLeft: number;
	limitRight: number | null;
	premium: number;
	description: string | null;
}
interface OptionDto {
	code: string;
	order: number;
	premium: number;
	premiumBeforeDiscounts: number;
	coverages: CoverageDto[];
	discounts: { code: string; name: string; value: number; position: number }[];
}

test.afterAll(() => golden.write());

test('the quote panel renders in backend order with the recorded formatting', async ({ page }) => {
	const quoteResponse = page.waitForResponse(
		(r) => r.request().method() === 'GET' && new URL(r.url()).pathname.endsWith('/quote')
	);
	await openApp(page);
	const quote = (await (await quoteResponse).json()) as { options: OptionDto[] };

	const matrix = await readMatrix(page);

	await test.step('options, coverages and discounts render in array order (DR-05)', async () => {
		// ⛔ NO BRANCH. The backend already sorts - options by `order`, coverages and discounts
		// by `position` - so DR-05 removes the dead `| orderBy: position` and introduces NO
		// client-side sort. Rendering must therefore follow the response array on both frontends.
		expect(
			matrix.tiers,
			'DR-05: options render in response array order, with no client-side sort'
		).toEqual(quote.options.map((o) => o.code));

		for (const [index, option] of quote.options.entries()) {
			const positions = option.coverages.map((c) => c.position);
			expect(
				positions,
				`${option.code}: the backend already returns coverage positions ascending - the DR-05 evidence`
			).toEqual([...positions].sort((a, b) => a - b));

			// A column read: the rows this tier actually carries, in the order the matrix lists
			// them. The merge preserves each option's own ordering, so this is that option's array.
			expect(
				matrix.rows.filter((r) => r.cells[index] !== null).map((r) => r.label),
				`${option.code}: its coverages appear in response array order`
			).toEqual(option.coverages.map((c) => `${c.name} (${c.code})`));

			expect(
				matrix.discounts.filter((d) => d.values[index] !== null).map((d) => d.label),
				`${option.code}: its discounts appear in response array order`
			).toEqual(option.discounts.map((d) => d.name));
		}
	});

	await test.step('a tier states the cover it does NOT include', async () => {
		// ⛔ The reason the matrix replaced three parallel lists: a list can only stop, which reads
		// as "nothing more to say"; a cell can say "not in this tier". Which of the two it is
		// happens to be the whole basis for choosing a tier.
		const union = new Set(quote.options.flatMap((o) => o.coverages.map((c) => c.code)));
		expect(matrix.rows.length, 'every coverage any tier carries gets exactly one row').toBe(
			union.size
		);

		for (const [index, option] of quote.options.entries()) {
			expect(
				matrix.rows.filter((r) => r.cells[index] === null).length,
				`${option.code}: every coverage it lacks is stated as an absence, not omitted`
			).toBe(union.size - option.coverages.length);
		}
	});

	await test.step('coverage limits and premiums are en-US formatted (DR-11)', async () => {
		// ⛔ NO BRANCH. `| number: 2` already renders en-US despite `<html lang="pl">`; the target
		// leaves `LOCALE_ID` at Angular's default `en-US` and does NOT register `pl`, so
		// `100000` must stay `100,000.00` and never become `100 000,00`.
		for (const [index, option] of quote.options.entries()) {
			const cells = matrix.rows.map((r) => r.cells[index]).filter((c) => c !== null);

			expect(
				cells.map((c) => c.limit),
				`${option.code}: DR-11 coverage limits render as en-US decimals`
			).toEqual(
				option.coverages.map((c) =>
					c.limitRight ? `${money(c.limitLeft)} / ${money(c.limitRight)}` : money(c.limitLeft)
				)
			);

			expect(
				cells.map((c) => c.premium),
				`${option.code}: DR-11 coverage premiums render as en-US decimals`
			).toEqual(option.coverages.map((c) => money(c.premium)));
		}
	});

	await test.step('the four money fields outside the coverage rows (DR-10)', async () => {
		// DR-10: `premiumBeforeDiscounts`, the final premium and the discount value are all
		// interpolated RAW today (`variants.html:19,28`, `discount.html:11`), inconsistently with
		// the coverage rows right above them. The target applies `DecimalPipe '1.2-2'` to all
		// four - keeping the literal `-` prefix on discounts and adding no currency symbol.
		const format = byStage('DR-10', { pre: raw, post: money });

		expect(
			await textsOf(page, TESTID.variantPremiumBeforeDiscounts),
			'DR-10: `premiumBeforeDiscounts` is unformatted today and DecimalPipe-formatted after'
		).toEqual(quote.options.map((o) => format(o.premiumBeforeDiscounts)));

		expect(
			await textsOf(page, TESTID.variantPremium),
			'DR-10: the final premium is unformatted today and DecimalPipe-formatted after'
		).toEqual(quote.options.map((o) => format(o.premium)));

		expect(
			await textsOf(page, TESTID.discountValue),
			'DR-10: the discount value keeps its literal `-` prefix and gains only the formatting'
		).toEqual(quote.options.flatMap((o) => o.discounts.map((d) => `-${format(d.value)}`)));
	});

	await test.step('no currency symbol appears anywhere (DR-10)', async () => {
		// ⛔ NO BRANCH. Adding a currency symbol would be a new feature.
		const text = await renderedText(page);
		for (const symbol of ['$', 'zł', 'PLN', '€']) {
			expect(text, `DR-10: no currency symbol - found '${symbol}'`).not.toContain(symbol);
		}
	});

	await test.step('the coverage description popover renders its content (C-8)', async () => {
		const described = quote.options[0].coverages.find(
			(c) => typeof c.description === 'string' && c.description.length > 0
		);
		expect(described, 'the snapshot must supply at least one coverage description').toBeTruthy();
		const description = described!.description as string;

		expect(
			await renderedText(page),
			'the description is hidden until the coverage row is hovered'
		).not.toContain(description);

		const row = page.getByTestId(TESTID.coverageRow).nth(
			quote.options[0].coverages.indexOf(described!)
		);
		await row.getByTestId(TESTID.coverageHeader).hover();
		await expect
			.poll(
				async () => (await renderedText(page)).includes(description),
				{ message: 'C-8: hovering a coverage row must reveal its description' }
			)
			.toBe(true);

		// C-8: `coverage.description` is documented as HTML. If it were escaped rather than
		// rendered, the page text would carry the literal markup; if it were dropped (which is
		// what MatTooltip would do), the text would not match at all. Byte-equality with the
		// response value covers both failure modes.
		// ⚠️ LIMITATION, recorded honestly: no description in the CURRENT snapshot contains any
		// markup, so the escaping half of this assertion is not exercised by the data. See
		// e2e/README.md.
		golden.record('popover', {
			coverage: described!.code,
			descriptionContainsMarkup: /<[a-zA-Z/!]/.test(description)
		});
	});

	golden.record('quote', {
		options: quote.options.map((o) => ({
			code: o.code,
			order: o.order,
			premium: o.premium,
			premiumBeforeDiscounts: o.premiumBeforeDiscounts,
			coverages: o.coverages.map((c) => ({
				position: c.position,
				code: c.code,
				limitLeft: c.limitLeft,
				limitRight: c.limitRight,
				premium: c.premium
			})),
			discounts: o.discounts.map((d) => ({ position: d.position, code: d.code, value: d.value }))
		}))
	});
	golden.record('renderedMatrixRows', await textsOf(page, TESTID.coverageRow));

	await golden.screenshot(page, 'quote-panel');
});
