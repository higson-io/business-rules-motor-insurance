import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';

import {
	AGE_RANGE,
	DEFAULT_MAIN_SUM,
	InsuredDetails,
	MAIN_SUMS,
	OccupationClass,
	RIDERS,
	Rider,
	RiderCode,
	bmiOf,
	priceQuote,
} from './pricing/life-tariff';

/** The four occupational risk classes, in the wording underwriting actually uses. */
const OCCUPATIONS: readonly { readonly value: OccupationClass; readonly label: string }[] = [
	{ value: 1, label: 'I — praca biurowa' },
	{ value: 2, label: 'II — praca w terenie, handel, usługi' },
	{ value: 3, label: 'III — praca fizyczna, transport' },
	{ value: 4, label: 'IV — budowy, wysokości, maszyny' },
];

/**
 * The life demo: an individual protection policy you assemble.
 *
 * ⛔ Deliberately NOT the motor demo's shape. That screen compares three ready-made tiers; a Polish
 * individual life policy is not sold that way — you take a main contract and hang riders off it, and
 * the premium accumulates as you do. So this is a configurator, and the interesting moment is
 * watching the monthly figure move, not putting three columns side by side.
 *
 * ⛔ NO BACKEND. `pricing/life-tariff.ts` does the arithmetic locally. That module is the seam: it
 * already returns the breakdown a service would, so wiring it to Higson later replaces one import
 * and nothing else. Nothing in this component knows where a premium came from.
 *
 * The copy is Polish because the product is: `suma ubezpieczenia`, `umowy dodatkowe`, `karencja` and
 * `poważne zachorowanie` are the terms a Polish buyer and a Polish underwriter both use, and
 * translating them loses the recognition that is the whole point of the demo.
 */
@Component({
	selector: 'app-life',
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
	templateUrl: './life.html',
	styleUrl: './life.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Life {
	readonly ageRange = AGE_RANGE;
	readonly occupations = OCCUPATIONS;
	readonly mainSums = MAIN_SUMS;
	readonly riders = RIDERS;

	// ── The answers ───────────────────────────────────────────────────────────────────────────
	// ⛔ No gender. Since Test-Achats took effect in December 2012 an EU insurer may not price on
	// sex, so asking for it here would either be a field that changes nothing or a demonstration of
	// something unlawful. See `life-tariff.ts`.
	readonly age = signal(35);
	readonly smoker = signal(false);
	readonly heightCm = signal(178);
	readonly weightKg = signal(76);
	readonly occupationClass = signal<OccupationClass>(1);
	readonly mainSum = signal(DEFAULT_MAIN_SUM);

	/** Rider code → chosen cover amount. Absent means the rider is not on the policy. */
	private readonly selected = signal<ReadonlyMap<RiderCode, number>>(
		new Map([['PZ', 50_000] as const]),
	);

	// ── Derived ───────────────────────────────────────────────────────────────────────────────

	private readonly details = computed<InsuredDetails>(() => ({
		age: this.age(),
		smoker: this.smoker(),
		heightCm: this.heightCm(),
		weightKg: this.weightKg(),
		occupationClass: this.occupationClass(),
	}));

	readonly bmi = computed(() => bmiOf(this.details()));

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

	readonly quote = computed(() =>
		priceQuote(this.details(), this.mainSum(), this.selected()),
	);

	/**
	 * Each line's share of the monthly premium, 0–1 — the stacked bar above the total.
	 *
	 * The same idea as the motor demo's composition bars and deliberately a different shape: there
	 * you are comparing three prices, here you are building one, so the parts belong in a single bar
	 * rather than one per row. It is what makes "adding this rider costs that much" visible without
	 * reading a column of figures.
	 */
	readonly shares = computed(() => {
		const { lines, total } = this.quote();
		if (total <= 0) {
			return [];
		}
		return lines.map((line) => ({ code: line.code, share: line.premium / total }));
	});

	// ── Interaction ───────────────────────────────────────────────────────────────────────────

	isSelected(code: RiderCode): boolean {
		return this.selected().has(code);
	}

	amountOf(rider: Rider): number {
		return this.selected().get(rider.code) ?? rider.defaultAmount;
	}

	toggle(rider: Rider, on: boolean): void {
		this.selected.update((current) => {
			const next = new Map(current);
			if (on) {
				next.set(rider.code, this.amountOf(rider));
			} else {
				next.delete(rider.code);
			}
			return next;
		});
	}

	setAmount(rider: Rider, amount: number): void {
		this.selected.update((current) => {
			const next = new Map(current);
			next.set(rider.code, amount);
			return next;
		});
	}

	// ── Polish number formatting ──────────────────────────────────────────────────────────────
	//
	// ⛔ NOT `DecimalPipe`, and NOT a registered locale. Angular's pipe follows `LOCALE_ID`, which is
	// deliberately left at `en-US` application-wide: the motor demo's goldens pin `1,234.50` and
	// AC-S5 checks that no locale is ever registered, because doing so would silently reformat that
	// screen. A Polish life quotation that printed `300,000 zł` would be wrong in the other
	// direction, so this screen formats through `Intl` directly — one demo's locale, applied where
	// it belongs and nowhere else.
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
	money(value: number): string {
		return this.plnFormat.format(value);
	}

	/** `300 000` — a sum insured carries no grosze and groups with a space. */
	sum(value: number): string {
		return this.wholeFormat.format(value);
	}

	bmiText(): string {
		return this.bmiFormat.format(this.bmi());
	}

	/** Keeps the age input inside the range the product is written for. */
	setAge(value: number): void {
		if (!Number.isFinite(value)) {
			return;
		}
		this.age.set(Math.min(AGE_RANGE.max, Math.max(AGE_RANGE.min, Math.round(value))));
	}
}
