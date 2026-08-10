import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

/**
 * The one loading indicator in the application.
 *
 * It has no state and no inputs on purpose: whether it is on screen at all is decided by
 * `app.html`'s `@if (pendingRequests.count() > 0)`, and that counter is fed by
 * `pendingRequestsInterceptor` — the same "count every request in and out again" mechanism the
 * AngularJS `ngLoadingSpinner` `$httpProvider` interceptor implemented.
 *
 * ⛔ There is no per-component loading state anywhere in this application and none must be added:
 * no `ctrl.loading`, no disabled-while-saving, no optimistic UI. The screen has exactly one
 * loading surface and it is this one.
 *
 * `role="status"` gives it the polite live region it never had — the AngularJS spinner was an
 * empty `<span>`, invisible to assistive technology. The complementary `aria-busy` sits on the
 * shell host, on the region that is actually being updated.
 */
@Component({
	selector: 'app-spinner',
	imports: [MatProgressSpinner],
	templateUrl: './spinner.html',
	styleUrl: './spinner.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
	host: {
		'data-testid': 'app-spinner',
		role: 'status',
		'aria-label': 'Loading',
	},
})
export class Spinner {}
