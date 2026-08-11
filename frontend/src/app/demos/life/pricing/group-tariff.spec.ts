import { BENEFITS, GroupDetails, VARIANTS, priceGroup } from './group-tariff';

/**
 * The mocked group tariff.
 *
 * As with the individual one, these pin PROPERTIES rather than rates: that each group factor moves
 * the premium the way an underwriter says it should, and — the one that matters most — that the
 * screen never prices on anything an individual policy would ask about.
 */
const TYPICAL: GroupDetails = {
	headcount: 120,
	averageAge: 38,
	industryClass: 1,
	participation: 80,
	includeSpouse: false,
	includeAdultChild: false,
};

describe('Group tariff', () => {
	it('rates the group, never the person', () => {
		// ⛔ The substantive difference from individual cover, and the reason this path exists. A
		// group scheme carries no medical underwriting: no BMI, no tobacco question, no age of the
		// individual. If one of these ever appears in the input type, the demo has stopped modelling
		// the product it claims to.
		const keys = Object.keys(TYPICAL);
		for (const individualFactor of ['smoker', 'heightCm', 'weightKg', 'bmi', 'age', 'gender']) {
			expect(
				keys,
				`group cover has no individual underwriting — '${individualFactor}' must not be an input`,
			).not.toContain(individualFactor);
		}
	});

	it('lands where a Polish employer scheme sits, per employee', () => {
		const [basic, , full] = priceGroup(TYPICAL).variants;

		expect(basic.perEmployee, 'the basic variant is priced like a payroll deduction').toBeGreaterThan(
			25,
		);
		expect(basic.perEmployee, 'and not like an individual policy').toBeLessThan(70);
		expect(full.perEmployee, 'the full variant costs more than the basic one').toBeGreaterThan(
			basic.perEmployee,
		);
	});

	it('moves the premium the way each group factor implies', () => {
		const base = priceGroup(TYPICAL).variants[0].perEmployee;
		const per = (details: Partial<GroupDetails>) =>
			priceGroup({ ...TYPICAL, ...details }).variants[0].perEmployee;

		expect(per({ averageAge: 52 }), 'an older population costs more').toBeGreaterThan(base);
		expect(per({ industryClass: 4 }), 'a higher-risk industry costs more').toBeGreaterThan(base);
		expect(per({ headcount: 6 }), 'a very small group pools badly and is loaded').toBeGreaterThan(
			base,
		);
		expect(per({ headcount: 800 }), 'a large group pools well').toBeLessThan(base);
		expect(per({ includeSpouse: true }), 'extending to a spouse costs more').toBeGreaterThan(base);

		// ⚠️ The factor that distinguishes group business. Low participation means the people who
		// join are the ones expecting to claim, so it is loaded — and a demo that got this backwards
		// would be teaching the opposite of how the product works.
		//
		// ⚠️ Compared across BANDS, not against the default. The tariff grades participation in
		// steps, as a real one does, and the screen's default of 80% already sits in the best band —
		// so asserting that 95% beats 80% would be asserting that the tariff is continuous when it
		// deliberately is not. The first version of this test did exactly that and failed, which is
		// the assertion doing its job.
		const midBand = per({ participation: 60 });
		expect(
			per({ participation: 35 }),
			'low participation is an anti-selection risk and must cost MORE',
		).toBeGreaterThan(midBand);
		expect(
			per({ participation: 95 }),
			'participation past the top threshold earns a better rate',
		).toBeLessThan(midBand);
	});

	it('answers both the employee and the employer', () => {
		const quote = priceGroup(TYPICAL);
		const variant = quote.variants[0];

		expect(quote.insuredCount, '120 employees at 80% participation is 96 insured').toBe(96);
		// Two different conversations: what comes off a payslip, and what the scheme costs. The
		// second must be the first multiplied by the people actually covered, or the employer's
		// figure is fiction.
		expect(
			variant.monthlyTotal,
			'the programme cost is the per-employee premium times the insured count',
		).toBeCloseTo(variant.perEmployee * quote.insuredCount, 2);
	});

	it('offers a ladder in which every variant adds cover', () => {
		const counts = VARIANTS.map(
			({ code }) => BENEFITS.filter((benefit) => benefit.sums[code] !== null).length,
		);

		// The variants are sold as a ladder, so each rung must actually carry more than the one
		// below it — otherwise the middle option is decoration.
		expect(counts[1], 'variant II carries more benefits than I').toBeGreaterThan(counts[0]);
		expect(counts[2], 'variant III carries more benefits than II').toBeGreaterThan(counts[1]);

		for (const benefit of BENEFITS) {
			if (benefit.sums.I !== null && benefit.sums.II !== null) {
				expect(
					benefit.sums.II,
					`${benefit.name}: a higher variant must not insure LESS than a lower one`,
				).toBeGreaterThanOrEqual(benefit.sums.I);
			}
		}
	});
});
