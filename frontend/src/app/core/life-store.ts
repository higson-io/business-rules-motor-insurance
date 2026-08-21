import { Injectable, inject, resource, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { LifeService } from '../services/life-service';

/**
 * Server state of the life demo. Same shape as `QuoteStore`: one `resource()` over the session-scoped
 * quote, one trigger that reloads it, and no client-side model — the backend holds the aggregate and
 * Higson holds the pricing, so the SPA stays a thin remote-controlled view.
 *
 * ⚠️ Read `quote` through `hasValue()`, never through a bare `value()`. In Angular 22 a resource in
 * the error state **throws `ResourceValueError`** from `value()` rather than returning `undefined`,
 * so `value()?.riders ?? []` does not degrade — it takes the component down on exactly the failure
 * the error banner exists for. See `standards/frontend/state.md`.
 */
@Injectable({ providedIn: 'root' })
export class LifeStore {
	private readonly lifeService = inject(LifeService);

	/**
	 * ⛔ No `params` trigger — recalculation goes through `reload()`.
	 *
	 * A params change is a NEW request: the resource drops to the loading state with no value, so
	 * every component reading it falls back to its empty rendering and the whole screen blinks on
	 * each keystroke commit. `reload()` keeps the previous value visible while the new one is in
	 * flight, which is what a recalculation should look like.
	 */
	readonly quote = resource({
		loader: () => firstValueFrom(this.lifeService.getQuote()),
	});

	/**
	 * Every mutation goes through here: send the change, then reload. Sequencing matters — the GET
	 * has to run after the PUT has been applied to the session bean, otherwise the screen shows the
	 * previous premium and looks like the rules ignored the input.
	 */
	async commit(change: Promise<unknown>): Promise<void> {
		await change;
		this.recalculate();
	}

	recalculate(): void {
		this.quote.reload();
	}
}
