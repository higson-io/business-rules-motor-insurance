# Architektura systemu

## Przegląd

**Business Rules for Motor Insurance** to jednomodułowa aplikacja Spring Boot (`packaging: war`) demonstrująca integrację z silnikiem reguł biznesowych **Higson BRMS**. Aplikacja wylicza wycenę ubezpieczenia komunikacyjnego w trzech wariantach (Bronze / Silver / Gold), przy czym **cała logika cenowa znajduje się w regułach Higsona, a nie w kodzie Java**.

Kluczowa zasada architektoniczna: kod Java jest cienką warstwą tłumaczącą model dziedzinowy aplikacji na kanoniczny model kontekstowy Higsona i z powrotem. Zmiana taryfy nie wymaga rekompilacji — wystarczy publikacja reguł w Studio.

UI reaguje na zmiany bez przycisku „przelicz": zatwierdzenie zmiany danych kierowcy lub pojazdu wywołuje ponowne przeliczenie wyceny. Dwa świadome wyjątki: pola tekstowe i liczbowe zatwierdzają się na `blur` (nie na każde naciśnięcie klawisza), a w kaskadzie pojazdu przeładowuje dopiero wybór **modelu**.

---

## Wzorzec architektoniczny

**Wzorzec**: warstwowy monolit MVC z wzorcem **Adapter** na granicy silnika reguł.

```
Przeglądarka (Angular 22 SPA)
        │  HTTP/JSON
        ▼
┌───────────────────────────────────────────┐
│  API           (kontrolery REST + DTO)    │
├───────────────────────────────────────────┤
│  Converter     (DTO ⇄ Domain)             │
├───────────────────────────────────────────┤
│  Service       (@SessionScope QuoteService)│
├───────────────────────────────────────────┤
│  Domain        (Quote, Option, Coverage…)  │
├───────────────────────────────────────────┤
│  Adapter       (Domain → model kanoniczny) │
├───────────────────────────────────────────┤
│  Context       (MotorContext)             │
├───────────────────────────────────────────┤
│  Higson Engine (tablice decyzyjne, funkcje)│
└───────────────────────────────────────────┘
        │  JDBC (przez DataSourceProxy)
        ▼
   PostgreSQL / H2  (repozytorium reguł)
```

### Dlaczego taki podział

- **Adapter jako granica** — model dziedzinowy aplikacji (`Quote`, `Driver`, `Vehicle`) pozostaje niezależny od API Higsona. Klasy `*Adapter` są jedynym miejscem, które zna kanoniczne ścieżki kontekstu. Zmiana struktury domeny w Studio dotyka wyłącznie adapterów.
- **Stan w sesji** — `QuoteService` jest `@SessionScope`, dzięki czemu obiekt `Quote` przeżywa między żądaniami. To umożliwia inkrementalne aktualizacje (zmiana jednego pola → przeliczenie całości) bez przesyłania pełnego stanu z frontendu.
- **Brak ORM** — aplikacja nie ma własnych encji ani repozytoriów. Baza danych należy w całości do Higsona; dostęp realizuje runtime.

---

## Struktura systemu

Bazowy pakiet: `pl.decerto.higson.demo.motor`

### API — kontrolery REST

- **Lokalizacja**: `src/main/java/pl/decerto/higson/demo/motor/api/`
- **Rola**: endpointy HTTP konsumowane przez SPA; kontekst aplikacji `/demo`
- **Kluczowe klasy**:
  - `QuoteApi` — `@GetMapping("/quote")`, główny endpoint wyceny
  - `DriverApi` — dane kierowcy
  - `VehicleApi` — dane pojazdu
  - `DictionaryApi` — dane słownikowe (marki, modele, typy pojazdów)
  - `ProfilersApi` — endpointy profilowania Higsona (`/profilers/jdbc`)

### DTO — kontrakty API

- **Lokalizacja**: `src/main/java/pl/decerto/higson/demo/motor/api/dto/`
- **Rola**: obiekty transferowe oddzielające kontrakt HTTP od modelu dziedzinowego
- **Kluczowe klasy**: `QuoteDto`, `OptionDto`, `CoverageDto`, `DiscountDto`, `DriverDto`, `VehicleDto`, `AddressDto`, `DictionaryEntryDto`

