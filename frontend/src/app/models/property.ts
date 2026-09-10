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

/**
 * What the answer IS — and, from that, how the control is drawn. One attribute, not two.
 *
 * ⛔ There used to be a separate `valueType`. Splitting "how to draw it" from "what it is" gave
 * four combinations where two made sense, and a mismatch did not fail — it quietly produced the
 * wrong number. Splitting `NUMBER` into `INTEGER` and `DECIMAL` made the second attribute
 * redundant, so it is gone from the wire as well.
 */
export type QuestionKind =
	| 'CHOICE'
	| 'MULTI'
	| 'INTEGER'
	| 'DECIMAL'
	| 'MONEY'
	| 'BOOLEAN'
	| 'TEXT';

/**
 * Gałąź drzewa, z której pochodzi pytanie — i zarazem krok, w którym się je zadaje.
 *
 * ⛔ To NIE jest dokumentacja: komponent filtruje po tej wartości (`questionnaire.ts`, wejście
 * `group`). `NEEDS` to siedem pytań analizy potrzeb, elementów drzewa domeny; `SUBJECT` to
 * dwanaście pól przedmiotu ubezpieczenia, które elementami nie są i idą z katalogu.
 */
/**
 * Gałąź drzewa, w której pytanie wisi — i zarazem krok, w którym się pokazuje.
 *
 * ⛔ To NIE jest sekcja. Sekcje zniknęły 4 września 2026 razem z całym poziomem grupowania:
 * pytania wiszą teraz płasko w dwóch kolekcjach pod produktem. `QUESTIONS` to analiza potrzeb,
 * `SUBJECT` to przedmiot ubezpieczenia i sytuacja — dane zbierane poza APK.
 */
export type QuestionSection = 'NEEDS' | 'SUBJECT';

export interface PropertyQuestionDto {
	readonly code: string;
	readonly text: string;
	readonly kind: QuestionKind;
	readonly section: QuestionSection;
	readonly position: number;
	/**
	 * Pola pytania ZŁOŻONEGO — grupa pyta o kilka rzeczy naraz.
	 *
	 * ⛔ Pusta lista znaczy pytanie zwykłe. Zagnieżdżenie robi konwerter na granicy drutu; po
	 * stronie Javy lista pytań zostaje PŁASKA, żeby wyszukanie po kodzie nie znało rodzica.
	 */
	readonly fields: readonly PropertyQuestionDto[];
	/**
	 * Czy o to pytanie pytamy TEGO klienta.
	 *
	 * ⛔ Rozstrzyga JEDNA reguła, kluczowana kodem pytania i odpowiedziami — bo zależność między
	 * pytaniami to zależność od odpowiedzi, a te są w kontekście. „Pomoc fachowca tylko przy
	 * wynajmie" jest wierszem tabeli, nie łańcuchem w kodzie.
	 */
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

export interface PropertyQuoteDto {
	readonly startDate: string;
	readonly deductible: number;
	/**
	 * Franszyza narzucona przez strefę powodziową — warunek JEDNEGO pokrycia, nie polisy.
	 *
	 * ⛔ `deductible` powyżej to franszyza POLISY, wybrana przez klienta w kroku 2. Ta jest
	 * narzucona regułą strefy i pokazuje się tylko wtedy, gdy wybrany pakiet niesie powódź.
	 */
	readonly floodDeductible: number | null;

	readonly questions: readonly PropertyQuestionDto[];

	readonly floodZone: string | null;
	readonly theftZone: string | null;
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
	 * Czy wariant wybrał CZŁOWIEK.
	 *
	 * ⛔ To nie to samo co `selectedVariant !== null`: serwer podstawia pod `selectedVariant`
	 * wariant rekomendowany, żeby ekran miał co pokazać, zanim klient cokolwiek kliknie.
	 */
	readonly variantChosen: boolean;
}
