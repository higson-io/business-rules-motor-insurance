import { expect, test } from '@playwright/test';
import { openApp, settle, TESTID, typeAndBlur } from '../support/app';
import { Golden } from '../support/golden';
import { byStage } from '../support/stage';

/**
 * FLOW 6 - the only error branch in the application (`variants.js:35-38`), and the fourteen
 * mutation sites that have none.
 *
 * The failed-PUT case is exercised FIRST, while the session still holds a valid quote. The
 * failed-`GET quote` case blanks the first name, which makes `QuoteValidator` reject the quote
 * for the remaining life of the session - so it has to come last.
 */

const golden = new Golden('error-paths');

test.afterAll(() => golden.write());

test('a failed mutation and a failed quote surface as recorded', async ({ page }) => {
	const log = await openApp(page);
	const banner = page.getByTestId(TESTID.errorBanner);
	const observed: Record<string, unknown> = {};

	await test.step('a failed PUT (DR-06)', async () => {
		// Forcing the failure at the network layer is what the DR-06 acceptance criterion
		// prescribes: "Playwright with a forced 500 on PUT driver/city".
		await page.route('**/driver/address/city', async (route) => {
			if (route.request().method() !== 'PUT') {
				await route.fallback();
				return;
			}
			await route.fulfill({ status: 500, contentType: 'application/json', body: '{}' });
		});

		log.reset();
		await typeAndBlur(page.getByTestId(TESTID.city), 'Gdansk');
		await settle(page);

		expect(
			log.count('PUT', 'driver/address/city'),
			'DR-06: the failed mutation is issued once and never retried (C-7 keeps the fix narrow)'
		).toBe(1);
		expect(
			log.count('GET', 'quote'),
			'a failed mutation must not trigger a recalculation - `.then(emitRecalculate)` runs on 2xx only'
		).toBe(0);

		const visible = await banner.isVisible();
		const text = visible ? (await banner.innerText()).trim() : null;
		observed['failedPut'] = { bannerVisible: visible, bannerText: text, requests: log.summary() };

		// DR-06: all 14 mutation sites have a success callback only, so today a failed PUT is
		// swallowed in complete silence. The target adds one HttpInterceptorFn that surfaces it
		// through the same banner - with no retry and no rollback.
		if (byStage('DR-06', { pre: 'silent', post: 'banner' }) === 'banner') {
			await expect(banner, 'DR-06: a failed mutation surfaces through the banner').toBeVisible();
			expect(text, 'DR-06: the banner carries text, not an empty box').toBeTruthy();
		} else {
			await expect(
				banner,
				'DR-06: today a failed mutation is swallowed and nothing is shown'
			).toBeHidden();
		}

		await page.unroute('**/driver/address/city');
	});

	await test.step('a failed GET quote (DR-07, DR-14)', async () => {
		const cardsBefore = await page.getByTestId(TESTID.variantCard).count();
		expect(cardsBefore, 'the quote is still rendered before the failure is provoked').toBe(3);

		log.reset();
		// A REAL server-side failure, not a mocked one: blanking the first name makes
		// `QuoteValidator` reject the quote. That matters, because DR-14 changes what the SERVER
		// returns - a mocked response would keep returning the pre-migration body forever and the
		// DR-14 branch below would prove nothing.
		await typeAndBlur(page.getByTestId(TESTID.firstName), '');
		await settle(page);

		expect(
			log.count('GET', 'quote'),
			'blanking the first name commits and triggers exactly one failing recalculation'
		).toBe(1);

		const quoteResponses = log.allResponses().filter((r) => r.path === 'quote');
		const cardsAfter = await page.getByTestId(TESTID.variantCard).count();
		const bannerText = (await banner.innerText()).trim();
		observed['failedQuote'] = {
			responses: quoteResponses,
			cardsAfter,
			bannerVisible: await banner.isVisible(),
			bannerText
		};

		await expect(
			banner,
			'the error banner is shown on both frontends when the quote fails'
		).toBeVisible();

		// DR-07: `variants.js:35-38` sets `errorOccured` but never clears `ctrl.variants`, so the
		// previous vehicle's cards stay on screen underneath the red banner.
		expect(
			cardsAfter,
			'DR-07: today the stale variant cards remain visible beneath the banner'
		).toBe(byStage('DR-07', { pre: 3, post: 0 }));

		// DR-14 / C-6: today `GlobalExceptionHandler` is a plain `@ControllerAdvice` whose handlers
		// return `String` without `@ResponseBody`, so Spring treats the value as a view name, view
		// resolution fails, and the 404 overrides `@ResponseStatus(BAD_REQUEST)`. The body carries
		// no `message` key at all, so `error.data.message` is `undefined` and the banner renders
		// EMPTY. The backend fix in G10 turns this into 400 + `{"message": ...}`.
		expect(
			quoteResponses.map((r) => r.status),
			'DR-14: the failing quote status - 404 today, 400 after the @RestControllerAdvice fix'
		).toEqual([byStage('DR-14', { pre: 404, post: 400 })]);

		if (byStage('DR-14', { pre: 'empty', post: 'message' }) === 'message') {
			expect(bannerText, 'DR-14: the banner shows the QuoteValidator message').toBe(
				'Driver first name not supplied.'
			);
		} else {
			expect(
				bannerText,
				'DR-14: today the response has no `message` key, so the red banner renders with no text at all'
			).toBe('');
		}
	});

	golden.record('errorPaths', observed);
	await golden.screenshot(page, 'quote-failure');
});
