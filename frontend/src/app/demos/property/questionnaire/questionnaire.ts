import {
	ChangeDetectionStrategy,
	Component,
	computed,
	inject,
	input,
	linkedSignal,
	output,
} from '@angular/core';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatFormField } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';

import { PropertyStore } from '../../../core/property-store';
import { PropertyQuestionDto } from '../../../models/property';
import { PropertyService } from '../../../services/property-service';

/**
 * ⛔ Nie ma tu ani listy sekcji, ani ich tytułów, ani kolejności — i to jest cała zmiana.
 * Do 28 sierpnia 2026 stały tu cztery tytuły i tablica `SECTION_ORDER`, więc dołożenie sekcji
 * w Studio wymagało wdrożenia frontu. Sekcja jest teraz gałęzią drzewa domeny: przychodzi
 * z serwera z tytułem, kolejnością i własną regułą widoczności.
 */
interface Section {
	readonly code: string;
	readonly title: string;
	readonly questions: readonly PropertyQuestionDto[];
}


/**
 * The APK questionnaire — and the component that proves the demo's claim.
 *
 * ⛔ It does not know a single question. It receives a list, renders a control per `kind`, and
 * sends the answer back under the question's own code. Adding a question in Higson Studio changes
 * this screen without touching this file, and moving the policy date across 1 July 2027 changes it
 * without touching anything at all.
 *
 * ⛔ Visibility is NOT computed here. `visible` arrives from the server, where
 * `demo.property.apk.question.visibility` decides it. A component that filtered by its own rules
 * would quietly make that table stop being a rule.
 */
