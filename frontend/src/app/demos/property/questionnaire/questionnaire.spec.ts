import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { Questionnaire } from './questionnaire';
import { PropertyQuestionDto, PropertyQuoteDto } from '../../../models/property';

/**
 * The questionnaire's whole contract is that it renders whatever the server sends and computes
 * nothing about it. These tests exist to catch the two ways that can quietly stop being true:
 * a component that filters by its own rule, and a component that hard-codes a question.
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

function quote(questions: readonly PropertyQuestionDto[]): PropertyQuoteDto {
	return {
		startDate: '2026-08-21',
		deductible: 0,
		regime: {
			code: 'STATUTORY',
			name: 'APK — minimum ustawowe',
			legalBasis: 'art. 8 ustawy o dystrybucji ubezpieczeń',
			requiresBudget: false,
			requiresExclusionCheck: false,
			requiresJustification: false,
			requiresVariantDifferences: false,
		},
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
			justification: [],
			needs: [],
			warnings: [],
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
		},
		selectedVariant: null,
	};
}

describe('Questionnaire', () => {
	let httpTesting: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				provideZonelessChangeDetection(),
				provideNoopAnimations(),
				provideHttpClient(),
				provideHttpClientTesting(),
			],
		});
		httpTesting = TestBed.inject(HttpTestingController);
	});

	async function render(questions: readonly PropertyQuestionDto[]): Promise<HTMLElement> {
		const fixture = TestBed.createComponent(Questionnaire);
		TestBed.tick();
		httpTesting.expectOne('property').flush(quote(questions));
		await TestBed.inject(ApplicationRef).whenStable();
		fixture.detectChanges();
		return fixture.nativeElement as HTMLElement;
	}

	it('renders the questions the server sends, and nothing it does not', async () => {
		const element = await render([
			question({ code: 'AREA', text: 'Metraż' }),
			question({ code: 'POSTCODE', text: 'Kod pocztowy', position: 20 }),
		]);

		expect(element.querySelector('[data-question="AREA"]')).not.toBeNull();
		expect(element.querySelector('[data-question="POSTCODE"]')).not.toBeNull();
		// A question this component never heard of must be absent, not blank.
		expect(element.querySelector('[data-question="BUDGET"]')).toBeNull();
	});

	it('does not render a hidden question at all — visibility is the rules’ answer, not ours', async () => {
		const element = await render([
			question({ code: 'AREA' }),
			question({ code: 'WALL_MATERIAL', visible: false }),
		]);

		expect(element.querySelector('[data-question="AREA"]')).not.toBeNull();
		expect(
			element.querySelector('[data-question="WALL_MATERIAL"]'),
			'a hidden question must not reach the DOM — a hidden-but-present control still gets tabbed into',
		).toBeNull();
	});

	it('renders a section only when it has a visible question', async () => {
		const element = await render([
			question({ code: 'AREA', section: 'SUBJECT' }),
			question({ code: 'BUDGET', section: 'NEEDS', visible: false }),
		]);

		expect(element.querySelector('[data-section="SUBJECT"]')).not.toBeNull();
		expect(element.querySelector('[data-section="NEEDS"]')).toBeNull();
	});

	it('shows the prefill source only where the server supplied one (recommendation 8.3)', async () => {
		const element = await render([
			question({ code: 'AREA', prefillSource: 'Metraż z poprzedniej polisy' }),
			question({ code: 'POSTCODE', position: 20 }),
		]);

		const notes = element.querySelectorAll('[data-testid="property-prefill"]');
		expect(notes.length, 'an empty prefill source means the client answered, not "unknown"').toBe(1);
		expect(notes[0].textContent).toContain('Metraż z poprzedniej polisy');
	});

	it('shows the consequence of declining, and only after declining (recommendation 8.4)', async () => {
		const element = await render([
			question({
				code: 'CLAIMS_3Y',
				refused: true,
				refusalMessage: 'Przyjmiemy najostrożniejsze założenie.',
			}),
			question({ code: 'AREA', position: 20, refusalMessage: 'Nie wyliczymy sumy.' }),
		]);

		const messages = element.querySelectorAll('[data-testid="property-refusal"]');
		expect(messages.length).toBe(1);
		expect(messages[0].textContent).toContain('najostrożniejsze');
	});
});
