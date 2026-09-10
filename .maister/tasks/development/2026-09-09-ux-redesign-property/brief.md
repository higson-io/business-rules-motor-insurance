# Redesign UX dema PROPERTY — ustalenia i zakres

**Data:** 2026-09-09
**Źródło:** `makiety-ux.pdf` (w tym katalogu) — przeskalowana kopia `Demo Higson-5-11.pdf`.
Oryginał w pełnej rozdzielczości: `~/Downloads/Demo Higson.pdf`, 11 stron.

---

## 1. Co właściwie dostaliśmy

Pełna teczka od UX-a ma 11 stron i dzieli się na dwie części, czego sam plik nigdzie nie mówi:

| Strony | Co to jest | Jak ustalone |
|---|---|---|
| 1–4 | **zrzuty obecnej aplikacji** | tło `#F5F5F5` i teal `#317989` co do bitu zgadzają się z `_variables.less` i `theme.scss`; podpis „wyliczona z metrażu i standardu wykończenia" jest dosłownie z `property.html` |
| 5–11 | **redesign** | inny teal (`#2AA399`), inne tło (`#F3F5F9`), inne podpisy |

Wycinek `Demo Higson-5-11.pdf` to sam redesign. Pięć różnych ekranów: strony 3 i 4 wycinka są
identyczne, strona 5 to wcześniejsza iteracja strony 6.

⛔ **Wiążąca jest iteracja ze strony 6/7**, nie ze strony 5. Strona 6 niesie komplet (tabela
+ rekomendacja + stopka nawigacji), strona 5 urywa się na tabeli i ma inne podpisy pokryć.

### Czego makieta NIE pokrywa

- **krok 2 „Przedmiot ubezpieczenia" — ani jednej strony.** Trzynaście pól, cały krok.
- stany brzegowe: wariant niedostępny z powodem, pytanie blokujące, błąd ładowania, brak rekomendacji
- widok mobilny — wszystkie artboardy są desktopowe (1920 px)

## 2. Wniosek nadrzędny

To **skórka na istniejące demo**, nie nowy przepływ. Cztery kroki, ankieta jedno pytanie na ekran,
tabela wariantów z badge'em i limitami, blok „Proponujemy wariant X / Pokrywa 4 z 4 / Alternatywa",
oferta z notatką o odstępstwie i załącznikiem APK — wszystko to już jest i zgadza się co do układu.
Około 80% pracy to LESS.

Poza LESS-em wychodzą cztery pozycje: lista pytań w kroku 1 (D-2), pytania złożone (D-3),
kolor (D-1) i to, czego makieta nie pokrywa (D-9).

## 3. Decyzje

### D-1 — kolor akcji: `#2AA399`, wprost z makiety

⚠️ **To jest świadome odstępstwo od `accessibility.md` i tak ma być zapisane, a nie przemilczane.**
Zmierzone kontrasty:

| | na białym | na tle strony |
|---|---|---|
| `#2AA399` (przyjęty) | **3,09:1** | **2,83:1** |
| `#317989` (poprzedni) | 4,97:1 | 4,55:1 |
| `#207A72` (odcień makiety przyciemniony — odrzucony) | 5,14:1 | 4,71:1 |

Próg z `accessibility.md` to 4,5:1 dla drobnego tekstu. W makiecie teal niesie nie tylko nagłówki,
ale i aktywną zakładkę, numerację uzasadnienia oraz biały napis na przycisku — czyli tekst, którego
ten próg dotyczy. Decyzja właściciela dema: wygląd makiety ma pierwszeństwo.

➡️ Do zrobienia: dopisać to odstępstwo do `standards/frontend/css.md` razem z liczbami, żeby
następna osoba nie „naprawiła" koloru jako pomyłki — dokładnie tak, jak `theme.scss` opisuje
przyciemnienie portalowego `#3F8797`.

### D-2 — lista pytań w kroku 1: tak, ze zmianą w Studio

