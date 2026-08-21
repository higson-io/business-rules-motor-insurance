import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { PropertyStore } from '../../core/property-store';
import { ErrorBanner } from '../../shared/error-banner/error-banner';
import { Comparison } from './comparison/comparison';
import { EffectiveDate } from './effective-date/effective-date';
import { Questionnaire } from './questionnaire/questionnaire';
import { Recommendation } from './recommendation/recommendation';

/**
 * Demo majątkowe: ubezpieczenie domu i mieszkania, w którym analiza potrzeb klienta jest
 * konfiguracją, a nie formularzem.
 *
 * ⛔ NO ROUTER, konsekwentnie z resztą aplikacji (ND-7). Kroki są sekcjami jednej strony, więc
 * widz widzi naraz ankietę i to, co z niej wynika — a o to w tym demo chodzi.
 */
@Component({
	selector: 'app-property',
	imports: [Comparison, EffectiveDate, ErrorBanner, Questionnaire, Recommendation],
	templateUrl: './property.html',
	styleUrl: './property.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Property {
	private readonly store = inject(PropertyStore);

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
