## Spring Framework Conventions

How this project wires Spring beans, scopes and configuration. These standards were discovered from this codebase and describe the wiring style every new class is expected to follow.

### Constructor Injection With private final Fields
Dependencies are injected through a single explicitly `@Autowired` constructor and stored in `private final` fields. Field and setter injection are never used.

```java
private final DriverService driverService;
private final DriverConverter driverConverter;

@Autowired
public DriverApi(DriverService driverService, DriverConverter driverConverter) {
	this.driverService = driverService;
	this.driverConverter = driverConverter;
}
```

*Evidence (2026-09-08):* every bean with dependencies uses constructor injection; the 9 `@Autowired` occurrences across 107 files are all on constructors, none on fields.

### Stereotype Annotation Matches the Layer
`@RestController` on api classes, `@Service` on service classes, `@Component` on converters and `example/` collaborators. Adapters and context classes are deliberately NOT Spring beans — they are instantiated with `new` because they wrap a specific domain instance. Use `@SessionScope` only where per-user state is genuinely required.

*Evidence (2026-09-08):* 8 `@RestController`, 6 `@Service`, 13 `@Component`; no adapter and no context class is a bean; **3 `@SessionScope`** — one quote service per demo (`QuoteService`, `LifeQuoteService`, `PropertyQuoteService`), and they are the only classes holding per-user state.

### Prefer Starter Auto-Configuration Over Manual Bean Wiring
Let the Higson starter auto-configure engine, data source and dialect from `higson.*` properties. Hand-write beans only for behaviour the starter does not cover, and record in a class-level Javadoc why the manual wiring is still required.

*Evidence:* pom.xml comment "auto-configures HigsonEngine, DataSource, dialect"; HigsonIntegrationConfiguration Javadoc documents the one exception.

### Two-Tier External Property Override
Ship defaults as a classpath properties file and let a same-named file under `${user.home}/conf/` override it, with `ignoreResourceNotFound = true` so the app starts without the external file. Last match wins. Deployment-specific values go in the external file — never by editing the packaged one.

```java
@PropertySource(value = {
	"classpath:higson-demo-app.properties",
	"file:${user.home}/conf/higson-demo-app.properties"
}, ignoreResourceNotFound = true)
```

*Evidence:* HigsonIntegrationConfiguration; Readme.md documents the same order; Dockerfile supplies the override layer at `/root/conf/`.

### Fail Fast With Custom Runtime Exceptions, Handle Centrally
Validation failures throw a domain-specific unchecked exception (extends `RuntimeException`, single String-message constructor) raised from a dedicated validator component. HTTP translation happens once, in a `@RestControllerAdvice` pairing `@ExceptionHandler` with `@ResponseStatus` and returning an `ErrorDto` body.

⛔ **`@RestControllerAdvice`, never plain `@ControllerAdvice` returning a `String`** — that was defect DR-14: Spring MVC read the returned message as a **view name**, dispatched a forward, and the failed view resolution produced a 404 that overrode the `@ResponseStatus`. The client got an empty error banner. The class Javadoc records this.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(InvalidCarDataException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ErrorDto handleInvalidCarDataException(InvalidCarDataException e) {
		return new ErrorDto(e.getMessage());
	}
}
```

*Evidence (2026-09-08):* 2 of 2 custom exceptions follow the shape; 1 `@RestControllerAdvice`; validation isolated in `QuoteValidator`.

⚠️ **Four `catch` blocks exist** — `Motor.java:243` and three in `Life.java` — and they are the documented exception, not drift: a decision table declared `matchRequired` throws when no row matches, and for an availability lookup "no row" simply means *this variant does not contain that cover*. Catching there turns a rules-engine convention into a boolean. Each carries a comment saying so. Anywhere else, let it fail.
