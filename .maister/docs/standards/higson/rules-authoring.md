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

### A Value That Only Records a Structural Split Belongs in Code, Not in Configuration

When a number exists solely to encode a distinction the structure already makes, configuring it adds
a value to maintain without adding a decision anyone will make. Put it in the application as a named
constant and say in one place what the structure means.

*Evidence (2026-09-10):* needs declared by the client live on question attributes, needs inferred
from the subject live in a table; the weight settling collisions between them was first a type
default plus a column, then two constants — `WEIGHT_DECLARED` / `WEIGHT_INFERRED` — because it never
varied per question and never could without contradicting where each need comes from.

⛔ This is the **narrow** exception to "pricing and product logic belong in rules". It applies when
the value has exactly one sensible setting given the structure. A threshold someone might tune, a
rate, a limit, a sentence the client reads — none of those qualify, however constant they look today.

⚠️ Verify the distinction is real before flattening it: here it settled four collisions and changed
the outcome in three, always the same direction. Had it changed outcomes in both directions, it would
have been a genuine dimension and belonged in a table.

### Put the Arithmetic in a Function Bound to `source`, Not in the Application

A decision table cannot divide, so somebody must compute the number a threshold is compared against.
That somebody is a **small function bound to the level's `source`** — not the application. The table
then shows one honestly named column, and the reader sees what is compared with what.

```toml
[[inputLevel]]
code = "ratio"
label = "suma wybrana do wyliczonej"
matcher = "between/ii"
levelCreator = "$f demo.property.util.sumRatio"
source = "$f demo.property.util.sumRatio"
```

⛔ **A column whose meaning changes per row is the symptom to look for.** One table serving several
subjects, switched by a `family`-style column, ends up with a level named "proportion or count" —
which is an admission that it cannot be named. Split it: one table per question, each column
computed by its own function.

⚠️ **Moving a decision out of Java moves its guards too.** An `if (value != null && value > 0)` that
used to stand before the call must become a **closed lower bound** in the table, because an open
bound (`* - 0.85`) matches `null` (W-30) — otherwise a client who supplied nothing is told their
figure is too low.

⚠️ The cost is real and worth stating: this split traded one table for four plus four functions.
Configuration grew so it could be read without the code — that is the trade, and it is only worth
making where someone outside the codebase has to understand the result.

### Check `find_usages` BEFORE Deleting, and Publish the Session After Fixing

Two mistakes from one afternoon, both cheap to avoid:

⛔ **`higson_find_usages` before every delete.** A function deleted without it was called by a flow
two hops away (`apk.recommend` → `apk.covers` → the deleted one), and every request returned HTTP 500.
The tool answers in one call; `confirm: true` on the delete silences the very check that would have
caught it.

⛔ **`open` → `get` → `update` is mandatory for matrix edits.** An `UPDATE` whose ids came from a
`get` without a preceding `higson_open_decision_table` **adds a row instead of replacing it** — and
the table can keep behaving correctly, because the narrower row still wins, so only reading the
snapshot reveals it. It happened three times in one pass.

⚠️ **A corrected function does nothing until its work session is published.** `higson_update_function`
opens a new session; rebuilding and retesting against an unpublished change reproduces the original
error exactly, which reads as "the fix did not work".

### A Rule Belongs to One Element — So It Carries No Column Naming That Element

When an element can hold a pointer, give it **its own** table, keyed only by what actually decides
the answer. The identity is the place in the tree, so a `question`/`cover`/`code` column is
redundant — and a shared table with such a column is a rule *about* elements rather than a rule *of*
one, which is how unrelated branches end up sharing a matrix.

```
# question/assistance/visible.mat — hangs under HOME_ASSISTANCE.IS_VISIBLE
rentedOut;visible
*;false
true;true
```

⚙️ The column IS needed where **no element exists to hold the pointer** — subject fields here are
catalogue rows, so their visibility table keys on the field code and lives beside their catalogue.
That is the criterion: not "how many rows", but "is there an element that could carry this".

⛔ **Do not put a rule for one branch on another branch's element.** A rule about step two hanging
under the needs analysis will be read as belonging there, and the name will not save it.

### Derive From the Children, Except Where the Parent Is a Fixed Frame

A parent's property that is a function of its children — "the step is required if any of its
questions is" — is derived, never configured: an attribute on the parent is the same rule written
twice, and the copy drifts silently. Check it against the data first; here the group attribute
matched its questions 6 times out of 6, which is what proved it redundant.

