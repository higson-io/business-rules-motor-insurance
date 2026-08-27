/**
 * Wire model of `PropertyApi`. Every field is what the backend actually sends — nullability
 * included, because `strictTemplates` turns a wrong guess here into a compile error rather than a
 * blank cell.
 *
 * ⛔ There is no union of question codes here, and that is the point. The catalogue lives in
 * Higson and changes with the policy date; a `type QuestionCode = 'AREA' | …` would put the
 * regulator's decision back into a build artefact.
 */

import { ApkRecommendationDto } from './apk';

/** How to render the control. Comes from the question's KIND attribute. */
export type QuestionKind = 'CHOICE' | 'MULTI' | 'NUMBER' | 'MONEY' | 'BOOLEAN' | 'TEXT';

/** What the answer IS, separately from how it is drawn — see the VALUE_TYPE attribute. */
export type QuestionValueType = 'STRING' | 'INTEGER' | 'DECIMAL' | 'BOOLEAN' | 'DATE';

/** Sections of the questionnaire. NEEDS and EXCLUSIONS exist only under the 2026 recommendations. */
export type QuestionSection = 'SUBJECT' | 'SITUATION' | 'NEEDS' | 'EXCLUSIONS';

export interface PropertyQuestionDto {
	readonly code: string;
	readonly text: string;
	readonly kind: QuestionKind;
	readonly valueType: QuestionValueType;
	readonly section: QuestionSection;
	readonly help: string | null;
	readonly position: number;
	/** ⛔ Computed by the rules, never by this application. */
	readonly visible: boolean;
	readonly required: boolean;
	readonly options: readonly string[];
	readonly optionLabels: readonly string[];
	/** Non-empty only where the value really was prefilled from elsewhere (recommendation 8.3). */
	readonly prefillSource: string | null;
	/** What happens if the client declines to answer (recommendation 8.4). */
	readonly refusalMessage: string | null;
	readonly value: string | null;
	readonly values: readonly string[];
	readonly refused: boolean;
}

export interface PropertyCoverDto {
	readonly code: string;
	readonly name: string;
	readonly description: string | null;
	readonly position: number;
	readonly sumBasis: string | null;
	readonly capLabel: string | null;
	readonly effectiveLimit: number | null;
	readonly sum: number | null;
}

export interface PropertyVariantDto {
	readonly code: string;
	readonly name: string;
	readonly description: string | null;
	readonly position: number;
	readonly available: boolean;
	/** Present exactly when `available` is false — the variant never disappears silently. */
	readonly unavailableReason: string | null;
	readonly liabilitySum: number | null;
	readonly annualPremium: number | null;
	readonly instalment: number | null;
	readonly breakdown: Readonly<Record<string, number>>;
	readonly covers: readonly string[];
}

// Potrzeby, ostrzeżenia i rekomendacja majątkowa NIE mają już własnych typów: to jest ta sama
// analiza co w pozostałych dwóch demach, więc jedzie tym samym `ApkRecommendationDto`.

/** The APK regime in force for the chosen policy date. Versioned by the region, not by the clock. */
export interface PropertyRegimeDto {
	readonly code: string | null;
	readonly name: string | null;
	readonly legalBasis: string | null;
	readonly requiresBudget: boolean;
	readonly requiresExclusionCheck: boolean;
	readonly requiresJustification: boolean;
	readonly requiresVariantDifferences: boolean;
}

export interface PropertyQuoteDto {
	readonly startDate: string;
	readonly deductible: number;

	readonly regime: PropertyRegimeDto;
	readonly questions: readonly PropertyQuestionDto[];

	readonly floodZone: string | null;
	readonly theftZone: string | null;
	readonly buildingAge: number | null;
	readonly riskFactor: number | null;

	readonly sumBuilding: number | null;
	readonly sumFixtures: number | null;
	readonly sumContents: number | null;
	/** What the rules computed, kept beside what the client chose — that is what makes a warning possible. */
	readonly suggestedSumBuilding: number | null;

	readonly covers: readonly PropertyCoverDto[];
	readonly variants: readonly PropertyVariantDto[];
	readonly recommendation: ApkRecommendationDto;
	readonly selectedVariant: string | null;
}
