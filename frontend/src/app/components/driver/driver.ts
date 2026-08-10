import {
	ChangeDetectionStrategy,
	Component,
	WritableSignal,
	computed,
	inject,
	linkedSignal,
} from '@angular/core';
import {
	MatButtonToggle,
	MatButtonToggleChange,
	MatButtonToggleGroup,
} from '@angular/material/button-toggle';
import { DateAdapter } from '@angular/material/core';
import {
	MatDatepicker,
	MatDatepickerInput,
	MatDatepickerToggle,
} from '@angular/material/datepicker';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { Observable } from 'rxjs';

import { QuoteStore } from '../../core/quote-store';
import { DriverDto } from '../../models/driver';
import { DriverService } from '../../services/driver-service';

/**
 * The seven driver fields — five plain text inputs, the birth date and the gender toggle.
 *
 * Unlike the vehicle cascade, there is no asymmetry here: every accepted mutation reloads the
 * quote, exactly as `driver.js:96-98` did for all seven.
 */
@Component({
	selector: 'app-driver',
	imports: [
		MatFormField,
		MatLabel,
		MatInput,
		MatSuffix,
		MatDatepicker,
		MatDatepickerInput,
		MatDatepickerToggle,
		MatButtonToggleGroup,
		MatButtonToggle,
	],
	templateUrl: './driver.html',
	styleUrl: './driver.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Driver {
	private readonly driverService = inject(DriverService);
	private readonly store = inject(QuoteStore);
	private readonly dateAdapter: DateAdapter<Date> = inject(DateAdapter);

	/**
	 * DR-09: the one shared `GET driver`. The AngularJS build fetched the same session-scoped
	 * aggregate twice on startup, once here and once in `driverHistory.js`.
	 *
	 * ⚠️ Read through `hasValue()`, never through a bare `value()`: on a failed load an Angular 22
	 * resource THROWS `ResourceValueError` from `value()` rather than returning `undefined`.
	 */
	private readonly driver = computed<DriverDto | undefined>(() =>
		this.store.driver.hasValue() ? this.store.driver.value() : undefined,
	);

	// Each field seeds from the shared fetch and then holds the last value the server accepted —
	// which is what a rejected edit falls back to.
	readonly firstName = linkedSignal(() => this.driver()?.firstName ?? '');
	readonly lastName = linkedSignal(() => this.driver()?.lastName ?? '');
	readonly zipCode = linkedSignal(() => this.driver()?.address.zipCode ?? '');
	readonly city = linkedSignal(() => this.driver()?.address.city ?? '');
	readonly street = linkedSignal(() => this.driver()?.address.street ?? '');
	readonly gender = linkedSignal(() => this.driver()?.gender ?? '');

	/**
	 * The component holds a `Date`; the wire model is an ISO-8601 string with the SERVER's offset.
	 * The adapter owns that conversion in both directions — hand-rolled parsing is precisely where
	 * an off-by-one-day timezone bug lands (P-4 pins `TZ=UTC` for capture and replay).
	 */
	readonly birthDate = linkedSignal(() => {
		const wireValue = this.driver()?.dateOfBirth;
		return wireValue === undefined ? null : this.dateAdapter.deserialize(wireValue);
	});

	commitFirstName(value: string): void {
		this.commitText(value, this.firstName, (firstName) =>
			this.driverService.updateFirstName(firstName),
		);
	}

	commitLastName(value: string): void {
		this.commitText(value, this.lastName, (lastName) =>
			this.driverService.updateLastName(lastName),
		);
	}

	commitZipCode(value: string): void {
		this.commitText(value, this.zipCode, (zipCode) => this.driverService.updateZipCode(zipCode));
	}

	commitCity(value: string): void {
		this.commitText(value, this.city, (city) => this.driverService.updateCity(city));
	}

	commitStreet(value: string): void {
		this.commitText(value, this.street, (street) => this.driverService.updateStreet(street));
	}

	/**
	 * ⛔ DR-04 — the only two commit points are the datepicker closing and the input blurring.
	 * `driver.html:23-35` has no `updateOn: 'blur'`, so today every keystroke that happens to parse
	 * as a date fires its own PUT and its own full rules evaluation: the golden records THREE for
	 * one typed date.
	 *
	 * Both commit paths run for a calendar pick — closing hands focus back, which blurs the input —
	 * so the unchanged guard is what keeps that from costing two PUTs. An invalid or empty field is
	 * never sent: `Date.prototype.toJSON` renders an invalid date as `null`, and a null birth date
	 * is the DR-02 failure in another shape, silent because no handler surfaces it.
	 */
	commitBirthDate(value: Date | null): void {
		if (
			value === null ||
			!this.dateAdapter.isValid(value) ||
			this.dateAdapter.sameDate(value, this.birthDate())
		) {
			return;
		}
		// ⛔ A `Date` OBJECT, never a string. `DriverApi.updateBirthDate` is `@RequestBody Date`;
		// a string body would go out as text/plain and Jackson would answer 415.
		this.driverService.updateBirthDate(value).subscribe(() => {
			this.birthDate.set(value);
			this.store.recalculate();
		});
	}

	/**
	 * ⛔ DR-08 ordering — the value is set BEFORE the PUT is issued. AngularJS achieved that only
	 * implicitly: `btnRadio` and `ngClick` sat on the same element at equal priority, so
	 * alphabetical directive ordering alone decided that the model was written before the click
	 * handler read it.
	 *
	 * The other half of DR-08 needs no code at all: `MatButtonToggleGroup` does not emit `change`
	 * when the already-selected toggle is re-clicked, so the redundant PUT and the full
	 * recalculation behind it disappear by construction.
	 */
	onGender(change: MatButtonToggleChange): void {
		const gender: string = change.value;
		this.gender.set(gender);
		this.driverService.updateGender(gender).subscribe(() => this.store.recalculate());
	}

	/**
	 * The five text fields commit on blur, reproducing `ng-model-options="{updateOn: 'blur'}"`.
	 *
	 * The unchanged check reproduces what `ng-change` gave for free: it fires only when the model
	 * actually changes, so blurring a field nobody edited must not spend a PUT and a full Higson
	 * evaluation. Without it, tabbing through the form would multiply request volume against no
	 * register row at all.
	 *
	 * ⛔ The value is passed through verbatim: never re-serialised, never wrapped in quotes, and
	 * with no hand-set `Content-Type`. A string body goes out as `text/plain`, which is exactly
	 * what `@RequestBody String` wants; a quoted body would be stored WITH its quotes.
	 */
	private commitText(
		value: string,
		field: WritableSignal<string>,
		put: (value: string) => Observable<void>,
	): void {
		if (value === field()) {
			return;
		}
		put(value).subscribe(() => {
			field.set(value);
			this.store.recalculate();
		});
	}
}
