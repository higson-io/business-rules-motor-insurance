import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatOption } from '@angular/material/core';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatSelect } from '@angular/material/select';

import { QuoteStore } from '../../core/quote-store';
import { DictionaryEntryDto } from '../../models/dictionary-entry';
import { VehicleDto } from '../../models/vehicle';
import { DictionaryService } from '../../services/dictionary-service';
import { VehicleService } from '../../services/vehicle-service';

/**
 * Every numeric field of `VehicleDto` is a primitive `int` server-side, so "unset" arrives as `0`
 * and never as null. The whole cascade is built on that: `if (vehicle.makeId)` is what decides
 * whether a dependent dictionary is worth loading. Typing the model `number | null` would silently
 * break all three of those guards, which is why the blank state is `0` here and `null` only at the
 * `<mat-select>` binding, where the placeholder option needs a value of its own.
 */
const UNSET_VEHICLE: VehicleDto = {
	productionYear: 0,
	makeId: 0,
	make: '',
	typeId: 0,
	modelId: 0,
};

/**
 * The four-level dictionary cascade: production year → make → type → model.
 *
 * Each level's PUT has a server-side side effect that this component mirrors locally so the
 * dropdowns agree with the session aggregate: `productionYear` clears make, type and model;
 * `make` clears type and model; `type` clears model; `model` clears nothing.
 */
