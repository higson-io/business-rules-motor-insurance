# Profil LIFE — plan implementacji w Higson Studio

**Studio:** 4.3.1, `http://localhost:38088/studio/` (kontener `higson-demo-local-studio`)
**Źródło projektu:** `.maister/docs/project/higson-life-profile.md` (decyzje D-1…D-7)
**Stan wyjściowy:** profil `LIFE` istnieje i jest **pusty** — domena tylko `ROOT`, kontekst tylko
`ROOT`, zero regionów, zero reguł.

---

## 1. Model izolacji — zweryfikowany, nie założony

To jest fundament całego planu, więc sprawdziłem każdą warstwę osobno przez MCP:

| Warstwa | Zakres | Dowód | Ryzyko dla DEMO |
|---|---|---|---|
| Typy domenowe | **per profil** | `get_domain_definition(LIFE)` → tylko `ROOT`; DEMO ma pełne drzewo | **żadne** |
| Drzewo domeny | **per profil** | jw. | **żadne** |
| Typy kontekstu | **per profil** | `list_context_types(LIFE)` → tylko `ROOT`; DEMO ma 8 typów | **żadne** |
| Regiony | **per profil** | `list_regions(LIFE)` → `[]` | **żadne** |
| Tabele decyzyjne | **globalne** + przypisanie | `list_decision_tables(LIFE)` zwróciło 40 tabel MOTO i telco | ⚠️ **realne** |
| Funkcje, flow | **globalne** + przypisanie | jw., istnieje `assign_to_profile` | ⚠️ **realne** |

**Wniosek, który steruje całą resztą planu.** Domena i kontekst izolują się same — cokolwiek
zrobimy w profilu `LIFE`, nie da się tym dotknąć DEMO. Ale **reguły żyją w jednej globalnej
przestrzeni nazw**: `profileCode` przy tworzeniu tabeli jest opcjonalny, a listowanie po profilu
zwraca wszystko. Izolacja reguł nie jest automatyczna — bierze się z dwóch rzeczy naraz:

1. **Prefiks kodu `demo.life.*`** — zweryfikowany jako wolny (0 trafień wśród 40 istniejących).
2. **Jawne przypisanie do `LIFE`** po utworzeniu.

### ⛔ Jedna operacja, która może zepsuć DEMO

`higson_assign_to_profile` z parametrem `currentProfileCode` **przenosi** element między profilami,
a nie kopiuje. Wywołanie go na jakimkolwiek kodzie spoza `demo.life.*` zabierze tę regułę z DEMO.

> **Zasada bezwzględna dla całej implementacji:** `assign_to_profile` wolno wywołać **wyłącznie**
> z listą kodów zaczynających się od `demo.life.` i **bez** `currentProfileCode` (nasze elementy
> nie należą jeszcze do żadnego profilu). Każde wywołanie z `currentProfileCode` jest błędem
> planu, nie decyzją wykonawczą.

Zakazane są też: `delete_*` na czymkolwiek spoza `demo.life.*`, `update_*` na istniejących
tabelach, `delete_all_domain_types`, oraz `update_domain_type` z `profileCode: DEMO`.

### Sesja robocza jako siatka bezpieczeństwa

`list_work_sessions` → obecnie **brak otwartych sesji**. Wszystkie zmiany trafiają do sesji
i wymagają `publish_changes`. To znaczy, że:

- do momentu publikacji runtime widzi **stary stan** — DEMO działa bez zmian nawet w połowie prac,
- `reject_changes` jest pełnym rollbackiem bez grzebania w bazie,
- publikujemy **raz, na końcu**, po weryfikacji — nie po każdej grupie.

---

## 2. Poprawki do dokumentu projektowego wymuszone przez API

Trzy rzeczy w `higson-life-profile.md` nie przechodzą wprost przez API i trzeba je poprawić
**przed** tworzeniem, żeby dokument i Studio się nie rozjechały.

