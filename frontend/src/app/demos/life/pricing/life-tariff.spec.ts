import { InsuredDetails, RIDERS, RiderCode, bmiOf, priceQuote } from './life-tariff';

/**
 * The mocked tariff.
 *
 * These are not tests of a rate table — the rates are invented and may be re-invented. They pin the
 * PROPERTIES a rate table has to hold if the demo is to be shown to anyone: that risk factors move
 * the premium in the direction the underwriting says, that the breakdown sums to the total it
 * prints, and that nothing prices on sex.
 */
const HEALTHY: InsuredDetails = {
	age: 35,
	smoker: false,
	heightCm: 178,
	weightKg: 76,
	occupationClass: 1,
};

const NONE = new Map<RiderCode, number>();

describe('Life tariff', () => {
	it('lands in the range the Polish market charges for term protection', () => {
		const monthly = priceQuote(HEALTHY, 300_000, NONE).total;

		// A 35-year-old non-smoker doing office work, insured for 300 000 zł. The band is wide on
		// purpose: the point is that the figure is plausible enough to show a client without
		// explaining it away, not that it matches any particular insurer.
		expect(
			monthly,
			`300 000 zł of cover at 35 costs ${monthly} zł a month — outside a believable band`,
		).toBeGreaterThan(40);
		expect(monthly, 'the same figure must not read as a bargain either').toBeLessThan(90);
	});

	it('moves the premium in the direction each risk factor implies', () => {
		const base = priceQuote(HEALTHY, 300_000, NONE).total;

		const older = priceQuote({ ...HEALTHY, age: 55 }, 300_000, NONE).total;
		expect(older, 'age is the strongest factor in life cover — 55 must cost well over 35').toBeGreaterThan(
			base * 2,
		);

		const smoker = priceQuote({ ...HEALTHY, smoker: true }, 300_000, NONE).total;
		expect(smoker, 'tobacco use loads the premium').toBeGreaterThan(base);

		const roofer = priceQuote({ ...HEALTHY, occupationClass: 4 }, 300_000, NONE).total;
		expect(roofer, 'the highest occupational class loads the premium').toBeGreaterThan(base);

		const obese = priceQuote({ ...HEALTHY, weightKg: 115 }, 300_000, NONE).total;
		expect(obese, 'a BMI past the obesity band loads the premium').toBeGreaterThan(base);

		// Cover scales the premium proportionally: this is the one relationship a buyer checks by
		// halving the sum and expecting to see roughly half the price.
		const half = priceQuote(HEALTHY, 150_000, NONE).total;
		expect(half, 'halving the sum insured halves the main premium').toBeCloseTo(base / 2, 1);
	});

	it('prices no factor that EU law forbids', () => {
		// ⛔ Test-Achats. Since December 2012 an EU insurer may not differentiate premium by sex, so
		// the input type has no such field and this asserts it stays that way — a demo that charged
		// women less would be showing something unlawful, however plausible the arithmetic looked.
		const keys = Object.keys(HEALTHY);
		for (const forbidden of ['gender', 'sex', 'plec', 'płeć']) {
			expect(keys, `the tariff must not take '${forbidden}' as an input`).not.toContain(forbidden);
		}
	});

	it('produces a breakdown that adds up to the total it prints', () => {
		const selected = new Map<RiderCode, number>([
			['PZ', 100_000],
			['SZP', 200],
			['AM', 0],
		]);
		const quote = priceQuote(HEALTHY, 300_000, selected);

		expect(quote.lines.map((l) => l.code), 'the main contract leads, then the chosen riders').toEqual([
			'MAIN',
			'PZ',
			'SZP',
			'AM',
		]);

		// ⚠️ Rounding happens per line, once. Summing unrounded lines and rounding the total would
		// print a total that does not equal its own parts — which on a quotation screen is the one
		// arithmetic error a reader will always catch.
		const summed = quote.lines.reduce((sum, line) => sum + line.premium, 0);
		expect(quote.total, 'the printed total equals the sum of the printed lines').toBe(
			Math.round(summed * 100) / 100,
		);
	});

	it('charges a flat rate for the service rider and nothing else scales it', () => {
		const assistance = RIDERS.find((r) => r.code === 'AM');
		const withAssistance = new Map<RiderCode, number>([['AM', 0]]);

		const young = priceQuote(HEALTHY, 100_000, withAssistance).lines.find((l) => l.code === 'AM');
		const old = priceQuote({ ...HEALTHY, age: 65, smoker: true }, 100_000, withAssistance).lines.find(
			(l) => l.code === 'AM',
		);

		// Assistance buys visits and transport, not a transfer of risk, so age and tobacco have
		// nothing to act on.
		expect(young?.premium, 'the service rider is a flat charge').toBe(assistance?.rate);
		expect(old?.premium, 'and stays flat regardless of the risk profile').toBe(assistance?.rate);
	});

	it('computes BMI from the two fields that produce it', () => {
		expect(bmiOf({ ...HEALTHY, heightCm: 180, weightKg: 81 }), '81 kg at 180 cm is BMI 25').toBeCloseTo(
			25,
			1,
		);
		expect(bmiOf({ ...HEALTHY, heightCm: 0 }), 'a nonsense height yields 0, never Infinity').toBe(0);
	});
});
