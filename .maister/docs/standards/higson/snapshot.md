## Rules Snapshot & Studio Sync

The business rules of this application live in a Higson snapshot that is synchronised with a Higson Studio instance through the `higson-cli` Maven plugin. These standards define where the snapshot lives, how it is moved in and out of the working tree, and what that implies for version control and release workflow.

### Snapshot Directory Is the Single Source of Truth
`src/main/higson` is the one canonical location of the Higson rules snapshot, addressed everywhere through the `${higson.snapshot.dir}` Maven property. The `higson-cli` plugin (pull rewrites it, push uploads it) and `SnapshotDataSourceDemoTest` all point at that same directory. Never introduce a second snapshot copy, a zip fixture for tests, or a hard-coded path.

*Evidence:* pom.xml:24-26 comment "Single source of truth for the snapshot: shared by the CLI (pull/push) and the SnapshotDataSourceDemoTest"; pom.xml:149 `<higsonSnapshotDir>${higson.snapshot.dir}</higsonSnapshotDir>`; SnapshotDataSourceDemoTest Javadoc.

### The Snapshot Is NOT Version-Controlled — Studio Is the System of Record
IMPORTANT — this is the single most consequential fact about this repo's workflow. `src/main/higson/` has never been tracked in git: it is absent from all 91 commits across 9 years, is untracked in the working tree, and is not listed in `.gitignore`. The rules live in the Higson Studio database and are moved in and out of the working tree on demand. Consequences: a Java change and its matching rule change are never atomically committed; a fresh clone has no rules until you run `mvn higson-cli:pull`; and when reading rule-dependent Java you must assume the Studio instance, not the repo, holds the matching rules.

*Evidence:* `git log --all --name-only -- 'src/main/higson/*'` returns empty across all refs; `git status` shows `?? src/main/higson/`; no `.gitignore` entry matches it.

### Higson CLI Runs On Demand With Safe Defaults
The `higson-cli-maven-plugin` is deliberately declared with no `<executions>`, so snapshot sync is never a side effect of a normal build. Defaults stay safe: nothing is deleted from Studio and nothing is auto-committed. Narrow the scope by adding `<pullOptions>`/`<pushOptions>` in pom.xml rather than by weakening the defaults; auto-commit requires an explicit opt-in flag per invocation.

```bash
mvn higson-cli:pull                      # Studio -> src/main/higson
mvn higson-cli:push                      # src/main/higson -> Studio, deletes nothing
mvn higson-cli:push -Dautocommit=true    # deliberate opt-in
```

*Evidence:* pom.xml:136-152 comment block; `src/main/higson/config.json` sets `removeNotInSnapshot:false` and `forceDelete:false` for functions, parameters and domain.

### Version Business Logic Through the Snapshot, Not SQL Migrations
The database schema is owned by Higson and the application treats it as a black box. There are no Flyway/Liquibase migrations and none should be added. Business logic changes are versioned as snapshot diffs synchronised with `higson-cli:pull` / `higson-cli:push` — subject to the caveat above that the snapshot is not currently committed.

*Evidence:* no migration tooling on the classpath; no application-owned entities or repositories.

### Bump the Higson Runtime and CLI Plugin in Lockstep
`io.higson:spring-boot-starter-higson-runtime` and `io.higson:higson-cli-maven-plugin` must always be raised to the same version together — they have to be compatible. After bumping: re-pull the snapshot and run the rule tests. Version bumps also touch `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh` and `Readme.md`; these files move as a unit.

*Evidence:* both artifacts currently pinned to 4.3.1; git history shows bump commits touching pom.xml + docker-compose.yml + Readme.md together.
