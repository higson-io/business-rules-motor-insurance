import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { Inputs } from '../../components/inputs/inputs';
import { Variants } from '../../components/variants/variants';
import { QuoteStore } from '../../core/quote-store';
import { ApkRecommendationCopy } from '../../models/apk';
import { ApkRecommendation } from '../../shared/apk/recommendation/recommendation';
import { Step, StepBar } from '../../shared/step-bar/step-bar';
import { MotoApk } from './apk/moto-apk';
import { EffectiveDate } from './effective-date/effective-date';

/**
 * Napisy ramki ekranu rekomendacji — po angielsku, tak jak reszta tego demo (ND-2), i tak jak
 * treść, którą podaje `demo.moto.apk.recommendation.reason`.
 */
const RECOMMENDATION_COPY: ApkRecommendationCopy = {
	proposal: 'Proposed cover: {0}',
	proposalNoVariant: 'Proposed cover',
	coverage: 'Meets {0} of your {1} needs',
	needsTitle: 'What your answers point to',
	missing: 'Left outside the cover: {0}.',
	justificationTitle: 'Why this proposal',
	warningsTitle: 'Before you decide',
	empty: 'Answer the questions — the proposal will appear here.',
};

/**
 * The motor-insurance demo — the screen the AngularJS application was, lifted out of `app.ts`
 * unchanged when the landing screen arrived.
 *
 * ⛔ Three steps, in the order the analysis runs rather than the order a calculator usually
 * offers: ask first, show what the answers imply, price last. A screen that opens on the price
 * has stopped being a needs analysis — which is the whole point of KNF recommendation 8.
 *
 * ⛔ Driver and vehicle inputs belong to the PRICING step, not to the questionnaire. They are risk
 * data, and the needs analysis deliberately does not depend on them: what the client needs is not
 * decided by what car they drive.
 *
 * ⛔ NO ROUTER (ND-7): the step is a signal, the document URL never moves, and every relative
 * asset keeps resolving under all three context paths.
 */
@Component({
	selector: 'app-moto',
	imports: [ApkRecommendation, EffectiveDate, Inputs, MotoApk, StepBar, Variants],
	templateUrl: './moto.html',
	styleUrl: './moto.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Moto {
	private readonly store = inject(QuoteStore);

	readonly copy = RECOMMENDATION_COPY;

	readonly step = signal<string>('apk');

	/** ⚠️ `hasValue()` first — a resource in the error state throws from `value()`. */
	private readonly loaded = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value() : null,
	);

	/**
	 * ⛔ The order is the argument: ask first, show what the answers imply, price last. The bar
	 * shows POSITION, not permission — every step is always reachable, because the presenter has
	 * to be able to go back, change one answer and show the proposal move.
	 */
	readonly steps: readonly Step[] = [
		{ key: 'apk', label: 'Needs analysis' },
		{ key: 'reco', label: 'Recommendation' },
		{ key: 'price', label: 'Premium' },
	];

	readonly recommendation = computed(() => this.loaded()?.apkRecommendation ?? null);

	/**
	 * ⛔ Kod, nie nazwa — bo tabela porównawcza tego demo też pokazuje kod (`variant-code`).
	 * `OptionDto` nazwy nie niesie i nie warto jej dokładać: nagłówek rekomendacji ma mówić tym
	 * samym słowem, którym mówi tabela pod nim, a nie drugim.
	 */
	readonly recommendedName = computed(
		() => this.loaded()?.apkRecommendation?.variantCode || null,
	);

	/**
	 * Nazwy pokryć, nie kody. Pokrycia wiszą pod wariantami, więc szukamy w pierwszym z brzegu —
	 * drabinka jest ścisła, każdy wariant zna te same kody, różni się dostępnością.
	 */
	readonly missingNames = computed(() => {
		const quote = this.loaded();
		if (quote === null) {
			return [];
		}
		const names = new Map<string, string>();
		for (const option of quote.options) {
			for (const coverage of option.coverages) {
				names.set(coverage.code, coverage.name);
			}
		}
		return (quote.apkRecommendation?.missingCovers ?? []).map((code) => names.get(code) ?? code);
	});

	goTo(step: string): void {
		this.step.set(step);
	}
}
