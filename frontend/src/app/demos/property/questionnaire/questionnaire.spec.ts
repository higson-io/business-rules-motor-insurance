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
		// ⚠️ Bez `section` — dopisze ją `render`, chyba że test poda własną.
		section: undefined as unknown as PropertyQuestionDto['section'],
		code: 'Q',
		text: 'Pytanie',
		// ⚠️ Domyślnie PUSTE. Krótka etykieta i pola są nieobowiązkowe, a fixture ma odwzorowywać
		// pytanie zwykłe — test pytania złożonego podaje `fields` jawnie i przez to widać, czego
		// dotyczy.
		fields: [],
		kind: 'TEXT',
		position: 10,
		// ⚠️ Domyślnie odsłonięte: fixture bada renderowanie, a nie łańcuch odsłaniania.
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

/**
 * Pytanie złożone o jednym polu wymaganym i jednym nie.
 *
 * ⚠️ Osobna funkcja, bo dwa testy potrzebują tego samego kształtu z inną wartością pola —
 * a `render` da się w jednym `it` wywołać tylko RAZ: zasób `PropertyStore` wystawia jedno
 * żądanie na TestBed, więc drugie `expectOne('property')` nie ma czego dopasować.
 */
function compound(budget: string | null): PropertyQuestionDto {
	return question({
		code: 'EXPECTATIONS',
		kind: 'GROUP' as PropertyQuestionDto['kind'],
		required: true,
		fields: [
			question({ code: 'BUDGET', kind: 'MONEY', required: true, value: budget }),
			question({ code: 'PROPERTY_VALUE', kind: 'MONEY', required: false, position: 20 }),
		],
	});
}

