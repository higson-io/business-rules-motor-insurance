import { ApkQuestionDto, ApkRegimeDto } from './apk';

/** Mirrors `api/dto/CoverageDto.java`. */
export interface CoverageDto {
	position: number;
	code: string;
	name: string;
	limitLeft: number;
	/**
	 * `BigDecimal` on the wire (`CoverageDto.java:11`) and genuinely absent for single-limit
	 * coverages — six of the nine recorded ones send `null` (`e2e/goldens/pre/quote-panel.json`).
	 */
	limitRight: number | null;
	premium: number;
	/**
	 * HTML produced by the Higson rules engine, rendered as markup rather than as text. `null` for
	 * the three recorded coverages the rules supply no description for.
	 */
	description: string | null;
}

/** Mirrors `api/dto/DiscountDto.java`. */
export interface DiscountDto {
	code: string;
	name: string;
	value: number;
	position: number;
}

/**
 * Mirrors `api/dto/OptionDto.java`. The backend already returns `coverages` and `discounts` ordered
 * by ascending `position`, so consumers render in array order and add no sort of their own.
 */
export interface OptionDto {
	code: string;
	order: number;
	premium: number;
	premiumBeforeDiscounts: number;
	coverages: CoverageDto[];
	discounts: DiscountDto[];
}

/** Mirrors `api/dto/QuoteDto.java`. */
export interface QuoteDto {
	options: OptionDto[];

	/**
	 * Data początku ochrony.
	 *
	 * ⛔ Wybiera wersję regionu APK_MOTOR, czyli reżim analizy potrzeb. Front NIE zna daty progu —
	 * nazwę reżimu podaje `apkRegime`, a to, o co wolno zapytać, wynika z długości `apkQuestions`.
	 */
	policyStartDate: string;

	apkRegime: ApkRegimeDto;

	apkQuestions: ApkQuestionDto[];
}
