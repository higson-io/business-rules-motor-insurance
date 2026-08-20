# Demo ubezpieczeń majątkowych — brief do wykonania

**Data zlecenia:** 2026-08-20
**Wzorzec:** demo LIFE (`.maister/tasks/development/2026-08-13-higson-life-implementation/`)
**Status:** research rozpoczęty, motyw przewodni rozstrzygnięty (U-3), reszta decyzji produktowych otwarta
**Gałąź:** `feature/property-demo`

---

## 1. Co jest zamówione

Nowe, trzecie demo w tej aplikacji: **ubezpieczenie domu i mieszkania (detal)**. Zakres pracy
obejmuje cztery rzeczy i żadna nie jest opcjonalna:

1. **Bardzo dokładna analiza rynku** — przegląd OWU i ofert ze stron polskich ubezpieczycieli.
   Nie streszczenia z porównywarek jako jedyne źródło: OWU są publicznymi PDF-ami, mają być czytane.
2. **Analiza nadchodzących zmian w APK** (analiza potrzeb klienta) i wpływu tych zmian na to,
   jak demo ma prowadzić klienta przez proces.
3. **Plan frontu** osadzony w obecnym demo — nie makieta w próżni, tylko konkretne komponenty
   w `frontend/src/app/demos/`, wpięte w istniejący katalog demo.
4. **Projekt konfiguracji Higson** (domena, konteksty, tabele, funkcje, flow, region)
   **oraz integracji backendowej** (pakiet Java jak `demo.life`).

### Rozstrzygnięcia użytkownika

| # | Pytanie | Odpowiedź |
|---|---|---|
| U-1 | Który produkt majątkowy | **Dom i mieszkanie (detal)** — mury, elementy stałe, ruchomości, OC w życiu prywatnym, assistance |
| U-2 | Tryb pracy | **Jak przy LIFE**: dokument projektowy po polsku → plan implementacji → konfiguracja w Studio → backend i ekrany |
| U-3 | Co demo ma pokazywać w Higsonie, czego MOTO i LIFE nie pokazują | **APK skonfigurowane w Higsonie i wersjonowane datami** — patrz §5. Demo ma pokazywać, że na zmianę regulacji klient reaguje konfiguracją, nie wdrożeniem |

---

## 2. Co już wiadomo z researchu (zrobione 2026-08-20)

### 2.1 APK — stan prawny jest świeży i to jest oś tego demo

**Uchwała nr 190/2026 KNF z 26 czerwca 2026 r.** wydała *Rekomendacje dla zakładów ubezpieczeń
dotyczące dystrybucji ubezpieczeń*, zastępując Wytyczne z 24 czerwca 2014 r.
Źródło: `https://dziennikurzedowy.knf.gov.pl/DU_KNF/2026/10/akt.pdf`
(kopia tekstu: `scratchpad/knf-rekomendacje.txt`, jeśli sesja jeszcze żyje — inaczej pobrać ponownie).

- **Termin dostosowania: 1 lipca 2027 r.** Wyjątek: rekomendacja 7.9 w zakresie części
  oszczędnościowej — 1 lipca 2028 r. (dla majątkowych bez znaczenia).
- Do tego czasu obowiązują stare Wytyczne. **To znaczy, że demo pokazuje produkt zgodny
  z regulacją, która jeszcze nie weszła — i to jest atut sprzedażowy, nie problem.**

**Rekomendacja 8 — przebieg, forma i udokumentowanie APK.** Punkty istotne dla demo:

