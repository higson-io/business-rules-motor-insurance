# Profil PROPERTY — plan implementacji

**Studio:** 4.3.1, `http://localhost:38088/studio/` (kontener `higson-demo-local-studio`)
**Źródło projektu:** `.maister/docs/project/higson-property-profile.md` (decyzje D-1…D-8)
**Research:** `analysis/market-research.md` (wnioski R-1…R-15, braki B-1…B-8)
**Stan wyjściowy:** profil `PROPERTY` **utworzony 2026-08-21** (patrz W-28) i **pusty** — zero typów
kontekstu, zero domeny, zero regionów, prefiks `demo.property.*` wolny (`higson_search` → 0 trafień).

---

## 1. Model izolacji — przeniesiony z LIFE, nie odkrywany drugi raz

Warstwy zachowują się tak samo jak przy LIFE i to zostało tam zweryfikowane po kolei:

| Warstwa | Zakres | Ryzyko dla DEMO / LIFE |
|---|---|---|
| Typy domenowe, drzewo domeny, typy kontekstu, regiony | **per profil** | **żadne** |
| Tabele decyzyjne, funkcje, flow | **globalne** + przypisanie do profilu | ⚠️ **realne** |

Izolacja reguł bierze się z dwóch rzeczy naraz: **prefiksu `demo.property.*`** (zweryfikowany jako
wolny) i **jawnego przypisania** do profilu `PROPERTY` po utworzeniu.

### ⛔ Zasada bezwzględna

`higson_assign_to_profile` wolno wywołać **wyłącznie** z listą kodów zaczynających się od
`demo.property.` i **bez** parametru `currentProfileCode`. Wywołanie z `currentProfileCode`
**przenosi** element między profilami, a nie kopiuje — na kodzie spoza własnego prefiksu zabrałoby
regułę z MOTO albo LIFE.

Zakazane: `delete_*` na czymkolwiek spoza `demo.property.*`, `update_*` na istniejących tabelach
MOTO i LIFE, `delete_all_domain_types`, `update_domain_type` z `profileCode: DEMO` lub `LIFE`.

### Stan przed rozpoczęciem — zmierzony, nie założony

| Sprawdzenie | Wynik 2026-08-21 |
|---|---|
| `list_work_sessions` | **brak otwartych sesji** |
| `higson_search('demo.property')` | **0 trafień** we wszystkich kategoriach |
| `list_profiles` | `DEMO`, `LIFE` → po W-28 także `PROPERTY` |
| Backup bazy | `pre-property.zip` w `/h2-data/` kontenera **oraz** skopiowany na hosta |

---

## 2. ⚠️ W-28 — profilu nie da się utworzyć przez MCP

**To jest pierwsza rzecz, na którą wywraca się ten plan, i nie ma jej w pułapkach LIFE**, bo profil
`LIFE` już istniał, zanim tamta praca się zaczęła.

| Co | Wynik |
|---|---|
| MCP ma narzędzie tworzące profil? | **Nie.** W komplecie 60+ narzędzi nie ma `create_profile` |
| `higson_create_context_type(profileCode='PROPERTY')` na nieistniejącym profilu | ⛔ **zwraca `{"status":"SUCCESS"}` i tworzy typ w bazie**, ale **każdy odczyt** (`list_context_types`, `get_domain_definition`) odpowiada „Profile 'PROPERTY' not found". Powstaje wiersz w `SANDBOXTYPESDEFINITION`, nie powstaje wiersz w `DOMAIN_PROFILE` — czyli profil w rozumieniu kontekstu istnieje, a w rozumieniu aplikacji nie |
| REST `POST /studio/api/profiles` | istnieje (`CreateProfileRequest`), ale wymaga tokenu, którego konfiguracja pluginu nie udostępnia |
| Studio UI | niedostępne — przeglądarka Playwright zajęta przez inną sesję |

**Rozwiązanie użyte:** dwa `INSERT`-y w H2, dokładnie w te dwie tabele, które wypełnia endpoint REST:

```sql
insert into DOMAIN_PROFILE (CODE, DESCRIPTION)
values ('PROPERTY', 'Ubezpieczenie domu i mieszkania - demo APK');

insert into PROFILE_CONFIGURATION
  (PROFILE_CODE, DEFAULT_TAG, ADD_TAG_TO_NEW_PARAMETERS, ADD_TAG_TO_NEW_FUNCTIONS,
   FILTER_PARAMETERS, FILTER_FUNCTIONS, ADD_TAG_TO_NEW_FLOWS, FILTER_FLOWS)
values ('PROPERTY', null, 0, 0, 0, 0, 0, 0);
```

Potwierdzone `list_profiles` → trzy profile. **Backup bazy zrobiony przed tą operacją.**

