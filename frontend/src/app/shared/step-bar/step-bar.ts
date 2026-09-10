import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

/** Jeden krok paska. */
export interface Step {
	readonly key: string;
	readonly label: string;
	/**
	 * Krok, do którego nie wolno jeszcze przejść — bo poprzedni nie jest domknięty.
	 *
	 * ⛔ Puste znaczy OTWARTY, więc demo, które o bramkowaniu nic nie wie (moto, życie), zachowuje
	 * się jak dotąd: wszystko klikalne. Bramkowanie liczy GOSPODARZ, bo tylko on wie, co znaczy
	 * „domknięty" w jego procesie.
	 */
	readonly locked?: boolean;
}

/**
 * Jak pasek ma być narysowany.
 *
 * `panel` — biały panel z cieniem i zaokrąglonymi górnymi rogami, siedzący na treści jak jej
 * nagłówek. Tak wygląda pasek w portalu i tak go używają moto oraz życie.
 *
 * `plain` — same zakładki na kresce, bez własnego tła. Wprowadzone 2026-09-09 razem z redesignem
 * dema majątkowego, w którym CAŁY ekran siedzi już w jednej białej karcie: panel w panelu rysował
 * drugą krawędź w miejscu, gdzie nic się nie kończy.
 *
 * ⛔ Wariant, a nie zmiana dla wszystkich. Redesign objął jedno demo; przestawienie paska globalnie
 * zostawiłoby moto i życie z zakładkami wiszącymi nad panelem, którego nagłówkiem przestały być.
 */
export type StepBarAppearance = 'panel' | 'plain';

/**
 * Pasek kroków, wzięty z `step-bar` z higson-agent-portal-demo-frontend: pełna szerokość, białe
 * tło, zaokrąglone tylko górne rogi, krok bieżący na jasnym tle z miętowym podkreśleniem.
 *
 * ⛔ **Klikalny, w odróżnieniu od portalowego.** Tam pasek jest napędzany routerem i ma
 * `cursor: default`, bo kolejność kroków wymusza proces sprzedaży. Tutaj kroków nie pilnuje
 * żaden proces — to jest demo, w którym prowadzący musi móc wrócić do ankiety, zmienić jedną
 * odpowiedź i pokazać, jak przestawia się rekomendacja. Bez tego cała teza demo wymaga
 * przeklikania się przez trzy ekrany w jedną stronę.
 *
 * ⛔ **Bez routera** (ND-7). Krok jest sygnałem w komponencie demo, nie adresem URL. Konsekwencja
 * jest ta sama co przy wyborze demo: wstecz przeglądarki nie cofa kroku.
 */
@Component({
	selector: 'app-step-bar',
	templateUrl: './step-bar.html',
	styleUrl: './step-bar.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
	// Wariant wygląda inaczej, ale ma to samo drzewo — różnicę niesie klasa na gospodarzu,
	// a nie druga gałąź w szablonie (`components.md`: host bindings w obiekcie `host`).
	host: {
		'[class.step-bar--plain]': "appearance() === 'plain'",
	},
})
export class StepBar {
	readonly steps = input.required<readonly Step[]>();
	readonly current = input.required<string>();
	readonly appearance = input<StepBarAppearance>('panel');

	readonly selected = output<string>();

	private readonly index = computed(() =>
		this.steps().findIndex((step) => step.key === this.current()),
	);

	/** Krok „zrobiony" to każdy PRZED bieżącym — pasek pokazuje postęp, nie kompletność danych. */
	isDone(step: Step): boolean {
		const position = this.steps().indexOf(step);
		return position > -1 && position < this.index();
	}

	/**
	 * ⛔ Blokada dotyczy WYŁĄCZNIE ruchu W PRZÓD i to jest cała jej treść. Krok już domknięty
	 * zostaje klikalny na zawsze — bez tego znika rzecz, dla której to demo istnieje: cofnąć się
	 * do jednej odpowiedzi i pokazać, jak przestawia się propozycja.
	 *
	 * ⚠️ Do 2026-09-09 osiągalne było wszystko, także zanim wycena się wczytała, i było to
	 * uzasadnione („ekran rekomendacji ma własny stan pusty"). Zmienione na wyraźne życzenie:
	 * skakanie w przód pokazywało gotową ofertę, zanim klient cokolwiek wybrał.
	 */
	go(step: Step): void {
		if (step.locked !== true && step.key !== this.current()) {
			this.selected.emit(step.key);
		}
	}
}
