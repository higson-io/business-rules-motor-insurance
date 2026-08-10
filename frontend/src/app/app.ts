import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { PendingRequests } from './core/pending-requests';
import { Sidebar } from './components/sidebar/sidebar';
import { Spinner } from './components/spinner/spinner';
import { Variants } from './components/variants/variants';

/**
 * The application shell — layout, and nothing else.
 *
 * ⛔ **DR-20**: the selector is `app-root`, never `main`. The AngularJS build registered its root
 * directive as the element `main` (`main.js:4`, `index.html:14`), which is a real HTML5 tag and an
 * invalid custom-element name.
 *
 * ⛔ `main.js:21-23` — the `$scope.$on('recalculate') → $scope.$broadcast('reloadQuote')` relay —
 * is **deleted, not ported**. It existed only because AngularJS had no shared state between
 * sibling directives; `QuoteStore` replaced the whole bus in G4. The class therefore holds one
 * injected counter and nothing else, and `app.html` is pure layout.
 *
 * ⛔ **ND-7**: no router. There is one screen, it is not addressable, and
 * `ApplicationConfig.addViewController("/")` stays sufficient and unchanged. ⛔ **ND-8**:
 * `Application.java` stays unchanged too — the WAR is deliberately not deployable to an external
 * Tomcat.
 */
@Component({
	selector: 'app-root',
	imports: [Sidebar, Spinner, Variants],
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
}
