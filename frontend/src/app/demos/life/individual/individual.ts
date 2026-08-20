import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';

import { LifeStore } from '../../../core/life-store';
import { LifeRiderDto } from '../../../models/life';
import { LifeService } from '../../../services/life-service';

/** The four occupational risk classes, in the wording underwriting actually uses. */
const OCCUPATIONS: readonly { readonly value: number; readonly label: string }[] = [
	{ value: 1, label: 'I — praca biurowa' },
	{ value: 2, label: 'II — praca w terenie, handel, usługi' },
	{ value: 3, label: 'III — praca fizyczna, transport' },
	{ value: 4, label: 'IV — budowy, wysokości, maszyny' },
];

/** Entry ages the product is written for — the tariff has no bands outside this. */
const AGE_RANGE = { min: 18, max: 70 } as const;

/**
 * The individual path: a protection policy you assemble.
 *
 * ⛔ Deliberately NOT the motor demo's shape. That screen compares three ready-made tiers; a Polish
 * individual life policy is not sold that way — you take a main contract and hang riders off it, and
 * the premium accumulates as you do.
 *
 * ⛔ NO LOCAL PRICING. Every figure on this screen comes from Higson through `LifeStore`. The
 * component holds no tariff, no factor and no threshold — it reports a change and re-reads the
 * quote. What used to be `pricing/life-tariff.ts` is now `demo.life.individual.calculateQuote`.
 *
 * ⛔ No gender. Since Test-Achats took effect in December 2012 an EU insurer may not price on sex,
 * so asking for it would either change nothing or demonstrate something unlawful.
 */