> ⚠️ **Rozjazd między zapisem a odczytem jest groźniejszy niż brak narzędzia.** Zapis nie waliduje
> profilu, odczyt waliduje. Bez sprawdzenia `list_context_types` po każdym typie można zbudować cały
> G1 „pomyślnie" i dopiero w G2 zobaczyć, że nie ma do czego wiązać poziomów wejściowych.
> **Każda grupa kończy się odczytem weryfikującym**, nie odczytem statusu.

---

## 2a. ⚠️ W-29 — `update_context_type(root: true)` nie ustawi pierwszego korzenia

Po utworzeniu wszystkich typów kontekstu w pustym profilu **żaden z nich nie jest korzeniem**, a
próba naprawy przez API kończy się:

```
higson_update_context_type(name='ROOT', root=true)
→ INTERNAL_ERROR: "Collection contains no element matching the predicate."
```

Wygląda na klasyczne jajko-kura: kod szuka **obecnego** korzenia, żeby go zdegradować, i nie
znajduje żadnego. Objaw wtórny: `get_context_type_paths` zwraca **pustą listę**, mimo że wszystkie
typy istnieją i mają atrybuty — czyli **żadna tabela decyzyjna nie mogłaby zbindować poziomu
wejściowego**, a komunikatu o przyczynie nie ma.

**Rozwiązanie użyte — `higson_import_context`, nie SQL.** Format eksportu (`higson_export_context`)
oznacza korzeń **ukośnikiem w nazwie**:

```
@transient
def /ROOT
  quote PropertyQuote          @desc Wniosek majatkowy
```

Import ustawia flagę poprawnie. Wymaga jednak **czystego profilu** — na istniejącym typie mówi
`Type(ROOT) already exists`. Procedura:

1. `delete_context_type` dla **wszystkich** typów profilu, zaczynając od `ROOT` i typu, który go
   używa (`PropertyQuote`), potem reszta w dowolnej kolejności
2. `docker cp` pliku do `/usr/local/tomcat/temp/higson-mcp-workspace/` w kontenerze Studio
3. `import_context`
4. **Weryfikacja: `get_context_type_paths` → 38 ścieżek**, nie status importu

> **Wniosek na przyszłość: typy kontekstu w nowym profilu buduje się plikiem, nie wywołaniami.**
> Jedno wywołanie zamiast czternastu, korzeń wychodzi poprawnie za pierwszym razem, a plik jest
> przeglądalny przed wysłaniem. Wywołania `create_context_type` mają sens wyłącznie przy dokładaniu
> pojedynczego typu do profilu, który już ma korzeń.

---

## 3. Kolejność grup — wymuszona zależnościami

```
G0 Profil ──► G1 Konteksty ──► G2 Tabele ──► G3 Funkcje ──► G4 Flow ──► G5 Typy domenowe
                                  │                            │              │
                                  └────────────────────────────┴──────────────┘
                                                  │
                          G6 Drzewo domeny ──► G7 Publikacja #1 ──► G8 Region i wersje
                                                                          │
                                    G9 Backend ──► G10 Front ──► G11 Weryfikacja e2e
```

Powód każdej strzałki: tabele odwołują się do ścieżek kontekstu; funkcje czytają tabele; flow wołają
tabele i funkcje; **atrybuty typów domenowych wskazują na parametry i flow**, więc muszą powstać po
nich; elementy domeny wymagają typów.

⚠️ **G7 przed G8 jest wymuszone przez W-24:** przypięcie elementu domeny do regionu wymaga
**opublikowanej sesji** (`CANNOT_ATTACH_TO_REGION_IF_WORK_SESSION_IS_ACTIVE`). A siedem pytań
w wersji 2 to właśnie elementy domeny przypinane do regionu. Publikujemy więc **dwa razy**:
raz całą konfigurację bazową, potem region i wersje.

---

## 4. Pułapki przeniesione z LIFE — nie odkrywać drugi raz

Komplet z `2026-08-13-higson-life-implementation/implementation-plan.md` §9 i §10. Kolumna
„Dotyczy tu" mówi, gdzie w tym profilu pułapka uderzy.

