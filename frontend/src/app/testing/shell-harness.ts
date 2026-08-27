import { EnvironmentProviders, Provider, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DateAdapter, MAT_DATE_FORMATS } from '@angular/material/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { DemoSelection } from '../core/demo-selection';
import { IsoDateAdapter, MOTOR_DATE_FORMATS } from '../core/iso-date.adapter';
import { mutationErrorInterceptor } from '../core/mutation-error.interceptor';
import { pendingRequestsInterceptor } from '../core/pending-requests.interceptor';
import { DictionaryEntryDto } from '../models/dictionary-entry';
import { DriverDto } from '../models/driver';
import { LifeQuoteDto } from '../models/life';
import { QuoteDto } from '../models/quote';
import { VehicleDto } from '../models/vehicle';

/**
 * Test-only helper, shared by the three G8 shell specs.
 *
 * ⚠️ It is not a `.spec.ts`, so `tsconfig.app.json` type-checks it with the application — it is
 * compile-clean and unreachable from `main.ts`, so nothing of it reaches the bundle. It lives here
 * rather than being copied into three spec files because all three mount the WHOLE screen and so
 * all three have to answer the same startup traffic.
 */

/**
 * The application's own providers, minus the ones a test cannot use.
 *
 * ⚠️ The interceptor chain is the same one `app.config.ts` installs, and that is load-bearing:
 * without `pendingRequestsInterceptor` the counter never moves and the spinner assertions would
 * pass against a spinner that is simply always absent.
 */
export const SHELL_PROVIDERS: (Provider | EnvironmentProviders)[] = [
	provideZonelessChangeDetection(),
	provideNoopAnimations(),
	provideHttpClient(withInterceptors([pendingRequestsInterceptor, mutationErrorInterceptor])),
	provideHttpClientTesting(),
	{ provide: DateAdapter, useClass: IsoDateAdapter },
	{ provide: MAT_DATE_FORMATS, useValue: MOTOR_DATE_FORMATS },
];

/**
 * The server-seeded session the goldens were captured against
 * (`e2e/goldens/pre/startup.json`).
 */
export const DRIVER: DriverDto = {
	firstName: 'John',
	lastName: 'Potter',
	dateOfBirth: '1986-08-06T00:00:00.000+00:00',
	gender: 'M',
	address: { zipCode: '77566', city: 'Lake Jackson', street: 'Allwood St' },
	numberOfAccidents: 0,
	numberOfTickets: 0,
	licenceObtainedAtAge: 18,
};

export const VEHICLE: VehicleDto = {
	productionYear: 2010,
	makeId: 217,
	make: 'TOYOTA',
	typeId: 28654,
	modelId: 218915,
};

/** Reżim ustawowy i pusta ankieta: te testy dotyczą powłoki, nie analizy potrzeb. */
export const APK_STATUTORY = {
	code: 'STATUTORY',
	name: 'APK — minimum ustawowe',
	legalBasis: 'art. 8 ustawy o dystrybucji ubezpieczeń',
	requiresBudget: false,
	requiresExclusionCheck: false,
	requiresJustification: false,
	requiresVariantDifferences: false,
};

/**
 * Pola APK w kształcie „ankieta nieistotna dla tego testu".
 *
 * Rozsypane po pięciu specyfikacjach powtarzałyby się identycznie i przy każdym nowym polu
 * trzeba by je poprawiać w pięciu miejscach — a żaden z tych testów nie dotyczy analizy potrzeb.
 */
export const APK_ABSENT = {
	policyStartDate: '2026-08-24',
	apkRegime: APK_STATUTORY,
	apkQuestions: [],
	apkRecommendation: null,
} satisfies Pick<QuoteDto, 'policyStartDate' | 'apkRegime' | 'apkQuestions' | 'apkRecommendation'>;

/** To samo co `APK_ABSENT`, dla demo życiowego — te testy też nie dotyczą analizy potrzeb. */
export const APK_ABSENT_LIFE = {
	apkRegime: APK_STATUTORY,
	apkQuestions: [],
	apkRecommendation: null,
};

export const QUOTE: QuoteDto = {
	...APK_ABSENT,
	options: [
		{
			code: 'BRONZE',
			order: 1,
			premium: 264.78,
			premiumBeforeDiscounts: 284.78,
			coverages: [],
			discounts: [],
		},
	],
};

