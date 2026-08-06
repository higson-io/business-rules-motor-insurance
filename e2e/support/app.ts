import { expect, type Locator, type Page } from '@playwright/test';
import { RequestLog } from './request-log';

/**
 * The frontend-agnostic page harness.
 *
 * SELECTOR POLICY - BINDING (spec.md 8.3, plan step 2.3). Elements may be located ONLY by:
 *   1. accessible role + accessible name
 *   2. label text
 *   3. `data-testid`
 *   4. visible text content
 *
 * FORBIDDEN: Bootstrap class names, `.common-input-default__input`, `mat-*` class names,
 * `nth-child`, DOM-structure paths. A selector that survives only one of the two frontends
 * defeats the entire golden strategy, because the whole point is that these spec files run
 * unmodified against both.
 *
 * WHY (1) AND (2) ARE UNAVAILABLE FOR THE FORM CONTROLS. The AngularJS templates render
 * every field label as `<div class="common-input-default__label">First name</div>` - a
 * SIBLING div, not a `<label>`, with no `for`, no `id`, no `aria-label`. None of the 16
 * form controls therefore has an accessible name at all, and neither `getByRole(..., {name})`
 * nor `getByLabel()` can reach any of them. spec.md 8.3's assumption that "everything with a
 * visible label is reachable by role/label in both frontends without touching anything" does
 * not hold for this codebase. Rule (3) is the sanctioned fallback for elements with no
 * accessible name, so the controls carry `data-testid` on both sides. The Angular components
 * built in G5-G8 MUST reproduce these exact ids.
 */
export const TESTID = {
	spinner: 'app-spinner',

	firstName: 'driver-first-name',
	lastName: 'driver-last-name',
	dateOfBirth: 'driver-date-of-birth',
	dateOfBirthToggle: 'driver-date-of-birth-toggle',
	genderMale: 'driver-gender-male',
	genderFemale: 'driver-gender-female',
	zipCode: 'driver-zip-code',
	city: 'driver-city',
	street: 'driver-street',

	accidents: 'driver-accidents',
	tickets: 'driver-tickets',
	licenceAge: 'driver-licence-age',

	productionYear: 'vehicle-production-year',
	make: 'vehicle-make',
	type: 'vehicle-type',
	model: 'vehicle-model',

	errorBanner: 'quote-error-banner',
	variantCard: 'variant-card',
	variantCode: 'variant-code',
	variantPremiumBeforeDiscounts: 'variant-premium-before-discounts',
	variantPremium: 'variant-premium',

	coverageRow: 'coverage-row',
	coverageHeader: 'coverage-header',
	coverageLimit: 'coverage-limit',
	coveragePremium: 'coverage-premium',

	discountRow: 'discount-row',
	discountName: 'discount-name',
	discountValue: 'discount-value'
} as const;

export const BASE_URL = process.env.APP_BASE_URL ?? 'http://localhost:48080/demo/';

/**
 * Opens the application on a fresh browser context and returns a request log that is already
 * attached, so the startup traffic (which DR-09 asserts on) is captured from the first byte.
 */
export async function openApp(page: Page): Promise<RequestLog> {
	const log = RequestLog.attach(page, BASE_URL);
	await page.goto(BASE_URL);
	await expect(
		page.getByTestId(TESTID.variantCard).first(),
		'the quote panel must render before any flow starts'
	).toBeVisible();
	await settle(page);
	return log;
}

/**
 * Waits until the application has finished reacting to an interaction.
 *
 * The trailing quiet period is load-bearing: several assertions prove that a request did NOT
 * happen (DR-08's re-clicked gender toggle, and above all DR-13's "zero GET quote on a
 * year / make / type change"). Without a margin after the network goes quiet, a late request
 * would simply be missed and the assertion would pass for the wrong reason.
 */
export async function settle(page: Page): Promise<void> {
	await page.waitForLoadState('networkidle');
	await page.waitForTimeout(400);
}

/** Commits a text field the way a user does: focus, replace the value, blur. */
export async function typeAndBlur(field: Locator, value: string): Promise<void> {
	await field.click();
	await field.fill(value);
	await field.blur();
}

/**
 * Picks a dropdown entry by its VISIBLE LABEL, on either frontend.
 *
 * The AngularJS build renders a native `<select>`; the Angular build renders a
 * `<mat-select>` that opens a CDK overlay (spec.md 4.7). The spec files must stay
 * byte-identical across both, so the difference is absorbed here - by feature detection on
 * the element itself, never by a `mat-*` class name. The overlay entries are located by
 * accessible role + name, which is selector-policy rule 1.
 */
export async function chooseOption(page: Page, testid: string, label: string): Promise<void> {
	const control = page.getByTestId(testid);
	if (await isNativeSelect(control)) {
		await control.selectOption({ label });
		return;
	}
	await control.click();
	await page.getByRole('option', { name: label, exact: true }).click();
}

/** The selectable entries of a dropdown, in order, on either frontend. */
export async function optionLabels(page: Page, testid: string): Promise<string[]> {
	const control = page.getByTestId(testid);
	if (await isNativeSelect(control)) {
		return (await control.getByRole('option').allTextContents()).map((t) => t.trim());
	}
	await control.click();
	const labels = (await page.getByRole('option').allTextContents()).map((t) => t.trim());
	await page.keyboard.press('Escape');
	return labels;
}

/** The label currently shown by a dropdown, on either frontend. */
export async function selectedLabel(page: Page, testid: string): Promise<string> {
	const control = page.getByTestId(testid);
	if (await isNativeSelect(control)) {
		return control.evaluate(
			(el) => (el as HTMLSelectElement).selectedOptions[0]?.textContent?.trim() ?? ''
		);
	}
	return (await control.innerText()).trim();
}

async function isNativeSelect(control: Locator): Promise<boolean> {
	return control.evaluate((el) => el.tagName.toLowerCase() === 'select');
}

/**
 * Everything the page currently RENDERS, as text.
 *
 * `innerText` honours CSS visibility, so this is a markup-free way to ask "is this string on
 * screen?" - which is how the coverage popover (C-8) is asserted without naming a single
 * class or DOM path belonging to either tooltip implementation.
 */
export async function renderedText(page: Page): Promise<string> {
	return page.evaluate(() => document.body.innerText);
}

/** The visible text of every element carrying `testid`, in document order. */
export async function textsOf(scope: Page | Locator, testid: string): Promise<string[]> {
	return (await scope.getByTestId(testid).allInnerTexts()).map((t) => t.trim());
}

/**
 * The server seeds `dateOfBirth` as "today - 40 years, start of day, SYSTEM timezone"
 * (`Motor.createDriver`). Pinned to UTC by P-4, this is deterministic - but it moves with the
 * calendar, so goldens store the token `<today-40y>` rather than a literal that would make a
 * replay on any other day report a false regression.
 */
export function defaultDateOfBirth(): string {
	const now = new Date();
	const year = now.getUTCFullYear() - 40;
	const month = String(now.getUTCMonth() + 1).padStart(2, '0');
	const day = String(now.getUTCDate()).padStart(2, '0');
	return `${year}-${month}-${day}`;
}
