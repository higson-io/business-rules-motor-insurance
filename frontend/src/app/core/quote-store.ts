import { Injectable, inject, resource, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { DriverService } from '../services/driver-service';
import { VariantService } from '../services/variant-service';

/** The four vehicle fields a component can report as changed. */
export type VehicleField = 'productionYear' | 'make' | 'type' | 'model';

/**
 * ⛔ DR-13 / ND-1 — the reload asymmetry, encoded as data rather than as component discipline.
 *
 * Only a Model change reloads the quote. Production year, Make and Type deliberately do NOT: each
 * quote reload is a full rules evaluation, and the two intermediate cascade steps produce an
 * incomplete vehicle anyway. This is intended behaviour carried over from the AngularJS build, not
 * an oversight — a "tidy" reactive rewrite that recalculates on every vehicle change is a FAILED
 * migration, not an improvement.
 *
 * The map is exhaustive over `VehicleField` by construction: adding a field to the union is a
 * compile error until someone decides, explicitly and in this one place, whether it reloads. That
 * is the point — the asymmetry cannot be lost by a component forgetting about it, only by someone
 * editing this table.
 */
const RELOADS_QUOTE: Record<VehicleField, boolean> = {
	productionYear: false,
	make: false,
	type: false,
	model: true,
};

/**
 * Holds the two pieces of server state the screen shares, and the one signal that says when the
 * quote is stale. It replaces the AngularJS `$emit('recalculate')` → relay → `$on('reloadQuote')`
 * event bus.
 *
 * There is no client-side quote model here on purpose: the backend keeps the whole quote in a
 * session-scoped bean, so the SPA stays a thin remote-controlled view over it.
 */
@Injectable({ providedIn: 'root' })
export class QuoteStore {
	// The store depends on the service layer, never on HttpClient directly.
	private readonly variantService = inject(VariantService);
	private readonly driverService = inject(DriverService);

	/** Write-only counter; bumping it is what reloads the quote. */
	private readonly reloadTrigger = signal(0);

	/**
	 * DR-07: a stale set of variant cards can never survive underneath the error banner, because
	 * nothing caches the last good value — a failed load simply has no value to read.
	 *
	 * ⚠️ Read this through `hasValue()`, never through a bare `value()`. In Angular 22 a resource in
	 * the error state **throws `ResourceValueError`** from `value()` rather than returning
	 * `undefined`, so `value()?.options ?? []` does not degrade — it takes the component down on
	 * exactly the failure DR-07 is about. See `standards/frontend/state.md`.
	 */
	readonly quote = resource({
		params: () => this.reloadTrigger(),
		loader: () => firstValueFrom(this.variantService.getQuote()),
	});

	/**
	 * Loaded once and shared by every consumer. The AngularJS build fetched the driver twice on
	 * startup, once per component.
	 */
	readonly driver = resource({
		loader: () => firstValueFrom(this.driverService.getDriver()),
	});

	/** Written by the mutation-error interceptor; rendered by the existing error banner. */
	readonly mutationError = signal<string | null>(null);

	/** Replaces `$scope.$emit('recalculate')` at all 11 emit sites. */
	recalculate(): void {
		this.reloadTrigger.update((n) => n + 1);
	}

	/**
	 * ⛔ DR-13 / ND-1. The vehicle component reports every field change through this one method and
	 * never calls `recalculate()` itself, so the asymmetry above applies uniformly and cannot be
	 * reintroduced by a component that routes all four fields through a single handler.
	 */
	vehicleFieldChanged(field: VehicleField): void {
		if (RELOADS_QUOTE[field]) {
			this.recalculate();
		}
	}
}
