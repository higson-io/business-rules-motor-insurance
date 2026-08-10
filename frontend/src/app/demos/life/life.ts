import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * The life-insurance demo — a deliberately empty screen.
 *
 * It exists so the shape of adding an industry is visible and already wired: a catalogue entry in
 * `demo-catalog.ts`, a component here, and a branch in `app.html`'s switch. Nothing else in the
 * application needed to change for this one to appear, which is the point.
 *
 * When it is built it will need its own state and services, exactly as the motor demo has
 * `QuoteStore` and the four `*Service` classes — HTTP stays out of components
 * (`standards/frontend/services.md`).
 */
@Component({
	selector: 'app-life',
	templateUrl: './life.html',
	styleUrl: './life.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Life {}