⚠️ **But not every parent property is derived, and the difference is not obvious.** Whether a step
is *listed* is a fixed frame, not a function of its children — the outline has to be stable so a
reader sees how many stages there are before anything is answered. Conditionality belongs inside the
step. Deriving that one the same way as "required" is a regression that the scenarios miss, because
they watch the leaves; only a full baseline diff catches it.

### A Dependency Between Questions Is a Dependency on Answers

The answers are already in the context, under the paths the questions declare. So a rule that decides
whether to ask the next question takes **those paths as input levels** — and the dependency is done.
There is nothing to compute in the application.

```
question;propertyKind;rentedOut;visible      # inputs: question.code + two answer paths
*;*;*;true
HOME_ASSISTANCE;*;*;false
HOME_ASSISTANCE;*;true;true
```

⛔ **Do not build a chain beside the engine.** The version this replaced computed the *state of the
source question* in Java — answered / refused / required / matching — pushed it through five context
fields into a table with five input levels, and had that table say "revealed". Roughly 75 lines of
application code, one table and two attributes, all to express what one input level expresses.

⚠️ **The warning sign is a table with no column naming the thing it decides about.** That old table
had no `question` column at all: it keyed only on state the application had just calculated. A rule
whose inputs come from the application's own computations rather than from the quote is usually a
mechanism invented alongside the engine, not configuration.

⚙️ Row specificity, not order, decides which row wins — so a general row for the whole question and a
narrower one for the condition coexist, and a missing answer falls to the general one. Verify it with
the empty case, not only the interesting one.

### Do Not Configure What the Element Already Implies

Before adding an attribute, ask whether its value can be **derived from what the element already
carries**. A chain saying "this step comes after that one" beside a `POSITION` of 200, 210, 220 is
the same fact written twice — and the second copy is the one that goes stale.

*Evidence (2026-09-10):* `revealedBy` held two roles. On five groups it restated the order `POSITION`
already gave; on three questions it carried a genuine condition on an answer. Only the second kind
survived. The end of a survey likewise needs no flag: it is the absence of a next position.

⛔ **A mechanism nobody demonstrates is configuration maintained for no reason.** When the owner says
a feature will not be shown, delete it rather than relocating it — the gate removed here had one row,
one attribute pair, a model field, a DTO field, a converter line, two signals, a template block and a
stylesheet rule, all kept alive for a case that was never going to be on screen.

⚠️ Splitting one attribute's rows by **role** is the cheap check: if some rows restate a neighbour and
others do not, the attribute is doing two jobs and only one of them is real configuration.

### Two Mechanisms With One Effect Are One Mechanism

Before adding a flag beside an existing one, check whether anything uses **both**. Compute the
intersection of the populations they govern: an empty intersection means they are the same concept
under two names, applied to different halves of the data.

```python
set(rows_of_rule_a) & set(rows_of_rule_b)   # empty → merge them
```

*Evidence (2026-09-10):* a question's visibility (`IS_VISIBLE`) and its reveal chain (`revealedBy`)
both answered "is this question asked or not". Visibility governed two subject fields, the chain six
survey questions, and **no question used both**. "The floor does not apply to a house" turned out to
be expressible as a reveal condition, so one attribute, one table and one wire field went away.

⚠️ Look for the one place the duplication was load-bearing before merging — here a progress counter
that counted by one flag while the other decided rendering. Merging is right anyway when that
consequence does not actually occur in the data, but it has to be found and stated, not discovered
by a user.

⛔ A test that encodes the distinction goes with the distinction. Rewrite it to cover what remains;
never bend it until it passes against the merged concept.

### One Cell, Either a Value or a Rule — That Is What Makes an Element Self-Describing

An element attribute holds a constant **or** a pointer to a rule, in the same cell. Use that: write
the answer literally where it never varies for this element, and point at a rule where it does.
Looking at the element then tells you whether there is anything to decide at all, and turning one
into the other touches nothing but that cell.

```
QUESTIONS[NEEDS_PROPERTY]/IS_VISIBLE = true                          # concerns everyone
SUBJECT_FIELD FLOOR       /IS_VISIBLE = $p ...question.visibility    # depends on the property kind
```

