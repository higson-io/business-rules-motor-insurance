import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';

import {
	BENEFITS,
	Benefit,
	GroupDetails,
	HEADCOUNT_RANGE,
	IndustryClass,
	PARTICIPATION_RANGE,
	VARIANTS,
	VariantCode,
	priceGroup,
} from '../pricing/group-tariff';

/** Industry risk classes, in the wording a group underwriter classifies an employer by. */
const INDUSTRIES: readonly { readonly value: IndustryClass; readonly label: string }[] = [
	{ value: 1, label: 'I — biura, IT, finanse, administracja' },
	{ value: 2, label: 'II — handel, edukacja, ochrona zdrowia' },
	{ value: 3, label: 'III — produkcja, logistyka, transport' },
	{ value: 4, label: 'IV — budownictwo, górnictwo, energetyka' },
];

/**
 * The group path: an employer-sponsored scheme, compared across three ready-made variants.
 *
 * ⛔ Deliberately the OPPOSITE interaction to the individual path beside it. There you assemble a
 * contract and the premium accumulates; group cover is not assembled — the insurer offers fixed
 * variants and the employer picks one, so the question is what each variant contains and the answer
 * is a comparison. Benefits are rows and variants are columns, the same shape the motor demo uses
 * for the same reason.
 *
 * ⛔ AND NO INDIVIDUAL UNDERWRITING. No age of the person, no BMI, no tobacco — a group policy rates
 * the GROUP. That is not a simplification for the demo: it is the product. See `group-tariff.ts` for
 * why participation is the factor that matters most.
 */
@Component({
	selector: 'app-life-group',
	imports: [MatCheckbox, MatFormField, MatInput, MatLabel, MatOption, MatSelect, MatSuffix],
	templateUrl: './group.html',
	styleUrl: './group.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Group {
	readonly headcountRange = HEADCOUNT_RANGE;
	readonly participationRange = PARTICIPATION_RANGE;
	readonly industries = INDUSTRIES;
	readonly benefits = BENEFITS;
	readonly variants = VARIANTS;

	readonly headcount = signal(120);
	readonly averageAge = signal(38);
	readonly industryClass = signal<IndustryClass>(1);
	readonly participation = signal(80);
	readonly includeSpouse = signal(false);
	readonly includeAdultChild = signal(false);

	private readonly details = computed<GroupDetails>(() => ({
		headcount: this.headcount(),
		averageAge: this.averageAge(),
		industryClass: this.industryClass(),
		participation: this.participation(),
		includeSpouse: this.includeSpouse(),
		includeAdultChild: this.includeAdultChild(),
	}));

	readonly quote = computed(() => priceGroup(this.details()));

	/**
	 * How many benefits a variant carries, against the richest — the gauge under each column
	 * heading, so the ladder reads before any figure is compared. Derived from the benefit table
	 * rather than written per column, for the same reason the motor demo derives its own.
	 */
	breadthOf(code: VariantCode): number {
		const counts = this.variants.map((v) => this.benefitCount(v.code));
		const richest = Math.max(0, ...counts);
		return richest === 0 ? 0 : this.benefitCount(code) / richest;
	}

	benefitCount(code: VariantCode): number {
		return this.benefits.filter((benefit) => benefit.sums[code] !== null).length;
	}

	sumFor(benefit: Benefit, code: VariantCode): number | null {
		return benefit.sums[code];
	}

	// ── Bounded inputs ────────────────────────────────────────────────────────────────────────
	// Both are typed by hand and both have ranges the product is written for, so they clamp rather
	// than letting a slip produce a quotation for four employees aged two hundred.

	setHeadcount(value: number): void {
		if (!Number.isFinite(value)) {
			return;
		}
		this.headcount.set(
			Math.min(HEADCOUNT_RANGE.max, Math.max(HEADCOUNT_RANGE.min, Math.round(value))),
		);
	}

	setParticipation(value: number): void {
		if (!Number.isFinite(value)) {
			return;
		}
		this.participation.set(
			Math.min(PARTICIPATION_RANGE.max, Math.max(PARTICIPATION_RANGE.min, Math.round(value))),
		);
	}

	setAverageAge(value: number): void {
		if (!Number.isFinite(value)) {
			return;
		}
		this.averageAge.set(Math.min(67, Math.max(18, Math.round(value))));
	}

	// ── Polish number formatting ──────────────────────────────────────────────────────────────
	// Through `Intl` rather than a registered locale, for the reason recorded in the individual
	// path: the application stays on en-US because the motor goldens pin that formatting.
	private readonly plnFormat = new Intl.NumberFormat('pl-PL', {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	});
	private readonly wholeFormat = new Intl.NumberFormat('pl-PL', { maximumFractionDigits: 0 });

	money(value: number): string {
		return this.plnFormat.format(value);
	}

	sum(value: number): string {
		return this.wholeFormat.format(value);
	}
}
