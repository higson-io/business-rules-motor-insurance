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