| # | Problem | Poprawka |
|---|---|---|
| P-1 | Macierze przedziałowe zapisane jako `18-29` | API wymaga **`18 - 29`** (spacja-myślnik-spacja) dla każdej kolumny `between/*`. Dotyczy 9 tabel |
| P-2 | Dokument mówi `cacheable = true` / `nullable = false` | W API to `inMemory` (domyślnie `true`) i `matchRequired` (domyślnie `true`). Nasze wiersze `*` sprawiają, że `matchRequired=true` nigdy nie wybucha — zostaje domyślnie |
| P-3 | Typ `boolean` w macierzach zapisany jako `yes`/`no` | Do potwierdzenia przy pierwszej tabeli — jeśli API wymaga `true`/`false`, poprawiamy dokument, a nie obchodzimy typem `string` |

---

## 3. Kolejność grup — wymuszona zależnościami

```
G1 Konteksty ──► G2 Tabele ──► G3 Funkcje ──► G4 Flow ──► G5 Typy domenowe
                    │                            │              │
                    └────────────────────────────┴──────────────┘
                                    │
                            G6 Drzewo domeny ──► G7 Region i wersje ──► G8 Testy ──► G9 Publikacja
```

Powód każdej strzałki: tabele odwołują się do ścieżek kontekstu; funkcje czytają tabele
(`ageRatingFactor`, `resolveDecision`); flow wołają tabele i funkcje; **atrybuty typów domenowych
wskazują na parametry i flow**, więc muszą powstać po nich; elementy domeny wymagają typów; region
przypina istniejące już tabele.

---

## G1 — Typy kontekstu (5 typów)

Kontekst jest **kontraktem z kodem Java**, więc odwzorowujemy §3 dokumentu co do znaku.

| Typ | Atrybuty |
|---|---|
| `Insured` | `birthDate` date, `entryAge` integer, `smoker` boolean, `heightCm` integer, `weightKg` integer, `bmi` number, `occupationClass` integer, `monthlyIncome` number, `priorCoverFrom` date, `priorCoverTo` date, `priorCoverMonths` integer, `priorCoverGapDays` integer |
| `Policy` | `startDate` date, `mainSum` number, `paymentFrequency` string |
| `GroupData` | `headcount` integer, `averageAge` integer, `industryClass` integer, `participation` number, `includeSpouse` boolean, `includeAdultChild` boolean, `topVariantShare` number, `enrolment` → `Enrolment` multiValue |
| `Enrolment` | `variantCode` string, `count` integer |
| `Quote` | `insured` → `Insured`, `policy` → `Policy`, `group` → `GroupData`, `lineCode` string |

Do `ROOT` dokładamy: `quote` → `Quote`, oraz `rider`/`variant`/`benefit` (kod + suma) i `claim.cause`.

> **Do potwierdzenia (W-7).** W profilu DEMO tabele używają źródeł `coverage.code` i `option.code`,
> mimo że `ROOT` ma tylko `quote` — silnik wystawia bieżący element iteracji pod nazwą typu.
> Zanim utworzymy 29 tabel, robimy **jedną** tabelę próbną ze źródłem `rider.code` i sprawdzamy
> w Testerze, czy się binduje. Jeśli nie — zmieniamy ścieżki na zagnieżdżone
> (`quote.riders.code`) w dokumencie **i** w planie, zanim powstanie reszta.

`gmoPersistenceTriggerType` zostawiamy domyślne (`NO`) — LIFE nie persystuje przez GMO, w
przeciwieństwie do DEMO, gdzie `Driver` ma mapowanie na kolumny. Nic tego nie potrzebuje, dopóki
front działa na mockach.

---

## G2 — Tabele decyzyjne (29)

Kolejność wewnątrz grupy jest dowolna — tabele nie zależą od siebie. Podział na podgrupy służy
weryfikacji, nie zależnościom.

