/**
 * A mocked life tariff, shaped like the Polish individual protection market.
 *
 * ⛔ NO HIGSON. Everything here is arithmetic in TypeScript, deliberately — this demo exists to show
 * the screen, not the engine. The seam is `priceQuote()`: it takes the answers and returns a
 * breakdown, which is exactly the shape a `LifeQuoteService` calling the runtime would return. When
 * the rules exist, this file is what gets deleted.
 *
 * The figures are plausible rather than real. A 35-year-old non-smoker in the lowest occupational
 * class, insured for 300 000 zł, lands around 60 zł a month here, which is roughly where the Polish
 * market sits for term protection — close enough to demo without apologising for the numbers, and
 * nowhere near a quotation anyone should rely on.
 *
 * ⛔ NO GENDER. Not an omission: since the Court of Justice's Test-Achats ruling took effect in
 * December 2012, EU insurers may not price on sex, so a life calculator that asked for it and then
 * charged women less would be demonstrating something illegal. Age, tobacco use, build and
 * occupational risk are all still lawful rating factors, and they are what this tariff uses.
 */

// ── Inputs ────────────────────────────────────────────────────────────────────────────────────

/**
 * Occupational risk classes. Four is the usual split in Polish underwriting; the labels name the
 * kind of work rather than a job title, because that is how the classification actually reads.
 */
export type OccupationClass = 1 | 2 | 3 | 4;

export interface InsuredDetails {
	/** Whole years at entry. Polish individual protection is typically written from 18 to 70. */
	readonly age: number;
	readonly smoker: boolean;
	/** Centimetres. With `weightKg`, the only use is the BMI band below. */
	readonly heightCm: number;
	readonly weightKg: number;
	readonly occupationClass: OccupationClass;
}

export const AGE_RANGE = { min: 18, max: 70 } as const;

// ── The main policy ───────────────────────────────────────────────────────────────────────────

/** Monthly rate per 1 000 zł of death cover, by age band. */
const BASE_RATE_PER_1000: readonly { readonly upTo: number; readonly rate: number }[] = [
	{ upTo: 29, rate: 0.09 },
	{ upTo: 34, rate: 0.13 },
	{ upTo: 39, rate: 0.19 },
	{ upTo: 44, rate: 0.29 },
	{ upTo: 49, rate: 0.46 },
	{ upTo: 54, rate: 0.75 },
	{ upTo: 59, rate: 1.22 },
	{ upTo: 64, rate: 1.95 },
	{ upTo: 70, rate: 3.1 },
];

const SMOKER_FACTOR = 1.75;

/** Class 1 is office work; class 4 is the one with heights, machinery or a uniform. */
const OCCUPATION_FACTOR: Record<OccupationClass, number> = { 1: 1, 2: 1.15, 3: 1.4, 4: 1.9 };

function rateFor(age: number): number {
	const band = BASE_RATE_PER_1000.find((b) => age <= b.upTo);
	return band?.rate ?? BASE_RATE_PER_1000[BASE_RATE_PER_1000.length - 1].rate;
}

export function bmiOf(details: InsuredDetails): number {
	const metres = details.heightCm / 100;
	if (metres <= 0) {
		return 0;
	}
	return details.weightKg / (metres * metres);
}

/** Underweight and obesity both load the premium; the middle band is neutral. */
function bmiFactor(bmi: number): number {
	if (bmi === 0) {
		return 1;
	}
	if (bmi < 18.5) {
		return 1.15;
	}
	if (bmi < 27) {
		return 1;
	}
	if (bmi < 32) {
		return 1.25;
	}
	return 1.6;
}

/** Everything that scales a rate but is not the sum insured. */
function riskFactor(details: InsuredDetails): number {
	return (
		(details.smoker ? SMOKER_FACTOR : 1) *
		OCCUPATION_FACTOR[details.occupationClass] *
		bmiFactor(bmiOf(details))
	);
}

// ── Riders ────────────────────────────────────────────────────────────────────────────────────

export type RiderCode = 'NW' | 'PZ' | 'UT' | 'SZP' | 'OP' | 'NDP' | 'AM';

/**
 * How a rider's cover is expressed. Polish policies mix all three on one contract, which is why the
 * screen cannot simply ask for "a sum" the way the motor demo asks for a limit.
 */
export type CoverKind =
	/** A one-off benefit, quoted per 1 000 zł of sum insured. */
	| 'suma'
	/** A daily hospital benefit, quoted per złoty per day. */
	| 'dzienne'
	/** A flat monthly charge — assistance services, where there is no sum to insure. */
	| 'ryczalt';

export interface Rider {
	readonly code: RiderCode;
	readonly name: string;
	readonly description: string;
	readonly kind: CoverKind;
	/** Selectable cover amounts. Empty for `ryczalt`, where there is nothing to choose. */
	readonly amounts: readonly number[];
	readonly defaultAmount: number;
	/** Monthly rate per 1 000 zł, per złoty of daily benefit, or the flat charge itself. */
	readonly rate: number;
	/** Whether age scales this rider. Accident cover barely does; illness cover does sharply. */
	readonly agedRated: boolean;
	/** The waiting period, in the wording a Polish policy document uses. */
	readonly karencja: string | null;
}