### Converter — mapowanie DTO ⇄ Domain

- **Lokalizacja**: `src/main/java/pl/decerto/higson/demo/motor/converter/`
- **Rola**: jawna, ręczna konwersja między DTO a obiektami dziedzinowymi (brak MapStructa/ModelMappera — celowo, dla czytelności demo)

### Service — logika aplikacyjna

- **Lokalizacja**: `src/main/java/pl/decerto/higson/demo/motor/service/`
- **Rola**: orkiestracja przypadków użycia
- **Kluczowa klasa**: `QuoteService` — **`@SessionScope`**; przechowuje bieżącą wycenę użytkownika i wyzwala ponowną kalkulację po każdej zmianie danych wejściowych

### Domain — model dziedzinowy

- **Lokalizacja**: `src/main/java/pl/decerto/higson/demo/motor/domain/`
- **Rola**: obiekty biznesowe, agregat `Quote`
- **Model**:

| Klasa | Rola | Wyliczane przez reguły |
|---|---|---|
| `Quote` | korzeń agregatu — plan, kierowca, pojazd, warianty | — |
| `Option` | wariant cenowy (Bronze / Silver / Gold): kod, kolejność, ochrony, zniżki | — |
| `Coverage` | rodzaj ochrony: kod, nazwa, opis, składka, `limit1`, `limit2`, pozycja | ✅ `PREMIUM`, `LIMIT_1`, `LIMIT_2` |
| `Discount` | zniżka: kod, nazwa, wartość, pozycja | ✅ dostępność (atrybut `available`) i wartość |
| `Driver` | ubezpieczony: imię, nazwisko, płeć, data urodzenia, wiek uzyskania prawa jazdy, adres | — (dane wejściowe reguł) |
| `Vehicle` | pojazd: rok produkcji, marka, typ, model | — (dane wejściowe reguł) |
| `Address` | miasto, ulica, kod pocztowy | — (dane wejściowe reguł) |

### Adapter — most do Higsona

- **Lokalizacja**: `src/main/java/pl/decerto/higson/demo/motor/adapter/`
- **Rola**: tłumaczenie obiektów dziedzinowych na kanoniczny model kontekstu Higsona
- **Wzorzec**: Adapter — jedyne miejsce w kodzie znające ścieżki kontekstu Higsona
- **Kluczowe klasy**: `QuoteAdapter`, `OptionAdapter`, `CoverageAdapter`, `DriverAdapter` (oraz odpowiedniki dla pozostałych encji)

### Context — konfiguracja kontekstu Higsona

- **Lokalizacja**: `src/main/java/pl/decerto/higson/demo/motor/context/`
- **Rola**: inicjalizacja kontekstu i rozwiązywanie ścieżek
- **Kluczowa klasa**: `MotorContext extends HigsonContext`

### Example — orkiestracja kalkulacji

- **Lokalizacja**: `src/main/java/pl/decerto/higson/demo/motor/example/`
- **Kluczowa klasa**: `Motor` — właściwy silnik przypadku użycia; iteruje po wariantach i ochronach, wywołując atrybuty domenowe Higsona

### Exceptions — obsługa błędów

- **Lokalizacja**: `src/main/java/pl/decerto/higson/demo/motor/exceptions/`
- **Rola**: wyjątki własne + `GlobalExceptionHandler` (`@RestControllerAdvice`) mapujący je na odpowiedzi HTTP z ciałem `ErrorDto` (`{"message": …}`)
- **Uwaga historyczna**: do sierpnia 2026 r. był to `@ControllerAdvice` zwracający `String`. Spring traktował zwróconą wartość jako **nazwę widoku**, więc nieudana walidacja kończyła się `404` z pustym ciałem, a komunikat walidatora nigdy nie docierał do użytkownika. Poprawka (`ErrorDto` + `@RestControllerAdvice`) jest jedyną zmianą backendu w całej migracji frontendu

### Analiza potrzeb — jedna implementacja na trzy linie

Cała mechanika APK żyje w `pl.decerto.higson.demo.apk`: `Question`, `Answer`, `Regime`,
`Need`, `Warning`, `Recommendation`, `QuestionCatalogue`, `RegimeReader` oraz
**`ApkAnalysis`** — cztery przejścia (wyprowadź potrzeby, skonfrontuj z wyłączeniami,
policz pokrycie, uzasadnij), z których korzystają wszystkie trzy profile.

