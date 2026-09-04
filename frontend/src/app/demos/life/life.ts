import {
	ChangeDetectionStrategy,
	Component,
	computed,
	inject,
	linkedSignal,
	signal,
} from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { LifeStore } from '../../core/life-store';
import { ApkRecommendationCopy } from '../../models/apk';
import { LifeLineCode, LifeQuoteDto } from '../../models/life';
import { LifeService } from '../../services/life-service';
import { ApkRecommendation } from '../../shared/apk/recommendation/recommendation';
import { ErrorBanner } from '../../shared/error-banner/error-banner';
import { Step, StepBar } from '../../shared/step-bar/step-bar';
import { EffectiveDate } from './effective-date/effective-date';
import { LifeApk } from './apk/life-apk';
import { Group } from './group/group';
import { Individual } from './individual/individual';

/** Napisy ramki ekranu rekomendacji. Treść merytoryczna przychodzi z reguł. */
const RECOMMENDATION_COPY: ApkRecommendationCopy = {
	proposal: 'Proponujemy wariant {0}',
	proposalNoVariant: 'Proponowany zakres ochrony',
	coverage: 'Pokrywa {0} z {1} Twoich potrzeb',
	needsTitle: 'Potrzeby wynikające z odpowiedzi',
	missing: 'Poza zakresem zostaje: {0}.',
	justificationTitle: 'Dlaczego ta propozycja',
	empty: 'Odpowiedz na pytania ankiety — propozycja pojawi się tutaj.',
	premiumLabel: 'Składka miesięczna',
	premiumNote: 'za jedną osobę',
	needCovered: '✓',
	needMissing: '—',
};

/** The two ways life cover is bought in Poland, and they are genuinely different products. */
export type LifePath = 'individual' | 'group';

/** The screen's word for a path, and the rules engine's word for the same thing. */
const LINE_CODES: Record<LifePath, LifeLineCode> = {
	individual: 'INDIVIDUAL',
	group: 'GROUP',
};

/**
 * The life demo: two paths, sharing nothing but a heading.
 *
 * ⛔ Not a variant of one screen. Individual protection is assembled — a main contract plus riders,
 * priced on the person after a health questionnaire. Group cover is chosen — fixed variants, priced
 * on the employer's population, with no individual underwriting at all. They ask different
 * questions, price on different factors and answer with different shapes, so each path is its own
 * component and the switch below is the only thing they have in common.
 *
 * ⛔ NO ROUTER, consistent with the rest of the application (ND-7): the path is a signal, the
 * document URL never moves, and every relative asset and API reference keeps resolving under all
 * three context paths. See `DemoSelection` for the full reasoning.
 */