| Podgrupa | Tabele | Liczba |
|---|---|---|
| G2.1 Wspólne | `productLine.availability`, `productLine.position`, `paymentFrequency.factor` | 3 |
| G2.2 Taryfa indywidualna | `individual.tariff.baseRate`, `individual.rider.rate`, `individual.factor.{smoker,occupation,bmi}` | 5 |
| G2.3 Konfiguracja umów | `individual.rider.{availability,sumsOffered,benefitKind,position}`, `individual.waitingPeriod.{base,credit}`, `individual.limit.{againstMainSum,againstIncome}` | 8 |
| G2.4 Underwriting | `underwriting.{rules,customerMessage,sumLimit,medicalExamThreshold}` | 4 |
| G2.5 Grupowe — konfiguracja | `group.benefit.{availability,sumAssured,benefitKind,position}`, `group.variant.{availability,position}`, `group.eligibility` | 7 |
| G2.6 Grupowe — taryfa | `group.tariff.basePremium`, `group.factor.{averageAge,industry,headcount,participation,variantMix,familyExtension}` | 7 |

**Uwaga do G2.5:** `group.benefit.availability` ma 27 wierszy, `sumAssured` 25 — najbliżej sufitu
z §11 dokumentu. Tworzymy je dokładnie tak, jak są w dokumencie, bez skracania.

**Po każdej podgrupie:** `list_decision_tables(filterCode='demo.life')` i sprawdzenie, że licznik
rośnie o tyle, ile utworzyliśmy, oraz że **nie zmienił się licznik całkowity spoza `demo.life.*`**
(ma zostać 40).

**Na koniec G2:** `assign_to_profile(type=DECISION_TABLE, codes=[29 kodów demo.life.*],
newProfileCode='LIFE')` — bez `currentProfileCode`.

---

## G3 — Funkcje Groovy (8)

`util.entryAge`, `util.bmi`, `util.ageRatingFactor`, `util.insuredCount`, `util.priorCoverSpan`,
`underwriting.resolveDecision`, `group.enrolmentStats`, `group.programCost`.

Ciała są w §6 dokumentu i mieszczą się w limicie (max 44 linie). Dwie rzeczy do sprawdzenia przy
pierwszym uruchomieniu:

- `type.paramValue()` builder — używają go trzy funkcje (`resolveDecision`, `enrolmentStats`,
  `priorCoverSpan`) do zwrócenia wielu wartości. Weryfikujemy na `util.priorCoverSpan`, bo jest
  najprostsza.
- `domain.get('LIFE', ...)` w `enrolmentStats` — **wymaga istniejącego drzewa domeny**, którego
  na tym etapie jeszcze nie ma. Dlatego tę jedną funkcję testujemy dopiero po G6.

Przypisanie do profilu na koniec grupy, tą samą regułą co w G2.

---

## G4 — Flow (6)

`individual.calculateQuote`, `individual.calculateRiderPremium`, `individual.maxSumAssured`,
`individual.effectiveWaitingPeriod`, `underwriting.evaluate`, `group.calculateQuote`.

Kolejność tworzenia od najgłębszych: `maxSumAssured` i `effectiveWaitingPeriod` → `evaluate` →
`calculateRiderPremium` → `calculateQuote` (ind.) → `calculateQuote` (grp.), bo flow wołają flow.

Węzły są w §5 dokumentu (max 13/15). Diagramy w dokumencie są **logiczne, nie eksportowalne** —
w Studio budujemy je nodami z akcjami `DECISION_TABLE` / `FUNCTION` / `FLOW`, zgodnie z formatem
widocznym w `demo.motor.plan.full.calculate.premium`.

---

## G5 — Typy domenowe (4)

`PRODUCT_LINE`, `RIDER`, `VARIANT`, `BENEFIT` — atrybuty z §2.2 dokumentu, każdy z `defaultValue`
wskazującym na parametr albo flow utworzony w G2/G4.

Uwaga na `MAIN_COVER`: zgodnie z przeglądem rynkowym umowa główna **nie jest** elementem kolekcji
`RIDERS`, tylko osobnym komponentem `PRODUCT_LINE`. To upraszcza `RIDER`, bo znika pytanie
o dostępność umowy obowiązkowej.

---

## G6 — Drzewo domeny (~25 elementów)

```
/PRODUCT_LINES[INDIVIDUAL]/MAIN_COVER
/PRODUCT_LINES[INDIVIDUAL]/RIDERS[CRITICAL_ILLNESS | ACCIDENTAL_DEATH | PERMANENT_IMPAIRMENT
                                  | HOSPITAL_STAY | SURGERY | DISABILITY | MEDICAL_ASSISTANCE]
/PRODUCT_LINES[GROUP]/VARIANTS[BASIC | EXTENDED | COMPLETE]
/PRODUCT_LINES[GROUP]/BENEFITS[12 świadczeń — bez MENTAL_HEALTH_SUPPORT]
```

