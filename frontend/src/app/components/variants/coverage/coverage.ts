import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';

import { HtmlPopover } from '../../../shared/html-popover/html-popover';

/** One coverage row inside a variant card. Purely presentational (`coverage.js:12-19`). */
@Component({
	selector: 'app-coverage',
	imports: [DecimalPipe, HtmlPopover],
	templateUrl: './coverage.html',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Coverage {
	readonly code = input.required<string>();
	readonly name = input.required<string>();
	/** HTML from the Higson rules, shown in the popover. Null for coverages that have none. */
	readonly description = input.required<string | null>();
	readonly limitLeft = input.required<number>();
	readonly limitRight = input.required<number | null>();
	readonly premium = input.required<number>();
}
