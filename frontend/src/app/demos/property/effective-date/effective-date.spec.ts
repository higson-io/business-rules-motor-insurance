import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { EffectiveDate } from './effective-date';
import { PropertyQuestionDto, PropertyQuoteDto } from '../../../models/property';

/**
 * Pasek daty niesie tezę całego demo: jedna data zmienia to, o co wolno zapytać. Dwie rzeczy mogą
 * cicho przestać być prawdą i te testy je łapią.
 *
 * Pierwsza: odczyt liczony z DATY zamiast z odpowiedzi serwera. Taki pasek pokazywałby prawidłowe
 * liczby dopóki ktoś nie zmieniłby reguły w Studio — i wtedy kłamałby, nie psując się.
 *
 * Druga: „było N" pokazujące się zawsze. Ten znacznik ma sens wyłącznie dlatego, że pojawia się
 * rzadko; wyświetlany przy każdym przeliczeniu staje się ozdobnikiem.
 */

function question(overrides: Partial<PropertyQuestionDto>): PropertyQuestionDto {
	return {
		code: 'Q',
		text: 'Pytanie',
		kind: 'TEXT',
		valueType: 'STRING',
		section: 'SUBJECT',
		help: null,
		position: 10,
		visible: true,
		required: true,
		options: [],
		optionLabels: [],
		prefillSource: null,
		refusalMessage: null,
		value: null,
		values: [],
		refused: false,
		...overrides,
	};
}

const STATUTORY = {
	code: 'STATUTORY',
	name: 'APK — minimum ustawowe',
	legalBasis: 'art. 8 ustawy o dystrybucji ubezpieczeń',
	requiresBudget: false,
	requiresExclusionCheck: false,
	requiresJustification: false,
	requiresVariantDifferences: false,
};

const KNF = {
	code: 'KNF_190_2026',
	name: 'APK — uchwała KNF 190/2026',
	legalBasis: 'rekomendacje 8 i 9',
	requiresBudget: true,
	requiresExclusionCheck: true,
	requiresJustification: true,
	requiresVariantDifferences: true,
};

function quote(
	regime: typeof STATUTORY,
	questions: readonly PropertyQuestionDto[],
	justification: readonly string[] = [],
): PropertyQuoteDto {
	return {
		startDate: '2026-08-21',
		deductible: 0,
		regime,
		questions,
		floodZone: 'Z0',
		theftZone: 'T1',
		buildingAge: 20,
		riskFactor: 1,
		sumBuilding: 100000,
		sumFixtures: 20000,
		sumContents: 10000,
		suggestedSumBuilding: 100000,
		covers: [],
		variants: [],
		recommendation: {
			variantCode: null,
			coveredNeeds: 0,
			totalNeeds: 0,
			missingCovers: [],
			budgetOutcome: null,
			justification,
			needs: [],
			warnings: [],
		},
		policyholder: {
			firstName: 'Anna',
			lastName: 'Kowalska',
			pesel: '85092612346',
			birthDate: '1985-09-26',
			idDocument: 'AZS 481920',
			street: 'Marszałkowska 84/92',
			flat: '37',
			postcode: '00-950',
			city: 'Warszawa',
			email: 'anna.kowalska@example.com',
			phone: '+48 601 234 567',
			source: 'Kartoteka klienta (CRM)',
		},
		offer: {
			number: null,
			issuedOn: null,
			validUntil: null,
			paymentDueOn: null,
			statement: null,
			needsAnalysisAttached: false,
			policyNumber: null,
			coverFrom: null,
			coverTo: null,
			variantCode: null,
			premium: null,
			choiceKind: null,
			choiceLabel: null,
			choiceText: null,
			choiceRequiresAcknowledgement: false,
			unmetNeeds: [],
		},
		selectedVariant: null,
	};
}