| Punkt | Treść (skrót) | Co z tego wynika dla demo |
|---|---|---|
| 8.1 | Projektując APK bierze się pod uwagę rodzaj produktu, zakres informacji niezbędnych do określenia wymagań i potrzeb, **kluczowe wyłączenia i ograniczenia** oraz kanał dystrybucji | APK jest **konfiguracją produktu**, nie formularzem w kodzie — czyli należy do Higsona |
| 8.2.b | Ustalenie, **na wypadek jakich okoliczności** i **w jakim okresie** klient chce ochrony oraz **jaka suma ubezpieczenia** odpowiada jego potrzebom | Reguła wyliczająca rekomendowaną SU z odpowiedzi klienta |
| 8.2.c | Ustalenie cech i sytuacji klienta, **żeby sprawdzić, czy wyłączenia nie stoją w sprzeczności z jego potrzebami** | Tabela decyzyjna „sytuacja klienta → wyłączenie, które go dotyka" — ostrzeżenie na ekranie |
| 8.2.d | Propozycja odpowiadająca potrzebom **przy uwzględnieniu akceptowalnej dla klienta składki** | Budżet klienta jako wejście do rekomendacji wariantu |
| 8.3 | Wolno użyć danych z przeszłości lub z innych źródeł, ale trzeba o tym **poinformować** i umożliwić weryfikację (zakres i źródło na żądanie) | Ekran musi oznaczać pola prefillowane i mówić skąd są |
| 8.4 | Przy odmowie podania informacji — **poinformować o konsekwencjach** | Ścieżka „nie chcę podać" z komunikatem z reguł |
| 8.7 | Dokumentacja APK ma utrwalać informacje od klienta **i wykazywać, że propozycja jest zgodna z wymaganiami i potrzebami** | Wynik APK to dokument z uzasadnieniem, nie tylko cena |
| 8.11 | Przy odnowieniu — poinformować, że opiera się na danych posiadanych, i dać możliwość aktualizacji | Opcjonalny scenariusz „odnowienie" |

**Rekomendacja 9 — zgodność propozycji z wymaganiami i potrzebami.**

- 9.1 — propozycja z produktów, **dla których klient należy do rynku docelowego**; poza rynkiem
  docelowym wolno zaproponować tylko, jeśli z APK wynika zgodność z potrzebami.
- 9.3 — **gdy kilka wariantów odpowiada potrzebom, trzeba wyraźnie wskazać różnice między nimi.**
  To jest dokładnie macierz porównawcza, którą ekran grupowy LIFE już umie rysować.

**Rekomendacja 7 — wartość produktu dla klienta.** 7.2 stawia twardy próg: produkt nie spełnia
zasady, gdy oczekiwane odszkodowania stanowią **mniej niż 30%** oczekiwanych składek brutto
(20% dla rat ≤ 2,5 EUR/mies. albo 30 EUR/rok). Do rozważenia jako *opcjonalny* ekran „wskaźnik
wartości produktu" — pokazuje, że silnik reguł liczy też metryki nadzorcze, nie tylko cenę.

### 2.2 Rynek — stan researchu

Zebrane wstępnie (do pogłębienia, patrz §4):

- Struktura polisy w PL jest stała: **mury** (konstrukcja) / **elementy stałe** (to, co zamontowane
  na trwałe: podłogi, glazura, armatura, zabudowa, drzwi, okna, rolety) / **ruchomości domowe** /
  **OC w życiu prywatnym**, plus assistance i dodatki (kradzież, przepięcia, powódź, dewastacja).
- Sumy ubezpieczenia ustala się **osobno dla każdej z tych kategorii**.
- ⚠️ Granica mury/elementy stałe **różni się między towarzystwami** (okna i drzwi bywają po obu
  stronach) — to jest realny fakt rynkowy, który warto oddać w konfiguracji jako parametr, a nie
  zaszywać w kodzie.

Źródła przejrzane pobieżnie: rankomat.pl, polisoteka.pl, meritumubezpieczenia.eu.
**Nie przejrzano jeszcze żadnego OWU w oryginale — to jest pierwsze zadanie.**

---

## 3. Kontekst repozytorium (obowiązkowy do przeczytania przed pisaniem czegokolwiek)

- `CLAUDE.md` i `.maister/docs/INDEX.md` — standardy, których trzeba trzymać.
- `.maister/docs/project/higson-life-profile.md` — **wzorzec dokumentu projektowego**: decyzje
  D-1…D-n, kwestie otwarte W-n, budżet złożoności, wersjonowanie regionem.
- `.maister/tasks/development/2026-08-13-higson-life-implementation/implementation-plan.md` —
  **wzorzec planu**, a w §9 i §10 komplet pułapek API Higsona (W-15…W-27). Czytać zanim się
  cokolwiek zacznie klikać w Studio, bo tam jest opisane m.in. że `EXPRESSION` we flow nie działa,
  że `open_decision_table` jest obowiązkowe przed edycją macierzy i jak wejść do nieaktywnej
  wersji regionu.
