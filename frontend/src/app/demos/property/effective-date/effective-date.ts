import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal } from '@angular/core';
import { MatDatepicker, MatDatepickerInput, MatDatepickerToggle } from '@angular/material/datepicker';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';

import { PropertyStore } from '../../../core/property-store';
import { PropertyService } from '../../../services/property-service';

/** What this policy date currently yields. Every number here moves when the regime switches. */
interface Reading {
	readonly regime: string | null;
	readonly legalBasis: string | null;
	readonly questions: number;
	readonly sections: number;
	readonly justified: boolean;
}

@Component({
	selector: 'app-property-effective-date',
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

	private readonly reading = computed<Reading | null>(() => {
		const quote = this.loaded();
		if (quote === null) {
			return null;
		}
		const visible = quote.questions.filter((question) => question.visible);
		return {
			regime: quote.regime.name,
			legalBasis: quote.regime.legalBasis,
			questions: visible.length,
			sections: new Set(visible.map((question) => question.section)).size,
			justified: quote.recommendation.justification.length > 0,
		};
	});

	/**
	 * The current reading, and the one before it — but only across a regime change.
	 *
	 * ⛔ The threshold date is NOT known here and must never be. Which side of 1 July 2027 a date
	 * falls on is a region timeline in Higson; hard-coding it would put a second copy of the rule
	 * in the browser, and the copy is the one that goes stale. The change is detected by comparing
	 * two server payloads — if the regime the server named is different, something happened.
	 *
	 * `linkedSignal` rather than an effect writing to state: the previous reading is derived from
	 * the source, so it cannot drift out of step with what is on screen.
	 */
	private readonly readings = linkedSignal<Reading | null, { now: Reading; before: Reading | null } | null>({
		source: this.reading,
		computation: (now, previous) => {
			if (now === null) {
				return null;
			}
			const last = previous?.value?.now ?? null;
			return { now, before: last !== null && last.regime !== now.regime ? last : null };
		},
	});

	readonly now = computed(() => this.readings()?.now ?? null);
	readonly before = computed(() => this.readings()?.before ?? null);

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
