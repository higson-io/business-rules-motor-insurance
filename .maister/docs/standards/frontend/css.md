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

### ⛔ The Action Colour Is the Portal's Teal, Not Red

The demos take their palette from `higson-agent-portal-demo-frontend/src/styles/colors.scss`, so the
two applications read as one family. **The single value that decides whether a screen looks like the
portal is the action colour, and it is teal.** `colors.scss` sets
`--p-button-primary-background: var(--p-primary-150)` = `#3F8797`, and every primary button, selected
state, focus ring and panel title in the portal uses it.

`--accent-500` (`#FF0000`) is an **accent** there: it appears in the brand gradient and nowhere else —
no button, no border, no selected state. This application read that backwards for a while and painted
its buttons, focus rings, prices and required markers red, which is exactly what made the demos stop
looking like the portal. Structural red is now gone from every screen; `@color-accent` stays declared
in `_variables.less` because it is a real portal token, and is used nowhere.

Alongside it: panel titles take `@color-panel-title` (the same teal), a discount or other favourable
outcome takes `@color-success`, and anything that says *something is missing* takes `@color-warn` —
never the action colour, which promises an interaction.

### ⚠️ Contrast Is Measured on Every Background, Not Just White

Two of the portal's published values do not clear the 4.5:1 floor `accessibility.md` sets:
`--primary-150` (`#3F8797`) measures **4.10:1** against white, and `--slate-grey` (`#6D8196`)
measures **4.01:1**. Both are darkened by the smallest step that passes — `#317989` and `#5b6f84` —
holding the hue, and both `_variables.less` and `theme.scss` record the measured figures.

The trap worth remembering: **the same text sits on two grounds.** Muted copy appears inside a white
panel and directly on `@color-ground` (`#f5f5f5`), and the grey ground costs roughly 0.4 of a point.
A first attempt at the muted tone passed on white (4.55) and failed on the ground (4.18) — invisible
unless both are measured. Check any new foreground against `#ffffff`, `#f5f5f5` **and** whatever
tinted fill it may land on.

⚠️ When measuring in the browser, resolve backgrounds honestly: the motor comparison table stripes
rows with `rgba(91, 111, 132, 0.04)`, and a probe that treats a translucent colour as opaque reports
a dark background and a fabricated failure.

### ⛔ Recorded Departure (2026-09-09): the Action Colour Now Fails That Floor, Deliberately

Since 2026-09-09 the palette follows the UX mockups for the property demo rather than the portal,
and the action colour is **`#2AA399`** — measured **3.09:1** against white and **2.83:1** against the
page ground, both under the 4.5:1 floor. In the mockups that colour carries the active step-bar tab,
the justification numbering and white button labels, so it is copy the floor applies to.

Two passing options were offered and declined: `#207A72` (the same hue darkened — 5.14:1 / 4.71:1)
and the previous `#317989` (4.97:1 / 4.55:1). The demo's owner chose the mockup as drawn. See D-1 in
`.maister/tasks/development/2026-09-09-ux-redesign-property/brief.md`.

What this means for anyone reading the code:

- **It is not a bug and must not be "fixed" back to a passing value** without that decision being
  revisited. `_variables.less` and `theme.scss` both carry the measured figures beside the hex.
- **The departure covers the action colour only.** Every other foreground still has to clear 4.5:1 on
  every background it lands on — the muted tone, the amber departure block in the offer, and anything
  new. Measure them; the exception does not spread.
- **The change is global, not per-demo.** `@color-action` and `--mat-sys-primary` are one token for
  all three demos, so motor and life took the new teal too. Splitting it three ways would have put
  three palettes in one application, which is worse than one recorded departure.
- The page ground moved with it, from `#f5f5f5` to `#f3f5f9`. Re-measure against the new value.

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