- Kod LIFE jako wzorzec integracji: `src/main/java/pl/decerto/higson/demo/life/`,
  `frontend/src/app/{core/life-store.ts,services/life-service.ts,models/life.ts,demos/life/}`.
- Nowe demo wymaga wpisu w `frontend/src/app/demos/demo-catalog.ts` (`DemoCode`, `DEMOS`)
  i gałęzi w `@switch` w `frontend/src/app/app.html`.
- Snapshot `src/main/higson/` **nie jest w gicie** — konfiguracja żyje w Studio.

### Jak uruchomić (bo Readme kłamie)

```bash
colima start                                            # docker
PATH=/opt/homebrew/opt/node@22/bin:$PATH mvn -o package -Pall_in_one -DskipTests
HIGSON_DATABASE_URL='jdbc:h2:tcp://localhost:9192/higson.demo;NON_KEYWORDS=VALUE' \
HIGSON_DATABASE_USERNAME=sa HIGSON_DATABASE_PASSWORD=sa \
java -jar target/motor-insurance.war                    # http://localhost:48080/demo
```

Studio: `http://localhost:38088/studio` (MCP `higson`). Node musi być z `node@22` — domyślny v23
odbija się od `engines`. `-Dexec.skip=true` pomija przebudowę frontu przy zmianach w Javie.

---

## 4. Zakres do wykonania

### G0 — Research (najpierw, bo wszystko inne z niego wynika)

1. **OWU w oryginale**, minimum pięć towarzystw, pobrane i przeczytane, nie streszczone
   z porównywarki: PZU Dom, Warta Dom Komfort, Allianz Mój Dom, Generali/Proama, Link4 Dom,
   ERGO Hestia, Compensa, InterRisk, Uniqa, Beesafe — wybrać 5–7 realnie dostępnych.
   Wynik: tabela porównawcza **zakresów, definicji, sum, franszyz, wyłączeń i limitów**
   (limity na gotówkę, rowery, sprzęt elektroniczny, przedmioty wartościowe — tam są różnice).
2. **Oferty i kalkulatory** ze stron TU: jakie pytania zadają, w jakiej kolejności, co jest
   wymagane, a co domyślne. To jest wzorzec dla ekranu.
3. **Czynniki taryfowe**: co realnie wpływa na składkę (lokalizacja, rodzaj i wiek budynku,
   konstrukcja/palność, metraż, sposób użytkowania — stałe/sezonowe/wynajem, zabezpieczenia,
   historia szkód, zakres, suma, franszyza, okres).
4. **Powódź i zalanie** — jak rynek dzieli te ryzyka i jak podchodzi do stref zagrożenia
   (to najciekawszy wymiar geograficzny, którego MOTO i LIFE nie mają).
5. **APK w praktyce**: jak dziś wyglądają formularze APK w majątkowych i czego zabraknie
   względem rekomendacji 8 i 9.

Wynik G0: `analysis/market-research.md` z **przypisami do źródeł** (URL + data dostępu).
Wszystkie liczby użyte potem w taryfie mają być oznaczone jako demonstracyjne, jeśli nie pochodzą
wprost ze źródła — tak jak W-3 w dokumencie LIFE.

### G1 — Dokument projektowy

`.maister/docs/project/higson-property-profile.md`, po polsku, w konwencji dokumentu LIFE:
drzewo domeny, typy domenowe, konteksty wejściowe, tabele decyzyjne z macierzami, flow, funkcje,
wersjonowanie, testy regułowe, decyzje **D-n** z uzasadnieniem, kwestie otwarte **W-n**,
budżet złożoności (funkcja ≤80 linii, tabela ≤30 wierszy, flow ≤15 nodów).

Musi rozstrzygnąć m.in.: podział mury/elementy stałe, sposób wyznaczania SU, franszyza jako oś,
strefy ryzyka, rabaty i ich składanie, co jest w wariancie a co dokupowane, jak APK mapuje się
na rekomendację wariantu.

### G2 — Plan implementacji

`.maister/tasks/development/2026-08-20-property-demo/implementation-plan.md` — grupy zadań
z zależnościami, wzorem planu LIFE, wraz z listą pułapek przeniesionych z W-15…W-27.

### G3 — Plan frontu

