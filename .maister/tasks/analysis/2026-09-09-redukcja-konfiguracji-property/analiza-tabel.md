# Które tabele decyzyjne PROPERTY da się usunąć (2026-09-09)

Analiza pięciu analityków i pięciu recenzentów, każdy odcinek weryfikowany przez drugą osobę
z zadaniem **obalenia**, nie potwierdzenia. Stan wejściowy: **31 tabel decyzyjnych, 19 funkcji
Groovy, 3 flow** w profilu `PROPERTY`.

Warunek właściciela: **usunąć, nie zastąpić funkcją ani flow**. Dozwolone zamienniki: kolumna
w istniejącej tabeli, stała `LITERAL` na elemencie domeny (D-23), wycięcie wymiaru.

## Wynik w jednym zdaniu

**Sześć tabel, nie dziesięć.** Dziesiątki w tej konfiguracji nie ma i to jest wynik, a nie porażka:
po §30, §32 i §33 większość z 31 tabel to jedna reguła w jednym pliku. Luźnych słowników do
wchłonięcia już nie zostało.

## Zero martwych artefaktów regułowych

Wszystkie 31 tabel, 19 funkcji i 3 flow mają co najmniej jedną żywą ścieżkę użycia — sprawdzone
czterema drogami (atrybuty domeny, flow, Groovy, Java) i potwierdzone niezależnie.

⚠️ **Dwa artefakty żyją WYŁĄCZNIE przez `engine.call` z Javy** — `demo.property.zone.resolve`
i `demo.property.util.buildingAge` — a `higson_find_usages` pokazuje dla nich **zero**, bo nie
przeszukuje kodu Java. Ktoś sprzątający konfigurację samym narzędziem Studia skasowałby obie.

## Sześć tabel do usunięcia, w kolejności pewności

| # | Tabela | Co wchodzi w jej miejsce | Ekran | Pewność |
|---|---|---|---|---|
| 1 | `factor.deductible` | wycięcie wymiaru: węzły `n6`/`n7` z `risk.calculate`, `n5`→`n8` | bez zmian | **wysoka** |
| 2 | `factor.buildingAge` | wycięcie wymiaru: węzły `n8`/`n9`, `n7`→`n10` | bez zmian w typowym przebiegu | **wysoka** |
| 3 | `apk.question.blocker` | dwie kolumny w katalogu `apk.question` | bez zmian | **wysoka** |
| 4 | `offer.choiceKind` | kolumna `kind` w `offer.choice`, wejście `delta` przez istniejące `$f` | bez zmian | średnia — najpierw naprawa W-30 |
| 5 | `zone.theft` | scalenie z `zone.flood` w `demo.property.zone` | bez zmian | średnia — 16 obszarów do przepisania ręcznie |
| 6 | `sum.valueGap` | usunięcie bloku `if` w Javie | bez zmian w typowym przebiegu | wysoka technicznie, **ale traci funkcję** |

### 1. `factor.deductible` — martwa, bo front nigdy nie woła endpointu

Najmocniejsze znalezisko. `PropertyService.updateDeductible()` (`property-service.ts:50`) **nie ma
ani jednego wywołania** w całym `frontend/src` — franszyza jest tylko wyświetlana
(`offer.ts:110`). Jedyny `setDeductible` w Javie to `PropertyApi.java:85`, martwy z tego samego
powodu. Wejście tabeli zawsze wynosi `0` i trafia w wiersz `0;1.00`, identyczny z `*`; trzy z pięciu
wierszy są nieosiągalne.

⚠️ Sprawdzona i **obalona** hipoteza ratunkowa: `cover.flood` ma kolumnę `minDeductible`, ale ta
wartość idzie wyłącznie do zdania uzasadnienia (`Property.java:957-963`), nigdy do wniosku.

