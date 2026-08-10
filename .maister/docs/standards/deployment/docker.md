# Container Image & Compose

The application ships as a Docker image. These conventions keep the image reproducible — pinned bases, explicit ports, no hidden defaults — and keep the compose stack self-contained so the whole demo environment starts from a single `docker compose up`.

### Minimal JRE Base Image With Runtime-Overridable JVM Options

The application image starts from a slim, pinned JRE base (`eclipse-temurin:17-jre-jammy` — JRE, not JDK), declares a maintainer `LABEL`, `EXPOSE`s its port explicitly, and passes JVM tuning through an empty-by-default `JAVA_OPTS` env var so operators can override memory and GC settings at run time without rebuilding.

```bash
docker run -e JAVA_OPTS="-Xmx512m" decerto/higson-motor-insurance-demo:latest
```

*Evidence:* Dockerfile `FROM eclipse-temurin:17-jre-jammy`; `ENV JAVA_OPTS=""` with the comment "Extra JVM options, overridable at runtime"; `EXPOSE 48080`; `CMD ["sh", "-c", "java $JAVA_OPTS -jar motor-insurance.war"]`.

### Multi-Arch Publishing With Version and Latest Tags

Images are built and pushed with `docker buildx` for both `linux/amd64` and `linux/arm64` in one command, tagged with the explicit release version and with `latest`. Registry credentials are passed as script arguments, never hardcoded.

```bash
./push-to-docker.sh <dockerhub-user> <dockerhub-token>
```

*Evidence:* push-to-docker.sh: `docker buildx build --platform linux/amd64,linux/arm64 -t ${IMAGE_NAME}:${VERSION} -t ${IMAGE_NAME}:latest --push .`; `USER=$1` / `PASS=$2`. Confidence 75.

### Pinned Service Images on a Dedicated Compose Network

Every third-party service in `docker-compose.yml` uses an explicitly pinned image tag — never `latest` — plus a fixed `container_name`, a `depends_on` for its prerequisites, and membership of the project-specific bridge network `higson-net` rather than the default one. Only the locally built application image may use the `latest` tag.

```yaml
image: decerto/higson-studio:4.0.11   # not :latest
```

*Evidence:* db 4.0.0, studio 4.0.11, runtime-rest 4.0.11 all pinned; `depends_on: [db]` and `networks: [higson-net]` on every dependent service; `higson-net` declared as a bridge driver. Confidence 70.

### Single Canonical HTTP Port (48080)

The application listens on 48080 in every layer — packaged properties, container properties, Dockerfile `EXPOSE` and the compose port mapping. Changing the port means updating all four; do not rely on the framework default.

*Evidence:* `server.port=48080` in both higson-demo-app.properties and docker/app.properties; `EXPOSE 48080`; compose `"48080:48080"`. Confidence 70.
