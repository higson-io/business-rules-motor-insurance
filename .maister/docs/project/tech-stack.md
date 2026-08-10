# Stos technologiczny

## Przegląd

Dokument opisuje wybory technologiczne i ich uzasadnienie w projekcie **Business Rules for Motor Insurance** (`pl.decerto:motor-insurance`, wersja `4.3.0-SNAPSHOT`).

Projekt jest **aplikacją demonstracyjną / referencyjną** dla silnika reguł biznesowych **Higson BRMS**. Pokazuje produkcyjny wzorzec integracji Higsona ze Spring Bootem na przykładzie wyceny ubezpieczenia komunikacyjnego (kalkulacja składki, limitów i zniżek w wariantach Bronze / Silver / Gold).

> **Kontekst:** projekt demonstracyjny, nie produkcyjny. Utrzymywany przez małą (1–2 os.) grupę. Brak SLA produkcyjnego — świadome luki w obszarze bezpieczeństwa i observability są akceptowane.

---

## Języki

### Java 17 (główny język backendu)

- **Udział**: ~47 plików źródłowych, ok. 2 400 linii kodu — całość logiki serwerowej
- **Źródło**: `pom.xml` → `<maven.compiler.release>17</maven.compiler.release>`
- **Uzasadnienie**: wersja LTS wymagana przez Spring Boot 3.x (baseline Java 17) oraz przez runtime Higson 4.3.x
- **Wykorzystywane cechy**: standardowe API Java 17; kod celowo prosty i czytelny, bo służy jako materiał referencyjny dla klientów Higsona

### TypeScript ~6.0.3 (frontend)

- **Udział**: 10 komponentów Angulara, 4 serwisy, magazyn stanu i 2 interceptory w `frontend/src/app/`
- **Źródło**: `frontend/package.json` → `"typescript": "~6.0.3"`
- **Uzasadnienie**: zastąpił nietypowany ES5 przy migracji AngularJS → Angular 22 (sierpień 2026)
- **Tryb ścisły**: `strict: true` oraz `strictTemplates: true` — **wpisane ręcznie**, bo Angular CLI 22 ich nie generuje mimo domyślnego `--strict`. Zero wystąpień `any` w kodzie produkcyjnym
- **Przypięcie wersji**: `typescript@latest` to obecnie 7.0.2 i **psuje build** — zakres peer kompilatora Angulara to `>=6.0 <6.1`

### LESS

- **Zastosowanie**: cały styling aplikacji; punkt wejścia `frontend/src/styles.less`, partiale w `frontend/src/styles/`, style komponentowe obok komponentów
- **Kompilacja**: przez Angular CLI w ramach `ng build` — nie ma osobnego kroku budowania arkuszy
- **Jeden wyjątek**: `frontend/src/theme.scss` to jedyny plik Sass w projekcie. API motywów Angular Material (`mat.theme()`) istnieje wyłącznie w Sassie i nie ma odpowiednika w LESS-ie. Wymieniony w `angular.json` jako **pierwszy**, żeby LESS wygrywał kaskadę

---

## Frameworki

### Backend

#### Spring Boot 3.5.16

- **Źródło**: `spring-boot-starter-parent` (parent POM)
- **Moduły**: `spring-boot-starter-web` (REST + wbudowany Tomcat), `spring-context`
- **Uzasadnienie**: autokonfiguracja pozwala na minimalną ilość kodu „klejącego" — cała konfiguracja aplikacji zamyka się w klasie `ApplicationConfig`, dzięki czemu w demo widać wyłącznie logikę integracji z Higsonem
- **Konfiguracja**: `@ComponentScan("pl.decerto.higson.demo.motor")`, `ViewControllerRegistry` przekierowuje `/` na `index.html`

#### Higson BRMS 4.3.1 (silnik reguł biznesowych) — kluczowa technologia projektu

- **Artefakt**: `io.higson:spring-boot-starter-higson-runtime:4.3.1`
- **Rola**: cała logika cenowa (składka, limity, zniżki) żyje w regułach Higsona, nie w kodzie Java. Java odpowiada wyłącznie za przygotowanie kontekstu i odczyt wyników
- **Autokonfiguracja startera**: `HigsonEngine`, `DataSource`, dialekt bazy danych
- **Profilowanie**: `io.higson.runtime.profiler.jdbc.proxy.DataSourceProxy` opakowuje natywny `DataSource` (konfiguracja w `HigsonIntegrationConfiguration`); wyniki dostępne pod `GET /demo/profilers/jdbc`
- **Uzasadnienie**: to jest przedmiot demonstracji — starter reprezentuje zalecany, produkcyjny sposób osadzania Higsona w aplikacji Spring Boot

