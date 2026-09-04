import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { ApkRecommendationCopy } from '../../models/apk';
import { PropertyStore } from '../../core/property-store';
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
	needsTitle: 'Potrzeby wynikające z odpowiedzi',
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
 * ⛔ Trzy kroki, w kolejności analizy, a nie w kolejności ekranów: najpierw pytamy (APK), potem
 * pokazujemy, co z odpowiedzi wynika (rekomendacja), a cena jest na końcu. To jest odwrotność
 * kolejności, w jakiej robi się to zwykle w kalkulatorach — i o tę odwrotność chodzi w rek. 8
 * KNF. Ekran, który zaczyna od ceny, przestaje być analizą potrzeb.
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
	 * ⛔ Pasek pokazuje POZYCJĘ, nie uprawnienie — każdy krok jest osiągalny zawsze, bo prowadzący
	 * musi móc wrócić do jednej odpowiedzi i pokazać, jak przestawia się propozycja.
	 *
	 * ⛔ Czwarty krok TYLKO w demie majątkowym. Domyka proces ofertą i polisą, czyli pokazuje, że
	 * reguły nie kończą się na składce — mówią też, ile oferta jest ważna i co dokument musi
	 * zawierać. Pozostałe dwa dema zatrzymują się na składce świadomie: to samo domknięcie
	 * powtórzone trzy razy nie dodałoby ani jednej nowej rzeczy do pokazania.
	 */
	readonly steps: readonly Step[] = [
		{ key: 'apk', label: 'Analiza potrzeb' },
		{ key: 'subject', label: 'Przedmiot ubezpieczenia' },
		{ key: 'proposal', label: 'Rekomendacja i składka' },
		{ key: 'offer', label: 'Oferta' },
	];

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

	/**
	 * Bramka ankiety — odpowiedź, po której nie ma czego dalej analizować.
	 *
	 * ⛔ Front nie wie, KTÓRE to pytanie ani KTÓRA odpowiedź. Pyta serwer o dwie rzeczy:
	 * czy zablokowane i co powiedzieć — jedno i drugie pochodzi z tabeli
	 * `demo.property.apk.question.blocker`.
	 *
	 * ⚠️ Blokada dotyczy PRZEJŚCIA DALEJ, nie ankiety. Cofnięcie odpowiedzi ma natychmiast
	 * odblokować ekran, więc pytania zostają aktywne, a pasek kroków — klikalny wstecz.
	 */
	readonly blocked = computed(() => this.loaded()?.blocked ?? false);
	readonly blockMessage = computed(() => this.loaded()?.blockMessage ?? null);

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
