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

*Evidence:* 9 of 9 beans with dependencies use constructor injection; 0 of 47 files use field-level `@Autowired`.

### Stereotype Annotation Matches the Layer
`@RestController` on api classes, `@Service` on service classes, `@Component` on converters and `example/` collaborators. Adapters and context classes are deliberately NOT Spring beans — they are instantiated with `new` because they wrap a specific domain instance. Use `@SessionScope` only where per-user state is genuinely required.

*Evidence:* 5 `@RestController`, 4 `@Service`, 6 `@Component`; 0 of 6 adapters and 0 of 1 context classes are beans; 1 `@SessionScope`, on QuoteService, the only class holding per-user state.

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
No `try`/`catch` appears anywhere in application code. Validation failures throw a domain-specific unchecked exception (extends `RuntimeException`, single String-message constructor) raised from a dedicated validator component. HTTP translation happens once, in a `@ControllerAdvice` pairing `@ExceptionHandler` with `@ResponseStatus`.

```java
@ControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(InvalidCarDataException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public String handleInvalidCarDataException(InvalidCarDataException e) {
		return e.getMessage();
	}
}
```

*Evidence:* 0 catch blocks across 47 files; 2 of 2 custom exceptions follow the shape; 1 `@ControllerAdvice`; validation isolated in `QuoteValidator` with 11 guard calls.
