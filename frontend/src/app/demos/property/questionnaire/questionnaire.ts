import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';

import { PropertyStore } from '../../../core/property-store';
import { PropertyQuestionDto, QuestionSection } from '../../../models/property';
import { PropertyService } from '../../../services/property-service';

/** Section headings. The sections themselves come from the rules; only their titles live here. */
const SECTION_TITLES: Record<QuestionSection, string> = {
	SUBJECT: 'Przedmiot ubezpieczenia',
	SITUATION: 'Twoja sytuacja',
	NEEDS: 'Czego potrzebujesz',
	EXCLUSIONS: 'Co może ograniczyć ochronę',
};

/** The order sections appear in. A section with no visible questions is not rendered at all. */
const SECTION_ORDER: readonly QuestionSection[] = ['SUBJECT', 'SITUATION', 'NEEDS', 'EXCLUSIONS'];

interface Section {
	readonly code: QuestionSection;
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
		MatHint,
		MatInput,
		MatLabel,
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

	readonly sections = computed<readonly Section[]>(() => {
		const questions = this.loaded()?.questions ?? [];
		return SECTION_ORDER.map((code) => ({
			code,
			title: SECTION_TITLES[code],
			questions: questions.filter((question) => question.section === code && question.visible),
		})).filter((section) => section.questions.length > 0);
	});

	/** How many questions the rules currently show — the number the demo is about. */
	readonly visibleCount = computed(
		() => (this.loaded()?.questions ?? []).filter((question) => question.visible).length,
	);

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
