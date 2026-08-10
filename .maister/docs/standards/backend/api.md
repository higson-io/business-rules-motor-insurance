## API Design

Project-specific standards discovered from this codebase come first and take precedence over the generic baseline guidance further down.

### REST Controllers Are Named *Api, Never *Controller
Spring REST controllers use the `*Api` suffix and live in the `api` package. No class named `*Controller` exists anywhere in the codebase.

*Evidence:* 5 of 5 `@RestController` classes (QuoteApi, DriverApi, VehicleApi, DictionaryApi, ProfilersApi); 0 classes named `*Controller`.

### Class-Level @RequestMapping Root, Verb-Specific Method Mappings
Each controller declares a class-level `@RequestMapping` with the resource root, then uses only verb-specific method annotations (`@GetMapping` / `@PutMapping`). Method-level `@RequestMapping` is not used.

*Evidence:* 5 of 5 controllers declare a class-level root (/quote, /driver, /vehicle, /dictionaries, /profilers); 0 method-level `@RequestMapping` across 25 endpoints.

### Every Mutation Is a PUT of a Single Field
State changes are `PUT` to a camelCase sub-path carrying `@RequestBody`; optional scalars use `@RequestBody(required = false)`. POST, PATCH and DELETE are not used at all. Reads are GET with `@RequestParam` for filters. This suits a session-scoped quote that is edited field by field — follow it rather than introducing REST-canonical POST semantics.

```java
@RestController
@RequestMapping("/driver")
public class DriverApi {

	@PutMapping("/address/zipCode")
	public void updateZipCode(@RequestBody(required = false) String zipCode) {
		driverService.updateZipCode(zipCode);
	}
}
```

*Evidence:* 25 endpoints = 11 `@GetMapping` + 14 `@PutMapping`, 0 POST/PATCH/DELETE; multi-word segments camelCase in 14 of 14 cases (/birthDate, /trafficTicketsCount, /productionYear).

### DTOs Cross the API Boundary, Never Domain Objects
Controller return types are always DTOs from `api/dto` (or `List<...Dto>`). Domain objects never leave the api layer; conversion goes through an injected `*Converter` bean rather than inline mapping.

*Evidence:* 4 of 5 controllers return DTOs (ProfilersApi returns pre-rendered diagnostics HTML); 4 of 4 delegate to an injected converter. Note: `VehicleApi` currently mutates domain `Vehicle` objects directly, bypassing its service — a known layering leak, not a pattern to copy.

---

*Generic baseline guidance below. Where it conflicts with the project-specific standards above, the standards above win.*

### RESTful Principles
Use resource-based URLs with appropriate HTTP methods (GET, POST, PUT, PATCH, DELETE).

### Consistent Naming
Use lowercase, hyphenated or underscored names consistently across endpoints.

### Versioning
Implement versioning (URL path or headers) to manage breaking changes.

### Plural Nouns
Use plural nouns for resources (`/users`, `/products`).

### Limited Nesting
Keep URL nesting to 2-3 levels maximum for readability.

### Query Parameters
Use query parameters for filtering, sorting, and pagination.

### Proper Status Codes
Return appropriate HTTP status codes (200, 201, 400, 404, 500).

### Rate Limit Headers
Include rate limit information in response headers.