@Component({
	selector: 'app-property-questionnaire',
	imports: [
		MatButtonToggle,
		MatButtonToggleGroup,
		MatCheckbox,
		MatFormField,
		MatInput,
		MatOption,
		MatSelect,
	],
	templateUrl: './questionnaire.html',
	styleUrl: './questionnaire.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Questionnaire {
	private readonly store = inject(PropertyStore);
	private readonly propertyService = inject(PropertyService);

	/**
	 * ⚠️ `hasValue()` before `value()`. A resource in the error state THROWS `ResourceValueError`
	 * in Angular 22, so `value()?.x ?? y` would not degrade — it would take the screen down.
	 */
	private readonly loaded = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value() : null,
	);

	readonly ready = computed(() => this.loaded() !== null);

	/**
	 * ⚠️ Kolejność bierze się z `position` policzonego przez serwer, a lista sekcji z serwera —
	 * komponent niczego nie sortuje ani nie uzupełnia. Sekcja niewidoczna regułą i sekcja pusta
	 * wypadają tak samo, bo dla czytelnika to ta sama sytuacja: nie ma o co pytać.
	 */
	/**
	 * Które sekcje renderować — i, co za tym idzie, JAK.
	 *
	 * `FORM` — analiza potrzeb: **jedno pytanie na ekran**, po kolei, z własną nawigacją.
	 * `KNOWN` — przedmiot ubezpieczenia: lista, bo to dane już zebrane, a nie rozmowa.
	 *
	 * ⛔ Rodzaj przychodzi z Higsona (atrybut `KIND` sekcji), a komponent dostaje tylko to, którą
	 * połowę ma pokazać. Nie ma tu ani jednego kodu sekcji: przeniesienie sekcji z kroku 1 do
	 * kroku 2 jest zmianą jednego atrybutu w Studio.
	 */
	readonly kind = input.required<'FORM' | 'KNOWN'>();

	/** Zgłaszane, gdy klient domknął ostatnie pytanie ankiety — gospodarz przechodzi dalej. */
	readonly completed = output<void>();

	/**
	 * ⚠️ Kolejność bierze się z `position` policzonego przez serwer, a lista sekcji z serwera —
	 * komponent niczego nie sortuje ani nie uzupełnia. Sekcja niewidoczna regułą i sekcja pusta
	 * wypadają tak samo, bo dla czytelnika to ta sama sytuacja: nie ma o co pytać.
	 *
	 * ⚠️ `visible` to reguła („czy pytanie dotyczy tego klienta"), `revealed` to łańcuch („czy już
	 * pora"). Sekcje trzymają pytania przefiltrowane po REGULE — odsłanianie rozstrzyga się niżej,
	 * przy wyborze pytania bieżącego, bo licznik postępu musi znać całość.
	 */
	readonly sections = computed<readonly Section[]>(() => {
		const quote = this.loaded();
		if (quote === null) {
			return [];
		}
		return quote.sections
			.filter((section) => section.visible && section.kind === this.kind())
			.map((section) => ({
				code: section.code,
				title: section.title,
				questions: quote.questions.filter(
					(question) => question.section === section.code && question.visible,
				),
			}))
			.filter((section) => section.questions.length > 0);
	});

	/** Wszystkie pytania, o które w tym kroku wolno zapytać — w kolejności zadawania. */
	private readonly asked = computed<readonly PropertyQuestionDto[]>(() =>
		this.sections().flatMap((section) => section.questions),
	);

	/** Te, do których łańcuch już doszedł. Ostatnie z nich jest bieżące. */
	private readonly reached = computed<readonly PropertyQuestionDto[]>(() =>
		this.asked().filter((question) => question.revealed),
	);

	/** Czy klient na to pytanie odpowiedział. Odpowiednik serwerowego `isSettled()`. */
	private settled(question: PropertyQuestionDto): boolean {
		return question.refused || question.values.length > 0 || !!question.value;
	}

	/**
	 * Numer pytania oglądanego teraz.
	 *
	 * ⛔ `linkedSignal`, nie `signal`, i to daje DWIE rzeczy naraz. Po odpowiedzi łańcuch odsłania
	 * kolejne pytanie, więc `reached` się zmienia, kursor się przelicza i ekran sam przechodzi na
	 * to nowe pytanie — czyli ankieta zachowuje się jak ankieta, bez osobnego mechanizmu
	 * przewijania. A gdy klient cofnie się i poprawi odpowiedź, która niczego nie odsłania,
	 * `reached` nie drgnie i kursor zostaje tam, gdzie klient go postawił.
	 *
	 * ⚠️ Pierwsze NIEUDZIELONE, nie ostatnie odsłonięte. Ostatnie odsłonięte przy wejściu na krok
	 * z częściowo wypełnioną ankietą wrzucałoby klienta na koniec listy zamiast tam, gdzie skończył.
	 */
	private readonly cursor = linkedSignal<readonly PropertyQuestionDto[], number>({
		source: this.reached,
		computation: (reached, previous) => {
			const pending = reached.findIndex((question) => !this.settled(question));
			const fallback = pending >= 0 ? pending : Math.max(reached.length - 1, 0);
			const kept = previous?.value;
			return kept !== undefined && kept < reached.length && kept > fallback ? kept : fallback;
		},
	});

	readonly current = computed<PropertyQuestionDto | null>(
		() => this.reached()[this.cursor()] ?? null,
	);

	/**
	 * Pytanie bieżące jako JEDNOELEMENTOWA LISTA — po to, żeby szablon mógł je śledzić po kodzie.
	 *
	 * ⛔ NOŚNE, nie stylistyka. `@if` nie ma klucza, więc Angular utrzymywał ten sam węzeł
	 * `mat-button-toggle-group` przy przejściu do następnego pytania i podmieniał mu tylko
	 * wartość. Gdy dwa kolejne pytania miały TĘ SAMĄ odpowiedź — „Nie" przy powodzi, potem „Nie"
	 * przy sprzęcie przenośnym — Material nie widział zmiany, nie emitował `change`, odpowiedź
	 * nie leciała na serwer i ankieta stawała w miejscu z wyłączonym „Dalej".
	 *
	 * ⚠️ Objaw był mylący: klikało się i nic się nie działo, więc wyglądało to na zgubione
	 * zdarzenie albo wyścig z przeładowaniem. `@for` z `track question.code` odtwarza kontrolkę
	 * przy każdej zmianie pytania i problem znika u źródła.
	 */
	readonly currentAsList = computed<readonly PropertyQuestionDto[]>(() => {
		const question = this.current();
		return question === null ? [] : [question];
	});

	/** „Pytanie 3 z 7" — mianownik liczy po REGULE, więc nie rośnie w trakcie odpowiadania. */
	readonly stepNumber = computed(() => this.cursor() + 1);
	readonly stepCount = computed(() => this.asked().length);

	/**
	 * Nazwa sekcji, w której stoi bieżące pytanie.
	 *
	 * ⛔ Do 4 września 2026 sekcje w ankiecie były NIEWIDOCZNE — istniały w domenie, dzieliły
	 * pytania i nie mówiły na ekranie nic. Przy jednym pytaniu na ekran klient przechodził od
	 * potrzeb przez wymagania do wyłączeń, nie wiedząc, że zmienił temat rozmowy; a próg KNF,
	 * który dokłada DWA CAŁE OBSZARY pytań, wyglądał jak siedem pytań znikąd.
	 */
	readonly sectionTitle = computed<string | null>(() => {
		const question = this.current();
		if (question === null) {
			return null;
		}
		return this.sections().find((section) => section.code === question.section)?.title ?? null;
	});

	/** Pozycja bieżącego pytania w całej ankiecie — potrzebna paskowi, nie widzowi. */
	private readonly askedIndex = computed(() => {
		const question = this.current();
		return question === null
			? -1
			: this.asked().findIndex((asked) => asked.code === question.code);
	});

	/**
	 * Pasek postępu podzielony na sekcje.
	 *
	 * ⛔ Segment na sekcję, szeroki proporcjonalnie do liczby jej pytań. Jeden ciągły pasek pokazuje
	 * tylko „ile jeszcze"; podzielony pokazuje też Z CZEGO — a po przesunięciu daty przez próg KNF
	 * segmenty przybywają na oczach widza. To jest ta sama informacja co „12 pytań, było 5"
	 * w panelu ustawień, tyle że bez otwierania panelu.
	 *
	 * ⚠️ Sekcje są rozłączne i idą po kolei, bo `asked` powstaje ze spłaszczenia `sections()` —
	 * dlatego wystarczy bieżące przesunięcie, bez szukania pytań po kodach.
	 */
	readonly segments = computed(() => {
		const at = this.askedIndex();
		let start = 0;
		return this.sections().map((section) => {
			const total = section.questions.length;
			const end = start + total;
			const done = Math.min(Math.max(at - start + 1, 0), total);
			const segment = {
				code: section.code,
				total,
				done,
				current: at >= start && at < end,
			};
			start = end;
			return segment;
		});
	});

	readonly hasPrevious = computed(() => this.cursor() > 0);

	/**
	 * Czy po tym pytaniu jest jeszcze jakieś — liczone po REGULE, nie po odsłonięciu.
	 *
	 * ⛔ Po odsłonięciu byłoby źle i to widać od razu: przy pierwszym pytaniu odsłonięte jest
	 * tylko ono, więc przycisk mówiłby „Zakończ analizę potrzeb" na pytaniu 1 z 5.
	 */
	readonly hasNext = computed(() => this.cursor() < this.stepCount() - 1);

	/**
	 * Czy wolno przejść dalej.
	 *
	 * ⛔ Dopóki klient nie odpowie na pytanie OBOWIĄZKOWE, następne nie jest odsłonięte — nie ma
	 * dokąd iść. Wyszarzony przycisk mówi to wprost, zamiast pozwalać kliknąć i nie zrobić nic.
	 *
	 * ⚠️ Pytanie nieobowiązkowe wolno pominąć. Do 4 września 2026 rolę tę pełniła odmowa („nie
	 * chcę podać"), która domykała pytanie bez odpowiadania; po jej usunięciu bez tej gałęzi
	 * oczekiwana składka byłaby ślepym zaułkiem.
	 */
	readonly canAdvance = computed(() => {
		const question = this.current();
		return question !== null && (this.settled(question) || !question.required);
	});

	/**
	 * ⛔ Nawigacja jest JAWNA, bez samoczynnego przeskoku po kliknięciu odpowiedzi. Na prezentacji
	 * ekran nie może uciec prowadzącemu w chwili, w której zaczyna mówić o tym, co się właśnie
	 * przeliczyło — a przy pytaniach z polem tekstowym samoprzewijanie i tak nie miałoby czego
	 * nasłuchiwać.
	 */
	next(): void {
		if (!this.canAdvance()) {
			return;
		}
		// ⚠️ Nie przeskakuj poza to, co odsłonięte — łańcuch może prowadzić inaczej niż kolejność.
		if (this.cursor() < this.reached().length - 1) {
			this.cursor.update((value) => value + 1);
		} else {
			this.completed.emit();
		}
	}

	previous(): void {
		this.cursor.update((value) => Math.max(value - 1, 0));
	}

	label(question: PropertyQuestionDto, option: string): string {
		const index = question.options.indexOf(option);
		return index >= 0 && index < question.optionLabels.length
			? question.optionLabels[index]
			: option;
	}

	isChecked(question: PropertyQuestionDto, option: string): boolean {
		return question.values.includes(option);
	}

	answer(question: PropertyQuestionDto, value: string): void {
		void this.store.commit(firstValueFrom(this.propertyService.updateAnswer(question.code, value)));
	}

	toggle(question: PropertyQuestionDto, option: string, checked: boolean): void {
		const next = checked
			? [...question.values, option]
			: question.values.filter((value) => value !== option);
		void this.store.commit(
			firstValueFrom(this.propertyService.updateMultiAnswer(question.code, next)),
		);
	}

}
