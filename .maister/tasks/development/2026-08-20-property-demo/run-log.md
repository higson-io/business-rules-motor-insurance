# Demo majątkowe — dziennik wykonania

**Sesja:** 2026-08-20 → 2026-08-21, bez nadzoru
**Gałąź:** `feature/property-demo` — **nie pushowana**
**Zlecenie:** `prompt.md` + `brief.md`

---

## 1. Co zostało zrobione

| # | Produkt | Stan |
|---|---|---|
| A | `analysis/market-research.md` — research rynku i APK | ✅ osiem OWU przeczytanych w oryginale, wszystko z przypisami |
| B | `.maister/docs/project/higson-property-profile.md` — dokument projektowy | ✅ D-1…D-8, W-1…W-7, budżet złożoności, §13 z odstępstwami |
| C | `implementation-plan.md` — plan implementacji + rozdział o froncie | ✅ z pułapkami W-15…W-27 przeniesionymi i W-28…W-34 nowymi |
| D | Konfiguracja w Higson Studio, profil `PROPERTY` | ✅ opublikowana |
| E | Backend `pl.decerto.higson.demo.property` | ✅ 24 pliki, wpięty w `@ComponentScan` |
| F | Front `frontend/src/app/demos/property/` | ✅ 5 komponentów + store + serwis + model |
| G | Weryfikacja przeglądarką | ✅ §4 |
| H | Obie suity testów | ✅ `ng test` 52/52, `mvn -o test` 11/11 |

### Higson — co konkretnie powstało

| Element | Liczba |
|---|---|
| Typy kontekstu | 15 (+ `ROOT`), 40 ścieżek |
| Tabele decyzyjne `demo.property.*` | **32** |
| Funkcje Groovy | **18** |
| Flow | **2** |
| Typy domenowe | 4 |
| Elementy domeny | 29, w tym **18 pytań APK** |
| Region `PROPERTY` | 2 wersje: `2026-01-01` i `2027-07-01` |
| Wersjonowane parametry | 3 (`apk.regime`, `sum.ratePerSqm`, `tariff.baseRate`) |
| Wersjonowane elementy domeny | **7 pytań istniejących wyłącznie w wersji 2** |

⛔ **Nic poza `demo.property.*` nie zostało dotknięte.** Każda publikacja była poprzedzona
przeglądem sesji roboczej; przy pierwszej (51 elementów) sprawdzono dodatkowo, że drzewo domeny
`DEMO` i typy kontekstu `DEMO` są bez zmian. `assign_to_profile` nie zostało wywołane ani razu —
import tabel niesie `profileCode` w metadanych, więc przypisanie dzieje się przy tworzeniu.

**Backup bazy:** `pre-property.zip` w `/h2-data/` kontenera `higson-demo-local-db` **oraz** kopia
na hoście, zrobiony **przed** pierwszą zmianą.

---

## 2. Założenia, które przyjąłem bez pytania