`MENTAL_HEALTH_SUPPORT` **nie powstaje tutaj** — jest elementem wersji 2 regionu i tworzy się go
w G7. To jest cała demonstracja D-4, więc pomyłka w tym miejscu kosztuje najciekawszy scenariusz.

Po G6: uruchomienie `group.enrolmentStats` w Testerze — pierwsze wywołanie `domain.get` na
realnym drzewie.

---

## G7 — Region `LIFE` i dwie wersje

1. Region `LIFE`, wersja 1 „Taryfa 2026 H1", timeline `2026-01-01`.
2. Wersja 2 „Taryfa 2026 H2", timeline `2026-07-01`.
3. `attach_to_region` dla **sześciu** tabel: `individual.tariff.baseRate`,
   `individual.rider.rate`, `underwriting.medicalExamThreshold`, `group.tariff.basePremium`,
   `group.factor.participation`, `group.factor.variantMix`.
4. Macierze wersji 2 wg §7.3 (cztery zmiany).
5. Element domeny `MENTAL_HEALTH_SUPPORT` w wersji 2 regionu + jego wiersze w czterech tabelach
   `group.benefit.*` — **globalne, nie wersjonowane** (D-4).

---

## G8 — Testy regułowe (38)

Z §9 dokumentu. Priorytet, gdyby zabrakło czasu — te, które pilnują decyzji, a nie liczb:

| Test | Czego pilnuje |
|---|---|
| `underwriting.refer.pricesQuote` | D-1: `REFER` wraca z niezerową składką i `binding=false` |
| `underwriting.multiRow` | D-1: cztery przesłanki naraz → `DECLINE`, niezależnie od kolejności wierszy |
| `group.phases.consistent` | D-2: obie fazy dają identyczne `perEmployee` |
| `individual.cap.doesNotEmptyList` | D-3: przy najniższej sumie głównej każda umowa ma ≥1 wybieralną sumę |
| `version.shape` | D-4: 12 świadczeń przed `2026-07-01`, 13 po |
| `version.selective` | D-4: przedział 40-44 identyczny po obu stronach daty |
| `installment.sumsUp` | D-6: roczna = rata × liczba rat, dokładnie |

---

## G9 — Weryfikacja i publikacja

**Przed publikacją, obowiązkowo:**

1. `list_decision_tables()` bez filtra → **69 tabel** (40 istniejących + 29 nowych). Jeśli mniej niż
   40 spoza `demo.life.*` — coś zostało przeniesione, **nie publikujemy**.
2. `get_domain_definition(DEMO)` → drzewo MOTO bez zmian.
3. `list_context_types(DEMO)` → nadal 8 typów.
4. `find_usages` na dwóch losowych tabelach MOTO → referencje bez zmian.
5. `list_work_sessions` → przegląd pełnej listy zmian; **każdy element musi mieć kod
   `demo.life.*` albo należeć do profilu `LIFE`**. Jeden obcy wpis = `reject_changes`.
6. Dopiero wtedy `publish_changes(confirmed=true)` — po pokazaniu listy użytkownikowi.

**Rollback:** `reject_changes` do momentu publikacji. Po publikacji rollback to usunięcie
elementów `demo.life.*` i profilu — nadal bez dotykania DEMO, bo nic wspólnego nie powstało.

---

## 4. Szacunek objętości

| Grupa | Elementy | Wywołania MCP (rząd) |
|---|---|---|
| G1 Konteksty | 5 typów + ROOT | ~7 |
| G2 Tabele | 29 tabel + macierze | ~60 |
| G3 Funkcje | 8 | ~10 |
| G4 Flow | 6 | ~8 |
| G5 Typy domenowe | 4 | ~5 |
| G6 Drzewo | ~25 elementów | ~28 |
| G7 Region | 2 wersje + 6 przypięć + 1 element | ~15 |
| G8 Testy | 38 | ~40 |
| **Razem** | | **~175** |