@Component({
	selector: 'app-vehicle',
	imports: [MatFormField, MatLabel, MatSelect, MatOption],
	templateUrl: './vehicle.html',
	styleUrl: './vehicle.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Vehicle {
	private readonly vehicleService = inject(VehicleService);
	private readonly dictionaryService = inject(DictionaryService);
	private readonly store = inject(QuoteStore);

	readonly vehicle = signal<VehicleDto>(UNSET_VEHICLE);

	readonly productionYearDictionary = signal<readonly DictionaryEntryDto[]>([]);
	readonly makeDictionary = signal<readonly DictionaryEntryDto[]>([]);
	readonly typeDictionary = signal<readonly DictionaryEntryDto[]>([]);
	readonly modelDictionary = signal<readonly DictionaryEntryDto[]>([]);

	/**
	 * What each `<mat-select>` shows. `0` means unset, and no dictionary entry ever carries it, so
	 * it maps to the `null` of the blank placeholder option.
	 */
	readonly selected = computed(() => {
		const vehicle = this.vehicle();
		return {
			productionYear: vehicle.productionYear || null,
			makeId: vehicle.makeId || null,
			typeId: vehicle.typeId || null,
			modelId: vehicle.modelId || null,
		};
	});

	constructor() {
		this.vehicleService.getVehicle().subscribe((vehicle) => {
			this.vehicle.set(vehicle);
			this.loadDictionaries(vehicle);
		});
	}

	/**
	 * `DictionaryEntryDto.code` is a String on the wire while the vehicle contract is `int`. The
	 * AngularJS template coerced it with an implicit unary `+` inside `ng-options`; here the
	 * conversion is explicit and happens in exactly one place.
	 */
	numericCode(entry: DictionaryEntryDto): number {
		return Number(entry.code);
	}

	onProductionYearChange(productionYear: number | null): void {
		if (productionYear === null) {
			// Same guard as DR-01 one level up: the blank placeholder carries no value to commit,
			// and `PUT vehicle/productionYear` with an empty body is the DR-02 500 in another shape.
			this.clearProductionYear();
			return;
		}
		this.vehicle.update((vehicle) => ({ ...vehicle, productionYear }));
		this.vehicleService.updateProductionYear(productionYear).subscribe(() => {
			this.clearMake();
			this.loadMakeDictionary(productionYear);
			// ⛔ DR-13 / ND-1: a production-year change must NOT reload the quote. The store's
			// `RELOADS_QUOTE` table is what enforces that; this component never decides for itself.
			this.store.vehicleFieldChanged('productionYear');
		});
	}

	onMakeChange(makeId: number | null): void {
		const make =
			makeId === null
				? undefined
				: this.makeDictionary().find((entry) => this.numericCode(entry) === makeId)?.name;

		// DR-01: `vehicle.js:61-63` reads `…filter(…)[0].name` with no guard, so picking the blank
		// placeholder makes `filter` return `[]` and throws an uncaught TypeError — with zero
		// requests issued, because the throw happens before the PUT. Guarding it keeps the request
		// count at zero and drops the console error.
		if (makeId === null || make === undefined) {
			this.clearMake();
			return;
		}

		this.vehicle.update((vehicle) => ({ ...vehicle, makeId, make }));
		this.vehicleService.updateMake(makeId, make).subscribe(() => {
			this.clearType();
			this.loadTypeDictionary(makeId);
			// ⛔ DR-13 / ND-1: a make change must NOT reload the quote.
			this.store.vehicleFieldChanged('make');
		});
	}

	onTypeChange(typeId: number | null): void {
		if (typeId === null) {
			this.clearType();
			return;
		}
		this.vehicle.update((vehicle) => ({ ...vehicle, typeId }));
		this.vehicleService.updateType(typeId).subscribe(() => {
			this.clearModel();
			this.loadModelDictionary(typeId);
			// ⛔ DR-13 / ND-1: a type change must NOT reload the quote.
			this.store.vehicleFieldChanged('type');
		});
	}

	onModelChange(modelId: number | null): void {
		if (modelId === null) {
			this.clearModel();
			return;
		}
		this.vehicle.update((vehicle) => ({ ...vehicle, modelId }));
		this.vehicleService.updateModel(modelId).subscribe(() => {
			// ⛔ DR-13 / ND-1: the model is the ONE level that reloads the quote. Until the user
			// gets back here the premium panel deliberately shows the previous vehicle's prices.
			this.store.vehicleFieldChanged('model');
		});
	}

	private loadDictionaries(vehicle: VehicleDto): void {
		this.dictionaryService
			.getProductionYearDictionary()
			.subscribe((entries) => this.productionYearDictionary.set(entries));

		// The falsy-`0` guards of `vehicle.js:41-49`: an unset level has no dictionary to ask for.
		if (vehicle.productionYear) {
			this.loadMakeDictionary(vehicle.productionYear);
		}
		if (vehicle.makeId) {
			this.loadTypeDictionary(vehicle.makeId);
		}
		if (vehicle.typeId) {
			this.loadModelDictionary(vehicle.typeId);
		}
	}

	private loadMakeDictionary(productionYear: number): void {
		this.dictionaryService
			.getMakeDictionary(productionYear)
			.subscribe((entries) => this.makeDictionary.set(entries));
	}

	private loadTypeDictionary(makeId: number): void {
		this.dictionaryService
			.getTypeDictionary(makeId)
			.subscribe((entries) => this.typeDictionary.set(entries));
	}

	private loadModelDictionary(typeId: number): void {
		this.dictionaryService
			.getModelDictionary(typeId)
			.subscribe((entries) => this.modelDictionary.set(entries));
	}

	/**
	 * DR-12: `clearDependantsVehicleFields` (`vehicle.js:102-107`) nulls the field values but leaves
	 * `typeDictionary` and `modelDictionary` populated, so both dropdowns keep offering the previous
	 * vehicle's entries. Clearing a level here always clears its dictionary with it.
	 */
	private clearProductionYear(): void {
		this.vehicle.update((vehicle) => ({ ...vehicle, productionYear: 0 }));
		this.makeDictionary.set([]);
		this.clearMake();
	}

	private clearMake(): void {
		this.vehicle.update((vehicle) => ({ ...vehicle, makeId: 0, make: '' }));
		this.clearType();
	}

	private clearType(): void {
		this.vehicle.update((vehicle) => ({ ...vehicle, typeId: 0 }));
		this.typeDictionary.set([]);
		this.clearModel();
	}

	private clearModel(): void {
		this.vehicle.update((vehicle) => ({ ...vehicle, modelId: 0 }));
		this.modelDictionary.set([]);
	}
}