@Component({
	selector: 'app-life',
	imports: [ApkRecommendation, EffectiveDate, ErrorBanner, Group, Individual, LifeApk, StepBar],
	templateUrl: './life.html',
	styleUrl: './life.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Life {
	private readonly store = inject(LifeStore);
	private readonly lifeService = inject(LifeService);

	readonly copy = RECOMMENDATION_COPY;

	readonly step = signal<string>('apk');

	/** ⚠️ `hasValue()` first — zasób w stanie błędu rzuca z `value()`. */
	private readonly loaded = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value() : null,
	);

	/**
	 * Wybrana ścieżka — **wyprowadzona z linii, którą trzyma serwer**, a nie z lokalnego stanu.
	 *
	 * ⛔ `linkedSignal`, nie `signal`. Wycena jest w sesji po stronie serwera i przeżywa wyjście
	 * z demo: kto raz przełączył na grupowe i wrócił przez „Wszystkie dema", dostawał zakładkę
	 * „Indywidualne" nad ankietą grupową i grupową ceną. Zakładka kłamała o tym, co jest liczone —
	 * i nie miała jak tego zauważyć, bo nikt jej nie pytał serwera.
	 *
	 * Zapis lokalny nadal działa: `open()` ustawia ścieżkę od razu, a `computation` przelicza się
	 * dopiero, gdy `PUT life/line` wróci z nową wyceną — więc w locie nie ma migotania.
	 */
	readonly path = linkedSignal<LifeQuoteDto | null, LifePath>({
		source: this.loaded,
		computation: (quote, previous) =>
			quote === null
				? (previous?.value ?? 'individual')
				: quote.lineCode === 'GROUP'
					? 'group'
					: 'individual',
	});

	/**
	 * ⛔ Kolejność jest argumentem: najpierw pytamy, potem pokazujemy, co z odpowiedzi wynika,
	 * a cena jest na końcu. Pasek pokazuje POZYCJĘ, nie uprawnienie — każdy krok jest osiągalny
	 * zawsze, bo prowadzący musi móc wrócić do jednej odpowiedzi i pokazać, jak przestawia się
	 * propozycja.
	 */
	readonly steps: readonly Step[] = [
		{ key: 'apk', label: 'Analiza potrzeb' },
		{ key: 'reco', label: 'Rekomendacja' },
		{ key: 'price', label: 'Składka' },
	];

	readonly recommendation = computed(() => this.loaded()?.apkRecommendation ?? null);

	/**
	 * ⛔ Nazwa wariantu tylko w ścieżce GRUPOWEJ. Ochrona indywidualna nie ma wariantów — jej
	 * propozycją jest ZESTAW umów dodatkowych, więc `variantCode` przychodzi pusty i nagłówek
	 * ekranu rekomendacji ma to uszanować, a nie wypisać pustkę po słowie „wariant".
	 */
	readonly recommendedName = computed(() => {
		const quote = this.loaded();
		const code = quote?.apkRecommendation?.variantCode;
		if (!quote || !code) {
			return null;
		}
		return quote.variants.find((variant) => variant.code === code)?.name ?? code;
	});

	readonly missingNames = computed(() => {
		const quote = this.loaded();
		if (quote === null) {
			return [];
		}
		const names = new Map<string, string>();
		for (const benefit of quote.benefits) {
			names.set(benefit.code, benefit.name);
		}
		for (const rider of quote.riders) {
			names.set(rider.code, rider.name);
		}
		return (quote.apkRecommendation?.missingCovers ?? []).map((code) => names.get(code) ?? code);
	});

	/**
	 * Składka proponowanego zakresu — rek. 8.2.d.
	 *
	 * ⛔ Obie ścieżki podają liczbę ZA JEDNĄ OSOBĘ i to nie jest uproszczenie. W ochronie
	 * indywidualnej jest tylko jedna osoba, a w programie grupowym pracownik płaci właśnie tyle;
	 * koszt całego programu należy do ekranu składki, bo jest liczbą pracodawcy, nie tej analizy.
	 */
	readonly premium = computed(() => {
		const quote = this.loaded();
		if (quote === null) {
			return null;
		}
		const code = quote.apkRecommendation?.variantCode;
		const amount = code
			? (quote.variants.find((variant) => variant.code === code)?.perEmployee ?? null)
			: quote.premiumMonthly;
		return amount === null ? null : this.money.format(amount) + ' zł';
	});

	private readonly money = new Intl.NumberFormat('pl-PL', {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	});

	goTo(step: string): void {
		this.step.set(step);
	}

	/** ⚠️ `error()`, nie `value()`. Zasób w stanie błędu rzuca z `value()` w Angularze 22. */
	readonly loadError = computed(() => this.store.quote.error());

	/**
	 * ⛔ The switch is not local state. The backend holds the aggregate and decides which flow prices
	 * it, so the path has to reach `PUT life/line` — otherwise the group screen renders against an
	 * individually-priced quote, which has no available variants and shows an empty comparison.
	 */
	open(path: LifePath): void {
		if (this.path() === path) {
			return;
		}
		this.path.set(path);
		void this.store.commit(firstValueFrom(this.lifeService.updateLine(LINE_CODES[path])));
	}
}