⛔ **`ApkAnalysis` nie zna ani jednej reguły.** Żadnego kodu pytania, kodu pokrycia, progu,
mnożnika ani zdania, które przeczyta klient. Java chodzi po listach, tabele rozstrzygają.
Sprawdzian jest mechaniczny: `grep -c 'MOTOR\|LIFE\|PROPERTY\|COMP\|MAIN_COVER'` na tym
pliku zwraca 0.

Podział został przeprowadzony 27 sierpnia 2026 r.: **dwanaście funkcji Groovy i trzy flow**
zniknęły, bo w większości składały i rozbierały łańcuchy `KOD=WARTOŚĆ;KOD=WARTOŚĆ` — flow
zwraca skalary, a Groovy był jedynym miejscem z pętlą. Ani jedna tabela decyzyjna się przy
tym nie ruszyła. Kryterium: **funkcja zostaje, jeżeli zawiera regułę** (wzór, mnożnik, próg);
znika, jeżeli tylko chodziła po liście.

Na drucie jedziemy jednym `RecommendationDto` — potrzeby, ostrzeżenia i uzasadnienie razem,
bo razem są jedną analizą; trzy osobne pola najwyższego poziomu kusiły, żeby pokazać jedno
bez drugiego.

### Frontend — SPA

- **Lokalizacja**: `frontend/` (workspace Angular CLI); wynik builda ląduje w korzeniu WAR-a
- **Framework**: Angular 22.1.0, komponenty standalone, bez `NgModule` i **bez routera** (ekrany są nieadresowalne — patrz niżej)
- **Komponenty** (TS + HTML + LESS współlokowane w jednym katalogu, nazwanym jak komponent):
  `app-root` (powłoka — sam layout), `app-demo-picker` (ekran startowy), trzy gospodarze dem (`app-moto`, `app-life`, `app-property`), `app-inputs` (panel wejściowy), `app-vehicle`, `app-driver`, `app-driver-history`, `app-variants` (warianty cenowe), `app-coverage`, `app-discount`, `app-spinner`, oraz dyrektywa `[appHtmlPopover]` na CDK Overlay
- **Wspólne dla trzech dem** (`shared/`): `app-step-bar`, `app-apk-questionnaire`, `app-apk-recommendation`, `app-error-banner`

#### Trzy kroki: analiza potrzeb → rekomendacja → składka

Każde demo jest podzielone na te same trzy kroki, w kolejności, w jakiej biegnie analiza,
a **nie** w kolejności, w jakiej robi to typowy kalkulator. Ekran otwierający się na cenie
przestaje być analizą potrzeb — o tę odwrotność chodzi w rek. 8 KNF.

- **Krok jest sygnałem, nie adresem** (ND-7). Wstecz przeglądarki go nie cofa, tak samo jak
  przy wyborze demo.
- ⛔ **Każdy krok osiągalny zawsze**, także zanim wycena się wczyta. Pasek pokazuje POZYCJĘ,
  nie uprawnienie: prowadzący musi móc wrócić do jednej odpowiedzi i pokazać, jak przestawia
  się propozycja. Ekrany dalsze mają własne stany puste i własny baner błędu, więc blokada
  niczego nie chroni, a produkuje kliknięcia bez skutku.
- **Data polisy stoi NAD paskiem** we wszystkich trzech demach (`app-*-effective-date`,
  wspólny partial `styles/_date-band.less`). Jest kontekstem całej wyceny, a nie odpowiedzią
  na pytanie: przesunięcie jej przez próg zmienia reżim, zestaw pytań i rekomendację naraz.
- **`app-apk-recommendation` nie zna żadnego produktu.** Dostaje gotową analizę i nazwy do
  wypisania, dlatego ten sam plik renderuje propozycję Z wariantem (majątek, życie grupowe)
  i BEZ wariantu (życie indywidualne, gdzie propozycją jest zestaw umów dodatkowych,
  a `variantCode` jest pusty **celowo**).
- Wygląd paska wzięty z `step-bar` z `higson-agent-portal-demo-frontend`, z jedną świadomą
  różnicą: tamten jest napędzany routerem i nieklikalny, bo kolejność wymusza proces
  sprzedaży. Tutaj nic jej nie wymusza.
