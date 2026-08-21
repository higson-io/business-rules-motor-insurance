import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { DemoSelection } from './core/demo-selection';
import { DemoPicker } from './demos/demo-picker/demo-picker';
import { Life } from './demos/life/life';
import { Moto } from './demos/moto/moto';
import { Property } from './demos/property/property';
import { PendingRequests } from './core/pending-requests';
import { Spinner } from './components/spinner/spinner';

/**
 * The application shell — the menubar, the one loading indicator, and the switch between the
 * landing screen and whichever demo is open.
 *
 * ⛔ **DR-20**: the selector is `app-root`, never `main`. The AngularJS build registered its root
 * directive as the element `main` (`main.js:4`, `index.html:14`), which is a real HTML5 tag and an
 * invalid custom-element name.
 *
 * ⛔ `main.js:21-23` — the `$scope.$on('recalculate') → $scope.$broadcast('reloadQuote')` relay —
 * is **deleted, not ported**. It existed only because AngularJS had no shared state between
 * sibling directives; `QuoteStore` replaced the whole bus in G4.
 *
 * ⛔ **ND-7 still holds: no router.** The demo switch is a signal, not a URL — see `DemoSelection`
 * for why, and for what that costs. ⛔ **ND-8**: `Application.java` and
 * `ApplicationConfig.addViewController("/")` remain unchanged and remain sufficient.
 */
@Component({
	selector: 'app-root',
	imports: [DemoPicker, Life, Moto, Property, Spinner],
	templateUrl: './app.html',
	styleUrl: './app.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
	// The busy state belongs on the region being updated, which is the whole screen — there is
	// exactly one loading indicator and it covers everything (`accessibility.md`).
	host: { '[attr.aria-busy]': 'pendingRequests.count() > 0' },
})
export class App {
	/**
	 * The single, global, interceptor-driven loading mechanism (G4). The AngularJS build did the
	 * same thing through an `$httpProvider` interceptor that no template mentioned.
	 */
	readonly pendingRequests = inject(PendingRequests);

	private readonly selection = inject(DemoSelection);

	readonly demo = this.selection.current;

	backToDemos(): void {
		this.selection.close();
	}
}
