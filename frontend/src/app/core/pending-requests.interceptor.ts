import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';

import { PendingRequests } from './pending-requests';

/**
 * Counts every request in and out again. `finalize` fires on success, on error and on
 * cancellation, so the counter always returns to zero and the spinner can never stick.
 */
export const pendingRequestsInterceptor: HttpInterceptorFn = (request, next) => {
	const pending = inject(PendingRequests);
	pending.increment();
	return next(request).pipe(finalize(() => pending.decrement()));
};
