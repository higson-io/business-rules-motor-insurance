## Database Queries

Project-specific standards discovered from this codebase come first and take precedence over the generic baseline guidance further down.

### No ORM and No Application-Owned Schema
The application defines no JPA entities, no repositories and no SQL. All data access goes through the Higson runtime over JDBC, because the database schema is owned entirely by Higson. Do not add Spring Data, JPA, or hand-written DAOs — if you need data, it comes from a rule.

*Evidence:* no ORM on the classpath; 0 entity or repository classes across 47 Java files; HikariCP present only because the Higson starter uses it.

### Dictionary Lookups Go Through the Higson Engine
Reference data (vehicle makes, models, types) is served by Higson parameters queried through the engine with a constant code plus a `HigsonContext`, then mapped to DTOs by a row-mapper method reference. There is no separate lookup table or cache in the application.

*Evidence:* 4 of 4 `DictionaryService` lookups follow this shape.

---

*Generic baseline guidance below. Where it conflicts with the project-specific standards above, the standards above win — this project writes no SQL of its own, so the items below apply only if that ever changes.*

### Parameterized Queries
Always use parameterized queries or ORM methods; never interpolate user input into SQL.

### Avoid N+1
Use eager loading or joins to fetch related data in one query.

### Select Only Needed Columns
Request only the columns you need rather than SELECT *.

### Index Strategic Columns
Index columns used in WHERE, JOIN, and ORDER BY clauses.

### Transactions
Wrap related operations in transactions to maintain consistency.

### Query Timeouts
Set timeouts to prevent runaway queries from impacting performance.

### Cache Expensive Queries
Cache results of complex or frequent queries when appropriate.
