import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { LifeStore } from '../../../core/life-store';
import { ApkAnswerChange, ApkCopy, ApkRefusalChange } from '../../../models/apk';
import { LifeService } from '../../../services/life-service';
import { ApkQuestionnaire } from '../../../shared/apk/questionnaire/questionnaire';

/** Napisy własne ankiety — po polsku, bo całe demo życiowe jest po polsku. */
const COPY: ApkCopy = {
	sections: {
		SUBJECT: 'Twoja sytuacja finansowa',
		SITUATION: 'Co już masz',
		NEEDS: 'Czego potrzebujesz',
		EXCLUSIONS: 'Co może ograniczyć ochronę',
	},
	yes: 'Tak',
	no: 'Nie',
	prefilledFrom: 'Podpowiedziane:',
	refuse: 'Nie chcę podać',
	unrefuse: 'Jednak podam',
};

/**
 * Analiza potrzeb klienta na demie życiowym.
 *
 * ⛔ Ankieta jest WSPÓLNA z demem komunikacyjnym i majątkowym — ten komponent tylko wie, dokąd
 * wysłać odpowiedź i w jakim języku mówi ekran. Cała różnica między liniami produktowymi siedzi
 * w katalogu pytań w Studio.
 *
 * ⛔ Ścieżka grupowa nie pyta o dane osobowe i rozstrzyga o tym REGUŁA, nie ten komponent:
 * `demo.life.apk.question.visibility` ma wiersze `GROUP → false` dla dziesięciu pytań. Polisa
 * grupowa nie prowadzi indywidualnej oceny ryzyka, więc pytanie o osoby na utrzymaniu byłoby
 * pytaniem bez konsekwencji.
 */
@Component({
	selector: 'app-life-apk',
	imports: [ApkQuestionnaire],
	templateUrl: './life-apk.html',
	styleUrl: './life-apk.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LifeApk {
	private readonly store = inject(LifeStore);
	private readonly lifeService = inject(LifeService);

	readonly copy = COPY;

	/** ⚠️ `hasValue()` najpierw — zasób w stanie błędu rzuca z `value()`. */
	private readonly loaded = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value() : null,
	);

	readonly ready = computed(() => this.loaded() !== null);
	readonly questions = computed(() => this.loaded()?.apkQuestions ?? []);
	readonly regime = computed(() => this.loaded()?.apkRegime ?? null);
	readonly visibleCount = computed(() => this.questions().filter((q) => q.visible).length);

	onAnswered(change: ApkAnswerChange): void {
		void this.store.commit(
			firstValueFrom(this.lifeService.updateApkAnswer(change.code, change.value)),
		);
	}

	onRefusalToggled(change: ApkRefusalChange): void {
		void this.store.commit(
			firstValueFrom(this.lifeService.updateApkRefused(change.code, change.refused)),
		);
	}
}
