## Testing Higson Rules

> ⛔ **STAN NA 2026-09-08: KATALOG `@tests` NIE ISTNIEJE.** Ten standard opisuje pokrycie, którego
> w tym repozytorium nie ma — ani jednego testu regułowego. Reguły trzymające całe zachowanie
> systemu (taryfa, dostępność pokryć, dobór pakietu, treść uzasadnienia) nie mają automatycznej
> weryfikacji; jedyne, co je sprawdza, to ręczne przejście przez demo. Reguła poniżej jest nadal
> tym, co chcemy robić — ale nie opisuje tego, co jest.

The business logic of this application lives in rules, not in Java. Consequently most meaningful test coverage here is rule coverage: the Java layer only prepares the context and reads results back, so testing it proves almost nothing about pricing behaviour. These standards define where rule tests live and how Java-side tests exercise rules without a live Studio database.

### Business Logic Is Covered by Higson Rule Tests
Coverage of the pricing logic comes from Higson rule tests in `src/main/higson/@tests` (9 files, run by Studio or the runtime), not from Java unit tests. Java tests cover integration wiring. When you add a new business rule, its test belongs in the snapshot. Note the caveat in `higson/snapshot.md`: the snapshot is committed to git since 2026-09-04, so these tests are not visible in the repository.

*Evidence:* `src/main/higson/@tests` holds 9 test files plus 2 `@testPackages`; the Java side has one integration test.

### Run Rule Tests Off the Snapshot, Not a Live Database
Java tests that exercise Higson rules boot the engine from the snapshot directory via `HigsonSnapshotDataSource` instead of a live Studio database. This gives self-contained, offline, reproducible runs with no database and no network. `HigsonSnapshotDataSource` accepts either a `.zip` or an unpacked directory.

```java
HigsonSnapshotDataSource ds = new HigsonSnapshotDataSource(new File("src/main/higson"));
HigsonEngineFactory factory = new HigsonEngineFactory();
factory.setDataSource(ds);
engine = factory.create();
```

*Evidence:* SnapshotDataSourceDemoTest Javadoc: "the engine then reads parameters/domain/functions directly from the snapshot files — no database, no network, fully self-contained."

⚠️ **A snapshot-booted engine is not identical to the live runtime — do not generalise its failures into production configuration changes.** Leave `setValidateFunctionArgumentsDataTypes` at the factory default in test engines. Turning it on to "mirror production" makes the snapshot reader fail to bind the declared `ParamContext` argument of a Groovy function when a **flow** invokes it, so a coverage that prices correctly in the running application fails in the test. Isolated by a four-cell differential varying the data source and the flag independently:

| Data source | `validate-arguments` | Result |
|---|---|---|
| snapshot | false | prices |
| snapshot | **true** | **fails** |
| live DB | false | prices |
| live DB | **true** | prices |

The data source is the discriminator, not the property. `standards/higson/integration.md`'s `function.validate-arguments=true` clause is **correct** and must not be "fixed" on the strength of a snapshot-engine failure. This cost real time once already: a confident recommendation to change production configuration and rewrite a standard was produced from a two-row probe. **Vary one factor at a time and cover the full matrix before proposing a production change.**

### Tests Read the Canonical Snapshot Directory Directly
Snapshot-backed tests consume the same `${higson.snapshot.dir}` (`src/main/higson`) that the CLI syncs, so a rule change pulled from Studio is exercised by tests without any fixture duplication or zip extraction step. Never copy the snapshot into a test resources directory.

*Evidence:* pom.xml comment naming the directory as shared between the CLI and SnapshotDataSourceDemoTest.
