## Models

Project-specific standards discovered from this codebase come first and take precedence over the generic baseline guidance further down.

### Mutable POJOs — No Records, Builders or Lombok
Domain objects and DTOs are plain mutable classes: private fields, public getters, void setters, a no-arg or field constructor. Java records, Lombok and builder classes are not used — the code is reference material for Higson customers and stays deliberately explicit. Express immutability with `private final` fields and getter-only access where intended.

*Evidence (2026-09-08):* every domain and DTO class is a plain class; 0 records, 0 builders, 0 Lombok across all 107 Java files; setters are void, never fluent.

### Spring Converter<S,T> for Domain-to-DTO Mapping
Every mapper implements `org.springframework.core.convert.converter.Converter<Domain, Dto>`, is annotated `@Component`, guards its input with `Objects.requireNonNull` carrying a human-readable message, and builds the DTO with `new` plus setters. Nested collections map through private `toXxxDto` helpers using streams. Mapping frameworks (MapStruct, ModelMapper) are deliberately avoided so the data flow stays readable.

```java
@Component
public class VehicleConverter implements Converter<Vehicle, VehicleDto> {
	@Override
	public VehicleDto convert(Vehicle vehicle) {
		Objects.requireNonNull(vehicle, "Vehicle can not be null.");
		VehicleDto dto = new VehicleDto();
		dto.setMake(vehicle.getMake());
		return dto;
	}
}
```

*Evidence:* 4 of 4 converters implement `Converter<Domain, Dto>` and are `@Component`; 4 of 4 open with `Objects.requireNonNull`; 3 of 4 map collections with streams.

### toString via Commons ToStringBuilder
Classes that override `toString` build it with `org.apache.commons.lang3.builder.ToStringBuilder`, appending one named field per line.

*Evidence:* 10 of 13 classes overriding toString use it; the 3 outliers use a manual StringBuilder. Confidence 70 — dominant but not universal.

---

*Generic baseline guidance below. Where it conflicts with the project-specific standards above, the standards above win. Note that this project owns no database schema (see `queries.md` and `migrations.md`), so the persistence-oriented items are largely informational here.*

### Clear Naming
Use singular names for models and plural for tables (or follow framework conventions).

### Timestamps
Include created and updated timestamps for auditing and debugging.

### Database Constraints
Enforce data rules at the database level (NOT NULL, UNIQUE, foreign keys).

### Appropriate Types
Choose data types that match purpose and size requirements.

### Index Foreign Keys
Index foreign key columns and frequently queried fields.

### Multi-Layer Validation
Validate at both model and database levels for defense in depth.

### Clear Relationships
Define relationships with appropriate cascade behaviors and naming.

### Practical Normalization
Balance normalization with query performance needs.
