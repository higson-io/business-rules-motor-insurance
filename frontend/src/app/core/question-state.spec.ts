import { isBranchComplete, isSettled, shownFields } from './question-state';
import { PropertyQuestionDto } from '../models/property';

/**
 * Domknięcie gałęzi decyduje o tym, czy wolno przejść do następnego kroku — a bramkowanie kroków
 * jest jedyną rzeczą, która stoi między klientem a ofertą wystawioną, zanim cokolwiek wybrał.
 * Reguła jest krótka i dlatego łatwa do „uproszczenia" w złą stronę; te testy pilnują trzech
 * gałęzi, z których każda zniknęłaby przy innym uproszczeniu.
 */
function question(overrides: Partial<PropertyQuestionDto>): PropertyQuestionDto {
	return {
		code: 'Q',
		text: 'Pytanie',
		fields: [],
		kind: 'TEXT',
		section: 'NEEDS',
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

describe('isBranchComplete', () => {
	it('is false while a required question has no answer', () => {
		expect(isBranchComplete([question({ code: 'A', value: 'tak' }), question({ code: 'B' })])).toBe(
			false,
		);
	});

	/**
	 * ⛔ Ta gałąź znika przy „uproszczeniu" do `every(isSettled)`. Pytanie nieobowiązkowe wolno
	 * pominąć (§22 zdjął odmowę z ekranu i obowiązkowość przejęła jej rolę), więc warunek
	 * „wszystkie udzielone" zatrzasnąłby klienta na kroku pierwszym na zawsze, gdyby nie podał
	 * oczekiwanej składki.
	 */
	it('is true when only optional questions are left unanswered', () => {
		expect(
			isBranchComplete([
				question({ code: 'A', value: 'tak' }),
				question({ code: 'B', required: false }),
			]),
		).toBe(true);
	});

	/**
	 * ⛔ A ta znika przy „uproszczeniu" do `every(q => !q.required || isSettled(q))`. Nieodsłonięte
	 * pytanie NIEOBOWIĄZKOWE przeszłoby wtedy jako domknięte, choć klient go jeszcze nie widział —
	 * i krok otworzyłby się w środku ankiety.
	 */
	it('is false while the chain has not reached every question', () => {
		expect(
			isBranchComplete([
				question({ code: 'A', value: 'tak' }),
				question({ code: 'B', required: false, visible: false }),
			]),
		).toBe(false);
	});

	/** Pusta gałąź nie jest „domknięta" — nie ma czego domykać, a krok dalej nie ma podstawy. */
	it('is false for an empty branch', () => {
		expect(isBranchComplete([])).toBe(false);
	});

	it('counts a compound question by its required fields, not by its own value', () => {
		const compound = (budget: string | null) =>
			question({
				code: 'EXPECTATIONS',
				fields: [
					question({ code: 'BUDGET', required: true, value: budget }),
					question({ code: 'PROPERTY_VALUE', required: false }),
				],
			});

		expect(isSettled(compound(null))).toBe(false);
		expect(isSettled(compound('3000'))).toBe(true);
		expect(isBranchComplete([compound(null)])).toBe(false);
		expect(isBranchComplete([compound('3000')])).toBe(true);
	});
});

/**
 * Pytanie WARUNKOWE wewnątrz grupy — mechanizm, który pozwala grupie zapytać o coś dopiero po
 * odpowiedzi na inne swoje pytanie. Bez filtra po `revealed` grupa z takim pytaniem nigdy by się
 * nie domknęła, a łańcuch stanąłby na niej na zawsze.
 */
describe('pytanie warunkowe w grupie', () => {
	const group = (assistanceRevealed: boolean, assistanceAnswered: boolean) =>
		question({
			code: 'PROPERTY_NEED',
			fields: [
				question({ code: 'NEEDS_PROPERTY', required: true, value: 'true' }),
				question({
					code: 'HOME_ASSISTANCE',
					required: false,
					visible: assistanceRevealed,
					value: assistanceAnswered ? 'true' : null,
				}),
			],
		});

	it('is settled on the required field alone, before the conditional one appears', () => {
		expect(isSettled(group(false, false))).toBe(true);
	});

	it('stays settled once the conditional one appears and is skipped', () => {
		expect(isSettled(group(true, false))).toBe(true);
	});

	it('hides a conditional field from the rendered list until it is revealed', () => {
		expect(shownFields(group(false, false)).map((f) => f.code)).toEqual(['NEEDS_PROPERTY']);
		expect(shownFields(group(true, false)).map((f) => f.code)).toEqual([
			'NEEDS_PROPERTY',
			'HOME_ASSISTANCE',
		]);
	});
});
