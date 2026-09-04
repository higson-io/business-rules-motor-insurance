import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { PropertyStore } from '../../../core/property-store';
import { PropertyService } from '../../../services/property-service';

/**
 * Ostatni krok: oferta, a po akceptacji — polisa.
 *
 * ⛔ **Domknięcie procesu, nie ozdoba.** Demo pokazywało analizę, rekomendację i cenę, po czym
 * urywało się w miejscu, w którym w prawdziwej sprzedaży coś się dopiero zaczyna. Ten ekran
 * pokazuje, że reguły nie kończą się na składce: to one mówią, ile oferta jest ważna, do kiedy
 * płatność i **co dokument musi zawierać**.
 *
 * ⛔ Treść oferty jest wersjonowana — pośrednio. `demo.property.offer.terms` kluczuje się KODEM
 * REŻIMU, a reżim idzie z regionu PROPERTY, więc przesunięcie daty polisy przez 1 lipca 2027
 * dokłada do oferty dokument analizy potrzeb i zmienia oświadczenie klienta. Drugi próg w drugim
 * miejscu rozjechałby się przy pierwszej zmianie terminu z uchwały.
 */
@Component({
	selector: 'app-property-offer',
	templateUrl: './offer.html',
	styleUrl: './offer.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Offer {
	private readonly store = inject(PropertyStore);
	private readonly propertyService = inject(PropertyService);

	/** ⚠️ `hasValue()` first — zasób w stanie błędu rzuca z `value()`. */
	private readonly loaded = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value() : null,
	);

	readonly offer = computed(() => this.loaded()?.offer ?? null);

	readonly issued = computed(() => this.offer()?.policyNumber !== null);

	/**
	 * Wariant dokumentu. Po wystawieniu — ten ZAMROŻONY w polisie, nie ten wybrany w wycenie.
	 *
	 * ⛔ Inaczej powrót do ankiety i zmiana odpowiedzi przestawiałaby zakres wydanej polisy.
	 */
	readonly variant = computed(() => {
		const quote = this.loaded();
		if (quote === null) {
			return null;
		}
		const code =
			quote.offer.variantCode ?? quote.selectedVariant ?? quote.recommendation.variantCode;
		return quote.variants.find((v) => v.code === code) ?? null;
	});

	/** Składka dokumentu — zamrożona po wystawieniu, z żywej wyceny dopóki to oferta. */
	readonly documentPremium = computed(
		() => this.offer()?.premium ?? this.variant()?.annualPremium ?? null,
	);

	/**
	 * Pokrycia wchodzące w wybrany wariant, z SUMĄ UBEZPIECZENIA — to jest treść ochrony.
	 *
	 * ⛔ `effectiveLimit` jest DRUGIM wyborem, nigdy pierwszym — to PODLIMIT, a nie suma:
	 * 34 848 zł na elementy stałe w pomieszczeniach przynależnych przy sumie 116 160 zł. Pokazany
	 * jako kwota ochrony zaniżał ją trzykrotnie. Podlimit jedzie osobno, jako `capLabel` pod nazwą
	 * pokrycia — tak samo jak w tabeli porównawczej. Sięgamy po niego tylko wtedy, gdy sumy nie ma
	 * wcale (assistance) i limit JEST całą kwotą ochrony.
	 *
	 * ⚠️ OC w życiu prywatnym było tu kiedyś trzecim przypadkiem: jego suma nie bierze się
	 * z metrażu, tylko z wariantu (`liabilitySum`), więc na pokryciu była pusta i dokument
	 * pokazywał kreskę zamiast dwóch milionów. Rozstrzygał o tym atrybut `sumBasis` z wartością
	 * `CATALOGUE`. Teraz backend wypełnia `sum` także dla OC — gdy tylko wiadomo, który wariant
	 * jest pokazywany — więc front nie musi wiedzieć, że OC jest inne.
	 */
	readonly covers = computed(() => {
		const quote = this.loaded();
		const chosen = this.variant();
		if (quote === null || chosen === null) {
			return [];
		}
		return quote.covers
			.filter((cover) => chosen.covers.includes(cover.code))
			.map((cover) => ({
				code: cover.code,
				name: cover.name,
				amount: cover.sum ?? cover.effectiveLimit,
				capLabel: cover.capLabel,
			}));
	});

	/**
	 * Rata pokazuje się tylko wtedy, gdy naprawdę jest ratą.
	 *
	 * ⚠️ Przy rocznym okresie tabela `demo.property.factor.period` daje jedną ratę, więc rata
	 * równa się składce — a zdanie „albo w ratach po 2912,65 zł" pod kwotą 2912,65 zł wygląda
	 * na usterkę renderowania, nie na ofertę.
	 */
	readonly instalment = computed(() => {
		const chosen = this.variant();
		if (this.issued() || chosen === null || chosen.instalment === null) {
			return null;
		}
		const total = chosen.annualPremium;
		return total !== null && chosen.instalment < total ? chosen.instalment : null;
	});

	readonly justification = computed(() => this.loaded()?.recommendation.justification ?? []);

	readonly needs = computed(() => this.loaded()?.recommendation.needs ?? []);

	readonly deductible = computed(() => this.loaded()?.deductible ?? null);

	readonly policyholder = computed(() => this.loaded()?.policyholder ?? null);

	/** Rek. 9.3 — notatka o zgodności wyboru z rekomendacją. */
	readonly choice = computed(() => {
		const offer = this.offer();
		if (offer === null || offer.choiceKind === null) {
			return null;
		}
		return {
			kind: offer.choiceKind,
			label: offer.choiceLabel,
			text: offer.choiceText,
			acknowledge: offer.choiceRequiresAcknowledgement,
			unmet: offer.unmetNeeds,
		};
	});

	issue(): void {
		void this.store.commit(firstValueFrom(this.propertyService.updatePolicyIssued(true)));
	}

	withdraw(): void {
		void this.store.commit(firstValueFrom(this.propertyService.updatePolicyIssued(false)));
	}

	// ── Formatowanie ────────────────────────────────────────────────────────────────────────
	// Przez `Intl`, a nie zarejestrowaną locale — aplikacja zostaje na en-US, bo goldeny
	// motoryzacyjne przypinają tamto formatowanie. Ta sama konstrukcja co w reszcie demo.
	private readonly whole = new Intl.NumberFormat('pl-PL', { maximumFractionDigits: 0 });
	private readonly pln = new Intl.NumberFormat('pl-PL', {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	});

	money(value: number | null): string {
		return value === null ? '—' : this.whole.format(value) + ' zł';
	}

	premium(value: number | null): string {
		return value === null ? '—' : this.pln.format(value) + ' zł';
	}

	/** `2028-01-15` → `15.01.2028`. Data przychodzi jako ISO i nie ma powodu jej parsować. */
	date(iso: string | null): string {
		if (iso === null) {
			return '—';
		}
		const [year, month, day] = iso.split('-');
		return `${day}.${month}.${year}`;
	}
}
