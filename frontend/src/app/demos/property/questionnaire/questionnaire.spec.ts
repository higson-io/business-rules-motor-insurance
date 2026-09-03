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
		section: 'SUBJECT',
		position: 10,
		visible: true,
		// ⚠️ Domyślnie odsłonięte: fixture bada renderowanie, a nie łańcuch odsłaniania.
		revealed: true,
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

function quote(questions: readonly PropertyQuestionDto[], kind: 'FORM' | 'KNOWN' = 'KNOWN'): PropertyQuoteDto {
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
		// ⛔ Sekcje wyprowadzone z pytań, a nie wypisane obok nich. Wypisana lista rozjechałaby się
		// z pytaniami przy pierwszym teście, który doda pytanie w nowej sekcji — a wtedy test
		// przechodziłby albo nie z powodu, którego nie bada.
		sections: [...new Set(questions.map((q) => q.section))].map((code, index) => ({
			code,
			title: `Sekcja ${code}`,
			kind,
			position: (index + 1) * 10,
			visible: true,
		})),
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
			alternativeVariantCode: null,
			alternativePremium: null,
			justification: [],
			needs: [],
			warnings: [],
		},
		policyholder: {
			firstName: 'Anna',
			lastName: 'Przykładowa',
			pesel: '12345678901',
			birthDate: '1990-01-01',
			idDocument: 'ABC 123456',
			street: 'ul. Przykładowa 1',
			flat: '2',
			postcode: '00-000',
			city: 'Warszawa',
			email: 'anna.przykladowa@example.com',
			phone: '+48 000 000 000',
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
		blocked: false,
		blockMessage: null,
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

	/**
	 * ⚠️ Domyślnie KNOWN, czyli układ LISTY. Asercje o kilku pytaniach naraz mają sens tylko tam:
	 * tryb FORM jest ankietą i renderuje z założenia jedno pytanie na ekran, więc uruchomione
	 * w nim przechodziłyby albo nie z powodu, którego nie badają. Sam przebieg krokowy ma własny
	 * test niżej.
	 *
	 * ⚠️ `kind` jest wymagane i musi być ustawione PRZED pierwszym `tick()` — komponent czyta je
	 * w `computed`, więc brak wejścia wybucha przy pierwszym odczycie, a nie przy tworzeniu.
	 */
	async function render(
		questions: readonly PropertyQuestionDto[],
		kind: 'FORM' | 'KNOWN' = 'KNOWN',
	): Promise<HTMLElement> {
		const fixture = TestBed.createComponent(Questionnaire);
		fixture.componentRef.setInput('kind', kind);
		TestBed.tick();
		httpTesting.expectOne('property').flush(quote(questions, kind));
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

	/**
	 * ⛔ To jest teza kroku pierwszego: APK jest ANKIETĄ, nie formularzem. Trzy pytania spełniają
	 * regułę widoczności, ale łańcuch doszedł dopiero do dwóch — na ekranie ma być JEDNO, a
	 * licznik ma mówić „z 3", bo mianownik liczy po regule, nie po odsłonięciu.
	 *
	 * ⚠️ Test przechodzi też wtedy, gdy komponent renderuje listę i po prostu ukrywa nadmiar
	 * CSS-em, więc sprawdza LICZBĘ WĘZŁÓW, a nie widoczność.
	 */
	it('asks one question at a time and counts by the rule, not by what is revealed', async () => {
		const element = await render(
			[
				question({ code: 'NEEDS_PROPERTY', kind: 'BOOLEAN' }),
				question({ code: 'NEED_THEFT', kind: 'BOOLEAN', position: 20 }),
				question({ code: 'NEED_FLOOD', kind: 'BOOLEAN', position: 30, revealed: false }),
			],
			'FORM',
		);

		expect(element.querySelectorAll('[data-question]').length).toBe(1);
		expect(element.querySelector('[data-question="NEEDS_PROPERTY"]')).not.toBeNull();
		expect(element.textContent).toContain('Pytanie 1 z 3');
	});

	/**
	 * ⛔ Ankieta stoi na pierwszym NIEUDZIELONYM pytaniu, a nie na ostatnim odsłoniętym. Dzięki
	 * temu odpowiedź sama przenosi ekran na kolejne pytanie — odsłonięcie i przejście to jedno
	 * zdarzenie, bez osobnego mechanizmu przewijania.
	 *
	 * ⛔ „Dalej" jest WYŁĄCZONE, dopóki bieżące pytanie nie jest domknięte: następne nie jest
	 * jeszcze odsłonięte, więc nie ma dokąd iść. Etykieta mówi mimo to „Następne pytanie", bo
	 * liczy po regule — „Zakończ" na pytaniu 2 z 3 byłoby kłamstwem.
	 */
	it('sits on the first unanswered question and will not advance past it', async () => {
		const element = await render(
			[
				question({ code: 'NEEDS_PROPERTY', kind: 'BOOLEAN', value: 'true' }),
				question({ code: 'NEED_THEFT', kind: 'BOOLEAN', position: 20 }),
				question({ code: 'NEED_FLOOD', kind: 'BOOLEAN', position: 30, revealed: false }),
			],
			'FORM',
		);

		expect(element.querySelector('[data-question="NEED_THEFT"]')).not.toBeNull();
		expect(element.querySelectorAll('[data-question]').length).toBe(1);
		expect(element.textContent).toContain('Pytanie 2 z 3');

		const next = element.querySelector('[data-testid="apk-next"]') as HTMLButtonElement;
		expect(next.textContent, 'liczy po regule, nie po odsłonięciu').toContain('Następne pytanie');
		expect(next.disabled, 'bez odpowiedzi nie ma dokąd przejść').toBe(true);
	});

	it('ends the step from the last question rather than scrolling on', async () => {
		const element = await render(
			[
				question({ code: 'NEEDS_PROPERTY', kind: 'BOOLEAN', value: 'true' }),
				question({ code: 'NEED_THEFT', kind: 'BOOLEAN', position: 20, value: 'false' }),
			],
			'FORM',
		);

		expect(element.textContent).toContain('Pytanie 2 z 2');
		const next = element.querySelector('[data-testid="apk-next"]') as HTMLButtonElement;
		expect(next.disabled).toBe(false);
		expect(next.textContent).toContain('Zakończ');
	});

	/**
	 * ⛔ REGRESJA, która kosztowała pół godziny szukania. `@if` nie ma klucza, więc Angular
	 * utrzymywał ten sam węzeł `mat-button-toggle-group` przy przejściu do następnego pytania
	 * i podmieniał mu tylko wartość. Gdy dwa kolejne pytania miały TĘ SAMĄ odpowiedź, Material
	 * nie widział zmiany, nie emitował `change` i odpowiedź nie leciała na serwer — ankieta
	 * stawała w miejscu, a wyglądało to na zgubione zdarzenie.
	 *
	 * Test pilnuje przyczyny, nie objawu: kontrolka bieżącego pytania ma być INNYM węzłem DOM
	 * niż kontrolka poprzedniego. Dopóki to zachodzi, Material nie ma czego pamiętać.
	 */
	it('rebuilds the control when the question changes, so a repeated answer still registers', async () => {
		const element = await render(
			[
				question({ code: 'NEED_FLOOD', kind: 'BOOLEAN', value: 'false' }),
				question({ code: 'NEED_PORTABLE', kind: 'BOOLEAN', position: 20 }),
			],
			'FORM',
		);

		const first = element.querySelector('[data-question="NEED_PORTABLE"] mat-button-toggle-group');
		expect(first, 'kursor stoi na pierwszym nieudzielonym').not.toBeNull();

		// Cofnięcie na pytanie poprzednie musi dać INNY węzeł kontrolki.
		(element.querySelector('.apk__nav__back') as HTMLButtonElement).click();
		TestBed.tick();

		const second = element.querySelector('[data-question="NEED_FLOOD"] mat-button-toggle-group');
		expect(second).not.toBeNull();
		expect(second, 'ten sam węzeł = Material pamięta poprzednią wartość').not.toBe(first);
	});
});
