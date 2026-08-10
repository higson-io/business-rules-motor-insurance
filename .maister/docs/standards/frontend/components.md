## Components

This frontend is **Angular 22**, in the `frontend/` workspace. It replaced the end-of-life AngularJS 1.8.3 SPA that lived in `src/main/webapp/`.

Project-specific standards take precedence over the generic guidance at the end of this file wherever the two overlap. Where a project rule and general Angular best practice disagree, the project rule wins — see the open item at the end of this file.

---

### Standalone Components — Never Write `standalone: true`
Every component and directive is standalone and declares its own `imports` array. There are no `NgModule`s.

`standalone: true` is the **default** in Angular 22 and writing it is redundant. Do not add it.

```typescript
@Component({
	selector: 'app-coverage',
	imports: [DecimalPipe, HtmlPopover],
	templateUrl: './coverage.html',
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Coverage { … }
```

*Evidence:* AC-S1 — `grep -rn 'standalone: true' frontend/src` returns empty; 0 `NgModule` declarations in the workspace.

### Signal Inputs, Not Decorators
Inputs are declared with the `input()` / `input.required()` functions on `readonly` fields, never with `@Input()`. They are signals: read them by calling them. Outputs, when one is needed, use the `output()` function rather than `@Output()` / `EventEmitter`.

```typescript
readonly code = input.required<string>();
readonly description = input.required<string | null>();   // nullable on the wire — say so
```

No component in this workspace currently declares an output: sibling components communicate through `QuoteStore`, not through event chains (see below). Reach for `output()` only for a genuinely local parent-child notification.

*Evidence:* AC-S2 — `grep -rn '@Input\|@Output' frontend/src` returns empty; 10 signal inputs across `coverage`, `discount` and `html-popover`.

### `inject()` Over Constructor Parameters
Dependencies are obtained with `inject()` assigned to `private readonly` (or `readonly`, when a template reads it) fields. Constructor injection is not used.

```typescript
export class QuoteStore {
	private readonly variantService = inject(VariantService);
	private readonly driverService = inject(DriverService);
}
```

A constructor body is still legitimate for wiring that must run at construction time — `vehicle.ts` has the one in this workspace — but it takes no parameters.

*Evidence:* 1 constructor across all production classes, with an empty parameter list.

### Built-In Control Flow, With a Mandatory `track`
Templates use `@if` / `@for` / `@switch`. The structural directives `*ngIf` and `*ngFor` and the attribute directives `ngClass` / `ngStyle` are not used; bind `[class.x]` and `[style.y]` directly instead.

Every `@for` **must** name a `track` expression, and it must be a stable identity — a business key, not `$index`. Every list this application renders has one (`variant.code`, `coverage.code`, `discount.code`).

```html
@for (coverage of variant.coverages; track coverage.code) { … }
```

⛔ **Add no client-side sort to these loops.** The backend already returns options by ascending `order` and coverages and discounts by ascending `position`, pinned by the MockMvc contract tests. The AngularJS templates carried an `orderBy` filter with an unquoted predicate, which resolved to `undefined` and sorted nothing — a no-op that only looked like a sort. Re-adding a real one "to be safe" changes rendered output that the goldens pin (DR-05).

*Evidence:* AC-S2 — `grep -rn '\*ngIf\|\*ngFor\|ngClass\|ngStyle' frontend/src` returns empty; DR-05 — `grep -rn '\.sort(\|orderBy' frontend/src` returns empty.

### OnPush Everywhere, and the `host` Object for Host Bindings
Every component sets `changeDetection: ChangeDetectionStrategy.OnPush`. Combined with signal-based state this makes rendering predictable and cheap.

Host bindings and host listeners go in the `host` object of the decorator metadata, not in `@HostBinding` / `@HostListener` decorators.

```typescript
host: { '[attr.aria-busy]': 'pendingRequests.count() > 0' },
```

*Evidence:* 11 of 11 components and directives declare `OnPush`; 3 use the `host` object; 0 use `@HostBinding` or `@HostListener`.

### Component Files Are Co-Located and Same-Named, With No `.component` Suffix
Each component lives in its own directory under `frontend/src/app/components/` holding a `.ts`, an `.html` and — when it needs styles — a `.less`, all sharing the directory name. The class name is the PascalCase form with no suffix (`Driver`, not `DriverComponent`); the selector is the `app-` prefixed kebab-case form. Nested components sit in a subdirectory of their parent (`variants/coverage`, `variants/discount`). Shared, reusable pieces live under `shared/` rather than `components/`.