##### Konfiguracja reguł (snapshot Higson Studio)

Reguły żyją w katalogu `src/main/higson/` (tzw. *snapshot*) — to jedyna kanoniczna lokalizacja snapshotu w drzewie roboczym, adresowana wszędzie przez właściwość Mavena `${higson.snapshot.dir}`. Snapshot **nie jest jednak śledzony w git** i nigdy nie był. Systemem zapisu (*system of record*) dla reguł jest baza danych **Higson Studio**; snapshot jest wyłącznie kopią roboczą, wyciąganą do drzewa i odsyłaną z powrotem na żądanie pluginem `higson-cli` (`mvn higson-cli:pull` / `higson-cli:push`).

> ⚠️ **Uwaga:** świeży klon repozytorium **nie zawiera żadnych reguł** — pojawiają się dopiero po uruchomieniu `mvn higson-cli:pull`. Konsekwencja praktyczna: zmiana w kodzie Javy i odpowiadająca jej zmiana reguły **nigdy nie trafiają do jednego, atomowego commita**. Czytając kod Javy zależny od reguł, trzeba założyć, że pasujące reguły trzyma instancja Studio, a nie repozytorium.

Struktura snapshotu (stan bieżącego drzewa roboczego):

| Katalog | Zawartość | Liczba plików |
|---|---|---|
| `@domains/` | definicje domen taryfowych | 33 |
| `@functions/` | funkcje własne (Groovy / Rhino) | 36 |
| `@params/` | parametry i tablice decyzyjne | 81 |
| `@tests/` | testy reguł | 9 |
| `@profiles/` | profile środowiskowe | 6 |
| `@flows/` | przepływy reguł | 2 |
| `@testPackages/` | paczki testowe | 2 |
| `@tags/` | tagi organizacyjne | 1 |

### Frontend

#### Angular 22.1.0

- **Lokalizacja**: `frontend/` (workspace Angular CLI 22.1.3)
- **Źródło**: `frontend/package.json`
- **Historia**: zastąpił AngularJS 1.8.3 (EOL od 2022 r.) w migracji przeprowadzonej w sierpniu 2026 r. Poprzednia SPA żyła w `src/main/webapp/`
- **Architektura**: komponenty standalone (bez `NgModule`), sygnały jako model reaktywności, `input()` zamiast `@Input()`, `inject()` zamiast wstrzykiwania przez konstruktor, wbudowany control flow `@if`/`@for` z obowiązkowym `track`, `ChangeDetectionStrategy.OnPush` w każdym komponencie
- **Komponenty**: `app-root` (powłoka), `app-sidebar`, `app-vehicle`, `app-driver`, `app-driver-history`, `app-variants`, `app-coverage`, `app-discount`, `app-spinner` oraz dyrektywa `[appHtmlPopover]`
- **Serwisy**: `DictionaryService`, `DriverService`, `VehicleService`, `VariantService` — jedyne miejsca, które wstrzykują `HttpClient`
- **Stan**: `QuoteStore` (`@Injectable({ providedIn: 'root' })`) — dwa `resource()` (wycena, kierowca) plus sygnał błędu mutacji. Zastąpił magistralę zdarzeń `$emit('recalculate')` / `$broadcast('reloadQuote')`
- **Bez routera**: aplikacja to jeden, nieadresowalny ekran (`ApplicationConfig.addViewController("/")` pozostaje wystarczające)
- **Bez elementu `<base>`**: ścieżka kontekstu serwletu jest wartością runtime'ową (`/demo` domyślnie, `/app` w opublikowanym obrazie kontenera), więc jeden WAR działa pod dowolną ścieżką

#### Biblioteki UI

| Biblioteka | Wersja | Rola |
|---|---|---|
| `@angular/material` | 22.1.1 | pełny zestaw kontrolek: pola formularza, selecty, datepicker, karty, przełączniki, spinner |
| `@angular/cdk` | 22.1.1 | Overlay dla własnego popovera z opisem ochrony (HTML z reguł Higsona — `MatTooltip` obsługuje tylko tekst) |
| `@angular/animations` | 22.1.0 | wydzielany do osobnego, leniwego chunka (~67,7 kB) |
| `rxjs` | ~7.8.2 | wyłącznie na granicy `HttpClient`; stan aplikacji trzymają sygnały, nie `BehaviorSubject` |

