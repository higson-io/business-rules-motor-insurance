# Motor Insurance frontend

Angular workspace for the Higson Motor Insurance demo, generated with Angular CLI 22.1.3.
It replaces the AngularJS application that lived in `src/main/webapp/`.

## Node

Angular 22 requires Node `^22.22.3 || ^24.15.0 || >=26.0.0`. The range is enforced by `engines` in
`package.json` together with `engine-strict=true` in `.npmrc`, so an unsupported Node fails early
with a readable message instead of failing opaquely inside the Maven `prepare-package` phase.

Note that 23.x is *newer* than 22.22.3 and is still rejected — Angular supports even-numbered (LTS)
majors only. Nothing here installs or selects a version: there is no `.nvmrc` and no
`frontend-maven-plugin`, so the right Node has to be on PATH before you build. If it is installed
side-by-side (Homebrew's `node@22` is keg-only, for example), prepend it for the session:

```bash
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"
```

## Build

```bash
npm run build      # npm ci && ng build  -- what `mvn package` runs
```

Output goes to `dist/motor-insurance/browser/`, which `maven-war-plugin` copies to the WAR root.
A plain `mvn package` at the repository root always rebuilds these assets; there is no need to run
the frontend build by hand. `mvn clean` wipes `node_modules/`, `dist/` and `.angular/`.

The bundle ships **no HTML base element** and `angular.json` sets **no `baseHref`**: the servlet
context path is a runtime value (`/demo` by default, `/app` in the published container image), so
every emitted URL stays relative with no leading slash and resolves against the document URL.

## Development server

```bash
# terminal 1 - backend at the ROOT context, so JSESSIONID gets Path=/ and needs no rewriting
SERVER_SERVLET_CONTEXT_PATH=/ mvn spring-boot:run

# terminal 2
npm start          # ng serve --proxy-config proxy.conf.json, http://localhost:4200/
```

`proxy.conf.json` forwards `driver`, `vehicle`, `dictionaries` and `quote` to
`http://localhost:48080`. If you instead run the backend under `/demo`, you must add
`cookiePathRewrite` — otherwise Tomcat sets `JSESSIONID; Path=/demo`, the browser at
`localhost:4200` never sends it back, and the quote resets to defaults on every request.

## Unit tests

```bash
npm test                          # ng test, Vitest
npx ng test --watch=false         # single run
npx ng test --watch=false --filter="^QuoteStore"   # one suite
```

Tests are **not** part of `mvn package`. End-to-end tests live in `e2e/` at the repository root and
are deliberately kept outside this workspace so the same specs run against both frontends.

Two invocations that look reasonable and do not work here:

- ⛔ `npx vitest run` → `ReferenceError: describe is not defined`. The Angular CLI supplies the test
  environment and the compiler plugin; there is no `vitest.config.ts` to fall back on.
- ⛔ `ng test --include=<dir>` drops the Angular compiler plugin and fails with *"No loader is
  configured for .less files"*, which looks like a styling problem and is not. Use `--filter`.

## Styling

LESS everywhere, with one deliberate exception: `src/theme.scss`. Angular Material's theming API is
Sass-only and has no LESS equivalent. `angular.json` lists the theme first so the LESS entry point
wins the cascade.
