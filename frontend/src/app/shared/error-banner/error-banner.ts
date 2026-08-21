import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { MatCard } from '@angular/material/card';

import { MutationErrors } from '../../core/mutation-errors';
import { bodyMessageOf } from '../../core/http-error-message';

/**
 * Jeden baner, dwa źródła: nieudana mutacja i nieudane wczytanie wyceny.
 *
 * Nieudana mutacja wygrywa, kiedy oba są żywe, bo jest tym, co użytkownik zrobił ostatnio;
 * interceptor kasuje ją w chwili startu następnej mutacji, więc nie zostaje nagrobkiem. Ta sama
 * zasada, którą demo motoryzacyjne dostało w DR-06 — tyle że demo życiowe i majątkowe nie miały
 * jej wcale i nieudany PUT ginął w nich bez śladu.
 *
 * ⛔ Demo motoryzacyjne zostaje przy swoim banerze wpisanym w `variants.html`. Jego znacznik i
 * treść są przypięte przez goldeny parity; wymiana ich na ten komponent byłaby ryzykiem bez zysku.
 */
@Component({
	selector: 'app-error-banner',
	imports: [MatCard],
	template: `
		@if (message(); as text) {
			<mat-card role="alert" class="error-banner" [attr.data-testid]="testId()">
				{{ text }}
			</mat-card>
		}
	`,
	styles: `
		.error-banner {
			margin-bottom: 16px;
			padding: 12px 16px;
			font-size: 0.875rem;
		}
	`,
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorBanner {
	private readonly mutationErrors = inject(MutationErrors);

	/**
	 * Błąd wczytania, podany przez wołającego. ⚠️ `resource.error()` zwraca `undefined`, gdy nic
	 * się nie zepsuło — i to jest jedyny bezpieczny sposób, żeby o to zapytać: `value()` na zasobie
	 * w stanie błędu RZUCA w Angularze 22.
	 */
	readonly loadError = input<Error | undefined>(undefined);

	/** Treść, gdy serwer nie przysłał własnej. */
	readonly genericMessage = input('Nie udało się wczytać wyceny. Odśwież stronę i spróbuj ponownie.');

	readonly testId = input('error-banner');

	readonly message = computed<string | null>(() => {
		const mutationFailure = this.mutationErrors.message();
		if (mutationFailure !== null) {
			return mutationFailure;
		}

		const loadFailure = this.loadError();
		if (loadFailure === undefined) {
			return null;
		}
		return bodyMessageOf(loadFailure) ?? this.genericMessage();
	});
}
