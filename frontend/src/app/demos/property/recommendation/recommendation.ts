import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { PropertyStore } from '../../../core/property-store';

/**
 * Rekomendacja: potrzeby, ostrzeżenia i uzasadnienie.
 *
 * ⛔ Wszystkie trzy bloki są sterowane danymi, nie datą. Pusta lista uzasadnienia nie renderuje
 * pustej ramki — w reżimie sprzed 2027 uzasadnienia po prostu nie ma i ekran ma to pokazywać
 * nieobecnością, a nie nagłówkiem nad niczym.
 */
@Component({
	selector: 'app-property-recommendation',
	templateUrl: './recommendation.html',
	styleUrl: './recommendation.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Recommendation {
	private readonly store = inject(PropertyStore);

	/** ⚠️ `hasValue()` first — a resource in the error state throws from `value()`. */
	private readonly loaded = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value() : null,
	);

	readonly ready = computed(() => this.loaded() !== null);
	readonly needs = computed(() => this.loaded()?.needs ?? []);
	readonly warnings = computed(() => this.loaded()?.warnings ?? []);
	readonly justification = computed(() => this.loaded()?.recommendation.justification ?? []);

	readonly recommendedName = computed(() => {
		const quote = this.loaded();
		if (quote === null) {
			return null;
		}
		const code = quote.recommendation.variantCode;
		return quote.variants.find((variant) => variant.code === code)?.name ?? code;
	});

	readonly coverage = computed(() => {
		const recommendation = this.loaded()?.recommendation;
		return recommendation ? `${recommendation.coveredNeeds} z ${recommendation.totalNeeds}` : null;
	});

	readonly missing = computed(() => {
		const quote = this.loaded();
		if (quote === null) {
			return [];
		}
		return quote.recommendation.missingCovers.map(
			(code) => quote.covers.find((cover) => cover.code === code)?.name ?? code,
		);
	});
}
