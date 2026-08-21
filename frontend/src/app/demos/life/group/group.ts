import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';

import { LifeStore } from '../../../core/life-store';
import { LifeBenefitDto, LifeVariantDto } from '../../../models/life';
import { LifeService } from '../../../services/life-service';

/** Industry risk classes, in the wording a group underwriter classifies an employer by. */
const INDUSTRIES: readonly { readonly value: number; readonly label: string }[] = [
	{ value: 1, label: 'I — biura, IT, finanse, administracja' },
	{ value: 2, label: 'II — handel, edukacja, ochrona zdrowia' },
	{ value: 3, label: 'III — produkcja, logistyka, transport' },
	{ value: 4, label: 'IV — budownictwo, górnictwo, energetyka' },
];

const HEADCOUNT_RANGE = { min: 3, max: 5000 } as const;
const PARTICIPATION_RANGE = { min: 10, max: 100 } as const;

/**
 * The group path: an employer-sponsored scheme, compared across ready-made variants.
 *
 * ⛔ Deliberately the OPPOSITE interaction to the individual path. There you assemble a contract and
 * the premium accumulates; group cover is not assembled — the insurer offers fixed variants and the
 * employer picks one, so the question is what each variant contains and the answer is a comparison.
 *
 * ⛔ AND NO INDIVIDUAL UNDERWRITING. No age of the person, no BMI, no tobacco — a group policy rates
 * the GROUP. That is not a simplification for the demo: it is the product, and the backend enforces
 * it by giving this branch no access to `quote.insured.*` at all.
 *
 * ⛔ NO LOCAL PRICING. Every figure comes from `demo.life.group.calculateQuote` through `LifeStore`.
 */
