## Java ↔ Higson Integration Boundary

The Java application is a thin shell around the Higson rules engine. These standards define where `io.higson.*` may appear, how domain objects are exposed to rules, and how the engine is configured — the goal being that a domain or rule restructure touches a small, predictable set of classes.

### Higson Runtime API Confined to adapter / context / service
`io.higson.*` types may only be referenced from the packages whose job is Higson integration: `adapter`, `context`, `service` (engine lookups) and the `example` orchestrator. The `api`, `dto`, `converter` and `domain` packages must stay entirely Higson-free.

*Evidence:* `io.higson.*` imported by 10 of 47 files, all in adapter (6), context (1), service (1), example (1) plus the profiler-only ProfilersApi; 0 of 9 DTOs, 0 of 4 converters and 0 of 7 domain classes reference it; 0 of 5 api classes import adapter or context.

### One Adapter Per Domain Class, Exposing Canonical Paths
Each domain aggregate gets one `Adapter` extending `io.higson.runtime.ext.adapter.Adapter`, holding the domain object in a single-argument constructor and exposing `getMapping()` returning a fluent `Mapping`. Leaf values map directly, child aggregates by nesting a child Adapter, collections via `CollectionAdapter`. Canonical path segments (`quote.driver.gender`, `coverage.limit1`, `option.code`) are defined here and nowhere else — so a domain restructure in Studio touches only the adapters.

```java
public class QuoteAdapter extends Adapter {
	private Quote quote;

	public QuoteAdapter(Quote quote) { this.quote = quote; }

	@Override
	protected Mapping getMapping() {
		return new Mapping()
			.add("driver", new DriverAdapter(quote.getDriver()))
			.add("options", new CollectionAdapter(quote.getOptions(), o -> new OptionAdapter((Option) o)));
	}
}
```

*Evidence:* 6 of 6 adapters follow this shape; 3 of 6 nest child adapters; 2 of 6 use CollectionAdapter.

### Wire Only the DataSourceProxy Manually
The starter auto-configures engine, data source, dialect, dev mode, instance name and diagnostic profiler from `higson.*` properties — do not re-declare them. The single piece still wired by hand is the `DataSourceProxy` wrapping the starter's data source so JDBC profiling keeps reaching `/profilers/jdbc`. Its `BeanPostProcessor` `@Bean` must be declared `static` so it registers before the beans it post-processes.

*Evidence:* HigsonIntegrationConfiguration Javadoc: "the only piece we still wire manually is the Higson DataSourceProxy"; "Declared static so it is registered early, before the beans it post-processes."

### Configure the Engine Only Through higson.* Properties
Keep runtime safety switches at their documented defaults: `dev-mode-enabled=false`, `function.validate-arguments=true`, `diagnostic.profiler.enabled=false`. Do not set `higson.database.dialect` — the starter detects it from the JDBC URL. Do not set `higson.database.schema` — the schema comes from the driver's `currentSchema` parameter, so that it is not applied as a runtime-qualified schema.

Documented exception: `higson.runtime.diagnostic.profiler.enabled=true` is allowed in a demonstration or presentation configuration, and only there — never in a production one. A rule-execution-trace view (which rules fired, how often, served from cache or loaded from the database) is the strongest argument a rules engine can make for itself, and it needs the profiler on; the need recurs in every demo configuration, which is why it is documented here rather than argued case by case. Four conditions come with it: set the property in the properties file with a comment marking it demo-only and not to be carried over to production; wire `/profilers/clear` into the calculator's session reset so statistics describe one session instead of accumulating; accept the cost knowingly — the diagnostic profiler flushes per-invocation call stacks to the database, and a UI that recalculates on every field change writes a lot of them; leave the other two safety switches at their defaults.

```properties
higson.database.url=jdbc:postgresql://localhost:10432/higson?currentSchema=public
# demo-only exception — do not carry over to a production configuration
higson.runtime.diagnostic.profiler.enabled=true
```

*Evidence:* higson-demo-app.properties comments; docker/app.properties repeats the same keys; docker-compose.yml:36. The demo exception comes from a design review of the profiler-trace feature.

### Rule Execution Traces Come From the Profiler, Not From Rule Bodies
Never add logging or trace-collection code to Groovy rule bodies to build an execution-trace view — that breaks *Rule Bodies Read Only `ctx` and the `higson` Helpers* in `rules-authoring.md`. Take the trace from the engine. `EngineProfiler`, exposed by `ProfilersApi` at `/profilers/parameters`, `/profilers/functions`, `/profilers/domain/attr` and `/profilers/jdbc`, returns aggregate statistics — `key`, `count`, `avgTime` — split into `invokeStats` and `loadStats`, and that split is what distinguishes a cache hit from a database load. It works regardless of the diagnostic profiler flag; enabling the flag adds per-invocation call stacks (call order and nesting) persisted to the database. The profiler returns no match information: neither the matched `.mat` row nor the input-level values can be read from it. Input-level values are already known to the Java layer because the adapters built the context, and the matched row belongs in Studio — link to the table there instead of rebuilding the matrix in the application UI. `ProfilerHelper.printSummaryAsHtml` renders for a browser, not for a SPA panel, so a JSON-returning variant is needed when the trace feeds a front-end view.

*Evidence:* `ProfilersApi` exposes four `/profilers/*` endpoints over `EngineProfiler.PARAMETER` / `EngineProfiler.FUNCTION` `getInvokeStats()` / `getLoadStats()` and `JdbcProfiler`; the `DataSourceProxy` wired in `HigsonIntegrationConfiguration` keeps JDBC profiling reaching `/profilers/jdbc`; higson-demo-app.properties:21 documents the diagnostic profiler as "per-invocation call-stacks flushed to the database". Confidence 55 — derived from one design review plus the existing `ProfilersApi`, not from a broad code sample.

### Reference Rule Codes Through Java Constants
When Java has to name a Higson parameter, hold the code string in a constant rather than inlining it: a package-private constants holder for dictionaries, an enum for prefetch parameters. Supply context values with the varargs `HigsonContext(path, value, ...)` constructor using the same canonical paths the `.def` files bind to.

```java
abstract class Dictionaries {
	static final String VEHICLE_MAKES = "demo.motor.dict.vehicle.availableMakes";
}
```

*Evidence:* 4 of 4 dictionary codes live in `service/Dictionaries`, 3 more in the `PrefetchParameters` enum; 4 of 4 lookups pass a constant plus a `new HigsonContext(...)`. Confidence 68 — emerging convention, small sample.