⛔ **Many elements carrying the same literal is fine; many carrying the same pointer is the smell.**
The criterion is not how many attributes repeat, but whether the value **says something about this
element**. `true` beside `$p rule` says something. The same `$p rule` on all of them says nothing,
and that is what an earlier pass in this repository removed.

⚙️ A table whose only real content is one or two rows beside its `*` row is usually this pattern
waiting to happen — the answer belongs on the elements, and the table can go. Check what remains
keys on before deleting: a table that keys on rows **without** elements, or on something other than
the element (a chain's state, a second dimension), is not the same case and stays.

⚠️ When behaviour moves onto elements, the code that reads it must tolerate elements that do not
exist — in a tree where only part of a population is modelled, a lookup returning `null` is the
normal case, not a fault.

### A Catalogue Table Earns Its Place Only When Its Rows Have No Element

Before accepting "a catalogue table already owns facts of this kind" as the reason a constant stays
in a table (see the standard below), **count how its columns are filled against the tree**. A column
filled only for rows that ARE elements, beside a column filled only for rows that are NOT, means the
table is two tables sharing a name — and the half that has elements belongs on them.

```python
# per column: how many rows filled, and how many of those exist in the tree
filled = [r[0] for r in rows if r[i].strip()]
in_tree = sum(1 for f in filled if f in element_codes)
```

*Evidence (2026-09-10):* `demo.property.apk.question` had 27 rows; 5 of its 11 columns were filled
**only** for the 13 subject fields and 2 **only** for the 14 tree elements. Splitting it moved six
facts onto `QUESTION` / `QUESTION_GROUP` and left a table that honestly describes subject fields.

⛔ The split stops where the elements stop. Three sibling tables stayed exactly as they were, each
for its own reason, and none of them is untidiness: one keys on rows that have no element, one has a
**second dimension** (so D-23 keeps it a table), and one does not key on the row at all.

⚙️ Prove the element decides, rather than assuming it: change one attribute in Studio, publish, wait
out the runtime watcher, and check the application without restarting it. Matching output before and
after a migration proves nothing — the data is the same either way.

### A Per-Element Constant Goes on the Element — Unless a Catalogue Table Already Owns Its Kind

D-23 says a constant that differs per element belongs on the element, because a table with only one dimension is a table pretending to be a lookup. That rule stands, and it has one exception that is already built and must not be "corrected" back: **where a catalogue table already exists whose declared job is "one row per X, everything that is constant about it", a new per-X constant goes there as a column.**

The live example is `demo.property.apk.question`. §17 moved seven per-question constants *out of* element attributes *into* that table on purpose, and the table also carries `text`, `kind` and `contextPath` for questions that are not tree elements at all. Adding `shortLabel` (2026-09-09) as an element attribute would have split one kind of fact across two homes and cost a run-in with **W-29** for the sake of one line of text.

The deciding question is not "does it have a second dimension" but **"is there already a place that owns facts of this kind"**. If yes, put it there. If no, put it on the element.

⚠️ A second name for the same thing is still forbidden — that is what D-20 removed when it deleted the `TEXT` attribute, after four of nineteen questions had silently drifted from their element names. `shortLabel` passes only because it is **a different thing** (a line in a table of contents, not the question) and because both appear on screen at once, so a drift is visible rather than hidden.

⚙️ **The `TEXT` attribute came back on 2026-09-09, and the condition that lets it come back is the whole point.** D-20 objected to the *duplication*, not to the attribute: the text sat in the element name **and** in an attribute, and nothing kept the two equal. It is allowed again because the element name stopped being the same text — names are now short branch labels ("Wynajem"), the attribute carries the question. The load-bearing part is that the reader **must not fall back**: `Property` reads `TEXT` with no `orElse(element.getName())`, so a missing attribute yields an empty question that is obvious on screen, rather than a quiet return to a second source of truth. A fallback would rebuild exactly what D-20 deleted.

### Group Domain Attributes by NAME, Not by Number

An attribute carries three positioning fields, and only two of them are visible to a reader:

| Field | Where it appears | What it does |
|---|---|---|
| `group` | type definition (`higson_get_domain_definition`) | the group's **name**, rendered as a section header in Studio |
| `groupName` | element details (`higson_get_domain_element_details`) | the same value, under a different key |
| `groupOrder` | both | which group; **`0` means no group** |
| `attributeOrder` | both | position inside the group |

⛔ **`groupOrder` alone buys nothing.** Setting `groupOrder: 1 / 2 / 3` without names produces three anonymous blocks separated only by spacing — the reader still has to work out what each block is for. The name is what makes the grouping legible, and it is the whole point of grouping.

⛔ **Name a group only where the grouping means something; leave the rest ungrouped at `groupOrder: 0`.** Forcing every attribute into a named bucket produces labels like "Pozostale", which say nothing.

**The group name is a label, not a code.** `PRODUCT` on the property demo reads *Podstawy wyceny* / *Warunki oferty* / *Notatka o wyborze klienta* — a phrase in the same register as the attribute names beside it, ordered to follow the process (what feeds the price → what the offer document says → what records the client's choice). A code-shaped name like `OFFER` fits the field but not the reader.

⚠️ Attribute and group names in this configuration avoid Polish diacritics, matching the rest of the Studio metadata; attribute *values* carry them normally.

⚠️ **The MCP tool's schema does not list `group`, but it round-trips.** `higson_update_domain_type` documents attributes as `{code, name, defaultValue?, defaultValueType?, action?, attributeOrder?, groupOrder?}`; passing `group` alongside them works and persists (verified 2026-09-09 on a throwaway type). Do not conclude from the schema that grouping has to be done by hand in the UI.

⚠️ In the snapshot the field is `<groupName>` inside `<typeAttribute>` in `@def/<PROFILE>.xml`.

### An Element Says What It Does; How It Computes Belongs in Its Table

The criterion for what earns a place in the domain tree (2026-09-09): **an attribute stays when the
application reads it through the attribute, or when it is the one thing a business user opens on this
element.** Everything else is a rule's internal wiring and belongs where the rule is.

The practical shape of that: in each attribute group, the **first attribute is the result** and the
rest were its ingredients. `COVER` carried `RATE` / `RATE_BASIS` / `CAP_RATIO` / `CAP_ABSOLUTE` —
four pointers into columns of the cover's **own** table (`demo.property.cover.<code> [rate]`, and so
on). Seven covers carried twenty-eight addresses that said what the naming convention already says.
They are gone; `cover.premium` and `limit.effective` derive the table name the same way they already
derived the element path (`/PRODUCTS[HOME]/COVERS[<code>]`), so this reuses an existing assumption
rather than adding one.

What stays on a cover: `IS_AVAILABLE` + `UNAVAILABLE_REASON`, `SUM` + `SUM_BASIS` + `SUM_REFERENCE`,
`PREMIUM`, `LIMIT` + `LIMIT_LABEL`, `POSITION`. ⚠️ `SUM_BASIS` and `SUM_REFERENCE` look like
ingredients but stay: they are **literals**, not pointers, and "where does this cover's sum come
from" is a business question. Pushed into the table they would be a column with the same value in
every row — which is what D-23 exists to prevent.

⛔ This narrows D-16 rather than reversing it. §23 said "remove attributes that point at no rule";
the tree is still a table of contents, it just stops listing ingredients beside results.

⚠️ **Order the migration so the rule stops reading the attribute BEFORE the attribute disappears.**
Functions first, publish, verify the numbers are identical, then edit the type. The reverse order
leaves a window where the rule reads something that is gone, and removing an attribute deletes its
value on every element at once — twenty-eight values here, recoverable only from an export.

⛔ **Pass `action: "NO_CHANGES"` on every attribute when you are only regrouping.** The default is
`SET_TO_ALL`, which overwrites the value on **every element** with the type's default (W-29) — a
naming pass over `COVER` would have wiped seven covers' pointers to their own tariff tables. And send
values **without** the `$p` / `$f` / `$w` prefix: the engine stores them bare and adds the prefix for
display, so echoing back what you read doubles it (W-25). Where a default is deliberately empty, send
`defaultValueType` with no `defaultValue`.

⚠️ **A domain-type change is not session-scoped.** Creating, updating or deleting a type applies immediately and leaves no open work session to publish or reject — unlike a decision table or a domain *element*. There is nothing to roll back through the session mechanism, so get the shape right before writing.

### A Question May Be Composed of Fields, and That Is a Column Too

A catalogue row that names a parent (`partOf`) is not a question but a **field of one**. The parent carries `KIND = GROUP` and no `CONTEXT_PATH`, because no answer ever lands under its code. Rules that follow from that, and that must hold together or not at all:

- progress counters count parents, never fields
- a reveal chain runs between parents
- a parent is settled when every **required** field is answered; when no field is required, when **any** is — without the second branch, a question made only of optional fields dead-ends the survey
- application code keeps the question list **flat** and nests only at the wire boundary, so answer lookup by code stays ignorant of parentage

**A field may be conditional on an ANSWER, and that is one more column — not an `if`.** The reveal
chain (`revealedBy`) fires on **settlement**, which counts a refusal and a skipped optional question
on purpose; it therefore knows nothing about *what* was answered, and a question that should appear
only after "yes" is not expressible with it. The second column is `revealedWhen`: the answer that
reveals, empty meaning *any*. Java resolves the reference and compares strings; the decision table
gains one boolean input and decides what follows — the same division of labour as `revealedBy`,
where the configuration says which question and the rule says what that implies.

```
hasSource ; answered ; refused ; required ; matches ; revealed
true      ; *        ; *       ; *        ; false   ; false     ← the whole conditional mechanism
```

⚠️ Empty must mean "any answer", or adding the column changes the behaviour of every existing link
instead of the one question being configured.

⚠️ **An answer to a question the client cannot see must not reach the rules.** A conditional
question can be revealed, answered, and then hidden again when its source answer changes. Keep the
answer — the presenter comes back to it — but filter it out of every list the rules walk, or the
recommendation keeps resting on a question that is no longer on screen. ⛔ The filter belongs to the
profile that computes `revealed`, **not** to the shared APK layer: the other demos never set the
flag, so a shared filter would discard all of their answers.

⚠️ A value condition on a **compound** question has nothing to bind to — the parent holds no answer
— so it leaves the dependent question hidden. Fail visibly; silently ignoring the condition would
leave configuration on screen that nothing enforces.

⛔ This is not screen grouping, which §31 reversed. `partOf` says "this field belongs to that question" — a statement about the subject matter. A `SCREEN` or `GROUP` column saying "these questions appear together" remains forbidden, because it would put the application's layout back into the configuration.

### The Context Carries Facts; Derivations Belong to the Rules

⛔ **If a value can be derived from what the context already holds, the application must not push it
in.** A decision table's level `source` may be a function (`$f`), so the derivation lives where the
rule lives and the application stops carrying a second copy of it.

The property demo went from fifteen pushed values to nine on 2026-09-09. What went:

- five paths **nobody read at all** — three of them backed by model fields that had no setter, so the
  application was pushing `null` into the engine on every call;
- the building age, whose whole chain (engine call → model field → DTO → frontend model) collapsed
  once `factor.buildingAge` took `$f demo.property.util.buildingAge` as its level source;
- the two-digit postcode area, an intermediate that existed only to key two tables — the tables now
  key on the **full postcode**, which is the client's own answer.

⚠️ **Search the application as well as the rules before calling a context path dead.** The
application both writes and reads its own context: `quote.sums.suggestedBuilding` had no reader among
the rules and one in Java, and removing it silently added a sentence to the client's justification.

⚠️ **Keeping a derived value is legitimate when the derivation is expensive or its inputs are not
available at the point of reading.** The property quote still pushes the risk factor (a fourteen-node
flow that would otherwise run twenty-one times per quote) and the flood zone (read from narrow
contexts that would each have to carry three inputs instead of one result). Those are memoisation
points, and a comment says so.

### Reach for a Decision Table First, a Flow Second, a Function Last
The order is fixed and it is about who can read the rule, not only about speed. A **decision table**
is the most efficient thing the engine has and the only artifact a business person can change
unaided. A **flow** comes next: slower, but it draws the decision as a diagram they can follow. A
**Groovy function** comes last — it is code, and code is where a business reader stops. Functions
earn their place for arithmetic, for date maths, for set operations, and for small utilities that
several rules reuse. They do not earn it for branching.

⛔ **A chain of `if`s returning a value is a decision table you have not written yet.** That includes
the null checks: a level's `source` may be a function (`$f code`) or a util function (`$u code`), so
"is this value missing" becomes an input column and "what to answer when it is" becomes a row. The
function shrinks to the one line that computes the number; the table names what the number means.

```
# level source is a function, not a context path
source = "$f demo.property.util.choiceDelta"

delta ; kind
* - -1 ; POORER
0 - 0  ; MATCHES
1 - *  ; RICHER
```

⚠️ **An unbounded range bound also matches `null`, and it beats the wildcard row.** A row `* - 1`
matches a null input and wins over a plain `*` row, so a "nothing to compare here" default written
as `*` is silently unreachable. Verified 8 September 2026: a missing budget came back as
`BUDGET_FIT`. Either bound the ranges on both sides, or have the function pass an impossible
sentinel value (`0` for a ratio) that a bounded row can catch.

*Evidence:* `demo.property.offer.choiceKind` and `demo.property.apk.budget` replaced two Groovy
functions on 8 September 2026; `demo.motor.risk.gender.mapper` has been a level source since long
before that.

### `higson_update_function` Overwrites Every Field You Omit

⛔ **The call replaces the function, it does not patch it.** Any field left out of the request is
cleared, even though the schema marks it optional. Confirmed on two fields the same day:

| Omitted field | What happens |
|---|---|
| `arguments` | the function resets to a single `ctx` argument — the engine then refuses the call |
| `description` | the description is emptied, silently; nothing fails, the knowledge is just gone |

**Always read first (`higson_get_function`) and echo back `arguments` and `description` with every
`body` update.** The `description` case is the nastier of the two because there is no error: six
functions lost their descriptions in one pass on 2026-09-09 and it was noticed only by re-reading
them afterwards.

### Updating a Function Without `arguments` Wipes Its Extra Arguments

⛔ **`higson_update_function` documents `arguments` as optional, and omitting it silently resets the
function to a single `ctx` argument.** The body updates correctly, so nothing looks wrong until the
engine refuses the call:

```
InvalidFunctionArgumentsNumberException:
Invalid arguments number. Expected: 1, Passed: 2 for function 'demo.property.cover.premium'
```

Verified 2026-09-09: editing `demo.property.cover.premium` (which takes `ctx` **and** `coverCode`)
without passing `arguments` took the whole property quote down with HTTP 500 — `calculateQuote`
passes two arguments to a function that now declares one. Always re-send the full `arguments` list,
read from `higson_get_function` first, for any function whose signature is not just `ctx`.

⚠️ There is no partial degradation to warn you: `Property.calculate()` has no `catch` and
`GlobalExceptionHandler` covers only the motor exceptions, so a signature mismatch returns a bare
500 for the entire quote rather than a missing field.

### Groovy Function Header Contract
Every Groovy function opens with the generated TOML header block containing `#Full code: <code>` matching the file path, `categories = []`, `logLevel`, and an `[[arguments]]` list whose first entry is always `ctx` of type `SPECIFIED_CLASS` bound to a `ParamContext`. Do not hand-edit the header beyond description and extra arguments. Bodies end with an explicit `return`.

*Evidence:* 36 of 36 `.groovy` files carry the header and match `#Full code` to their path; 36 of 36 declare `categories = []` and `ctx` first; 34 of 36 end with an explicit return.

### How a Rule Function Is Written

A rule body is read on a projector by people who do not write Groovy. That audience, not the
compiler, sets the bar. Five rules, in force from 2026-09-09:

**1. Name every intermediate value.** Never pass one call into another call's argument list. The
variable name is the documentation — `answerCtx`, `fixturesRatio`, `premium` each say what the value
*is*, which the nested form leaves the reader to infer from position.

**2. No comments narrating history.** ⛔ A function must not carry "until 9 September this was…",
"changed because…", or a record of what a previous version got wrong. That belongs in
`.maister/docs/project/*-profile.md`, which is where anyone looking for the reasoning will go. In
source it obscures the rule the reader came to read, and it is already forbidden by
`global/commenting.md`.

**3. A comment survives only when it names a trap that a good name cannot.** One line, not a
paragraph, and only where the obvious edit would reintroduce a known defect — for example that
`demo.util.multiply` treats a missing value as one, so a factor table without a wildcard row is
skipped rather than zeroed. Everything else is deleted; if the code needs prose to be followed, the
names are wrong.

**4. Guard only what can actually happen.** ⛔ A null check on a table with `nullable = false` and a
wildcard row is unreachable code pretending to be error handling. Check the table before writing the
guard. Where a guard is real, prefer failing loudly over substituting a neutral value: a zero sum
prices the whole quote at zero and presents it as correct.

**5. Return a plain map, not `type.paramValue()`** — unless the wire shape matters; see the note
below.

*Evidence:* three property functions were rewritten to this shape on 2026-09-09 (`sum.suggest`
25 lines → 12 of code, `apk.needs`, `tariff.coverPremiums`), verified against a captured baseline
with zero behavioural difference.

### Name Every Intermediate Value; Never Nest One Call Inside Another's Arguments

A rule body is read by people who do not write Groovy. Passing a call straight into another call's
argument list saves a line and costs the reader the whole expression:

```groovy
// ⛔ nie
def row = higson.getValue('demo.property.apk.answer', higson.createContext(
        'question.code', pair.substring(0, sep), 'answer.value', pair.substring(sep + 1)))

// ⚙️ tak
def questionCode = pair.substring(0, sep)
def answerValue = pair.substring(sep + 1)
def answerCtx = higson.createContext('question.code', questionCode, 'answer.value', answerValue)
def row = higson.getValue('demo.property.apk.answer', answerCtx)
```

The variable name is the documentation: `answerCtx`, `fixturesRatio`, `premium` each say what the
value *is*, which the nested form leaves the reader to infer from position. This costs nothing at
runtime and is the difference between a rule a business analyst can follow on a projector and one
they cannot.

⚠️ Same for arithmetic assembled inline into a returned structure — compute `fixtures` and
`contents` into their own variables, then build the map from names.

### Prefer a Plain Map Over `type.paramValue()` — But Know What You Lose

A Groovy function returning several values can return a plain map; Java reads it as a `Map<?, ?>`
and `asDecimal` handles the values. That is roughly half the lines of `type.paramValue()` with its
parallel `withCodes` / `withTypes` / `addValues` lists.

⚠️ **The one thing `withTypes` actually buys is the type declaration.** Without it numbers come back
as `Double`, so a sum of `528000` serialises as `528000.0`. Coercing inside the map (`as BigDecimal`)
does **not** survive — verified 2026-09-09. The values are numerically identical and formatting hides
it on screen, but if the raw wire shape matters, keep `paramValue` for that function.

### Rule Bodies Read Only ctx and the higson Helpers
Groovy rule bodies never import or reference application Java classes. Read inputs from the context with typed accessors (`ctx.getNumber/getString/getInteger`), invoke other rules through the `higson` helper, and round arithmetic through the `math` helper. This keeps pricing logic fully inside Higson and independent of the Java model.

```groovy
def factor = higson.getNumber('demo.motor.coverage.bi.tariff', ctx)
def limit1 = ctx.getNumber('coverage.limit1')
return math.round((100000 + limit1) / 1000 * factor, 2)
```

*Evidence:* 0 of 36 Groovy functions contain an import; 0 of 36 reference `pl.decerto.higson.demo` classes.

### Flows Use Only DECISION_TABLE, FUNCTION and FLOW — Never EXPRESSION

⛔ **`EXPRESSION` is forbidden.** The MCP `create_flow`/`update_flow` description still lists
`FUNCTION|DECISION_TABLE|FLOW|DOMAIN|EXPRESSION` as action types, but that documentation is stale:
newer Higson releases corrected it and removed the capability. Do not reach for it, and do not
propose it as a way to shorten a flow — a flow that relied on it would be built on something the
platform is retiring.

**A flow cannot do arithmetic**, and that is the constraint to design around, not to fight. It
fetches values into variables and branches on conditions; anything computed goes into a function.
For simple maths, small utilities keep the flow readable rather than pushing the whole calculation
back into one Groovy blob — `demo.util.multiply` and `demo.util.round4` exist for exactly that.

⚠️ `multiply` treats a **missing value as one, not zero**. Factor tables without a wildcard row
(`factor.buildingAge`, `factor.claims`) legitimately return nothing, and the neutral element lets
the chain skip them without a branch per factor.

⚠️ **A `FLOW` action returns a `FlowResult`, not a scalar.** Assigning it to a numeric variable
fails later with `conversion not supported for: class io.higson.runtime.flow.FlowResult`. A flow
that needs one number from another flow should take it from the context, where whoever ran that
flow published it — which usually reveals that the value was being computed more times than it
had to be.

### Known Drift: Legacy ParamContext Binding
New and touched functions should bind `ctx` to `io.higson.runtime.engine.core.context.ParamContext`. 29 of 36 existing functions still bind the legacy `org.smartparam.engine.core.context.ParamContext`. Migrate opportunistically when editing a function; do not open a dedicated sweep.

*Evidence:* 29 legacy vs 7 current across the 36 `.groovy` files in the snapshot.
