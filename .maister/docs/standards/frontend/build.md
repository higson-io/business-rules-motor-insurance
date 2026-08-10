## Frontend Build & Assets

The frontend build is driven by Maven, not run independently in normal use. A plain `mvn package` is the single command that produces a deployable artifact with current assets.

The Angular workspace lives in **`frontend/`** at the repository root.

---

### Ship No HTML Base Element — The Context Path Is a Runtime Value
`frontend/src/index.html` contains **no `<base>` element**, and `angular.json` sets **no `baseHref`**. This is deliberate and it is load-bearing: the servlet context path is decided at run time, not at build time. The packaged application defaults to `/demo` (`higson-demo-app.properties`), the published container image serves it at `/app`, and a developer may run it at the server root.

Angular CLI scaffolds `<base href="/">` by default. Left in place it pins every emitted asset URL to the server root, so **every asset 404s** under any non-root context path — while the index page itself still returns 200, which makes the failure look like a bundling problem rather than a base-href problem.

With no `<base>`, every emitted URL stays relative and resolves against the document URL, and one WAR serves correctly everywhere. Verified by rebooting a single build under each context path and varying nothing else:

| `server.servlet.context-path` | index | `<base>` in served page | `main-*.js` | `styles-*.css` | `GET quote` |
|---|---|---|---|---|---|
| `/demo` | 200 | **0** | 200 | 200 | 200 |
| *(empty — root)* | 200 | **0** | 200 | 200 | 200 |
| `/app` | 200 | **0** | 200 | 200 | 200 |

The companion rule is in `services.md`: request URLs are relative with no leading slash. Together they mean **nothing in the bundle encodes where the application is mounted**.

*Evidence:* AC-S6 / DR-20 — `grep '<base' frontend/src/index.html` empty; G11 parity verdict §6.3, including a real browser render at `/app` with 0 console errors.

### Stylesheet Assets Live Beside the Partial That References Them
Fonts and images referenced from a stylesheet belong under `frontend/src/styles/`, **not** in `frontend/public/`. Angular's LESS pipeline rewrites each `url()` relative to the file that wrote it and resolves it at build time — an unresolvable path is a hard build error, not a warning. Assets under `src/styles/` are content-hashed into `media/` with references rewritten to `./media/…`, which is relative to the emitted stylesheet and therefore correct at every context path.

`frontend/public/` is copied verbatim and is **not** resolvable from a stylesheet. It holds `favicon.ico` only, because `index.html` — not the CSS — refers to it.

*Evidence:* G9 ran a 7-row differential over asset locations; the only `public/` variant that built at all emitted a leading-slash URL, which breaks every non-root context path.

### Frontend Build Is Wired Into Maven prepare-package
The Angular build is bound to the Maven `prepare-package` phase via `exec-maven-plugin`, so a plain `mvn package` always produces a WAR with freshly built assets. The plugin sets `<workingDirectory>${project.basedir}/frontend</workingDirectory>` — without it the plugin runs at the project root and picks up the wrong `package.json`. The npm entry point is `npm ci && ng build` (lockfile-deterministic) — **never `npm install`**.

```bash
mvn package   # compiles Java, runs npm ci && ng build in frontend/, packages target/motor-insurance.war
```

Unit tests are **not** part of `mvn package`. `npm run build` deliberately does not invoke Vitest or Playwright.

*Evidence:* `pom.xml` `exec-maven-plugin` bound to `prepare-package` with `<workingDirectory>`; `frontend/package.json` `"build": "npm ci && ng build"`; `frontend/package-lock.json` committed.

### Do Not Pin maven-war-plugin
The Angular output is pulled into the WAR root by a `maven-war-plugin` `<webResources>` block pointing at `frontend/dist/motor-insurance/browser`. The WAR root is exactly where the previous frontend lived, so `ApplicationConfig`'s `forward:/index.html` needs no change.

The `<version>` element is **deliberately omitted**. `spring-boot-starter-parent` manages this plugin at 3.4.0 and its `pluginManagement` entry also supplies an `<archive>` block that an explicit `<version>` here would clobber. Adding a version to "be explicit" breaks the packaging.

*Evidence:* `pom.xml` `maven-war-plugin` block and its comment; `standards/backend/java.md` pins only what the Boot BOM does not manage.

### Generated Assets Are Never Committed
`frontend/node_modules/`, `frontend/dist/` and the Angular CLI cache `frontend/.angular/` are git-ignored and removed outright by `mvn clean` via `maven-clean-plugin` filesets. The filesets carry **no `<includes>`**: with one, the plugin empties the directory and leaves the directory behind, so `mvn clean` would not return the tree to a clean-clone state.

Nothing is emitted into `src/main/webapp/`. That directory is version-controlled and is not touched by `mvn clean`, so hashed bundles written there would accumulate forever.

*Evidence:* `.gitignore` entries for `node_modules/`, `frontend/dist/`, `frontend/.angular/`; `pom.xml` `maven-clean-plugin` filesets.

### Exact Pins for Framework-Critical npm Packages
Every `@angular/*` package, `rxjs` and `typescript` are pinned to exact versions. Caret ranges are used only for peripheral tooling (`less`, `prettier`, `jsdom`, `vitest`).

```json
"@angular/core": "22.1.0"     not     "@angular/core": "^22.1.0"
"typescript": "~6.0.3"        not     "typescript": "latest"
```

Two traps this pinning exists to prevent:

- **`typescript@latest` is 7.0.2 and breaks the build.** Angular 22's compiler peer range is `>=6.0 <6.1`, so the pin is `~6.0.3`.
- **`less` must be declared explicitly.** It is an *optional* peer of `@angular/build`, which ships `sass` — not `less`. Without an explicit `less@^4.2.0` devDependency the build fails on the first `.less` import.

*Evidence:* `frontend/package.json` — 8 exact `@angular/*` pins plus `rxjs` and `typescript`; carets only on `jsdom`, `less`, `prettier`, `vitest`.

### The Node Range Is Enforced by the Workspace, Not by the Repository
`frontend/package.json` declares `"engines": { "node": "^22.22.3 || ^24.15.0 || >=26.0.0" }` and `frontend/.npmrc` sets `engine-strict=true`, so `npm ci` fails early with a readable message on an unsupported Node.

⚠️ **Nothing installs or selects a Node version.** There is no `.nvmrc` and no `frontend-maven-plugin` (decision C-5). A developer whose shell has the wrong Node on PATH gets the `engines` rejection buried inside the Maven `prepare-package` output. See `standards/global/conventions.md` for the toolchain baseline and the local PATH caveat.
