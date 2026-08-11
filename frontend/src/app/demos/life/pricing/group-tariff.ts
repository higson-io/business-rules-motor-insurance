/**
 * A mocked group-life tariff, shaped like the Polish employer-sponsored market.
 *
 * ⛔ NO HIGSON, same as the individual tariff — arithmetic in TypeScript, and `priceGroup()` is the
 * seam a real service would replace.
 *
 * ⛔ AND NO INDIVIDUAL UNDERWRITING, which is the substantive difference from `life-tariff.ts` and
 * the reason the group screen asks completely different questions. A group policy does not assess
 * the person: there is no medical questionnaire, no BMI, no tobacco question, and usually no
 * waiting period for anyone who joins on time. The insurer rates the GROUP — how many people, how
 * old on average, what the employer does, and how many of the staff join — because participation is
 * what stops only the sick from signing up. Asking an employee about their health here would be
 * modelling a product that does not exist.
 */

// ── Inputs ────────────────────────────────────────────────────────────────────────────────────

/** Industry risk classes, in the shape a Polish group underwriter classifies an employer. */
export type IndustryClass = 1 | 2 | 3 | 4;

export interface GroupDetails {
	/** Employees eligible to join. */
	readonly headcount: number;
	/** Average age of the eligible population, whole years. */
	readonly averageAge: number;
	readonly industryClass: IndustryClass;
	/** Share of eligible employees who join, 0–100. The anti-selection control. */
	readonly participation: number;
	readonly includeSpouse: boolean;
	readonly includeAdultChild: boolean;
}

export const HEADCOUNT_RANGE = { min: 3, max: 5000 } as const;
export const PARTICIPATION_RANGE = { min: 10, max: 100 } as const;

// ── The variants ──────────────────────────────────────────────────────────────────────────────

export type VariantCode = 'I' | 'II' | 'III';

/**
 * A benefit line. Group policies are sold as fixed variants, so a benefit is a row and a variant is
 * a column — the same comparison the motor demo makes, for the same reason: three ready-made
 * packages, and the question is what each one contains.
 */
export interface Benefit {
	readonly code: string;
	readonly name: string;
	/** Sum insured per variant, in złoty. `null` where the variant does not carry the benefit. */
	readonly sums: Readonly<Record<VariantCode, number | null>>;
	/** How the figure should read — a lump sum, or a daily benefit. */
	readonly kind: 'suma' | 'dzienne';
}

export const VARIANTS: readonly { readonly code: VariantCode; readonly name: string }[] = [
	{ code: 'I', name: 'Wariant I — podstawowy' },
	{ code: 'II', name: 'Wariant II — rozszerzony' },
	{ code: 'III', name: 'Wariant III — pełny' },
];

export const BENEFITS: readonly Benefit[] = [
	{
		code: 'ZGON',
		name: 'Zgon ubezpieczonego',
		kind: 'suma',
		sums: { I: 50_000, II: 80_000, III: 120_000 },
	},
	{
		code: 'ZGON_NW',
		name: 'Zgon wskutek nieszczęśliwego wypadku',
		kind: 'suma',
		sums: { I: 100_000, II: 160_000, III: 240_000 },
	},
	{
		code: 'USZCZ',
		name: 'Trwały uszczerbek — za 1% uszczerbku',
		kind: 'suma',
		sums: { I: 400, II: 600, III: 900 },
	},
	{
		code: 'ZGON_MALZ',
		name: 'Zgon małżonka',
		kind: 'suma',
		sums: { I: 20_000, II: 30_000, III: 45_000 },
	},
	{
		code: 'ZGON_RODZ',
		name: 'Zgon rodzica lub teścia',
		kind: 'suma',
		sums: { I: 2_000, II: 3_000, III: 4_500 },
	},
	{
		code: 'URODZ',
		name: 'Urodzenie dziecka',
		kind: 'suma',
		sums: { I: 1_500, II: 2_200, III: 3_000 },
	},
	{
		code: 'OSIER',
		name: 'Osierocenie dziecka',
		kind: 'suma',
		sums: { I: 4_000, II: 6_000, III: 9_000 },
	},
	{
		code: 'PZ',
		name: 'Poważne zachorowanie',
		kind: 'suma',
		sums: { I: null, II: 10_000, III: 20_000 },
	},
	{
		code: 'SZP',
		name: 'Pobyt w szpitalu — za dzień',
		kind: 'dzienne',
		sums: { I: null, II: 60, III: 100 },
	},
	{
		code: 'OP',
		name: 'Operacje chirurgiczne',
		kind: 'suma',
		sums: { I: null, II: 5_000, III: 10_000 },
	},
	{
		code: 'NDP',
		name: 'Trwała niezdolność do pracy',
		kind: 'suma',
		sums: { I: null, II: null, III: 60_000 },
	},
	{
		code: 'AM',
		name: 'Assistance medyczny',
		kind: 'suma',
		sums: { I: null, II: null, III: 1 },
	},
];

