import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { ApkRecommendationCopy } from '../../models/apk';
import { PropertyStore } from '../../core/property-store';
import { isBranchComplete } from '../../core/question-state';
import { ApkRecommendation } from '../../shared/apk/recommendation/recommendation';
import { ErrorBanner } from '../../shared/error-banner/error-banner';
import { Step, StepBar } from '../../shared/step-bar/step-bar';
import { Comparison } from './comparison/comparison';
import { Client } from './client/client';
import { Offer } from './offer/offer';
import { Questionnaire } from './questionnaire/questionnaire';

/**
 * Napisy ramki ekranu rekomendacji. Treść merytoryczna przychodzi z reguł — tu są tylko
 * nagłówki i zdania łączące (patrz `ApkRecommendationCopy`).
 */
const RECOMMENDATION_COPY: ApkRecommendationCopy = {
	proposal: 'Proponujemy wariant {0}',
	proposalNoVariant: 'Proponowany zakres ochrony',
	coverage: 'Pokrywa {0} z {1} Twoich potrzeb',
	needsTitle: 'Twoje potrzeby',
	needInferred: 'wynika z danych o nieruchomości, nie ze zgłoszenia',
	missing: 'Poza zakresem zostaje: {0}.',
	justificationTitle: 'Dlaczego ta propozycja',
	empty: 'Odpowiedz na pytania ankiety — propozycja pojawi się tutaj.',
	premiumLabel: 'Składka',
	premiumNote: 'za wybrany okres ochrony',
	needCovered: '✓',
	needMissing: '—',
};

/**
 * Demo majątkowe: ubezpieczenie domu i mieszkania, w którym analiza potrzeb klienta jest
 * konfiguracją, a nie formularzem.
 *
 * ⛔ NO ROUTER, konsekwentnie z resztą aplikacji (ND-7). Krok jest sygnałem, nie adresem — więc
 * wstecz przeglądarki go nie cofa, dokładnie tak samo jak przy wyborze demo.
 *
 * ⛔ Cztery kroki, w kolejności analizy, a nie w kolejności ekranów: najpierw pytamy o potrzeby,
 * potem zbieramy przedmiot ubezpieczenia, dalej pokazujemy, co z odpowiedzi wynika — rekomendację
 * RAZEM ze składką, bo to jedna decyzja (rek. 8.2.d), a nie dwa ekrany — a na końcu ofertę. To
 * jest odwrotność kolejności, w jakiej robi się to zwykle w kalkulatorach, i o tę odwrotność
 * chodzi w rek. 8 KNF. Ekran, który zaczyna od ceny, przestaje być analizą potrzeb.
 */
