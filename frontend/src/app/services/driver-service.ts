import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { DriverDto } from '../models/driver';

/**
 * All eleven `DriverApi` endpoints, one method per endpoint. URLs stay relative with no leading
 * slash so the SPA keeps working under any servlet context path.
 *
 * ⛔ Two opposite body-encoding rules live in this one class. `HttpClient` branches on the
 * JavaScript type of the body: a `string` goes on the wire verbatim with `Content-Type:
 * text/plain`, anything else is serialised as JSON. Both branches are load-bearing here — see the
 * comments on `updateBirthDate` and on the six string setters.
 */
@Injectable({ providedIn: 'root' })
export class DriverService {
	private readonly http = inject(HttpClient);

	getDriver(): Observable<DriverDto> {
		return this.http.get<DriverDto>('driver');
	}

	/**
	 * ⛔ Takes a `Date` OBJECT, never a string. `DriverApi.updateBirthDate` is `@RequestBody Date`
	 * with no `required = false`, so a body is mandatory. A `Date` takes the object branch and is
	 * serialised to a quoted ISO-8601 string with `application/json` — byte-identical to what the
	 * AngularJS build sends. A string would take the string branch, arrive as `text/plain`, and
	 * Jackson would reject it with HTTP 415. This pulls the opposite way from the six setters below.
	 */
	updateBirthDate(birthDate: Date): Observable<void> {
		return this.http.put<void>('driver/birthDate', birthDate);
	}

	// ⛔ The six string setters below pass the raw value and set no Content-Type. Wrapping a value
	// to "be safe" is what corrupts it: a quoted body is stored with its quotes, so `Krakow` comes
	// back as `"Krakow"`. Spring's StringHttpMessageConverter accepts text/plain for
	// `@RequestBody String`, and none of the handlers declares `consumes`.

	updateFirstName(firstName: string): Observable<void> {
		return this.http.put<void>('driver/firstName', firstName);
	}

	updateGender(gender: string): Observable<void> {
		return this.http.put<void>('driver/gender', gender);
	}

	updateLastName(lastName: string): Observable<void> {
		return this.http.put<void>('driver/lastName', lastName);
	}

	updateZipCode(zipCode: string): Observable<void> {
		return this.http.put<void>('driver/address/zipCode', zipCode);
	}

	updateCity(city: string): Observable<void> {
		return this.http.put<void>('driver/address/city', city);
	}

	updateStreet(street: string): Observable<void> {
		return this.http.put<void>('driver/address/street', street);
	}

	updateAccidentCount(accidentCount: number): Observable<void> {
		return this.http.put<void>('driver/accidentCount', accidentCount);
	}

	updateTrafficTicketsCount(trafficTicketsCount: number): Observable<void> {
		return this.http.put<void>('driver/trafficTicketsCount', trafficTicketsCount);
	}

	updateLicenceObtainedAtAge(licenceObtainedAtAge: number): Observable<void> {
		return this.http.put<void>('driver/licenceObtainedAtAge', licenceObtainedAtAge);
	}
}
