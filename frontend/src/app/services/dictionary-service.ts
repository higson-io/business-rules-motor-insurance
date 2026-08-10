import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { DictionaryEntryDto } from '../models/dictionary-entry';

/**
 * The four `DictionaryApi` lookups that drive the vehicle cascade. Query parameters go in the
 * `params` option object; they are never concatenated into the URL.
 */
@Injectable({ providedIn: 'root' })
export class DictionaryService {
	private readonly http = inject(HttpClient);

	getProductionYearDictionary(): Observable<DictionaryEntryDto[]> {
		return this.http.get<DictionaryEntryDto[]>('dictionaries/productionYear');
	}

	getMakeDictionary(productionYear: number): Observable<DictionaryEntryDto[]> {
		return this.http.get<DictionaryEntryDto[]>('dictionaries/make', {
			params: { productionYear },
		});
	}

	getTypeDictionary(makeId: number): Observable<DictionaryEntryDto[]> {
		return this.http.get<DictionaryEntryDto[]>('dictionaries/type', { params: { makeId } });
	}

	getModelDictionary(typeId: number): Observable<DictionaryEntryDto[]> {
		return this.http.get<DictionaryEntryDto[]>('dictionaries/model', { params: { typeId } });
	}
}