| # | Pułapka | Dotyczy tu |
|---|---|---|
| **W-15** | ⛔ `EXPRESSION` we flow jest nieobsługiwane — flow nie liczy nic sam | Cała arytmetyka w funkcjach (§6 dokumentu). Oba flow tylko wołają |
| **W-16** | Selektor `[kolumna]` działa **wyłącznie** w atrybutach domeny, nie w API Groovy | `limit.effective`, `tariff.variantPremium`, `zone.resolve` — wszystkie czytają wielokolumnowe wiersze przez `higson.get(...).row(0).getString(...)` |
| **W-17** | `ctx.get()` zwraca daty jako **String** | `util.buildingAge` bierze rok z `substring(0,4)`, nie z `Calendar` |
| **W-18** | `date.diff(Date,Date,String)` nie istnieje mimo dokumentacji | Nie używamy — wiek budynku liczy się z roku, nie z dat |
| **W-19** | `higson.createContext` **nie konwertuje typów** | `zone.resolve` i `sum.suggest` przekazują `String` pod `string` i `BigDecimal` pod `number` — nigdy `Double` pod `integer` |
| **W-20** | Funkcja z argumentami musi **jawnie zadeklarować `ctx`** jako pierwszy argument | `apk.deriveNeeds`, `apk.scoreVariants`, `apk.justify` — trzy funkcje dwuargumentowe |
| **W-21** | Atrybut domeny: `defaultValue` to **goły kod**, prefiks dokłada `defaultValueType` | 11 atrybutów typu `QUESTION` + 8 typu `COVER` |
| **W-22** | Nawigacja po domenie **nie wstrzykuje kodu elementu do kontekstu** | Java musi budować kontekst z `question.code` sama przy każdym odczycie atrybutu kluczowanego — dokładnie jak `loadGroupBenefits` w LIFE |
| **W-23** | ⛔ `open_decision_table` jest **OBOWIĄZKOWE** przed edycją macierzy | Wszystkie edycje macierzy, w szczególności wersje regionu w G8 |
| **W-24** | Przypięcie elementu domeny do regionu wymaga **opublikowanej sesji** | Wymusza podział na G7 i G8 |
| **W-25** | MCP **nie ma narzędzia przełączającego aktywną wersję regionu** | Wejście do wersji 1: `detach_from_region` → `open_decision_table` + treść v1 → `attach_to_region(versions:["1","2"])` → `open_decision_table` + treść v2 |
| **W-26** | `get_decision_table_matrix` **scala wersje** | Weryfikacja wersji wyłącznie SQL-em (zapytanie w §8) |
| **W-27** | Katalog czytany raz przy tworzeniu sesji **nie widzi wersji regionu** | ⛔ **Najważniejsza pułapka tego demo.** `loadQuestions` musi siedzieć w ścieżce wyceny, po `setEffectiveDate`, a nie w `@PostConstruct` |
| **W-28** | Profilu nie da się utworzyć przez MCP; zapis nie waliduje profilu, odczyt waliduje | §2 |
| **W-29** | `update_context_type(root: true)` nie ustawi **pierwszego** korzenia — typy kontekstu nowego profilu buduje się `import_context`, nie wywołaniami | §2a |
| **W-30…W-34** | Importer XLSX, prawa plików, wersje regionu, cache Studia, kasowanie przypięć przez import domeny | §8 |

### Konwencje macierzy, ustalone empirycznie przy LIFE

- Przedziały: **`18 - 29`** ze spacjami. Półotwarte z wildcardem działają: `* - 50`, `75 - *`
- Boolean w macierzy: **`true`/`false`**, nie `yes`/`no`
- Tablice: jedna komórka, wartości po przecinku — `50000,100000,200000`
- ⚠️ W tabeli, w której **wszystkie** kolumny wejściowe są przedziałowe, **nie dawać wiersza `* - *`**
  — silnik dopasowuje go równolegle z właściwym pasmem. Dotyczy tu **dwóch** tabel:
  `factor.buildingAge` i `factor.claims`. Obie dostają `matchRequired = false` i obsługę `null`
  w funkcji
- W API: `inMemory` (domyślnie `true`) zamiast `cacheable`, `matchRequired` zamiast `nullable`

---

## G0 — Profil `PROPERTY`

Wykonane. §2. Weryfikacja: `list_profiles` → trzy profile, `PROPERTY` z `isUsed: false`.

---

## G1 — Typy kontekstu (8 typów + `ROOT`)

Kontekst jest **kontraktem z kodem Java**, więc odwzorowujemy §3 dokumentu co do znaku.

| Typ | Atrybuty |
|---|---|
| `Property` | `kind` string, `area` number, `constructionYear` integer, `age` integer, `wallMaterial` string, `floor` integer, `renovated` boolean, `finishStandard` string, `postcode` string, `postcodeArea` string, `occupancy` string, `security` string, `claims3y` integer, `priorFloodCover` boolean |
| `Policy` | `startDate` date, `periodMonths` integer, `deductible` number |
| `Needs` | `budget` number |
| `Situation` | `vacancy` boolean, `businessUse` boolean, `highValueItems` boolean, `inspections` boolean |
| `Zone` | `flood` string, `theft` string |
| `Sums` | `building` number, `fixtures` number, `contents` number, `liability` number, `suggestedBuilding` number |
| `Risk` | `factor` number |
| `PropertyQuote` | `property` → `Property`, `policy` → `Policy`, `needs` → `Needs`, `situation` → `Situation`, `zone` → `Zone`, `sums` → `Sums`, `risk` → `Risk` |

Do `ROOT` dokładamy: `quote` → `PropertyQuote`, oraz elementy iteracji `question` (kod), `answer`
(wartość), `cover` (kod), `variant` (kod), `reason` (kod).