@Component({
	selector: 'app-life-individual',
	imports: [
		MatButtonToggle,
		MatButtonToggleGroup,
		MatCheckbox,
		MatFormField,
		MatInput,
		MatLabel,
		MatOption,
		MatSelect,
		MatSuffix,
	],
	templateUrl: './individual.html',
	styleUrl: './individual.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Individual {
	private readonly store = inject(LifeStore);
	private readonly lifeService = inject(LifeService);

	readonly ageRange = AGE_RANGE;
	readonly occupations = OCCUPATIONS;

	/**
	 * ⚠️ `hasValue()` before `value()`. A resource in the error state THROWS `ResourceValueError`
	 * from `value()` in Angular 22 — `value()?.x ?? y` would not degrade, it would take the screen
	 * down on exactly the failure the banner is for.
	 */
	private readonly loaded = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value() : null,
	);

	readonly ready = computed(() => this.loaded() !== null);

	readonly age = computed(() => this.loaded()?.entryAge ?? AGE_RANGE.min);
	readonly smoker = computed(() => this.loaded()?.smoker ?? false);
	readonly heightCm = computed(() => this.loaded()?.heightCm ?? 0);
	readonly weightKg = computed(() => this.loaded()?.weightKg ?? 0);
	readonly occupationClass = computed(() => this.loaded()?.occupationClass ?? 1);
	readonly mainSum = computed(() => this.loaded()?.mainSum ?? 0);
	readonly bmi = computed(() => this.loaded()?.bmi ?? 0);

	readonly riders = computed<readonly LifeRiderDto[]>(() => this.loaded()?.riders ?? []);

	/** Sums offered for the main contract come from the rules, not from a constant here. */
	readonly mainSums = computed<readonly number[]>(() => [
		50_000, 100_000, 150_000, 200_000, 300_000, 500_000, 750_000, 1_000_000,
	]);

	readonly premiumMonthly = computed(() => this.loaded()?.premiumMonthly ?? 0);
	readonly instalment = computed(() => this.loaded()?.instalment ?? null);
	readonly premiumAnnual = computed(() => this.loaded()?.premiumAnnual ?? null);

	readonly decision = computed(() => this.loaded()?.decision ?? null);

	/**
	 * Whether the price can be bought. Read it, never infer it from the premium being present —
	 * a REFER quote carries a full premium and is still not binding (D-1).
	 */
	readonly binding = computed(() => this.loaded()?.binding ?? false);
	readonly message = computed(() => this.loaded()?.message ?? null);
	readonly medicalRequired = computed(() => this.loaded()?.medicalRequired ?? false);

	/** The wording a Polish medical questionnaire uses for the band, shown beside the figure. */
	readonly bmiLabel = computed(() => {
		const bmi = this.bmi();
		if (bmi < 18.5) {
			return 'niedowaga';
		}
		if (bmi < 25) {
			return 'norma';
		}
		if (bmi < 30) {
			return 'nadwaga';
		}
		return 'otyłość';
	});

	/** Each selected line's share of the monthly premium — the stacked bar above the total. */
	readonly shares = computed(() => {
		const total = this.premiumMonthly();
		if (total <= 0) {
			return [];
		}
		return this.riders()
			.filter((rider) => rider.selected && rider.premium !== null)
			.map((rider) => ({ code: rider.code, share: (rider.premium ?? 0) / total }));
	});

	// ── Interaction ───────────────────────────────────────────────────────────────────────────
	//
	// Every setter is the same two steps: send the single field, then re-read the quote. The
	// component never computes a premium, so it cannot drift from the rules.

	setAge(value: number): void {
		if (!Number.isFinite(value)) {
			return;
		}
		const clamped = Math.min(AGE_RANGE.max, Math.max(AGE_RANGE.min, Math.round(value)));
		void this.store.commit(firstValueFrom(this.lifeService.updateAge(clamped)));
	}

	setSmoker(value: boolean): void {
		void this.store.commit(firstValueFrom(this.lifeService.updateSmoker(value)));
	}

	setHeight(value: number): void {
		if (Number.isFinite(value)) {
			void this.store.commit(firstValueFrom(this.lifeService.updateHeight(Math.round(value))));
		}
	}

	setWeight(value: number): void {
		if (Number.isFinite(value)) {
			void this.store.commit(firstValueFrom(this.lifeService.updateWeight(Math.round(value))));
		}
	}

	setOccupationClass(value: number): void {
		void this.store.commit(firstValueFrom(this.lifeService.updateOccupationClass(value)));
	}

	setMainSum(value: number): void {
		void this.store.commit(firstValueFrom(this.lifeService.updateMainSum(value)));
	}

	toggle(rider: LifeRiderDto, on: boolean): void {
		void this.store.commit(
			firstValueFrom(this.lifeService.updateRiderSelected(rider.code, on)),
		);
	}

	setAmount(rider: LifeRiderDto, amount: number): void {
		void this.store.commit(firstValueFrom(this.lifeService.updateRiderSum(rider.code, amount)));
	}

	amountOf(rider: LifeRiderDto): number {
		return rider.sum ?? rider.defaultSum ?? 0;
	}

	// ── Polish number formatting ──────────────────────────────────────────────────────────────
	//
	// ⛔ NOT `DecimalPipe`, and NOT a registered locale. `LOCALE_ID` stays at `en-US`
	// application-wide because the motor demo's goldens pin `1,234.50`. This screen formats through
	// `Intl` directly — one demo's locale, applied where it belongs and nowhere else.
	private readonly plnFormat = new Intl.NumberFormat('pl-PL', {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	});
	private readonly wholeFormat = new Intl.NumberFormat('pl-PL', { maximumFractionDigits: 0 });
	private readonly bmiFormat = new Intl.NumberFormat('pl-PL', {
		minimumFractionDigits: 1,
		maximumFractionDigits: 1,
	});

	/** `75,00` — grosze always shown, comma decimal, as a Polish premium is written. */
	money(value: number | null): string {
		return this.plnFormat.format(value ?? 0);
	}

	/** `300 000` — a sum insured carries no grosze and groups with a space. */
	sum(value: number | null): string {
		return this.wholeFormat.format(value ?? 0);
	}

	bmiText(): string {
		return this.bmiFormat.format(this.bmi());
	}
}