export const RIDERS: readonly Rider[] = [
	{
		code: 'PZ',
		name: 'Poważne zachorowanie',
		description:
			'Jednorazowe świadczenie po zdiagnozowaniu jednej z objętych umową chorób — m.in. nowotwór złośliwy, zawał serca, udar mózgu, niewydolność nerek.',
		kind: 'suma',
		amounts: [20_000, 50_000, 100_000, 150_000, 200_000],
		defaultAmount: 50_000,
		rate: 0.36,
		agedRated: true,
		karencja: '90 dni',
	},
	{
		code: 'NW',
		name: 'Śmierć wskutek nieszczęśliwego wypadku',
		description:
			'Świadczenie wypłacane dodatkowo, ponad sumę z umowy głównej, jeżeli przyczyną zgonu był nieszczęśliwy wypadek.',
		kind: 'suma',
		amounts: [50_000, 100_000, 200_000, 300_000, 500_000],
		defaultAmount: 100_000,
		rate: 0.035,
		agedRated: false,
		karencja: null,
	},
	{
		code: 'UT',
		name: 'Trwały uszczerbek na zdrowiu wskutek NW',
		description:
			'Świadczenie proporcjonalne do orzeczonego procentu uszczerbku — 1% sumy ubezpieczenia za każdy 1% uszczerbku.',
		kind: 'suma',
		amounts: [20_000, 50_000, 100_000, 150_000],
		defaultAmount: 50_000,
		rate: 0.075,
		agedRated: false,
		karencja: null,
	},
	{
		code: 'SZP',
		name: 'Pobyt w szpitalu',
		description:
			'Świadczenie za każdy dzień pobytu w szpitalu, od pierwszego dnia w następstwie NW i od czwartego w następstwie choroby.',
		kind: 'dzienne',
		amounts: [50, 100, 150, 200, 300],
		defaultAmount: 100,
		rate: 0.115,
		agedRated: true,
		karencja: '30 dni (choroba)',
	},
	{
		code: 'OP',
		name: 'Operacje chirurgiczne',
		description:
			'Świadczenie za operację z katalogu, w wysokości zależnej od jej klasy — od 10% do 100% sumy ubezpieczenia.',
		kind: 'suma',
		amounts: [10_000, 20_000, 30_000, 50_000],
		defaultAmount: 20_000,
		rate: 0.14,
		agedRated: true,
		karencja: '180 dni',
	},
	{
		code: 'NDP',
		name: 'Trwała niezdolność do pracy',
		description:
			'Świadczenie po orzeczeniu całkowitej i trwałej niezdolności do pracy zarobkowej, niezależnie od jej przyczyny.',
		kind: 'suma',
		amounts: [50_000, 100_000, 200_000, 300_000],
		defaultAmount: 100_000,
		rate: 0.21,
		agedRated: true,
		karencja: '180 dni',
	},
	{
		code: 'AM',
		name: 'Assistance medyczny',
		description:
			'Wizyty domowe lekarza, transport medyczny, konsultacje telemedyczne i opieka pielęgniarska po hospitalizacji.',
		kind: 'ryczalt',
		amounts: [],
		defaultAmount: 0,
		rate: 9,
		agedRated: false,
		karencja: null,
	},
];

export const MAIN_SUMS: readonly number[] = [
	50_000, 100_000, 150_000, 200_000, 300_000, 500_000, 750_000, 1_000_000,
];

export const DEFAULT_MAIN_SUM = 300_000;

// ── Pricing ───────────────────────────────────────────────────────────────────────────────────

export interface PremiumLine {
	readonly code: 'MAIN' | RiderCode;
	readonly name: string;
	/** The cover this line buys, in the unit its `kind` implies. */
	readonly amount: number;
	readonly kind: CoverKind;
	/** Monthly, in złoty. */
	readonly premium: number;
}

export interface LifeQuote {
	readonly lines: readonly PremiumLine[];
	/** Monthly total, in złoty. */
	readonly total: number;
}

/** Riders scale with age more gently than the main cover does. */
function agedFactor(age: number): number {
	return rateFor(age) / rateFor(35);
}

function premiumFor(rider: Rider, amount: number, details: InsuredDetails): number {
	const risk = riskFactor(details);
	const aged = rider.agedRated ? agedFactor(details.age) : 1;

	switch (rider.kind) {
		case 'suma':
			return (amount / 1000) * rider.rate * risk * aged;
		case 'dzienne':
			return amount * rider.rate * risk * aged;
		case 'ryczalt':
			// Assistance is a service, not a risk transfer, so nothing scales it.
			return rider.rate;
	}
}

/**
 * Prices a configured policy.
 *
 * ⛔ The seam. A real implementation calls the runtime and returns the same shape; nothing outside
 * this module knows the difference, which is the point of returning a breakdown rather than a
 * number. Rounding is to the grosz, once, at the end of each line — summing unrounded lines and
 * rounding the total would print a total that does not match its own parts.
 */
export function priceQuote(
	details: InsuredDetails,
	mainSum: number,
	selected: ReadonlyMap<RiderCode, number>,
): LifeQuote {
	const round = (value: number) => Math.round(value * 100) / 100;

	const main: PremiumLine = {
		code: 'MAIN',
		name: 'Umowa główna — na życie',
		amount: mainSum,
		kind: 'suma',
		premium: round((mainSum / 1000) * rateFor(details.age) * riskFactor(details)),
	};

	const riderLines = RIDERS.filter((rider) => selected.has(rider.code)).map((rider) => {
		const amount = selected.get(rider.code) ?? rider.defaultAmount;
		return {
			code: rider.code,
			name: rider.name,
			amount,
			kind: rider.kind,
			premium: round(premiumFor(rider, amount, details)),
		} satisfies PremiumLine;
	});

	const lines = [main, ...riderLines];
	return { lines, total: round(lines.reduce((sum, line) => sum + line.premium, 0)) };
}