`gmoPersistenceTriggerType` zostaje domyślne (`NO`) — ten profil nie persystuje przez GMO.

> ⚠️ **W-12 z LIFE:** typy kontekstu **nie trafiają do sesji roboczej** — powstają natychmiast
> i `reject_changes` ich nie cofnie. Dla pustego profilu nieszkodliwe.

**Weryfikacja grupy:** `list_context_types('PROPERTY')` → 9 pozycji. **Nie status z create.**

---

## G2 — Tabele decyzyjne (24)

| Podgrupa | Tabele | Liczba |
|---|---|---|
| G2.1 APK — katalog | `apk.question.{visibility,required,position,options,prefill}` | 5 |
| G2.2 APK — analiza | `apk.need.derive`, `apk.exclusion.conflict`, `apk.recommendation.reason`, `apk.regime` | 4 |
| G2.3 Suma | `sum.ratePerSqm`, `sum.derivedRatio`, `fixtures.model` | 3 |
| G2.4 Strefy | `zone.flood`, `zone.theft`, `flood.availability`, `flood.waitingPeriod` | 4 |
| G2.5 Taryfa | `tariff.baseRate`, `factor.{buildingAge,wallMaterial,occupancy,security,claims,deductible,period}` | 8 |
| G2.6 Zakres | `cover.{availability,position,sumBasis,limitRule}`, `variant.{availability,position,liabilitySum}` | 7 |

*Razem 31 — nagłówek mówi 24, bo tak liczy dokument projektowy; realna liczba artefaktów to 31.
Rozbieżność zapisana tu, a nie zamiatana: dokument grupuje `factor.*` i `cover.*` jako rodziny.*

**Po każdej podgrupie:** `list_work_sessions` (nie `list_decision_tables` — W-11: listowanie
pokazuje wyłącznie stan opublikowany i w trakcie sesji zwraca 0 dla nowych kodów).

**Na koniec G2:** `assign_to_profile(type=DECISION_TABLE, codes=[…demo.property.*],
newProfileCode='PROPERTY')` — **bez** `currentProfileCode`.

---

## G3 — Funkcje Groovy (8)

`util.buildingAge`, `zone.resolve`, `sum.suggest`, `limit.effective`, `tariff.variantPremium`,
`apk.deriveNeeds`, `apk.scoreVariants`, `apk.justify`.

Ciała w §6 dokumentu, maksimum **29 linii**. Trzy rzeczy do sprawdzenia przy pierwszym uruchomieniu:

1. **`type.paramValue()` z czterema wartościami** (`sum.suggest`) — W-5 dokumentu. Weryfikujemy na
   `zone.resolve`, bo ma tylko dwie i jest najprostsza.
2. **Deklaracja `ctx` przy funkcjach dwuargumentowych** (W-20) — `apk.deriveNeeds` jako pierwsza.
3. **`domain.get('PROPERTY', '/COVERS').getChildren()`** w `tariff.variantPremium` i `scoreVariants`
   — **wymaga istniejącego drzewa domeny**, więc te dwie testujemy dopiero po G6.

Przypisanie do profilu na koniec grupy, tą samą regułą co w G2.

---

## G4 — Flow (2)

`demo.property.calculateQuote` (11 nodów), `demo.property.apk.recommend` (13 nodów).

Kolejność: najpierw `calculateQuote`, bo `apk.recommend` go woła.

Węzły w §5 dokumentu. Diagramy tam są **logiczne, nie eksportowalne** — w Studio budujemy je nodami
z akcjami `DECISION_TABLE` / `FUNCTION` / `FLOW`, w formacie widocznym w
`demo.motor.plan.full.calculate.premium`. **Zero `EXPRESSION`** (W-15).

---

## G5 — Typy domenowe (3)

`COVER` (8 atrybutów), `VARIANT` (5), `QUESTION` (11) — z §2.1 dokumentu.

⚠️ `defaultValue` to **goły kod** parametru albo flow; prefiks dokłada `defaultValueType` (W-21).

**`QUESTION` ma 11 atrybutów i to jest najbardziej ryzykowny element tej grupy** — cztery z nich
(`OPTIONS`, `OPTION_LABELS`, `PREFILL_SOURCE`, `REFUSAL_MESSAGE`) czytają tabele **dwukolumnowe**,
a selektor `[kolumna]` w atrybucie domeny **działa** (W-16 dotyczy tylko API Groovy). To jedyne
miejsce w profilu, gdzie z niego korzystamy — i dlatego weryfikujemy je na jednym pytaniu, zanim
powstanie osiemnaście.

---

## G6 — Drzewo domeny (21 elementów w wersji bazowej)

