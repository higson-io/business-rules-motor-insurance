import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal } from '@angular/core';
import { MatDatepicker, MatDatepickerInput, MatDatepickerToggle } from '@angular/material/datepicker';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';

import { LifeStore } from '../../../core/life-store';
import { LifeService } from '../../../services/life-service';

/** Co ta data właśnie daje. Każda liczba tutaj rusza się przy zmianie wersji taryfy. */
interface Reading {
	readonly version: string;
	readonly validFrom: string;
	readonly catalogue: number;
	readonly catalogueUnit: string;
	readonly price: number | null;
	readonly priceUnit: string;
}

/**
 * Data, która wybiera wersję taryfy.
 *
 * To nie jest zwykłe pole formularza. Backend podaje ją do `HigsonEngine.setEffectiveDate`, więc oś
 * czasu regionu rozwiązuje się po dacie polisy, a nie po zegarze serwera. Przesunięcie przez próg
 * przełącza naraz wszystkie wersjonowane tabele — stawki, progi medyczne i dwa czynniki
 * antyselekcyjne — i sprawia, że pojawia się świadczenie, które wcześniej nie istnieje.
 *
 * ⛔ Stoi w powłoce, nie w jednej ze ścieżek, bo dotyczy obu. Kopia w każdej pozwoliłaby obu ekranom
 * twierdzić co innego o tym, którą taryfę wyceniają.
 *
 * ⛔ Data progu NIE jest tutaj znana i nie może być. Nazwę wersji podaje `demo.life.tariff.version`,
 * tabela wersjonowana regionem. Wcześniej stała tu `const VERSION_2_FROM = new Date('2026-07-01')` —
 * druga kopia reguły, w miejscu, w którym nikt by jej nie szukał, gdyby próg się przesunął.
 */
@Component({
	selector: 'app-life-effective-date',
	imports: [
		MatDatepicker,
		MatDatepickerInput,
		MatDatepickerToggle,
		MatFormField,
		MatInput,
		MatLabel,
		MatSuffix,
	],
	templateUrl: './effective-date.html',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EffectiveDate {
	private readonly store = inject(LifeStore);
	private readonly lifeService = inject(LifeService);

	/**
	 * ⚠️ `hasValue()` najpierw. Resource w stanie błędu rzuca z `value()`, więc czytanie wprost
	 * położyłoby ekran dokładnie na tej awarii, o której baner ma poinformować.
	 */
	private readonly loaded = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value() : null,
	);

	readonly startDate = computed(() => {
		const value = this.loaded()?.startDate;
		return value ? new Date(value) : null;
	});

	private readonly reading = computed<Reading | null>(() => {
		const quote = this.loaded();
		if (quote === null) {
			return null;
		}
		const group = quote.lineCode === 'GROUP';
		return {
			version: quote.tariffVersion.name,
			validFrom: quote.tariffVersion.validFrom,
			// Katalog grupowy rośnie o MENTAL_HEALTH_SUPPORT dopiero w wersji 2 — to jest ta
			// liczba, którą widz ma zobaczyć skaczącą.
			catalogue: group ? quote.benefits.length : quote.riders.length,
			catalogueUnit: group ? 'świadczeń w katalogu' : 'umów dodatkowych w ofercie',
			price: group ? quote.programCost : quote.premiumMonthly,
			priceUnit: group ? 'koszt programu (zł/mies.)' : 'składka miesięczna (zł)',
		};
	});

	/**
	 * Bieżący odczyt i ten poprzedni — ale tylko w poprzek zmiany wersji.
	 *
	 * `linkedSignal`, a nie `effect()` piszący do sygnału: poprzedni odczyt jest wyprowadzony ze
	 * źródła, więc nie może rozjechać się z tym, co jest na ekranie.
	 */
	private readonly readings = linkedSignal<Reading | null, { now: Reading; before: Reading | null } | null>({
		source: this.reading,
		computation: (now, previous) => {
			if (now === null) {
				return null;
			}
			const last = previous?.value?.now ?? null;
			return { now, before: last !== null && last.version !== now.version ? last : null };
		},
	});

	readonly now = computed(() => this.readings()?.now ?? null);
	readonly before = computed(() => this.readings()?.before ?? null);

	/**
	 * ⛔ Wysyła OBIEKT `Date`. `LifeApi.setStartDate` to `@RequestBody Date`; string przyszedłby
	 * jako `text/plain` i Jackson odpowiada 415.
	 */
	pick(value: Date | null): void {
		if (value === null) {
			return;
		}
		void this.store.commit(firstValueFrom(this.lifeService.updateStartDate(value)));
	}
}
