/**
 * The catalogue of demos the landing screen offers.
 *
 * Deliberately a TypeScript constant rather than an endpoint or a Higson decision table. A new
 * industry is a new screen — it needs a component either way, so a build is unavoidable and a
 * runtime lookup would only add a request that can fail. It also keeps the landing screen working
 * on a fresh clone, which a rules-backed list would not: the snapshot is not in git.
 *
 * To add one: append an entry here, add the component, and extend the switch in `app.html`. The
 * `code` is the discriminator in all three places.
 *
 * ⛔ THE ORDER IS THE ARGUMENT, not alphabetical and not chronological. Each step hands the rules
 * engine something bigger: first the price, then which version of the tariff, then what the seller
 * is allowed to ask the client at all. Shuffling these turns a build-up into three unrelated
 * screens — the demo would still work and would stop meaning anything.
 */
export type DemoCode = 'moto' | 'life' | 'property';

export interface Demo {
	readonly code: DemoCode;
	/** Position in the story. Shown on the tile, because the sequence is the point. */
	readonly step: string;
	readonly name: string;
	/** One line, shown under the name on the tile. Written to be read in sequence. */
	readonly summary: string;
}

export const DEMOS: readonly Demo[] = [
	{
		code: 'moto',
		step: '01',
		name: 'Ubezpieczenie komunikacyjne',
		summary:
			'Cena i zakres każdego wariantu pochodzą z tabel decyzyjnych, nie z kodu. Zmień cokolwiek po lewej — przelicza się na bieżąco.',
	},
	{
		code: 'life',
		step: '02',
		name: 'Ubezpieczenie na życie',
		summary:
			'Ten sam silnik sprzedaje zupełnie inny produkt — a data polisy wybiera wersję taryfy i to, które świadczenia w ogóle istnieją.',
	},
	{
		code: 'property',
		step: '03',
		// ⚠️ Opis mówi, CO klient tu przechodzi, a nie z czego zbudowane jest demo. Pozostałe dwa
		// kafelki nadal opowiadają o silniku — do ujednolicenia, jeśli ekran wyboru też ma być
		// pokazywany klientom.
		name: 'Ubezpieczenie majątkowe',
		summary:
			'Analiza potrzeb klienta, dobór zakresu i oferta dla domu albo mieszkania — krok po kroku, tak jak u agenta.',
	},
];
