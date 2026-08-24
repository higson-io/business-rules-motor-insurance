import { ChangeDetectionStrategy, Component } from '@angular/core';

import { Inputs } from '../../components/inputs/inputs';
import { Variants } from '../../components/variants/variants';
import { MotoApk } from './apk/moto-apk';

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
	imports: [Inputs, MotoApk, Variants],
	templateUrl: './moto.html',
	styleUrl: './moto.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Moto {}
