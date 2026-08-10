import {
	ChangeDetectionStrategy,
	Component,
	WritableSignal,
	computed,
	inject,
	linkedSignal,
} from '@angular/core';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { Observable } from 'rxjs';

import { QuoteStore } from '../../core/quote-store';
import { DriverService } from '../../services/driver-service';

/**
 * The `min` attributes of `driverHistory.html:6,16,24`. They are the only validation the screen
 * has ever carried, and today nothing enforces them: the browser empties `ng-model` on a violation
 * and the empty value is committed anyway (DR-02).
 */
const MIN_ACCIDENTS = 0;
const MIN_TICKETS = 0;
const MIN_LICENCE_AGE = 16;

/**
 * The three numeric claim-history fields — the largest request-volume change in the migration.
 *
 * The AngularJS build committed on every keystroke and sent whatever `ng-model` held, including
 * `undefined`. Here each field commits once, on blur, and only when it holds a usable number.
 */
@Component({
	selector: 'app-driver-history',
	imports: [MatFormField, MatLabel, MatInput],
	templateUrl: './driver-history.html',
	styleUrl: './driver-history.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DriverHistory {
	private readonly driverService = inject(DriverService);
	private readonly store = inject(QuoteStore);

	// DR-09: the driver comes from the store's single shared resource. The AngularJS build issued a
	// second, independent `GET driver` here (`driverHistory.js:30-36`) for the same session-scoped
	// aggregate. `linkedSignal` seeds each field from that one fetch and then holds the last value
	// the server accepted — which is what a rejected edit falls back to.
	//
	// ⚠️ Read through `hasValue()`, never through a bare `value()`. In Angular 22 a resource in the
	// error state THROWS `ResourceValueError` from `value()` rather than returning `undefined`, so
	// `value()?.x ?? null` does not degrade gracefully — it takes down the whole component the
	// moment `GET driver` fails.
	private readonly driver = computed(() =>
		this.store.driver.hasValue() ? this.store.driver.value() : undefined,
	);

	readonly accidents = linkedSignal(() => this.driver()?.numberOfAccidents ?? null);
	readonly tickets = linkedSignal(() => this.driver()?.numberOfTickets ?? null);
	readonly licenceAge = linkedSignal(() => this.driver()?.licenceObtainedAtAge ?? null);

	commitAccidents(value: number): void {
		this.commit(value, MIN_ACCIDENTS, this.accidents, (accidents) =>
			this.driverService.updateAccidentCount(accidents),
		);
	}

	commitTickets(value: number): void {
		this.commit(value, MIN_TICKETS, this.tickets, (tickets) =>
			this.driverService.updateTrafficTicketsCount(tickets),
		);
	}

	commitLicenceAge(value: number): void {
		this.commit(value, MIN_LICENCE_AGE, this.licenceAge, (licenceAge) =>
			this.driverService.updateLicenceObtainedAtAge(licenceAge),
		);
	}

	/**
	 * ⛔ DR-02 — guard BEFORE sending. `valueAsNumber` is `NaN` for an empty or unparseable field,
	 * and that is exactly the state the AngularJS build put on the wire: `ng-model` became
	 * `undefined`, `$http.put` sent no body at all, Spring bound null, and unboxing into `int` threw
	 * — a verified HTTP 500 that no error path surfaced, so the user saw nothing.
	 *
	 * The unchanged check reproduces `ng-change`, which fires only when the model actually changes:
	 * blurring a field nobody edited must not spend a PUT and a full rules evaluation.
	 */
	private commit(
		value: number,
		min: number,
		field: WritableSignal<number | null>,
		put: (value: number) => Observable<void>,
	): void {
		if (!Number.isFinite(value) || value < min || value === field()) {
			return;
		}
		put(value).subscribe(() => {
			field.set(value);
			this.store.recalculate();
		});
	}
}
