# Dziennik wykonania

## Etap 1 — skórka (2026-09-09) ✅

Zrobione i sprawdzone w przeglądarce na uruchomionej aplikacji (`ng serve` + WAR na `:48080`).
Build czysty, `ng test --watch=false` → **20 plików, 60 testów, wszystkie zielone**.

### Tokeny

| Token | Było | Jest | Skąd |
|---|---|---|---|
| `@color-action` | `#317989` | `#2aa399` | zmierzone z makiety (D-1) |
| `@color-action-fill` | `#3f8797` | = `@color-action` | makieta ma jeden teal na tekst i wypełnienia |
| `--mat-sys-primary` | `#317989` | `#2aa399` | jak wyżej |
| `@color-ground` | `#f5f5f5` | `#f3f5f9` | zmierzone z makiety |
| `@color-action-soft` | `rgba(93,199,213,.12)` | `#ebf7f6` | nieprzezroczysty — ten sam tint stoi na bieli i na tle |
| `@color-surface-muted` | — | `#f8fafc` | nowy: karta klienta, kolumna proponowana |
| `@color-border-soft` | — | `#e5e7eb` | nowy: obrys tych powierzchni |
| `@color-tile-info` | — | `#eff6ff` | nowy: kafelek wariantu |
| `@color-warn-amber*` | — | `#fffbeb` / `#fde68a` / `#b45309` | nowy: blok odstąpienia w ofercie |

Projektant pracował na palecie Tailwinda — stąd wartości wyglądające jak `slate-50`, `blue-50`,
`amber-50`. Zapisane jako liczby, bo tej palety w projekcie nie ma.

⚠️ Zmiana koloru akcji jest **globalna** — moto i życie też ją dostały. Odstępstwo od progu
kontrastu zapisane w `standards/frontend/css.md`.

### Ekrany

- **kontener** — całe demo w jednej białej karcie (`:host` w `property.less`), tytuł 30px/700
- **karta klienta** — siatka czterech równych kolumn zamiast zawijanego wiersza, etykieta
  „Ubezpieczający" nad nazwiskiem, kontakt w dwóch liniach, awatar w tincie akcji, obrys zamiast cienia
- **pasek kroków** — nowy wariant `appearance="plain"` (zakładki na kresce, numer w linii, ✓ przy
  zamkniętych). ⛔ Wariant, nie zmiana globalna: moto i życie zostają przy `panel`
- **tabela porównania** — bez własnego panelu, tint na kolumnie proponowanej, ✓/— jako znaczniki
  w kolorze akcji, cena i „Wybieram" w jednej komórce, przycisk wypełniony tylko w kolumnie proponowanej
- **podsumowanie** — obrys zamiast cienia, ciemny tytuł, kwoty zostają (D-5), nowy kafelek
  z nazwą i opisem wariantu proponowanego (`PropertyVariantDto.description`, dotąd nieużywane)
- **rekomendacja** — komponent wspólny, więc obudowa sterowana właściwościami niestandardowymi
  (`--reco-border`/`--reco-radius`/`--reco-shadow`); ✓ przy potrzebach i numeracja uzasadnienia
  w kolorze akcji
- **oferta** — obrys zamiast cienia, „OFERTA" szara zamiast turkusowej, nagłówki sekcji 700,
  blok odstąpienia od rekomendacji jako pełny bursztynowy panel z ⚠ i ⊗ zamiast trzypikselowej kreski
- **ankieta** — zdjęty panel (karta w karcie); układ dwukolumnowy należy do etapu 2

### Poprawka po pierwszym zrzucie

Kolumna proponowana dostawała tint **i** obrys naraz, dopóki wybór pokrywał się z propozycją —
dwa wyróżnienia w jednym miejscu. `Comparison.isPicked()` odzywa się teraz dopiero wtedy, gdy wybór
różni się od propozycji, tak samo jak `Property.choiceDiffers` przy cenie w podsumowaniu.

### Znalezione po drodze, poza zakresem redesignu

1. ⛔ **`frontend/proxy.conf.json` był nieaktualny i naprawiony.** Cele wskazywały
   `http://localhost:48080` bez ścieżki kontekstu `/demo`, a lista obejmowała tylko cztery korzenie
   motoryzacyjne. Skutek: `ng serve` nie działał dla ŻADNEGO dema — API zwracało stronę błędu
   Tomcata, a ekran pokazywał baner „Nie udało się wczytać wyceny" i wyglądał na zepsuty front.
   Doszły `/life`, `/property`, `/profilers`, wszystkie cele z `/demo`.

2. ⚠️ **Strefa powodziowa pokazuje surowy kod `Z0`**, a makieta ma tam „Brak ryzyka".
   `PropertyQuoteDto.floodZone` niesie kod, nie etykietę. To nie jest usterka stylu — brakuje
   słownika po stronie reguł albo mapowania w konwerterze. Do rozstrzygnięcia w etapie 4;
   po myśli tego projektu etykieta należy do reguł, nie do Javy.

3. ⚠️ Ostrzeżenie o budżecie bundla (`838 kB` przy limicie `500 kB`) jest **sprzed** tych zmian —
   otwarta pozycja z migracji sierpniowej, nie regres.

### Nie zrobione w tym etapie (świadomie)

- układ dwukolumnowy kroku 1 z listą pytań — etap 2
- pytania złożone — etap 3
- krok 2, stany brzegowe, RWD — etap 4

---

## Etap 2 — spis pytań w kroku 1 (2026-09-09) ✅

**Higson.** Dwie kolumny dołożone do `demo.property.apk.question` w JEDNEJ operacji (`shortLabel`
i `partOf` — druga na zapas dla etapu 3), żeby raz przejść przez W-27. Krótkie etykiety wpisane dla
wszystkich 20 pytań: siedmiu z gałęzi `NEEDS` i trzynastu pól przedmiotu.

⚠️ **W-27 potwierdzone.** Zmiana kolumn przenumerowała poziomy (`10097` → `10166`) i wszystkie
wiersze. Kolejność, która działa: struktura → `open_decision_table` → **ponowny odczyt macierzy** →
`update` po świeżych id → **odczyt kontrolny i policzenie wierszy** (21, bez duplikatów).

**Java.** `Question.shortLabel`, odczyt w `Property.applyCatalogue`, pole w `PropertyQuestionDto`
i konwerterze.

**Angular.** `outline()` — spis liczony po REGULE widoczności, nie po odsłonięciu; pozycja
nieodsłonięta jest widoczna, ale nieklikalna. Dwukolumnowy układ kroku 1, pytanie wyrównane do
lewej (środkowanie straciło powód, gdy obok stanął spis). **Segmentowy pasek postępu usunięty** —
spis mówi to samo i więcej, a dwie miary postępu to jedna za dużo.

