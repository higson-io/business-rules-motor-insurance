import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

/**
 * The floating panel of {@link HtmlPopover}. It exists only so the CDK Overlay has a component to
 * attach; everything it does is render one trusted HTML fragment.
 */
@Component({
	selector: 'app-html-popover-panel',
	template: `<div data-testid="html-popover-panel" [innerHTML]="safeHtml()"></div>`,
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HtmlPopoverPanel {
	private readonly sanitizer = inject(DomSanitizer);

	readonly html = input.required<string>();

	/**
	 * ⚠️ AC-S8 — the ONLY `bypassSecurity*` call in the application, and it must stay that way.
	 *
	 * The value is `CoverageDto.description`, which the Higson rules engine produces as HTML and the
	 * backend passes through unchanged (`adapter/CoverageAdapter.java` → the `DESCRIPTION` context
	 * path). It is authored content from the rules snapshot, never user input and never anything the
	 * browser sends us, so it is trusted at its source. Angular's default sanitizer would strip parts
	 * of that markup — and `MatTooltip`, being text-only, would drop it entirely. Both would be
	 * FUNCTIONAL regressions (C-8), not cosmetic ones.
	 *
	 * If a future change ever routes user-supplied text through this input, this line becomes an XSS
	 * hole and must be deleted rather than worked around.
	 */
	readonly safeHtml = computed(() => this.sanitizer.bypassSecurityTrustHtml(this.html()));
}
