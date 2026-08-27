import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import {
	ApkNeedDto,
	ApkRecommendationCopy,
	ApkRecommendationDto,
	ApkWarningDto,
} from '../../../models/apk';

/**
 * Ekran rekomendacji — wspólny dla wszystkich trzech linii.
 *
 * ⛔ Ten komponent nie zna ani jednego produktu. Dostaje gotową rekomendację z reguł i nazwy
 * do wyświetlenia; nie wie, czym jest wariant, umowa dodatkowa ani pokrycie. Dlatego ten sam
 * plik obsługuje propozycję Z wariantem (majątek, grupowe życie) i BEZ wariantu (ochrona
 * indywidualna, gdzie propozycją jest zestaw umów, a nie pudełko z półki).
 *
 * ⛔ Wszystkie bloki są sterowane danymi, nie datą. Pusta lista uzasadnienia nie renderuje pustej
 * ramki — w reżimie ustawowym uzasadnienia po prostu nie ma i ekran ma to pokazywać
 * nieobecnością, a nie nagłówkiem nad niczym. To jest cała demonstracja różnicy reżimów.
 */
@Component({
	selector: 'app-apk-recommendation',
	templateUrl: './recommendation.html',
	styleUrl: './recommendation.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApkRecommendation {
	readonly recommendation = input.required<ApkRecommendationDto | null>();
	readonly copy = input.required<ApkRecommendationCopy>();

	/**
	 * Nazwa proponowanego wariantu, albo `null`, gdy propozycja wariantu nie ma.
	 *
	 * ⛔ Gospodarz podaje NAZWĘ, nie kod — bo tylko on wie, gdzie w jego modelu leżą nazwy
	 * (majątek ma `variants`, życie grupowe też, ochrona indywidualna nie ma ich wcale).
	 */
	readonly variantName = input<string | null>(null);

	/** Nazwy pokryć, których w propozycji brakuje. Gospodarz tłumaczy kody na nazwy. */
	readonly missingNames = input<readonly string[]>([]);

	readonly needs = computed<readonly ApkNeedDto[]>(() => this.recommendation()?.needs ?? []);

	/** Ostrzeżenia przychodzą już posortowane wagą malejąco — sortowania po stronie klienta brak. */
	readonly warnings = computed<readonly ApkWarningDto[]>(
		() => this.recommendation()?.warnings ?? [],
	);

	readonly justification = computed<readonly string[]>(
		() => this.recommendation()?.justification ?? [],
	);

	readonly hasProposal = computed(() => {
		const recommendation = this.recommendation();
		return recommendation !== null && recommendation.totalNeeds > 0;
	});

	readonly heading = computed(() => {
		const name = this.variantName();
		const copy = this.copy();
		return name === null || name === '' ? copy.proposalNoVariant : fill(copy.proposal, name);
	});

	readonly coverage = computed(() => {
		const recommendation = this.recommendation();
		return recommendation === null
			? null
			: fill(this.copy().coverage, recommendation.coveredNeeds, recommendation.totalNeeds);
	});

	readonly missingLine = computed(() => {
		const names = this.missingNames();
		return names.length === 0 ? null : fill(this.copy().missing, names.join(', '));
	});
}

/** Podstawienie `{0}`, `{1}`… — ten sam mechanizm, co w tabelach powodów po stronie reguł. */
function fill(text: string, ...values: readonly (string | number)[]): string {
	return values.reduce<string>(
		(out, value, index) => out.replace(`{${index}}`, String(value)),
		text,
	);
}