Osobno albo jako rozdział planu: jakie komponenty, w którym miejscu drzewa, co idzie do
`demo-catalog.ts`, jak wygląda przepływ ekranów (APK → kalkulacja → porównanie wariantów →
podsumowanie), co jest sygnałem a co stanem serwera, gdzie siedzi store.
**Wzorzec:** `LifeStore` + `resource()` + `reload()`, podsumowanie w przyklejonej kolumnie.

### G4 — Konfiguracja w Studio i integracja backendowa

Wchodzi w zakres. Kolejność i pułapki — jak w planie LIFE (G1 konteksty → G2 tabele → G3 funkcje →
G4 flow → G5 typy domenowe → G6 drzewo → G7 region i wersje → G8 testy → G9 publikacja).
Backend: pakiet `pl.decerto.higson.demo.property` w konwencji `demo.life`. Front: nowy wpis
w `demo-catalog.ts`, komponenty w `frontend/src/app/demos/property/`, store wzorowany na
`LifeStore` (`resource()` + `reload()`, nigdy `params`).

**Warunek:** G4 zaczyna się dopiero, gdy G1–G3 są napisane i wewnętrznie spójne. Lepiej oddać
kompletny projekt bez kodu niż kod bez projektu.

---

## 5. Motyw przewodni — APK jako konfiguracja, wersjonowana datą

**To jest rozstrzygnięte (U-3) i jest osią całego demo.** Nie jest to dodatek do kalkulatora
składki — to jest powód, dla którego to demo istnieje.

### Teza demonstracyjna

Regulator zmienia zasady analizy potrzeb klienta. Zakład ubezpieczeń, który ma APK zaszyte
w aplikacji, potrzebuje wdrożenia: analiza, kod, testy, release. Zakład, który ma APK w Higsonie,
potrzebuje **edycji tabeli i daty obowiązywania**. Demo ma to pokazać na jednym ekranie,
w kilkanaście sekund.

### Co konkretnie musi być konfiguracją, nie kodem

| Element APK | Nośnik w Higsonie | Dlaczego nie kod |
|---|---|---|
| **Zestaw pytań** — treść, kolejność, typ odpowiedzi, obowiązkowość | Domena (`QUESTION`) + tabela z atrybutami | Zmiana regulacji to dodanie pytania, a nie nowy formularz |
| **Widoczność pytania** — pokaż tylko, gdy wcześniejsza odpowiedź tego wymaga | Tabela decyzyjna `apk.question.visibility` | Warunki są produktowe i zmienne, w kodzie zamieniają się w drabinę `if` |
| **Mapowanie odpowiedź → potrzeba** (rek. 8.2.b) | Tabela `apk.need.derive` | To jest wiedza produktowa aktuariusza, nie logika UI |
| **Rekomendowana suma ubezpieczenia** (rek. 8.2.b) | Funkcja + tabela stawek zł/m² | Zmienia się z cenami budowlanymi, czyli częściej niż release |
| **Wyłączenie kontra sytuacja klienta** (rek. 8.2.c) | Tabela `apk.exclusion.conflict` | Wyłączenia zmieniają się z OWU, OWU zmienia się co sezon |
| **Dobór wariantu do potrzeb i budżetu** (rek. 8.2.d, 9.1) | Flow `apk.recommend` | Serce rekomendacji — musi być audytowalne i wersjonowane |
| **Uzasadnienie propozycji** (rek. 8.7) | Tabela komunikatów + funkcja składająca | Dokument APK ma dowodzić zgodności, więc jego treść jest regułą |
| **Różnice między wariantami** (rek. 9.3) | Ta sama macierz porównawcza co w LIFE grupowym | Gotowy wzorzec, sprawdzony |

### Wersjonowanie datą — scenariusz pokazowy

Region `PROPERTY` z **dwoma timeline'ami wokół realnej daty regulacyjnej**:

| Wersja | Obowiązuje od | Co zawiera |
|---|---|---|
| **1 — Wytyczne 2014** | `2026-01-01` | APK w dzisiejszym kształcie: krótka ankieta, brak pytania o budżet, brak konfrontacji wyłączeń z sytuacją klienta, propozycja bez pisemnego uzasadnienia |
| **2 — Rekomendacje 190/2026** | `2027-07-01` | APK wg rekomendacji 8 i 9: pytania o okoliczności i okres ochrony, wyliczana SU, pytanie o akceptowalną składkę, ostrzeżenia o wyłączeniach kolidujących z sytuacją, uzasadnienie propozycji, jawne wskazanie różnic między wariantami |

