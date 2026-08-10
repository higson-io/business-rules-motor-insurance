## Development Conventions

### Predictable Structure
Organize files and directories in a logical, navigable layout.

### Up-to-Date Documentation
Keep README files current with setup steps, architecture overview, and contribution guidelines.

### Clean Version Control
Write clear commit messages, use feature branches, and add meaningful descriptions to pull requests.

### Environment Variables
Store configuration in environment variables; never commit secrets or API keys.

### Minimal Dependencies
Keep dependencies lean and up-to-date; document why major ones are included.

### Consistent Reviews
Follow a defined code review process with clear expectations for reviewers and authors.

### Testing Standards
Define required test coverage (unit, integration, etc.) before merging.

### Feature Flags
Use flags for incomplete features instead of long-lived branches.

### Changelog Updates
Maintain a changelog or release notes for significant changes.

### Build What's Needed
Avoid speculative code and "just in case" additions (see minimal-implementation.md).

## Project-Specific Conventions

*The standards below are specific to this project and take precedence over the generic guidance above where they overlap.*

### Required Local Toolchain Baseline
Development requires at least Java 17, a recent Maven, Higson Studio 4.0.2 or later, and **Node in the range Angular 22 accepts**:

```
^22.22.3 || ^24.15.0 || >=26.0.0
```

Both `mvn` and `node`/`npm` must be on the system PATH, because the Maven build invokes `npm run build` in `frontend/` during `prepare-package`.

⚠️ **Nothing in this repository installs or selects a Node version.** Per decision C-5 there is no `.nvmrc` and no `frontend-maven-plugin`; the range is declared in `frontend/package.json` `engines` with `engine-strict=true` in `frontend/.npmrc`, which makes `npm ci` refuse to run — but only once Maven has already reached `prepare-package`. **A bare `mvn package` in a shell with the wrong Node fails inside `prepare-package` with an `exec-maven-plugin` non-zero exit and no mention of Node anywhere near the top of the output.** Check `node -v` first when a package build fails for no apparent reason.

Note the version numbers are narrow on purpose: Node 23.x is *newer* than 22.22.3 and is still rejected. Angular pins to even-numbered (LTS) majors.

⚠️ **Local caveat on the current development machine.** Node 22.23.2 is installed via Homebrew but is **keg-only** at `/opt/homebrew/opt/node@22/bin` and is not on the default PATH, where `node` is still an unsupported 23.x. Prefix build and test commands with:

```bash
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"
```

This is a property of one machine's shell, not of the repository, which is exactly why it is written down rather than committed.

*Evidence:* `frontend/package.json` `engines`; `frontend/.npmrc` `engine-strict=true`; Readme.md prerequisites section; higson-demo-app.properties comment "point this to Higson Studio Database (version 4.0.2 or later)".

### Build the Executable WAR With the all_in_one Profile
The fully executable WAR is produced only with the `all_in_one` Maven profile, and the app must be packaged with that profile before a Docker image is built.

```bash
mvn clean package -Pall_in_one
docker build -t decerto/higson-motor-insurance-demo .
```

*Evidence:* Readme.md: "Before building the docker image, you need to build the app using the command: mvn clean package -Pall_in_one".

### Build Output, IDE Files and Tool Logs Stay Out of Version Control
`.gitignore` covers build output (`target`, `frontend/dist/`), all IDE metadata (`.idea`, `.classpath`, `.project`, `.settings/`, `*.iml`), OS cruft, generated dependency trees (`node_modules/`), the Angular CLI cache (`frontend/.angular/`) and tool logs. No IDE configuration is tracked. When a tool's log filename changes, update the ignore pattern with it — the `higson-cli.log` entry was recently widened to `higson-cli*.log` because the plugin actually writes `higson-cli-pull.log` and `higson-cli-push.log`.

*Evidence:* `git ls-files` returns nothing for .idea/.settings/.classpath/*.iml.

### Commit and Branch Practice — Weak Conventions, Stated Honestly
Do NOT enforce a commit-message format here; the history does not support one. Measured over 69 non-merge commits: conventional-commit prefixes appear in only 24.6% and adoption is *declining* (0 of the 9 commits since 2025). Jira `MPP-####` keys appear in 5.8%, covering 2 tickets ever. Branch prefixes (`bump/`, `feat/`) appear in 31% and only from 2024 onward. Do observe the two patterns that do hold: subjects are single-line with no body (90%), and `bump:` is the usual prefix for version-raise commits (~58% of them). Never reject a commit merely for lacking a prefix.

*Evidence:* full git history analysis over 91 commits, 2017-03-24 to 2026-08-04.

### Releases Ship as Docker Images, Not Git Tags
The repository has zero git tags across 9 years and no GitHub release flow. Releases are Docker images built and pushed by `push-to-docker.sh`. The Maven `<version>` is a perpetual `-SNAPSHOT` re-based onto the Higson runtime version line rather than following its own semver. When bumping the Higson runtime, update `pom.xml`, `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh` and `Readme.md` together — these files move as a unit.

*Evidence:* `git tag -l` returns 0 tags; `<version>` took only 3 values in 9 years; those 5 files top the change-frequency ranking, ahead of every Java source file.
