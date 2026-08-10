# Business Rules for Motor Insurance / demo app

This is a sample application to demonstrate capabilities of Higson.io library (Java [Business Rules Engine](https://www.higson.io/usecase/business-rules-management-system) (BRE)/Java Pricing Engine). 

The application demonstrates a typical sales path of insurance products, in this particular case Car/Motor insurance. The first step (left panel in this example) customer or agent provides all data required to calculate quotations. Each change in the input data on the left panel (eg. Drivers age/Drivers Sex/Number of accidents in the last 5 years) triggers recalculations for all coverages in 3 pricing options (Bronze/Silver/Gold). There is no need for the "Re-Calculate" button as calculations are made instantly which significantly improves UX for the final customer.

The right panel is completely configured in Higson, all the rules dictating what to offer in that case and for what price. 
It's done in different ways to show all capabilities of Higson. 

You can see an example of making a decision based on parameters (decision tables), simple ones, and also forwarding decision to the next decision table - because this rule engine allows you to stack decision tables onto each other. 
There are also examples of making a decision based on functions.  The Higson allows you to implement functions in Rhino (JavaScript) and Groovy languages. 
Although everything in the right panel needs to be calculated after a change in the left panel, it works smoothly because of the very high performance of Higson. 
All the configuration is clear and easy to understand because it is organized in the business domain. Each visible element is modeled as a business object with attributes. These attributes are rules. The sample application calls them and get's a ready decision about what to show in that particular case. After downloading this demo you should start with browsing "Domain configuration" in Higson and all the configuration should be clear for you within minutes. 

There is a second similar demo of Higson built around vehicle insurance subject link it differs with data storage implementation. It uses Higson Persistance - dynamic data model. It's another layer of flexibility you can add to your Java application but it is completely optional as you can see in this example and you can use Higson without higson persistance. 

If want to learn more about how to use Higson, check the [Higson documentation](https://www.higson.io/docs/start-documentation).

## Prerequisites

Make sure you have at least:

### Java 17

### Maven 

To install go to:

https://maven.apache.org/download.cgi

Previous Maven versions might work as well but this was not checked. 

#### NodeJS — version matters

The frontend is an Angular 22 application, which accepts only these Node versions:

```
^22.22.3 || ^24.15.0 || >=26.0.0
```

Note that Node 23.x is **newer** than 22.22.3 and is still rejected — Angular supports even-numbered (LTS) majors only.

To install go to:
1. [Windows or Mac OS](https://nodejs.org/en/download/)
2. [Linux](https://github.com/nodesource/distributions)

Check what you have before building:

```shell
node -v
```

⚠️ **If the version is out of range, `mvn package` fails inside the `prepare-package` phase and the error does not mention Node.** The build stops at `exec-maven-plugin` with a non-zero exit from `npm ci`; the actual `engines` rejection is buried further up the output. Nothing in this repository installs or pins a Node version for you — there is no `.nvmrc` and no `frontend-maven-plugin` — so getting the right one onto your PATH is a manual, local step.

If Node is installed side-by-side (for example Homebrew's keg-only `node@22`), prepend it to your PATH for the session before building:

```shell
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"   # macOS / Homebrew example
```

#### Higson Studio 4.0.2
1. Go to:  https://www.higson.io/docs/download
2. download zip, unpack it to the directory of your choice and run it as described [here](https://www.higson.io/docs/start-documentation). 

## Project layout

| Path | Contents |
|---|---|
| `src/main/java/` | Spring Boot backend — REST API, domain model, Higson adapters |
| `src/main/higson/` | Higson rules snapshot. **Not tracked in git** — run `mvn higson-cli:pull` after a fresh clone |
| `frontend/` | Angular 22 single-page application (see `frontend/README.md`) |
| `e2e/` | Playwright behavioural test harness |

## Setup
Make sure that both commands ```mvn``` and ```npm``` are accessible through system path. If not, add them.
In file ```higson-demo-app.properties``` set ```higson.database.url``` to point Higson Studio H2 database file, e.g.:
```properties
higson.database.url=jdbc:h2:/higson-studio/database/higson.demo;NON_KEYWORDS=VALUE
```
or on Windows
```properties
higson.database.url=jdbc:h2:c:/higson-studio/database/higson.demo;NON_KEYWORDS=VALUE
```

## Building and running

Execute below maven command to run Spring Boot.
```shell
mvn clean package
mvn spring-boot:run
```
Application will be accessible on port 48080. If you need to use other port change it in `higson-demo-app.properties` -> `server.port`.
URL: [http://localhost:48080/demo](http://localhost:48080/demo)

A plain `mvn clean package` builds the frontend too: `exec-maven-plugin` runs `npm ci && ng build` in `frontend/` during the `prepare-package` phase, and `maven-war-plugin` copies the result into the WAR. There is no separate frontend build step to remember, and no generated asset is committed. `mvn clean` removes `frontend/node_modules/`, `frontend/dist/` and the Angular CLI cache, so the first build after a clean re-downloads npm dependencies and takes noticeably longer.

The application is served under the `/demo` context path by default. The bundle ships **no HTML base element**, so the same WAR also works unchanged at the server root or under any other context path — the published container image serves it at `/app`.

### Frontend development server

For frontend work, run the Angular dev server with hot reload against a locally running backend:

```shell
# terminal 1 — backend at the ROOT context, so JSESSIONID gets Path=/ and needs no rewriting
SERVER_SERVLET_CONTEXT_PATH=/ mvn spring-boot:run

# terminal 2
cd frontend
npm start          # http://localhost:4200/
```

See `frontend/README.md` for details, including the cookie-path caveat if you run the backend under `/demo` instead.

### Tests

```shell
mvn test                                 # Java: MockMvc contract tests + Higson snapshot test
cd frontend && npx ng test --watch=false # Angular unit tests (Vitest)
```

Frontend tests are deliberately **not** part of `mvn package`. End-to-end Playwright flows live in `e2e/` and require a running application — see `e2e/README.md`.

## Running with Docker
Assuming you have docker and docker compose installed, motor-demo docker image is available on a [dockerhub](https://hub.docker.com/r/decerto/higson-motor-insurance-demo) as `decerto/higson-motor-insurance-demo:latest`.   
However, if you want to build this app on your own, we've provided a necessary [Dockerfile](./Dockerfile).
Before building the docker image, you need to build the app using the command:
```shell
mvn clean package -Pall_in_one
```
Then, to build a docker image execute the code below:
```shell
docker build -t decerto/higson-motor-insurance-demo .
```

If image is build, then application can be run in docker container like:
```shell
docker run -p 48080:48080 \ 
    -e higson.database.url=<jdbc_url_to_running_db> \
    -e higson.database.dialect=<choose> \
    -e higson.database.username=<db_username> \
    -e higson.database.password=<db_password> \
    -e higson.studio.instance-name=higson_docker \
    decerto/higson-motor-insurance-demo
```
## Running with Docker compose
Application can be run with bundle-h2-demo and higson-studio images using docker-compose based on [docker-compose.yml](./docker-compose.
yml)
```shell
docker-compose up   
# or (regarding to docker version)
docker compose up
```
* By default Higson Studio will be available at: [host]:38080/higson/app
* By default Demo application will be available at: [host]:48080/demo
* By default Runtime REST will be available at: [host]:8081/api ([host]:8081/swagger-ui.html)

## Executable war
Enable maven profile "all_in_one" to build fully executable war archive, eg.
```shell
mvn clean package -Pall_in_one
```
The resulting war archive can be executed from shell:
```shell
./target/motor-insurance.war
```
The artifact name is version-free by design (`<finalName>motor-insurance</finalName>`), so scripts and the `Dockerfile` reference the same path across releases.
For more details about Spring Boot executables refer to 
[https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html)

## Configuration files locations 
Configuration files are searched for properties in following order (last matching overrides):
1. classpath:higson-demo-app.properties 
2. file:${user.home}/conf/higson-demo-app.properties

# Feedback
If you have any feedback regarding this App or Higson.io library do not hesitate to contact: [higson@decerto.com](mailto:higson@decerto.com).