To jest realna praca na kilka sesji, nie na jedno wywołanie. Podział na grupy jest tak
poprowadzony, żeby po każdej dało się przerwać z sesją roboczą w stanie spójnym.

---

## 5. Ustalenia z wykonania

### Rozstrzygnięte

| # | Pytanie | Odpowiedź |
|---|---|---|
| W-7 | Czy `rider.code` zbinduje się bez zagnieżdżenia? | **Tak.** `get_context_type_paths(DEMO)` też nie listuje `coverage.code` ani `option.code`, a siedem produkcyjnych tabel MOTO ich używa. Lista ścieżek pokazuje wyłącznie statyczne drzewo; element iteracji adresuje się nazwą typu. **Bez zmian w dokumencie** |
| W-8 | `yes`/`no` czy `true`/`false`? | **`true`/`false`.** Dokument wymaga poprawki w 8 tabelach z kolumną boolean |
| P-1 | Format przedziałów | Potwierdzony: `18 - 29` ze spacjami. Działają też półotwarte z wildcardem: `* - 18.5`, `32 - *` |
| — | Tablice w macierzy | Zapisywane jako `50000,100000,200000` w jednej komórce |

### Nowe, znalezione w trakcie

| # | Ustalenie | Konsekwencja |
|---|---|---|
| **W-10** | **Nie istnieje `util.ctx.set`** — jest tylko `util.ctx.get`. Flow **nie potrafi zapisać** do kontekstu | Każda tabela kluczowana **wyliczoną** wartością musi być czytana z funkcji przez `higson.createContext`, a nie z flow. Dotyczy `underwriting.customerMessage`: odczyt przenosi się z flow do `resolveDecision`. Treść zostaje w tabeli, więc D-1 obowiązuje. **Do naniesienia w §5.5 dokumentu** |
| **W-11** | `list_decision_tables` pokazuje wyłącznie stan **opublikowany** — w trakcie otwartej sesji zwraca 0 dla nowych kodów | Weryfikacja licznikiem z G9 nie działa w trakcie prac. Stan sesji sprawdza się przez `list_work_sessions` |
| **W-12** | Typy kontekstu **nie trafiają do sesji roboczej** — powstają natychmiast | Rollback przez `reject_changes` **nie cofnie** typów kontekstu. Dla LIFE nieszkodliwe (profil był pusty), ale przy zmianach w istniejącym profilu byłoby to nieodwracalne bez ręcznego cofnięcia |
| — | Liczba tabel: **34, nie 29** | Dokument definiuje nagłówkiem 29, ale odwołuje się jeszcze do pięciu tabel `position`/`kind` wymienionych tylko w układzie plików. Wszystkie utworzone |

### Otwarte

| # | Pytanie | Blokuje |
|---|---|---|
| W-9 | Czy `attach_to_region` działa na tabelę już przypisaną do profilu? | G7 |
| W-1 | Czy wiersz z częściowym wildcardem ma pierwszeństwo nad `*`? | Jeśli tak, `waitingPeriod.credit` skraca się z 7 do 4 wierszy — do sprawdzenia w Testerze po publikacji |

---

## 6. Postęp

Stan na **2026-08-20**, zweryfikowany w Studio i uruchomieniem aplikacji, nie z planu.

| Grupa | Stan | Elementy |
|---|---|---|
| G1 Konteksty | ✅ | 9 typów + `ROOT` |
| G2 Tabele | ✅ | **34 tabele**, wszystkie z macierzami |
| G3 Funkcje | ✅ | **22** — więcej niż zakładane 8, bo brak `EXPRESSION` przeniósł arytmetykę z flow do funkcji |
| G4 Flow | ✅ | **2**: `individual.calculateQuote`, `group.calculateQuote`. Pozostałe cztery są funkcjami — odstępstwo opisane niżej |
| G5 Typy domenowe | ✅ | 4 |
| G6 Drzewo | ✅ | pełne, obie linie produktu |
| G7 Region | ✅ | 2 wersje, 6 tabel, obie wersje wypełnione (naprawa 2026-08-20, §10) |
| G8 Testy | ⬜ | 0 z 38 — jedyna grupa nietknięta |
| G9 Publikacja | ✅ | brak otwartych sesji, runtime widzi całość |