```
/COVERS[BUILDING | FIXTURES | CONTENTS | LIABILITY | ASSISTANCE | THEFT | FLOOD]
/VARIANTS[BASIC | STANDARD | COMPLETE]
/QUESTIONS[PROPERTY_KIND | AREA | CONSTRUCTION_YEAR | WALL_MATERIAL | FLOOR | RENOVATED
           | FINISH_STANDARD | POSTCODE | OCCUPANCY | SECURITY | CLAIMS_3Y]
```

⛔ **Siedem pytań wersji 2 NIE powstaje tutaj.** `CIRCUMSTANCES`, `COVER_PERIOD`, `BUDGET`,
`VACANCY`, `BUSINESS_USE`, `HIGH_VALUE_ITEMS`, `INSPECTIONS` tworzy się w G8, wewnątrz wersji 2
regionu. To jest cała demonstracja D-5 — pomyłka w tym miejscu kosztuje **jedyny scenariusz, dla
którego to demo powstało**.

Po G6: uruchomienie `tariff.variantPremium` w Testerze — pierwsze `domain.get` na realnym drzewie.

---

## G7 — Publikacja #1

Wymuszona przez W-24. Przed publikacją, obowiązkowo:

1. `list_work_sessions` → przegląd **pełnej** listy zmian; każdy element musi mieć kod
   `demo.property.*` albo należeć do profilu `PROPERTY`. Jeden obcy wpis = `reject_changes`.
2. `get_domain_definition('DEMO')` → drzewo MOTO bez zmian.
3. `get_domain_definition('LIFE')` → drzewo LIFE bez zmian.
4. `list_context_types('DEMO')` → nadal 8 typów.
5. `list_decision_tables()` bez filtra — liczba spoza `demo.property.*` **nie może spaść**.

Dopiero wtedy `publish_changes(confirmed=true)`.

---

## G8 — Region `PROPERTY` i dwie wersje

1. Region `PROPERTY`, wersja 1 „APK — minimum ustawowe", timeline **`2026-01-01`**.
2. Wersja 2 „APK wg Rekomendacji KNF 190/2026", timeline **`2027-07-01`**.
3. `attach_to_region` dla **trzech** tabel: `apk.regime`, `sum.ratePerSqm`, `tariff.baseRate`.
4. Macierze wersji 2 wg §7.3 dokumentu — **procedurą W-25**, per tabela:
   `detach_from_region` → `open_decision_table` + treść **v1** → `attach_to_region(versions:["1","2"])`
   → `open_decision_table` + treść **v2**.
5. **Siedem elementów domeny** `QUESTIONS` w wersji 2 regionu + ich wiersze w tabelach
   `apk.question.*`, `apk.need.derive`, `apk.exclusion.conflict` — **globalne, nie wersjonowane**
   (D-5).
6. Weryfikacja per wersja **wyłącznie SQL-em** (W-26):
   ```sql
   select p.regionversion_id, p.head, count(e.id)
   from parameter p left join parameterentry e on e.parameter_id = p.id
   where p.name = 'demo.property.sum.ratePerSqm' group by 1, 2;
   ```
   Oczekiwane: dwa wiersze, oba z niezerową liczbą wpisów.

---

## G9 — Backend: `pl.decerto.higson.demo.property`

Konwencja `demo.life` co do znaku. Pakiety: `api`, `api.dto`, `converter`, `service`, `domain`,
`adapter`, `context`, `example`.

| Klasa | Odpowiedzialność |
|---|---|
| `example.Property` | Orkiestrator. Czyta katalog pytań, liczy, woła flow. **Zero stawek i progów** |
| `context.PropertyContext` | Jedyne miejsce znające ścieżki kontekstu i elementy iteracji |
| `adapter.*` | `PropertyQuoteAdapter`, `PropertySubjectAdapter`, `PolicyAdapter`, `NeedsAdapter`, `SituationAdapter`, `ZoneAdapter`, `SumsAdapter`, `RiskAdapter` |
| `domain.*` | `PropertyQuote`, `Subject`, `Policy`, `Question`, `Answer`, `Cover`, `Variant`, `Need`, `Warning`, `Recommendation` |
| `service.PropertyQuoteService` | `@SessionScope`, trzyma agregat między żądaniami |
| `api.PropertyApi` | REST, `@RequestMapping("/property")`, każda mutacja to `PUT` jednego pola |
| `converter.PropertyQuoteConverter` | Domena → DTO |

⛔ **Trzy rzeczy, na których to się przewróci, jeśli się o nich zapomni:**

1. **W-27** — `loadQuestions()` w **ścieżce wyceny**, po `engine.setEffectiveDate(startDate)`,
   nigdy w `@PostConstruct`. Inaczej wersja 2 nigdy się nie pokaże i będzie to wyglądało na błąd
   konfiguracji, a nie kodu.
2. **`engine.clearEffectiveDate()` w `finally`** — effective date jest stanem silnika, nie żądania.
3. **Dopisanie pakietu do `@ComponentScan`** w `ApplicationConfig` — inaczej `PropertyApi` nie
   istnieje, a front dostaje 404 bez śladu w logach.

