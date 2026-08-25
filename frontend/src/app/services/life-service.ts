import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { LifeLineCode, LifeQuoteDto, PaymentFrequency } from '../models/life';

/**
 * Every `LifeApi` endpoint, one method per endpoint. URLs are relative with no leading slash so the
 * SPA keeps working under any servlet context path.
 *
 * ⛔ The same two opposite body-encoding rules as `DriverService`. `HttpClient` branches on the
 * JavaScript type of the body: a `string` goes on the wire verbatim as `text/plain`, anything else
 * is serialised as JSON. Both branches are load-bearing — see `updateStartDate` versus the two
 * string setters.
 */
@Injectable({ providedIn: 'root' })
export class LifeService {
	private readonly http = inject(HttpClient);

	getQuote(): Observable<LifeQuoteDto> {
		return this.http.get<LifeQuoteDto>('life');
	}

	// ⛔ Raw strings, no wrapping. A quoted body is stored WITH its quotes, so `GROUP` would come
	// back as `"GROUP"` and never match a rule.

	updateLine(lineCode: LifeLineCode): Observable<void> {
		return this.http.put<void>('life/line', lineCode);
	}

	updatePaymentFrequency(frequency: PaymentFrequency): Observable<void> {
		return this.http.put<void>('life/policy/paymentFrequency', frequency);
	}

	/**
	 * ⛔ Takes a `Date` OBJECT, never a string. `LifeApi.setStartDate` is `@RequestBody Date`, so a
	 * `Date` takes the object branch and is serialised to a quoted ISO-8601 string as
	 * `application/json`. A string would arrive as `text/plain` and Jackson rejects it with 415.
	 *
	 * This one date does more than filter a form: the backend feeds it to `engine.setEffectiveDate`,
	 * so moving it across the region timeline switches the tariff version. It is the control that
	 * makes date-driven configuration visible.
	 */
	updateStartDate(startDate: Date): Observable<void> {
		return this.http.put<void>('life/policy/startDate', startDate);
	}

	updateAge(age: number): Observable<void> {
		return this.http.put<void>('life/insured/age', age);
	}

	updateBirthDate(birthDate: Date): Observable<void> {
		return this.http.put<void>('life/insured/birthDate', birthDate);
	}

	updateMainSum(mainSum: number): Observable<void> {
		return this.http.put<void>('life/policy/mainSum', mainSum);
	}

	updateSmoker(smoker: boolean): Observable<void> {
		return this.http.put<void>('life/insured/smoker', smoker);
	}

	updateHeight(heightCm: number): Observable<void> {
		return this.http.put<void>('life/insured/heightCm', heightCm);
	}

	updateWeight(weightKg: number): Observable<void> {
		return this.http.put<void>('life/insured/weightKg', weightKg);
	}

	updateOccupationClass(occupationClass: number): Observable<void> {
		return this.http.put<void>('life/insured/occupationClass', occupationClass);
	}

	updateMonthlyIncome(monthlyIncome: number): Observable<void> {
		return this.http.put<void>('life/insured/monthlyIncome', monthlyIncome);
	}

	updateRiderSelected(code: string, selected: boolean): Observable<void> {
		return this.http.put<void>(`life/rider/${code}/selected`, selected);
	}

	updateRiderSum(code: string, sum: number): Observable<void> {
		return this.http.put<void>(`life/rider/${code}/sum`, sum);
	}

	updateHeadcount(headcount: number): Observable<void> {
		return this.http.put<void>('life/group/headcount', headcount);
	}

	updateAverageAge(averageAge: number): Observable<void> {
		return this.http.put<void>('life/group/averageAge', averageAge);
	}

	updateIndustryClass(industryClass: number): Observable<void> {
		return this.http.put<void>('life/group/industryClass', industryClass);
	}

	updateParticipation(participation: number): Observable<void> {
		return this.http.put<void>('life/group/participation', participation);
	}

	updateIncludeSpouse(includeSpouse: boolean): Observable<void> {
		return this.http.put<void>('life/group/includeSpouse', includeSpouse);
	}

	updateIncludeAdultChild(includeAdultChild: boolean): Observable<void> {
		return this.http.put<void>('life/group/includeAdultChild', includeAdultChild);
	}

	/**
	 * Ilu pracowników wybrało dany wariant.
	 *
	 * ⛔ Zero USUWA pozycję po stronie serwera, a nie zapisuje zera — i to jest jedyny sposób,
	 * żeby wrócić z fazy programu do fazy oferty. Pusta lista przystąpień jest tym, co te dwie
	 * fazy odróżnia (D-2).
	 */
	/** ⛔ Surowy string, bez `JSON.stringify` — endpoint przyjmuje `@RequestBody String`. */
	updateApkAnswer(code: string, value: string): Observable<void> {
		return this.http.put<void>(`life/apk/${code}`, value);
	}

	updateApkRefused(code: string, refused: boolean): Observable<void> {
		return this.http.put<void>(`life/apk/${code}/refused`, refused);
	}

	updateEnrolment(variantCode: string, count: number): Observable<void> {
		return this.http.put<void>(`life/group/enrolment/${variantCode}`, count);
	}
}
