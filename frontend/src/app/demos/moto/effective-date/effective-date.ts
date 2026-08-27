import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatDatepicker, MatDatepickerInput, MatDatepickerToggle } from '@angular/material/datepicker';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';

import { QuoteStore } from '../../../core/quote-store';
import { VariantService } from '../../../services/variant-service';

/**
 * The policy date, and what it currently yields.
 *
 * ⛔ It lives ABOVE the step bar, not inside the first step, because it is context for the whole
 * quote rather than an answer to a question: moving it across 1 July 2027 changes the regime, the
 * question set and the recommendation at once. The same construction, in the same place, in all
 * three demos — a presenter switching demos should not have to look for it.
 *
 * ⛔ The threshold date is NOT known here and must never be. Which side of it a date falls on is
 * a region timeline in Higson; the screen only reports the name the server gave it.
 */
@Component({
	selector: 'app-moto-effective-date',
	imports: [
		MatDatepicker,
		MatDatepickerInput,
		MatDatepickerToggle,
		MatFormField,
		MatInput,
		MatLabel,
		MatSuffix,
	],
	templateUrl: './effective-date.html',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EffectiveDate {
	private readonly store = inject(QuoteStore);
	private readonly variantService = inject(VariantService);

	/** ⚠️ `hasValue()` first — a resource in the error state throws from `value()`. */
	private readonly loaded = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value() : null,
	);

	readonly regime = computed(() => this.loaded()?.apkRegime ?? null);

	readonly startDate = computed(() => {
		const value = this.loaded()?.policyStartDate;
		return value ? new Date(value) : null;
	});

	readonly visibleCount = computed(
		() => (this.loaded()?.apkQuestions ?? []).filter((question) => question.visible).length,
	);

	pickDate(value: Date | null): void {
		if (value === null) {
			return;
		}
		void this.commit(firstValueFrom(this.variantService.updateStartDate(value)));
	}

	private async commit(change: Promise<unknown>): Promise<void> {
		await change;
		this.store.recalculate();
	}
}
