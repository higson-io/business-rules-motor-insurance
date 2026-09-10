# Kierunek okrajania demo PROPERTY (2026-09-09)

Analiza produktowa, nie techniczna: **czego ta prezentacja nie potrzebuje**. Osobna od
`analiza-tabel.md`, która odpowiada na pytanie „co da się usunąć bezpiecznie".

Podstawa: konfiguracja ze snapshotu i Studia, przejście aplikacji na żywo, uchwała KNF 190/2026
(pełny tekst w repo: `.maister/tasks/development/2026-08-20-property-demo/knf-rekomendacje-190-2026.txt`),
stanowisko UKNF z 1.10.2021, metodyka *Great Demo!*, porównanie z profilami DEMO i LIFE.

## A. Teza demo

> **Analiza potrzeb klienta — o co pytamy, co z odpowiedzi wynika, co proponujemy i czym to
> uzasadniamy — jest w Higsonie konfiguracją, którą biznes zmienia w Studio bez wdrożenia;
> a to jest obowiązek, który zakład i tak musi wdrożyć do 1 lipca 2027.**

Druga połowa tego zdania to najmocniejsza amunicja i **nie ma jej dziś w demie**. Uchwała 190/2026 §2
daje twardy termin, a stanowisko UKNF mówi wprost, że *„jedynym dowodem prawidłowego wykonania tego
obowiązku nie może być oświadczenie klienta"*.

⛔ **Konsekwencja dla cięcia:** wszystko, co jest taryfą, a nie analizą potrzeb, jest w PROPERTY
obsadą drugoplanową. Demo moto jest o taryfie, demo życiowe o wersjonowaniu taryfy. Taryfa w majątku
ma wystarczyć do tego, żeby propozycja miała cenę (rek. 8.2.d) i żeby dało się pokazać różnice między
wariantami (rek. 9.3). Nic ponadto.

## B. Lista do cięcia, od najmniej potrzebnej

| # | Co | Działanie | Zwalnia |
|---|---|---|---|
| 1 | `factor.deductible` — franszyza, której nikt nie ustawia | USUŃ | 1 tabela, 2 węzły flow, wiersz „Franszyza 0 zł" w ofercie |
| 2 | kolumna `partOf` w katalogu pytań | USUŃ | 1 kolumna × 29 wierszy |
| 3 | `zone.theft` — strefa, której nie widać na żadnym ekranie | USUŃ | 1 tabela, 2 węzły flow |
| 4 | pytanie `RENOVATED` | USUŃ | 1 wiersz katalogu + 1 widoczności |
| 5 | pytanie `FLOOR` i reguła „mieszkanie powyżej parteru = Z0" | USUŃ | 1 pole, 8 linii Groovy — **odblokowuje demonstrację stref** |
| 6 | pięć tabel czynnikowych + flow `risk.calculate` | USUŃ / SCAL w jedną 2-wymiarową | 5–6 tabel, 1–3 funkcje, 10–14 węzłów |
| 7 | `apk.question.visibility` | USUŃ (po 4–6 ma zero żywych wierszy) | 1 tabela |
| 8 | `cover.fixtures` — bliźniacze z `contents` | SCAL | 1 tabela, 1 element drzewa, 2 referencje |
| 9 | `offer.choiceKind` + `offer.choice` | SCAL | 1 tabela |
| 10 | poziomy `APK/NEEDS/GROUPS/QUESTIONS` | UPROŚĆ | 1–2 poziomy nawigacji |
| 11 | `flood.waitingPeriod` — druga oś karencji | UPROŚĆ | 2 wiersze, 1 pole |
| 12 | gałąź alternatywy budżetowej | UPROŚĆ świadomie | 1 funkcja, 1 węzeł |
| 13 | wiersze wagi 1 w `apk.answer` | tylko ostre cięcie | ⚠️ szkodzi tezie |
| 14 | pokrycie `ASSISTANCE` | tylko ostre cięcie | ⚠️ gospodarz pytania warunkowego |
| 15 | `sum.valueGap` + `PROPERTY_VALUE` | tylko ostre — to cięcie w regulacji | rek. 8.2.b |

### Pozycje warte osobnej uwagi

**#5 `FLOOR` sabotuje najlepszy moment demo.** Przy domyślnych odpowiedziach (Mieszkanie, 3. piętro)
strefa powodziowa wychodzi **Z0 niezależnie od kodu pocztowego**, bo reguła z OWU Generali §21 ust. 2
podnosi mieszkania powyżej parteru. Żeby pokazać franszyzę w Z2 albo odmowę w ZX, prowadzący musi
najpierw zejść na parter albo przełączyć się na dom.

**#6 czynniki ryzyka to najdroższy blok na widok.** Siedem tabel + najdłuższy flow (14 węzłów) +
trzy funkcje, a `riskFactor` **nie jest renderowany w żadnym szablonie** — sprawdzone: występuje
tylko w modelu i w fixture'ach testów. Widz widzi jedną składkę, której nie umie rozłożyć.
Rekomendacja: jedna tabela dwuwymiarowa (sposób korzystania × zabezpieczenia). Zgodne z §33.9
(tabela → flow → funkcja), a `risk.calculate` to jedyny flow **bez rozgałęzienia**, czyli najsłabszy
jako diagram.

**#10 to jest właściwa przyczyna wrażenia „przeładowana domena".**

| Profil | Elementy drzewa | Głębokość |
|---|---|---|
| DEMO (moto) | 28 | 7 |
| LIFE | 40 | 7 |
| **PROPERTY** | **26 + 12 referencji** | **8** |

PROPERTY ma **najmniej elementów z trzech profili i najgłębsze drzewo**. Kolekcja `APK` trzyma jeden
element, a cztery z sześciu grup trzymają jedno pytanie. Osiem kliknięć do pytania „Czy potrzebujesz
ubezpieczyć majątek od kradzieży?".
⚠️ Jedyna pozycja wymagająca zmiany w Javie (`Property.readGroups`).

