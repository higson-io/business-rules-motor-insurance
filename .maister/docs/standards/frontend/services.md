## Angular Services & HTTP

All server access in this SPA is centralised in injectable services. Components render and coordinate; they never issue HTTP calls themselves.

The frontend is **Angular 22** in `frontend/`. It replaced the AngularJS 1.8.3 SPA that lived in `src/main/webapp/`.

---

### HTTP Only in *Service Classes
All server access lives in `@Injectable({ providedIn: 'root' })` classes named `<Domain>Service` under `frontend/src/app/services/` (flat, no templates). Each injects `HttpClient` through `inject()` into a `private readonly` field and exposes one thin method per backend endpoint, returning the `Observable` unchanged. Components must never inject `HttpClient`.

```typescript
@Injectable({ providedIn: 'root' })
export class DictionaryService {
	private readonly http = inject(HttpClient);

	getMakeDictionary(productionYear: number): Observable<DictionaryEntryDto[]> {
		return this.http.get<DictionaryEntryDto[]>('dictionaries/make', {
			params: { productionYear },
		});
	}
}
```

*Evidence:* 4 of 4 services follow this shape (`dictionary-service.ts`, `driver-service.ts`, `vehicle-service.ts`, `variant-service.ts`); `HttpClient` appears in 0 of 10 component classes (AC-C5, verified by `frontend/scripts/g11-parity-greps.sh`).

### Never Unwrap a Response Body — HttpClient Already Did
`HttpClient` resolves to the **response body itself**, typed by the generic parameter. There is no `response.data` to reach through, and no wrapper to unpack.

This rule exists because the AngularJS `$http` predecessor did the opposite: it resolved to a response *envelope* and every caller wrote `response.data`. A leftover `.data` under `HttpClient` yields `undefined` rather than a compile error or a runtime throw, so the failure is silent and shows up as a blank field, not as a stack trace.

```typescript
// right — the body is the value
this.http.get<DriverDto>('driver')            // -> Observable<DriverDto>

// wrong — `.data` does not exist on DriverDto; the value is `undefined` at runtime
this.http.get<DriverDto>('driver').pipe(map((r) => r.data))
```

*Evidence:* AC-C6 — `grep -rn '\.data' frontend/src` returns empty across all 11 call sites migrated from `$http`.

### Relative URLs Mirroring the Backend Mapping Roots
Request URLs are relative with **no leading slash** and mirror the backend `@RequestMapping` roots (`driver`, `vehicle/make`, `dictionaries/model`, `quote`). A leading slash would pin every request to the server root and break the application under its servlet context path. Query parameters go in the `params` option object (a plain object or an `HttpParams` instance) — never concatenated into the URL string.

This rule and the no-`<base>`-element rule in `build.md` are two halves of one property: **nothing in the bundle encodes where the application is mounted.** The same WAR serves correctly at `/demo`, at `/app` and at the server root, which G11 verified by restarting one build under each context path.

*Evidence:* AC-C4 — `grep -rn "http\.\(get\|put\)<[^>]*>('/"` returns empty; all 21 endpoint calls use relative URLs; G11 §6.3 context-path matrix.

### The Body Type Is the Content Type — Do Not Stringify
`HttpClient` branches on the JavaScript type of the request body: a `string` goes on the wire verbatim as `text/plain`, anything else is serialised as JSON. Both branches are load-bearing against this backend and they pull in opposite directions on adjacent endpoints.

- The six string setters on `DriverApi` (`firstName`, `gender`, `address/city`, …) take `@RequestBody String`. Pass the raw string. `JSON.stringify`-ing it — or hand-setting `Content-Type` — stores the value **with its quotes**: `Krakow` comes back as `"Krakow"`.
- `PUT driver/birthDate` takes `@RequestBody Date`. Pass a `Date` **object**, which takes the JSON branch. Passing the same value as an ISO string takes the string branch, arrives as `text/plain`, and Jackson rejects it with **HTTP 415**.

*Evidence:* AC-C2 — `grep -rn 'JSON.stringify' frontend/src` returns empty; both behaviours pinned live by the G1 MockMvc contract tests.
