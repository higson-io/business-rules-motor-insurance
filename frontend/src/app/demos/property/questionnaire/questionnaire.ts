import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
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
	private readonly all = computed<readonly (Section & { readonly kind: string })[]>(() => {
		const quote = this.loaded();
		if (quote === null) {
			return [];
		}
		return quote.sections
			.filter((section) => section.visible)
			.map((section) => ({
				code: section.code,
				title: section.title,
				kind: section.kind,
				questions: quote.questions.filter(
					(question) => question.section === section.code && question.visible,
				),
			}))
			.filter((section) => section.questions.length > 0);
	});

	/** Sekcje, o które PYTAMY. */
	readonly sections = computed<readonly Section[]>(() =>
		this.all().filter((section) => section.kind !== 'KNOWN'),
	);

	/**
	 * Sekcje, które już ZNAMY — charakterystyka przedmiotu ubezpieczenia i dane oceny ryzyka.
	 *
	 * ⛔ Nie ma tu ani jednego kodu sekcji. Podziałem rządzi atrybut `KIND` z Higsona, więc
	 * przeniesienie sekcji między paskiem a ankietą jest zmianą konfiguracji. To nie jest
	 * kosmetyka: analiza potrzeb to pytania o POTRZEBY, a metraż i rok budowy potrzebą nie są —
	 * zakłady zbierają je i dziś, poza APK.
	 */
	readonly known = computed<readonly Section[]>(() =>
		this.all().filter((section) => section.kind === 'KNOWN'),
	);

	/** Skrót paska: same wartości, w kolejności pytań, żeby dało się je przeczytać jednym rzutem. */
	readonly knownSummary = computed<string>(() =>
		this.known()
			.flatMap((section) => section.questions)
			.map((question) => this.display(question))
			.filter((value) => value !== '')
			.join(' · '),
	);

	readonly knownCount = computed<number>(() =>
		this.known().reduce((count, section) => count + section.questions.length, 0),
	);

	readonly expanded = signal(false);

	toggleKnown(): void {
		this.expanded.update((open) => !open);
	}

	/** Wartość odpowiedzi po ludzku: etykieta wyboru zamiast kodu, „—" zamiast pustki. */
	display(question: PropertyQuestionDto): string {
		if (question.refused) {
			return 'nie podano';
		}
		if (question.values.length > 0) {
			return question.values.map((value) => this.label(question, value)).join(', ');
		}
		return question.value === null || question.value === '' ? '' : this.label(question, question.value);
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

	/**
	 * „Nie chcę podać" — trzeci stan odpowiedzi (decyzja D-6). Nie blokuje wyceny; pokazuje
	 * konsekwencję, którą podaje reguła.
	 */
	refuse(question: PropertyQuestionDto): void {
		void this.store.commit(
			firstValueFrom(this.propertyService.updateRefused(question.code, !question.refused)),
		);
	}
}