- **Serwisy**: `DictionaryService`, `DriverService`, `VehicleService`, `VariantService` — jedyne klasy wstrzykujące `HttpClient`; jedna metoda na endpoint, URL-e względne bez wiodącego ukośnika
- **Stan współdzielony**: `QuoteStore` — patrz niżej
- **Przekrojowe**: dwa funkcyjne interceptory HTTP — licznik żądań w locie (zasila jeden globalny spinner i `aria-busy` powłoki) oraz przechwytywanie błędów mutacji (zapisuje komunikat do `QuoteStore`, **bez ponowień**)
- **Style**: LESS, punkt wejścia `frontend/src/styles.less`; jeden wyjątek Sass — `frontend/src/theme.scss` dla `mat.theme()`
- **Ścieżka kontekstu**: brak elementu `<base>` i brak `baseHref` — jeden WAR obsługuje `/demo`, `/app` i korzeń serwera bez przebudowy

#### `QuoteStore` — magazyn sygnałowy zamiast magistrali zdarzeń

`QuoteStore` (`@Injectable({ providedIn: 'root' })`) zastąpił w całości mechanizm AngularJS-owy `$scope.$emit('recalculate')` → przekaźnik w `main` → `$scope.$broadcast('reloadQuote')`. Przekaźnik został **usunięty, a nie przeniesiony**: istniał tylko dlatego, że AngularJS nie miał stanu współdzielonego między siostrzanymi dyrektywami.

Magazyn trzyma dokładnie trzy rzeczy:

| Element | Rodzaj | Rola |
|---|---|---|
| `quote` | `resource()` | bieżąca wycena; przeładowywana przez podbicie prywatnego sygnału wyzwalacza |
| `driver` | `resource()` | ładowany **raz** i współdzielony przez wszystkich konsumentów (wcześniej pobierany dwukrotnie na starcie) |
| `mutationError` | `signal<string \| null>` | zapisywany przez interceptor błędów, renderowany przez czerwony baner |

Po stronie klienta **nie ma modelu wyceny** — pełną wycenę trzyma sesyjny bean backendu, więc SPA pozostaje cienkim, zdalnie sterowanym widokiem.

⛔ **Asymetria kaskady pojazdu.** Tylko zmiana **modelu** pojazdu przeładowuje wycenę; rok produkcji, marka i typ celowo nie. Jest to zachowanie przeniesione 1:1 z poprzedniej wersji i zakodowane jako wyczerpująca tablica `RELOADS_QUOTE` w magazynie, a nie jako dyscyplina komponentu: komponent pojazdu zgłasza wszystkie cztery poziomy przez jedną metodę `vehicleFieldChanged(field)` i **nigdy sam nie woła `recalculate()`**. Dodanie pola do unii `VehicleField` jest błędem kompilacji, dopóki ktoś świadomie nie zadeklaruje w tym jednym miejscu, czy przeładowuje.

### Konfiguracja reguł biznesowych

- **Lokalizacja**: `src/main/higson/` — kanoniczna lokalizacja snapshotu w drzewie roboczym, adresowana przez właściwość Mavena `${higson.snapshot.dir}`. **Nie jest śledzona w git**
- **System zapisu (*system of record*)**: baza danych **Higson Studio** — to tam faktycznie żyją reguły. Snapshot jest kopią roboczą, wyciąganą do drzewa i odsyłaną z powrotem na żądanie (`mvn higson-cli:pull` / `higson-cli:push`)
- **Struktura**: `@domains/` (33), `@params/` (81, tablice decyzyjne), `@functions/` (36, Groovy/Rhino), `@tests/` (9), `@profiles/` (6), `@flows/` (2), `@testPackages/` (2), `@tags/` (1), `config.json` (metadane snapshotu)

> ⚠️ **Uwaga:** snapshot nigdy nie był wersjonowany w git — nie występuje w żadnym commicie i nie jest też wpisany do `.gitignore`. Świeży klon repozytorium nie zawiera **żadnych reguł**, dopóki nie uruchomi się `mvn higson-cli:pull`. Wynika z tego, że zmiana w kodzie Javy i odpowiadająca jej zmiana reguły nigdy nie są commitowane atomowo. Analizując kod Javy zależny od reguł, zakładaj, że pasujące reguły znajdują się w instancji Studio, a nie w repozytorium.

