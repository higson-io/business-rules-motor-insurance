import { Injectable, signal } from '@angular/core';

import { DemoCode } from '../demos/demo-catalog';

/**
 * Which demo the user is looking at. `null` means the landing screen.
 *
 * ⛔ **ND-7 still holds — there is deliberately no router.** The selection is a signal, not a URL,
 * so the address bar never changes and neither does the document path. That is not laziness: every
 * API URL in this application is relative with no leading slash (`standards/frontend/services.md`)
 * and resolves against the document URL, and the WAR is served under three different context paths
 * (`/demo`, `/app`, `/`). A path-based route would silently re-anchor every one of those calls the
 * moment a route grew past one segment, and a deep link would 404 because
 * `ApplicationConfig.addViewControllers` maps only `"/"`.
 *
 * The cost is real and accepted: a demo cannot be linked to or bookmarked, and the browser's back
 * button does not step out of one. If that ever matters more than the above, the honest upgrade is
 * hash routing — it keeps the document path fixed, so the URL trap stays closed.
 */
@Injectable({ providedIn: 'root' })
export class DemoSelection {
	private readonly selected = signal<DemoCode | null>(null);

	/** `null` while the landing screen is showing. */
	readonly current = this.selected.asReadonly();

	open(code: DemoCode): void {
		this.selected.set(code);
	}

	/** Back to the landing screen. */
	close(): void {
		this.selected.set(null);
	}
}
