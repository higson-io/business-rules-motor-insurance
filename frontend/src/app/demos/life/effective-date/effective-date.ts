import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatDatepicker, MatDatepickerInput, MatDatepickerToggle } from '@angular/material/datepicker';
import { MatFormField, MatHint, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';

import { LifeStore } from '../../../core/life-store';
import { LifeService } from '../../../services/life-service';

/** The day the region timeline switches from version 1 to version 2. */
const VERSION_2_FROM = new Date('2026-07-01');

/**
 * The date that selects the tariff version.
 *
 * This is not an ordinary form field. The backend feeds it to `HigsonEngine.setEffectiveDate`, so
 * the region timeline resolves against the policy start date instead of the server clock. Moving it
 * across `2026-07-01` switches every versioned table at once — rates, medical thresholds and the two
 * anti-selection factors — and makes a benefit appear that does not exist before that date.
 *
 * ⛔ It sits in the shell, not inside one path, because it governs both. A copy per path would let
 * the two screens disagree about which tariff they are quoting.
 */
@Component({
	selector: 'app-life-effective-date',
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
	private readonly store = inject(LifeStore);
	private readonly lifeService = inject(LifeService);

	/**
	 * ⚠️ `hasValue()` first. A resource in the error state throws from `value()`, so reading it
	 * directly would take the screen down on the very failure the banner exists to report.
	 */
	readonly startDate = computed(() => {
		const quote = this.store.quote;
		return quote.hasValue() ? new Date(quote.value().startDate) : null;
	});

	/** Which region version the chosen date lands in — the whole point of the control. */
	readonly tariffVersion = computed(() => {
		const date = this.startDate();
		if (date === null) {
			return null;
		}
		return date >= VERSION_2_FROM ? 'Taryfa 2026 H2' : 'Taryfa 2026 H1';
	});

	/**
	 * ⛔ Sends a `Date` OBJECT. `LifeApi.setStartDate` is `@RequestBody Date`; a string would arrive
	 * as `text/plain` and Jackson answers 415.
	 */
	pick(value: Date | null): void {
		if (value === null) {
			return;
		}
		void this.store.commit(firstValueFrom(this.lifeService.updateStartDate(value)));
	}
}