describe('Property EffectiveDate', () => {
	let httpTesting: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				provideZonelessChangeDetection(),
				provideNoopAnimations(),
				provideNativeDateAdapter(),
				provideHttpClient(),
				provideHttpClientTesting(),
			],
		});
		httpTesting = TestBed.inject(HttpTestingController);
	});

	async function render(first: PropertyQuoteDto): Promise<{
		element: HTMLElement;
		reload: (next: PropertyQuoteDto) => Promise<void>;
	}> {
		const fixture = TestBed.createComponent(EffectiveDate);
		TestBed.tick();
		httpTesting.expectOne('property').flush(first);
		await TestBed.inject(ApplicationRef).whenStable();
		fixture.detectChanges();

		const component = fixture.componentInstance;
		return {
			element: fixture.nativeElement as HTMLElement,
			reload: async (next) => {
				// Ten sam kanał, którym wraca przeliczenie: mutacja, potem przeładowanie zasobu.
				//
				// ⚠️ tick() + match(), nie whenStable(): `commit()` czeka na obietnicę PUT-a i
				// dopiero potem woła `reload()`, więc GET jeszcze nie istnieje w chwili, w której
				// whenStable() uznaje aplikację za spokojną — i expectOne('property') nie znajduje
				// niczego. Mikrozadania muszą dostać swoją kolej jawnie.
				component.pick(new Date('2027-08-01'));
				TestBed.tick();
				httpTesting.expectOne((request) => request.method === 'PUT').flush(null);
				await Promise.resolve();
				await Promise.resolve();
				TestBed.tick();
				httpTesting.expectOne('property').flush(next);
				await TestBed.inject(ApplicationRef).whenStable();
				fixture.detectChanges();
			},
		};
	}

	it('czyta liczbę pytań z odpowiedzi serwera, a nie z daty', async () => {
		const { element } = await render(
			quote(STATUTORY, [
				question({ code: 'AREA' }),
				question({ code: 'POSTCODE' }),
				// Pytanie niewidoczne nie jest pytaniem w ankiecie — liczy się to, co widać.
				question({ code: 'BUDGET', visible: false }),
			]),
		);

		const readout = element.querySelector('[data-testid="property-reading"]');
		expect(readout?.textContent).toContain('2');
		expect(element.querySelector('[data-testid="property-regime"]')?.textContent).toContain(
			'minimum ustawowe',
		);
	});

	it('pokazuje „było N" dopiero wtedy, gdy reżim faktycznie się zmienił', async () => {
		const { element, reload } = await render(
			quote(STATUTORY, [question({ code: 'AREA' }), question({ code: 'POSTCODE' })]),
		);

		expect(
			element.textContent,
			'przy pierwszym wczytaniu nie ma się do czego porównać',
		).not.toContain('było');

		await reload(
			quote(
				KNF,
				[
					question({ code: 'AREA' }),
					question({ code: 'POSTCODE' }),
					question({ code: 'BUDGET', section: 'NEEDS' }),
				],
				['Wybrano wariant pokrywający zgłoszone potrzeby.'],
			),
		);

		const readout = element.querySelector('[data-testid="property-reading"]');
		expect(readout?.textContent).toContain('3');
		expect(readout?.textContent, 'poprzedni odczyt ma zostać na ekranie').toContain('było 2');
		expect(element.querySelector('[data-testid="property-regime"]')?.textContent).toContain(
			'uchwała KNF',
		);
	});

	it('milczy o liczbie, która się nie zmieniła, choć reżim owszem', async () => {
		// Wersjonowanie jest selektywne. Reżim przeskakuje, ale sekcje zostają dwie — i właśnie
		// wtedy znacznik „było" przy sekcjach byłby szumem, a jego brak jest odpowiedzią.
		const { element, reload } = await render(
			quote(STATUTORY, [
				question({ code: 'AREA', section: 'SUBJECT' }),
				question({ code: 'BUDGET', section: 'NEEDS' }),
			]),
		);

		await reload(
			quote(KNF, [
				question({ code: 'AREA', section: 'SUBJECT' }),
				question({ code: 'POSTCODE', section: 'SUBJECT' }),
				question({ code: 'BUDGET', section: 'NEEDS' }),
			]),
		);

		const readout = element.querySelector('[data-testid="property-reading"]');
		expect(readout?.textContent, 'liczba pytań się ruszyła, więc ma swój znacznik').toContain(
			'było 2',
		);
		expect(
			(readout?.textContent?.match(/było/g) ?? []).length,
			'dokładnie jeden znacznik: sekcje zostały dwie, uzasadnienia jak nie było, tak nie ma',
		).toBe(1);
	});

	it('nie pokazuje „było N", gdy reżim został ten sam a liczby się ruszyły', async () => {
		const { element, reload } = await render(quote(STATUTORY, [question({ code: 'AREA' })]));

		await reload(
			quote(STATUTORY, [question({ code: 'AREA' }), question({ code: 'POSTCODE' })]),
		);

		const readout = element.querySelector('[data-testid="property-reading"]');
		expect(readout?.textContent).toContain('2');
		expect(
			readout?.textContent,
			'przeliczenie w tym samym reżimie nie jest zdarzeniem, które pasek ma zgłaszać',
		).not.toContain('było');
	});
});
