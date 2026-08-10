## TypeScript

The Angular 22 workspace in `frontend/` is written in TypeScript. The predecessor SPA was untyped ES5, so there is no legacy to accommodate: the type checker is turned all the way up and stays there.

---

### `strict: true` Must Be Set Explicitly — The CLI Does Not Emit It
`frontend/tsconfig.json` sets `"strict": true` in `compilerOptions` and `"strictTemplates": true` in `angularCompilerOptions`.

⚠️ **Angular CLI 22 does not write `"strict": true` into a generated `tsconfig.json`**, despite `--strict` defaulting to `true`. It emits only the individual `noImplicit*` flags, which leaves `strictNullChecks` — the one that matters most here — off. Both lines were added by hand and must not be removed. If you scaffold a new workspace or regenerate the config, check for them.

Alongside them the workspace keeps the CLI's stricter extras: `noImplicitOverride`, `noPropertyAccessFromIndexSignature`, `noImplicitReturns`, `noFallthroughCasesInSwitch`, plus `strictInjectionParameters` and `strictInputAccessModifiers`.

*Evidence:* `frontend/tsconfig.json`; recorded during scaffolding in G3.

### No `any`
`any` does not appear in this workspace. When a type is genuinely unknown, use `unknown` and narrow it; when it is a value from the wire, give it a DTO interface under `frontend/src/app/models/`.

*Evidence:* AC-S4 — `grep -rn ': any' frontend/src` returns empty, verified by `frontend/scripts/g11-parity-greps.sh`.

### Model the Wire Honestly, Including Nullability
DTO interfaces mirror the backend contract exactly, including which fields are nullable and which are not. Getting this wrong is not cosmetic under `strictNullChecks`: it either hides a real `null` or forces defensive code that can never trigger.

Two cases this project actually hit:

- `CoverageDto.limitRight` and `CoverageDto.description` are nullable `BigDecimal` / `String` on the wire, and the goldens record six `limitRight: null`. Typing them non-nullable was a real defect, caught and corrected.
- `VehicleDto`'s cascade fields are primitive `int` on the backend and are never null — a cleared value reads back as `0`. Typing them `number | null` would break the falsy-`0` guards the cascade depends on. They are `number`.

*Evidence:* `frontend/src/app/models/`; `CoverageDto.java`; G1 contract goldens.

### Prefer Inference Over Annotation
Annotate what forms a contract — public method return types, exported model interfaces, `input()` / `resource()` type parameters — and let the compiler infer the rest. Do not restate a type the initialiser already establishes.

`DictionaryEntryDto.code` is worth calling out as the counter-case: it arrives as a **JSON string** even though it reads as an id. Convert explicitly with `Number()` at the binding site rather than letting a string flow into a numeric comparison.

### Strict Types Are Load-Bearing, Not Ceremony
Strictness caught a real defect during the migration that no test did: with the DR-01 blank-option guard removed, the unguarded `[0].name` access **failed to typecheck**. Under a loose config that mutation would have compiled and shipped a runtime `TypeError`.

Treat a type error as a finding, not an obstacle. Do not silence one with a cast, a non-null assertion, or `any`.
