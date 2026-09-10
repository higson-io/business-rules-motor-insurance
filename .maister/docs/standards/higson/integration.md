## Java ↔ Higson Integration Boundary

The Java application is a thin shell around the Higson rules engine. These standards define where `io.higson.*` may appear, how domain objects are exposed to rules, and how the engine is configured — the goal being that a domain or rule restructure touches a small, predictable set of classes.

### Higson Runtime API Confined to adapter / context / service
`io.higson.*` types may only be referenced from the packages whose job is Higson integration: `adapter`, `context`, `service` (engine lookups), the `example` orchestrator, the shared `apk` layer and the Spring configuration class. The `api`, `dto`, `converter` and `domain` packages must stay entirely Higson-free.

*Evidence (2026-09-08):* `io.higson.*` imported by **24 of 107** files — adapter 12, context 3, example 3, service 1, `apk` 3 (`ApkAnalysis`, `QuestionCatalogue`, `RegimeReader`), `HigsonIntegrationConfiguration`, plus the profiler-only `ProfilersApi`; no DTO, converter or domain class references it.

⚠️ The `apk` package is on the list because the needs analysis was moved out of Groovy into it (`§14` of both profile documents), and it reads the engine directly rather than through an adapter — it walks the domain tree and asks tables, it does not expose a domain object to rules. That is why it needs no adapter and why it is named here explicitly rather than left as an exception someone has to rediscover.

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

*Evidence (2026-09-08):* 12 of 12 adapters follow this shape — 6 motor, 6 life.

⛔ **The property profile has no adapters at all** and this is deliberate, not an omission: `PropertyContext` builds the context path by path instead of exposing a domain object through a `Mapping`. For PROPERTY, therefore, the canonical paths are defined in `PropertyContext` — the sentence "defined here and nowhere else" holds per profile, not across the application.

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

### Reach a Rule Through the Domain Tree, Not by Its Code

An application asks the tree for an element and reads a rule from its **attribute**. It does not
name the rule. `engine.get(code)`, `engine.call(code)` and `engine.flow(code)` are the exception now,
and an exception has to be argued for — the PROPERTY profile has none since 2026-09-10.

```java
private ParamValue ruleOf(DomainObject element, String attribute, PropertyContext ctx) {
	return element.getAttribute(attribute).getValue(ctx);
}
```

Three things break when Java names the rule, and all three break silently:

- Repointing the attribute in Studio changes nothing, while looking as though it should.
- Deleting the element breaks nothing visible; the tree quietly stops describing the system.
- The call site encodes **what kind of rule it is** — `engine.flow` vs. `engine.get` — so swapping
  a table for a flow becomes a code change instead of a configuration change.

⛔ **Read the whole row, not a value per attribute.** `DomainAttribute.getValue(ctx)` returns a
`ParamValue`, so one attribute serves a rule with several outputs and there is no need for one
pointer per column — which is the pattern this repository has already removed twice. Keep the
`[column]` suffix in the attribute's value only where the answer genuinely is a single one.

⚠️ **An attribute backed by a FLOW has no named levels.** Reading it with `getBigDecimal("factor")`
throws `UnknownLevelNameException`; a flow returns a `FlowResult`, a Groovy function a plain map.
Hide that behind ONE helper — the moment each call site branches on it, knowledge of the rule's kind
is back in the application:

```java
Object raw = ruleOf(element, attribute, ctx).get();
return raw instanceof FlowResult flow ? flow.getResult() : (Map<?, ?>) raw;
```

⛔ **Shared layers take a supplier, never an address.** `ApkAnalysis` used to receive `(engine,
tableCode, ...)` — an address, in a class whose own header says it knows no rule. It now receives
`Function<String, ParamValue>`, so each profile reaches for its rows the way it reaches for
everything else. ⚠️ `deriveNeeds` still takes a table code for the motor and life demos: known debt.

*Evidence:* PROPERTY — 18 of 18 rule invocations go through an attribute; zero rule codes and zero
`engine.get`/`call`/`flow` remain in `Property.java`. MOTOR and LIFE are not converted.

### Reference Rule Codes Through Java Constants

> ⚠️ **Applies only where the rule is genuinely not in the tree** (dictionaries, prefetch). For
> anything an element could carry, the standard above takes precedence.

When Java has to name a Higson parameter, hold the code string in a constant rather than inlining it: a package-private constants holder for dictionaries, an enum for prefetch parameters. Supply context values with the varargs `HigsonContext(path, value, ...)` constructor using the same canonical paths the `.def` files bind to.

```java
abstract class Dictionaries {
	static final String VEHICLE_MAKES = "demo.motor.dict.vehicle.availableMakes";
}
```

*Evidence:* 4 of 4 dictionary codes live in `service/Dictionaries`, 3 more in the `PrefetchParameters` enum; 4 of 4 lookups pass a constant plus a `new HigsonContext(...)`. Confidence 68 — emerging convention, small sample.