function quote(questions: readonly PropertyQuestionDto[]): PropertyQuoteDto {
	return {
		startDate: '2026-08-21',
		deductible: 0,
		floodDeductible: null,
		questions,
		floodZone: 'Z0',
		theftZone: 'T1',
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
		variantChosen: false,
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
		group: 'NEEDS' | 'SUBJECT' = 'SUBJECT',
	): Promise<HTMLElement> {
		const fixture = TestBed.createComponent(Questionnaire);
		fixture.componentRef.setInput('group', group);
		TestBed.tick();
		// ⚠️ Gałąź stemplowana tam, gdzie test jej nie podał. Bez tego każdy test musiałby powtarzać
		// `section` przy każdym pytaniu, a niedopasowanie objawiałoby się pustym ekranem — czyli
		// awarią, której test nie bada. Test, który gałąź podaje jawnie, zachowuje swoją.
		httpTesting
			.expectOne('property')
			.flush(quote(questions.map((q) => ({ ...q, section: q.section ?? group }))));
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

	it('does not render an unrevealed question at all — the chain is the rules’ answer, not ours', async () => {
		const element = await render([
			question({ code: 'AREA' }),
			question({ code: 'WALL_MATERIAL', visible: false }),
		]);

		expect(element.querySelector('[data-question="AREA"]')).not.toBeNull();
		expect(
			element.querySelector('[data-question="WALL_MATERIAL"]'),
			'an unrevealed question must not reach the DOM — a hidden-but-present control still gets tabbed into',
		).toBeNull();
	});

	/**
	 * ⛔ Zastąpił test „sekcja renderuje się tylko, gdy ma widoczne pytanie". Sekcji nie ma —
	 * pytania wiszą płasko w dwóch gałęziach drzewa, a ekran renderuje jedną z nich. Test pilnuje
	 * tego, co przejęło tamtą rolę: pytanie z DRUGIEJ gałęzi nie ma prawa się pokazać.
	 */
	it('renders only the branch it was asked for', async () => {
		const element = await render(
			[
				question({ code: 'AREA', section: 'SUBJECT' }),
				question({ code: 'BUDGET', section: 'NEEDS' }),
			],
			'SUBJECT',
		);

		expect(element.querySelector('[data-question="AREA"]')).not.toBeNull();
		expect(
			element.querySelector('[data-question="BUDGET"]'),
			'pytanie z gałęzi APK nie należy do kroku przedmiotu ubezpieczenia',
		).toBeNull();
	});

	/**
	 * ⛔ Zastąpił test „pokazuje źródło podpowiedzi tam, gdzie serwer je podał (rek. 8.3)".
	 * Podpis „Podpowiedziane: …" zszedł z ekranu 2026-09-09 na decyzję właściciela dema.
	 *
	 * ⚠️ Test pilnuje teraz DECYZJI, a nie mechanizmu: `prefillSource` nadal przychodzi z serwera
	 * i nadal jest w katalogu reguł, więc dopisanie podpisu z powrotem jest jedną linijką
	 * w szablonie — i wyglądałoby jak naprawa brakującej funkcji, a nie cofnięcie decyzji.
	 */
	it('does not print the prefill source, even when the server sends one', async () => {
		const element = await render([
			question({ code: 'AREA', prefillSource: 'Metraż z poprzedniej polisy' }),
			question({ code: 'POSTCODE', position: 20 }),
		]);

		expect(element.querySelector('[data-question="AREA"]')).not.toBeNull();
		expect(
			element.textContent,
			'źródło podpowiedzi zdjęte z ekranu — nie przywracać bez decyzji',
		).not.toContain('Metraż z poprzedniej polisy');
	});

	/**
	 * ⛔ Zastąpił test odmowy („nie chcę podać"), usuniętej z ekranu 4 września 2026. Odmowa
	 * domykała pytanie bez odpowiadania na nie i to ona przepuszczała ankietę przez pytania
	 * nieobowiązkowe; jej rolę przejęła sama obowiązkowość.
	 *
	 * ⚠️ Gdyby tej gałęzi zabrakło, oczekiwana składka byłaby ślepym zaułkiem: pytanie
	 * nieobowiązkowe, na które klient nie chce odpowiedzieć, zatrzymywałoby resztę ankiety.
	 */
	it('lets an optional question be skipped, so it cannot dead-end the questionnaire', async () => {
		const element = await render(
			[
				question({ code: 'BUDGET', kind: 'MONEY', required: false }),
				question({ code: 'PROPERTY_VALUE', kind: 'MONEY', position: 20, required: false }),
			],
			'NEEDS',
		);

		const next = element.querySelector('[data-testid="apk-next"]') as HTMLButtonElement;
		expect(next.disabled, 'nieobowiązkowe wolno pominąć').toBe(false);
	});

	/**
	 * ⛔ Pytanie ZŁOŻONE jest JEDNYM pytaniem o kilku polach. Test pilnuje trzech rzeczy naraz,
	 * bo każda z nich osobno da się zepsuć tą samą pomyłką — potraktowaniem pól jak pytań:
	 * licznik ma mówić „1 z 1", pola mają się pojawić w DOM, a rodzic NIE ma mieć własnej
	 * kontrolki, bo nie ma ścieżki kontekstu, pod którą wylądowałaby odpowiedź.
	 */
	it('renders a compound question as one question with several fields', async () => {
		const element = await render(
			[
				question({
					code: 'EXPECTATIONS',
					text: 'Twoje oczekiwania wobec ochrony',
					kind: 'GROUP' as PropertyQuestionDto['kind'],
					required: false,
					fields: [
						question({ code: 'BUDGET', text: 'Oczekiwana składka', kind: 'MONEY', required: false }),
						question({
							code: 'PROPERTY_VALUE',
							text: 'Wartość nieruchomości',
							kind: 'MONEY',
							required: false,
							position: 20,
						}),
					],
				}),
			],
			'NEEDS',
		);

		expect(element.textContent, 'pola nie są osobnymi pytaniami').toContain('Pytanie 1 z 1');
		expect(element.querySelector('[data-question="BUDGET"]')).not.toBeNull();
		expect(element.querySelector('[data-question="PROPERTY_VALUE"]')).not.toBeNull();
		expect(
			element.querySelectorAll('[data-question="EXPECTATIONS"] > .apk__body > input').length,
			'rodzic nie ma własnej kontrolki — jego `kind` to GROUP',
		).toBe(0);
	});

	/**
	 * ⛔ Domknięcie pytania złożonego liczy się po POLACH, a nie po odpowiedzi pod kodem rodzica —
	 * pod tym kodem żadna nigdy nie wyląduje. Ta sama reguła stoi po stronie serwera
	 * (`Property.allRequiredAnswered`) i prowadzi łańcuch odsłaniania.
	 *
	 * ⚠️ Test celuje w mutację, która przechodzi najłatwiej: `settled()` czytające `question.value`
	 * rodzica zamiast pól. Wtedy „Dalej" byłoby wyłączone także po wypełnieniu pola wymaganego.
	 */
	it('will not advance a compound question while its required field is empty', async () => {
		const element = await render([compound(null)], 'NEEDS');

		expect(
			(element.querySelector('[data-testid="apk-next"]') as HTMLButtonElement).disabled,
			'pole wymagane jest puste — nie ma dokąd iść',
		).toBe(true);
	});

	it('advances a compound question once its required field is answered', async () => {
		const element = await render([compound('3000')], 'NEEDS');

		expect(
			(element.querySelector('[data-testid="apk-next"]') as HTMLButtonElement).disabled,
			'pole wymagane wypełnione — pytanie domknięte',
		).toBe(false);
	});

	/**
	 * ⛔ Spis pytań liczy po REGULE widoczności, nie po odsłonięciu. Spis, który rośnie w trakcie
	 * odpowiadania, przestaje mówić, ile jeszcze zostało — a to jego jedyne zadanie.
	 *
	 * ⚠️ Etykietą pozycji spisu jest TREŚĆ pytania — czyli dla kroku 1 nazwa grupy, bo to grupy są
	 * jego pozycjami. Zapasowej nazwy w kodzie nie ma i to jest decyzja, a nie przeoczenie.
	 */
	it('lists every question of the step in the outline, revealed or not', async () => {
		const element = await render(
			[
				question({ code: 'NEEDS_PROPERTY', kind: 'BOOLEAN', text: 'Potrzeba ubezpieczenia' }),
				question({ code: 'NEED_THEFT', kind: 'BOOLEAN', position: 20, visible: false }),
			],
			'NEEDS',
		);

		expect(element.querySelectorAll('.apk__outline__item').length).toBe(2);
		expect(element.querySelector('[data-testid="apk-outline-NEEDS_PROPERTY"]')?.textContent)
			.toContain('Potrzeba ubezpieczenia');
		expect(
			(element.querySelector('[data-testid="apk-outline-NEED_THEFT"]') as HTMLButtonElement)
				.disabled,
			'nieodsłonięte jest widoczne, ale nieklikalne — łańcuch jest konfiguracją',
		).toBe(true);
	});

	it('still blocks a required question until it is answered', async () => {
		const element = await render(
			[question({ code: 'NEEDS_PROPERTY', kind: 'BOOLEAN', required: true })],
			'NEEDS',
		);

		const next = element.querySelector('[data-testid="apk-next"]') as HTMLButtonElement;
		expect(next.disabled).toBe(true);
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
				question({ code: 'NEED_FLOOD', kind: 'BOOLEAN', position: 30, visible: false }),
			],
			'NEEDS',
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
				question({ code: 'NEED_FLOOD', kind: 'BOOLEAN', position: 30, visible: false }),
			],
			'NEEDS',
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
			'NEEDS',
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
			'NEEDS',
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