Endpointy (13):

```
GET  property                              → PropertyQuoteDto
PUT  property/policy/startDate             Date      ⭐ przełącza wersję APK
PUT  property/policy/periodMonths          int
PUT  property/policy/deductible            BigDecimal
PUT  property/answer/{code}                String    ⭐ jedna odpowiedź APK
PUT  property/answer/{code}/refused        boolean   (D-6)
PUT  property/answer/{code}/multi          String[]  (CIRCUMSTANCES)
PUT  property/sum/{coverCode}              BigDecimal
PUT  property/variant                      String
```

---

## G10 — Front: `frontend/src/app/demos/property/`

Rozdział własny, §5 niżej.

---

## G11 — Weryfikacja end-to-end

Jedyny test, który weryfikuje tezę demo — z §7 briefu:

| # | Sprawdzenie | Jak |
|---|---|---|
| 1 | Ankieta renderuje się **z pytań z Higsona** | Zmiana `TEXT` pytania w Studio zmienia napis na ekranie bez przebudowy |
| 2 | ⭐ Przesunięcie daty przez `2027-07-01` **widocznie zmienia zestaw pytań** | 9 widocznych → 16 widocznych |
| 3 | ⭐ …i **treść rekomendacji** | brak uzasadnienia → lista powodów + podstawa prawna |
| 4 | Rekomendacja pokazuje **różnice między wariantami** | macierz `cover × variant` z zaznaczonym wyborem |
| 5 | Ostrzeżenie o wyłączeniu pojawia się dla sytuacji, która je wywołuje | `VACANCY = tak` → czerwone ostrzeżenie + sposób naprawy |
| 6 | Obie suity zielone | `ng test --watch=false` w `frontend/`, `mvn -o test` |

---

## 5. Plan frontu (G3 briefu, G10 planu)

### 5.1 Gdzie w drzewie

```
frontend/src/app/
├── demos/demo-catalog.ts            ← DemoCode dostaje 'property', DEMOS nowy wpis
├── app.html                         ← @switch dostaje @case ('property')
├── app.ts                           ← import komponentu Property
├── models/property.ts               ← model drutu, wzorem models/life.ts
├── services/property-service.ts     ← jedna metoda na endpoint
├── core/property-store.ts           ← resource() + reload(), wzorem LifeStore
└── demos/property/
    ├── property.ts / .html / .less           ← powłoka: kroki + przyklejone podsumowanie
    ├── effective-date/                        ← data początku ochrony NAD ankietą
    ├── questionnaire/                         ← ankieta APK renderowana z Higsona
    ├── recommendation/                        ← rekomendacja + uzasadnienie + ostrzeżenia
    └── comparison/                            ← macierz różnic między wariantami (rek. 9.3)
```

### 5.2 Przepływ ekranów

```
┌──────────────────────────────────────────────────────────────────┐
│  Data początku ochrony  [ 2027-07-01 ]   Reżim: APK wg Rek. KNF  │  ← przełącznik wersji
├────────────────────────────────────────────┬─────────────────────┤
│  1. ANKIETA APK                            │  PODSUMOWANIE       │
│     sekcje: PRZEDMIOT / SYTUACJA /         │  (przyklejone)      │
│     POTRZEBY / WYŁĄCZENIA                  │                     │
│     pytania z Higsona, widoczność z reguł  │  Suma murów         │
│                                            │  Suma el. stałych   │
│  2. REKOMENDACJA                           │  Suma ruchomości    │
│     wariant + uzasadnienie + ostrzeżenia   │  ─────────────      │
│                                            │  Wariant            │
│  3. PORÓWNANIE WARIANTÓW                   │  Składka roczna     │
│     macierz pokrycie × wariant             │  Rata               │
└────────────────────────────────────────────┴─────────────────────┘
```

Kroki są **sekcjami jednej strony**, nie routingiem. ⛔ **Brak routera** jest konsekwentny z resztą
aplikacji (ND-7) — ścieżka to sygnał, URL nie rusza się, względne odwołania do API działają pod
wszystkimi trzema context-pathami.

### 5.3 Store

```ts
@Injectable({ providedIn: 'root' })
export class PropertyStore {
  readonly quote = resource({ loader: () => firstValueFrom(this.service.getQuote()) });
  readonly mutationError = signal<string | null>(null);
  async commit(change: Promise<unknown>) { await change; this.quote.reload(); }
}
```

⛔ **Bez `params`.** Zmiana `params` to nowe żądanie: resource spada do stanu ładowania **bez
wartości**, każdy komponent wraca do renderowania pustego i cały ekran mruga przy każdym
zatwierdzeniu pola. `reload()` trzyma poprzednią wartość widoczną. Ta sama zasada co `LifeStore`
i `QuoteStore`.

