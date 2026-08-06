import type { Page, Request, Response } from '@playwright/test';

/**
 * Request counting (implementation plan, step 2.4).
 *
 * This is a FIRST-CLASS assertion, not a nicety: DR-02, DR-03, DR-04, DR-08, DR-09 and
 * DR-13 are observable ONLY as changes in request volume. DR-13 / ND-1 in particular -
 * "only a vehicle MODEL change triggers a quote reload" - is invisible to every other
 * kind of assertion, and an idiomatic reactive rewrite reconnects it by accident.
 */

/**
 * The frozen REST surface (spec.md 6.2). Exactly 21 URLs, relative, no leading slash.
 */
export const API_PATHS = [
	'dictionaries/make',
	'dictionaries/model',
	'dictionaries/productionYear',
	'dictionaries/type',
	'driver',
	'driver/birthDate',
	'driver/firstName',
	'driver/gender',
	'driver/lastName',
	'driver/accidentCount',
	'driver/trafficTicketsCount',
	'driver/licenceObtainedAtAge',
	'driver/address/zipCode',
	'driver/address/city',
	'driver/address/street',
	'vehicle',
	'vehicle/make',
	'vehicle/model',
	'vehicle/type',
	'vehicle/productionYear',
	'quote'
] as const;

export type ApiPath = (typeof API_PATHS)[number];

export interface RecordedRequest {
	readonly method: string;
	/** Relative to the application root, no leading slash, no query string. */
	readonly path: string;
	/** Sorted `key=value` pairs, or `''` when there were none. */
	readonly query: string;
	/** True when `path` is one of the 21 frozen endpoints. */
	readonly api: boolean;
}

/**
 * Records application HTTP traffic, normalised to the relative URLs of the frozen REST
 * contract so counts are comparable across the two frontends and across context paths.
 *
 * Requests to a third-party origin are ignored outright - the compiled stylesheet fetches
 * webfonts from a Google host, and those are not application behaviour.
 */
export interface RecordedResponse {
	readonly method: string;
	readonly path: string;
	readonly status: number;
}

export class RequestLog {
	private records: RecordedRequest[] = [];
	private responses: RecordedResponse[] = [];

	private constructor(private readonly root: URL) {}

	static attach(page: Page, baseUrl: string): RequestLog {
		const log = new RequestLog(new URL(baseUrl));
		page.on('request', (request: Request) => log.accept(request));
		page.on('response', (response: Response) => log.acceptResponse(response));
		return log;
	}

	/** `null` when the URL is not an application request at all. */
	private normalise(rawUrl: string): { path: string; query: string } | null {
		let url: URL;
		try {
			url = new URL(rawUrl);
		} catch {
			return null;
		}
		if (url.origin !== this.root.origin) {
			return null; // third-party origin (the stylesheet's webfont host)
		}
		const prefix = this.root.pathname.endsWith('/')
			? this.root.pathname
			: `${this.root.pathname}/`;
		if (!url.pathname.startsWith(prefix)) {
			return null;
		}
		const path = url.pathname.slice(prefix.length);
		if (path === '' || STATIC_ASSET.test(path)) {
			return null; // document, template fetch, bundle, stylesheet, font or image
		}
		const query = [...url.searchParams.entries()]
			.map(([k, v]) => `${k}=${v}`)
			.sort()
			.join('&');
		return { path, query };
	}

	private accept(request: Request): void {
		const parts = this.normalise(request.url());
		if (!parts) {
			return;
		}
		this.records.push({
			method: request.method(),
			path: parts.path,
			query: parts.query,
			api: isApiPath(parts.path)
		});
	}

	private acceptResponse(response: Response): void {
		const parts = this.normalise(response.url());
		if (!parts) {
			return;
		}
		this.responses.push({
			method: response.request().method(),
			path: parts.path,
			status: response.status()
		});
	}

	/** Drops everything recorded so far. Call immediately before the interaction under test. */
	reset(): void {
		this.records = [];
		this.responses = [];
	}

	/** Every application response since the last `reset()`, in order. */
	allResponses(): readonly RecordedResponse[] {
		return [...this.responses];
	}

	/**
	 * Application responses with a 5xx status. DR-02 exists because clearing a numeric field
	 * currently sends an empty body, which Spring unboxes into `int` and blows up - and the
	 * failure path has no error handler, so today the user sees nothing at all.
	 */
	serverErrors(): readonly RecordedResponse[] {
		return this.responses.filter((r) => r.status >= 500);
	}

	/** Every application request since the last `reset()`, in order. */
	all(): readonly RecordedRequest[] {
		return [...this.records];
	}

	/**
	 * Same-origin, non-static requests that are NOT one of the 21 frozen endpoints.
	 * Must always be empty - the REST contract is frozen (spec.md 6).
	 */
	offContract(): readonly RecordedRequest[] {
		return this.records.filter((r) => !r.api);
	}

	/** Exact number of `<method> <path>` calls since the last `reset()`. */
	count(method: string, path: ApiPath): number {
		return this.records.filter((r) => r.method === method && r.path === path).length;
	}

	/** `['GET quote', 'PUT driver/address/city']` - a stable, ordered shape for goldens. */
	summary(): string[] {
		return this.records.map((r) => `${r.method} ${r.path}${r.query ? `?${r.query}` : ''}`);
	}

	/**
	 * `{ 'GET driver': 2, 'GET quote': 1 }` - counts, key-sorted.
	 *
	 * Use this instead of `summary()` whenever the requests under observation are issued
	 * concurrently. The startup burst is the case in point: the two `GET driver` calls and the
	 * `GET vehicle` call race, so their relative order is not a behavioural property and
	 * recording it would make the golden flap between runs for no reason.
	 */
	tally(): Record<string, number> {
		const counts: Record<string, number> = {};
		for (const entry of this.summary()) {
			counts[entry] = (counts[entry] ?? 0) + 1;
		}
		return Object.fromEntries(Object.entries(counts).sort(([a], [b]) => a.localeCompare(b)));
	}
}

const STATIC_ASSET =
	/\.(html|js|css|map|ico|png|jpe?g|gif|svg|woff2?|ttf|otf|eot|json|txt)$/i;

function isApiPath(path: string): path is ApiPath {
	return (API_PATHS as readonly string[]).includes(path);
}