```
components/driver-history/driver-history.ts     -> export class DriverHistory, selector 'app-driver-history'
components/driver-history/driver-history.html   -> templateUrl: './driver-history.html'
components/driver-history/driver-history.less   -> styleUrl:    './driver-history.less'
```

The root component's selector is **`app-root`**. It must never be a real HTML tag name — the AngularJS build registered its root as the element `main`, which is both a valid HTML5 tag and an invalid custom-element name (DR-20).

*Evidence:* 10 of 10 component directories match; 2 of 2 child components nested under `variants/`; `index.html` contains `<app-root>`.

### ⛔ Recalculation Runs Through `QuoteStore`, and the Vehicle Asymmetry Is Encoded as Data
Input components do not talk to the quote panel, and they do not talk to each other. After a successful update they call `QuoteStore.recalculate()`, which bumps a trigger signal that the quote `resource()` depends on. This replaced the AngularJS `$scope.$emit('recalculate')` → `main` relay → `$scope.$broadcast('reloadQuote')` event bus outright; the relay was **deleted, not ported**.

**The vehicle cascade is the exception, and preserving it is not optional.** Only a vehicle **model** change reloads the quote. Production year, make and type deliberately do not — each reload is a full rules evaluation, and the two intermediate cascade steps describe an incomplete vehicle anyway. This is carried-over intended behaviour (DR-13 / ND-1), and an idiomatic reactive rewrite reconnects the other three *by accident*.

It is therefore encoded as an exhaustive table in the store rather than as component discipline:

```typescript
const RELOADS_QUOTE: Record<VehicleField, boolean> = {
	productionYear: false, make: false, type: false, model: true,
};

vehicleFieldChanged(field: VehicleField): void {
	if (RELOADS_QUOTE[field]) { this.recalculate(); }
}
```

The record is exhaustive by construction: adding a member to the `VehicleField` union is a compile error until someone decides, explicitly and in that one place, whether it reloads. The vehicle component routes all four fields through `vehicleFieldChanged()` and **never calls `recalculate()` itself**, so a component that handles all four in one handler cannot reintroduce the loss.

⚠️ A "tidy" rewrite that recalculates on every vehicle change is a **failed migration, not an improvement**.

*Evidence:* `frontend/src/app/core/quote-store.ts`; `vehicle.ts` — 4 `vehicleFieldChanged` call sites, 0 direct `recalculate()` calls; the four NO-BRANCH golden rows are byte-identical pre and post (parity verdict §1).

### Commit Text and Numeric Fields on Blur, and Only When the Value Changed
The three numeric claim-history fields and the seven driver text fields commit on `(blur)`, not on `(input)` — so the quote no longer recalculates on every keystroke (DR-03).

Every commit handler first compares the new value against the current one and returns early when they are equal. AngularJS's `ng-change` fired only when the model actually changed, so blurring an untouched field cost nothing; a bare `(blur)` handler PUTs on **every** blur, which is a request-volume increase traceable to no defect-register row.

*Evidence:* DR-03 golden row — typing `12` went from 2 PUT + 2 `GET quote` to 1 + 1; the unchanged-value guard is on all ten fields.

---

## Open item, for the team to settle

The Angular MCP best-practices served for this workspace say *"do **not** set `changeDetection: OnPush` explicitly — it is the default in v22+"*, which contradicts the OnPush rule above. The project rule was followed during the migration (documentation priority beats general best practice), and every component sets it explicitly.

**Suggested restatement**: *"No component opts **out** of OnPush."* That keeps the guarantee that matters — no component silently falls back to default change detection — without requiring redundant metadata once the framework default is trusted. Settle it before the next component is written, so the workspace stays uniform either way.

---

## General Component Guidance

### Single Responsibility
Each component should do one thing well.

### Reusability
Design components to work across different contexts with configurable inputs.

### Composability
Build complex UIs by combining smaller components rather than creating monoliths.

### Clear Interface
Define explicit, documented inputs with sensible defaults.

### Encapsulation
Keep implementation details private; expose only what's necessary.

### Consistent Naming
Use descriptive names that indicate purpose and follow team conventions.

### Local State
Keep state as close to where it's used as possible; lift only when needed.

### Minimal Inputs
If a component needs many inputs, consider composition or splitting it.

### Documentation
Document usage, inputs, and examples to help team adoption.