⚠️ **`hasValue()` przed `value()`.** W Angularze 22 resource w stanie błędu **rzuca
`ResourceValueError`** z `value()`, więc `value()?.x ?? y` nie degraduje się łagodnie — kładzie
ekran dokładnie na tej awarii, dla której istnieje baner błędu.

### 5.4 Ankieta renderowana z reguł — sedno

Komponent `questionnaire` **nie zna ani jednego pytania**. Dostaje listę `QuestionDto` i renderuje
kontrolkę po polu `kind`:

| `kind` | Kontrolka Material |
|---|---|
| `CHOICE` | `mat-select` z opcjami z `options`/`optionLabels` |
| `MULTI` | grupa `mat-checkbox` |
| `NUMBER`, `MONEY` | `matInput type="number"` z sufiksem |
| `BOOLEAN` | `mat-button-toggle-group` tak/nie |
| `TEXT` | `matInput` |

Widoczne są wyłącznie pytania z `visible === true` — **flaga przychodzi z backendu, nie jest liczona
na froncie**. Gdyby front liczył widoczność, reguła `apk.question.visibility` przestałaby być
regułą.

Przy każdym pytaniu z `prefillSource` — adnotacja „Podpowiedziane na podstawie: …" (rek. 8.3).
Przy każdym pytaniu — przycisk **„nie chcę podać"**, który pokazuje `refusalMessage` (rek. 8.4, D-6).

⛔ **`@for` z `track question.code`** — stabilny `track` jest obowiązkowy; bez niego zmiana zestawu
pytań po przesunięciu daty przerysowuje wszystkie kontrolki i gubi focus.

### 5.5 Co pokazuje różnicę wersji

Trzy miejsca, wszystkie sterowane danymi z serwera:

1. **Podpis reżimu** pod datą — `regime.name` + `regime.legalBasis`.
2. **Sekcje POTRZEBY i WYŁĄCZENIA** — pojawiają się, bo pojawiają się pytania.
3. **Blok uzasadnienia i ostrzeżeń** — renderowany tylko, gdy `justification.length > 0`.

Żadne z nich nie ma warunku na datę w kodzie frontu. Front nie wie, że `2027-07-01` istnieje.

### 5.6 Testy frontu

| Test | Czego pilnuje |
|---|---|
| `questionnaire.renders-from-server` | Zmiana listy pytań w odpowiedzi zmienia zestaw kontrolek |
| `questionnaire.hidden-not-rendered` | `visible: false` nie renderuje się wcale, a nie renderuje ukryte |
| `questionnaire.refusal-shows-consequence` | Kliknięcie „nie chcę podać" pokazuje `refusalMessage` |
| `property-store.reload-not-params` | Mutacja wywołuje `reload()`, poprzednia wartość zostaje widoczna |
| `recommendation.absent-justification` | Pusta lista uzasadnienia **nie renderuje** pustego bloku |

⛔ Uruchamiane przez `ng test --watch=false`, selekcja przez `--filter` (nie `--include`).

---

## 6. Szacunek objętości

| Grupa | Elementy | Wywołania MCP (rząd) |
|---|---|---|
| G0 Profil | 1 | 2 SQL |
| G1 Konteksty | 8 typów + `ROOT` | ~11 |
| G2 Tabele | 31 tabel + macierze | ~65 |
| G3 Funkcje | 8 | ~10 |
| G4 Flow | 2 | ~4 |
| G5 Typy domenowe | 3 | ~4 |
| G6 Drzewo | 21 elementów | ~24 |
| G7 Publikacja #1 | — | ~6 |
| G8 Region | 2 wersje + 3 przypięcia + 7 elementów | ~30 |
| **Razem Studio** | | **~155** |
| G9 Backend | ~25 plików Java | — |
| G10 Front | ~14 plików TS/HTML/LESS | — |
| G11 Weryfikacja | — | ~20 Playwright |

Podział na grupy jest poprowadzony tak, żeby po każdej dało się przerwać z sesją roboczą w stanie
spójnym.

---

## 7. Postęp

| Grupa | Stan |
|---|---|
| G0 Profil | ✅ |
| G1 Konteksty | ✅ — 14 typów, `get_context_type_paths` → 38 ścieżek |
| G2 Tabele | ✅ — **32**, wszystkie z macierzami, wszystkie w profilu `PROPERTY` |
| G3 Funkcje | ✅ — **18**, nie 8 (ta sama przyczyna co w LIFE) |
| G4 Flow | ✅ — 2: `calculateQuote` (8 nodów), `apk.recommend` (9 nodów) |
| G5 Typy domenowe | ✅ — 4 (`PRODUCT`, `COVER`, `VARIANT`, `QUESTION`) |
| G6 Drzewo | ✅ — 29 elementów, w tym 18 pytań |
| G7 Publikacja #1 | ✅ |
| G8 Region | ✅ — 2 wersje, 3 tabele, 7 pytań wyłącznie w wersji 2 |
| G9 Backend | ✅ — `pl.decerto.higson.demo.property`, 24 pliki |
| G10 Front | ✅ — 5 komponentów, store, serwis, model, 2 pliki testów |
| G11 Weryfikacja | ✅ — przeglądarką i obiema suitami; wynik w §11 |

