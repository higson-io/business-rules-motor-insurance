import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

/** Jeden krok paska. */
export interface Step {
	readonly key: string;
	readonly label: string;
}

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
})
export class StepBar {
	readonly steps = input.required<readonly Step[]>();
	readonly current = input.required<string>();

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
	 * ⛔ Każdy krok osiągalny ZAWSZE, także zanim wycena się wczyta. Blokada „dopóki nie ma
	 * danych" nic tu nie chroni — ekran rekomendacji ma własny stan pusty, a ekran składki własny
	 * baner błędu — a kosztuje kliknięcie, które nic nie robi. Na prezentacji to wygląda na
	 * zepsute demo, a nie na zabezpieczenie.
	 */
	go(step: Step): void {
		if (step.key !== this.current()) {
			this.selected.emit(step.key);
		}
	}
}