**Aplikacja** (poza zakresem tego planu, ale to ona weryfikuje reguły): backend `pl.decerto.higson.demo.life`
i ekrany SPA działają na Higsonie, obie ścieżki liczą się end-to-end. Otwarte: `programCost` /
`insuredCount` (druga faza D-2 — brak endpointu na rozkład przystąpień) i składka per umowa dodatkowa.

## 7. Rozdzielenie profili — wykonane

Na prośbę użytkownika elementy motoryzacyjne dostały profil. **Nie utworzono profilu `MOTO`** —
`Motor.java:60` ma zaszyte `engine.getDomain("DEMO", ...)`, więc nowy profil zerwałby to powiązanie.
Użyto istniejącego `DEMO`.

| Przypisane do `DEMO` | Ile |
|---|---|
| Tabele `demo.motor.*` | 13 bezpośrednio + **3 aliasy, które poszły za masterem samoczynnie** |
| Funkcje `demo.motor.*` | 6 |
| Flow `demo.motor.plan.full.calculate.premium` | 1 |

**Świadomie nietknięte:** 16 tabel i 23 funkcje telco, 5 słowników `dictionary.*`, `util.*`
(współdzielone przez flow MOTO), `system.function.logging.level`, `upmd.risk.table`,
`vegas.discount.value`. Publikowanych tabel zostało **24** — dokładnie tyle, ile powinno.

**W-13:** aliasu nie da się przypisać osobno — API zwraca `CANNOT_CHANGE_PROFILE_ELEMENT_IS_ALIAS`
i każe zmienić profil na masterze. To dobra walidacja, ale trzeba o niej wiedzieć przy planowaniu list.

**W-14:** przypisanie profilu trafia do sesji jako `UPDATE`, czyli **jest odwracalne** — ale sesja
jest jedna i wspólna, więc `reject_changes` cofnąłby też wszystkie 42 elementy LIFE.
Wycofanie samego przypisania robi się przez `detach_from_profile`, nie przez reject.

---

## 8. Co jest zbudowane, a czego jeszcze nikt nie uruchomił

⚠️ **Żaden element nie został wykonany.** Wszystko jest w sesji 4852, nieopublikowane, więc runtime
go nie widzi. Poprawność składniowa jest potwierdzona przez API, **poprawność działania nie**.

Założenia przyjęte przy budowie flow, do zweryfikowania w Testerze zaraz po publikacji:

| # | Założenie | Gdzie użyte | Co jeśli błędne |
|---|---|---|---|
| **W-15** | Akcja `EXPRESSION` działa i wykonuje wyrażenia Groovy | 3 flow, ~15 węzłów | Przepisanie węzłów na funkcje Groovy |
| **W-16** | Selektor kolumny `tabela [kolumna]` działa w `value` węzła `DECISION_TABLE` | `maxSumAssured`, 4 węzły | Rozbicie tabel dwukolumnowych na jednokolumnowe |
| **W-17** | `resolved.get(0,'decision')` czyta `ParamValue` w wyrażeniu flow | `underwriting.evaluate`, 4 węzły | Zwracanie wartości skalarnych z funkcji zamiast `ParamValue` |
| **W-18** | `type.paramValue()` builder zwraca wielowartościowy wynik | 3 funkcje | Rozbicie na osobne funkcje jednowartościowe |
| **W-19** | Flow nie ma akcji rzucającej wyjątek | `calculateRiderPremium` — jeszcze nie zbudowany | Dziewiąta funkcja Groovy do walidacji pułapu (D-3 wymaga błędu, nie cichego przycięcia) |

**Decyzja wykonawcza:** przerywam budowę pozostałych trzech flow do czasu weryfikacji W-15…W-18.
Trzy zbudowane flow opierają się na tych samych założeniach; budowanie kolejnych trzech przed
sprawdzeniem oznaczałoby, że jeden błędny wniosek trzeba naprawiać w sześciu miejscach zamiast
w trzech.

---

## 9. Pułapki API odkryte przez uruchamianie (W-15…W-24)

