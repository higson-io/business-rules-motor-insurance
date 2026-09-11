import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Dwa przyciski dla PROWADZĄCEGO pokaz, nie dla klienta.
 *
 * ⛔ To jedyny element tego ekranu, który mówi do osoby prowadzącej demo, a nie do osoby
 * kupującej ubezpieczenie — i dlatego stoi w rogu, jest mały i wyciszony. Reszta ekranu została
 * świadomie oczyszczona z komentarza o samej sobie (§21); te przyciski nie są komentarzem, tylko
 * narzędziem, ale muszą wyglądać na narzędzie i nie wchodzić w kadr.
 *
 * `refresh` — pyta serwer o wycenę jeszcze raz, BEZ ruszania odpowiedzi. Aplikacja czyta wtedy
 * drzewo domeny i reguły na nowo, więc pakiet dołożony w Studio pojawia się na ekranie bez
 * restartu i bez przechodzenia procesu od początku. To jest sedno pokazu „konfiguracja, nie kod".
 *
 * `reset` — porzuca wniosek i zaczyna proces od zera, żeby przed kolejnym przejściem mieć pewność,
 * że na ekranie nie ma resztek po poprzednim.
 *
 * ⛔ BEZ POTWIERDZENIA przy resecie. Okno „na pewno?" w trakcie pokazu kosztuje więcej niż
 * przypadkowe wyczyszczenie: prowadzący i tak zaczyna od nowa, a zgubiony rytm widać na sali.
 * Stąd też rozmiar i róg ekranu — przycisk, w który trudno trafić przypadkiem.
 */
@Component({
	selector: 'app-demo-tools',
	template: `
		<div class="demo-tools" role="group" aria-label="Narzędzia prowadzącego demo">
			<button
				type="button"
				class="demo-tools__button demo-tools__button--refresh"
				data-testid="demo-tools-refresh"
				[disabled]="busy()"
				[attr.aria-busy]="busy()"
				title="Pyta Higsona o wszystko jeszcze raz. Odpowiedzi zostają — zmienia się to, co powiedziała konfiguracja."
				(click)="refresh.emit()"
			>
				<span class="demo-tools__glyph" aria-hidden="true">&#8635;</span>
				{{ busy() ? 'Odświeżam…' : 'Odśwież z Higsona' }}
			</button>

			<button
				type="button"
				class="demo-tools__button"
				data-testid="demo-tools-reset"
				[disabled]="busy()"
				title="Porzuca wniosek i zaczyna proces od pustego ekranu."
				(click)="reset.emit()"
			>
				<span class="demo-tools__glyph" aria-hidden="true">&#10005;</span>
				Zacznij od nowa
			</button>
		</div>
	`,
	styles: `
		@import '_variables';

		// ⛔ Przyklejone do rogu OKNA, nie do końca treści. Ekrany tego dema mają różną wysokość
		// (ankieta to jedno pytanie, oferta to długi dokument), a narzędzie ma być pod ręką
		// zawsze — także wtedy, gdy prowadzący jest w połowie przewiniętej oferty.
		.demo-tools {
			position: fixed;
			right: 16px;
			bottom: 16px;
			z-index: 20;
			display: flex;
			gap: 8px;
		}

		// ⛔ Wyciszone, nie w kolorze akcji. Kolor akcji na tym ekranie znaczy „to jest krok
		// procesu, kliknij to" — przyciski prowadzącego nie są krokiem procesu i nie wolno im
		// konkurować z „Dalej".
		.demo-tools__button {
			display: inline-flex;
			align-items: center;
			gap: 6px;
			padding: 7px 12px;
			border: 1px solid @color-border-soft;
			border-radius: @radius-button;
			background: @color-white;
			box-shadow: @shadow-raised;
			font-family: inherit;
			font-size: @font-xs;
			font-weight: 600;
			color: @color-text-muted;
			cursor: pointer;

			&:hover:not(:disabled) {
				color: @color-text;
				border-color: @color-border-strong;
			}

			&:focus-visible {
				outline: 2px solid @color-action;
				outline-offset: 2px;
			}

			&:disabled {
				cursor: progress;
			}
		}

		// Odświeżenie jest tym, po co ten pasek powstał, więc jego obrys jest w kolorze akcji —
		// to nadal nie jest wypełnienie, więc nie czyta się jak „Dalej".
		.demo-tools__button--refresh {
			border-color: @color-action;
			color: @color-action;
		}

		.demo-tools__glyph {
			font-size: 14px;
			line-height: 1;
		}

		// Na wąskim ekranie zostają same znaki — dwa pełne napisy zajęłyby pół szerokości.
		@media (max-width: 700px) {
			.demo-tools__button {
				font-size: 0;
				padding: 9px;
				gap: 0;
			}

			.demo-tools__glyph {
				font-size: 16px;
			}
		}
	`,
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DemoTools {
	/** Żądanie w locie — oba przyciski milkną, żeby dwa kliknięcia nie zrobiły dwóch przejść. */
	readonly busy = input(false);

	readonly refresh = output<void>();
	readonly reset = output<void>();
}
