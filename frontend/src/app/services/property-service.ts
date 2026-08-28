import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { PropertyQuoteDto } from '../models/property';

/**
 * Every `PropertyApi` endpoint, one method per endpoint. URLs are relative with no leading slash
 * so the SPA keeps working under any servlet context path.
 *
 * ⛔ The same two opposite body-encoding rules as `LifeService`. `HttpClient` branches on the
 * JavaScript type of the body: a `string` goes on the wire verbatim as `text/plain`, anything else
 * is serialised as JSON. Both branches are load-bearing here too.
 *
 * ⛔ There is no method per question. The answer endpoint takes the question code as a path
 * segment because the catalogue is configuration — a method per question would mean a build every
 * time the regulator adds one, which is the exact claim this demo exists to disprove.
 */
@Injectable({ providedIn: 'root' })
export class PropertyService {
	private readonly http = inject(HttpClient);

	getQuote(): Observable<PropertyQuoteDto> {
		return this.http.get<PropertyQuoteDto>('property');
	}

	/**
	 * ⛔ Takes a `Date` OBJECT, never a string. `PropertyApi.setStartDate` is `@RequestBody Date`,
	 * so a `Date` takes the object branch and is serialised to a quoted ISO-8601 string as
	 * `application/json`. A string would arrive as `text/plain` and Jackson rejects it with 415.
	 *
	 * This one date does more than filter a form: the backend feeds it to
	 * `HigsonEngine.setEffectiveDate`, so moving it across 1 July 2027 switches the APK regime and
	 * the question catalogue at once.
	 */
	updateStartDate(startDate: Date): Observable<void> {
		return this.http.put<void>('property/policy/startDate', startDate);
	}

	updateDeductible(deductible: number): Observable<void> {
		return this.http.put<void>('property/policy/deductible', deductible);
	}

	// ⛔ Raw strings, no wrapping. A quoted body is stored WITH its quotes, so `FLAT` would come
	// back as `"FLAT"` and never match a rule.

	updateAnswer(code: string, value: string): Observable<void> {
		return this.http.put<void>(`property/answer/${code}`, value);
	}

	updateMultiAnswer(code: string, values: readonly string[]): Observable<void> {
		return this.http.put<void>(`property/answer/${code}/multi`, values.join(','));
	}

	updateRefused(code: string, refused: boolean): Observable<void> {
		return this.http.put<void>(`property/answer/${code}/refused`, refused);
	}

	updateSumBuilding(sum: number): Observable<void> {
		return this.http.put<void>('property/sum/building', sum);
	}

	updateVariant(variantCode: string): Observable<void> {
		return this.http.put<void>('property/variant', variantCode);
	}

	/**
	 * Wystawienie polisy albo cofnięcie do oferty.
	 *
	 * ⛔ `boolean`, nie łańcuch — endpoint bierze `@RequestBody boolean`, a nie tekst, więc tu
	 * NIE stosuje się reguły o surowych stringach z komentarza wyżej.
	 */
	updatePolicyIssued(issued: boolean): Observable<void> {
		return this.http.put<void>('property/policy/issued', issued);
	}
}