Bootstrap 3, Font Awesome 4, angular-ui-bootstrap, angular-tooltips, angular-spinner/spin.js i Open Sans zostały **usunięte**, nie zastąpione odpowiednikami — całość UI rysuje Material (decyzja C-9). Manrope jest hostowany lokalnie (dwa pliki woff2, latin i latin-ext, z `unicode-range`) zamiast pobierania z CDN Google Fonts.

### Testy

Wszystkie trzy zestawy powstały razem z migracją frontendu. **Nie ma CI**, więc żaden z nich niczego nie bramkuje — „testy przechodzą" znaczy tylko tyle, że ktoś uruchomił je lokalnie.

| Zestaw | Lokalizacja | Rozmiar | Uruchomienie |
|---|---|---|---|
| Java — kontraktowe MockMvc + silnik ze snapshotu | `src/test/java/` | 6 plików, 11 testów | `mvn test` |
| Angular — jednostkowe (Vitest przez Angular CLI) | `frontend/src/**/*.spec.ts` | 14 plików, 27 testów | `npx ng test --watch=false` |
| Playwright — behawioralne goldeny | `e2e/` | 28 plików, 6 przepływów | `npx playwright test` (wymaga działającej aplikacji) |

- **JUnit 5** przez `spring-boot-starter-test` (wersja z BOM-u Spring Boota). Jawna zależność na JUnit 4 została usunięta
- **Strefa czasowa** przypięta w `maven-surefire-plugin` (`-Duser.timezone=UTC`) — `Motor.createDriver` zasiewa `dateOfBirth` względem strefy systemowej
- **Pokrycie kontraktu**: testy MockMvc pinują 18 z 21 endpointów; trzy PUT-y historii szkodowej pokrywa Playwright
- **Testy jednostkowe frontendu nie są częścią `mvn package`** — `npm run build` to `npm ci && ng build` i nic więcej
- **Testy reguł**: właściwe pokrycie logiki biznesowej nadal realizują testy Higsona w `src/main/higson/@tests` (9 plików), uruchamiane po stronie Studio / runtime — i, jak cały snapshot, **niewidoczne w repozytorium**

---

## Baza danych

### PostgreSQL (konfiguracja domyślna)

- **Typ**: relacyjna
- **Połączenie**: `jdbc:postgresql://localhost:10432/higson?currentSchema=public`
- **Pula połączeń**: HikariCP (wersja zarządzana przez Spring Boot BOM)
- **ORM**: **brak** — dostęp do danych realizuje wyłącznie runtime Higsona przez JDBC. Aplikacja nie ma własnych encji ani repozytoriów
- **Uzasadnienie**: schemat bazy należy do Higsona (repozytorium reguł, parametry, wersjonowanie); aplikacja go nie modyfikuje

### H2 2.1.212 (tryb demo / testy)

- **Typ**: relacyjna, in-memory / plikowa
- **Zastosowanie**: uruchomienie demo bez zewnętrznej bazy oraz `SnapshotDataSourceDemoTest`
- **W `docker-compose.yml`** udostępniona jako osobny serwis (porty 8082, 9092)

---

## Narzędzia budowania i zarządzanie pakietami

### Maven (backend + orkiestracja całości)

- **Artefakt**: `motor-insurance.war` (`<finalName>motor-insurance</finalName>`)
- **Podstawowe polecenie**: `mvn clean package`
- **Profil `all_in_one`**: dołącza `spring-boot-maven-plugin` z `<executable>true</executable>` → samodzielnie uruchamialny WAR
- **Integracja z npm**: `exec-maven-plugin` 3.0.0 uruchamia `npm run build` w fazie `prepare-package`, z `<workingDirectory>${project.basedir}/frontend</workingDirectory>` — build frontendu jest częścią builda Mavena
- **`maven-war-plugin`**: blok `<webResources>` wciąga `frontend/dist/motor-insurance/browser` do korzenia WAR-a. ⚠️ **Wersja celowo pominięta** — `spring-boot-starter-parent` zarządza tym pluginem (3.4.0), a jego `pluginManagement` niesie blok `<archive>`, który jawna wersja by nadpisała
- **`maven-clean-plugin`**: usuwa `frontend/node_modules`, `frontend/dist` i `frontend/.angular`. Filesety **bez `<includes>`** — z nimi plugin czyści zawartość, ale zostawia puste katalogi
- **`maven-compiler-plugin`**: 3.10.1 (przez `pluginManagement`)
- **`maven-surefire-plugin`**: `-Duser.timezone=UTC` (wersja dziedziczona)