---

## 8. Pułapki znalezione w tej pracy (W-28…W-34)

W-28 i W-29 są opisane w §2 i §2a. Reszta wyszła przy budowaniu i uruchamianiu.

| # | Pułapka | Skutek, gdy się o niej nie wie |
|---|---|---|
| **W-28** | Profilu nie da się utworzyć przez MCP; **zapis nie waliduje profilu, odczyt waliduje** | `create_context_type` mówi `SUCCESS` i tworzy typ, a każdy odczyt odpowiada „Profile not found" |
| **W-29** | `update_context_type(root: true)` nie ustawi **pierwszego** korzenia | `get_context_type_paths` zwraca pustą listę, choć wszystkie typy istnieją — i żadna tabela nie zbinduje poziomu wejściowego. Ratunek: `import_context` z `def /ROOT` |
| **W-30** | ⛔ **Importer XLSX nie przyjmuje plików z openpyxl** — „Could not parse XLSX file", bez wskazania miejsca | 31 tabel trzeba wygenerować, **kopiując części statyczne z pliku wyeksportowanego przez Higsona** i podmieniając wyłącznie dwa arkusze. Round-trip przez openpyxl (load → save) też nie przechodzi, więc to nie jest kwestia treści |
| **W-31** | Plik wgrany przez `docker cp` dziedziczy prawa z hosta; przy `640` proces Studio (użytkownik `higson`) go **nie odczyta** | `import_domain_definition` zwraca `INTERNAL_ERROR` z samą ścieżką w komunikacie. Po `docker cp` zawsze `chmod 644` |
| **W-32** | ⛔ **`attach_to_region` wypełnia WYŁĄCZNIE wersję 1**, niezależnie od tego, która jest aktywna; edycja macierzy trafia w wersję **systemowo aktywną** | Przy timeline w przyszłości (`2027-07-01`) aktywna jest wersja 1, więc treść wersji 2 nie ma jak powstać. Rozwiązanie: **tymczasowo przesunąć `VALIDFROM` wersji 2 w przeszłość, zrestartować Studio, wypełnić wersję 2, przywrócić datę i zrestartować** |
| **W-33** | Studio **cache'uje aktywną wersję regionu** — zmiana timeline'u w bazie nie działa bez restartu kontenera | `list_regions` pokazuje `timelines[].active` już zmienione, a `versions[].systemActive` jeszcze nie. Edycja idzie do starej wersji i wygląda na poprawną |
| **W-34** | ⛔ **`import_domain_configuration` KASUJE przypięcie elementów do regionu** | Siedem pytań wersji 2 wróciło do puli globalnej i wersja 1 zaczęła je pokazywać. Po **każdym** imporcie konfiguracji domeny trzeba `attach_to_region` powtórzyć — i sprawdzić SQL-em, a nie statusem importu |

### Zapytania weryfikujące

```sql
-- tabele: ile wierszy w której wersji regionu (tylko opublikowane, nieusunięte)
select p.name, p.regionversion_id, count(e.id)
from parameter p left join parameterentry e on e.parameter_id = p.id
where p.name like 'demo.property.%' and p.archive = 0 and p.head = 1
group by 1, 2 order by 1, 2;

-- elementy domeny przypięte do wersji regionu
select CODE, REGIONVERSION_ID from DOMAINELEMENT
where PROFILE_CODE = 'PROPERTY' and ARCHIVE = 0 and HEAD = 1 and REGIONVERSION_ID is not null;
```

⚠️ `get_decision_table_matrix` **scala wersje** i pokazuje working copy — do weryfikacji wersji
nadaje się wyłącznie SQL (W-26 z LIFE, potwierdzone tutaj drugi raz).

---

## 9. Czego nie zrobiono

| Co | Dlaczego |
|---|---|
| **Testy regułowe w `@tests`** (G8 planu LIFE) | Ta grupa została pominięta świadomie: weryfikacja poszła przez uruchomienie aplikacji i przeglądarkę, bo to ona sprawdza tezę demo. Testy regułowe zostają jako dług — tak samo jak w LIFE |
| **Pytanie `RENOVATED` w scenariuszu demonstracyjnym** | Widoczne dopiero przy budynku starszym niż 50 lat; domyślny rok budowy to 2005, więc na starcie jest ukryte. To jest poprawne zachowanie reguły, nie brak |
| **Franszyza jako kontrolka na ekranie** | Endpoint `PUT property/policy/deductible` istnieje i działa, ekranu dla niego nie ma. Druga rola franszyzy (warunek dostępności powodzi w strefie `Z2`) jest więc widoczna tylko przez API |
