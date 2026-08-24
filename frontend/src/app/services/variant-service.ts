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

	/**
	 * ⛔ Wysyła OBIEKT `Date`. `QuoteApi.setStartDate` to `@RequestBody Date`; string przyszedłby
	 * jako `text/plain` i Jackson odpowiada 415. Ta sama zasada co w demo życiowym i majątkowym.
	 */
	updateStartDate(startDate: Date): Observable<void> {
		return this.http.put<void>('quote/startDate', startDate);
	}

	/**
	 * ⛔ Surowy string, bez `JSON.stringify`. Endpoint przyjmuje `@RequestBody String`, więc
	 * cudzysłowy z JSON-a zapisałyby się jako część odpowiedzi.
	 */
	updateApkAnswer(code: string, value: string): Observable<void> {
		return this.http.put<void>(`quote/apk/${code}`, value);
	}

	updateApkRefused(code: string, refused: boolean): Observable<void> {
		return this.http.put<void>(`quote/apk/${code}/refused`, refused);
	}
}