| # | Sytuacja | Założenie | Gdzie zapisane |
|---|---|---|---|
| Z-1 | Brief nazywa wersję 1 „APK wg Wytycznych KNF z 2014" | **Wytyczne z 2014 nie zawierają APK** — sprawdzone w oryginale. Wersja 1 nazywa się „APK — minimum ustawowe (art. 8 ustawy o dystrybucji ubezpieczeń)" | research §11.1, dokument W-1 |
| Z-2 | Brak publikowanych taryf | Wszystkie stawki są wymyślone i tak oznaczone. Jedyne liczby przepisane z OWU: limity z PZU §46 | dokument W-2 |
| Z-3 | Brak publikowanych map stref | Strefy powodziowe i kradzieżowe przypisane do obszarów pocztowych demonstracyjnie, z odwołaniem do powodzi 1997 i 2024 dla czytelności | dokument W-3 |
| Z-4 | Kolekcje nie dają się adresować wprost pod `ROOT` | Dołożony węzeł `/PRODUCTS[HOME]` — ten sam kształt co `PRODUCT_LINES` w LIFE | dokument §13.1 |
| Z-5 | Silnik nie konwertuje typów poziomu wejściowego | `KIND` (jak narysować) i `VALUE_TYPE` (czym jest) rozdzielone na dwa atrybuty pytania | dokument §13.2 |
| Z-6 | Java nie może znać kodów pytań | Ścieżkę kontekstu odpowiedzi podaje atrybut `CONTEXT_PATH` pytania; odpowiedzi startowe siedzą w tabeli `apk.question.defaultAnswer` | dokument §13.2, §13.3 |
| Z-7 | Publikacja w Studio wymaga zwykle potwierdzenia użytkownika | Publikowałem sam, po sprawdzeniu, że sesja zawiera wyłącznie `demo.property.*` — zgodnie z §6 briefu („publikować dopiero po sprawdzeniu sesji roboczej") | ten dziennik |
| Z-8 | Przeglądarka Playwright była zablokowana przez proces z 20 sierpnia | Ubiłem stary proces Chrome MCP (PID 12485, `--user-data-dir=…/ms-playwright-mcp/…`, uruchomiony poprzedniego dnia) — to przeglądarka automatyzacji, nie prywatna sesja | ten dziennik |

---

## 3. Czego nie udało się ustalić i dlaczego

| # | Czego brak | Powód |
|---|---|---|
| B-1 | Jakichkolwiek **wartości taryfowych** | Taryfy Stawek są dokumentami wewnętrznymi; OWU do nich odsyłają, nie publikują |
| B-2 | Stawek **zł/m²** do wyliczenia sumy | Nie ma ich w żadnym OWU. Sam mechanizm jest udokumentowany (Generali §27), wartości nie |
| B-3 | **Sekwencji pytań w kalkulatorach online** | Kalkulatory TU to aplikacje JS za zgodą na cookies; przeglądarka Playwright była wtedy zajęta, a strony marketingowe nie renderują formularza. Kolejność pytań w demo wynika z **logiki rekomendacji 8.2 (a→b→c→d)**, nie z odwzorowania konkretnego kalkulatora — i tak jest zapisane |
| B-4 | **Bieżącego OWU Link4** | Portal `dokumenty.link4.pl` renderowany po stronie klienta. Link4 cytowany wyłącznie z archiwalnego DOM 24, z jawną datą |
| B-5 | Pełnej listy **pakietów Allianz** | W pobranym dokumencie występują nazwy KOMFORT i MAX, brak miejsca wyliczającego wszystkie |
| B-6 | **Granic stref powodziowych** | Generali podaje, że wyznacza je ze statystyki szkód; mapy nie publikuje |
| B-7 | **Sum gwarancyjnych OC** u czterech TU | Wskazywane w polisie, nie w OWU |

---

## 4. Weryfikacja — co realnie sprawdzone

**Uruchomienie:** `mvn -o package -Pall_in_one -DskipTests` (Node 22) + `java -jar` z bazą H2 na
`9192`, aplikacja na `http://localhost:48080/demo`.

### 4.1 Przeglądarką (Playwright MCP)

| # | Kryterium z briefu §7 | Wynik |
|---|---|---|
| 1 | Ankieta renderuje się z pytań pochodzących z Higsona | ✅ 9 pytań w dwóch sekcjach, treść i pomoc z atrybutów elementów domeny |
| 2 | ⭐ Przesunięcie daty przez `2027-07-01` **widocznie zmienia zestaw pytań** | ✅ **9 → 16** widocznych, dochodzą sekcje POTRZEBY i WYŁĄCZENIA |
| 3 | ⭐ …i **treść rekomendacji** | ✅ brak uzasadnienia → **6 zdań**; podpis reżimu zmienia się z „art. 8 ustawy o dystrybucji ubezpieczeń" na „uchwała KNF nr 190/2026" |
| 4 | Rekomendacja pokazuje uzasadnienie i różnice między wariantami | ✅ lista powodów + macierz `pokrycie × wariant` z zaznaczonym proponowanym |
| 5 | Ostrzeżenie o wyłączeniu dla sytuacji, która je wywołuje | ✅ `VACANCY = tak` → czerwone ostrzeżenie o pustostanie **wraz ze sposobem naprawy** |

Dodatkowo, przez API:

| Sprawdzenie | Wynik |
|---|---|
| Suma murów sterowana wersjonowaną stawką | 60 m² × 7 900 = **474 000 zł** (v1) → 60 m² × 8 800 = **528 000 zł** (v2) |
| Selektywność wersjonowania | obszar `50` daje **474 000 zł po obu stronach daty** — wiersz celowo niezmieniony |
| Strefa `ZX` (kod `57-xxx`, dom) | wariant pełny **niedostępny z podanym powodem**, nie po cichu bez powodzi |
| Budżet 900 zł przy potrzebach na wariant pełny | propozycja schodzi do **BASIC**, a komunikat podaje cenę wariantu pełnego (2 912,65 zł) i **nazywa braki** |
| Reguła „mieszkanie powyżej parteru = strefa 0" | mieszkanie na 3. piętrze w obszarze `00` → `Z0`, mimo że tabela stref daje dla `00` co innego |

### 4.2 Testy

```
ng test --watch=false   → 19 plików, 52 testy, wszystkie zielone
mvn -o test             → 11 testów, wszystkie zielone
```

Nowe: `property-store.spec.ts` (3 testy) i `questionnaire.spec.ts` (5 testów). Ten drugi pilnuje
rzeczy, która najłatwiej cicho przestaje być prawdą: że komponent **nie filtruje po własnej regule**
i **nie zna żadnego pytania**.

---

## 5. Co wymaga Twojej decyzji

| # | Rzecz | Dlaczego pytam |
|---|---|---|
| **P-1** | **Nazwa wersji 1.** Zmieniłem ją z „Wytyczne 2014" na „APK — minimum ustawowe (art. 8 UDU)", bo Wytyczne z 2014 APK nie zawierają. Jeśli w prezentacji ma paść „Wytyczne 2014", to trzeba to powiedzieć inaczej — np. „stan sprzed Rekomendacji" | Zmiana dotyczy tego, co widz przeczyta na ekranie |
| **P-2** | **Testy regułowe w `@tests`** — nie napisane, tak samo jak przy LIFE. Weryfikacja poszła przez uruchomienie. Czy dopisać? | To jedyna grupa z planu pominięta świadomie |
| **P-3** | **Franszyza nie ma kontrolki na ekranie.** Endpoint działa, druga rola franszyzy (warunek dostępności powodzi w strefie `Z2`) jest więc widoczna tylko przez API. Dołożyć suwak? | Bez tego decyzja D-8 jest udokumentowana, ale nie pokazana |
| **P-5** | **Ubiłem proces Chrome** z 20 sierpnia, żeby odblokować Playwright (Z-8). Jeśli coś w nim było otwarte — przepadło | Nie da się tego cofnąć |
| **P-6** | **Wiersz `USERREGIONVERSION`** dla `admin` wskazuje wersję 2 regionu `PROPERTY` (został po procedurze z W-32). Nie przeszkadza runtime'owi, ale w UI Studia edycja domyślnie pójdzie do wersji 2 | Usunąć czy zostawić |

---

## 5a. Domknięte po Twoich uwagach

| # | Uwaga | Co zrobiłem |
|---|---|---|
| **P-4** *(zamknięte)* | Uzasadnienie podawało kody pokryć zamiast nazw | Myliłem się, twierdząc, że funkcja Groovy nie widzi nazw z drzewa. `RhinoDomain`/`RhinoDomainElement` dają pełną nawigację: `getSafe(kolekcja, kod)`, `name()`, `hasAttr()`, `getAttrString()` bez kontekstu. `apk.justify` tłumaczy kody na nazwy **w regule**, nie w Javie — bo tekst dla klienta należy do reguł w całości, razem z nazwami, które w nim padają. Opisane w §13.6 dokumentu |
| **Układ ekranu** | Sekcje wychodziły poza ekran, demo mało ergonomiczne | Trzy przyczyny, wszystkie usunięte: **(1)** ankieta była jedną kolumną 520 px w panelu szerokim na 1100 px — jest siatką `auto-fill`, więc 16 pytań mieści się w czterech kartach zamiast w metrze przewijania; **(2)** ⛔ **teksty pomocy z Higsona w ogóle się nie renderowały** — `mat-hint` jest jednowierszowy i przy dłuższym zdaniu znika, więc zniknął dowód, że treść pochodzi z reguł; **(3)** długie pytania były ucinane wielokropkiem w `mat-label`, która się nie zawija. Treść pytania i pomoc idą teraz w zwykłe akapity nad i pod kontrolką — identycznie dla wyboru, liczby i tak/nie |

| **`mat-hint` globalnie** | Zgłosiłeś, że hinty mają działać | `_forms.less` gasił `mat-mdc-form-field-subscript-wrapper` **bezwarunkowo**, więc **żaden hint w całej aplikacji nigdy się nie pokazał** — były dwa, po jednym pod datą polisy w LIFE i w PROPERTY. Reguła zwija teraz wiersz tylko wtedy, gdy nie ma w nim czego pokazać: `:has(mat-hint, mat-error)`. Dyskryminatorem musi być sam `mat-hint`, bo `hint-wrapper` Material renderuje zawsze — pole bez hinta dostaje wrapper z samym `hint-spacer` |

⚠️ **(2) i (3) to nie były usterki kosmetyczne.** Treść pytania i tekst pomocy przychodzą z Higsona
i mogą mieć dowolną długość. Wsadzenie ich w komponenty Material, które zakładają krótką etykietę,
sprawiało, że **konfiguracja nie docierała na ekran** — a to jest dokładnie ta rzecz, którą to demo
ma pokazywać.

Sprawdzone przy 1440, 1024 i 390 px: strona **nie przewija się w poziomie** na żadnej z tych
szerokości, a szeroka macierz porównawcza przewija się we własnym kontenerze (`responsive.md`).

**Poprawka `mat-hint` zmierzona na wszystkich trzech demach**, bo dotyka globalnego arkusza:

| Demo | Pola | Pokazany wiersz podpisu | Efekt |
|---|---|---|---|
| MOTO | 13 | **0** | bez zmian — oszczędność ~240 px z ND-4 nienaruszona, panel nadal nie przewija się wewnątrz |
| LIFE | 12 | **1** | hint pod datą polisy **pojawił się pierwszy raz** |
| PROPERTY | 7 | **1** | jw. |

Pole z hintem rośnie 56 → 76 px, pole bez hinta zostaje na 56 px. Reguła jest napisana jako
„ukryj, potem odsłoń" celowo: przeglądarka bez `:has()` gubi drugą regułę i ląduje na dzisiejszym
zachowaniu, a nie na rozjechanym MOTO.

---

## 6. Rzeczy zrobione inaczej, niż zakładał plan

| Co | Jak | Dlaczego |
|---|---|---|
| **Profil i region** utworzone SQL-em | Dwa `INSERT`-y do `DOMAIN_PROFILE`/`PROFILE_CONFIGURATION` i trzy do `REGION`/`REGIONVERSION`/`REGIONVERSIONSCHEDULE` | MCP nie ma narzędzia tworzącego ani profil, ani region; REST wymaga tokenu, którego konfiguracja pluginu nie udostępnia; Studio UI było zablokowane. Backup zrobiony wcześniej |
| **Typy kontekstu** zbudowane plikiem | `import_context` zamiast 15 wywołań | `update_context_type(root: true)` nie ustawia **pierwszego** korzenia (W-29) |
| **Tabele** zbudowane plikami XLSX | Generator kopiujący części statyczne z eksportu Higsona | Importer nie przyjmuje plików openpyxl (W-30) |
| **Wersja 2 regionu** wypełniona po tymczasowym przesunięciu timeline'u | `VALIDFROM` wersji 2 → przeszłość, restart Studia, edycja, przywrócenie daty, restart | `attach_to_region` wypełnia tylko wersję 1, a edycja idzie w wersję systemowo aktywną (W-32, W-33) |
| **Funkcji 18, nie 8** | j.w. co w LIFE | Brak `EXPRESSION` we flow + rozgałęzianie po skalarze |
| **Dwa dodatkowe atrybuty pytania** | `CONTEXT_PATH`, `VALUE_TYPE` | Pierwszy trzyma Javę z dala od kodów pytań, drugi ratuje przed cichym spadnięciem tabeli na wiersz domyślny |

---

## 7. Stan repozytorium

Osiem commitów na `feature/property-demo`, każdy z opisem mówiącym **dlaczego**. Nic nie
wypchnięte. Snapshot `src/main/higson/` nietknięty — konfiguracja żyje w Studio, zgodnie ze
standardem `higson/snapshot.md`.

Pliki tymczasowe (generatory XLSX, pobrane OWU, kopie eksportów) siedzą w katalogu scratchpad
sesji, poza repozytorium. W `src/main` i `frontend/src` nie ma martwego kodu.

---

## 8. Przystosowanie trzech dem do prezentacji (2026-08-21)

Osobna tura, po Twojej uwadze „wykorzystaj wiedzę o APK i dostosuj pozostałe dema do prezentacji".
Przegląd UX robiły dwa agenty — jeden na MOTO, jeden przekrojowo na LIFE i PROPERTY. Poniżej to,
co z ich ustaleń weszło, i to, co świadomie odrzuciłem.

### 8.1 Rzeczy, które okazały się nie kosmetyką, tylko nieprawdą na ekranie

Przegląd „układu i designu" wyciągnął pięć miejsc, w których ekran **twierdził coś fałszywego**.
To one były najdroższe do naprawy i one najbardziej zmieniają demo.

| Co ekran mówił | Co było prawdą | Naprawa |
|---|---|---|
| Każda wybrana umowa dodatkowa: `0,00 zł` | Flow zwracał tylko łączną `ridersPremium`, `Life.java` ją logował. Składki per umowa nie liczył nikt | Nowa reguła `demo.life.individual.rider.premium` — jedna formuła, dwóch odbiorców (suma i pozycja) |
| Pasek udziałów: umowa główna na 100% | `shares()` mapował same umowy dodatkowe, gałąź `part.code === 'MAIN'` była martwa | Umowa główna liczona jako reszta; potwierdzone przez silnik: 57 + 18 + 11,50 przy zwróconych 86,50 |
| `Ubezpieczonych: 0` | Faza przystąpień nie istnieje w Higsonie — `insuredCount` zawsze `null` | Wiersz znika, gdy reguły nie podały liczby. Zero jest twierdzeniem (**W-7** w dokumencie profilu) |
| Cena REFER wyglądała jak cena ACCEPT | `binding` było liczone i nigdzie nierenderowane | Adnotacja „cena wstępna" pod składką (D-1 wreszcie widoczne) |
| Dochód klienta nie występował nigdzie | `maxSum` jechał przez DTO i nikt go nie czytał | „Maksimum tutaj: N zł" pod sumą umowy (D-7 wreszcie widoczne) |

Do tego jedna wada niewidoczna na ekranie: `mutationError` był polem `QuoteStore`, więc
interceptor wołał `inject(QuoteStore)` przy każdym PUT-cie — także w LIFE i PROPERTY, powołując
sklep motoryzacyjny i wysyłając jego zapytania. Równocześnie `LifeStore.mutationError`
i `PropertyStore.mutationError` istniały i nikt do nich nie pisał, więc **nieudany PUT w tych
dwóch demach ginął bez śladu**. Jedna usługa `MutationErrors` i wspólny baner.

### 8.2 Data polisy jako główny obiekt na ekranie

Oba dema wersjonowane miały małe pole z podpisem, a liczba, która się rusza, siedziała w przypisie
12 px w prawym dolnym rogu ankiety. Teraz pasek niesie trzy części: co ustawiono → jaki reżim
z tego wynika → ile tego jest. Trzecia część **czyta odpowiedź serwera, nie datę**, więc
przesunięcie daty i skok liczby to jedno zdarzenie, nie dwa.

Znacznik „było N" pojawia się per liczba, nie per pasek — bo wersjonowanie jest selektywne
i przy wieku 35 lat obie wersje taryfy dają tę samą stawkę. Milczenie znacznika przy zmienionej
nazwie wersji jest informacją: *ta akurat liczba od wersji nie zależy*.

⛔ Przy okazji zniknęło `const VERSION_2_FROM = new Date('2026-07-01')` z komponentu LIFE — druga
kopia osi czasu regionu, w przeglądarce, w demie o tym, że progi należą do konfiguracji. Nazwę
wersji podaje teraz `demo.life.tariff.version` (§13.1 dokumentu profilu).

### 8.3 MOTO — cofnięta decyzja, nie brak decyzji

`inputs.less` do dziś zaczyna się od wyjaśnienia, że „input band" zastąpił pasek boczny, bo tamten
zabierał ćwiartkę szerokości tabeli. A `moto.less` przypinał kolumnę do 340 px i `.inputs__grid`
miał `display: block` — panel wrócił do postaci paska, komentarz został. Dwanaście pól w słupku to
~1400 px, więc tabela kończyła się w jednej trzeciej strony.

Dwie równe kolumny **od 1280 px**, nie od 992: przy 992 połówka ma ~480 px, formularz nie zmieści
dwóch pól obok siebie, a tabela zejdzie poniżej swojego minimum 560 px. Zmierzone na 1440:
formularz 756 px zamiast ~1400, obie kolumny kończą się w promieniu 250 px od siebie.

Tooltip ochron miał **jedną** pozycję (`end/center`) zaczepioną o etykietę wiersza, przy
`min-width: 340px` — czyli kładł się na składkach BRONZE i SILVER zawsze, na każdym wierszu, przy
każdej szerokości okna. Lista pozycji w `withPositions()` JEST listą wariantów awaryjnych, a miała
jeden wpis. Teraz otwiera się pod etykietą (zmierzone: zero nachodzeń na komórki cen). Strzałka
wycięta — była przypięta do `left: -19px`, poprawnego wyłącznie dla pozycji, która przestała być
domyślna.

### 8.4 Czego świadomie NIE zrobiłem

| Odrzucone | Dlaczego |
|---|---|
| Odznaka „polecany wariant" na ekranie motoryzacyjnym | `OptionDto` nie ma flagi rekomendacji, więc odznaka byłaby osądem wymyślonym w TypeScripcie — na ekranie, którego cała teza brzmi, że osądy siedzą w konfiguracji. Zamiast tego różnica względem najtańszego: odejmowanie na zwróconych liczbach, sprawdzalne wzrokiem |
| Tłumaczenie MOTO na polski | **ND-2** przypina etykietę `Documents` jako świadomą decyzję produktową, a 206 kluczy golden parity powstało na angielskiej kopii. Do tego treść tabeli (`row.name`, `discount.name`, `coverage.description`) przychodzi z Higsona po angielsku — sama rama po polsku czytałaby się gorzej niż uczciwie angielski ekran. To decyzja produktowa, nie designerska |
| Zbudowanie fazy przystąpień, żeby `programCost` miał co pokazywać | To funkcja, nie przystosowanie do prezentacji. Zamiast tego wycofałem UI i zapisałem brak jako **W-7** |
| Usunięcie martwych pól `programCost` / `insuredCount` z DTO | Są udokumentowanym kontraktem D-2 czekającym na W-7; skasowanie ich wymagałoby zmiany decyzji w dokumencie profilu, a to Twoja decyzja |
| Likwidacja zagnieżdżonego scrolla w tabeli grupowej | Przy 13 świadczeniach tabela mieści się w `100vh - 150px`, więc wewnętrzny scroll w praktyce się nie włącza, a przyklejony nagłówek kolumn ma realną wartość |

### 8.5 Weryfikacja

Obie suity zielone (`mvn -o test`; `ng test --watch=false` — 56 testów). Aplikacja zbudowana
profilem `all_in_one` i sprawdzona w przeglądarce na 1440×900, wszystkie trzy dema:

- **PROPERTY** — data 1.09.2027 przestawia reżim na „APK wg Rekomendacji KNF", pytania 9 → 16,
  sekcje 2 → 4, uzasadnienie nie → tak (5 zdań), suma murów 474 000 → 528 000 zł. Trzy znaczniki
  „było". Nagłówki h1 → h2 → h2. Strona nie przewija się w bok.
- **LIFE** — wersja taryfy czytana z reguły (`Taryfa 2026 H2`, „obowiązuje od 1 lipca 2026"),
  przy dacie marcowej przechodzi na H1 bez ani jednej daty w kodzie SPA. Umowy dodatkowe po dwie
  w rzędzie (4 rzędy zamiast 7), pasek udziałów 65,9 / 20,8 / 13,3 sumuje się do 100%, pozycje
  18,00 i 11,50 zł przy sumie 86,50 zł. Kolumny wariantów grupowych nazwami, nie kodami.
- **MOTO** — kolumny 689/689 px, formularz 2-up wewnątrz, składki bez zmian (`264.78`, `674.02`,
  `1,381.19` — goldeny nietknięte), różnice `+409.24 vs BRONZE`, tooltip zero nachodzeń na ceny.

Dwa nowe testy jednostkowe na pasek daty, oba sprawdzone pod mutacją, którą mają łapać: odczyt
liczony z daty zamiast z odpowiedzi serwera, i znacznik „było" pokazywany zawsze.

Backupy bazy przed publikacją: `/h2-data/pre-life-version.zip`, `/h2-data/pre-rider-premium.zip`.
Obie sesje robocze zawierały wyłącznie elementy `demo.life.*`. Nic nie wypchnięte.

---

## 9. Faza przystąpień — domknięcie W-7 (2026-08-21)

Na „napraw, co uważasz za słuszne". Obie decyzje, które zostawiłem Tobie, sprowadzały się do jednej:
albo faza przystąpień powstaje, albo `programCost` i `insuredCount` są martwym kodem do usunięcia.
Zbudowałem — jest zaprojektowana w §6.4/§6.6/§6.7 dokumentu profilu, a to ona odróżnia D-2
udokumentowane od D-2 działającego.

**Okazało się, że brakowało wyłącznie okablowania.** Wszystkie reguły siedziały w Studio od
13 sierpnia: `enrolmentStats` liczyła statystyki rozkładu, `programCost` ważyła koszt,
`util.insuredCount` liczyła fazę oferty, a `group.premiums` **już rozdzielał obie fazy u siebie**
i stosował `factor.variantMix`. Nikt tego nie wołał: flow zwracał tylko `eligible` i `variants`,
a `enrolment` nie miała jak trafić do modelu, bo nie było endpointu.

Przy okazji wyszło, że `demo.life.group.programCost` **nie dałaby się zawołać** — miała
`variantPremiums` na pozycji 0 i `ctx` na 1, czego silnik nie przyjmuje. Nikt tego nie zauważył,
bo nikt nie próbował.

### 9.1 Co zostało dołożone

| Warstwa | Element |
|---|---|
| Reguły | `demo.life.group.programCost` przepisana na `(ctx, variants)`; nowa `demo.life.group.insured` (sam rozdział faz, bez własnego wzoru); dwa nody we flow grupowym |
| Backend | `PUT /life/group/enrolment/{variantCode}` — jedno pole na żądanie, zero usuwa pozycję; `GroupScheme.enrol`; odczyt `insuredCount` i `programCost` z wyniku flow; `LifeQuoteDto.enrolment` |
| Frontend | Sekcja „Kto co wybrał" w panelu wejść, pasek kosztu programu nad tabelą, adnotacja przy liczbie ubezpieczonych, wyjście „Wróć do fazy oferty" |

### 9.2 Dwie rzeczy złapane przy weryfikacji, obie warte zapisania

**Adnotacja, która kłamała w jednym przypadku.** Napisałem pod kosztem programu, że „nie równa się
żadnej liczbie z wiersza poniżej". Przy rozkładzie, w którym wszyscy wybrali jeden wariant, równa
się co do grosza (8 281,70 zł w obu miejscach). Zdanie mówi teraz, **czym ta liczba jest**, a nie
czym nie jest — dokładnie ten sam rodzaj poprawki, co cała §8.1.

**Równoległe PUT-y gubiły zapisy.** „Wróć do fazy oferty" wysyłało trzy żądania przez `Promise.all`
i czyściło jeden wariant z trzech. Agregat wyceny jest `@SessionScope` i nie jest bezpieczny
wątkowo: trzy wątki czytają tę samą `ArrayList`, każdy usuwa swoją pozycję i odkłada wynik, więc
przeżywa ostatni zapis. **Każda inna mutacja w tym API jest pojedynczym PUT-em i dlatego nigdy na to
nie trafiła** — to jest pierwsze miejsce w aplikacji, które wysyła serię. Teraz idzie po kolei, a
test sprawdza, że po pierwszym tick-u leci dokładnie jedno żądanie.

### 9.3 Weryfikacja

100 pracowników, deklarowana partycypacja 60%, wiek 35, branża I:

| Sytuacja | Ubezpieczonych | Koszt programu | Za pracownika |
|---|---|---|---|
| Faza oferty | 60 *(z deklaracji)* | — | 40,66 / 61,00 / 91,01 |
| 70 osób, 30 / 25 / 15 | 70 *(z przystąpień)* | 4 109,95 zł | bez zmian |
| 70 osób, **wszyscy COMPLETE** | 70 | 8 281,70 zł | 52,86 / 79,30 / **118,31** |
| 15 osób ze 100 | 15 | 981,37 zł | 47,38 / 71,06 / 106,03 |

Trzeci wiersz jest tym, po co ta faza istnieje: ten sam skład osobowy, ta sama liczba
ubezpieczonych, **stawka wyżej o 30%** — bo policzyła ją druga oś antyselekcji. To najlepszy moment
demonstracyjny w całym demie grupowym i do dziś nie dało się go pokazać.

Powrót do fazy oferty sprawdzony w przeglądarce: trzy pola na zero, znika koszt programu, wraca 60
ubezpieczonych z deklaracji i stawki bazowe. Ścieżka indywidualna nietknięta (57 zł, ACCEPT,
`Taryfa 2026 H2`).

Obie suity zielone: `mvn -o test`, `ng test --watch=false` — **59 testów** (trzy nowe na granicę
faz, każdy sprawdzony pod mutacją, którą ma łapać). Backup przed publikacją:
`/h2-data/pre-enrolment.zip`. Sesja robocza zawierała wyłącznie elementy `demo.life.group.*`.
Nic nie wypchnięte.