### Konfiguracja Springa

- **`ApplicationConfig`** — `@Configuration`, `@ComponentScan("pl.decerto.higson.demo.motor")`, import `HigsonIntegrationConfiguration`, `ViewControllerRegistry` przekierowujący `/` → `index.html`
- **`HigsonIntegrationConfiguration`** — ręczne opakowanie `DataSource` w `DataSourceProxy` na potrzeby profilowania JDBC

---

## Przepływ danych

Pełny cykl przeliczenia wyceny:

1. **Użytkownik** zmienia dane kierowcy lub pojazdu w panelu bocznym SPA. Pola tekstowe i liczbowe zatwierdzają się na `blur`, nie na każde naciśnięcie klawisza, i tylko jeśli wartość faktycznie się zmieniła
2. **Frontend** wysyła pojedyncze `PUT` do odpowiedniego endpointu (`DriverApi` / `VehicleApi`), a po jego powodzeniu woła `QuoteStore.recalculate()`, co podbija sygnał wyzwalacza i powoduje ponowne pobranie `GET /demo/quote` przez `resource()`
3. **Kontroler** konwertuje DTO na obiekty dziedzinowe (warstwa `converter`)
4. **`QuoteService`** (sesyjny) aktualizuje przechowywany agregat `Quote`
5. **`Motor`** iteruje po wariantach (`Option`) i dla każdego po ochronach (`Coverage`):
   - `*Adapter` przygotowuje kontekst Higsona z danych kierowcy, pojazdu i wariantu
   - silnik wylicza atrybuty domenowe: `PREMIUM`, `LIMIT_1`, `LIMIT_2`
   - dla każdej zniżki sprawdzany jest boolowski atrybut `available` oraz jej wartość
6. **Higson** wykonuje reguły — tablice decyzyjne i funkcje ze snapshotu, czytane przez JDBC z bazy
7. **Wyniki** wracają do obiektów `Coverage` i `Discount` w agregacie
8. **Kontroler** mapuje `Quote` na `QuoteDto` i zwraca JSON
9. **Frontend** przerysowuje warianty z nowymi składkami, limitami i zniżkami — bez sortowania po stronie klienta, bo backend zwraca warianty po rosnącym `order`, a ochrony i zniżki po rosnącym `position`

Przykład zależności regułowej: *marka pojazdu + rok produkcji + wiek kierowcy → składka za daną ochronę*.

⛔ **Wyjątek od kroku 2**: w kaskadzie pojazdu zmiana roku produkcji, marki i typu wysyła `PUT`, ale **nie** wyzwala `GET /demo/quote` — przeładowanie następuje dopiero po wyborze modelu. Patrz `QuoteStore` wyżej.

---

## Integracje zewnętrzne

| Integracja | Rola | Sposób podłączenia |
|---|---|---|
| **Higson Runtime** | wykonywanie reguł | osadzony w aplikacji przez `spring-boot-starter-higson-runtime` |
| **Higson Studio** | projektowanie i publikacja reguł | HTTP, `http://localhost:38088/studio`; synchronizacja przez `higson-cli-maven-plugin` (`pull` / `push`), uwierzytelnienie tokenem integracyjnym |
| **PostgreSQL / H2** | repozytorium reguł i parametrów | JDBC przez HikariCP, opakowane `DataSourceProxy` |
| **Higson Runtime REST** | (tylko `docker-compose`) osobna usługa REST runtime'u | port 8081 |

Aplikacja **nie integruje się** z żadnym zewnętrznym API ubezpieczeniowym — wszystkie dane słownikowe pochodzą ze snapshotu Higsona.

---

## Schemat bazy danych

Brak ORM i brak migracji po stronie aplikacji. Schemat bazy jest **własnością Higsona** — tworzony i wersjonowany przez runtime/Studio. Aplikacja traktuje bazę jako czarną skrzynkę dostępną wyłącznie przez silnik reguł.

Migracji SQL faktycznie nie ma i nie należy ich dodawać. Wersjonowanie logiki biznesowej „przez snapshot w `src/main/higson/` zamiast migracji SQL" jest jednak stanem **postulowanym, a nie faktycznym**: snapshot nie jest commitowany, więc po stronie repozytorium nie istnieje żadna historia zmian reguł. Rzeczywistą historię wersji trzyma Higson Studio, a snapshot pełni wyłącznie rolę nośnika przenoszenia reguł poleceniami `higson-cli:pull` / `higson-cli:push`.