Lewa kolumna z numerowanymi pozycjami wymaga **krótkiej nazwy** pytania („Wartość nieruchomości"),
różnej od jego treści („Na ile szacujesz wartość swojej nieruchomości?"). D-20 zrobił z treści
pytania nazwę elementu, więc druga nazwa musi mieć własne miejsce.

⛔ **ZREALIZOWANE INACZEJ, NIŻ ZAPOWIADAŁ TEN AKAPIT.** Pierwotnie: „atrybut typu `QUESTION`, nie
tabela — nie ma drugiego wymiaru (§32/D-23)". Po przeczytaniu `demo.property.apk.question`
okazało się, że decyzja jest już podjęta gdzie indziej i przeciwnie: ta tabela **z założenia**
trzyma „wszystko, co jest o pytaniu STAŁE" i niesie już `position`, `required`, `options`,
`labels`, `prefill`, `revealedBy`, a dla pytań spoza drzewa także `text`, `kind` i `contextPath` —
same rzeczy bez drugiego wymiaru. §17 przeniósł je tam **z** atrybutów elementu.

Krótka etykieta poszła więc **kolumną `shortLabel`** tej tabeli. Zysk poza spójnością: dokładanie
atrybutu do typu `QUESTION` znaczyłoby wejść w W-29 po jedną linijkę tekstu.

⚠️ Ryzyko z D-20 (dwie nazwy tego samego, rozjeżdżające się niewidocznie) tu **nie zachodzi**:
treść pytania i pozycja spisu są widoczne na ekranie **naraz**, więc rozjazd rzuca się w oczy.

⚠️ **W-27 potwierdzone w praktyce**: zmiana kolumn przenumerowała poziomy (`10097` → `10166`)
i wszystkie wiersze. Kolejność, która zadziałała: zmień strukturę → `open_decision_table` →
**przeczytaj macierz na nowo** → dopiero wtedy `update` po świeżych id → przeczytaj jeszcze raz
i policz wiersze.

⚠️ Etykiety z makiety („Rodzaj nieruchomości", „Lokalizacja", „Historia szkód", „Data rozpoczęcia
ochrony") **nie pasują do tych pytań** — opisują przedmiot ubezpieczenia, czyli krok 2. Projektant
wymyślił prawdopodobne tematy. Wpisane zostały etykiety do pytań, które naprawdę istnieją.

### D-3 — pytania złożone: tak, konfigurowalne, kolumną `partOf`

Pytanie może mieć kilka pól. Zasady zostały te same, co w pierwotnym projekcie:

- licznik „Pytanie 6 z 6" liczy **rodziców**, nie pola
- `revealedBy` biegnie **między rodzicami**
- pytanie jest domknięte, gdy odpowiedziano na wszystkie **wymagane** pola; gdy żadne pole nie jest
  wymagane — gdy odpowiedziano na choć jedno (inaczej pytanie z samych pól nieobowiązkowych byłoby
  ślepym zaułkiem, dokładnie jak oczekiwana składka po §22)
- na drucie: `PropertyQuestionDto.fields`, puste dla pytania prostego — jeden poziom zagnieżdżenia

⛔ **ZMIENIŁ SIĘ MECHANIZM.** Zapowiadana była kolekcja `FIELDS` pod elementem pytania. Odrzucona
po zobaczeniu, jak naprawdę zbudowany jest katalog: **elementami drzewa są tylko pytania APK**,
a trzynaście pól przedmiotu ubezpieczenia to same wiersze tabeli, z `text`/`kind`/`contextPath`
w kolumnach. Kolekcja pod elementem działałaby więc wyłącznie w jednej z dwóch gałęzi.

Zamiast tego: **kolumna `partOf`** w tym samym katalogu. Wiersz z wypełnionym `partOf` nie jest
osobnym pytaniem, tylko polem wskazanego. Działa jednakowo w obu gałęziach, kosztuje jedną kolumnę
i **korzysta z mechanizmu, który już istniał** — „pytanie, które nie jest elementem drzewa".

⛔ To wciąż **nie** jest grupowanie po ekranie, którego zabrania §31. `partOf` mówi „to pole należy
do tamtego pytania" — zdanie o przedmiocie rozmowy. Odrzucone pozostaje `SCREEN`/`GROUP`, czyli
„te pytania pokazują się razem".

⚠️ Model po stronie Javy zostaje **płaski**; zagnieżdżenie robi dopiero konwerter. Dzięki temu
odpowiedzi, wartości demonstracyjne i każde wyszukanie po kodzie działa bez wiedzy o rodzicu.

**Zbudowane i włączone:** `EXPECTATIONS` — „Twoje oczekiwania wobec ochrony", element domeny
z `KIND = GROUP` i bez `CONTEXT_PATH`, z polami `BUDGET` i `PROPERTY_VALUE`. Ankieta ma przez to
**sześć pytań zamiast siedmiu**. Cofa się to wyczyszczeniem kolumny `partOf` w dwóch wierszach.

### D-4 — podpis pod nazwą pokrycia: `capLabel` w obu miejscach

Właściciel nie miał zdania, więc rozstrzyga wiążąca iteracja: strony 6 i 7 pokazują limity
(„Gotówka — 10% sumy ruchomości, nie więcej niż 10 000 zł"), a nie opisy zakresu. To jest zarazem
obecne zachowanie kodu — zerowa zmiana.

`PropertyCoverDto.description` zostaje nieużyty na tych dwóch ekranach. Przełączenie to jedna linia
w `comparison.html`, gdyby UX wrócił do wersji ze strony 5.

### D-5 — „W cenie" w Podsumowaniu: odrzucone

Makieta chowa sumy elementów stałych i ruchomości pod słowem „W cenie". Zostają kwoty
(116 160 zł / 79 200 zł): wylicza je silnik reguł i to jest to, co demo ma pokazywać.

To jedyne miejsce, w którym świadomie odchodzimy od treści makiety.

### D-6 — podświetlona kolumna: obie, różnie *(default, bez sprzeciwu)*

Makieta podświetla kolumnę proponowaną; kod podświetla wybraną. W makiecie to ten sam wariant, więc
różnicy nie widać. Zostaje tint makiety na **proponowanej** i osobny znacznik na **wybranej** —
inaczej po zmianie wyboru ekran przestaje pokazywać, co klient wybrał.

### D-7 — załącznik APK w ofercie: zwijany, domyślnie rozwinięty *(default)*

Makieta ma chevron. Domyślnie rozwinięty, bo to dokument.

### D-8 — kolejność prac

Etap 1 (skórka) rusza od razu i jest niezależny od D-2 i D-3.

### D-9 — krok 2 i stany brzegowe *(default)*

Bez makiety. Robione przez analogię do reszty, założenia spisane w run-logu w miarę powstawania.

## 4. Luki, ekran po ekranie

Legenda: `[CSS]` sam styl · `[NG]` szablon/komponent · `[API]` DTO i konwerter · `[HIG]` reguły w Studio.

### Krok 1 — Analiza potrzeb

| Makieta | Stan | Klasa |
|---|---|---|
| lewa kolumna: 7 pytań, numerowane, bieżące podświetlone | nie ma | `[NG]` `[CSS]` |
| krótkie nazwy w liście | brak źródła danych | `[HIG]` `[API]` |
| pasek postępu | jest segmentowy; makieta zastępuje go listą | `[NG]` |
| Tak/Nie jako dwa duże przyciski | jest `mat-button-toggle-group` | `[CSS]` |
| pytanie o kilku polach | jedno pytanie = jedna kontrolka | `[HIG]` `[API]` `[NG]` |

### Karta klienta

Wersaliki na etykietach, kolumna „UBEZPIECZAJĄCY" nad nazwiskiem, e-mail i telefon w dwóch liniach
zamiast `email · phone`, inicjały teal w jasnozielonym kole. `[CSS]` + kilka linii `[NG]`.

### Pasek kroków

Numeracja „1. Analiza potrzeb", podkreślenie aktywnego, ✓ zamiast numeru przy zamkniętych.
`isDone` i `step-bar__mark` już są — czysty `[CSS]`.

### Krok 3 — porównanie i rekomendacja

| Makieta | Stan | Klasa |
|---|---|---|
| ✓ / — teal, tint na kolumnie proponowanej | jest, inny wygląd | `[CSS]` |
| cena i „Wybieram" w jednym bloku | dwa osobne wiersze tabeli | `[NG]` `[CSS]` |
| kafelek „Wariant Pełny (Rekomendowany)" z opisem | `PropertyVariantDto.description` jest, nieużywany | `[NG]` |
| blok rekomendacji: teal ✓, teal numeracja, „SKŁADKA" po prawej | układ 1:1, inny wygląd | `[CSS]` |

### Krok 4 — Oferta

Bursztynowy blok „Wybór węższy niż rekomendacja" z ⚠ i czerwonym ⊗ przy niepokrytej potrzebie —
klasa `offer__choice--departs` istnieje, brakuje wyglądu. Etykieta „OFERTA" nad numerem. Chevron
przy załączniku. Reszta 1:1.

## 5. Plan

| Etap | Zakres | Zależy od |
|---|---|---|
| **1 — skórka** | tokeny koloru i tła, typografia, kontener strony, karta klienta, pasek kroków, tabela porównania, blok rekomendacji, oferta, podsumowanie | — |
| **2 — lista pytań** | atrybut w Studio, pole w `Question`/DTO/konwerterze, dwukolumnowy układ kroku 1 | D-2 |
| **3 — pytania złożone** | kolekcja `FIELDS`, `fields` na drucie, render N kontrolek, domknięcie po wymaganych polach | D-3 |
| **4 — domknięcie** | krok 2, stany brzegowe, RWD | D-9 |

Etapy 1 i 2 są rozłączne. Etap 3 zakłada 2 (oba ruszają katalog pytań).

## 6. Testy

- **etap 1**: bez nowych testów — zmiana wyglądu nie jest bramką parity (`css.md`)
- **etap 2**: test mutacyjny — zmiana krótkiej etykiety w Studio przestawia listę bez restartu
- **etap 3**: pytanie złożone nie domyka się, dopóki brakuje wymaganego pola; licznik nie rośnie
  o pola (`test-writing.md`: udowodnić, że test pada pod mutacją, którą ma łapać)
