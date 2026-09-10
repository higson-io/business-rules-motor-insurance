import { PropertyQuestionDto } from '../models/property';

/**
 * Kiedy pytanie jest domknięte — jedno miejsce dla całego dema majątkowego.
 *
 * ⛔ Było w dwóch: ankieta liczyła to u siebie, a gospodarz kroków potrzebował tego samego,
 * żeby powiedzieć, czy krok wolno już opuścić. Dwie kopie tej samej reguły rozjechałyby się przy
 * pierwszej zmianie — a zmiana już raz przyszła, gdy pojawiły się pytania złożone.
 *
 * ⚠️ Ta sama reguła stoi po stronie serwera (`Property.allRequiredAnswered`) i to nie jest
 * duplikat do usunięcia: serwer prowadzi łańcuch odsłaniania, ekran włącza przyciski. Obie strony
 * muszą odpowiadać tak samo, inaczej ankieta pokazuje „dalej" tam, gdzie serwer nie odsłonił
 * następnego pytania.
 */

/** Czy pod kodem tego pytania leży odpowiedź. */
export function isAnswered(question: PropertyQuestionDto): boolean {
	return question.refused || question.values.length > 0 || !!question.value;
}

/**
 * Pytania grupy, które klient NA PEWNO widzi.
 *
 * ⛔ Grupa może mieć pytania warunkowe — takie, które odsłania dopiero odpowiedź na inne pytanie
 * tej samej grupy — regułą widoczności. Pytanie, o które nie pytamy, nie może ani
 * się renderować, ani decydować o domknięciu kroku: klient go nie widział.
 */
export function shownFields(question: PropertyQuestionDto): readonly PropertyQuestionDto[] {
	return question.fields.filter((field) => field.visible);
}

/**
 * Czy klient domknął to pytanie.
 *
 * ⛔ Pytanie ZŁOŻONE nie ma własnej odpowiedzi — domykają je pola. Wymagane muszą być wszystkie;
 * gdy żadne nie jest wymagane, wystarczy jedno, bo inaczej pytanie z samych pól nieobowiązkowych
 * byłoby ślepym zaułkiem.
 *
 * ⛔ Liczą się WYŁĄCZNIE pola odsłonięte. Pole warunkowe, którego klient jeszcze nie zobaczył,
 * blokowałoby krok warunkiem niemożliwym do spełnienia — a pole odsłonięte i pominięte blokuje
 * tylko wtedy, gdy jest obowiązkowe, jak każde inne.
 */
export function isSettled(question: PropertyQuestionDto): boolean {
	if (question.fields.length === 0) {
		return isAnswered(question);
	}
	const shown = shownFields(question);
	const required = shown.filter((field) => field.required);
	return required.length > 0
		? required.every(isAnswered)
		: shown.some(isAnswered);
}

/**
 * Czy gałąź pytań jest domknięta — czyli czy nie ma już nic, o co MUSIMY zapytać.
 *
 * ⛔ „Wszystkie odsłonięte i każde wymagane domknięte", a nie „wszystkie domknięte". Pytanie
 * nieobowiązkowe wolno pominąć (§22 zdjął z ekranu odmowę i to obowiązkowość przejęła jej rolę),
 * więc warunek „wszystkie domknięte" zostawiałby krok otwarty na zawsze, gdy klient pominie
 * oczekiwaną składkę.
 *
 * ⛔ Warunek „wszystkie odsłonięte" jest tym, co łapie NIEUDZIELONE PYTANIE OBOWIĄZKOWE: łańcuch
 * o pytanie, o które nie pytamy, nie wolno się zaciąć — nie może być domknięte. Dlatego
 * ekran nie musi znać łańcucha — wystarczy, że policzy, czy doszedł do końca.
 */
export function isBranchComplete(questions: readonly PropertyQuestionDto[]): boolean {
	if (questions.length === 0) {
		return false;
	}
	return questions.every(
		(question) => question.visible && (!question.required || isSettled(question)),
	);
}