const DICTIONARY: DictionaryEntryDto[] = [{ code: '1', name: 'Entry' }];

/**
 * Opens the motor demo, which is what mounts the quote screen.
 *
 * ⚠️ Call this before asserting anything about that screen. The shell now starts on the landing
 * screen, and the motor components — and therefore every startup request — do not exist until a
 * demo is opened. A spec that skips this is asserting against the picker.
 *
 * It deliberately drives `DemoSelection` rather than clicking a tile: the specs that use it are
 * about the assembled quote screen, not about the picker's markup. `demo-picker.spec.ts` covers
 * the click.
 */
export function openMoto(): void {
	TestBed.inject(DemoSelection).open('moto');
	TestBed.tick();
}

/**
 * Answers every request the mounted screen has in flight, repeatedly, until it goes quiet — the
 * vehicle cascade issues its dependent dictionary loads only once the vehicle itself has landed,
 * so one round of flushing is never enough.
 *
 * ⚠️ `TestBed.tick()`, never `ApplicationRef.whenStable()`: a settled screen still holds an open
 * `GET quote` after any mutation, and `whenStable()` waits for it forever.
 *
 * @returns the URL of every request answered, in the order they were answered.
 */
/**
 * The life demo's opening quote, as `LifeApi` builds it from the seeded insured. Trimmed to one
 * rider and one variant: these specs assert the shell's wiring, not the tariff, and a full
 * thirteen-benefit catalogue here would only obscure that.
 */
export const LIFE_QUOTE: LifeQuoteDto = {
	lineCode: 'INDIVIDUAL',
	startDate: '2026-08-20',
	tariffVersion: { code: 'V2', name: 'Taryfa 2026 H2', validFrom: '1 lipca 2026' },
	// Pusta mapa = faza oferty, i tego dotyczą te testy: powłoka nie zna fazy programu.
	enrolment: {},
	...APK_ABSENT_LIFE,
	mainSum: 300000,
	paymentFrequency: 'MONTHLY',
	birthDate: null,
	entryAge: 35,
	smoker: false,
	heightCm: 178,
	weightKg: 78,
	bmi: 24.6,
	occupationClass: 1,
	monthlyIncome: 8000,
	priorCoverFrom: null,
	priorCoverTo: null,
	decision: 'ACCEPT',
	binding: true,
	message: null,
	medicalRequired: false,
	premiumMonthly: 57,
	instalment: 57,
	installments: null,
	premiumAnnual: 684,
	riders: [
		{
			code: 'CRITICAL_ILLNESS',
			name: 'Powazne zachorowanie',
			description: null,
			position: 1,
			benefitKind: 'SUM_ASSURED',
			sums: [20000, 50000],
			defaultSum: 50000,
			maxSum: 300000,
			waitingPeriodDays: 90,
			selected: false,
			sum: 50000,
			premium: null,
		},
	],
	headcount: 100,
	averageAge: 35,
	industryClass: 1,
	participation: 60,
	includeSpouse: false,
	includeAdultChild: false,
	insuredCount: null,
	programCost: null,
	variants: [
		{ code: 'BASIC', name: 'Wariant podstawowy', position: 1, available: false, perEmployee: null, monthlyTotalIfAll: null },
	],
	benefits: [
		{ code: 'DEATH', name: 'Smierc ubezpieczonego', position: 1, benefitKind: 'SUM_ASSURED', sumByVariant: null },
	],
};

export function settleShell(httpTesting: HttpTestingController): string[] {
	const answered: string[] = [];

	for (let round = 0; round < 10; round++) {
		TestBed.tick();
		const open = httpTesting.match(() => true);
		if (open.length === 0) {
			return answered;
		}
		for (const request of open) {
			answered.push(request.request.url);
			request.flush(bodyFor(request.request.url));
		}
	}

	throw new Error(`the screen never went quiet; answered so far: ${answered.join(', ')}`);
}

function bodyFor(url: string): object {
	if (url === 'life') {
		return LIFE_QUOTE;
	}
	if (url === 'quote') {
		return QUOTE;
	}
	if (url === 'driver') {
		return DRIVER;
	}
	if (url === 'vehicle') {
		return VEHICLE;
	}
	if (url.startsWith('dictionaries/')) {
		return DICTIONARY;
	}
	throw new Error(`unexpected request to "${url}" — the 21-endpoint contract is frozen`);
}
