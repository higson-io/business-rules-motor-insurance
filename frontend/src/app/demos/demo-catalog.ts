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
 */
export type DemoCode = 'moto' | 'life';

export interface Demo {
	readonly code: DemoCode;
	readonly name: string;
	/** One line, shown under the name on the tile. */
	readonly summary: string;
	/**
	 * `false` renders the tile greyed out and unselectable. A demo appears here as soon as someone
	 * intends to build it, so the landing screen shows the roadmap rather than hiding it.
	 */
	readonly available: boolean;
}

export const DEMOS: readonly Demo[] = [
	{
		code: 'moto',
		name: 'Motor insurance',
		summary: 'Rate a car policy across three cover levels, with limits and discounts priced by rules.',
		available: true,
	},
	{
		code: 'life',
		name: 'Life insurance',
		summary:
			'Assemble an individual protection policy the way the Polish market sells one: a main contract plus riders, priced as you go.',
		available: true,
	},
];