@Component({
	selector: 'app-life-group',
	imports: [MatCheckbox, MatFormField, MatInput, MatLabel, MatOption, MatSelect, MatSuffix],
	templateUrl: './group.html',
	styleUrl: './group.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Group {
	private readonly store = inject(LifeStore);
	private readonly lifeService = inject(LifeService);

	readonly headcountRange = HEADCOUNT_RANGE;
	readonly participationRange = PARTICIPATION_RANGE;
	readonly industries = INDUSTRIES;

	/** ⚠️ `hasValue()` before `value()` — a failed resource throws from `value()` in Angular 22. */
	private readonly loaded = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value() : null,
	);

	readonly headcount = computed(() => this.loaded()?.headcount ?? HEADCOUNT_RANGE.min);
	readonly averageAge = computed(() => this.loaded()?.averageAge ?? 35);
	readonly industryClass = computed(() => this.loaded()?.industryClass ?? 1);
	readonly participation = computed(() => this.loaded()?.participation ?? 0);
	readonly includeSpouse = computed(() => this.loaded()?.includeSpouse ?? false);
	readonly includeAdultChild = computed(() => this.loaded()?.includeAdultChild ?? false);

	readonly benefits = computed<readonly LifeBenefitDto[]>(() => this.loaded()?.benefits ?? []);

	/** Only variants the rules made available for this headcount reach the comparison. */
	readonly variants = computed<readonly LifeVariantDto[]>(() =>
		(this.loaded()?.variants ?? []).filter((variant) => variant.available),
	);

	readonly insuredCount = computed(() => this.loaded()?.insuredCount ?? null);

	/**
	 * Rozkład wyborów pracowników, i to, czy w ogóle jakiś jest.
	 *
	 * ⛔ `enrolled()` czyta rozkład, a NIE liczy fazy z niczego innego. To jedyny przełącznik
	 * między fazą oferty a fazą programu (D-2), a rozstrzygają go reguły — ten komponent tylko
	 * pyta serwer, po której stronie właśnie jest.
	 */
	private readonly enrolment = computed<Readonly<Record<string, number>>>(
		() => this.loaded()?.enrolment ?? {},
	);

	readonly enrolled = computed(() => Object.keys(this.enrolment()).length > 0);

	/** Rzeczywisty koszt programu. `null` w fazie oferty — i tam nie wolno pokazać zera. */
	readonly programCost = computed(() => this.loaded()?.programCost ?? null);

	enrolledIn(code: string): number {
		return this.enrolment()[code] ?? 0;
	}

	setEnrolment(code: string, value: number): void {
		if (!Number.isFinite(value)) {
			return;
		}
		const clamped = Math.max(0, Math.round(value));
		void this.store.commit(firstValueFrom(this.lifeService.updateEnrolment(code, clamped)));
	}

	/**
	 * Wyjście z fazy programu: wyzerowanie wszystkich przystąpień.
	 *
	 * ⛔ PO KOLEI, nie przez `Promise.all`. Agregat wyceny jest `@SessionScope` i nie jest
	 * bezpieczny wątkowo — trzy równoległe PUT-y czytają tę samą listę, każdy usuwa z niej swoją
	 * pozycję i odkłada wynik, więc przeżywa tylko ostatni zapis. Zmierzone: z trzech wariantów
	 * czyścił się jeden. Każda inna mutacja w tej aplikacji jest pojedynczym PUT-em i dlatego
	 * nigdy na to nie trafiła.
	 */
	clearEnrolment(): void {
		const codes = Object.keys(this.enrolment());
		void this.store.commit(
			(async () => {
				for (const code of codes) {
					await firstValueFrom(this.lifeService.updateEnrolment(code, 0));
				}
			})(),
		);
	}


	/**
	 * How many benefits a variant carries, against the richest — the gauge under each column
	 * heading, so the ladder reads before any figure is compared.
	 */
	breadthOf(code: string): number {
		const counts = this.variants().map((variant) => this.benefitCount(variant.code));
		const richest = Math.max(0, ...counts);
		return richest === 0 ? 0 : this.benefitCount(code) / richest;
	}

	benefitCount(code: string): number {
		return this.benefits().filter((benefit) => this.sumFor(benefit, code) !== null).length;
	}

	/**
	 * A benefit's amount in a variant, or null when the variant does not carry it.
	 *
	 * ⚠️ Zero means "carried, but as a service package" — assistance and mental-health support have
	 * no sum. Treating 0 as absent would drop two rows from the richest variant.
	 */
	sumFor(benefit: LifeBenefitDto, code: string): number | null {
		const sum = benefit.sumByVariant?.[code];
		if (sum === undefined) {
			return null;
		}
		return sum;
	}

	isServicePackage(benefit: LifeBenefitDto): boolean {
		return benefit.benefitKind === 'SERVICE_PACKAGE';
	}

	// ── Bounded inputs ────────────────────────────────────────────────────────────────────────
	// Both ranges are the ones the product is written for, so they clamp rather than letting a slip
	// produce a quotation for four employees aged two hundred.

	setHeadcount(value: number): void {
		if (!Number.isFinite(value)) {
			return;
		}
		const clamped = Math.min(
			HEADCOUNT_RANGE.max,
			Math.max(HEADCOUNT_RANGE.min, Math.round(value)),
		);
		void this.store.commit(firstValueFrom(this.lifeService.updateHeadcount(clamped)));
	}

	setParticipation(value: number): void {
		if (!Number.isFinite(value)) {
			return;
		}
		const clamped = Math.min(
			PARTICIPATION_RANGE.max,
			Math.max(PARTICIPATION_RANGE.min, Math.round(value)),
		);
		void this.store.commit(firstValueFrom(this.lifeService.updateParticipation(clamped)));
	}

	setAverageAge(value: number): void {
		if (!Number.isFinite(value)) {
			return;
		}
		const clamped = Math.min(67, Math.max(18, Math.round(value)));
		void this.store.commit(firstValueFrom(this.lifeService.updateAverageAge(clamped)));
	}

	setIndustryClass(value: number): void {
		void this.store.commit(firstValueFrom(this.lifeService.updateIndustryClass(value)));
	}

	setIncludeSpouse(value: boolean): void {
		void this.store.commit(firstValueFrom(this.lifeService.updateIncludeSpouse(value)));
	}

	setIncludeAdultChild(value: boolean): void {
		void this.store.commit(firstValueFrom(this.lifeService.updateIncludeAdultChild(value)));
	}

	// ── Polish number formatting ──────────────────────────────────────────────────────────────
	// Through `Intl` rather than a registered locale: the application stays on en-US because the
	// motor goldens pin that formatting.
	private readonly plnFormat = new Intl.NumberFormat('pl-PL', {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	});
	private readonly wholeFormat = new Intl.NumberFormat('pl-PL', { maximumFractionDigits: 0 });

	money(value: number | null): string {
		return this.plnFormat.format(value ?? 0);
	}

	sum(value: number | null): string {
		return this.wholeFormat.format(value ?? 0);
	}
}