/** Base monthly premium per employee, before any group factor. */
const BASE_PER_EMPLOYEE: Readonly<Record<VariantCode, number>> = { I: 42, II: 63, III: 89 };

// ── Group factors ─────────────────────────────────────────────────────────────────────────────

function ageFactor(averageAge: number): number {
	if (averageAge < 30) {
		return 0.85;
	}
	if (averageAge < 40) {
		return 1;
	}
	if (averageAge < 50) {
		return 1.25;
	}
	return 1.6;
}

const INDUSTRY_FACTOR: Record<IndustryClass, number> = { 1: 1, 2: 1.15, 3: 1.35, 4: 1.7 };

/** Larger groups pool better, so the loading for a small one is real rather than a discount ladder. */
function sizeFactor(headcount: number): number {
	if (headcount < 10) {
		return 1.15;
	}
	if (headcount < 50) {
		return 1;
	}
	if (headcount < 200) {
		return 0.94;
	}
	return 0.88;
}

/**
 * The anti-selection control, and the factor that actually distinguishes group business.
 *
 * When only half the staff join, the half that joins is the half that expects to claim. High
 * participation is what makes a group policy cheap, which is why an insurer will quote a better rate
 * to an employer who funds the premium than to one who lets people opt in individually.
 */
function participationFactor(participation: number): number {
	if (participation < 50) {
		return 1.12;
	}
	if (participation < 75) {
		return 1;
	}
	return 0.93;
}

const SPOUSE_FACTOR = 1.35;
const ADULT_CHILD_FACTOR = 1.12;

// ── Pricing ───────────────────────────────────────────────────────────────────────────────────

export interface VariantPremium {
	readonly code: VariantCode;
	readonly name: string;
	/** Monthly, per employee, in złoty. */
	readonly perEmployee: number;
	/** Monthly, for the whole insured population. */
	readonly monthlyTotal: number;
}

export interface GroupQuote {
	readonly variants: readonly VariantPremium[];
	/** How many people actually join, given headcount and participation. */
	readonly insuredCount: number;
}

/**
 * Prices the three variants for a group.
 *
 * ⛔ The seam. Returns per-employee AND total, because those are two different conversations: the
 * employee asks what comes off their payslip, the employer asks what the scheme costs. A screen that
 * showed only one of them would be useless to half the room.
 */
export function priceGroup(details: GroupDetails): GroupQuote {
	const factor =
		ageFactor(details.averageAge) *
		INDUSTRY_FACTOR[details.industryClass] *
		sizeFactor(details.headcount) *
		participationFactor(details.participation) *
		(details.includeSpouse ? SPOUSE_FACTOR : 1) *
		(details.includeAdultChild ? ADULT_CHILD_FACTOR : 1);

	const insuredCount = Math.max(1, Math.round((details.headcount * details.participation) / 100));

	return {
		insuredCount,
		variants: VARIANTS.map(({ code, name }) => {
			const perEmployee = Math.round(BASE_PER_EMPLOYEE[code] * factor * 100) / 100;
			return {
				code,
				name,
				perEmployee,
				monthlyTotal: Math.round(perEmployee * insuredCount * 100) / 100,
			} satisfies VariantPremium;
		}),
	};
}