### npm + Angular CLI 22.1.3 (frontend)

- **Skrypt builda**: `npm run build` → `npm ci && ng build` (`npm ci`, nigdy `npm install`)
- **Wyjście**: `frontend/dist/motor-insurance/browser/` — hashowane bundle plus katalog `media/` z czcionkami i SVG
- **Bundler**: `@angular/build:application` (esbuild/Vite pod spodem) — pełne bundlowanie modułów ES, w miejsce dawnego kopiowania plików do `vendors/` i ładowania tagami `<script>`
- **`less@^4.2.0`** zadeklarowany jawnie: jest *opcjonalnym* peerem `@angular/build`, który domyślnie dostarcza `sass`, nie `less`
- **Serwer deweloperski**: `npm start` → `ng serve --proxy-config proxy.conf.json` (port 4200, proxy na `localhost:48080`)
- **Wymagany Node**: `^22.22.3 || ^24.15.0 || >=26.0.0`, wymuszany przez `engines` + `engine-strict=true` w `frontend/.npmrc`. ⚠️ **Nic w repozytorium nie instaluje ani nie wybiera wersji Node** (decyzja C-5) — zły Node powoduje niejasną awarię wewnątrz fazy `prepare-package`, bez wzmianki o Node

### Higson CLI Maven Plugin 4.3.1

Synchronizacja snapshotu reguł między drzewem roboczym (`src/main/higson/`) a Higson Studio — nie między git-em a Studio. Plugin nie ma podpiętych `<executions>` — uruchamiany jest na żądanie:

```bash
mvn higson-cli:pull    # Studio -> src/main/higson
mvn higson-cli:push    # src/main/higson -> Studio
mvn higson-cli:push -Dautocommit=true   # z automatycznym commitem w Studio
```

- **Konfiguracja**: `higson.studio.url` (domyślnie `http://localhost:38088/studio`), `higson.snapshot.dir` (`${project.basedir}/src/main/higson`), `higson.studio.token`
- **Log**: `higson-cli.log` w katalogu głównym projektu
- **Domyślne zachowanie**: bezpieczne — nic nie jest usuwane, brak auto-commitu

---

## Infrastruktura

### Konteneryzacja

#### Docker

- **Obraz bazowy**: `eclipse-temurin:17-jre-jammy`
- **Parametryzacja**: `JAVA_OPTS` jako zmienna środowiskowa
- **Skrypt pomocniczy**: `push-to-docker.sh`

#### Docker Compose

Orkiestracja czterech usług:

| Usługa | Rola | Porty |
|---|---|---|
| `db` | baza H2 | 8082, 9092 |
| `studio` | Higson Studio (projektowanie reguł) | 38080 |
| `runtime-rest` | Higson Runtime REST API | 8081 |
| `web` | aplikacja demo | 48080 |

### CI/CD

**Brak skonfigurowanego pipeline'u** w repozytorium. Buildy MR-owe dla samego Higsona działają po stronie Jenkinsa (`ci.decerto.pl`), ale ten projekt nie ma własnej definicji CI.

### Hosting

Uruchamiany lokalnie lub przez Docker Compose. Brak dedykowanego środowiska hostingowego — zgodnie z demonstracyjnym charakterem projektu.

---

## Narzędzia deweloperskie

### Linting i formatowanie

- **Frontend**: `prettier` (devDependency) z `frontend/.prettierrc` i `frontend/.editorconfig` — oba przestawione na tabulatory, zgodnie z `standards/global/coding-style.md`. **Brak ESLinta**; formatowanie nie jest niczym wymuszane
- **Backend**: brak Spotless / Checkstyle. Konwencje utrzymywane ręcznie — patrz `.maister/docs/standards/`

### Sprawdzanie typów

