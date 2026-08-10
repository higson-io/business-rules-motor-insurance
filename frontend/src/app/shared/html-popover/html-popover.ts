import { Directive, ElementRef, OnDestroy, inject, input } from '@angular/core';
import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';

import { HtmlPopoverPanel } from './html-popover-panel';

/**
 * Shows a floating panel of trusted HTML while the host element is hovered or focused.
 *
 * ⛔ WHY THIS EXISTS INSTEAD OF `MatTooltip` (C-8). `coverage.description` arrives from the Higson
 * rules as HTML. `MatTooltip` renders text only, so adopting it would silently drop the markup — a
 * functional regression, and therefore outside the appearance exemption. The AngularJS build used
 * `angular-tooltips` with `tooltip-template` (`coverage.html:1-6`), which is AngularJS-locked.
 *
 * Geometry reproduces `tooltip-side="right"`: the panel is anchored to the right edge of the host,
 * vertically centred.
 */
@Directive({
	selector: '[appHtmlPopover]',
	host: {
		'(mouseenter)': 'open()',
		'(mouseleave)': 'close()',
		// Keyboard parity: `angular-tooltips` was hover-only, which left the descriptions unreachable
		// without a mouse. Focus opens the same panel, Escape dismisses it, and the host only enters
		// the tab order when it actually has something to show.
		'(focusin)': 'open()',
		'(focusout)': 'close()',
		'(keydown.escape)': 'close()',
		'[attr.tabindex]': 'html() ? 0 : null',
	},
})
export class HtmlPopover implements OnDestroy {
	/** The trusted HTML fragment. `null` — three of the nine coverages — shows nothing at all. */
	readonly html = input.required<string | null>({ alias: 'appHtmlPopover' });

	private readonly overlay = inject(Overlay);
	private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
	private overlayRef?: OverlayRef;

	open(): void {
		const html = this.html();
		if (!html || this.overlayRef) {
			return;
		}

		this.overlayRef = this.overlay.create({
			positionStrategy: this.overlay
				.position()
				.flexibleConnectedTo(this.elementRef)
				.withPositions([
					{ originX: 'end', originY: 'center', overlayX: 'start', overlayY: 'center' },
				]),
			scrollStrategy: this.overlay.scrollStrategies.reposition(),
			panelClass: 'html-popover',
		});

		const panel = this.overlayRef.attach(new ComponentPortal(HtmlPopoverPanel));
		panel.setInput('html', html);
	}

	close(): void {
		this.overlayRef?.dispose();
		this.overlayRef = undefined;
	}

	ngOnDestroy(): void {
		this.close();
	}
}