## C. Nietykalne

**Trzon tezy:** `apk.question` (ankieta jest konfiguracją), `apk.answer` (moment WOW — jedna komórka
przestawia rekomendację), `apk.recommendation.reason` (polskie zdania w regule, nie w kodzie),
`apk.reason`, `question.reveal` + `revealedBy`/`revealedWhen`, `apk.question.blocker`.

**Materiał regulacyjny — nudny, ale to argument sprzedażowy wobec zakładu:**

| Element | Podstawa |
|---|---|
| `cover.theft` z odmową przy braku zabezpieczeń | rek. 8.2.c — **jedyny pozostały materiał** po usunięciu EXCLUSIONS w §24/§25 |
| `cover.flood` + `zone.flood` + `variant.availability` | 8.2.c + jedyne strefy geograficzne |
| `capRatio`/`capAbsolute`/`capLabel` | rek. 10.2 — obowiązek informowania o ograniczeniach |
| `sum.ratePerSqm` | rek. 8.2.b + jedyna **trójwymiarowa** tabela w profilu |
| `factor.period` | rek. 8.2.b, trzy wyjścia z jednej tabeli |
| kolumna `prefill` + „Dane z CRM" | rek. 8.3 |
| trzy warianty + tabela różnic | rek. 9.3 |
| krok 4 — oferta z załącznikiem | rek. 8.7 + 8.8 |
| referencje pakiet→pokrycia (D-22) | jedyne prawdziwe referencje w repo |
| testy reguł (`@tests` + batch) | ⚠️ **jedyne w repozytorium — i demo o nich nie mówi** |

⚠️ Sformułowanie: rek. 8.7 wymaga **udokumentowania wykazującego zgodność**, nie załącznika.
Załącznik jest jednym z dopuszczalnych sposobów (8.8). Mówić „rekomendacja wymaga dowodu, a to jest
nasz dowód", nie „wymaga załącznika".

## D. Trzy warianty zasięgu

Stan wyjściowy: **31 tabel · 19 funkcji · 3 flow · 26 elementów + 12 referencji · głębokość 8 ·
11 pól w kroku 2 · 7 pokryć.**

| Wariant | Zakres | Po cięciu | Co widać inaczej |
|---|---|---|---|
| **Lekkie** | poz. 1–7 | **24 tabele · 16 funkcji · 2 flow**, krok 2: 11 → 8 pól | tylko zniknięcie „Franszyza 0 zł" i pięciu pól; kroki 1, 3, 4 identyczne |
| **Średnie** ✅ | + poz. 8–11 | **21–22 tabele · 16 funkcji · 2 flow · głębokość 6–7 · 6 pokryć** | porównanie 6 wierszy zamiast 7, krok 2: 7 pól, dwa kliknięcia bliżej w Studio |
| **Ostre** | + poz. 12–15 | **17–18 tabel · 15 funkcji · 2 flow · 5 pokryć · 5 grup pytań** | znika stawka kwotowa, alternatywa budżetowa (zdanie D-10), konfrontacja oszacowania z sumą |

**Rekomendacja: średnie.** Zdejmuje z ekranu Studia jedną trzecią konfiguracji, nie ruszając ani
jednego artefaktu niosącego tezę ani obowiązek regulacyjny.

## Trzy rzeczy poza listą

**1. Cięcie rozwiąże połowę problemu; drugą połową jest scenariusz.** *Great Demo!*: zacznij od
ekranu z wynikiem, dojdź najkrótszą drogą, warstwy odsłaniaj **tylko na pytanie z sali**. Przy tym
reżimie 31 tabel nie boli, bo otwierasz jedną. Ale przy 21 otwarcie drzewa na chybił trafił też nie
boli — i to jest cała różnica.

**2. Demo adresuje jedno z ośmiu kryteriów zakupowych BRMS.** Audytu i **testowania reguł** nie
opowiada wcale, mimo że od 9 września testy w repo są. Minuty zwolnione cięciem warto wydać na drugi
cykl: „a teraz pokażę, jak sprawdzam, że ta zmiana niczego nie zepsuła". To mocniejsze niż siódme
pokrycie.

**3. Wersjonowania datą nie trzeba odtwarzać w majątku.** MOTO i LIFE mają po dwa regiony, w tym
regulacyjny z tym samym progiem `2027-07-01`. Zdolność jest zademonstrowana podwójnie, w demach,
które są dokładnie o niej.

## Znaleziska poboczne

- ⛔ **`riskFactor` nigdy nie trafia na ekran** — potwierdzone: w `frontend/src` występuje wyłącznie
  w `models/property.ts` i w dwóch fixture'ach testowych.
- ⛔ **`quote.property.renovated` nie jest czytane przez żadną regułę** — jedyne wystąpienie poza
  katalogiem pytań to deklaracja typu w `context.def:46`.
- ⛔ **Kolumna `partOf` jest pusta we wszystkich 29 wierszach**, a `shortLabel` wypełniona w 13 —
  wyłącznie dla pól przedmiotu ubezpieczenia. Kolumna zbudowana dla kroku 1 służy dziś tylko krokowi 2.
- ⚠️ **§22.5 (D-15) opisuje stan, którego nie ma.** Dokument mówi „rekomendacja NAD pakietami";
  `property.html:85-87` ma `app-property-comparison` **przed** `app-apk-recommendation`. To nie jest
  ciche odwrócenie — szablon niesie komentarz uzasadniający tę kolejność („warunek spełniony przez
  to, że rekomendowany wariant jest w tabeli zaznaczony"). Nieaktualna jest **dokumentacja**.
