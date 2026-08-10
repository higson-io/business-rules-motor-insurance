import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { QuoteDto } from '../models/quote';

/**
 * The single `QuoteApi` endpoint. Every call runs a full rules evaluation and validation server
 * side, which is why the quote is reloaded deliberately rather than on every field change.
 */
@Injectable({ providedIn: 'root' })
export class VariantService {
	private readonly http = inject(HttpClient);

	getQuote(): Observable<QuoteDto> {
		return this.http.get<QuoteDto>('quote');
	}
}
