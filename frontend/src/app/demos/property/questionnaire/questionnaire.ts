import {
	ChangeDetectionStrategy,
	Component,
	computed,
	inject,
	input,
	linkedSignal,
	output,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatFormField } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';

/** Ostatnia deska ratunku, gdy katalog nie poda odpowiedzi dla pytania tak/nie. */
const BOOLEAN_FALLBACK: readonly string[] = ['true', 'false'];

import { PropertyStore } from '../../../core/property-store';
import { isSettled, shownFields } from '../../../core/question-state';
import { PropertyQuestionDto } from '../../../models/property';
import { PropertyService } from '../../../services/property-service';

/**
 * The APK questionnaire — and the component that proves the demo's claim.
 *
 * ⛔ It does not know a single question. It receives a list, renders a control per `kind`, and
 * sends the answer back under the question's own code. Adding a question in Higson Studio changes
 * this screen without touching this file — including the answers to offer, which come from the
 * catalogue and default to yes/no for every question that does not override them.
 *
 * ⛔ O tym, czy pytanie się zadaje, NIE rozstrzyga ten komponent. `visible` przychodzi z serwera,
 * gdzie jedna reguła kluczowana odpowiedziami mówi, o co pytamy tego klienta. Filtr po własnej
 * regule cicho odebrałby konfiguracji tę decyzję.
 */
/**
 * Rodzaje odpowiedzi udzielanych JEDNYM KLIKNIĘCIEM — tylko one przenoszą na następne pytanie.
 * Pole kwoty i tekstu emituje zdarzenie przy utracie ogniska, co nie jest kliknięciem w odpowiedź.
 */
const CLICK_ANSWERS = new Set<PropertyQuestionDto['kind']>(['BOOLEAN', 'CHOICE']);

