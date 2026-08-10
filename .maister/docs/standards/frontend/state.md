## State Management

The SPA holds almost no state of its own. The backend keeps the whole quote in a `@SessionScope` bean, so the frontend stays a thin remote-controlled view over it: it fetches, it renders, and it tells the server when something changed.

What little shared state exists lives in one root-provided store, `frontend/src/app/core/quote-store.ts`.

---

### Signals, Not `BehaviorSubject`
Shared state is held in signals. `BehaviorSubject` and the manual subscribe/unsubscribe lifecycle that comes with it are not used for state.

RxJS remains in the codebase for what it is good at — `HttpClient` returns `Observable`, and `firstValueFrom` bridges a single-shot request into a `resource()` loader. That is the extent of it.

*Evidence:* AC-S3 — `grep -rn 'BehaviorSubject' frontend/src` returns empty.

### `computed()` for Derived State
Anything that can be calculated from other signals is a `computed()`. Do not mirror derived values into a second writable signal and keep them in sync by hand, and do not write state from an `effect()`.

`linkedSignal()` is the right tool for the narrow case of local editable state that resets when its source changes — the driver-history fields use it to hold in-progress edits that follow the server value when the driver reloads.

### ⛔ Read a `resource()` Through `hasValue()` — Never a Bare `value()`
In Angular 22, a `resource()` in the **error** state does not return `undefined` from `value()`. It **throws `ResourceValueError`**.

```typescript
// ⛔ WRONG — throws when the load failed
readonly options = computed(() => this.store.quote.value()?.options ?? []);

// ✅ RIGHT
readonly options = computed(() =>
	this.store.quote.hasValue() ? this.store.quote.value().options : [],
);
```

**This is the single most expensive misconception of the migration.** It bit the project three times: the specification asserted the `undefined` behaviour, an implementation group repeated it, and a third group shipped three lines of `value()?.…` that would have crashed the driver panel the moment `GET driver` failed.

It is also the reason the rule is stated as an absolute rather than a preference. The `value()?.… ?? []` form **passed its test**. The test asserted that zero variant cards render after a failed quote load, and a template that throws renders zero cards — so a component that crashed the page and a component that degraded gracefully were indistinguishable to the assertion. A test that observes only the absence of output cannot tell "handled" from "threw".

When you write a test around a failed resource, assert on a recording `ErrorHandler` as well as on the rendered output. Otherwise the test passes for the wrong reason.

*Evidence:* proven by a full 2×4 differential over `defaultValue` × reader API and confirmed against the framework source (G5); the resulting fix is in `variants.ts`, `driver.ts` and `driver-history.ts`, each with a comment saying why.

### One Store, Injected — Not an Event Bus
`QuoteStore` is `@Injectable({ providedIn: 'root' })` and holds exactly three things:

| Member | Kind | Purpose |
|---|---|---|
| `quote` | `resource()` | the current quote, reloaded by bumping a private trigger signal |
| `driver` | `resource()` | loaded **once** and shared by every consumer |
| `mutationError` | `signal<string \| null>` | written by the mutation-error interceptor, rendered by the error banner |

Components inject the store and call `recalculate()` or `vehicleFieldChanged(field)`. They do not emit events at each other, and there is no client-side quote model — adding one would create a second source of truth alongside the server's session bean.

The shared `driver` resource is not incidental: the AngularJS build fetched the driver twice on startup, once per component, and collapsing that to one fetch is a recorded improvement (DR-09) that the goldens pin.

⛔ The vehicle-cascade reload asymmetry (DR-13 / ND-1) lives in this store as the exhaustive `RELOADS_QUOTE` table. See `components.md` — losing it is a failed migration.

### Cross-Cutting Request State Belongs in an Interceptor
Two behaviours are global and are implemented as functional `HttpInterceptorFn`s under `core/`, not as component logic:

- **Pending-request counter** — increments on send and decrements in `finalize()`, so it returns to zero on success *and* on error. One counter drives the single global spinner and the shell's `aria-busy`.
- **Mutation errors** — a failed `PUT` writes a message into `QuoteStore.mutationError` and does **not** retry. It prefers the backend's own message from the response body and falls back to `HttpErrorResponse.message` only when the body carries none, so the banner never renders as an empty box.

*Evidence:* `core/pending-requests.interceptor.ts`, `core/mutation-error.interceptor.ts`; DR-06 golden row — 1 request, no retry, banner visible with text.