Żadnej z nich nie dało się przewidzieć z dokumentacji. Wszystkie potwierdzone testem w Studio.

| # | Pułapka | Skutek, gdy się o niej nie wie |
|---|---|---|
| **W-15** | ⛔ **`EXPRESSION` w flow jest eksperymentalne i nieobsługiwane przez GUI** | Flow nie liczy nic sam. Arytmetyka i rozpakowanie wielowartościowego wyniku muszą iść do funkcji |
| **W-16** | Selektor `[kolumna]` działa **wyłącznie** w atrybutach domeny (`$p kod [kol]`), nie w API Groovy | `higson.getString('tabela [kol]', ctx)` → `Parameter not found in user perspective` |
| **W-17** | `ctx.get()` zwraca daty jako **String** | `date.diff` dostaje `(String,String,String)` i wybucha |
| **W-18** | **`date.diff(Date,Date,String)` nie istnieje** mimo dokumentacji | `MissingMethodException`. Rozwiązanie: natywne odejmowanie `Date` w Groovym daje dni, bez importów |
| **W-19** | `higson.createContext` **nie konwertuje typów** | `Double` pod poziom `integer` → `IllegalArgumentException` w `IntegerType.convert` |
| **W-20** | Funkcja z argumentami musi **jawnie zadeklarować `ctx`** jako pierwszy argument `SPECIFIED_CLASS` / `io.higson.runtime.engine.core.context.ParamContext` | Inaczej `ctx` przestaje być kontekstem i `getValue(kod, ctx)` czyta go jako pary klucz-wartość |
| **W-21** | Atrybut domeny: `defaultValue` to **goły kod**, prefiks dokłada `defaultValueType` | `$p kod` w `defaultValue` → silnik szuka parametru o nazwie zawierającej `$p ` |
| **W-22** | Nawigacja po domenie **nie wstrzykuje kodu elementu do kontekstu** | `domain.get(...).getAttrDecimal('POSITION', ctx)` nie ma z czego rozwiązać atrybutu kluczowanego `variant.code` |
| **W-23** | ⛔ **`open_decision_table` jest OBOWIĄZKOWE przed edycją macierzy** | Bez tego identyfikatory wierszy pochodzą z opublikowanego HEAD, `DELETE` **cicho nie robi nic**, a `CREATE` w tym samym wywołaniu działa → duplikaty |
| **W-24** | Przypięcie elementu domeny do regionu wymaga **opublikowanej sesji** | `CANNOT_ATTACH_TO_REGION_IF_WORK_SESSION_IS_ACTIVE` |

### Reguła dopasowania wierszy — ustalona empirycznie

**Wiersz `* - *` dopasowuje się RÓWNOLEGLE z właściwym pasmem** i silnik zwraca obie wartości;
`getDecimal` bierze pierwszą, przy niegwarantowanej kolejności.

Dotyczy **wyłącznie tabel, w których wszystkie kolumny wejściowe są przedziałowe.** Kolumna
dopasowywana dokładnie (np. `rider.code`) neutralizuje problem — sprawdzone:

| Tabela | Kolumny | Wynik |
|---|---|---|
| `rider.availability` | string + przedział | jedna wartość ✅ |
| `variant.availability` | string + przedział | jedna wartość ✅ |
| `eligibility` | dwa przedziały | **dwie sprzeczne wartości** ❌ |
| `factor.bmi` | jeden przedział | **dwie wartości** ❌ |

⚠️ **Częściowe wildcardy (`* - 50`, `75 - *`) działają poprawnie** jako otwarte granice — `75 - *`
nie dopasowało się do 60. Początkowo zdiagnozowałem to odwrotnie i niepotrzebnie przepisałem sześć
tabel; korekta w §4 dokumentu projektowego.

**Wniosek dla konwencji:** w tabeli o samych kolumnach przedziałowych **nie dawać wiersza `* - *`**;
zamiast tego `matchRequired = false` i obsługa `null` w funkcji.

### Wersjonowanie — co działa, a co nie

