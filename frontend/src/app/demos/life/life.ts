import { ChangeDetectionStrategy, Component, signal } from '@angular/core';

import { Group } from './group/group';
import { Individual } from './individual/individual';

/** The two ways life cover is bought in Poland, and they are genuinely different products. */
export type LifePath = 'individual' | 'group';

/**
 * The life demo: two paths, sharing nothing but a heading.
 *
 * ⛔ Not a variant of one screen. Individual protection is assembled — a main contract plus riders,
 * priced on the person after a health questionnaire. Group cover is chosen — fixed variants, priced
 * on the employer's population, with no individual underwriting at all. They ask different
 * questions, price on different factors and answer with different shapes, so each path is its own
 * component and the switch below is the only thing they have in common.
 *
 * ⛔ NO ROUTER, consistent with the rest of the application (ND-7): the path is a signal, the
 * document URL never moves, and every relative asset and API reference keeps resolving under all
 * three context paths. See `DemoSelection` for the full reasoning.
 */
@Component({
	selector: 'app-life',
	imports: [Group, Individual],
	templateUrl: './life.html',
	styleUrl: './life.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Life {
	readonly path = signal<LifePath>('individual');

	open(path: LifePath): void {
		this.path.set(path);
	}
}
