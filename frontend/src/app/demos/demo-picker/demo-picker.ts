import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { DEMOS, Demo } from '../demo-catalog';
import { DemoSelection } from '../../core/demo-selection';

/**
 * The landing screen: one tile per demo in `DEMOS`.
 *
 * Presentational apart from the click — it reads the catalogue and writes the selection, and holds
 * no state of its own.
 */
@Component({
	selector: 'app-demo-picker',
	templateUrl: './demo-picker.html',
	styleUrl: './demo-picker.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DemoPicker {
	private readonly selection = inject(DemoSelection);

	readonly demos = DEMOS;

	open(demo: Demo): void {
		if (!demo.available) {
			return;
		}
		this.selection.open(demo.code);
	}
}
