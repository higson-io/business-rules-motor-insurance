import { ApplicationRef, ErrorHandler, provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { DriverDto } from '../../models/driver';
import { APK_ABSENT } from '../../testing/shell-harness';
import { QuoteDto } from '../../models/quote';
import { QuoteStore } from '../../core/quote-store';
import { Variants } from './variants';

/**
 * ⛔ DELIBERATELY SCRAMBLED FIXTURE.
 *
 * The real backend already returns options by ascending `order` and coverages and discounts by
 * ascending `position` (G1 test 7). A fixture that mirrored that would pass whether or not a
 * client-side sort had been reintroduced, and would prove nothing. So every collection here is in
 * an order that a `position` / `order` sort would visibly change — that is what makes DR-05
 * falsifiable.
 */
const SCRAMBLED: QuoteDto = {
	...APK_ABSENT,
	options: [
		{
			code: 'GOLD',
			order: 3,
			premium: 1381.19,
			premiumBeforeDiscounts: 1401.19,
			coverages: [
				{
					position: 9,
					code: 'ERS',
					name: 'Emergency Road Service',
					limitLeft: 0,
					limitRight: null,
					premium: 17.8,
					description: null,
				},
				{
					position: 1,
					code: 'BI',
					name: 'Bodily Injury Liability',
					limitLeft: 60000,
					limitRight: 120000,
					premium: 201.88,
					description: 'Bodily injury liability.',
				},
			],
			discounts: [
				{ position: 5, code: 'LD', name: 'Loyalty Discount', value: 10 },
				{ position: 2, code: 'GD', name: 'Good Driver', value: 20 },
			],
		},
		{
			code: 'BRONZE',
			order: 1,
			premium: 264.78,
			premiumBeforeDiscounts: 284.78,
			coverages: [
				{
					position: 2,
					code: 'PD',
					name: 'Property Damage Liability',
					limitLeft: 25000,
					limitRight: null,
					premium: 100,
					description: null,
				},
			],
			discounts: [{ position: 2, code: 'GD', name: 'Good Driver', value: 20 }],
		},
	],
};

const DRIVER: DriverDto = {
	firstName: 'John',
	lastName: 'Potter',
	dateOfBirth: '1986-08-06T00:00:00.000+00:00',
	gender: 'M',
	address: { zipCode: '77566', city: 'Lake Jackson', street: 'Allwood St' },
	numberOfAccidents: 0,
	numberOfTickets: 0,
	licenceObtainedAtAge: 18,
};

/**
 * Records what the application would have reported to the browser, so an error thrown while
 * rendering cannot masquerade as a pass. It can: a template that throws renders nothing, and
 * "nothing" is indistinguishable from the zero cards DR-07 asks for unless the error is observed
 * too. `resource().value()` throws `ResourceValueError` in the error state, which makes that a live
 * failure mode here rather than a hypothetical one.
 */
class RecordingErrorHandler implements ErrorHandler {
	readonly errors: unknown[] = [];

	handleError(error: unknown): void {
		this.errors.push(error);
	}
}

describe('Variants', () => {
	let httpTesting: HttpTestingController;
	let fixture: ComponentFixture<Variants>;
	let errorHandler: RecordingErrorHandler;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				provideZonelessChangeDetection(),
				provideHttpClient(),
				provideHttpClientTesting(),
				{ provide: ErrorHandler, useClass: RecordingErrorHandler },
			],
		});
		errorHandler = TestBed.inject(ErrorHandler) as RecordingErrorHandler;
		httpTesting = TestBed.inject(HttpTestingController);
		fixture = TestBed.createComponent(Variants);
	});

	/** Renders the component with a successfully loaded quote and a settled driver resource. */
	async function loadQuote(): Promise<void> {
		fixture.detectChanges();
		httpTesting.expectOne('quote').flush(SCRAMBLED);
		httpTesting.expectOne('driver').flush(DRIVER);
		await TestBed.inject(ApplicationRef).whenStable();
		fixture.detectChanges();
	}

	function textsOf(testid: string, scope: Element | null = null): string[] {
		const root = scope ?? (fixture.nativeElement as HTMLElement);
		return [...root.querySelectorAll(`[data-testid="${testid}"]`)].map((e) =>
			(e.textContent ?? '').replace(/\s+/g, ' ').trim(),
		);
	}

	function columns(): Element[] {
		return [
			...(fixture.nativeElement as HTMLElement).querySelectorAll('[data-testid="variant-column"]'),
		];
	}

	// G5 test 2 — DR-05.
	it('renders options, coverages and discounts in response array order with no client-side sort', async () => {
		await loadQuote();

		expect(
			textsOf('variant-code'),
			'DR-05: options render in response array order — a sort by `order` would yield BRONZE, GOLD',
		).toEqual(['GOLD', 'BRONZE']);

		// ⚠️ ROWS ARE ORDERED BY `position`, and that is a real difference from "render the array".
		//
		// A matrix needs ONE row order that every column shares, and the union of three tiers' lists
		// has no array order of its own. First-seen-wins was tried and is wrong: the tiers
		// interleave, so taking SILVER's ordering first puts collision cover ahead of the uninsured
		// property row, and GOLD's own cells then come out in an order GOLD never sent.
		//
		// `position` is the engine's own ordering key and the G1 contract test pins that every tier
		// returns it ascending, so in practice this equals each tier's array order — which
		// `quote-panel.spec.ts` asserts per option against the live response. This fixture is
		// scrambled precisely so the difference is visible: GOLD sent [ERS, BI], and the rows come
		// out BI, PD, ERS.
		expect(
			textsOf('coverage-header'),
			'rows order by `position`, the only total order a union of three tiers has',
		).toEqual([
			'Bodily Injury Liability (BI)',
			'Property Damage Liability (PD)',
			'Emergency Road Service (ERS)',
		]);

		expect(
			textsOf('discount-name'),
			'discount rows likewise order by `position`',
		).toEqual(['Good Driver', 'Loyalty Discount']);

		// DR-10: `premiumBeforeDiscounts`, the final premium and the discount value are raw today
		// (`variants.html:19,28`, `discount.html:11`). The target formats all three, keeping the
		// literal `-` prefix on the discount and adding no currency symbol.
		expect(
			textsOf('variant-premium-before-discounts'),
			'DR-10: `premiumBeforeDiscounts` is DecimalPipe-formatted',
		).toEqual(['1,401.19', '284.78']);
		expect(textsOf('variant-premium'), 'DR-10: the final premium is DecimalPipe-formatted').toEqual(
			['1,381.19', '264.78'],
		);
		// Two discount rows × two columns. BRONZE does not earn the loyalty discount, so its cell in
		// that row states the absence rather than being blank.
		expect(
			textsOf('discount-value'),
			'DR-10: the discount value keeps its literal `-` prefix and gains only the formatting',
		).toEqual(['-20.00', '-20.00', '-10.00', '\u2014']);
	});

	it('states what a tier does NOT include instead of stopping the list early', async () => {
		await loadQuote();

		// ⛔ The reason the matrix replaced three parallel lists. A list can only stop, which reads
		// as "nothing more to say"; a cell can say "not in this tier". Which of the two it is
		// happens to be the entire basis for choosing a tier, and the old layout could not express
		// it at all.
		//
		// GOLD carries ERS and BI; BRONZE carries only PD. So of six cells, three are absences.
		expect(
			textsOf('coverage-cell'),
			'every tier gets a cell in every row — absent cover is stated, not omitted',
		).toEqual([
			'201.88 60,000.00 / 120,000.00',
			'\u2014',
			'\u2014',
			'100.00 25,000.00',
			'17.80 0.00',
			'\u2014',
		]);
	});

	// G5 test 3 — DR-07 (and the DR-14 message the C-6 backend fix will start sending).
	it('renders zero cards and one banner when the quote resource is in error', async () => {
		await loadQuote();
		expect(columns().length, 'the quote renders before the failure is provoked').toBe(2);

		TestBed.inject(QuoteStore).recalculate();
		fixture.detectChanges();
		httpTesting
			.expectOne('quote')
			.flush(
				{ message: 'Driver first name not supplied.' },
				{ status: 400, statusText: 'Bad Request' },
			);
		await TestBed.inject(ApplicationRef).whenStable();
		fixture.detectChanges();

		expect(
			columns().length,
			'DR-07: the previous quote must not survive underneath the banner — `resource()` drops it',
		).toBe(0);

		const banners = textsOf('quote-error-banner');
		expect(banners.length, 'exactly one error banner is rendered — C-7 adds no new surface').toBe(
			1,
		);
		expect(banners[0], 'the banner carries the message the backend sent (DR-14)').toBe(
			'Driver first name not supplied.',
		);

		expect(
			errorHandler.errors.map((e) => (e as Error).message),
			'the cards must be GONE, not merely unrendered because the template threw',
		).toEqual([]);
	});
});
