## Java Language & Build Conventions

Language level, dependency and source-file conventions for the Java backend. These standards were discovered from this codebase and apply to every Java source file and to the Maven build.

### Java 17 via maven.compiler.release
Compile against Java 17 using the `release` flag, never a `source`/`target` pair. Preview features are disabled and must not be used. The compiler, both IDE configurations and the runtime image all stay on 17.

*Evidence:* pom.xml `<maven.compiler.release>17</maven.compiler.release>`; Eclipse prefs compliance=17 with preview disabled; .idea/misc.xml JDK_17; Dockerfile `eclipse-temurin:17-jre-jammy`.

### Explicit Version Pinning for Non-BOM Dependencies
Inherit versions from `spring-boot-starter-parent` for anything it manages — declare those with no `<version>`. Pin every other dependency and plugin to an exact version. Version ranges, `LATEST`/`RELEASE` and unpinned plugins are not used.

*Evidence:* spring-boot-starter-web, spring-context and HikariCP declared without version; Higson 4.3.1, h2 2.1.212, commons-collections4 4.4, junit 4.13.2 and all four plugins pinned exactly.

### Layer Suffix in Class Names, Matching the Package
Every class carries a suffix naming its layer and the suffix matches its package: `api/*Api`, `api/dto/*Dto`, `converter/*Converter`, `adapter/*Adapter`, `service/*Service`, `exceptions/*Exception`. PascalCase, one public type per file, file name equals class name.

*Evidence:* 30 of 33 classes in suffix-bearing packages follow it; the 3 exceptions are intentional helpers (Dictionaries, DictionaryEntry, GlobalExceptionHandler).

### Retain Method Parameter Names in Bytecode
Compile with `-parameters` so Spring binds constructor and handler arguments without redundant `@Qualifier` / `@RequestParam("name")` annotations. Both IDE configurations match the Maven build.

*Evidence:* .idea/compiler.xml `-parameters`; Eclipse `codegen.methodParameters=generate`; spring-boot-starter-parent adds it by default.

### Explicit Imports, java.* First
Always fully qualified single-type imports; star imports are never used. Where `java.*` imports exist they come first, then third-party (`org.*`, `io.higson.*`), then project packages — usually as one contiguous block without blank-line grouping.

*Evidence (2026-09-08):* 0 of 107 files contain a wildcard import; files importing from `java.*` place it first.

### SLF4J Logger as private static final
Logging goes through SLF4J obtained with `LoggerFactory.getLogger(<Class>.class)` in a `private static final` field, using parameterised `{}` messages. No `System.out`, `java.util.logging` or Log4j usage anywhere.

```java
private static final Logger LOG = LoggerFactory.getLogger(Motor.class);

LOG.info("quote after calculation: {}", quote.print());
```

**Known drift:** the field name is split — `LOG` in 2 files, `log` in 1 (`Motor`). Prefer `LOG` for new code.

*Evidence:* 3 of 3 logging classes use the pattern; naming inconsistent across the 3.
