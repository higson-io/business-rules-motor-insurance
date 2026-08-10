import { Injectable, signal } from '@angular/core';

/**
 * Reference count of in-flight HTTP requests. The global spinner is shown while it is above zero.
 * It replaces the `ngLoadingSpinner` `$httpProvider` interceptor of the AngularJS build, which was
 * implicit behaviour named nowhere in the templates.
 */
@Injectable({ providedIn: 'root' })
export class PendingRequests {
	private readonly pending = signal(0);

	readonly count = this.pending.asReadonly();

	increment(): void {
		this.pending.update((n) => n + 1);
	}

	decrement(): void {
		this.pending.update((n) => n - 1);
	}
}