@Component({
	selector: 'app-property-questionnaire',
	imports: [
		// Jeden szablon kontrolki użyty w trzech miejscach — patrz komentarz w szablonie.
		NgTemplateOutlet,
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
	// Gałąź decyduje o układzie: ankieta ma spis obok pytania, lista przedmiotu nie ma czego
	// obok siebie postawić. Klasa na gospodarzu, nie druga gałąź w szablonie (`components.md`).
	host: {
		'[class.apk--survey]': "group() === 'NEEDS'",
	},
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
	 * Która gałąź drzewa: `NEEDS` — analiza potrzeb, `SUBJECT` — przedmiot ubezpieczenia.
	 *
	 * ⚠️ Kolejność bierze się z `position` policzonego przez serwer — komponent niczego nie
	 * sortuje ani nie uzupełnia. Pytanie niewidoczne regułą i pytanie nieodsłonięte wypadają tak
	 * samo, bo dla czytelnika to ta sama sytuacja: nie ma o co pytać.
	 *
	 * ⛔ Gałąź decyduje też o UKŁADZIE. Analiza potrzeb to ankieta: jedno pytanie na ekran, po
	 * kolei, z własną nawigacją. Przedmiot ubezpieczenia to lista, bo tam nic się nie odsłania —
	 * dane są zebrane wcześniej i klient je najwyżej poprawia.
	 *
	 * ⛔ Sekcji nie ma. Do 4 września 2026 pytania grupowały się w sekcje z własnym tytułem
	 * i kolejnością; przy siedmiu pytaniach był to poziom pośredni, który niczego nie rozstrzygał.
	 * Podział przebiega teraz po gałęzi drzewa i tylko po niej.
	 */
	readonly group = input.required<'NEEDS' | 'SUBJECT'>();

	/**
	 * Licznik ponownych startów procesu — rośnie, gdy prowadzący czyści wniosek.
	 *
	 * ⛔ Potrzebny, bo kursor CELOWO nie rusza się przy przeliczeniu (patrz niżej), a wyczyszczenie
	 * odpowiedzi przeliczeniem właśnie jest: lista pytań ma tę samą długość, więc kursor zostawał
	 * na pytaniu, na którym stał, nad ankietą bez ani jednej odpowiedzi. Rozróżnienia „to samo
	 * przeliczenie" od „nowy proces" nie da się wyczytać z samych pytań — musi przyjść z zewnątrz.
	 */
	readonly restart = input(0);

	/** Zgłaszane, gdy klient domknął ostatnie pytanie ankiety — gospodarz przechodzi dalej. */
	readonly completed = output<void>();

	/**
	 * Wszystkie pytania tej gałęzi — w kolejności zadawania, także te, do których łańcuch jeszcze
	 * nie doszedł, bo licznik postępu musi znać całość.
	 *
	 * ⛔ Nie ma tu drugiego filtra i nie powinno być. Do 10 września 2026 stał tu `visible`,
	 * osobna reguła „czy pytanie dotyczy tego klienta" — dająca ten sam skutek co łańcuch
	 * (pytanie na ekranie albo go nie ma), tylko dla innych pytań. Zostało jedno pojęcie.
	 */
	readonly asked = computed<readonly PropertyQuestionDto[]>(() => {
		const quote = this.loaded();
		if (quote === null) {
			return [];
		}
		return quote.questions.filter((question) => question.section === this.group());
	});

	/**
	 * Te, do których łańcuch już doszedł. Ostatnie z nich jest bieżące w ankiecie, a na kroku
	 * drugim — gdzie pola pokazują się wszystkie naraz — to jest po prostu lista do wyświetlenia.
	 */
	readonly reachable = computed<readonly PropertyQuestionDto[]>(() =>
		this.asked().filter((question) => question.visible),
	);

	/**
	 * ⛔ Domknięcie pytania liczy `core/question-state`, nie ten komponent. Tę samą regułę
	 * potrzebuje gospodarz kroków, żeby powiedzieć, czy krok wolno opuścić — a dwie kopie
	 * rozjechałyby się przy pierwszej zmianie.
	 */
	private settled(question: PropertyQuestionDto): boolean {
		return isSettled(question);
	}

	/**
	 * Pytania grupy, które klient widzi — bez warunkowych, do których łańcuch jeszcze nie doszedł.
	 *
	 * ⛔ Filtrowanie należy do `core/question-state`, nie do szablonu: tę samą listę liczy
	 * domknięcie kroku, a dwie definicje „co widać" rozjechałyby się przy pierwszej zmianie.
	 */
	fieldsOf(question: PropertyQuestionDto): readonly PropertyQuestionDto[] {
		return shownFields(question);
	}

	/**
	 * Numer pytania oglądanego teraz.
	 *
	 * ⛔ **BEZ SAMOPRZESKOKU PO ODPOWIEDZI** (2026-09-09). Do tej daty kursor liczył się jako
	 * „pierwsze nieudzielone spośród odsłoniętych", więc odpowiedź na pytanie natychmiast
	 * przesuwała ekran na następne — w 80 ms, zmierzone. Skutek był taki, że klient PATRZYŁ ZAWSZE
	 * NA PYTANIE NIEUDZIELONE, a przycisk „Następne pytanie" był przez całą ankietę wyszarzały,
	 * bo istotnie nie było dokąd iść. Zapalał się wyłącznie na ostatnim pytaniu, i to tylko
	 * dlatego, że jest nieobowiązkowe. Zgłoszone z testów: „ten przycisk był ciągle wyszarzały".
	 *
	 * ⛔ W pliku siedziały przez to DWA SPRZECZNE PROJEKTY: komentarz przy `next()` deklarował
	 * nawigację jawną („ekran nie może uciec prowadzącemu"), a ten sygnał robił przeskok jako
	 * efekt uboczny przeliczenia. Osobno każde brzmiało sensownie — stąd nikt tego nie zauważył.
	 *
	 * Teraz kursor **zostaje tam, gdzie stoi**, dopóki mieści się w zakresie. Liczy się od nowa
	 * tylko wtedy, gdy nie ma czego utrzymać: przy pierwszym wejściu na krok i po zmianie, która
	 * skróciła listę odsłoniętych.
	 *
	 * ⚠️ Wtedy „pierwsze NIEUDZIELONE", nie „ostatnie odsłonięte" — wejście na krok z częściowo
	 * wypełnioną ankietą ma postawić klienta tam, gdzie skończył, a nie na końcu listy.
	 */
	private readonly cursor = linkedSignal<
		{ reached: readonly PropertyQuestionDto[]; restart: number },
		number
	>({
		source: () => ({ reached: this.reachable(), restart: this.restart() }),
		computation: (now, previous) => {
			// ⛔ Nowy proces wraca na PIERWSZE pytanie — dosłownie zero, a nie „pierwsze
			// nieudzielone". To nie jest uproszczenie, tylko jedyna wersja odporna na kolejność:
			// licznik ponownych startów rośnie, gdy PUT /reset już wrócił, ale wyczyszczona wycena
			// jeszcze nie — „pierwsze nieudzielone" liczyło się wtedy na STARYCH odpowiedziach
			// i reset lądował na pytaniu drugim. Zmierzone, nie przewidziane.
			const restarted = previous !== undefined && previous.source.restart !== now.restart;
			if (restarted) {
				return 0;
			}
			const kept = previous?.value;
			if (kept !== undefined && kept < now.reached.length) {
				return kept;
			}
			const pending = now.reached.findIndex((question) => !this.settled(question));
			return pending >= 0 ? pending : Math.max(now.reached.length - 1, 0);
		},
	});

	readonly current = computed<PropertyQuestionDto | null>(
		() => this.reachable()[this.cursor()] ?? null,
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
	 * Spis pytań kroku — lista obok ankiety, z makiety UX.
	 *
	 * ⛔ Liczy po `asked()`, czyli po REGULE widoczności, a nie po odsłonięciu. Spis, który rośnie
	 * w miarę odpowiadania, przestaje być spisem: nie da się z niego odczytać, ile jeszcze zostało,
	 * a to jest jedyny powód, dla którego stoi na ekranie. Odsłonięcie decyduje wyłącznie o tym,
	 * czy w daną pozycję wolno kliknąć.
	 *
	 * Etykieta pozycji spisu to nazwa grupy pytan.
	 * Ekran nie ma zapasowej nazwy wpisanej w kod, bo byłaby to nazwa pytania trzymana poza
	 * regułami, czyli dokładnie to, czego to demo dowodzi, że robić nie trzeba.
	 */
	readonly outline = computed(() => {
		const currentCode = this.current()?.code ?? null;
		return this.asked().map((question, index) => ({
			code: question.code,
			label: question.text,
			number: index + 1,
			current: question.code === currentCode,
			done: question.visible && question.code !== currentCode && this.settled(question),
			reachable: question.visible,
		}));
	});

	/**
	 * Skok do pytania ze spisu.
	 *
	 * ⛔ Tylko do pytania ODSŁONIĘTEGO. Skok do nieodsłoniętego przeskakiwałby łańcuch — a łańcuch
	 * jest konfiguracją, nie ozdobą: pytanie o wynajem ma sens dopiero po tym, jak wiadomo, że
	 * klient w ogóle chce ubezpieczać nieruchomość.
	 */
	goToQuestion(code: string): void {
		const index = this.reachable().findIndex((question) => question.code === code);
		if (index >= 0) {
			this.cursor.set(index);
		}
	}


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
	 * Jawne przejście dalej — przyciskiem albo klawiaturą.
	 *
	 * ⛔ To JEDYNE miejsce, które może zakończyć krok (`completed`). Samoczynny przeskok po
	 * kliknięciu odpowiedzi (patrz `autoAdvance`) przesuwa się tylko między pytaniami i na
	 * ostatnim zatrzymuje się — przejście do następnego ekranu zostaje decyzją prowadzącego,
	 * bo tam zaskoczenie kosztuje najwięcej.
	 */
	next(): void {
		if (!this.canAdvance()) {
			return;
		}
		// ⚠️ Nie przeskakuj poza to, co odsłonięte — łańcuch może prowadzić inaczej niż kolejność.
		if (this.cursor() < this.reachable().length - 1) {
			this.cursor.update((value) => value + 1);
		} else {
			this.completed.emit();
		}
	}

	previous(): void {
		this.cursor.update((value) => Math.max(value - 1, 0));
	}

	/**
	 * Możliwe odpowiedzi pytania tak/nie — z konfiguracji, nie z szablonu.
	 *
	 * ⛔ Do 8 września 2026 „Tak" i „Nie" były wpisane w HTML, w dwóch miejscach. Katalog pytań
	 * niesie je teraz jak każdą inną listę odpowiedzi, a wiersz gwiazdkowy daje je domyślnie
	 * każdemu pytaniu — więc pytanie o zgodę można przemianować na „Zdecydowanie tak / Raczej nie"
	 * jedną komórką. Zaszyta para zostaje wyłącznie jako ostatnia deska ratunku, gdyby katalog
	 * milczał: pytanie bez odpowiedzi do wyboru byłoby ślepym zaułkiem ankiety.
	 */
	answersOf(question: PropertyQuestionDto): readonly string[] {
		return question.options.length > 0 ? question.options : BOOLEAN_FALLBACK;
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
		const from = this.cursor();
		void this.store
			.commit(firstValueFrom(this.propertyService.updateAnswer(question.code, value)))
			.then(() => this.autoAdvance(question, from));
	}

	/**
	 * Kliknięcie odpowiedzi przenosi na następne pytanie — rytm ankiety, nie formularza.
	 *
	 * ⛔ To NIE jest powrót do stanu sprzed 9 września. Wtedy przeskok był efektem ubocznym
	 * PRZELICZENIA: kursor liczył się od nowa po każdym odświeżeniu wyceny i uciekał także wtedy,
	 * gdy nikt nic nie kliknął. Tutaj jest skutkiem KLIKNIĘCIA, w obsłudze zdarzenia, i dlatego
	 * daje się ograniczyć czterema warunkami, z których każdy ma powód:
	 *
	 * 1. tylko ankieta (`NEEDS`) — krok przedmiotu pokazuje wszystkie pola naraz, nie ma dokąd iść;
	 * 2. tylko odpowiedź KLIKANA (`BOOLEAN`, `CHOICE`) — pole kwoty i tekstu emituje `change`
	 *    przy utracie ogniska, więc przeskok wyrywałby ekran w trakcie pisania;
	 * 3. tylko pytanie PROSTE — w pytaniu złożonym jedno pole nie domyka kroku;
	 * 4. tylko gdy prowadzący nie ruszył kursora sam w międzyczasie.
	 *
	 * ⛔ Na ostatnim pytaniu przeskoku NIE MA. Krok kończy się przyciskiem, świadomie.
	 *
	 * ⚠️ Przesunięcie następuje PO domknięciu zapisu, żeby łańcuch odsłaniania zdążył się
	 * przeliczyć: odpowiedź „tak" na pytanie o wynajem odsłania pytanie o pomoc fachowca, które
	 * wchodzi na listę tuż za nim.
	 */
	private autoAdvance(answered: PropertyQuestionDto, from: number): void {
		const question = this.current();
		if (
			this.group() !== 'NEEDS' ||
			question === null ||
			question.fields.length > 1 ||
			!CLICK_ANSWERS.has(answered.kind) ||
			this.cursor() !== from
		) {
			return;
		}
		if (this.cursor() < this.reachable().length - 1) {
			this.cursor.update((value) => value + 1);
		}
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