⛔ **Etykiety z makiety nie pasowały do tych pytań.** „Rodzaj nieruchomości", „Lokalizacja",
„Historia szkód", „Data rozpoczęcia ochrony" opisują **przedmiot ubezpieczenia**, czyli krok 2;
siedem pytań kroku 1 to potrzeby („Czy chcesz ubezpieczyć swój majątek na wypadek powodzi?").
Projektant wymyślił prawdopodobne tematy, nie odczytał istniejących. Wpisane zostały etykiety do
pytań, które naprawdę istnieją.

## Etap 3 — pytania złożone (2026-09-09) ✅

**Mechanizm inny niż w pierwotnym briefie** — uzasadnienie w D-3. Zamiast kolekcji `FIELDS` pod
elementem: kolumna **`partOf`** w katalogu. Powód: elementami drzewa są tylko pytania APK, więc
kolekcja pod elementem obsłużyłaby jedną z dwóch gałęzi.

**Higson.** Nowy element `QUESTIONS[EXPECTATIONS]` (`KIND = GROUP`, bez `CONTEXT_PATH`), wiersz
katalogu z `revealedBy = RENTED_OUT`, oraz `BUDGET` i `PROPERTY_VALUE` wpięte jako jego pola.

**Java.** `Question.partOf` / `isField()`; `applyReveal` liczy domknięcie rodzica po polach
(`allRequiredAnswered`); konwerter jako **jedyne miejsce**, w którym płaska lista staje się
drzewem. Pole bez rodzica na liście zostaje pytaniem samodzielnym, zamiast zniknąć po cichu.

**Angular.** `settled()` po polach; blok kontrolki wyciągnięty do jednego `ng-template`
(`ngTemplateOutlet`) — stał w tym pliku dwa razy, a pytania złożone dołożyłyby trzeci.

**Efekt w demie:** ankieta ma **6 pytań zamiast 7** — składka i wartość nieruchomości na jednym
ekranie jako dwa pola. Cofa się wyczyszczeniem `partOf` w dwóch wierszach.

## Etap 4 — krok 2, stany brzegowe, RWD (2026-09-09) ✅

- **Krok 2**: dwie kolumny i wyrównanie do lewej zamiast jednej wąskiej kolumny na środku.
  Strona zeszła z **4018 px do 2714 px** — wcześniej trzeba było przewijać w dół, żeby zobaczyć,
  jak zmienia się suma po prawej.
- **Stany brzegowe** sprowadzone do jednej formy: komunikat bramki ankiety, powód niedostępności
  wariantu i notatka o odstąpieniu w ofercie to teraz ten sam bursztynowy blok z ⚠. Wcześniej
  pierwszy miał wpisane z palca `#fff8e6`/`#d99e00`, drugi był szarym zdaniem, trzeci trzypikselową
  kreską z boku.
- **RWD**: zmierzone `scrollWidth == innerWidth` (390/390) na kroku 1 i 3 — brak przewijania
  w bok, spis pytań przechodzi nad pytanie.

### Usterki znalezione i naprawione po drodze

1. ⛔ **`proxy.conf.json`: brak `cookiePathRewrite`.** Tomcat wystawia JSESSIONID ze ścieżką
   `/demo`, a przez proxy przeglądarka widzi adresy pod `/` — więc ciasteczka nie odsyłała
   i KAŻDE żądanie zakładało nową sesję. `QuoteService` jest `@SessionScope`, więc każda odpowiedź
   lądowała w świeżej wycenie i przepadała: ankieta stała na pierwszym pytaniu, mimo że PUT wracał
   z 200. ⚠️ Objaw wygląda dokładnie jak zgubione zdarzenie w komponencie i tam się go najpierw
   szuka.
2. ⛔ **`PRIOR_FLOOD_COVER` pokazywał surowe `true`/`false`.** Wiersz miał **puste** kolumny
   odpowiedzi, a pusta wartość we własnym wierszu nadpisuje `true,false / Tak,Nie` z wiersza
   gwiazdkowego — nie dziedziczy go. Usterka sprzed tych prac, widoczna na ekranie kroku 2.
3. ⚠️ **Kolumna proponowana dostawała tint i obrys naraz** (wprowadzone w etapie 1, poprawione).

### Testy

- frontend: **64 testy, wszystkie zielone** (było 60; trzy nowe o pytaniu złożonym i spisie)
- Java: cały pakiet zielony
- ⚠️ Mutacja sprawdzona zgodnie z `test-writing.md`: podmiana `settled()` na czytanie wartości
  rodzica zamiast pól **wywala** test „advances a compound question once its required field is
  answered". Test nie jest pusty.

### Wciąż otwarte

- **Strefa powodziowa pokazuje kod `Z0`**, makieta ma „Brak ryzyka". Brakuje słownika po stronie
  reguł; etykieta należy do Higsona, nie do konwertera.
- Ostrzeżenie o budżecie bundla (838 kB przy limicie 500 kB) — otwarta pozycja z migracji
  sierpniowej, nie regres.
- „Ile szkód **zgłaszałeś**" — forma tylko męska, w treści pytania w regułach. Nie ruszane.

## Przegląd etykiet po uwadze właściciela (2026-09-09)

Potwierdzone: etykiety wymyślone przez UX-a **nigdy nie trafiły do kroku 1** — przy wdrożeniu
etapu 2 wpisane zostały nazwy do pytań, które naprawdę istnieją. Przegląd wszystkich dwudziestu
pod kątem „czy to jest nazwa POTRZEBY, czy nazwa CECHY PRZEDMIOTU" dał jedną poprawkę:

| kod | było | jest | dlaczego |
|---|---|---|---|
| `POSTCODE` | Kod pocztowy | **Lokalizacja** | słowo z makiety i lepsze: pole zasila strefę powodziową i kradzieżową, więc tematem jest lokalizacja, a nie format wpisu |

Pozostałe dwie etykiety UX-a, które dały się przypisać, były już użyte tam, gdzie pasują
(`Rodzaj nieruchomości` → `PROPERTY_KIND`, `Historia szkód` → `CLAIMS_3Y`). Dwie ostatnie odpadły:
„Dodatkowe ryzyka" nie ma odpowiednika, a „Data rozpoczęcia ochrony" nazywa datę, podczas gdy
`COVER_PERIOD` niesie **okres** — data startu nie jest pytaniem katalogu, tylko polem wyceny.

⚠️ **Etykiety pól przedmiotu są skonfigurowane, ale nigdzie się nie renderują.** Spis pytań
istnieje wyłącznie na kroku 1 (`group() === 'NEEDS'`); krok 2 jest listą z pełną treścią pytań.
Dane są gotowe, brakuje tylko miejsca, w którym miałyby się pokazać — do decyzji.

**Strefy powodziowe zostają kodami** (`Z0`) — decyzja właściciela: dla biznesu są czytelne.
Pozycja skreślona z listy otwartych.

## Przegląd frontu pod kątem defektów i widoczności na demo (2026-09-09)

Mierzone na **zbudowanym WAR-ze**, nie na serwerze deweloperskim, przy 1920×1080 i 1366×768.

### Usunięte podsumowanie z kroku 2 (decyzja właściciela)

Szpalta „Podsumowanie" zeszła z kroku przedmiotu ubezpieczenia. Stała tam z uzasadnieniem, że
zmianę metrażu ma być widać natychmiast — argument prawdziwy, ale przegrywał z kosztem: trzynaście
pól ściśniętych w dwie wąskie kolumny obok szpalty, na którą przez większość kroku nikt nie patrzy,
bo sumy zmieniają się po jednym czy dwóch polach z trzynastu.

Formularz dostał całą szerokość karty i **trzy kolumny** powyżej 1200 px.

### Naprawione defekty

1. ⛔ **Wyszarzone pozycje spisu pytań były nieczytelne.** `opacity: 0.55` na tekście
   `@color-text-muted` schodziło z 5.18:1 do około **2.2:1** — poniżej każdego progu, a na rzutniku
   w jasnej sali po prostu znikało. Stan „jeszcze nie teraz" niosą trzy inne rzeczy naraz (puste
   kółko, jaśniejszy obrys, kursor), więc przezroczystość zabierała kontrast i nie dokładała
   informacji. Mój defekt z etapu 2.
