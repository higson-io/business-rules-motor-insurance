# Testy taryfy majątkowej — dwa tryby

Higson testuje reguły na dwa sposoby i **oba mają tu swój artefakt**:

| Tryb | Gdzie | Co zawiera |
|---|---|---|
| **Tester** (Tools → Tester) | `src/main/higson/@tests/PROPERTY - skladka pakietu (demo.property.calculateQuote).json` | JEDEN przypadek — wzorzec |
| **Batch tester** (Tools → Batch tester → Load test package) | `skladka-pakietu-10-przypadkow.xlsx` | DZIESIĘĆ wierszy wejść dla tego samego flow |

Przedmiotem obu jest flow **`demo.property.calculateQuote`** — wycena jednego pakietu. Przechodzi
przez wszystko, co składa się na cenę: dostępność pakietu, siedem tabel pokryć, sumę linii, czynnik
okresu i ratę.

## ⛔ Arkusz `Mark` niesie TRYB pliku

To jedyna różnica między plikiem Testera a plikiem batch testera — i jedyny powód, dla którego
pierwsza wersja tej paczki wracała z HTTP 422 (`EXCEL_VALIDATION_INCORRECT_LABEL`):

| Wartość `Mark!A1` | Znaczenie |
|---|---|
| `TEST` | pojedynczy przypadek, eksport z Testera |
| `BATCH_TESTS` | paczka wsadowa, wczytywana w batch testerze |

Reszta układu jest wspólna: arkusz `CASES` ma w wierszu 2 nazwy ścieżek kontekstu, w `M1` kod
badanego elementu, a każdy kolejny wiersz to jeden przypadek — nazwa, wejścia, oczekiwany wynik.

## ⛔ Oczekiwanym wynikiem jest CAŁA struktura zwracana przez flow

Nie sama składka: `annual`, `instalment` **i rozbicie na pokrycia**. Flow zwraca komplet zmiennych
oznaczonych jako `returned`, więc wpisanie samego `1014.94` daje FAILURE przy poprawnym wyniku.
Efekt uboczny jest korzystny — zmiana stawki jednego pokrycia pokazuje się jako różnica w JEGO
linii, a nie jako rozjazd sumy.

## Co pilnuje dziesięć wierszy

Trzy pakiety, dwa rodzaje nieruchomości, cztery poziomy zabezpieczeń, trzy strefy powodziowe, dwa
standardy wykończenia i dwa okresy ochrony. Wiersze 04, 05 i 06 pilnują **zachowania**, nie kwoty:
pokrycie niedostępne kosztuje `THEFT=0` i zostaje w rozbiciu, pakiet niedostępny wychodzi z powodem
i bez ceny, a BASIC w tej samej strefie dalej się wycenia — bo dostępność jest na pakiet.

Pełny opis: `.maister/docs/project/higson-property-profile.md` §34.
