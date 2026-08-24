/**
 * Model analizy potrzeb klienta na drucie — wspólny dla wszystkich trzech linii produktowych.
 *
 * ⛔ Wspólny nie dla oszczędności, tylko dlatego, że APK jest jedna. Art. 8 ustawy o dystrybucji
 * ubezpieczeń nie rozróżnia ubezpieczeń majątkowych, komunikacyjnych ani życiowych, a termin
 * dostosowania z §2 uchwały KNF 190/2026 jest dla wszystkich ten sam. Różni się wyłącznie katalog
 * pytań — a ten jest konfiguracją, nie kodem.
 */

/** Sekcja ankiety. Same sekcje pochodzą z reguł; tylko ich tytuły żyją w komponencie. */
export type ApkSection = 'SUBJECT' | 'SITUATION' | 'NEEDS' | 'EXCLUSIONS';

/** Rodzaj kontrolki. ⚠️ To NIE jest typ wartości — ten niesie `valueType`. */
export type ApkQuestionKind = 'CHOICE' | 'MULTI' | 'NUMBER' | 'MONEY' | 'BOOLEAN' | 'TEXT';

export interface ApkRegimeDto {
	readonly code: string;
	readonly name: string;
	readonly legalBasis: string | null;
	readonly requiresBudget: boolean;
	readonly requiresExclusionCheck: boolean;
	readonly requiresJustification: boolean;
	readonly requiresVariantDifferences: boolean;
}

export interface ApkQuestionDto {
	readonly code: string;
	readonly text: string;
	readonly kind: ApkQuestionKind;
	readonly valueType: string;
	readonly section: ApkSection;
	readonly help: string | null;
	readonly position: number;
	readonly visible: boolean;
	readonly required: boolean;
	readonly options: readonly string[];
	readonly optionLabels: readonly string[];
	/** Rek. 8.3 — skąd wartość pochodzi, jeżeli nie od klienta. Puste znaczy „klient podał sam". */
	readonly prefillSource: string | null;
	/** Rek. 8.4 — co się stanie, jeżeli klient odmówi odpowiedzi. */
	readonly refusalMessage: string | null;
	readonly value: string | null;
	readonly values: readonly string[];
	readonly refused: boolean;
}

/** Co komponent ankiety zgłasza na zewnątrz. Sam nic nie wysyła — wysyła gospodarz. */
export interface ApkAnswerChange {
	readonly code: string;
	readonly value: string;
}

export interface ApkMultiAnswerChange {
	readonly code: string;
	readonly values: readonly string[];
}

export interface ApkRefusalChange {
	readonly code: string;
	readonly refused: boolean;
}
