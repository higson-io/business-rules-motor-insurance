## Rules Snapshot & Studio Sync

The business rules of this application live in a Higson snapshot that is synchronised with a Higson Studio instance through the `higson-cli` Maven plugin. These standards define where the snapshot lives, how it is moved in and out of the working tree, and what that implies for version control and release workflow.

### Snapshot Directory Is the Single Source of Truth
`src/main/higson` is the one canonical location of the Higson rules snapshot, addressed everywhere through the `${higson.snapshot.dir}` Maven property. The `higson-cli` plugin (pull rewrites it, push uploads it) and `SnapshotDataSourceDemoTest` all point at that same directory. Never introduce a second snapshot copy, a zip fixture for tests, or a hard-coded path.

*Evidence:* pom.xml:24-26 comment "Single source of truth for the snapshot: shared by the CLI (pull/push) and the SnapshotDataSourceDemoTest"; pom.xml:149 `<higsonSnapshotDir>${higson.snapshot.dir}</higsonSnapshotDir>`; SnapshotDataSourceDemoTest Javadoc.

### The Snapshot IS Version-Controlled Since 2026-09-04 — But Studio Is Still Where You Change It
⛔ **REVERSED.** Until 4 September 2026 `src/main/higson/` was never tracked: absent from all 91 commits across 9 years, and at some point added to `.gitignore` (which the earlier wording of this standard did not know about). It is now committed, in full, for all three profiles.

**Why it changed.** Every rule in the three demos existed only inside a local Studio container. A container that is pruned, a laptop that is reimaged, and the entire configuration is gone — there was no copy of it anywhere. The repository is now that copy.

⚠️ **Studio remains the place where rules are CHANGED.** The committed tree is a snapshot, so it drifts the moment anyone edits in Studio without exporting. Re-export before relying on it, and treat a stale working tree as the normal case rather than as a fault — this is exactly the trap that made an earlier session read section definitions out of files that no longer matched Studio.

**Getting a fresh copy.** `mvn higson-cli:pull` needs a Studio token, which by policy lives nowhere in the repo (see `global/security.md`). Without one, export through the Studio MCP server and copy the archive out of the container:

```bash
# MCP: higson_export_snapshot outputPath=/usr/local/tomcat/temp/higson-mcp-workspace/snapshot.zip profileCodes=["all"]
docker cp higson-demo-local-studio:/usr/local/tomcat/temp/higson-mcp-workspace/snapshot.zip .
```

⚠️ **Check the export for secrets before committing it.** This one was clean — `config.json` carries only export metadata, and no token, password or JDBC URL appears anywhere in the tree — but that is a property of this export, not a guarantee about the next one.

**Size.** 588 files, 27 MB unpacked, of which ~25 MB are three files in the motor profile: two vehicle dictionaries and a risk-assessment model. They compress to roughly 4 MB in git.

*Evidence:* `.gitignore` no longer ignores it; `SnapshotDataSourceDemoTest` boots an engine from the committed tree and passes.

### Higson CLI Runs On Demand With Safe Defaults
The `higson-cli-maven-plugin` is deliberately declared with no `<executions>`, so snapshot sync is never a side effect of a normal build. Defaults stay safe: nothing is deleted from Studio and nothing is auto-committed. Narrow the scope by adding `<pullOptions>`/`<pushOptions>` in pom.xml rather than by weakening the defaults; auto-commit requires an explicit opt-in flag per invocation.

```bash
mvn higson-cli:pull                      # Studio -> src/main/higson
mvn higson-cli:push                      # src/main/higson -> Studio, deletes nothing
mvn higson-cli:push -Dautocommit=true    # deliberate opt-in
```

*Evidence:* pom.xml:136-152 comment block; `src/main/higson/config.json` sets `removeNotInSnapshot:false` and `forceDelete:false` for functions, parameters and domain.

### Version Business Logic Through the Snapshot, Not SQL Migrations
The database schema is owned by Higson and the application treats it as a black box. There are no Flyway/Liquibase migrations and none should be added. Business logic changes are versioned as snapshot diffs synchronised with `higson-cli:pull` / `higson-cli:push`. Since 2026-09-04 those diffs are visible in git as well — but only as far as somebody re-exported; see the caveat above.

*Evidence:* no migration tooling on the classpath; no application-owned entities or repositories.

### Bump the Higson Runtime and CLI Plugin in Lockstep
`io.higson:spring-boot-starter-higson-runtime` and `io.higson:higson-cli-maven-plugin` must always be raised to the same version together — they have to be compatible. After bumping: re-pull the snapshot and run the rule tests. Version bumps also touch `Dockerfile`, `docker-compose.yml`, `push-to-docker.sh` and `Readme.md`; these files move as a unit.

*Evidence:* both artifacts currently pinned to 4.3.1; git history shows bump commits touching pom.xml + docker-compose.yml + Readme.md together.
