import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { UpdateMakeDto, VehicleDto } from '../models/vehicle';

/**
 * The five `VehicleApi` endpoints. Each setter has a server-side cascade side effect that callers
 * must mirror locally: production year clears make, type and model; make clears type and model;
 * type clears model; model clears nothing.
 */
@Injectable({ providedIn: 'root' })
export class VehicleService {
	private readonly http = inject(HttpClient);

	getVehicle(): Observable<VehicleDto> {
		return this.http.get<VehicleDto>('vehicle');
	}

	updateProductionYear(productionYear: number): Observable<void> {
		return this.http.put<void>('vehicle/productionYear', productionYear);
	}

	updateMake(makeId: number, make: string): Observable<void> {
		const body: UpdateMakeDto = { makeId, make };
		return this.http.put<void>('vehicle/make', body);
	}

	updateType(typeId: number): Observable<void> {
		return this.http.put<void>('vehicle/type', typeId);
	}

	updateModel(modelId: number): Observable<void> {
		return this.http.put<void>('vehicle/model', modelId);
	}
}
