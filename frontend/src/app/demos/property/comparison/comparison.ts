import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { PropertyStore } from '../../../core/property-store';
import { PropertyService } from '../../../services/property-service';

/**
 * Macierz różnic między wariantami — rekomendacja 9.3 wprost: „gdy kilka wariantów odpowiada
 * potrzebom, w sposób wyraźny wskazuje na występujące między nimi różnice".
 *
 * ⛔ Wariant niedostępny zostaje w tabeli i niesie powód. Usunięcie go z listy byłoby dokładnie tym
 * cichym zniknięciem, którego decyzja D-3 zabrania.
 */
@Component({
	selector: 'app-property-comparison',
	templateUrl: './comparison.html',
	styleUrl: './comparison.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Comparison {
	private readonly store = inject(PropertyStore);
	private readonly propertyService = inject(PropertyService);

	/** ⚠️ `hasValue()` first — a resource in the error state throws from `value()`. */
	private readonly loaded = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value() : null,
	);

	readonly ready = computed(() => this.loaded() !== null);
	readonly covers = computed(() => this.loaded()?.covers ?? []);
	readonly variants = computed(() => this.loaded()?.variants ?? []);
	readonly selected = computed(() => this.loaded()?.selectedVariant ?? null);
	readonly recommended = computed(() => this.loaded()?.recommendation.variantCode ?? null);

	includes(variantCode: string, coverCode: string): boolean {
		const variant = this.variants().find((candidate) => candidate.code === variantCode);
		return variant ? variant.covers.includes(coverCode) : false;
	}

	choose(variantCode: string): void {
		void this.store.commit(firstValueFrom(this.propertyService.updateVariant(variantCode)));
	}

	// ⚠️ To samo formatowanie co w prawej szpalcie. Ta sama liczba pokazana raz jako „891.5", a raz
	// jako „891,50" czyta się jak dwie różne ceny.
	private readonly plnFormat = new Intl.NumberFormat('pl-PL', {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	});

	premium(value: number | null): string {
		return this.plnFormat.format(value ?? 0);
	}
}