2. ⛔ **Nazwy wariantów nie stały w jednym rzędzie.** `vertical-align: bottom` na nagłówkach tabeli
   przy dwuwierszowym nagłówku kolumny proponowanej (nazwa + badge) spychało „Podstawowy"
   i „Standardowy" o linijkę niżej niż „Pełny". Zmienione na `top`.
3. ⛔ **Podpowiedź „Podpowiedziane: …" zaczynała się 94 px na prawo od swojego pola.** Resztka
   `justify-content: center` po układzie, w którym lista przedmiotu była jedną wyśrodkowaną
   kolumną. Zmierzone, nie oszacowane: `foot.left = 752`, `prefill.left = 846`.
4. ⛔ **Poszarpane kreski między wierszami formularza.** Każde pytanie niosło własną kreskę u góry;
   w siatce sąsiadujące komórki mają różne wysokości (jedna ma pod polem podpowiedź, druga nie),
   więc kreski przestawały się schodzić. Usunięte na rzecz odstępu.
5. ⚠️ **Krok 2 wychodził poza ekran 1920×1080 o 18 pikseli** — czyli strona dostawała pasek
   przewijania po nic, a „prawie się mieści" na rzutniku czyta się jak usterka układu. Ścięte
   przez dolną wyściółkę `app-shell` (3 rem → 2 rem) i odstęp pod paskiem kroków (20 → 16 px).

### Widoczność na prezentacji — zmierzone

| Ekran | 1920×1080 | 1366×768 |
|---|---|---|
| Wybór dema | mieści się | mieści się |
| 1 · Analiza potrzeb | **mieści się** | przewijanie 34 px |
| 1 · pytanie złożone | **mieści się** | przewijanie 117 px |
| 2 · Przedmiot ubezpieczenia | **mieści się** (było 4018 px przed etapem 4) | przewijanie 330 px |
| 3 · Rekomendacja i składka | przewijanie 465 px | przewijanie 873 px |
| 4 · Oferta | przewijanie 637 px | przewijanie 1195 px |

