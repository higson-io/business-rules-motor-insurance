## CSS

Project-specific standards take precedence over the generic guidance at the end of this file wherever the two overlap.

---

### LESS Is the Styling Language, With `frontend/src/styles.less` as the Sole Entry Point
All application styling is authored in LESS. `frontend/src/styles.less` is the single global entry point: it imports the partials under `frontend/src/styles/` and declares nothing itself. Component-scoped styles live in a `.less` file beside the component and are referenced by `styleUrl`. `angular.json` sets `inlineStyleLanguage: "less"` and the component schematic defaults to `style: "less"`, so generated code follows suit.

The Angular CLI compiles LESS as part of `ng build`; there is no separate stylesheet build step and **no generated stylesheet is committed**. The predecessor pipeline is gone: the `less/` source tree, `gulpfile.js` and its `less` task, and the generated-but-tracked `src/main/webapp/styles/styles.min.css` were all deleted at cutover (DR-21, DR-23). `src/main/webapp/` survives as an empty `warSourceDirectory` and holds no application source.

*Evidence:* `angular.json` `styles` array and `inlineStyleLanguage`; `frontend/src/styles.less` contains only `@import` statements; `less@^4.2.0` declared explicitly because it is an *optional* peer of `@angular/build`, which ships `sass`.

### One Scoped Sass Exception: `frontend/src/theme.scss`
`frontend/src/theme.scss` is the **only** `.scss` file in the workspace and the only place a brand value is declared. It exists because Angular Material's theming API (`mat.theme()`) is Sass-only and has no LESS equivalent. `@angular/build` already bundles `sass`, so it costs no new dependency.

Two rules keep the exception scoped:

1. **It is listed first** in `angular.json`'s `styles` array, ahead of `src/styles.less`, so where a LESS rule and a generated Material rule address the same property, the LESS rule wins the cascade.
2. **Do not add a second `.scss` file.** Sass is not a second styling language for the application to author in. `AC-S7` asserts there is exactly one.

Brand values are declared once there — the `#4C4CDE` primary, the 8px corner radius, Manrope as the Material typography family — and everything downstream references the resulting tokens (`var(--mat-sys-primary)`, `var(--mat-sys-corner-medium)`) rather than re-declaring the hex.

*Evidence:* AC-S7 — `find frontend/src -name '*.scss'` returns exactly `frontend/src/theme.scss`.

### Stylesheet Assets Live Under `frontend/src/styles/`
Fonts and images referenced by a `url()` must sit beside the partial that references them, not in `frontend/public/`. This is a build constraint, not a preference — see the rule of the same name in `build.md` for the mechanism and the evidence.

### Appearance Is Not a Parity Gate
The migration replaced every control on the screen with Angular Material (decision C-9). Screenshots are kept as documentation of the redesign, never as pass/fail evidence: with the whole UI redesigned, a before/after image diff cannot distinguish an intended redesign from a regression. **A visual difference is never, on its own, a finding.** Regression protection rests on the MockMvc contract tests and the Playwright behavioural flows.

The brand seed in `theme.scss` exists so the application still looks like itself — recognisability, not parity.

---

## General CSS Guidance

### Consistent Methodology
Stick to the project's chosen approach across the entire codebase.

### Work With the Framework
Use Material's tokens and component APIs as intended rather than fighting them with deep overrides of generated class names.

### Design Tokens
Establish and document consistent values for colors, spacing, and typography. Here they are the `--mat-sys-*` tokens seeded by `theme.scss`.

### Minimize Custom CSS
Prefer framework utilities and tokens to reduce custom styling maintenance.

### Production Optimization
Use CSS purging or tree-shaking to remove unused styles. Delete dead rules rather than porting them — the migration removed 13 blocks that no template had ever matched.
