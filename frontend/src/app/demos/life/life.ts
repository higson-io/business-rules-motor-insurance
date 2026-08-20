import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { LifeStore } from '../../core/life-store';
import { LifeLineCode } from '../../models/life';
import { LifeService } from '../../services/life-service';
import { EffectiveDate } from './effective-date/effective-date';
import { Group } from './group/group';
import { Individual } from './individual/individual';

/** The two ways life cover is bought in Poland, and they are genuinely different products. */
export type LifePath = 'individual' | 'group';

/** The screen's word for a path, and the rules engine's word for the same thing. */
const LINE_CODES: Record<LifePath, LifeLineCode> = {
	individual: 'INDIVIDUAL',
	group: 'GROUP',
};

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
	imports: [EffectiveDate, Group, Individual],
	templateUrl: './life.html',
	styleUrl: './life.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Life {
	private readonly store = inject(LifeStore);
	private readonly lifeService = inject(LifeService);

	readonly path = signal<LifePath>('individual');

	/**
	 * ⛔ The switch is not local state. The backend holds the aggregate and decides which flow prices
	 * it, so the path has to reach `PUT life/line` — otherwise the group screen renders against an
	 * individually-priced quote, which has no available variants and shows an empty comparison.
	 */
	open(path: LifePath): void {
		if (this.path() === path) {
			return;
		}
		this.path.set(path);
		void this.store.commit(firstValueFrom(this.lifeService.updateLine(LINE_CODES[path])));
	}
}
