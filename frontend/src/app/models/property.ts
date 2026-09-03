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

export interface PropertyOfferDto {
	readonly number: string | null;
	readonly issuedOn: string | null;
	readonly validUntil: string | null;
	readonly paymentDueOn: string | null;
	readonly statement: string | null;
	/** Rek. 8.7 — czy oferta niesie dokument analizy potrzeb. Z reguł, zmienia się z reżimem. */
	readonly needsAnalysisAttached: boolean;

	readonly policyNumber: string | null;
	readonly coverFrom: string | null;
	readonly coverTo: string | null;

	/**
	 * Wariant i składka zamrożone przy wystawieniu — puste, dopóki to oferta.
	 *
	 * ⛔ Wydany dokument niesie SWOJE liczby. Bez tego wystarczyło wrócić do ankiety i zmienić
	 * metraż, żeby polisa cicho pokazała inną składkę niż ta, na którą klient się zgodził.
	 */
	readonly variantCode: string | null;
	readonly premium: number | null;

	/**
	 * Rek. 9.3 — notatka o tym, czy klient wziął rekomendowany wariant.
	 *
	 * ⛔ `unmetNeeds` liczy się względem WYBORU klienta, a nie rekomendacji: gdy klient zejdzie
	 * niżej, bez pokrycia zostaje więcej niż mówi `recommendation.missingCovers`.
	 */
	readonly choiceKind: 'MATCHES' | 'RICHER' | 'POORER' | null;
	readonly choiceLabel: string | null;
	readonly choiceText: string | null;
	readonly choiceRequiresAcknowledgement: boolean;
	readonly unmetNeeds: readonly string[];
}

/** Ubezpieczający. Dane demonstracyjne — w prawdziwym wdrożeniu z CRM, nie z reguł. */
/**
 * Sekcja ankiety — **gałąź drzewa domeny**, nie wartość atrybutu na pytaniu.
 *
 * ⛔ Tytuł przychodzi z reguł. Wcześniej cztery tytuły były stałą w komponencie, więc dołożenie
 * sekcji w Studio wymagało wdrożenia frontu — dokładnie tego, czemu to demo zaprzecza.
 *
 * ⚠️ `visible` (reguła `demo.property.apk.section.visibility`) to co innego niż NIEOBECNOŚĆ
 * sekcji na liście: sekcji spoza wersji regionu nie ma tu wcale, razem z jej pytaniami.
 */
export interface PropertySectionDto {
	readonly code: string;
	readonly title: string;
	/**
	 * `FORM` — o to pytamy klienta. `KNOWN` — to już wiemy i tylko pokazujemy.
	 *
	 * ⛔ Charakterystyka przedmiotu ubezpieczenia nie jest analizą potrzeb: zakłady zbierają ją
	 * i dziś, poza APK. Ekran pokazuje ją zwiniętą, jako kontekst — a o tym, która sekcja jest
	 * którą, decyduje atrybut w Higsonie, nie lista kodów w komponencie.
	 */
	readonly kind: 'FORM' | 'KNOWN';
	readonly position: number;
	readonly visible: boolean;
}

export interface PropertyPolicyholderDto {
	readonly firstName: string;
	readonly lastName: string;
	readonly pesel: string;
	readonly birthDate: string;
	readonly idDocument: string;
	readonly street: string;
	readonly flat: string;
	readonly postcode: string;
	readonly city: string;
	readonly email: string;
	readonly phone: string;
	/** Rek. 8.3 — skąd wzięły się dane, skoro klient ich tu nie wpisywał. */
	readonly source: string;
}

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
	readonly sections: readonly PropertySectionDto[];
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

	/**
	 * Oferta, a po akceptacji — polisa.
	 *
	 * ⛔ `policyNumber` puste znaczy „jeszcze oferta". Osobnego pola stanu nie ma i nie powinno
	 * być: to jeden dokument, który zmienia stan, a nie dwa byty do zsynchronizowania.
	 */
	readonly offer: PropertyOfferDto;
	readonly policyholder: PropertyPolicyholderDto;
	readonly selectedVariant: string | null;

	/**
	 * Odpowiedź, po której nie ma czego dalej analizować — dziś brak potrzeby ubezpieczenia
	 * nieruchomości.
	 *
	 * ⛔ Decyduje o tym tabela `demo.property.apk.question.blocker`, a treść komunikatu też
	 * przychodzi z niej. Front nie zna ani pytania, ani odpowiedzi, która zamyka ścieżkę.
	 */
	readonly blocked: boolean;
	readonly blockMessage: string | null;
}