> ⚠️ **Uwaga:** katalog `src/main/higson/` nie jest śledzony w git (nie jest też ignorowany — po prostu nigdy nie został dodany). Dopóki tak pozostanie, snapshot nie daje żadnej gwarancji wersjonowania: zmiana w Javie i odpowiadająca jej zmiana reguły nie trafiają do wspólnego commita, a odtworzenie stanu reguł sprzed dowolnego commita wymaga sięgnięcia do Studio.

---

## Konfiguracja

Konfiguracja dwustopniowa, z nadpisywaniem:

1. `src/main/resources/higson-demo-app.properties` — wartości domyślne z classpath
2. `${user.home}/conf/higson-demo-app.properties` — nadpisania lokalne (ładowane, jeśli plik istnieje)

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

Parametry integracji ze Studio żyją w `pom.xml` jako właściwości Mavena: `higson.studio.url`, `higson.snapshot.dir`, `higson.studio.token`.

⚠️ `higson.studio.token` jest **pusty** i taki ma pozostać — token podaje się przy wywołaniu (`-Dhigson.studio.token=...`) albo wpisem `<server>` w `settings.xml`. Remote jest publiczny, a nic nie skanuje commitów.

---

## Architektura wdrożenia

### Artefakt

`mvn clean package` → `target/motor-insurance.war`. Build frontendu jest wpięty w cykl Mavena: `exec-maven-plugin` uruchamia `npm run build` (`npm ci && ng build`) w katalogu `frontend/` w fazie `prepare-package`, a `maven-war-plugin` wciąga `frontend/dist/motor-insurance/browser` do korzenia WAR-a — dokładnie tam, gdzie mieszkał poprzedni frontend, więc `ApplicationConfig` z `forward:/index.html` nie wymagał zmiany.

⚠️ Wersja `maven-war-plugin` jest **celowo pominięta**: zarządza nią `spring-boot-starter-parent` (3.4.0), a jego `pluginManagement` niesie blok `<archive>`, który jawna wersja by nadpisała.

Profil `all_in_one` dokłada `spring-boot-maven-plugin` z `<executable>true</executable>`, dając samodzielnie uruchamialny WAR.

Testy frontendu **nie** są częścią `mvn package` — `npm run build` nie uruchamia ani Vitesta, ani Playwrighta.

### Docker

Obraz na bazie `eclipse-temurin:17-jre-jammy`, z parametrem `JAVA_OPTS`.

### Docker Compose — pełne środowisko

```
┌──────────────┐   ┌──────────────┐   ┌──────────────────┐   ┌──────────────┐
│  db (H2)     │   │  studio      │   │  runtime-rest    │   │  web         │
│  8082, 9092  │◄──│  38080       │   │  8081            │   │  48080       │
└──────────────┘   └──────────────┘   └──────────────────┘   └──────────────┘
       ▲                   ▲                    ▲                    │
       └───────────────────┴────────────────────┴────────────────────┘
                        wspólne repozytorium reguł
```

- **`db`** — baza H2 z repozytorium reguł
- **`studio`** — Higson Studio, interfejs projektowania reguł
- **`runtime-rest`** — samodzielny runtime Higsona wystawiony przez REST
- **`web`** — aplikacja demo (`http://localhost:48080/demo`)

### Obserwowalność

Ograniczona, zgodnie z demonstracyjnym charakterem projektu:

- profiler JDBC Higsona — `GET /demo/profilers/jdbc`
- profiler diagnostyczny Higsona — dostępny, wyłączony domyślnie
- brak Actuatora, metryk i logowania strukturalnego

### CI/CD

Brak pipeline'u w repozytorium. Build i wdrożenie realizowane ręcznie lub przez `push-to-docker.sh`.

---

*Na podstawie analizy kodu przeprowadzonej 2026-08-06 (gałąź `techflash/5.8`), zaktualizowane 2026-08-10 po migracji frontendu AngularJS 1.8.3 → Angular 22 (gałąź `migration/angularjs-to-angular-22`)*
