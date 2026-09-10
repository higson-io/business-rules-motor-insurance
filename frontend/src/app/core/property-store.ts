import { Injectable, inject, resource, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { PropertyService } from '../services/property-service';

/**
 * Server state of the property demo. Same shape as `LifeStore` and `QuoteStore`: one `resource()`
 * over the session-scoped quote, one trigger that reloads it, and no client-side model.
 *
 * ⚠️ Read `quote` through `hasValue()`, never through a bare `value()`. In Angular 22 a resource in
 * the error state **throws `ResourceValueError`** from `value()` rather than returning `undefined`,
 * so `value()?.questions ?? []` does not degrade — it takes the component down on exactly the
 * failure the error banner exists for. See `standards/frontend/state.md`.
 */
@Injectable({ providedIn: 'root' })
export class PropertyStore {
	private readonly propertyService = inject(PropertyService);

	/**
	 * ⛔ No `params` trigger — recalculation goes through `reload()`.
	 *
	 * A params change is a NEW request: the resource drops to the loading state with no value, so
	 * every component reading it falls back to its empty rendering and the whole questionnaire
	 * blinks on each answer. `reload()` keeps the previous value visible while the new one is in
	 * flight, which matters more here than anywhere else in this application — the answer set is
	 * the screen.
	 */
	readonly quote = resource({
		loader: () => firstValueFrom(this.propertyService.getQuote()),
	});

	/**
	 * Every mutation goes through here: send the change, then reload. Sequencing matters — the GET
	 * has to run after the PUT has been applied to the session bean, otherwise the screen shows the
	 * previous questionnaire and looks like the rules ignored the answer.
	 */
	async commit(change: Promise<unknown>): Promise<void> {
		await change;
		this.recalculate();
	}

	recalculate(): void {
		this.quote.reload();
	}

	/**
	 * Nowy proces: serwer porzuca wniosek, ekran wczytuje pusty.
	 *
	 * ⛔ Wołane przy KAŻDYM wejściu do dema, nie z przycisku. Sesja HTTP żyje dłużej niż pokaz —
	 * prowadzący wraca do wyboru dema, wchodzi ponownie i bez tego zastaje ankietę poprzedniego
	 * klienta razem z jego ofertą. Na prezentacji resztka po poprzednim przejściu jest myląca
	 * w sposób, którego nie da się na żywo wytłumaczyć.
	 *
	 * ⚠️ `commit`, nie samo `reload()`: kolejność jest tu wszystkim. GET przed zakończeniem PUT-a
	 * przyniósłby stary wniosek i ekran pokazałby dane, które właśnie kazaliśmy skasować.
	 */
	async startNewProcess(): Promise<void> {
		await this.commit(firstValueFrom(this.propertyService.reset()));
	}
}
