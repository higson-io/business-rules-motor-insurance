import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatDatepicker, MatDatepickerInput, MatDatepickerToggle } from '@angular/material/datepicker';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';

import { QuoteStore } from '../../../core/quote-store';
import { ApkAnswerChange, ApkCopy, ApkRefusalChange } from '../../../models/apk';
import { VariantService } from '../../../services/variant-service';
import { ApkQuestionnaire } from '../../../shared/apk/questionnaire/questionnaire';

/**
 * Napisy własne ankiety — po angielsku, bo cały ekran komunikacyjny jest po angielsku.
 *
 * ⛔ ND-2 przypina angielskie etykiety tego demo jako świadomą decyzję produktową, a treść tabeli
 * porównawczej przychodzi z Higsona po angielsku. Polska sekcja nad angielską tabelą dawała ekran
 * mówiący dwoma językami naraz. Demo życiowe i majątkowe zostają po polsku i mają własne napisy —
 * język jest cechą ekranu, nie komponentu.
 */
const COPY: ApkCopy = {
	sections: {
		SUBJECT: 'What is being insured',
		SITUATION: 'Your situation',
		NEEDS: 'What you need',
		EXCLUSIONS: 'What could limit your cover',
	},
	yes: 'Yes',
	no: 'No',
	prefilledFrom: 'Prefilled from:',
	refuse: 'Prefer not to say',
	unrefuse: "I'll answer",
};

/**
 * Analiza potrzeb klienta na demie komunikacyjnym.
 *
 * ⛔ Ten komponent nie zna ani jednego pytania — jest gospodarzem wspólnej ankiety
 * `<app-apk-questionnaire>` i jedynym miejscem, które wie, że odpowiedź leci do `QuoteApi`.
 * Dokładnie tyle kodu kosztuje dołożenie APK do kolejnej linii produktowej.
 *
 * ⛔ Analiza potrzeb dotyczy AC, NNW i assistance, a nie OC. Zakres OC jest ustawowy i nie ma
 * w nim czego dobierać — pytanie o niego byłoby pytaniem bez konsekwencji.
 */
@Component({
	selector: 'app-moto-apk',
	imports: [
		ApkQuestionnaire,
		MatDatepicker,
		MatDatepickerInput,
		MatDatepickerToggle,
		MatFormField,
		MatInput,
		MatLabel,
		MatSuffix,
	],
	templateUrl: './moto-apk.html',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MotoApk {
	private readonly store = inject(QuoteStore);
	private readonly variantService = inject(VariantService);

	/** ⚠️ `hasValue()` najpierw — zasób w stanie błędu rzuca z `value()`. */
	private readonly loaded = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value() : null,
	);

	readonly copy = COPY;

	readonly ready = computed(() => this.loaded() !== null);
	readonly questions = computed(() => this.loaded()?.apkQuestions ?? []);
	readonly regime = computed(() => this.loaded()?.apkRegime ?? null);

	readonly startDate = computed(() => {
		const value = this.loaded()?.policyStartDate;
		return value ? new Date(value) : null;
	});

	readonly visibleCount = computed(() => this.questions().filter((q) => q.visible).length);

	pickDate(value: Date | null): void {
		if (value === null) {
			return;
		}
		void this.commit(firstValueFrom(this.variantService.updateStartDate(value)));
	}

	onAnswered(change: ApkAnswerChange): void {
		void this.commit(
			firstValueFrom(this.variantService.updateApkAnswer(change.code, change.value)),
		);
	}

	onRefusalToggled(change: ApkRefusalChange): void {
		void this.commit(
			firstValueFrom(this.variantService.updateApkRefused(change.code, change.refused)),
		);
	}

	/** Wyślij zmianę, potem przelicz — kolejność ma znaczenie, tak jak w pozostałych demach. */
	private async commit(change: Promise<unknown>): Promise<void> {
		await change;
		this.store.recalculate();
	}
}