**Data `2027-07-01` nie jest wymyślona** — to termin dostosowania z §2 uchwały 190/2026.
Widz przesuwa datę polisy przez ten próg i **ankieta zmienia się w locie**: przybywa pytań,
pojawia się uzasadnienie, pojawiają się ostrzeżenia. Nic nie jest przebudowywane, nic nie jest
wdrażane.

⚠️ **Pułapka wzięta z LIFE (W-27):** katalog pytań musi być czytany **przy każdym wywołaniu, po
`setEffectiveDate`**, a nie raz przy tworzeniu sesji. Inaczej wersja 2 nigdy się nie pokaże.
⚠️ Wersjonowanie obejmuje **dwa wymiary naraz** (wzorzec D-4 z LIFE): parametry odpowiadają na
„ile wynosi", a elementy domeny przypięte do regionu na „co w ogóle istnieje". Nowe pytanie APK
to **element domeny w wersji 2**, a jego wiersze w tabelach wartości zostają globalne.

### Trzy osie techniczne pod spodem

1. **Rekomendacja zamiast cennika** — z odpowiedzi wychodzi wariant *z uzasadnieniem*,
   a nie lista cen do wyboru.
2. **Suma ubezpieczenia liczona, nie wybierana** — SU murów z metrażu, standardu i stawki zł/m²,
   z ostrzeżeniem o niedoubezpieczeniu. LIFE ma katalog gotowych sum, więc to jest nowa jakość.
3. **Reguła, która odradza** — „to ubezpieczenie **nie** obejmie X w twojej sytuacji", zanim
   klient zapłaci.

Do tego **strefa ryzyka po lokalizacji** (powódź, kradzież) jako pierwszy wymiar geograficzny
w tej aplikacji.

## 6. Zasady pracy bez nadzoru

- Pracować na gałęzi, commitować **małymi krokami z prawdziwymi opisami** — użytkownik ma wrócić
  do przeglądalnej historii, nie do jednego commita „wip".
- **Nie pushować.**
- ⛔ **Nie dotykać reguł MOTO ani DEMO.** `assign_to_profile` wyłącznie z kodami
  `demo.property.*` i **bez** `currentProfileCode`. Żadnych `delete_*` poza własnym prefiksem.
- W Studio publikować dopiero po sprawdzeniu sesji roboczej: każdy element musi mieć własny
  prefiks. Przed publikacją zrobić backup bazy (`backup to '/h2-data/...zip'` + `docker cp`).
- Nie zostawiać martwego kodu ani plików tymczasowych w `frontend/src` i `src/main`.
- Testy: `ng test --watch=false` w `frontend/` oraz `mvn -o test`. Obie suity mają być zielone
  na koniec.
- Czego **nie** robić bez pytania: zmian w demo MOTO, zmian w `pom.xml`, kasowania istniejących
  plików spoza własnego zakresu, publikowania czegokolwiek na zewnątrz.

## 7. Definicja ukończenia

Minimum, z którym można wrócić do użytkownika:

1. `analysis/market-research.md` — research z źródłami, w tym porównanie OWU i sekcja o APK.
2. `higson-property-profile.md` — kompletny projekt konfiguracji z decyzjami i kwestiami otwartymi.
3. `implementation-plan.md` — grupy zadań, zależności, pułapki, szacunek objętości.
4. Rozdział o froncie z konkretnymi komponentami i przepływem ekranów.
5. Lista pytań do użytkownika, jeśli research wywrócił którekolwiek z założeń §5.

6. Konfiguracja w Studio i kod (G4) — **o ile G1–G3 są skończone**. Inaczej lepiej zostawić
   czysty projekt niż półdziałający kod.

Demo uznaje się za działające dopiero wtedy, gdy przesunięcie daty polisy przez `2027-07-01`
widocznie zmienia ankietę APK i treść rekomendacji. To jest jedyny test, który naprawdę
weryfikuje tezę tego demo.