- **Java** — typowana statycznie, `maven.compiler.release=17`
- **Frontend** — TypeScript w trybie ścisłym: `strict: true`, `strictTemplates: true`, `strictInjectionParameters`, `strictInputAccessModifiers`, `noImplicitOverride`, `noPropertyAccessFromIndexSignature`, `noImplicitReturns`, `noFallthroughCasesInSwitch`. Zero `any`

### Profilowanie

- **Higson JDBC Profiler** — `DataSourceProxy`, endpoint `GET /demo/profilers/jdbc`
- **Higson Diagnostic Profiler** — wyłączony domyślnie (`higson.runtime.diagnostic.profiler.enabled=false`)

---

## Kluczowe zależności

| Zależność | Wersja | Rola |
|---|---|---|
| `org.springframework.boot:spring-boot-starter-parent` | 3.5.16 | zarządzanie wersjami + autokonfiguracja |
| `org.springframework.boot:spring-boot-starter-web` | (z BOM) | REST API, wbudowany Tomcat |
| `io.higson:spring-boot-starter-higson-runtime` | 4.3.1 | silnik reguł biznesowych |
| `io.higson:higson-cli-maven-plugin` | 4.3.1 | synchronizacja snapshotu ze Studio |
| `org.springframework:spring-context` | (z BOM) | kontener IoC |
| `com.zaxxer:HikariCP` | (z BOM) | pula połączeń JDBC |
| `com.h2database:h2` | 2.1.212 | baza demo/testowa |
| `org.apache.commons:commons-collections4` | 4.4 | narzędzia kolekcyjne |
| `org.springframework.boot:spring-boot-starter-test` | (z BOM) | JUnit 5, AssertJ, MockMvc |
| `@angular/core` (+ `common`, `compiler`, `forms`, `platform-browser`) | 22.1.0 | framework frontendu |
| `@angular/material` / `@angular/cdk` | 22.1.1 | kontrolki UI i Overlay |
| `@angular/build` / `@angular/cli` | 22.1.3 | build i narzędzia frontendu |
| `typescript` | ~6.0.3 | język frontendu (7.x psuje build) |
| `@playwright/test` | 1.62.1 | harness testów E2E w `e2e/` |

---

## Zarządzanie wersjami

### Wersje bibliotek

- **Java/Spring**: wersje dziedziczone z `spring-boot-starter-parent` 3.5.16 (BOM). Jawnie przypięte tylko tam, gdzie BOM nie zarządza: Higson 4.3.1, H2 2.1.212, commons-collections4 4.4
- **Frontend**: `frontend/package.json` — **dokładne** przypięcia dla wszystkiego, od czego zależy zachowanie aplikacji (każdy `@angular/*`, `rxjs`, `typescript`); zakresy `^` tylko dla peryferyjnego oprzyrządowania (`less`, `prettier`, `jsdom`, `vitest`). `npm ci` w skrypcie builda zapewnia powtarzalność dzięki `frontend/package-lock.json`

### Wersja aplikacji

`4.3.0-SNAPSHOT` — wersja aplikacji celowo śledzi linię wersji Higsona, którą demonstruje. Bieżąca gałąź robocza: `techflash/5.8`.

### Konfiguracja środowiskowa

Dwustopniowa, z nadpisywaniem:

1. `src/main/resources/higson-demo-app.properties` (wartości domyślne z classpath)
2. `${user.home}/conf/higson-demo-app.properties` (nadpisania lokalne, ładowane jeśli plik istnieje)

Kluczowe właściwości:

```properties
higson.database.url=jdbc:postgresql://localhost:10432/higson?currentSchema=public
higson.database.username=higson
higson.database.password=higson
higson.runtime.instance-name=motor-insurance-demo
higson.runtime.dev-mode-enabled=false
higson.runtime.diagnostic.profiler.enabled=false
server.port=48080
server.servlet.context-path=/demo
```

### Sekrety

Właściwość `higson.studio.token` w `pom.xml` jest **pusta** i taka ma pozostać. Token podaje się przy wywołaniu — `-Dhigson.studio.token=...` albo wpisem `<server>` w `settings.xml`. Wcześniej w drzewie roboczym siedział tam wpisany na sztywno JWT z uprawnieniami admina; został usunięty, zanim `pom.xml` po raz pierwszy trafił do historii (commit `d09c201`).

⚠️ Nic tego nie pilnuje — nie ma hooka pre-commit ani skanowania sekretów, a remote jest publiczny. Przed każdym commitem `pom.xml`: `grep -c 'eyJ' pom.xml` musi zwrócić `0`.

