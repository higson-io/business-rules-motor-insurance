import { ChangeDetectionStrategy, Component } from '@angular/core';

import { Sidebar } from '../../components/sidebar/sidebar';
import { Variants } from '../../components/variants/variants';

/**
 * The motor-insurance demo — the screen the AngularJS application was, lifted out of `app.ts`
 * unchanged when the landing screen arrived.
 *
 * Pure layout, exactly as the shell was: the quote state lives in `QuoteStore`, and this component
 * injects nothing. Because it is created by an `@if` in `app.html`, none of the quote requests fire
 * until the demo is actually opened — the landing screen makes no calls at all.
 */
@Component({
	selector: 'app-moto',
	imports: [Sidebar, Variants],
	templateUrl: './moto.html',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Moto {}