- ✅ Wersje wypełniają się poprawnie: `baseRate` dla wieku 50 zwraca `0.75` (v1) i `0.81` (v2)
- ✅ `attach_to_region` kopiuje macierz do każdej wersji; późniejsze `UPDATE` trafia w wersję aktywną
- ⚠️ `get_decision_table_matrix` **scala wersje** i nie ma kolumny wersji — łatwo wziąć to za duplikaty
- ⛔ **Tester nie stosuje timeline'u** — odpytuje w poprzek wersji i bierze pierwszą wartość.
  Demonstracja różnicy między wersjami wymaga runtime'u albo przełącznika wersji w UI Studio.
  **To unieważnia scenariusze z §7.4 dokumentu**, które zakładały wybór wersji przez
  `quote.policy.startDate`

### Odstępstwa od dokumentu projektowego

| Co | Dlaczego |
|---|---|
| `underwriting.evaluate`, `individual.effectiveWaitingPeriod`, `individual.maxSumAssured` są **funkcjami**, nie flow | Bez `EXPRESSION` flow nie rozpakuje wielowartościowego wyniku; a `effectiveWaitingPeriod` kluczuje tabelę wyliczonymi miesiącami i przerwą, czego flow nie poda (brak `ctx.set`) |
| `underwriting.customerMessage` czytany w `resolveDecision`, nie we flow | jw. — brak `ctx.set` |
| Obie linie produktu widzą wszystkie cztery kolekcje | Higson nie pozwala na komponenty warunkowe per element. `INDIVIDUAL` ma puste `VARIANTS`/`BENEFITS`, `GROUP` puste `MAIN_COVER`/`RIDERS`. Wycena działa, ale drzewo nie oddaje §2 co do znaku |
| Minimalna składka jako tabela `individual.minimumPremium` | MOTO trzyma literał we flow; u nas parametr, żeby zmiana progu nie wymagała edycji algorytmu |


---

## 10. Naprawa wersji regionu (2026-08-20)

Po pierwszym uruchomieniu okazało się, że sześć tabel przypiętych do regionu ma **rozjechane wersje**:
`medicalExamThreshold` v1=0 wierszy, `rider.rate` v2=0, a `baseRate` v2=14 zamiast 9 (stare i nowe
pasma obok siebie). Skutek: każda data polisy przed `2026-07-01` kończyła się `PARAM_VALUE_NOT_FOUND`.

Przyczyny są dwie i obie są już opisane wyżej jako pułapki — tyle że zadziałały po cichu:
`attach_to_region` kopiuje macierz do **jednej** wersji, a edycja bez `open_decision_table` (W-23)
**dokłada** wiersze zamiast je podmienić.

**W-25 — MCP nie ma narzędzia przełączającego aktywną wersję regionu.** Edycja macierzy zawsze trafia
w wersję aktywną dla użytkownika, więc do wersji 1 wchodzi się okrężnie, per tabela:

1. `detach_from_region` — zostaje kopia bez regionu, z zawartością wersji **aktywnej**
2. `open_decision_table` + podmiana macierzy na treść **wersji 1**
3. `attach_to_region` z `versions: ["1","2"]` — kopia ląduje w wersji 1, wersja 2 zostaje pusta
4. `open_decision_table` + zapis treści **wersji 2** — trafia w wersję aktywną

**W-26 — `get_decision_table_matrix` scala wersje**, więc do weryfikacji nadaje się wyłącznie SQL:
`select p.regionversion_id, p.head, count(e.id) from parameter p left join parameterentry e on e.parameter_id = p.id where p.name = '...' group by 1, 2`.

Wynik po naprawie i publikacji: wiek 52 → **225 zł** dla daty marcowej i **243 zł** dla sierpniowej
(+8% zgodnie ze zmianą 1), a ścieżka grupowa pokazuje **12 świadczeń** przed 1 lipca i **13** po.

**W-27 — domenowa połowa D-4 nigdy nie była zepsuta.** `MENTAL_HEALTH_SUPPORT` był poprawnie przypięty
do wersji 2; to Java czytała katalog grupowy raz, przy tworzeniu sesji, zanim `setEffectiveDate` ustawiło
datę polisy. Odczyty katalogu muszą siedzieć w ścieżce wyceny, nie w konstruktorze wniosku.
