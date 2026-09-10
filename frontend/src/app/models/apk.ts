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
	readonly requiresJustification: boolean;
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

/**
 * Napisy własne ankiety — wszystko, czego NIE podają reguły.
 *
 * ⛔ Nie mają wartości domyślnych i to jest celowe. Gdyby komponent miał domyślną polszczyznę,
 * ekran komunikacyjny — który jest po angielsku, bo ND-2 przypina jego etykiety, a treść tabeli
 * przychodzi z Higsona po angielsku — dostałby dwa języki naraz i nikt by tego nie zauważył do
 * pierwszej prezentacji. Gospodarz musi powiedzieć, w jakim języku mówi jego ekran.
 *
 * Treść pytań, pomoc, etykiety odpowiedzi i komunikaty o odmowie NIE są tutaj — te pochodzą
 * z reguł, razem z pytaniem.
 */
export interface ApkCopy {
	readonly sections: Readonly<Record<ApkSection, string>>;
	readonly yes: string;
	readonly no: string;
	/** Rek. 8.3 — poprzedza nazwę źródła, np. „Podpowiedziane:" / „Prefilled from:". */
	readonly prefilledFrom: string;
	readonly refuse: string;
	readonly unrefuse: string;
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

/** Potrzeba wyprowadzona z odpowiedzi (rek. 8.2.b). */
export interface ApkNeedDto {
	readonly coverCode: string;
	readonly text: string;
	readonly weight: number;
	/**
	 * Czy klient tę potrzebę ZGŁOSIŁ, czy wynika ona z jego sytuacji.
	 *
	 * ⛔ Rek. 8.3 KNF pozwala analizować także informacje, których klient nie podał jako potrzeby,
	 * ale każe mu powiedzieć, że tak zrobiliśmy. Bez tego rozróżnienia zdanie „pilna potrzeba
	 * ochrony przed kradzieżą" czyta się jak jego deklaracja, a jest naszym wnioskiem.
	 */
	readonly declared: boolean;
}


export interface ApkRecommendationDto {
	/**
	 * `null` albo puste w ochronie indywidualnej — tam propozycją jest ZESTAW umów, nie wariant
	 * z półki. To nie jest brak danych; ekran rekomendacji ma to uszanować nagłówkiem bez wariantu.
	 */
	readonly variantCode: string | null;
	readonly coveredNeeds: number;
	readonly totalNeeds: number;
	readonly missingCovers: readonly string[];
	/** `null`, gdy reżim nie wymaga pytania o budżet — wtedy nie ma czego z czym porównywać. */
	readonly budgetOutcome: string | null;
	/**
	 * Tańszy wariant mieszczący się w oczekiwanej składce — ALTERNATYWA obok propozycji, nie
	 * zamiast niej.
	 *
	 * ⛔ `variantCode` powyżej niesie wariant najlepiej dopasowany do potrzeb, także wtedy, gdy
	 * kosztuje więcej, niż klient podał. Oczekiwana składka jest wymaganiem klienta, a nie filtrem
	 * zakresu — ekran, który podmieniałby propozycję na tańszą, robiłby z niej filtr z powrotem.
	 */
	readonly alternativeVariantCode: string | null;
	readonly alternativePremium: number | null;
	readonly justification: readonly string[];
	readonly needs: readonly ApkNeedDto[];
}

/**
 * Napisy własne ekranu rekomendacji — nagłówki sekcji i zdania łączące, czyli wszystko, czego
 * NIE podają reguły. Ta sama zasada i ten sam powód co przy `ApkCopy`: bez domyślnych.
 *
 * ⛔ Treść merytoryczna — potrzeby, ostrzeżenia, uzasadnienie — tutaj NIE trafia. Te zdania
 * przychodzą z `demo.*.apk.*` i mają tam zostać; to one są dowodem, że dokument dla klienta jest
 * konfiguracją. Tutaj są wyłącznie napisy ramki ekranu.
 *
 * `{0}`, `{1}` — te same podstawienia co w tabelach powodów, dla jednego mechanizmu w całym demo.
 */
export interface ApkRecommendationCopy {
	/** `{0}` — nazwa wariantu. Używane, gdy propozycja MA wariant. */
	readonly proposal: string;
	/** Nagłówek, gdy wariantu nie ma (ochrona indywidualna: propozycją jest zestaw umów). */
	readonly proposalNoVariant: string;
	/** `{0}` pokryte, `{1}` wszystkie. */
	readonly coverage: string;
	readonly needsTitle: string;
	/**
	 * Dopisek przy potrzebie, której klient nie zgłosił — wynika z jego sytuacji.
	 *
	 * ⛔ Rek. 8.3 KNF: wolno analizować informacje, których klient nie podał jako potrzeby,
	 * ale trzeba mu powiedzieć, że tak zrobiliśmy. Puste znaczy „ta linia nie rozróżnia" —
	 * moto i życie dziś nie rozróżniają, bo ich potrzeby biorą się wyłącznie z ankiety.
	 */
	readonly needInferred?: string;
	/** `{0}` — lista nazw po przecinku. */
	readonly missing: string;
	readonly justificationTitle: string;
	readonly empty: string;

	/**
	 * Podpis nad składką proponowanego zakresu, np. „Składka miesięczna".
	 *
	 * ⛔ Składka NALEŻY do rekomendacji, a nie tylko do ekranu ceny. Rek. 8.2.d żąda doboru
	 * „z uwzględnieniem akceptowalnej dla klienta wartości składki" — propozycja, która nie mówi,
	 * ile kosztuje, jest w tej części niekompletna, a uzasadnienie i tak już cytuje kwoty
	 * („Składka 61 zł miesięcznie przekracza podany budżet 45 zł").
	 *
	 * ⚠️ To JEDNA liczba, dotycząca jednej propozycji — nie tabela porównawcza. Porównanie
	 * wariantów zostaje w kroku trzecim: ekran rekomendacji, który zaczyna od zestawienia cen,
	 * przestaje być analizą potrzeb i staje się tym, czemu rek. 8 miała zapobiec.
	 */
	readonly premiumLabel: string;
	/** Doprecyzowanie okresu pod kwotą, np. „za wybrany okres ochrony". */
	readonly premiumNote: string;
	/** Znacznik przy potrzebie, którą propozycja pokrywa. */
	readonly needCovered: string;
	/** Znacznik przy potrzebie, której propozycja nie pokrywa. */
	readonly needMissing: string;
}
