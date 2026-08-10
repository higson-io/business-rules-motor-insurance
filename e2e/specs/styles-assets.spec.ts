import { expect, test, type Page, type Response } from '@playwright/test';
import { BASE_URL, chooseOption, openApp, settle, TESTID, typeAndBlur } from '../support/app';
import { Golden } from '../support/golden';
import { byStage } from '../support/stage';

/**
 * G9 checks 1 and 2 — the styling group's two runtime assertions.
 *
 * ⛔ THIS SPEC WRITES NO GOLDEN. It is a pass/fail gate on the stylesheet itself, not a
 * behavioural capture, so it adds nothing to the `goldens/` directory diff that G11 adjudicates.
 * It nevertheless lives in `specs/` and honours the stage mechanism, because the whole harness
 * rule is that spec files stay byte-identical between capture and replay (`support/stage.ts`).
 *
 * ⚠️ Appearance is out of scope (C-9). Neither check looks at how anything LOOKS — check 1 asks
 * whether the browser could FETCH what the stylesheet asked for, and check 2 asks for one
 * computed property that the defect register names explicitly.
 */

/**
 * Step 9.6 — the "after" screenshots, written next to G2's `pre` set.
 *
 * ⛔ DOCUMENTATION, NEVER AN ASSERTION. `toHaveScreenshot()` appears in no spec in this suite and
 * must not: C-9 redesigned every control on the screen, so an image diff cannot distinguish the
 * intended redesign from a regression and would report the group's own deliverable as a failure.
 * These files exist so G13 can show what changed, and for no other purpose.
 *
 * Only `screenshot()` is called — never `write()` — so this spec contributes no JSON to the
 * `goldens/` directory that G11 diffs.
 */
const shots = new Golden('styles');

/** `url()` targets: the things a re-anchored stylesheet silently fails to find. */
const ASSET = /\.(woff2?|ttf|otf|eot|svg|png|jpe?g|gif|ico)(\?|$)/i;

interface AssetResponse {
	readonly url: string;
	readonly status: number;
}

/**
 * Records every same-origin font and image response.
 *
 * Same-origin only, deliberately: a third-party font host reaching us or not is not this
 * application's behaviour, and the point of C-10 is precisely that there is no longer such a
 * host to reach. A 404 on a self-hosted asset, by contrast, is invisible on screen — the browser
 * simply paints nothing — which is exactly why it needs an assertion rather than an eye.
 */
function recordAssets(page: Page): AssetResponse[] {
	const seen: AssetResponse[] = [];
	const origin = new URL(BASE_URL).origin;
	page.on('response', (response: Response) => {
		const url = response.url();
		if (url.startsWith(origin) && ASSET.test(url)) {
			seen.push({ url, status: response.status() });
		}
	});
	return seen;
}

test('CHECK 1 — no font or image request fails during a full flow', async ({ page }) => {
	const assets = recordAssets(page);
	await openApp(page);

	// Captured on the seeded state, BEFORE the flow below mutates anything: the flow deliberately
	// clears the vehicle cascade and leaves the quote in its error state, which documents nothing
	// about the styling. Two widths, because the one thing about this layout that is a behaviour
	// rather than a look is where it collapses — 992px, Bootstrap's own `md` breakpoint, kept.
	await shots.screenshot(page, 'styled-desktop');
	await page.setViewportSize({ width: 900, height: 1000 });
	await settle(page);
	await shots.screenshot(page, 'styled-below-992');
	await page.setViewportSize({ width: 1280, height: 720 });
	await settle(page);

	// The flow has to actually EXERCISE the url() references, because a stylesheet only fetches
	// what it paints. The logo and the four sidebar icons come with the first render; the popover
	// arrow is fetched only when a popover opens, so hovering a coverage row is load-bearing here.
	await test.step('exercise the deferred url() references', async () => {
		await page.getByTestId(TESTID.coverageRow).first().hover();
		await expect(
			page.getByTestId('html-popover-panel'),
			'the popover must open, otherwise its arrow image is never requested'
		).toBeVisible();
		await chooseOption(page, TESTID.productionYear, '2011');
		await settle(page);
		await typeAndBlur(page.getByTestId(TESTID.city), 'Houston');
		await settle(page);
	});

	const failures = assets.filter((a) => a.status >= 400 && a.status < 500);
	expect(
		failures,
		'G-E2/G-E3: every url() in the stylesheet must resolve at the served context path — a 404 here paints nothing and reports nothing'
	).toEqual([]);

	expect(
		assets.length,
		'the flow must have requested at least one asset, otherwise the check proves nothing'
	).toBeGreaterThan(0);
});

test('CHECK 2 — DR-15: a variant card is not clickable and carries no hover affordance', async ({
	page
}) => {
	await openApp(page);
	const card = page.getByTestId(TESTID.variantCard).first();

	// ⚠️ `auto` AND `default` both satisfy this, and the G9 acceptance criterion's literal wording
	// ("the computed cursor is `default`") does not. `cursor` is not an inherited-and-resolved
	// property: with no declaration at all, `getComputedStyle` reports the initial value `auto`,
	// and over a non-text block element `auto` paints exactly the same arrow as `default`. Adding
	// `cursor: default` to the stylesheet purely to make this string match would be a declaration
	// that changes not one pixel — dead CSS of precisely the kind this group exists to remove
	// (`minimal-implementation.md`). What DR-15 is about is the affordance, so that is what is
	// asserted: anything but a pointer.
	const cursor = await card.evaluate((el) => getComputedStyle(el).cursor);
	expect(
		byStage('DR-15', { pre: ['pointer'], post: ['auto', 'default'] }),
		'DR-15: the variant column advertised `cursor: pointer` and a hover highlight while carrying no click handler at all — a purely misleading affordance'
	).toContain(cursor);

	// The computed cursor alone would still pass if a hover rule reintroduced the affordance on
	// hover only, so the loaded stylesheets are scanned for such a rule directly.
	const hoverRules = await page.evaluate(() => {
		const found: string[] = [];
		const walk = (rules: CSSRuleList) => {
			for (const rule of Array.from(rules)) {
				if (rule instanceof CSSStyleRule && /variants__column[^ ,]*:hover/.test(rule.selectorText)) {
					found.push(rule.selectorText);
				}
				const nested = (rule as CSSGroupingRule).cssRules;
				if (nested) {
					walk(nested);
				}
			}
		};
		for (const sheet of Array.from(document.styleSheets)) {
			try {
				walk(sheet.cssRules);
			} catch {
				// a cross-origin sheet cannot be read, and carries none of our rules anyway
			}
		}
		return found;
	});

	expect(
		hoverRules.length,
		'DR-15: no hover-state rule may target the variant column'
	).toBe(byStage('DR-15', { pre: 1, post: 0 }));
});
