import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatDatepicker, MatDatepickerInput, MatDatepickerToggle } from '@angular/material/datepicker';
import { MatFormField, MatHint, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';

import { PropertyStore } from '../../../core/property-store';
import { PropertyService } from '../../../services/property-service';

/**
 * Data początku ochrony — kontrolka, która przełącza reżim APK.
 *
 * ⛔ Nie ma tu daty 1 lipca 2027 i nie powinno jej być. Front nie wie, że próg istnieje: podpis pod
 * kontrolką to `regime.name` i `regime.legalBasis` przysłane z serwera, gdzie o wszystkim decyduje
 * wersja regionu. Zaszyta tu data byłaby drugą kopią reguły — i pierwszą, która by się rozjechała.
 */
@Component({
	selector: 'app-property-effective-date',
	imports: [
		MatDatepicker,
		MatDatepickerInput,
		MatDatepickerToggle,
		MatFormField,
		MatHint,
		MatInput,
		MatLabel,
		MatSuffix,
	],
	templateUrl: './effective-date.html',
	styleUrl: './effective-date.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EffectiveDate {
	private readonly store = inject(PropertyStore);
	private readonly propertyService = inject(PropertyService);

	/** ⚠️ `hasValue()` first — a resource in the error state throws from `value()`. */
	private readonly loaded = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value() : null,
	);

	readonly startDate = computed(() => {
		const value = this.loaded()?.startDate;
		return value ? new Date(value) : null;
	});

	readonly regimeName = computed(() => this.loaded()?.regime.name ?? null);
	readonly legalBasis = computed(() => this.loaded()?.regime.legalBasis ?? null);

	/**
	 * ⛔ Sends a `Date` OBJECT. `PropertyApi.setStartDate` is `@RequestBody Date`; a string would
	 * arrive as `text/plain` and Jackson answers 415.
	 */
	pick(value: Date | null): void {
		if (value === null) {
			return;
		}
		void this.store.commit(firstValueFrom(this.propertyService.updateStartDate(value)));
	}
}
