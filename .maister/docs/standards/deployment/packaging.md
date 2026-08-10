# Artifact Packaging & Container Config

How the deployable artifact is built and how it receives environment-specific configuration. The build produces a stable, version-free WAR name so downstream scripts never change, and the container layer injects only genuinely environment-dependent values through placeholders.

### WAR Packaging With a Stable finalName

The project builds a WAR under a fixed, version-free `finalName` (`motor-insurance`) so the Dockerfile and scripts can reference `target/motor-insurance.war` unchanged across releases. Do not add the version to the artifact name.

*Evidence:* pom.xml `<packaging>war</packaging>` and `<finalName>motor-insurance</finalName>`; Dockerfile `COPY ./target/motor-insurance.war /app/motor-insurance.war`.

### Executable Packaging Is an Opt-In Profile

Standalone executable packaging is the `all_in_one` Maven profile, which adds `spring-boot-maven-plugin` repackage with `<executable>true</executable>`. It is deliberately not on by default — build with the profile before producing a Docker image.

```bash
mvn clean package -Pall_in_one
```

*Evidence:* pom.xml `all_in_one` profile; Readme.md instructs building with it before `docker build`.

### Container Config via Environment Placeholders With Inline Defaults

The container-layer properties file resolves environment-specific values through `${ENV_VAR}` placeholders, supplying a safe inline default with the `${VAR:default}` form for optional toggles so the container starts even when the variable is unset. Externalize only genuinely environment-dependent values — the database URL and diagnostic switches — not the whole file.

```properties
higson.database.url=${HIGSON_DATABASE_URL}
higson.runtime.diagnostic.profiler.enabled=${HIGSON_RUNTIME_DIAGNOSTIC_PROFILER_ENABLED:false}
```

*Evidence:* docker/app.properties; docker-compose.yml supplies `HIGSON_DATABASE_URL` to the web service. Confidence 75.
