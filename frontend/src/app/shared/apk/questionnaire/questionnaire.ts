import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatFormField } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';

import {
	ApkAnswerChange,
	ApkCopy,
	ApkMultiAnswerChange,
	ApkQuestionDto,
	ApkRefusalChange,
	ApkSection,
} from '../../../models/apk';

/** Kolejność sekcji. Sekcja bez widocznych pytań nie jest renderowana wcale. */
const SECTION_ORDER: readonly ApkSection[] = ['SUBJECT', 'SITUATION', 'NEEDS', 'EXCLUSIONS'];

interface Section {
	readonly code: ApkSection;
	readonly title: string;
	readonly questions: readonly ApkQuestionDto[];
}

/**
 * Ankieta APK renderowana z reguł — wspólna dla wszystkich trzech linii produktowych.
 *
 * ⛔ Ani jedno pytanie nie jest tu wypisane, i ani jedna linia produktowa. Komponent dostaje listę,
 * renderuje kontrolkę per `kind` i zgłasza zmianę na zewnątrz. Nowa linia to nowy katalog pytań
 * w Studio i jeden `<app-apk-questionnaire>` w szablonie — nic więcej.
 *
 * ⛔ NIE wstrzykuje sklepu ani serwisu i to jest cała różnica między „wspólny" a „majątkowy, który
 * da się pożyczyć". Wysyłką zajmuje się gospodarz, bo to on wie, do którego demo należy.
 *
 * ⛔ Widoczność NIE jest tu liczona. `visible` przychodzi z serwera, gdzie rozstrzyga ją tabela
 * `*.apk.question.visibility`. Komponent filtrujący po własnej regule po cichu unieważniłby tamtą.
 */
@Component({
	selector: 'app-apk-questionnaire',
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
export class ApkQuestionnaire {
	readonly questions = input.required<readonly ApkQuestionDto[]>();

	/**
	 * ⛔ Wymagane, bez wartości domyślnej. Ankieta obsługuje ekran polski i angielski, a jedynym
	 * miejscem, które wie który jest który, jest gospodarz.
	 */
	readonly copy = input.required<ApkCopy>();

	readonly answered = output<ApkAnswerChange>();
	readonly multiAnswered = output<ApkMultiAnswerChange>();
	readonly refusalToggled = output<ApkRefusalChange>();

	readonly sections = computed<readonly Section[]>(() => {
		const questions = this.questions();
		return SECTION_ORDER.map((code) => ({
			code,
			title: this.copy().sections[code],
			questions: questions.filter((question) => question.section === code && question.visible),
		})).filter((section) => section.questions.length > 0);
	});

	label(question: ApkQuestionDto, option: string): string {
		const index = question.options.indexOf(option);
		return index >= 0 && index < question.optionLabels.length
			? question.optionLabels[index]
			: option;
	}

	isChecked(question: ApkQuestionDto, option: string): boolean {
		return question.values.includes(option);
	}

	answer(question: ApkQuestionDto, value: string): void {
		this.answered.emit({ code: question.code, value });
	}

	toggle(question: ApkQuestionDto, option: string, checked: boolean): void {
		const next = checked
			? [...question.values, option]
			: question.values.filter((value) => value !== option);
		this.multiAnswered.emit({ code: question.code, values: next });
	}

	/** „Nie chcę podać" — trzeci stan odpowiedzi. Nie blokuje wyceny; pokazuje konsekwencję. */
	refuse(question: ApkQuestionDto): void {
		this.refusalToggled.emit({ code: question.code, refused: !question.refused });
	}
}
