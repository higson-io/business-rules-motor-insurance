import { HttpErrorResponse } from '@angular/common/http';

import { ErrorDto } from '../models/error';

/**
 * The message the backend itself put in the response body, or `null` when it sent none.
 *
 * Both error surfaces need this: the mutation-error interceptor (DR-06) and the quote error banner
 * (DR-07 / DR-14). They differ only in what they fall back to, so the fallback is the caller's
 * decision and never this function's.
 *
 * Until the C-6 fix lands, the failing endpoints answer with Spring's default error body, which has
 * no `message` key at all — which is precisely why `null` is a normal, expected result here rather
 * than an error condition.
 */
export function bodyMessageOf(failure: unknown): string | null {
	if (!(failure instanceof HttpErrorResponse)) {
		return null;
	}

	const body: unknown = failure.error;
	if (typeof body === 'object' && body !== null && 'message' in body) {
		const message = (body as ErrorDto).message;
		if (typeof message === 'string' && message.length > 0) {
			return message;
		}
	}
	return null;
}
