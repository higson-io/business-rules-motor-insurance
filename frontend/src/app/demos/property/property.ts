import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { PropertyStore } from '../../core/property-store';
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
	imports: [Comparison, EffectiveDate, Questionnaire, Recommendation],
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

	readonly sumBuilding = computed(() => this.loaded()?.sumBuilding ?? null);
	readonly sumFixtures = computed(() => this.loaded()?.sumFixtures ?? null);
	readonly sumContents = computed(() => this.loaded()?.sumContents ?? null);
	readonly floodZone = computed(() => this.loaded()?.floodZone ?? null);

	readonly chosen = computed(() => {
		const quote = this.loaded();
		if (quote === null) {
			return null;
		}
		const code = quote.selectedVariant ?? quote.recommendation.variantCode;
		return quote.variants.find((variant) => variant.code === code) ?? null;
	});
}
