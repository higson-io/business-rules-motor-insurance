import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { PropertyStore } from '../../../core/property-store';

/**
 * Karta klienta na górze ekranu — to, co agent ma przed sobą, zanim zacznie rozmowę.
 *
 * ⛔ Zajęła miejsce paska technicznego (data polisy, nazwa reżimu, liczniki pytań). Tamten pasek
 * mówił prawdę o mechanizmie, ale mówił ją najgłośniejszą rzeczą na ekranie — a demo pokazuje
 * proces sprzedaży, nie panel konfiguracji. Reżim i data zeszły do wysuwanego panelu, gdzie są
 * o jedno kliknięcie, a pierwszy plan dostał to, co w tym miejscu jest w prawdziwej aplikacji.
 *
 * ⛔ DANE DEMONSTRACYJNE i ekran mówi to wprost. W prawdziwym wdrożeniu przyszłyby z CRM, a nie
 * z reguł — dlatego `source` jedzie z serwera razem z nimi i jest tu renderowane. Karta klienta
 * bez podanego źródła wygląda jak dane prawdziwe, a to jest ostatnia rzecz, jakiej chcemy na
 * ekranie pokazywanym obcym ludziom.
 */
@Component({
	selector: 'app-property-client',
	templateUrl: './client.html',
	styleUrl: './client.less',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Client {
	private readonly store = inject(PropertyStore);

	/** ⚠️ `hasValue()` przed `value()` — zasób w stanie błędu RZUCA z `value()` w Angularze 22. */
	private readonly loaded = computed(() =>
		this.store.quote.hasValue() ? this.store.quote.value() : null,
	);

	readonly client = computed(() => this.loaded()?.policyholder ?? null);

	readonly name = computed(() => {
		const client = this.client();
		return client === null ? '' : `${client.firstName} ${client.lastName}`;
	});

	/** Adres w jednej linii — dwie linijki w karcie nagłówkowej rozbijają rytm wiersza. */
	readonly address = computed(() => {
		const client = this.client();
		if (client === null) {
			return '';
		}
		const street = client.flat ? `${client.street}/${client.flat}` : client.street;
		return `${street}, ${client.postcode} ${client.city}`;
	});

	/** Inicjały do awatara. Puste nazwisko daje jedną literę, a nie pustą kropkę. */
	readonly initials = computed(() => {
		const client = this.client();
		if (client === null) {
			return '';
		}
		return [client.firstName, client.lastName]
			.filter((part) => part.length > 0)
			.map((part) => part[0].toUpperCase())
			.join('');
	});
}
