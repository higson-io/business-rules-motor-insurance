import { ChangeDetectionStrategy, Component } from '@angular/core';

import { Driver } from '../driver/driver';
import { DriverHistory } from '../driver-history/driver-history';
import { Vehicle } from '../vehicle/vehicle';

/**
 * The left-hand input panel: a header and three labelled sections, each introducing one widget.
 *
 * ⛔ DR-16 — `ctrl.sidebarHidden` and `ctrl.toggleSidebar()` (`sidebar.js:21-27`) are NOT ported.
 * They were declared on the controller and then referenced nowhere in `sidebar.html`: leftovers of
 * a collapse feature that was removed without removing its state. The matching CSS leftovers
 * (`less/sidebar/sidebar.less:33-36`, `transition: 0.5s` / `opacity: 1`) go in G9.
 *
 * The class therefore has no members at all, which is the point: this is composition, not
 * behaviour.
 */
@Component({
	selector: 'app-inputs',
	imports: [Driver, Vehicle, DriverHistory],
	templateUrl: './inputs.html',
	styleUrl: './inputs.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Inputs {}
