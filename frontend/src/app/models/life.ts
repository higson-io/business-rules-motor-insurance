import { ApkQuestionDto, ApkRecommendationDto, ApkRegimeDto } from './apk';

/**
 * Wire model of `LifeApi`. Every field is what the backend actually sends — nullability included,
 * because `strictTemplates` turns a wrong guess here into a compile error rather than a blank cell.
 */

export type LifeLineCode = 'INDIVIDUAL' | 'GROUP';

/** ACCEPT / REFER / DECLINE — the three underwriting outcomes, see decision D-1. */
export type UnderwritingDecision = 'ACCEPT' | 'REFER' | 'DECLINE';

/**
 * What the number in a "sum" field means. This is the only attribute that says so, and it drives
 * both the premium formula and how the amount must be rendered.
 */
export type BenefitKind = 'SUM_ASSURED' | 'DAILY_AMOUNT' | 'PER_PERCENT' | 'SERVICE_PACKAGE';

export type PaymentFrequency = 'MONTHLY' | 'QUARTERLY' | 'SEMIANNUAL' | 'ANNUAL';

export interface LifeRiderDto {
	readonly code: string;
	readonly name: string;
	readonly description: string | null;
	readonly position: number;
	readonly benefitKind: BenefitKind | null;
	readonly sums: readonly number[] | null;
	readonly defaultSum: number | null;
	/** Effective cap — the lower of the product cap and the income cap (D-7). */
	readonly maxSum: number | null;
	readonly waitingPeriodDays: number | null;
	readonly selected: boolean;
	readonly sum: number | null;
	readonly premium: number | null;
}

export interface LifeVariantDto {
	readonly code: string;
	readonly name: string;
	readonly position: number;
	readonly available: boolean;
	readonly perEmployee: number | null;
	/**
	 * "What it would cost if EVERYONE took this variant" — a hypothesis, not the programme cost.
	 * The real cost needs the enrolment split and lives in `programCost`.
	 */
	readonly monthlyTotalIfAll: number | null;
}

export interface LifeBenefitDto {
	readonly code: string;
	readonly name: string;
	readonly position: number;
	readonly benefitKind: BenefitKind | null;
	readonly sumByVariant: Readonly<Record<string, number>> | null;
}

/**
 * Obowiązująca wersja taryfy, podana przez `demo.life.tariff.version`.
 *
 * ⛔ Nie licz jej z `startDate`. Data progu jest osią czasu regionu w Higsonie; kopia w TypeScripcie
 * byłaby drugą definicją tej samej reguły i to ona zdezaktualizowałaby się pierwsza.
 */
export interface LifeTariffVersionDto {
	readonly code: string;
	readonly name: string;
	/** Data początku obowiązywania, gotowa do wyświetlenia. Też z reguł. */
	readonly validFrom: string;
}

export interface LifeQuoteDto {
	readonly lineCode: LifeLineCode;

	readonly startDate: string;
	readonly tariffVersion: LifeTariffVersionDto;
	readonly mainSum: number;
	readonly paymentFrequency: PaymentFrequency;

	readonly birthDate: string | null;
	readonly entryAge: number | null;
	readonly smoker: boolean;
	readonly heightCm: number;
	readonly weightKg: number;
	readonly bmi: number;
	readonly occupationClass: number;
	readonly monthlyIncome: number | null;
	readonly priorCoverFrom: string | null;
	readonly priorCoverTo: string | null;

	readonly decision: UnderwritingDecision | null;
	/**
	 * Whether the shown price can be bought. Separate from `premiumMonthly` on purpose: without it
	 * the screen cannot tell a binding price from a provisional one and would show a REFER premium
	 * exactly like an ACCEPT one.
	 */
	readonly binding: boolean;
	readonly message: string | null;
	readonly medicalRequired: boolean;

	readonly premiumMonthly: number | null;
	readonly instalment: number | null;
	readonly installments: number | null;
	readonly premiumAnnual: number | null;

	readonly riders: readonly LifeRiderDto[];

	readonly headcount: number;
	readonly averageAge: number;
	readonly industryClass: number;
	readonly participation: number;
	readonly includeSpouse: boolean;
	readonly includeAdultChild: boolean;
	readonly insuredCount: number | null;
	readonly programCost: number | null;

	/**
	 * Rozkład wyborów pracowników: kod wariantu → liczba osób.
	 *
	 * ⛔ Pusta mapa to FAZA OFERTY, nie „nikt nie wybrał". W fazie oferty partycypacja jest
	 * deklaracją pracodawcy, a `programCost` nie istnieje; pierwsze przystąpienie przełącza obie
	 * te rzeczy naraz i robią to reguły (D-2).
	 */
	readonly enrolment: Readonly<Record<string, number>>;

	/**
	 * Reżim analizy potrzeb i ankieta.
	 *
	 * ⛔ Wersjonowane DRUGIM regionem — APK_LIFE, próg 1 lipca 2027. `tariffVersion` idzie przez
	 * region LIFE z progiem 1 lipca 2026. Ta sama data polisy rozwiązuje obie osie niezależnie,
	 * więc można przeciąć jedną bez drugiej.
	 */
	readonly apkRegime: ApkRegimeDto;

	readonly apkQuestions: readonly ApkQuestionDto[];

	/**
	 * Wynik analizy potrzeb: potrzeby, ostrzeżenia o wyłączeniach i uzasadnienie.
	 *
	 * ⛔ `variantCode` jest PUSTY w ochronie indywidualnej i to nie jest brak danych. Tam nie ma
	 * czego wybierać z półki — propozycją jest zestaw umów dodatkowych.
	 */
	readonly apkRecommendation: ApkRecommendationDto | null;

	readonly variants: readonly LifeVariantDto[];
	readonly benefits: readonly LifeBenefitDto[];
}