---

## Ścieżka migracji

Projekt jest dojrzały (start 2017 r.), a część stosu wymaga modernizacji. Kierunki wynikające z celów projektu:

### Zrobione

1. ✅ **Frontend — odejście od AngularJS 1.8.3 (EOL)** — **zrealizowane w sierpniu 2026 r.**
   SPA przepisana na Angular 22 w `frontend/`; Bootstrap 3, Font Awesome 4 i Gulp usunięte, a nie zastąpione odpowiednikami — całość UI rysuje Angular Material (decyzja C-9). Warstwa REST pozostała **zamrożona**: 21 endpointów, zero dodanych, zero porzuconych. Jedyna zmiana w backendzie to `GlobalExceptionHandler` (`@ControllerAdvice` → `@RestControllerAdvice`), żeby nieudana wycena zwracała `400 {"message": …}` zamiast `404` z komunikatem walidatora potraktowanym jako nazwa widoku.
   Parytet potwierdzony przez odtworzenie goldenów: 206 kluczy porównanych, zero różnic bez uzasadnienia. Szczegóły: `.maister/tasks/migrations/2026-08-06-angularjs-to-angular/`.

2. ✅ **JUnit 4 → JUnit 5** — jawna zależność na JUnit 4 usunięta, dodany `spring-boot-starter-test`.

3. ✅ **Pokrycie testami** — z jednego pliku do trzech zestawów (11 testów Javy, 27 jednostkowych Angulara, 6 przepływów Playwrighta). Nadal **bez CI**.

### Priorytet wysoki

4. **Aktualizacje Higsona**
   Bieżąca gałąź `techflash/5.8` sugeruje przygotowanie pod nowszą linię runtime'u. Przy każdej podbitce: zaktualizować równolegle `spring-boot-starter-higson-runtime` i `higson-cli-maven-plugin` (muszą być zgodne), przepchnąć snapshot (`higson-cli:pull`) i uruchomić testy reguł.

### Priorytet średni

5. **CI** — istnieją trzy zestawy testów i żaden niczego nie bramkuje. Najtańszy sensowny krok: `mvn test` + `ng test --watch=false` na push. Dopiero wtedy warto domykać pozostałe trzy z 21 endpointów w testach kontraktowych i rozważyć `frontend-maven-plugin` (decyzja C-5 świadomie go nie dodała, bo bez CI nic by nie wnosił).
6. **Budżet bundla** — początkowy bundle to 680,86 kB surowo (ok. 143,7 kB transferu) przy progu ostrzegawczym 500 kB. To **ostrzeżenie, nie błąd** (`ng build` kończy się kodem 0; próg błędu to 1 MB). Material + CDK + datepicker to ~95% przekroczenia. Do decyzji: zaakceptować i podnieść `maximumWarning`, albo zejść z rozmiarem. `angular.json` celowo nietknięty, żeby sygnał nie zniknął bez decyzji.
7. **Dokumentacja API** — `springdoc-openapi-starter-webmvc-ui` dałoby interaktywną dokumentację endpointów.

### Priorytet niski

8. **Narzędzia jakości kodu** — Spotless/Checkstyle (backend), ESLint (frontend; Prettier już jest, ale nic go nie wymusza).
9. **Observability** — Spring Boot Actuator, logowanie strukturalne. Ograniczona wartość dla demo lokalnego.
10. **`<html lang="pl">` przy angielskim UI** — `frontend/src/index.html` deklaruje język polski, a cała treść interfejsu jest po angielsku. Zachowane 1:1 z poprzedniej wersji (świadomie poza zakresem migracji); `lang` steruje m.in. doborem podzbioru czcionki i czytnikami ekranu.

---

*Ostatnia aktualizacja*: 2026-08-10 (migracja frontendu AngularJS → Angular 22)
*Auto-wykryte*: język i wersje (`pom.xml`, `frontend/package.json`), frameworki, baza danych, narzędzia budowania, konteneryzacja, struktura snapshotu Higsona, liczba testów, konfiguracja środowiskowa
*Podane przez użytkownika*: charakter projektu (demo referencyjne, nie produkcja), kontekst zespołu (mały zespół), cele (demo Higsona, modernizacja frontendu, aktualizacje wersji Higsona), język dokumentacji