⛔ To **odbiera połowę D-8** („franszyza w dwóch rolach"). Druga rola — franszyza narzucona przy
powodzi — żyje dalej w `cover.flood`. Pierwsza, obniżka składki za dobrowolną franszyzę, przestaje
istnieć w konfiguracji, bo i tak nie istniała w aplikacji.

### 2. `factor.buildingAge` — neutralna w domyślnym przebiegu

`nullable = true`, bez wiersza `*`, a `demo.util.multiply` traktuje brak wartości jak jedynkę —
opis flow mówi o tym wprost, więc wycięcie jest zgodne z projektem, nie obejściem go. Domyślny wiek
budynku (21 lat) trafia w przedział o współczynniku **1,00**.

⛔ **`factor.claims` NIE jest jej odpowiednikiem, choć wygląda identycznie.** Domyślna szkodowość
`0` trafia w wiersz **0,90**, czyli realną zniżkę widoczną dziś w składce. Usunięcie podniosłoby
cenę na ekranie. „Nie pada" to nie to samo co „nic się nie zmienia" — to jest jedyna różnica między
tymi dwiema tabelami i cała analiza strukturalna jej nie widzi.

### 3. `apk.question.blocker` — dwie kolumny w katalogu pytań

Trzy warianty, wygrywa trzeci:

| Wariant | Werdykt |
|---|---|
| `LITERAL` na elemencie pytania | ⛔ wymaga `update_domain_type` na typie `QUESTION` — **W-29**, `SET_TO_ALL` czyści `KIND`/`CONTEXT_PATH` wszystkim pytaniom |
| wchłonięcie przez `apk.answer` | ⛔ zmusza 12 wierszy o innej odpowiedzialności do niesienia pustych kolumn blokady; **D-21** świadomie rozdzielił `ANSWER_MAP` i `BLOCKER` |
| **dwie kolumny w `apk.question`** | ✅ katalog już trzyma „wszystko, co o pytaniu stałe", ma bliźniaczy wzorzec `revealedBy`/`revealedWhen`, omija W-29 |

Trzeci wariant to wprost zapisany standard: *„where a catalogue table already exists whose declared
job is 'one row per X', a new per-X constant goes there as a column"*.

### 4. `offer.choiceKind` — najpierw naprawa, potem scalenie

⚠️ **W-30 siedzi już dziś w tej tabeli, niezależnie od scalenia.** Pierwszy wiersz to `* - -1`
z otwartym dolnym końcem, a `choiceDelta.groovy` realnie zwraca `null` w dwóch miejscach. Komentarz
w `.def` twierdzi, że null nie pasuje do żadnego wiersza — to stoi w sprzeczności z udokumentowanym
mechanizmem W-30. Domknąć zakres, dopiero potem scalać.

⚠️ Po usunięciu atrybut domeny `PRODUCT.CHOICE_KIND` zostaje wiszącym wskaźnikiem — przepiąć na
`demo.property.offer.choice [kind]`.

### 5. `zone.theft` + `zone.flood` → `demo.property.zone`

Ten sam jedyny klucz `quote.property.postcodeArea`. Ale ⚠️ **dopasowanie idzie po całym wierszu**:
zbiory obszarów pokrywają się tylko w trzech (`30`, `50`, `80`), siedem jest tylko w kradzieżowej,
sześć tylko w powodziowej. Scalona tabela musi mieć **jawny wiersz dla każdego z 16 obszarów** —
inaczej obszar obecny w jednej tabeli spadnie na wiersz `*` i po cichu straci swoją wartość. To nie
jest sklejenie plików, to przepisanie 13 wierszy.

⚠️ Dwóch konsumentów, nie jeden: `zone/resolve.groovy:32-33` **oraz** węzeł `n12` flow
`risk.calculate`, który czyta `zone.theft [factor]` z pominięciem funkcji.

### 6. `sum.valueGap` — jedyna warunkowo wołana, ale to jest utrata funkcji

`Property.java:924` woła ją tylko gdy klient poda wartość nieruchomości, a pytanie jest opcjonalne
i domyślnie puste. W typowym przebiegu demo nigdy nie strzela.

⛔ Usunięcie zabiera **rek. 8.2.c** — porównanie oszacowania klienta z sumą wyliczoną. To nie jest
sprzątanie, to decyzja produktowa: czy demo ma pokazywać, że silnik konfrontuje deklarację klienta
z wyliczeniem.

## Odrzucone, z powodami

| Kandydat | Dlaczego odpada |
|---|---|
| **rodzina `factor.*` → jedna tabela** | ⛔ kolumna wejściowa ma **jeden `source` na całą tabelę**, a każdy z 7 czynników czyta inną ścieżkę modelu. Wspólna kolumna wymaga funkcji orkiestrującej. Sprawdzona i odrzucona też furtka z funkcją jako `source` poziomu. Dodatkowo `between/ii` na `integer` vs exact na `string` — wspólny string zamieniłby przedziały w porównanie leksykograficzne |
| **`variant.availability`** | ⛔ dwa niezależne powody: ma **drugi wymiar** (`zone`) zmieniający się w runtime — potwierdzone na żywo, Z2 daje dostępny COMPLETE, ZX niedostępny z powodem; oraz jest czytana **węzłem `DECISION_TABLE` we flow**, a flow nie umie sięgnąć po atrybut domeny (`EXPRESSION` zakazane) |
| **`factor.claims`** | ⛔ zmienia widoczną składkę w domyślnym przebiegu (0,90 zamiast 1,00) |
| **`factor.security` → `cover.theft`** | ⛔ technicznie działa, ale robi z tabeli jednego pokrycia źródło mnożnika ryzyka dla **wszystkich** — zmiana progu kradzieży po cichu przestawiałaby składkę OC |
| **`cover.assistance`→`building`, `cover.fixtures`→`contents`** | ⛔ zgodność kluczy to zbieg okoliczności, nie fakt strukturalny: pozostałe kolumny są diametralnie różne (stawka kwotowa vs promilowa). Realne scalenie wymaga drugiego wymiaru i wraca do tej samej liczby wierszy. Odwraca §32 |
| **`apk.budget`, `sum.ratePerSqm`, `flood.waitingPeriod`, `cover.liability`, `factor.wallMaterial`, `apk.question.visibility`, `apk.reason`, `apk.recommendation.reason`** | żywe, wielowartościowe, osiągalne — sprawdzone po jednej |

⚠️ `flood.waitingPeriod`: wiersze `ZX` wyglądają na martwe, ale są **strażnikiem ciszy** — pusty
`reasonCode` powstrzymuje zdanie o karencji tam, gdzie powodzi się nie sprzedaje. Bez nich ZX spadłby
na wiersz `*` i klient zobaczyłby fałszywe „ochrona zaczyna działać po 30 dniach".

## Druga oś: przycinanie zawartości zamiast liczby tabel

Właściciel powiedział, że nie potrzebuje wszystkich mapowań, tylko fragmentów. To jest osobna
i tańsza droga niż usuwanie tabel.

`POSTCODE` jest **wolnym polem tekstowym** (`kind = TEXT`, `demoAnswer = "00-950"`), więc bez ręcznej
edycji demo dotyka **jednego prefiksu** z 21 wierszy obu tabel stref. Przycięcie scalonej
`demo.property.zone` do ~5 wierszy demonstracyjnych (`00`, jeden Z2, jeden ZX, jeden T3) nie zmniejsza
liczby artefaktów, ale zmniejsza to, co widz musi objąć wzrokiem — czyli dokładnie problem, który
właściciel zgłosił.

## Znaleziska poboczne

### ⛔ Usterka: oferta obiecuje franszyzę, której nie ma

W strefie powodziowej Z2 uzasadnienie mówi *„Ochrona od powodzi w Twojej lokalizacji wymaga
franszyzy redukcyjnej 1 000 zł"*, a `GET /property` w tym samym momencie zwraca `deductible: 0`.
Zdanie nie ma pokrycia w wycenie — składka nie uwzględnia żadnej franszyzy. Zweryfikowane:
`PROPERTY_KIND=HOUSE`, `POSTCODE=50-000` → `floodZone=Z2`, `deductible=0`.

### ⛔ Usterka: `PUT /property/variant` z nieznanym kodem → HTTP 500

Zgłoszone przy próbie reprodukcji innego przypadku. Nie badane dalej.

### Martwy kod w Javie

`Property.java:98` deklaruje `PARAM_OFFER_TERMS = "demo.property.offer.terms"` — tabela usunięta
w §35, stała nieużywana. Jedna linia do skasowania.

### Brak ochrony testowej

| Warstwa | Pokrycie |
|---|---|
| testy regułowe (`calculateQuote`) | **9 z 31** tabel — arkusz `CASES` podaje `risk.factor`, `sums.*`, `zone.flood` jako gotowe wartości kontekstu |
| testy Javy | **zero** plików dotyczy `pl.decerto.higson.demo.property` |
| testy frontu | dwa pliki, oba mockują cały `PropertyQuoteDto` |

⚠️ `calculate()` (`Property.java:150-165`) nie ma `catch`, a `GlobalExceptionHandler` obsługuje tylko
dwa wyjątki z profilu komunikacyjnego. Niedokończone przepięcie nie degraduje ekranu — wywala całe
`GET /property` na 500. Przy 22 tabelach bez ochrony testowej każde przepięcie wymaga ręcznego
sprawdzenia na żywej aplikacji.
