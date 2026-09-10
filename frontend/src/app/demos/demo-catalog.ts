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
}

/*
 * ⛔ OPISY KAFELKÓW USUNIĘTE 2026-09-09. Każdy kafelek niósł zdanie o tym, z czego demo jest
 * zbudowane („pochodzą z tabel decyzyjnych, nie z kodu", „ten sam silnik sprzedaje…") — czyli
 * reklamę narzędzia postawioną przed produktem, tę samą, którą §21 zdjął z ekranu majątkowego
 * i zostawił tutaj jako otwartą pozycję. Numer i nazwa mówią, co wybierasz; z czego to zbudowane,
 * pokazuje samo demo.
 */

export const DEMOS: readonly Demo[] = [
	{
		code: 'moto',
		step: '01',
		name: 'Ubezpieczenie komunikacyjne',
	},
	{
		code: 'life',
		step: '02',
		name: 'Ubezpieczenie na życie',
	},
	{
		code: 'property',
		step: '03',
		name: 'Ubezpieczenie majątkowe',
	},
];
