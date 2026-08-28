import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { ApkRecommendationCopy } from '../../models/apk';
import { PropertyStore } from '../../core/property-store';
import { ApkRecommendation } from '../../shared/apk/recommendation/recommendation';
import { ErrorBanner } from '../../shared/error-banner/error-banner';
import { Step, StepBar } from '../../shared/step-bar/step-bar';
import { Comparison } from './comparison/comparison';
import { EffectiveDate } from './effective-date/effective-date';
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
	warningsTitle: 'Zanim zdecydujesz',
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
	imports: [ApkRecommendation, Comparison, EffectiveDate, ErrorBanner, Questionnaire, StepBar],
	templateUrl: './property.html',
	styleUrl: './property.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Property {
	private readonly store = inject(PropertyStore);

	readonly copy = RECOMMENDATION_COPY;

	readonly step = signal<string>('apk');

	/**
	 * ⛔ Kolejność jest argumentem: najpierw pytamy, potem pokazujemy, co z odpowiedzi wynika,
	 * a cena jest na końcu. Pasek pokazuje POZYCJĘ, nie uprawnienie — każdy krok jest osiągalny
	 * zawsze, bo prowadzący musi móc wrócić do jednej odpowiedzi i pokazać, jak przestawia się
	 * propozycja.
	 */
	readonly steps: readonly Step[] = [
		{ key: 'apk', label: 'Analiza potrzeb' },
		{ key: 'reco', label: 'Rekomendacja' },
		{ key: 'price', label: 'Składka' },
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