⛔ **Kroki 3 i 4 przewijają się z konstrukcji i nie należy tego „naprawiać".** Krok 3 niesie
siedmiowierszową tabelę porównawczą **i** rekomendację z uzasadnieniem — zmieszczenie obojga na
1080 px wymagałoby ściśnięcia wierszy tak, że przestałyby być czytelne z sali. Blok rekomendacji
wystaje zza dolnej krawędzi („SKŁADKA"), co jest właściwym sygnałem, że jest dalszy ciąg. Krok 4
jest dokumentem i przewija się jak dokument.

⚠️ Na 1366×768 przewija się wszystko poza ekranem wyboru. Przy tej wysokości nie da się inaczej —
sam nagłówek z kartą klienta i paskiem kroków zajmuje 370 px.

### Brak przewijania w poziomie

Zmierzone `scrollWidth == innerWidth` na 390, 1366 i 1920 px.

## Zdjęte podpisy „Podpowiedziane: …" (2026-09-09, decyzja właściciela)

Podpis pod polem, mówiący skąd wzięliśmy wartość („Podpowiedziane: Metraż z poprzedniej polisy"),
zszedł z ekranu w demie majątkowym.

⚠️ **To była treść z rek. 8.3**, zostawiona świadomie w §21 — tamten przebieg kasował autoreklamę
narzędzia i wprost odróżnił od niej informację o pochodzeniu danych („Provenance copy stays […]
that is rec. 8.3, not a boast"). Wypada więc z demo element, który tę rekomendację pokazywał.
Odnotowane, nie przemilczane.

Co zostało nietknięte: `prefillSource` nadal przychodzi z serwera i nadal jest w katalogu reguł —
zniknął sam podpis. Zostają też dwa inne miejsca pokazujące pochodzenie danych, bo dotyczyły
czego innego niż pojedyncze pole: „Kartoteka klienta (CRM)" w karcie klienta i „Dane pobrane z:"
w ofercie. Moto i życie mają ten sam podpis przez komponent wspólny i nie były ruszane.

Przy okazji usunięte martwe `.apk__foot` / `.apk__prefill` — stopka pytania nie ma już czego nieść
(„nie chcę podać" zdjął §22). Test „shows the prefill source…" zamieniony na strażnika decyzji:
`does not print the prefill source, even when the server sends one`. ⚠️ Pilnuje DECYZJI, nie
mechanizmu — dopisanie podpisu z powrotem to jedna linijka w szablonie i wyglądałoby jak naprawa
brakującej funkcji, a nie cofnięcie ustalenia.

Efekt uboczny: wiersze formularza przedmiotu mają teraz równe wysokości, więc krok 2 mieści się na
1920×1080 z zapasem ~140 px zamiast dokładnie co do piksela.

## Wdrożenie na 120 (2026-09-09) ✅

`http://10.222.20.120:58090/demo` — stos `apk-demo`, cztery kontenery, wszystkie wstały.

**Kopia przed zmianą:** `~/apk-demo-h2-backup-20260909-131746.tar.gz` (101 MB) na 120. Stara paczka
odłożona jako `~/apk-demo-bundle-prev-*`. Cofnięcie: `down -v`, rozpakowanie kopii do wolumenu,
`up`.

⛔ **Aktualizacja to nie instalacja.** `install.sh` przerywa, gdy kontenery `apk-demo-*` już
istnieją, więc nie da się nim nadpisać działającego stosu. Trzeba `docker compose -p apk-demo
down -v`, odłożyć starą paczkę i dopiero rozpakować nową.

⛔ **Sama podmiana obrazu by nie wystarczyła.** Aplikacja czyta teraz kolumny `shortLabel`
i `partOf`; na starej bazie serwera zwróciłaby 500 (`UnknownLevelNameException`) na każdej wycenie
majątkowej. Dlatego baza z konfiguracją jedzie w paczce i **nadpisuje** tę na serwerze — co znaczy,
że ewentualne edycje zrobione w Studio na 120 po 7 września przepadły. Stąd kopia.

⚠️ **Na 120 nie ma `curl`.** Pętla czekająca na wstanie aplikacji wisiała w nieskończoność, bo
polecenie nie istnieje — a nie dlatego, że aplikacja nie wstawała. Weryfikacja poszła z maszyny
deweloperskiej; porty 58xxx są wystawione.

⚠️ Tar z macOS wkłada do paczki pliki `._*` (AppleDouble). Nieszkodliwe, ale zaśmiecają katalog —
`find ~/apk-demo-bundle -name '._*' -delete` po rozpakowaniu.

### Zweryfikowane na wdrożonym środowisku

- `GET /demo/` i `GET /demo/property` → 200
- sześć pytań kroku 1 z krótkimi etykietami, `EXPECTATIONS` z dwoma polami
- serwowany bundle `main-EMR37ZGZ.js` zawiera `apk__outline`
- przejście przez wszystkie cztery kroki w przeglądarce; kroki 0–2 mieszczą się na 1920×1080

## Uwagi z testów + bramkowanie kroków i nowy proces (2026-09-09)

### 1. „Ten przycisk przejdź do następnego był cały czas wyszarzały" — potwierdzone i naprawione

⛔ **To nie był lag.** Zmierzone na wdrożonym 120: po kliknięciu odpowiedzi kursor przeskakiwał na
następne pytanie w **80 ms**, więc klient patrzył ZAWSZE na pytanie nieudzielone, a przycisk był
istotnie martwy — nie było dokąd iść. Zapalał się wyłącznie na ostatnim pytaniu, i to tylko
dlatego, że jest nieobowiązkowe.

⛔ **W pliku siedziały dwa sprzeczne projekty.** Komentarz przy `next()` deklarował nawigację jawną
(„ekran nie może uciec prowadzącemu"), a `linkedSignal` kursora robił przeskok jako efekt uboczny
przeliczenia. Osobno każde brzmiało sensownie — dlatego nikt tego nie zauważył.

Kursor zostaje teraz tam, gdzie stoi; liczy się od nowa tylko wtedy, gdy nie ma czego utrzymać.

### 2. „Już się pokazuje, że jest wystawiona" — dwie osobne rzeczy

- **Słowo**: „Wystawiona" → **„Data oferty"**. Formalnie oferta JEST wystawiana, ale obok „Ważna do"
  i „Płatność do" czytało się jak „polisa już wystawiona".
- **To, że oferta w ogóle była** — rozwiązane bramkowaniem kroków, patrz niżej.

### 3. „Czy odstępstwo nie powinno być w załączniku" — tak

Notatka „Wybór węższy niż rekomendacja" przeniesiona **do środka** załącznika, jako jego pierwszy
blok. Załącznik to dokument, który zostaje przy kliencie (rek. 8.7) — informacja o odstąpieniu ma
się przechować razem z analizą, do której się odnosi.

⚠️ Poza załącznikiem została gałąź awaryjna: notatka pokazuje się zawsze, a załącznik tylko gdy każą
reguły. Bez niej odstąpienie zniknęłoby z dokumentu razem z załącznikiem.

### 4. Bramkowanie kroków (na życzenie właściciela)

⛔ **Odwraca decyzję „każdy krok osiągalny zawsze".** Bramkujemy **wyłącznie ruch w przód**; krok
odwiedzony albo bieżący zostaje otwarty na zawsze, bo cofnięcie się do jednej odpowiedzi i pokazanie,
jak przestawia się propozycja, jest tym, po co to demo istnieje.

| Krok | Otwiera się, gdy |
|---|---|
| 2 · Przedmiot | analiza potrzeb domknięta |
| 3 · Rekomendacja | + wszystkie wymagane pola przedmiotu wypełnione |
| 4 · Oferta | + klient **wybrał** wariant |

⚠️ **`selectedVariant != null` NIE nadaje się na tę bramkę** i to była pierwsza, błędna wersja:
serwer podstawia pod to pole wariant rekomendowany, żeby tabela miała co podświetlić, więc nigdy
nie jest puste — zmierzone, po samej ankiecie `selectedVariant = COMPLETE`, choć nikt nic nie
kliknął. Rozróżnienie istniało w domenie od dawna (`PropertyQuote.variantChosen`); wystawione
teraz na drut.

Reguła „czy gałąź domknięta" wyjechała do `core/question-state.ts`, bo potrzebują jej dwa
komponenty. Pięć testów jednostkowych, każdy celujący w inne „uproszczenie", które ją psuje.

### 5. Nowy proces resetuje wszystko

`PUT /property/reset` buduje wniosek od nowa; front woła to **przy każdym wejściu do dema**.
„Wszystkie dema" → kafelek jest więc gestem nowego procesu i nie ma osobnego przycisku.

⛔ Buduje od nowa, a nie czyści pola po jednym — przeoczone pole zostawałoby na ekranie następnej
prezentacji, czyli dokładnie ta resztka, o którą poszło zgłoszenie.

### 6. Ekran wyboru dema bez opisów

Zdjęty podpis pod „Wybierz demo" i opisy z trzech kafelków. To ta sama reklama narzędzia, którą §21
usunął z ekranu majątkowego i **zostawił tutaj jako otwartą pozycję** („the demo picker still
narrates the engine in its subtitle and in the moto/life tiles"). Numer i nazwa zostają, bo
kolejność jest argumentem.

### Zweryfikowane w przeglądarce

```
start                              apk:otw  subject:ZAMK proposal:ZAMK offer:ZAMK
po 1. odpowiedzi                   ...                                  dalej:aktywny, licznik „1 z 6"
po ankiecie                        apk:otw  subject:otw  proposal:otw  offer:ZAMK
po kliknięciu „Wybieram"           apk:otw  subject:otw  proposal:otw  offer:otw
po ponownym wejściu (nowy proces)  apk:otw  subject:ZAMK proposal:ZAMK offer:ZAMK, licznik „1 z 6"
```

69 testów frontowych zielonych (było 64), Java zielona.

## Sprzątanie po reżimach: dwie tabele i cztery atrybuty (2026-09-09)

### `demo.property.offer.terms` → cztery stałe na produkcie

Tabela miała **jeden żywy wiersz** i wiersz gwiazdkowy, do którego nic nie trafiało — bo klucz
(`regime.code`) przyjmuje jedną wartość, odkąd D-17 usunął region i drugi reżim. Zero drugiego
wymiaru, czyli dokładnie ten kształt, który D-23 każe zapisać na elemencie.

Cztery wartości poszły na atrybuty produktu: `OFFER_VALIDITY_DAYS` (30), `OFFER_PAYMENT_DAYS` (14),
`OFFER_NEEDS_ANALYSIS` (true), `OFFER_STATEMENT` (treść oświadczenia). Java czyta je przez nowe
`productText` / `productInt`, obok istniejącego `productFlag`.

### `demo.property.apk.regime` — usunięta

Jeden wiersz, a jej `code` był kluczem **wyłącznie** do `offer.terms`. `name` nie jechało nawet na
drut, odkąd D-17 zabrał ekranowi panel ustawień. Po przeniesieniu warunków straciła ostatniego
czytelnika. Razem z nią zniknęły: `loadRegime`, ścieżka kontekstu `regime.code`, pole
`PropertyQuote.regime` i atrybut `APK_REGIME`.

⚠️ Klasy `Regime` i `RegimeReader` **zostają** — są w pakiecie wspólnym `apk` i używają ich moto
oraz życie. Usunięte zostało wyłącznie użycie majątkowe.

### `OVERWRITE_MANUAL_SUM` i `FREEZE_ISSUED_OFFER` — zdjęte z atrybutów

⚠️ **To cofa część §33/D-23.** Oba przyjechały tam z tabeli `quote.recalculation` właśnie po to,
żeby reguły „suma podana ręcznie wygrywa" i „wystawiony dokument się nie zmienia" były widoczne
w konfiguracji, a nie jako `if` w Javie. Zdjęte na decyzję właściciela: pokrętła, których nikt
nigdy nie przekręcił, zaśmiecały ekran atrybutów. Wracają do kodu jako warunki, z komentarzem
mówiącym skąd przyszły — gdyby miały być przestawialne, wracają atrybutami.

### Atrybuty produktu po zmianie — trzy grupy zamiast dwóch

| Grupa | Atrybuty |
|---|---|
| 1 · Wycena | `SUM_INSURED`, `RISK_FACTOR` |
| 2 · Warunki oferty | `OFFER_VALIDITY_DAYS`, `OFFER_PAYMENT_DAYS`, `OFFER_NEEDS_ANALYSIS`, `OFFER_STATEMENT` |
| 3 · Notatka o wyborze | `CHOICE_KIND`, `CHOICE_NOTE` |

Osiem atrybutów zamiast ośmiu — ale dwa wskaźniki na tabele zamieniły się w cztery czytelne stałe,
a dwa martwe pokrętła zniknęły.

### Pułapki, które zadziałały

⚠️ **W-25 potwierdzone i rozbrojone**: ścieżkę parametru wysyła się do API **bez** prefiksu `$p` —
API dokłada go sam. Wysłanie z prefiksem podwaja go. Sprawdzone odczytem elementu po zapisie:
`demo.property.offer.choiceKind [kind]`, pojedynczo.

⚠️ **W-29**: `action` podane jawnie przy każdym atrybucie (`NO_CHANGES` dla zachowywanych,
`ADD_NEW_DEFAULT` dla nowych). Wartości na elemencie nietknięte.

⚠️ **Opis typu domeny ma limit 512 znaków** — pierwsza próba padła na `Value too long for column
DESCRIPTION`. Komunikat jest czytelny, ale limitu nie ma nigdzie w dokumentacji narzędzia.

### Zweryfikowane

`GET /demo/property` → 200, oferta: wystawiona 09.09, ważna do 09.10 (+30 dni), płatność do 23.09
(+14 dni), załącznik APK dołączony, oświadczenie w wersji KNF. 69 testów frontowych zielonych,
Java zielona, snapshot zsynchronizowany (dwie tabele skasowane, `attributes.txt` i `PROPERTY.xml`
przepisane z eksportu).

## Nazwane grupy atrybutów domeny (2026-09-09)

Właściciel pokazał w Studio mechanizm, którego nie znałem: grupa atrybutów ma **nazwę**, nie tylko
numer. Pole nazywa się różnie zależnie od endpointu — `group` w definicji typu, `groupName`
w szczegółach elementu i w snapshocie (`<typeAttribute>` w `@def/PROPERTY.xml`). `groupOrder: 0`
znaczy „poza grupą".

⛔ **Sam `groupOrder: 1/2/3` bez nazw** — czyli to, co zrobiłem wcześniej — daje bezimienne bloki
rozdzielone odstępem i nie wyjaśnia niczego.

`PRODUCT` ma teraz trzy nazwane grupy w kolejności procesu:

| Grupa | Atrybuty |
|---|---|
| Podstawy wyceny | `SUM_INSURED`, `RISK_FACTOR` |
| Warunki oferty | `OFFER_VALIDITY_DAYS`, `OFFER_PAYMENT_DAYS`, `OFFER_NEEDS_ANALYSIS`, `OFFER_STATEMENT` |
| Notatka o wyborze klienta | `CHOICE_KIND`, `CHOICE_NOTE` |

⚠️ **Schemat `higson_update_domain_type` nie wymienia pola `group`, ale ono przechodzi** —
sprawdzone na typie `ZZZ_GROUP_PROBE` zrobionym i skasowanym po odczycie, żeby nie ruszać żywej
konfiguracji. Grupowania nie trzeba więc klikać w UI.

⚠️ **Zmiana typu domeny NIE jest sesyjna** — stosuje się od razu, nie zostawia sesji do
opublikowania ani odrzucenia. Inaczej niż tabela decyzyjna albo element domeny.

⚠️ **Opis typu ma limit 512 znaków** (`Value too long for column DESCRIPTION`) — trafiony dwa razy.

Zapisane jako standard *Group Domain Attributes by NAME, Not by Number* w
`standards/higson/rules-authoring.md`, w `INDEX.md` i w pamięci projektu.

### Wciąż anonimowe

Typy `COVER` (4 grupy), `NEEDS_ANALYSIS` (4) i `VARIANT` (2) mają grupy **bez nazw**. Nadanie im
nazw to ta sama poprawa czytelności — ⚠️ ale `COVER` ma atrybuty z **celowo pustą** wartością
domyślną (§32: nowe pokrycie ma nie startować z cudzą taryfą), więc przepisanie typu musi tę pustkę
zachować. Nie ruszane bez decyzji.

## Grupy pytań jako poziom drzewa (2026-09-09)

Właściciel poprosił o to dwa razy; pierwszy raz odczytałem to jako „opakować pojedyncze pytania
w grupy" i odradziłem. Właściwe czytanie: **grupa ma być poziomem w DRZEWIE**, a spis na ekranie
ma pokazywać grupy — czyli to samo, co niosła kolumna `partOf`, tylko widoczne po rozwinięciu
gałęzi. To jest ten sam argument, którym D-18/D-22 przeniosły skład pakietu do drzewa.

### Kształt

```
APK[NEEDS]/GROUPS
├── PROPERTY_NEED  „Potrzeba ubezpieczenia"   └── QUESTIONS[NEEDS_PROPERTY]
├── THEFT          „Ochrona przed kradzieżą"  └── QUESTIONS[NEED_THEFT]
├── FLOOD          „Ochrona przed powodzią"   └── QUESTIONS[NEED_FLOOD]
├── PORTABLE       „Sprzęt przenośny"         └── QUESTIONS[NEED_PORTABLE]
├── RENTAL         „Wynajem"                  └── QUESTIONS[RENTED_OUT]
└── EXPECTATIONS   „Oczekiwania"              └── QUESTIONS[BUDGET, PROPERTY_VALUE]
```

- **Nazwa grupy jest etykietą spisu**, nazwa pytania jego treścią — to samo rozróżnienie co
  `shortLabel`/`text`, ale wyrażone strukturą. `shortLabel` przestaje być używana dla ankiety
  (zostaje dla pól przedmiotu, które elementami drzewa nie są).
- **`partOf` przestała być czytana z katalogu** — rodzica podaje `readGroups` z drzewa.
- **Łańcuch `revealedBy` biegnie między GRUPAMI**; pytania w grupie mają go pustego, więc
  odsłaniają się razem ze swoją grupą.
- **Nagłówek grupy tylko przy kilku pytaniach.** Pięć grup z sześciu ma jedno pytanie i
  pokazywałyby swoją nazwę tuż nad pytaniem, które mówi to samo pełnym zdaniem.
- `QUESTION_GROUP` nie ma atrybutów — kolejność i łańcuch stoją w tym samym katalogu co dla pytań.

### Jak przeniesione

Przez eksport XML → edycja → import z `removeUnused: true` (przepis z pamięci projektu), bo
pojedynczymi wywołaniami MCP byłoby kilkadziesiąt kroków. Wynik importu: 13 nowych elementów,
8 starych usuniętych.

### Trzy potknięcia po drodze

⚠️ **`<domainAttributes/>` wywala import.** Eksport nigdy nie zapisuje pustego elementu atrybutów;
element bez atrybutów po prostu tego bloku nie ma. Komunikat to samo „Invalid file.".

⚠️ **`errorId` i `timestamp` się POWTARZAJĄ przy tym samym błędzie** — pierwsze dwie odmowy miały
identyczny identyfikator i znacznik czasu, co wyglądało na odpowiedź z cache i zmyliło mnie na
dwie próby. To jest deterministyczny identyfikator błędu, nie ślad konkretnego wystąpienia.

⚠️ **Otwarta sesja blokuje import domeny** komunikatem „Import would change domain configuration,
but there are open sessions with colliding domain changes" — trzeba publikować MIĘDZY krokami.
Zmiana typu domeny jest odrzucana z tego samego powodu.

⚠️ Kotwica `s.find('<code>NEEDS</code>')` trafiła w **atrybut typu** o tym samym kodzie, nie
w element. W tym pliku ten sam kod potrafi być i nazwą atrybutu, i kodem elementu — szukać od końca
albo po kontekście.

### Zweryfikowane

Sześć kroków w kolejności, łańcuch odsłania po jednym, licznik „Pytanie N z 6", grupa
`Oczekiwania` renderuje nagłówek i dwa pola, bramkowanie kroków dalej działa. 69 testów frontowych
i Java zielone. Snapshot zsynchronizowany — `GROUPS/*/QUESTIONS` w drzewie.

## Pytanie zależne w grupie (2026-09-09)

Wybór pytania **wyszedł z danych, nie z gustu**. Przegląd `demo.property.apk.answer` pokazał, że
każde pokrycie ma zadeklarowaną potrzebę (waga 3) poza dwoma: `FIXTURES` ma tylko pochodne
(waga 1–2), a **`ASSISTANCE` nie ma żadnej**. „Pomoc w domu" siedziała wyłącznie w pakiecie Pełnym
i klient nie mógł o nią poprosić — wpadała mu do koszyka razem z najdroższym pakietem, nigdy jako
jego wybór.

**Zbudowane:** w grupie „Potrzeba ubezpieczenia", zależne od „Tak":

> *Czy chcesz mieć pomoc fachowca przy awarii w domu — hydraulika, elektryka, ślusarza?*

Trzy artefakty: element `GROUPS[PROPERTY_NEED]/QUESTIONS[HOME_ASSISTANCE]`
(`KIND=BOOLEAN`, `CONTEXT_PATH=quote.needs.assistance`), wiersz katalogu (pozycja 202,
`revealedBy = NEEDS_PROPERTY`, nieobowiązkowe) i wiersz mapowania
(`HOME_ASSISTANCE / true → ASSISTANCE`, waga 3). **Java bez zmian** — ścieżkę kontekstu podaje
atrybut pytania, dokładnie po to (D-4).

⛔ **To jest potrzeba, nie cecha przedmiotu.** Odrzucone zostały warianty w rodzaju „czy w okolicy
zdarzały się podtopienia", „czy trzymasz w domu gotówkę" — to charakterystyka przedmiotu
i sytuacji, czyli krok 2.

### Co trzeba było dołożyć poza konfiguracją

Grupa renderowała **wszystkie** swoje pytania. Teraz renderuje tylko **odsłonięte**, a domknięcie
kroku liczy się po tej samej liście (`shownFields` w `core/question-state`, ta sama funkcja po obu
stronach filtra). Serwerowe `allRequiredAnswered` pomija pola nieodsłonięte z tego samego powodu:
bez tego grupa z pytaniem warunkowym nigdy by się nie domknęła i łańcuch stanąłby na niej na zawsze.

⚠️ Odczyt `isRevealed()` w `allRequiredAnswered` jest bezpieczny **dzięki kolejności**: lista idzie
po `position`, a pola grupy mają pozycje tuż po niej (200 → 201, 202), więc licząc odsłonięcie
następnej grupy mamy już policzone `revealed` pól poprzedniej.

Nagłówek grupy pojawia się teraz przy **więcej niż jednym WIDOCZNYM** pytaniu — czyli razem
z pytaniem warunkowym. Ekran staje się grupą dopiero wtedy, gdy naprawdę nią jest.

### Zmierzony efekt

| Odpowiedzi | assistance | Proponowany pakiet | Potrzeby |
|---|---|---|---|
| tylko nieruchomość, reszta „Nie" | Nie | **STANDARD** | 2/2 |
| tylko nieruchomość, reszta „Nie" | **Tak** | **COMPLETE** | 3/3 |
| wszystko „Tak" | Nie | COMPLETE | 6/6 |
| wszystko „Tak" | **Tak** | COMPLETE | **7/7** |

Te same odpowiedzi wszędzie indziej — jedno pytanie zależne przestawia pakiet ze Standardowego na
Pełny, bo assistance jest tylko w nim.

### Testy

Trzy nowe w `question-state.spec.ts`: grupa domyka się na samym polu wymaganym, zanim warunkowe
się pojawi; zostaje domknięta, gdy warunkowe pojawi się i zostanie pominięte; pole warunkowe nie
trafia na listę renderowanych, dopóki nie jest odsłonięte. **72 testy frontowe** zielone (było 69).

### Mechanizm jest ogólny

Po tej zmianie **każda grupa** może mieć pytania warunkowe — wystarczy wskazać w kolumnie
`revealedBy` inne pytanie tej samej grupy. Bez wdrożenia.

## Przeniesienie pytania zależnego do „Wynajmu" i treść pytania w atrybucie (2026-09-09)

### Dlaczego przeniesienie

Właściciel: *„to pytanie które dodałeś w tym miejscu jest bez sensu"*. I było — w grupie „Potrzeba
ubezpieczenia" pytanie odsłaniało się po „czy potrzebujesz ubezpieczyć nieruchomość", czyli po
odpowiedzi, którą daje **każdy**, kto w ogóle tu przyszedł. Zależność była wtedy pozorna: pytanie
formalnie warunkowe, praktycznie zawsze widoczne.

**Przeniesione do grupy „Wynajem"**, gdzie warunek naprawdę dzieli klientów, i przeredagowane pod
ten kontekst:

> *Czy chcesz, żeby przy awarii u najemcy fachowiec przyjechał bez Twojego udziału — hydraulik,
> elektryk, ślusarz?*

`HOME_ASSISTANCE`: pozycja **242**, `revealedBy = RENTED_OUT`, element pod
`GROUPS[RENTAL]/QUESTIONS`.

⚠️ **`higson_delete_domain_element` zapisało się w sesji jako `UPDATE`, nie `DELETE`.** Eksport
konfiguracji wciąż niósł skasowany element, import go odtworzył i pytanie stanęło w **dwóch**
grupach naraz. Kasowanie powtórzone, drzewo sprawdzone przez `childrenCount` — nie przez samo
„status: SUCCESS".

### Treść pytania w atrybucie domeny

Na życzenie właściciela: *„chcę aby treść pytania była w atrybucie domeny"*. Typ `QUESTION`
dostał atrybut `TEXT` w grupie **Tresc**; `KIND` i `CONTEXT_PATH` przeniesione do grupy
**Zachowanie**. Nazwy elementów skrócone do etykiet gałęzi („Nieruchomość", „Kradzież", „Powódź",
„Sprzęt przenośny", „Wynajem", „Oczekiwana składka", „Wartość nieruchomości").

⛔ **To jest częściowe odwrócenie D-20 i wolno je zrobić tylko pod jednym warunkiem.** D-20 usunął
atrybut `TEXT` właśnie dlatego, że treść stała w dwóch miejscach naraz — w nazwie elementu
i w atrybucie — i cztery z dziewiętnastu pytań zdążyły się niewidocznie rozjechać. Warunek brzmi:
**nazwa elementu przestaje być tą samą treścią**. Dlatego Java czyta `TEXT` **bez odwrotu do
nazwy elementu**: brak atrybutu daje puste pytanie, które widać od razu, a nie cichy powrót do
drugiego źródła prawdy.

Grupa pytań (`QUESTION_GROUP`) atrybutów nie ma, więc jej `text` bierze się z nazwy elementu
(`readGroups`) — nazwa grupy jest etykietą gałęzi, nie treścią pytania, i nie ma drugiego miejsca,
w którym mogłaby stać.

## Pytanie zależne od ODPOWIEDZI, nie od samego domknięcia (2026-09-09)

### Co było nie tak

Weryfikacja na żywo po przeniesieniu: `RENTED_OUT = Nie` **też** odsłaniało pytanie o assistance.

```
--- po RENTED_OUT=true       --- po RENTED_OUT=false
RENTED_OUT      val=true     RENTED_OUT      val=false
HOME_ASSISTANCE ods=True     HOME_ASSISTANCE ods=True     ← źle
```

Łańcuch `revealedBy` z §18 odsłania następne pytanie po **domknięciu** poprzedniego — świadomie,
bo odmowa i pominięcie pytania nieobowiązkowego też domykają. Ale „domknięte" nie zna wartości
odpowiedzi, więc *pytanie zależne od TAK* nie było w tej konfiguracji wyrażalne wcale. Zamówienie
brzmiało „jeśli ktoś odpowie **tak**, wyświetla się dodatkowe" — i to nie działało.

### Jak to zostało zrobione

Jeden nowy fakt, po jednej kolumnie z każdej strony:

| Artefakt | Zmiana |
|---|---|
| `demo.property.apk.question` | nowa kolumna **`revealedWhen`** — „odsłania je odpowiedź (pusta = każda)", tuż obok `revealedBy` |
| `demo.property.apk.question.reveal` | nowy poziom wejściowy **`matches`** (`reveal.sourceMatches`) i szósty wiersz |
| Java | `Question.revealedWhen`, `Property.sourceMatches(...)`, piąty argument `PropertyContext.withReveal` |

Podział ról zostaje ten sam, co przy `revealedBy`: **Java rozwiązuje wskazanie i porównuje napisy,
wniosek wyciąga tabela.** Warunek jest konfiguracją, nie kodem — dołożenie drugiego pytania
zależnego to jedna komórka w Studio.

Matryca po zmianie (`hasSource; answered; refused; required; matches → revealed`):

```
false ; *     ; *     ; *     ; *     ; true      brak odsłaniającego — widoczne od razu
true  ; *     ; *     ; *     ; false ; false     odpowiedź inna niż warunek — ukryte
true  ; true  ; *     ; *     ; true  ; true      odpowiedziane i zgodne
true  ; false ; true  ; *     ; true  ; true      odmowa rozstrzyga
true  ; false ; false ; false ; true  ; true      nieobowiązkowe przepuszcza łańcuch
true  ; false ; false ; true  ; true  ; false     obowiązkowe bez odpowiedzi — czekamy
```

Wiersze są **rozłączne**, nie tylko zgodne: warunek `matches` rozstrzyga w wierszu drugim, a piąty
i szósty mają go ustawione na `true`, więc nie ma pary wierszy, która dopasowuje się do tego samego
wejścia.

⚠️ **Pusty `revealedWhen` znaczy „każda odpowiedź"** i tak ma sześć pytań z siedmiu. Inaczej
dołożenie kolumny zmieniłoby zachowanie całego łańcucha zamiast jednego pytania.

⚠️ **Warunek postawiony na pytaniu ZŁOŻONYM zostawia pytanie ukryte.** Grupa nie ma własnej
odpowiedzi, więc warunek nie ma się o co oprzeć; ukrycie widać na ekranie natychmiast, ciche
przepuszczenie znaczyłoby, że konfiguracja mówi coś, czego nikt nie egzekwuje.

⚠️ **W-27 znowu.** Dołożenie kolumny do katalogu **przenumerowało wszystkie 28 wierszy** — id
`345120…` stały się `345177…`. Matryca odczytana ponownie po zmianie struktury, przed
zapisem wiersza `HOME_ASSISTANCE`.

### Testy

Frontend **72/72**, Java **11/11**. Front nie wymagał zmiany: czyta gotową flagę `revealed`
i o warunku nie wie nic — co jest tym samym podziałem, co przy całej reszcie ankiety.

### Usterka, którą dopiero ta weryfikacja mogła pokazać

Trzeci przypadek testu na żywo: wynajem „Tak" → assistance „Tak" → wynajem cofnięty na „Nie".
Pytanie znikało z ekranu, ale **jego odpowiedź dalej liczyła się do rekomendacji** — pakiet stał na
Pełnym z powodu potrzeby wynikającej z pytania, którego klient już nie widzi. Na demo wyglądałoby to
jak błąd silnika.

Odpowiedzi **nie kasujemy** przy ukryciu (prowadzący wraca i nie chce wpisywać jej drugi raz) —
przestaje natomiast wchodzić do reguł. Filtr po `isRevealed()` w dwóch miejscach po stronie
majątkowej:

| Miejsce | Co odcina |
|---|---|
| `PropertyContext.collectAnswers` | odpowiedź nie ląduje pod swoją ścieżką kontekstu |
| `Property.revealed(quote)` przy `ApkAnalysis.serialize` | odpowiedź nie wchodzi na listę, po której chodzi reguła potrzeb |

⛔ **Filtr NIE mógł trafić do `ApkAnalysis.flatten`**, choć to jeden lej dla wszystkich odpowiedzi:
`revealed` ustawia **wyłącznie** profil majątkowy, więc w moto i życiu każde pytanie ma tam `false`
i wspólny filtr wyciąłby im wszystkie odpowiedzi. Warstwa wspólna zostaje nietknięta.

`applyBlocker` miał ten filtr od początku — teraz mają go wszystkie trzy ścieżki czytające
odpowiedzi.

### Weryfikacja końcowa na żywo

| Krok | assistance | Pakiet |
|---|---|---|
| wynajem Tak, assistance Nie | odsłonięte, `false` | **STANDARD** 3/3 |
| wynajem Tak, assistance Tak | odsłonięte, `true` | **COMPLETE** 4/4 |
| wynajem cofnięty na Nie | **ukryte**, `true` zachowane | **STANDARD** 2/2 |
| wynajem znów Tak | odsłonięte, `true` wraca | **COMPLETE** 4/4 |

Snapshot zsynchronizowany z eksportu (`@domains/PROPERTY`, `@params/@profiles/PROPERTY`) — niesie
`revealedWhen`, szósty wiersz `reveal.mat`, gałąź `GROUPS` i usunięte `apk/regime` oraz `offer/terms`.
Testy po naprawie: **frontend 72/72**, **Java 11/11**.


## Grupy atrybutów na COVER i VARIANT (2026-09-09)

Przegląd całej domeny PROPERTY pod kątem grupowania: `NEEDS_ANALYSIS`, `PRODUCT` i `QUESTION` miały
nazwane grupy, `COVER` (4) i `VARIANT` (2) miały **sam `groupOrder` bez nazw** — czyli dokładnie to,
czego zakazuje standard: bezimienne bloki rozdzielone odstępem. `QUESTION_GROUP` i `ROOT` atrybutów
nie mają.

| Typ | Przed | Po |
|---|---|---|
| `COVER` | 4 grupy bez nazw | **Dostepnosc · Suma ubezpieczenia · Skladka · Limit odpowiedzialnosci · Prezentacja** |
| `VARIANT` | 2 grupy bez nazw | **Dostepnosc · Suma gwarancyjna · Skladka · Prezentacja** |

Przy okazji przełożone: w każdej grupie pierwszy atrybut to WYNIK, kolejne to jego skład
(`SUM` przed `SUM_BASIS`/`SUM_REFERENCE`, `PREMIUM` przed `RATE`/`RATE_BASIS`). Wcześniej
w grupie 1 `COVER` stały obok siebie dostępność, suma i składka.

**Trzy pułapki obeszły się bez szkody, bo były znane:**

- ⛔ `action: NO_CHANGES` na każdym atrybucie (W-29). Domyślne `SET_TO_ALL` nadpisałoby wartość na
  każdym elemencie — siedem pokryć straciłoby wskazania na swoje tabele.
- ⚠️ Wartości bez prefiksu `$p`/`$f`/`$w` (W-25) — silnik trzyma je bez niego.
- ⚠️ Puste wartości domyślne `COVER` odesłane jako puste (§32).

Sprawdzone po zapisie na `COVERS[THEFT]` i `VARIANTS[STANDARD]`: wszystkie wartości elementów
nietknięte, kolekcja `COVERS` pakietu dalej z czterema dziećmi. Snapshot zsynchronizowany —
**41 z 41** atrybutów w PROPERTY ma nazwę grupy.

Wycena po zmianie typów sprawdzona na żywo: BASIC 545,29 / STANDARD 1014,94 / COMPLETE 3082,75,
siedem pokryć — czyli wskazania na tabele przetrwały co do jednego.

### Czego NIE ruszyłem — decyzja właściciela

| Profil | Atrybutów | Z nazwą grupy |
|---|---|---|
| PROPERTY | 41 | **41** |
| DEMO (moto) | 24 | 7 — i to nazwy `main` / `configuration`, czyli kody, nie etykiety |
| LIFE | 32 | **0** |

Zapytany, właściciel zdecydował: **tylko PROPERTY**. Moto i życie zostają jak są; w moto nie byłoby
to zresztą dokładanie nazw, tylko przemianowanie działających `main`/`configuration`.