@Component({
	selector: 'app-property',
	imports: [
		ApkRecommendation,
		Client,
		Comparison,
		ErrorBanner,
		Offer,
		Questionnaire,
		StepBar,
	],
	templateUrl: './property.html',
	styleUrl: './property.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Property {
	private readonly store = inject(PropertyStore);

	/**
	 * ⛔ Wejście do dema ZACZYNA NOWY PROCES. Komponent powstaje przy każdym otwarciu demo
	 * z ekranu wyboru, a sesja HTTP żyje dłużej — bez tego prowadzący wracał do wyboru dema,
	 * wchodził ponownie i zastawał ankietę poprzedniego klienta razem z jego ofertą.
	 *
	 * ⚠️ „Wszystkie dema" → kafelek JEST tu gestem nowego procesu i nie ma osobnego przycisku.
	 * Dokładanie drugiego sposobu na to samo kazałoby zgadywać, czym się różnią.
	 */
	constructor() {
		void this.store.startNewProcess();
	}

	readonly copy = RECOMMENDATION_COPY;

	readonly step = signal<string>('apk');

	/**
	 * ⛔ Kolejność jest argumentem: najpierw POTRZEBY, potem PRZEDMIOT, dopiero na końcu
	 * propozycja z ceną. To jest odwrotność kolejności, w jakiej robi się to w kalkulatorach —
	 * te zaczynają od metrażu i kodu pocztowego, czyli od rzeczy, które prowadzą wprost do
	 * składki, a o potrzebach pytają na końcu albo wcale. O tę odwrotność chodzi w rek. 8 KNF.
	 *
	 * ⛔ REKOMENDACJA I SKŁADKA TO JEDEN KROK. Rozdzielone kazały klientowi pamiętać propozycję
	 * przez przejście dalej, żeby zestawić ją z ceną — a rek. 8.2.d mówi o doborze
	 * „z uwzględnieniem akceptowalnej wartości składki", czyli o jednej decyzji.
	 *
	 * ⛔ Pasek pokazuje pozycję ORAZ dostępność — ale bramkuje wyłącznie ruch W PRZÓD. Wstecz
	 * wolno zawsze, bo prowadzący musi móc wrócić do jednej odpowiedzi i pokazać, jak przestawia
	 * się propozycja. Kiedy który krok się otwiera — patrz `steps` niżej.
	 *
	 * ⛔ Czwarty krok TYLKO w demie majątkowym. Domyka proces ofertą i polisą, czyli pokazuje, że
	 * reguły nie kończą się na składce — mówią też, ile oferta jest ważna i co dokument musi
	 * zawierać. Pozostałe dwa dema zatrzymują się na składce świadomie: to samo domknięcie
	 * powtórzone trzy razy nie dodałoby ani jednej nowej rzeczy do pokazania.
	 */
	private readonly stepLabels: readonly Step[] = [
		{ key: 'apk', label: 'Analiza potrzeb' },
		{ key: 'subject', label: 'Przedmiot ubezpieczenia' },
		{ key: 'proposal', label: 'Rekomendacja i składka' },
		{ key: 'offer', label: 'Oferta' },
	];

	/**
	 * Czy analiza potrzeb jest domknięta.
	 *
	 * ⛔ „Wszystkie widoczne odsłonięte i każde obowiązkowe udzielone" — patrz `isBranchComplete`.
	 * Nieudzielone pytanie obowiązkowe łapie się tu przez łańcuch: dopóki nie ma na nie odpowiedzi,
	 * następne nie jest odsłonięte, więc warunek „wszystkie odsłonięte" jest fałszywy. Ekran nie
	 * musi więc znać łańcucha ani go powtarzać.
	 */
	readonly apkComplete = computed(() =>
		isBranchComplete(
			(this.loaded()?.questions ?? []).filter((q) => q.section === 'NEEDS'),
		),
	);

	/**
	 * Czy przedmiot ubezpieczenia jest wypełniony.
	 *
	 * ⚠️ Pola przedmiotu pokazują się wszystkie naraz, ale nie wszystkie zawsze: dwa mają warunek
	 * (piętro nie dotyczy domu, materiał ścian nie dotyczy mieszkania) i wtedy nie są odsłonięte.
	 * Nieodsłonięte pole nie może blokować kroku, więc liczą się tylko odsłonięte obowiązkowe.
	 * Większość przychodzi wypełniona z poprzedniej polisy, co jest treścią tego kroku.
	 */
	readonly subjectComplete = computed(() =>
		isBranchComplete(
			(this.loaded()?.questions ?? []).filter((q) => q.section === 'SUBJECT' && q.visible),
		),
	);

	/**
	 * Czy klient wybrał wariant.
	 *
	 * ⛔ To jest warunek wejścia na OFERTĘ i zarazem odpowiedź na uwagę z testów: „bez wyklikania,
	 * że chcę tę polisę, już się pokazuje, że jest wystawiona". Oferta powstaje przy wejściu na
	 * krok czwarty, więc dopóki nikt niczego nie wybrał, nie ma czego ofertować.
	 *
	 * ⚠️ Czyta `variantChosen`, NIE `selectedVariant !== null`. Serwer podstawia pod
	 * `selectedVariant` wariant rekomendowany, żeby tabela miała co podświetlić — więc to pole
	 * nigdy nie jest puste i bramka na nim otwierała się od razu. Zmierzone: po samej ankiecie
	 * `selectedVariant` = COMPLETE, choć nikt niczego nie kliknął.
	 */
	readonly variantChosen = computed(() => this.loaded()?.variantChosen === true);

	/**
	 * Kroki z policzoną dostępnością.
	 *
	 * ⛔ **BRAMKUJEMY WYŁĄCZNIE RUCH W PRZÓD.** Krok już domknięty zostaje klikalny na zawsze —
	 * bez tego znika rzecz, dla której to demo istnieje: cofnąć się do jednej odpowiedzi
	 * i pokazać, jak przestawia się propozycja.
	 *
	 * ⚠️ Do 2026-09-09 wszystkie kroki były osiągalne zawsze i było to uzasadnione. Zmienione na
	 * wyraźne życzenie właściciela: skok w przód pokazywał gotową ofertę, zanim klient cokolwiek
	 * wybrał, i to czytało się jak „polisa już wystawiona".
	 */
	readonly steps = computed<readonly Step[]>(() => {
		const open = [
			true,
			this.apkComplete(),
			this.apkComplete() && this.subjectComplete(),
			this.apkComplete() && this.subjectComplete() && this.variantChosen(),
		];
		const current = this.stepLabels.findIndex((step) => step.key === this.step());
		return this.stepLabels.map((step, index) => ({
			...step,
			// Krok już odwiedzony albo bieżący zostaje otwarty, choćby warunek przestał być
			// spełniony — inaczej zmiana odpowiedzi wstecz zatrzaskiwałaby klienta na miejscu.
			locked: !open[index] && index > current,
		}));
	});

	/**
	 * Składka proponowanego wariantu — rek. 8.2.d, patrz `ApkRecommendationCopy.premiumLabel`.
	 *
	 * ⚠️ `proposedPremium`, nie `premium`: `premium(value)` to metoda formatująca, używana przez
	 * podsumowanie po prawej. Dwie różne rzeczy pod jedną nazwą to zaproszenie do pomyłki.
	 */
	readonly proposedPremium = computed(() => {
		const quote = this.loaded();
		const code = quote?.recommendation.variantCode;
		const variant = code ? quote?.variants.find((v) => v.code === code) : undefined;
		return variant?.annualPremium == null ? null : this.plnFormat.format(variant.annualPremium) + ' zł';
	});

	goTo(step: string): void {
		this.step.set(step);
	}

	/** ⚠️ `hasValue()` first — a resource in the error state throws from `value()`. */
	private readonly loaded = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value() : null,
	);

	readonly ready = computed(() => this.loaded() !== null);

	/** ⚠️ `error()`, nie `value()`. Zasób w stanie błędu rzuca z `value()` w Angularze 22. */
	readonly loadError = computed(() => this.store.quote.error());

	readonly sumBuilding = computed(() => this.money(this.loaded()?.sumBuilding));
	readonly sumFixtures = computed(() => this.money(this.loaded()?.sumFixtures));
	readonly sumContents = computed(() => this.money(this.loaded()?.sumContents));
	readonly floodZone = computed(() => this.loaded()?.floodZone ?? null);

	readonly recommendation = computed(() => this.loaded()?.recommendation ?? null);

	/** Nazwa i składka tańszej alternatywy, gdy propozycja nie mieści się w oczekiwanej składce. */
	readonly alternative = computed(() => {
		const quote = this.loaded();
		const code = quote?.recommendation.alternativeVariantCode;
		if (!quote || !code) {
			return null;
		}
		const variant = quote.variants.find((item) => item.code === code);
		return {
			name: variant?.name ?? code,
			premium: variant?.annualPremium == null
				? null
				: this.plnFormat.format(variant.annualPremium) + ' zł',
		};
	});

	/** Nazwa wariantu, nie kod — komponent rekomendacji nie wie, gdzie w tym modelu leżą nazwy. */
	readonly recommendedName = computed(() => {
		const quote = this.loaded();
		if (quote === null) {
			return null;
		}
		const code = quote.recommendation.variantCode;
		return quote.variants.find((variant) => variant.code === code)?.name ?? code;
	});

	/**
	 * Wariant proponowany w całości — kafelek w podsumowaniu pokazuje jego nazwę I opis.
	 *
	 * ⚠️ Osobno od `recommendedName()`: tamto oddaje samą nazwę komponentowi rekomendacji, który
	 * o wariantach nic nie wie i wiedzieć nie ma. Tutaj potrzebny jest cały obiekt, bo `description`
	 * leżało w modelu nieużywane, dopóki makieta nie znalazła mu miejsca.
	 */
	readonly recommendedVariant = computed(() => {
		const quote = this.loaded();
		if (quote === null) {
			return null;
		}
		const code = quote.recommendation.variantCode;
		return quote.variants.find((variant) => variant.code === code) ?? null;
	});

	readonly missingNames = computed(() => {
		const quote = this.loaded();
		if (quote === null) {
			return [];
		}
		return quote.recommendation.missingCovers.map(
			(code) => quote.covers.find((cover) => cover.code === code)?.name ?? code,
		);
	});

	readonly chosen = computed(() => {
		const quote = this.loaded();
		if (quote === null) {
			return null;
		}
		const code = quote.selectedVariant ?? quote.recommendation.variantCode;
		return quote.variants.find((variant) => variant.code === code) ?? null;
	});

	/**
	 * Czy klient odszedł od rekomendacji.
	 *
	 * ⛔ Odkąd propozycja i porównanie stoją na JEDNYM ekranie, cena wybranego wariantu pokazana
	 * bezwarunkowo powtarzała liczbę z nagłówka propozycji — ta sama kwota z tym samym podpisem,
	 * dwadzieścia centymetrów od siebie, czyta się jak usterka. Podsumowanie odzywa się więc
	 * dopiero wtedy, gdy ma co powiedzieć: gdy wybór różni się od propozycji.
	 */
	readonly choiceDiffers = computed(() => {
		const quote = this.loaded();
		return quote !== null
			&& quote.selectedVariant !== null
			&& quote.selectedVariant !== quote.recommendation.variantCode;
	});

	// ── Formatowanie polskie ────────────────────────────────────────────────────────────────
	// Przez `Intl`, a nie przez zarejestrowaną locale: aplikacja zostaje na en-US, bo goldeny
	// motoryzacyjne przypinają tamto formatowanie. Ta sama konstrukcja co w demo życiowym.
	//
	// ⚠️ Powód jest prezentacyjny, nie kosmetyczny: „474000 zł" na rzutniku trzeba przeliterować,
	// a to jest liczba, którą reguły policzyły z metrażu i o którą w tej szpalcie chodzi.
	private readonly wholeFormat = new Intl.NumberFormat('pl-PL', { maximumFractionDigits: 0 });
	private readonly plnFormat = new Intl.NumberFormat('pl-PL', {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	});

	private money(value: number | null | undefined): string | null {
		return value === null || value === undefined ? null : this.wholeFormat.format(value);
	}

	premium(value: number | null): string {
		return this.plnFormat.format(value ?? 0);
	}
}
