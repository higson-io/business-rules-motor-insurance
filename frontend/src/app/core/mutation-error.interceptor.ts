import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { QuoteStore } from './quote-store';
import { bodyMessageOf } from './http-error-message';

/**
 * Surfaces failed mutations through the existing error banner. Every mutation call site in the
 * AngularJS build had a success callback only, so a failed PUT was swallowed silently.
 *
 * ⛔ Deliberately narrow: no retry, no rollback of the optimistic local value, no new UI surface.
 * The message is cleared when the next mutation starts, so the banner always describes the most
 * recent attempt rather than becoming a permanent tombstone.
 */
export const mutationErrorInterceptor: HttpInterceptorFn = (request, next) => {
	if (request.method !== 'PUT') {
		return next(request);
	}

	const store = inject(QuoteStore);
	store.mutationError.set(null);

	return next(request).pipe(
		catchError((error: HttpErrorResponse) => {
			// The backend's own message when it sent one; otherwise the transport-level description,
			// so the banner never renders as an empty box.
			store.mutationError.set(bodyMessageOf(error) ?? error.message);
			return throwError(() => error);
		}),
	);
};
