## Authoring Rules in the Snapshot

These standards govern the content of the snapshot itself — decision table definitions (`.def`/`.mat`) and Groovy functions. They apply whether the rule is edited in Higson Studio or in the pulled working tree, and they keep pricing logic self-contained inside Higson.

### Pricing Logic Belongs in Higson Rules, Not Java
All pricing logic — premium, limits, discounts — lives in Higson rules. Java is a thin layer that prepares the context and reads results back. A tariff change must never require recompiling the application; it is published from Studio.

*Evidence:* rules compute the domain attributes `PREMIUM`, `LIMIT_1`, `LIMIT_2` and discount `available`; Java only reads them.

### Parameter Code Mirrors the Snapshot File Path
A parameter's `code` is exactly its path inside the snapshot with `/` replaced by `.`. Profile/region-scoped parameters drop the `@profiles/<PROFILE>/@regions/<REGION>/<N>` prefix and keep the same rule. Codes are dot-delimited lowerCamel segments under a business-domain prefix (`demo.motor.*` in this app), sub-namespaced by concern.

```
@params/@global/demo/motor/coverage/availability.def  ->  code = "demo.motor.coverage.availability"
```

*Evidence:* 43 of 43 `.def` files match; 19 of 19 motor parameters sit under `demo.motor.*`.

### Input Levels Bind property == source to Canonical Model Paths
Every decision-table input level declares `property` and `source` set to the same canonical model path resolved by MotorContext/adapters, plus a short `code` and a human-readable `label`. Never push a value in ad hoc under a different name.

```toml
[[inputLevel]]
code = "gender"
label = "gender code (M, F)"
type = "string"
property = "quote.driver.gender"
source = "quote.driver.gender"
```

*Evidence:* 58 of 58 input levels across 43 `.def` files have `property` identical to `source`; paths always start at an adapter-exposed root (`quote.*`, `coverage.*`, `option.*`).

### Decision Tables Are Cacheable With a Wildcard Default Row
Declare `cacheable = true`. The `.mat` matrix is semicolon-delimited, opens with a `#matrix for parameter <code>` comment and a header row of level codes. Tables that must always resolve declare `nullable = false` and start the data with a wildcard row so no lookup can miss.

```
#matrix for parameter demo.motor.coverage.availability
coverage;option;available
*;*;no
BI;BRONZE;yes
```

*Evidence:* 42 of 43 `.def` files declare `cacheable = true`; 35 of 35 `.mat` files open with the comment; 34 of 35 follow with a header row.

### Groovy Function Header Contract
Every Groovy function opens with the generated TOML header block containing `#Full code: <code>` matching the file path, `categories = []`, `logLevel`, and an `[[arguments]]` list whose first entry is always `ctx` of type `SPECIFIED_CLASS` bound to a `ParamContext`. Do not hand-edit the header beyond description and extra arguments. Bodies end with an explicit `return`.

*Evidence:* 36 of 36 `.groovy` files carry the header and match `#Full code` to their path; 36 of 36 declare `categories = []` and `ctx` first; 34 of 36 end with an explicit return.

### Rule Bodies Read Only ctx and the higson Helpers
Groovy rule bodies never import or reference application Java classes. Read inputs from the context with typed accessors (`ctx.getNumber/getString/getInteger`), invoke other rules through the `higson` helper, and round arithmetic through the `math` helper. This keeps pricing logic fully inside Higson and independent of the Java model.

```groovy
def factor = higson.getNumber('demo.motor.coverage.bi.tariff', ctx)
def limit1 = ctx.getNumber('coverage.limit1')
return math.round((100000 + limit1) / 1000 * factor, 2)
```

*Evidence:* 0 of 36 Groovy functions contain an import; 0 of 36 reference `pl.decerto.higson.demo` classes.

### Known Drift: Legacy ParamContext Binding
New and touched functions should bind `ctx` to `io.higson.runtime.engine.core.context.ParamContext`. 29 of 36 existing functions still bind the legacy `org.smartparam.engine.core.context.ParamContext`. Migrate opportunistically when editing a function; do not open a dedicated sweep.

*Evidence:* 29 legacy vs 7 current across the 36 `.groovy` files in the snapshot.
