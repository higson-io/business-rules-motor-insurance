## Database Migrations

Project-specific standards discovered from this codebase come first and take precedence over the generic baseline guidance further down.

### There Are No Database Migrations — By Design
This project has no Flyway, no Liquibase and no SQL migration directory, and none should be added. The schema belongs to Higson Studio/runtime, which creates and versions it. Business logic is versioned as a rules-snapshot diff instead. See `.maister/docs/standards/higson/snapshot.md`.

*Evidence:* no migration tooling in pom.xml; no `db/migration` or equivalent directory.

---

*Generic baseline guidance below. It does not apply to this project as it stands — keep it only as reference should application-owned migrations ever be introduced.*

### Reversible
Always implement rollback methods for safe migration reversals.

### Small and Focused
Keep each migration to a single logical change.

### Zero-Downtime Awareness
Consider deployment order and backward compatibility for high-availability systems.

### Separate Schema and Data
Keep schema changes separate from data migrations for safer rollbacks.

### Careful Indexing
Create indexes on large tables carefully, using concurrent options when available.

### Descriptive Names
Use names that indicate what the migration does.

### Version Control
Commit migrations; never modify existing ones after deployment.
