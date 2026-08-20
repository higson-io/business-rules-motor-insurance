# Ubezpieczenia domu i mieszkania w Polsce — research rynku i APK

**Data researchu:** 2026-08-20 (wszystkie daty dostępu do źródeł = ta data)
**Cel:** podstawa faktograficzna dla demo majątkowego (`brief.md` §4 G0). Wszystko, co trafi
potem do taryfy demonstracyjnej, ma być albo **zacytowane stąd**, albo **jawnie oznaczone jako
wymyślone**.
**Zasada:** czego nie dało się pobrać albo ustalić — jest zapisane jako **brak**, a nie zgadnięte.

---

## 1. Metoda

Pobrano i przeczytano **osiem dokumentów w oryginale** (PDF ze stron towarzystw albo z ich CDN-ów),
a nie streszczenia z porównywarek. Tekst wyciągnięto `pdftotext`, w dwóch trybach (kolumnowym
i `-layout`), bo OWU majątkowe są składane w dwóch–trzech szpaltach i sam `-layout` gubi kolejność
czytania w tabelach.

Czego **nie** udało się zrobić i dlaczego — §13.

### 1.1 Źródła podstawowe (OWU)

| # | Towarzystwo | Dokument | Identyfikacja wydania | URL |
|---|---|---|---|---|
| **[S1]** | PZU SA | OWU **PZU Dom** | uchwała Zarządu PZU SA **nr UZ/221/2025 z 2 września 2025 r.** | `https://www.pzu.pl/_fileserver/item/1564092` |
| **[S2]** | TUiR WARTA SA | OWU **WARTA Dom Komfort** | kod **C7187**, „obowiązują od dnia **17 maja 2026 roku**" | `https://www.warta.pl/documents/oferta_dla_ciebie/OWU_Warta_Dom_Komfort_C7187_int.pdf` |
| **[S3]** | STU ERGO Hestia SA | OWU pozakomunikacyjnych **ERGO 7** | kod **C-E7-PK-01/26**; errata uchwałą Zarządu **nr WS 03/2026 z 27 stycznia 2026 r.**, obowiązująca od **13.02.2026** | `https://cdn.bsbox.pl/files/hestia/OGI7MDA_/a881bebfd4434803df522ca1d2ffb26b_documents.pdf` |
| **[S4]** | TUiR Allianz Polska SA | **Mój Dom** — Przewodnik + OWU | Przewodnik zatwierdzony uchwałą Zarządu **nr 81/2025** | `https://www.allianz.pl/content/dam/onemarketing/cee/azpl/dokumenty/dom/Moj-Dom-25-10-25-OWU-przewodnik.pdf` |
| **[S5]** | Generali TU SA | OWU domów, mieszkań oraz OC w życiu prywatnym **„Generali, z myślą o domu i rodzinie"** | „Obowiązujące od **25 maja 2023 r.**" | `https://www.generali.pl/media/230508_OWU_Z_mysla_o_domu_i_rodzinie_473b1b951e.pdf` |
| **[S6]** | InterRisk TU SA VIG | OWU **Dom Max** | „obowiązujące od **24.08.2023 r.**" | `https://interrisk.pl/wp-content/uploads/2023/08/OWU-DOM-MAX-obowiazujace-od-24.08.2023-r.pdf` |
| **[S7]** | InterRisk TU SA VIG | OWU **Bezpieczny Dom w InterRisk** | uchwała **nr 01/20/01/2026** Zarządu InterRisk | `https://interrisk.pl/wp-content/uploads/2026/02/OWU-Bezpieczny-Dom-w-InterRisk-obowiazujace-od-02.2026.pdf` |
| **[S8]** | Link4 TU SA | OWU **DOM 24** | zatwierdzone przez Zarząd Link4 **3 kwietnia 2009 r.**, do umów od 1 maja 2009 r. | `https://www.link4.pl/sites/default/files/files/owu_dom_24.pdf` |

⚠️ **[S8] jest dokumentem archiwalnym i tak jest tu traktowany.** Bieżące OWU Link4 („OWU Dom od
24.10.2025") żyje w portalu `dokumenty.link4.pl` renderowanym po stronie klienta i nie dało się go
pobrać ani jako PDF, ani przez `WebFetch`. Link4 zostaje w zestawieniu wyłącznie dlatego, że jego
archiwalne OWU zawiera **trzeci, odrębny model granicy mury/elementy stałe** (§4), którego nie ma
w żadnym z pozostałych siedmiu dokumentów, i ten fakt jest datowany, a nie zgadnięty.

Sześć towarzystw i **pięć bieżących OWU** (PZU 2025, Warta 2026, ERGO Hestia 2026, Allianz 2025,
InterRisk 2026) spełnia minimum z briefu.

### 1.2 Źródła regulacyjne

| # | Dokument | URL / lokalizacja |
|---|---|---|
| **[R1]** | Ustawa z 15 grudnia 2017 r. **o dystrybucji ubezpieczeń** (Dz.U. 2017 poz. 2486) — art. 8 | `https://isap.sejm.gov.pl/isap.nsf/download.xsp/WDU20170002486/U/D20172486Lj.pdf` |
| **[R2]** | **Uchwała nr 190/2026 KNF z 26 czerwca 2026 r.** — Rekomendacje dla zakładów ubezpieczeń dotyczące dystrybucji ubezpieczeń (Dz. Urz. KNF z 2026 r. poz. 10, ogłoszona 29.06.2026) | `https://dziennikurzedowy.knf.gov.pl/DU_KNF/2026/10/akt.pdf`; kopia w repo: `../knf-rekomendacje-190-2026.txt` |
| **[R3]** | **Wytyczne KNF z 24 czerwca 2014 r.** dla zakładów ubezpieczeń dotyczące dystrybucji ubezpieczeń (uchwała nr 184/2014, Dz. Urz. KNF 2014 poz. 13) | `https://www.knf.gov.pl/knf/pl/komponenty/img/knf_177329_Wytyczne_dystrybucja_ubezpieczen_24-06-14_38337.pdf` |

---

## 2. Struktura produktu — co się w ogóle ubezpiecza

Wszystkie osiem dokumentów zgadza się co do rdzenia i różni się nazwami. Rdzeń to **cztery
przedmioty plus dwa dodatki**:

| Warstwa | PZU [S1] | Warta [S2] | ERGO 7 [S3] | Allianz [S4] | Generali [S5] | InterRisk [S6] |
|---|---|---|---|---|---|---|
| Konstrukcja (mury) | *nieruchomość*: dom jednorodzinny / mieszkanie | *dom jednorodzinny* / *lokal mieszkalny* | *Dom* / *Mieszkanie* | *dom* / *mieszkanie* | *dom* / *mieszkanie* / *lokal segment* | *Budynek mieszkalny* / *Lokal mieszkalny* |
| Wykończenie | **osobny przedmiot**: *stałe elementy* | wewnątrz definicji domu/lokalu | *elementy wykończenia* wewnątrz definicji | *stałe elementy wykończeniowe* | *stałe elementy* (osobna pozycja) | *Stałe elementy* |
| Ruchomości | *ruchomości domowe* (+ *mienie specjalne*) | *ruchomości domowe* (+ *specjalne*) | *Wyposażenie* + *Rzeczy osobiste* | *ruchomości domowe* | *ruchomości domowe* | *Mienie ruchome* |
| OC w życiu prywatnym | ✔ (rozdz. 10) | ✔ | ✔ (+ OC Najemcy) | ✔ | ✔ | ✔ |
| Assistance | *Pomoc w Domu*, *assistance medyczny*, *ochrona danych* | ✔ (w wariancie Komfort) | *Home Assistance* | ✔ | ✔ (+ assistance rowerowy) | ✔ (+ assistance rowerowy) |
| Dodatki poza rdzeniem | ochrona prawna, Asystent Zdrowotny, nagrobek, rośliny ogrodowe | mała architektura, ogrodzenie, budowa | NNW, OC Najemcy, architektura ogrodu | budynki/lokale gospodarcze, budowa | budynek gospodarczy, garaż, budowle | NNW, ochrona prawna, nagrobki, rower |

**Wniosek R-1.** Podział na **mury → elementy stałe → ruchomości → OC → assistance** jest stały na
rynku i nadaje się na pierwszy poziom drzewa domeny. Nazwy są zmienne, więc kod elementu ma być
angielski i neutralny (`BUILDING`, `FIXTURES`, `CONTENTS`, `LIABILITY`, `ASSISTANCE`), a etykieta
polska — atrybutem.

---

## 3. ⭐ Granica mury / elementy stałe — trzy różne modele, nie jeden

To jest najciekawsza różnica rynkowa w tym produkcie i **brief słusznie ją wytypował**. Rynek nie
różni się listą przedmiotów — różni się tym, **czym ta lista jest w konstrukcji polisy**.

### Model A — elementy stałe jako **osobny przedmiot z własną sumą ubezpieczenia** (PZU)

[S1] §2 pkt 52 definiuje *stałe elementy* jako samodzielne pojęcie, a §2 pkt 22 lit. b mówi wprost
o „ubezpieczeniu stałych elementów" jako o odrębnym ubezpieczeniu z własnym miejscem ubezpieczenia.
Klient ustala **osobną SU** dla murów i osobną dla wykończenia.

Zakres pojęcia (cytat skrócony z [S1] §2 pkt 52 lit. a):

> meble wraz z zamontowanym w nich sprzętem gospodarstwa domowego, wykładziny: ścian, podłóg,
> schodów, sufitów, słupów, kominki, schody wewnętrzne, antresole, podwieszane sufity, tynki,
> powłoki malarskie, wewnętrzne ścianki działowe, **stolarka okienna lub drzwiowa wraz z oszkleniami
> i zamknięciami, parapety wewnętrzne**, instalacje wraz z urządzeniami oraz osprzętem […],
> lustra wmontowane w ścianach, **stacje ładowania pojazdów, magazyny energii**

Litera c dokłada klimatyzację, pompy ciepła, kraty, rolety, monitoring i domofony; litera d —
zabudowy balkonów, fotowoltaikę, szambo, przydomową oczyszczalnię, zbiornik na gaz.

### Model B — elementy stałe **wewnątrz sumy nieruchomości**, bez podlimitu (Warta, ERGO Hestia, Allianz, Generali)

[S2] definiuje *Lokal mieszkalny* i *Dom jednorodzinny* tak, że **obejmują** instalacje i stałe
elementy; nie ma osobnej SU. Lista Warty jest niemal identyczna z PZU, w tym:

> d) **okna, drzwi zewnętrzne i wewnętrzne wraz z futrynami, oszkleniem, zamknięciami**,
> e) wewnętrzne: parapety, żaluzje, rolety, moskitiery […]
> j) zewnętrzne: parapety, zabezpieczenia przeciwwłamaniowe okien i drzwi, punkty oświetleniowe,
> skrzynki na listy, wszelkiego rodzaju markizy, rolety i żaluzje, moskitiery, anteny, domofony,
> wideodomofony, kamery, rynny, rury spustowe, płotki śniegowe

[S5] Generali (§4 pkt 77) wymienia je jako *stałe elementy* z osobną pozycją w §… przedmiotów
ubezpieczenia, w tym lit. g: „**zewnętrzna i wewnętrzna stolarka okienna i drzwiowa wraz
z oszkleniem, zamknięciami i zamkami**".

### ⭐ Model C — **ERGO 7 przesuwa granicę** — okna i drzwi są po stronie MURÓW

[S3] w definicji pojęcia *Dom*:

> Do domu należą również:
> a) **mury**, fundamenty, dach, tarasy, balkony, **ściany działowe niemające konstrukcji szkieletowej**;
> b) **zewnętrzne: drzwi, okna, parapety, rolety, kraty, markizy**;
> c) tynki zewnętrzne, elewacje, izolacje cieplne, przeciwwodne i dźwiękochłonne, schody, rynny […]

Czyli: **zewnętrzna** stolarka jest częścią *Domu*, a wewnętrzne wykończenie idzie do
*elementów wykończenia*. Dodatkowo **ścianka działowa o konstrukcji szkieletowej NIE jest murem**
u ERGO, a u PZU „wewnętrzne ścianki działowe" są elementem stałym bez rozróżnienia konstrukcji.

### Model D — elementy stałe w sumie nieruchomości, ale z **podlimitem procentowym** (Link4, archiwalne)

[S8] §5.1.1:

> stałe elementy nieruchomości – **do 30% sumy ubezpieczenia nieruchomości**

**Wniosek R-2 — kluczowy dla konfiguracji.** Granica mury/elementy stałe to **nie jest lista
przedmiotów, tylko trójka niezależnych parametrów**:

1. **Czy elementy stałe mają własną SU** (`SEPARATE`) czy siedzą w SU nieruchomości (`INCLUDED`).
2. **Czy w drugim przypadku obowiązuje podlimit** (Link4: 30%) czy nie (Warta, ERGO, Generali).
3. **Po której stronie leży stolarka zewnętrzna** (ERGO: mury; reszta: elementy stałe).

Trzy parametry, nie jedna decyzja. To jest dokładnie ten rodzaj wiedzy, którego nie wolno zaszyć
w Javie, bo zmiana modelu = zmiana kilku pól ekranu i sposobu liczenia SU.

---

## 4. Sumy ubezpieczenia i wartości

### 4.1 Wartości

Wszystkie dokumenty operują tą samą parą pojęć:

| Pojęcie | Definicja (za [S5] §4 pkt 107 i 109, zgodnie z [S1] §2 pkt 68, [S3], [S6] §2 pkt 99/101) |
|---|---|
| **wartość odtworzeniowa (nowa)** | koszt przywrócenia mienia do stanu sprzed szkody, nowego lecz nieulepszonego |
| **wartość rzeczywista** | wartość odtworzeniowa **pomniejszona o stopień zużycia technicznego** |

Kiedy która — najbardziej wprost mówi to [S5]:

> a) **wartość rzeczywista** dla domu letniskowego, budynku gospodarczego, garażu wolnostojącego
> i budowli […]
> c) **wartość odtworzeniowa dla domu bez względu na jego wiek i stopień zużycia technicznego**
> d) wartość odtworzeniowa dla nieruchomości w budowie, mieszkania, lokalu segmentu, stałych elementów […]

[S1] wiąże wybór z **wiekiem obiektu**: dom letniskowy i budynek niemieszkalny → wartość nowa do
20 lat, rzeczywista powyżej; budynek wielomieszkaniowy → próg **50 lat** i dodatkowo **konstrukcja
niepalna vs palna**. Stałe elementy zawsze w wartości nowej.

**Wniosek R-3.** *Wiek budynku* i *palność konstrukcji* nie są tylko czynnikami taryfowymi —
w OWU **przełączają sposób wyceny szkody**. Dla demo znaczy to, że pytanie o rok budowy i materiał
ścian ma dwa skutki naraz i warto to pokazać.

### 4.2 ⭐ Jak ustalana jest SU murów — Generali opisuje mechanizm wprost

Reguła domyślna jest wszędzie ta sama i pusta: „suma ubezpieczenia **powinna odpowiadać wartości**
ubezpieczanych przedmiotów i stanowi górną granicę odpowiedzialności" ([S1] §45 ust. 6). Kto ma tę
wartość wyliczyć — OWU nie mówią. Z jednym wyjątkiem.

[S5] **§ 27 — „Sugerowana suma ubezpieczenia"**, cytat:

> 2. W przypadku domu, mieszkania lub lokalu segmentu suma ubezpieczenia stanowi **iloczyn
> powierzchni użytkowej** domu, mieszkania lub lokalu segmentu **i wartości przyjętej przez
> Generali za 1 m² w danej miejscowości na podstawie danych statystycznych Generali**.
> 3. Jeżeli Ubezpieczający **zaakceptuje sugerowaną sumę ubezpieczenia** […] lub ustali ją na
> poziomie wyższym od sugerowanego, **odpowiedzialność Generali zostanie automatycznie zwiększona
> do 115% sumy ubezpieczenia** wskazanej w dokumencie ubezpieczenia.
> […]
> 7. **Decyzję o wysokości sum ubezpieczenia** dla poszczególnych przedmiotów ubezpieczenia
> **podejmuje Ubezpieczający**.

Dla ruchomości i stałych elementów (ust. 4–5) mechanizm jest ten sam, tylko podstawą są „własne dane
statystyczne" zamiast iloczynu metrażu i stawki. Domu letniskowego system nie obejmuje (§ … ,
zdanie „nie stosuje się systemu sugerowanej sumy ubezpieczenia").

**Wniosek R-3a — wprost pod §5 briefu.** Teza „suma ubezpieczenia liczona, nie wybierana" **nie jest
wymyślona na potrzeby demo**. Jest udokumentowaną praktyką rynkową i ma dokładnie tę strukturę,
którą chcemy pokazać: `SU = metraż × stawka(miejscowość)`, klient może ją **podnieść, ale nie
obniżyć bez konsekwencji**, a akceptacja jest **nagradzana** — bufor 115% zamiast kary za
niedoubezpieczenie. To jest lepszy materiał demonstracyjny niż straszenie proporcją, bo pokazuje
regułę, która klientowi **coś daje**.

➜ **Brak.** Wartości stawki zł/m² „w danej miejscowości" Generali nie publikuje (to jego dane
statystyczne). Każda stawka zł/m² w demo jest **demonstracyjna** (odpowiednik W-3 z LIFE).

### 4.3 Pierwsze ryzyko — zasada proporcji na tym rynku praktycznie nie istnieje

To jest korekta oczekiwania, z którym zaczynałem research.

| TU | Co mówi OWU |
|---|---|
| **InterRisk** [S6] **§ 6** | „W **każdym** rodzaju ubezpieczenia, z wyjątkiem NNW, kosztów leczenia, kosztów ratownictwa i poszukiwania oraz OC w życiu prywatnym, umowa ubezpieczenia jest zawarta w **systemie Ubezpieczenia na pierwsze ryzyko** i w związku z tym **nie ma zastosowania zasada proporcji** przy wypłacie odszkodowania" |
| **Generali** [S5] §4 pkt 95 | definiuje *ubezpieczenie na pierwsze ryzyko* jako system, „w którym suma ubezpieczenia odpowiada przypuszczalnej, maksymalnej wartości pierwszej szkody"; stosuje je m.in. do domu letniskowego, budynku gospodarczego, garażu, budowli i ruchomości |
| **PZU** [S1], **Warta** [S2], **ERGO 7** [S3], **Allianz** [S4], **Link4** [S8] | ➜ **brak** — słowo „proporcja" w kontekście niedoubezpieczenia nie występuje w żadnym z tych pięciu dokumentów. Występuje wyłącznie przy podziale odszkodowania między współwłaścicieli i przy zwrocie składki za niewykorzystany okres |

**Wniosek R-4 (skorygowany).** Polski detal majątkowy **odszedł od zasady proporcji**. Suma jest
**pułapem**, nie mianownikiem. Ostrzeżenie o niedoubezpieczeniu w demo nie może więc brzmieć
„odszkodowanie zostanie proporcjonalnie obniżone" — bo to nieprawda dla większości rynku.
Prawdziwe brzmienie to: **„suma niższa niż odtworzenie znaczy, że szkoda całkowita nie zostanie
pokryta w całości"**, a u Generali dodatkowo „akceptacja sugerowanej sumy podnosi odpowiedzialność
do 115%". Ta różnica jest merytoryczna, nie stylistyczna, i to ona decyduje o treści komunikatu APK.

---

## 5. Limity i podlimity — tam, gdzie rynek naprawdę się różni

Wszystkie liczby cytowane; puste pole = nie znaleziono w dokumencie.

| Pozycja | PZU [S1] §46 | ERGO 7 [S3] §7 | Allianz [S4] pkt 26/30 | Link4 [S8] §5 | Warta [S2] | Generali [S5] |
|---|---|---|---|---|---|---|
| **Gotówka / wartości pieniężne** | **10% SU, max 10 000 zł** | **10% SU rzeczy osobistych, max 10 000 zł** | **10% SU ruchomości, max 10 000 zł** | **10% SU ruchomości** | 1 000 zł (w limicie napadu 10 000 zł) | 1 000 zł (w limicie 5 000 zł) |
| **Biżuteria, złoto, srebro, platyna** | **40% SU, max 50 000 zł** | **50% SU rzeczy osobistych** | *przedmioty wartościowe*: 10% SU ruchomości, **max 20 000 zł** (podwyższalne za dopłatą) | — | — | — |
| **Sprzęt do działalności gospodarczej** | SU, **max 10 000 zł** | 20% łącznej SU wyposażenia i rzeczy osobistych, **max 10 000 zł** | — | — | — | — |
| **Ruchomości w pomieszczeniu gospodarczym / przynależnym** | 30% SU | 30% SU (ogień), **100% SU (kradzież)** | 30% SU ruchomości | 10% SU ruchomości | — | — |
| **Ruchomości na zewnątrz** | 10% SU | — | balkon/loggia/taras: **10 000 zł** | — | — | — |
| **Rabunek / napad poza miejscem ubezpieczenia** | SU, **max 10 000 zł** | — | napad uliczny **10 000 zł**, w tym **1 000 zł gotówki** | — | **10 000 zł, w tym gotówka 1 000 zł** | **5 000 zł, w tym wartości pieniężne 1 000 zł** |
| **Kradzież zwykła** (bez śladów włamania) | — | opcja dla architektury ogrodu i części zewnętrznych | **5 000 zł**; jednostki zewnętrzne pompy ciepła **20 000 zł** | — | — | opcja |
| **Wyłudzenie gotówki / biżuterii** („na wnuczka") | osobne ryzyko, podlimit 10% limitu kradzieżowego, max 10 000 zł | — | — | — | — | **50 000 zł na zdarzenie** |
| **Szkody od kun / zwierząt dzikich** | **50 000 zł** | 10% SU, **max 30 000 zł** (brak limitu kwotowego przy SU domu > 5 mln zł) | — | — | — | — |
| **Awaria fotowoltaiki lub pompy ciepła** | **8 000 zł** | stacje ładowania **4 000 zł** | — | — | — | — |
| **Koszty poszukiwania przyczyny szkody** | — | 10% SU domu/mieszkania, **max 50 000 zł** | 20% SU uszkodzonego przedmiotu (finansowanie działań po szkodzie) | 3% SU nieruchomości | — | — |
| **Wymiana zabezpieczeń po utracie kluczy** | — | **1 000 zł** | — | — | — | — |
| **Rośliny ogrodowe** | osobne ubezpieczenie z własną SU | **10 000 zł łącznie** | — | — | — | — |
| **OC — suma gwarancyjna** | ustalana w polisie | **do 2 000 000 zł (Wariant III)**; drony brak danych | drony **3 000 zł** | — | ulega konsumpcji | **nie ulega konsumpcji** |

**Wniosek R-5.** Limity mają dwie postacie naraz — **procent sumy** i **kwota** — i wygrywa niższa
([S1] §46 ust. 3: „górną granicą odpowiedzialności PZU jest **najniższy** limit […] spośród tych,
do których kwalifikuje się ten przedmiot"). To jest wzorzec `min(ratio × SU, absolute)`, ten sam,
który w LIFE nazwano „reguła bazowa × reguła modyfikująca" (D-3/D-7). Dobrze się przeniesie.

**Wniosek R-6.** Generali różni się w jednym miejscu jakościowo: **suma gwarancyjna OC nie ulega
konsumpcji**. To nie jest liczba, to inny kształt produktu — czyli kandydat na atrybut domenowy,
nie na wiersz w tabeli stawek.

---

## 6. Franszyzy i udziały własne

| TU | Konstrukcja | Cytat / wartość |
|---|---|---|
| **Generali** [S5] | *franszyza redukcyjna* — kwota, o którą Generali **każdorazowo** pomniejsza odszkodowanie; **warunek dostępności powodzi w strefie 2**, wykupywalna za dopłatą | §4 (słowniczek, *franszyza redukcyjna*), §21 ust. 1 |
| **InterRisk** [S6] | rozróżnia **franszyzę integralną** (poniżej niej brak wypłaty, powyżej wypłata **bez potrącenia**) i **franszyzę redukcyjną**; standardowo **200 PLN**, nie stosowana do szkód w elementach szklanych; w klauzulach dodatkowych **20% wartości szkody, min. 500 PLN** | §2 pkt 21–22 (słowniczek); §5 ust. 6–7; klauzule K1/K2 |
| **Allianz** [S4] | **udział własny 200 zł** — **opcjonalny, obniża składkę**; nie dotyczy assistance i szkód osobowych w OC | pkt 31 |
| **ERGO Hestia** [S3] | udział własny **500 zł** przy gradzie, upadku drzewa i naporze śniegu; **10% udziału własnego jako sankcja**, gdy klient korzysta ze zniżki za zabezpieczenia, a te nie zadziałały | §7 ust. 3 oraz postanowienia o udziale własnym w Ubezpieczeniu Mienia |
| **PZU** [S1] | ➜ **brak** — słowa „franszyza" ani „udział własny" nie ma w dokumencie; odpowiednikiem są limity i podlimity | — |
| **Warta** [S2] | ➜ **brak** w OWU; jest natomiast sankcja za niesprawne zabezpieczenia, gdy przyznano za nie zniżkę | postanowienia o zabezpieczeniach przeciwwłamaniowych |

**Wniosek R-7.** Franszyza jest na tym rynku **osią sprzedażową, nie tylko techniczną**: u Allianz
klient ją *wybiera*, żeby zapłacić mniej; u Generali jest *warunkiem dostępności* ryzyka powodzi.
Te dwie role są różne i demo powinno pokazać obie — pierwsza wchodzi do rekomendacji wariantu
(rek. 8.2.d: akceptowalna składka), druga do dostępności zakresu.

**Wniosek R-8.** ERGO ma sankcję „10% udziału własnego, jeśli korzystasz ze zniżki za zabezpieczenia,
a zabezpieczenia nie zadziałały". To jest reguła wiążąca **zniżkę** z **udziałem własnym** — czyli
zależność między dwoma parametrami polisy, a nie prosty czynnik taryfowy.

---

## 7. Wyłączenia nadające się na konfrontację z sytuacją klienta (rek. 8.2.c)

Rekomendacja 8.2.c każe ustalić cechy i sytuację klienta **po to, żeby sprawdzić, czy wyłączenia
nie stoją w sprzeczności z jego potrzebami**. Poniżej te wyłączenia, które da się związać
z odpowiedzią na proste pytanie — czyli materiał na tabelę `apk.exclusion.conflict`.

| Sytuacja klienta | Co się wtedy dzieje | Źródło |
|---|---|---|
| **Nieruchomość niezamieszkana / nieużytkowana** | Generali: **pustostan po 90 dniach** ciągłej nieobecności bez opieki i nadzoru → wyłączone. Allianz: **wyłączenie po 60 dniach**, chyba że klient zgłosił to **przed** zawarciem umowy i Allianz potwierdził odpowiedzialność w polisie. Warta: pustostany wyłączone wprost. Link4: przedmiotem ubezpieczenia są wyłącznie miejsca **zamieszkałe** | [S5] §4 pkt 68 („pustostan”), [S4] pkt 79.28, [S2], [S8] §11.2.1, §13 |
| **Wynajem / mieszkanie zajmowane przez najemcę** | Warta obejmuje najemców wprost („osoby, które są najemcami i chcą ubezpieczyć swoje mienie"); ERGO ma **osobne OC Najemcy**; Link4 wyłącza mienie „służące do prowadzenia działalności gospodarczej" | [S2], [S3], [S8] §13 |
| **Dom w remoncie** | Link4: dopuszczalny **pod warunkiem, że w czasie remontu jest zamieszkały** | [S8] §11.3 |
| **Dom starszy niż 50 lat** | ERGO: „Możesz ubezpieczyć również dom starszy niż 50-letni, **jeśli przeszedł remont** – wypłata według wartości domu w stanie nowym" — czyli bez remontu wypłata idzie w wartości rzeczywistej | [S3] |
| **Brak okresowych przeglądów instalacji** | Allianz: szkody z naruszenia obowiązku przeglądów gazowej/elektrycznej/kominowej — **limit 100 000 zł**, i tylko dla obiektu **nie starszego niż 40 lat** | [S4] pkt 30.5 |
| **Działalność gospodarcza w mieszkaniu** | Allianz: „rodzaj działalności prowadzonej w miejscu ubezpieczenia" jest **czynnikiem taryfowym**; Link4 wyłącza mienie firmowe; PZU i ERGO mają osobne limity na sprzęt firmowy (10 000 zł) | [S4] pkt 134, [S8] §13, [S1] §46, [S3] §7 |
| **Rowery, sprzęt elektroniczny w piwnicy / garażu** | Warta wyłącza sprzęt RTV, komputerowy, fotograficzny i wyroby ze złota **znajdujące się w garażu, innym budynku lub w pomieszczeniach przynależnych** | [S2] |
| **Mieszkanie na parterze / pomieszczenie przynależne w piwnicy** | Generali: mieszkanie powyżej parteru = **strefa 0** dla powodzi, ale ochrona **nie obejmuje wtedy pomieszczeń przynależnych** na parterze lub niżej | [S5] §21 ust. 2 |
| **Przedmioty wartościowe powyżej progu** | Allianz: ruchomości o wartości jednostkowej **> 30 000 zł** objęte **tylko** po zgłoszeniu i udokumentowaniu spisem lub dowodem nabycia | [S4] pkt 29 |
| **Rażące niedbalstwo** | PZU odpowiada, ale **do 50% sumy ubezpieczenia** | [S1] §46 ust. 5 |

**Wniosek R-9.** Osiem z jedenastu wierszy powyżej da się rozstrzygnąć **jednym pytaniem tak/nie
albo jedną liczbą**. To jest dowód, że „konfrontacja wyłączeń z sytuacją klienta" jest wykonalna
jako tabela decyzyjna, a nie jako esej — i że warto ją zrobić konfiguracją.

---

## 8. Powódź i zalanie — jedyny realny wymiar geograficzny

To jest wymiar, którego MOTO i LIFE w tej aplikacji nie mają.

### 8.1 Karencja — standard rynkowy

| TU | Karencja na powódź | Wyjątek |
|---|---|---|
| PZU [S1] | tak, okres wskazany w polisie (karencja) | „Karencji nie stosuje się w umowie […]" — kontynuacja |
| ERGO 7 [S3] | tak | „Karencja nie występuje w przypadku **nieprzerwanej kontynuacji**"; stosowana we wszystkich ubezpieczeniach poza wskazanymi wyjątkami |
| InterRisk [S6][S7] | **30 dni od daty zawarcia umowy** | brak przy nieprzerwanej kontynuacji |
| Allianz [S4] | **30 dni** | „Jeśli zawierasz […] zachowując ciągłość naszej ochrony od powodzi, **nie zastosujemy 30-dniowej karencji**"; ponadto: „**Po ocenie zagrożenia powodziowego karencji możemy nie stosować**" (pkt 156) |

### 8.2 Strefy — Generali podaje je wprost

[S5] §21 ust. 1, cytat:

> Rozszerzenie zakresu ubezpieczenia o ryzyko powodzi możliwe jest w zależności od zagrożenia
> powodziowego, występującego w miejscu ubezpieczenia, określonego przez Generali w oparciu
> o **statystykę zdarzeń szkodowych** i podzielonego na **strefę 0, strefę 1, strefę 2 i strefę X**.
> Ryzyko powodzi dostępne jest w sytuacji, gdy miejsce ubezpieczenia znajduje się w strefie 0
> lub w strefie 1 i w strefie 2, **pod warunkiem zastosowania franszyzy redukcyjnej**, którą
> Ubezpieczający może wykupić za opłaceniem dodatkowej składki. Mienie znajdujące się
> **w strefie X nie może zostać objęte ubezpieczeniem od ryzyka powodzi**.

I dalej, ust. 2: mieszkanie powyżej parteru **jest uznawane za strefę 0** niezależnie od adresu.

ERGO [S3] §40 ust. 3–4 potwierdza tę samą mechanikę innymi słowami: składka za powódź ustalana
„przy uwzględnieniu okresu udzielania ochrony i **prawdopodobieństwa wystąpienia powodzi w miejscu
ubezpieczenia**", a w ubezpieczeniu od kradzieży — „wysokość składki zależy od **strefy regionalnej
miejsca ubezpieczenia**".

**Wniosek R-10 — najważniejszy dla struktury demo.** Strefa nie jest czynnikiem mnożnikowym.
Jest **czterowartościowym przełącznikiem dostępności**:

| Strefa | Dostępność powodzi |
|---|---|
| 0 | dostępna bez warunków |
| 1 | dostępna bez warunków |
| 2 | dostępna **tylko z franszyzą redukcyjną** |
| X | **niedostępna** |

Do tego **dwie strefy naraz**: powodziowa (dostępność) i kradzieżowa (składka) — u ERGO to dwa różne
podziały tego samego adresu. Demo powinno mieć obie, bo jedna strefa wyglądałaby jak zwykły czynnik.

---

## 9. Czynniki taryfowe — cztery niezależne listy, zacytowane

Cztery z ośmiu dokumentów podają listę czynników wprost. Zestawienie poniżej to **jedyne twarde
źródło** tego, co realnie wpływa na składkę w tym produkcie.

### PZU [S1] §97 ust. 1

> 1) sumy ubezpieczenia, sumy gwarancyjnej, **zakresu ubezpieczenia, okresu odpowiedzialności**;
> 2) **rodzaju przedmiotu ubezpieczenia i jego charakterystyki, położenia miejsca ubezpieczenia**;
> 3) wyboru wariantu serwisowego;
> 4) rozszerzenia zakresu terytorialnego — w OC;
> 5) liczby ubezpieczonych — w NNW;
> 6) **wieku ubezpieczającego, ubezpieczonego**;
> 7) **zawarcia innych umów ubezpieczenia w PZU**;
> 8) **przebiegu umowy ubezpieczenia**;
> 9) przeniesienia prawa do odszkodowania (cesja);
> 10) **sposobu płatności składki**;
> 11) **oceny ryzyka, w tym związanej z zamontowanymi zabezpieczeniami przeciwpowodziowymi**.

### Warta [S2] §51

> 1) przedmiotu ubezpieczenia, 2) **adresu miejsca ubezpieczenia**, 3) **charakteru użytkowania
> przedmiotu ubezpieczenia**, 4) zakresu ubezpieczenia, 5) wysokości sumy ubezpieczenia/gwarancyjnej,
> 6) **charakteru umowy (nowa/wznowiona)**, 7) **przebiegu ubezpieczenia**, 8) schematu płatności składki.

### ERGO Hestia [S3] §40 ust. 2–6

> 1) **oceny poziomu ryzyka przy wnioskowanym zakresie**; 2) sposobu płatności składki;
> 3) **okresu i wariantu ubezpieczenia**; 4) wysokości sumy ubezpieczenia;
> 5) **historii umów ubezpieczenia zawieranych w ERGO Hestii**.
>
> […] za powódź — przy uwzględnieniu **prawdopodobieństwa wystąpienia powodzi w miejscu ubezpieczenia**;
> od kradzieży — od **strefy regionalnej** oraz **zniżek za zabezpieczenia przeciwkradzieżowe**;
> OC — także od **liczby pełnoletnich osób** objętych ochroną.

### Allianz [S4] pkt 134

> przedmiot ubezpieczenia, wysokość zadeklarowanych sum ubezpieczenia lub limitów, zakres
> ubezpieczenia, **miejsce ubezpieczenia**, **sposób zabezpieczenia mienia**, **rodzaj i palność
> budynków**, **długość okresu ubezpieczenia**, **historia szkodowa** poprzednich umów,
> **rodzaj działalności prowadzonej w miejscu ubezpieczenia** […]

### 9.1 Synteza — co występuje u ilu

| Czynnik | PZU | Warta | ERGO | Allianz | Razem |
|---|---|---|---|---|---|
| Suma ubezpieczenia | ✔ | ✔ | ✔ | ✔ | **4/4** |
| Zakres / wariant | ✔ | ✔ | ✔ | ✔ | **4/4** |
| Lokalizacja (adres, strefa) | ✔ | ✔ | ✔ | ✔ | **4/4** |
| Sposób / schemat płatności | ✔ | ✔ | ✔ | — | 3/4 |
| Historia szkodowa / przebieg | ✔ | ✔ | ✔ | ✔ | **4/4** |
| Okres ubezpieczenia | ✔ | — | ✔ | ✔ | 3/4 |
| Zabezpieczenia | ✔ (przeciwpowodziowe) | — | ✔ (przeciwkradzieżowe) | ✔ | 3/4 |
| Charakter użytkowania / działalność | — | ✔ | — | ✔ | 2/4 |
| Rodzaj i palność konstrukcji | ✔ (pośrednio) | — | — | ✔ | 2/4 |
| Nowa czy wznowiona / inne polisy | ✔ | ✔ | ✔ | — | 3/4 |
| Wiek ubezpieczonego | ✔ | — | — | — | 1/4 |

**Wniosek R-11.** Rdzeń taryfy to **suma × zakres × lokalizacja × zabezpieczenia × historia**.
Metraż, rok budowy i materiał ścian **nie pojawiają się wprost** w żadnej z czterech list — wchodzą
przez „rodzaj przedmiotu ubezpieczenia i jego charakterystykę" (PZU) oraz „rodzaj i palność
budynków" (Allianz). W demo to znaczy: metraż i rok budowy są **wejściem do wyliczenia SU**,
a dopiero suma jest wejściem do taryfy. Ta kolejność jest zgodna z rynkiem i jest ciekawsza
demonstracyjnie niż wrzucenie metrażu wprost do stawki.

➜ **Brak.** Żadne OWU nie publikuje **wartości** czynników (mnożników, stawek promilowych, wysokości
zniżek). Taryfy stawek są dokumentami wewnętrznymi. Wszystkie liczby w taryfie demo są wymyślone.

---

## 10. Warianty i sposób sprzedaży

| TU | Warianty |
|---|---|
| ERGO 7 [S3] | **Wariant I / II / III**; III ma OC do 2 mln zł, NNW rozszerzone o zawał, udar i nowotwór sutka/prostaty, SU NNW do 500 000 zł |
| Allianz [S4] | pakiety **KOMFORT** i **MAX** (nazwy występują w treści; pełna lista pakietów — patrz §13 „braki") |
| Warta [S2] | wariant **Komfort** (assistance), wariant serwisowy |
| PZU [S1] | **wariant serwisowy** jako osobny wybór wpływający na składkę |
| Generali [S5], InterRisk [S6][S7], Link4 [S8] | konfiguracja przez wybór przedmiotów i rozszerzeń, bez nazwanej drabinki wariantów |

**Wniosek R-12.** Rynek sprzedaje ten produkt na **dwa różne sposoby naraz**: drabinką wariantów
(ERGO, Allianz) i konfiguratorem przedmiotów (Generali, InterRisk). Rekomendacja 9.3 („gdy kilka
wariantów odpowiada potrzebom, wskaż wyraźnie różnice") dotyczy pierwszego modelu — więc demo
powinno mieć **wariantową drabinkę**, żeby miało co porównywać. Konfigurator przedmiotów zostaje
jako druga oś (dokupienie powodzi, kradzieży, OC).

**Wniosek R-13.** ERGO wciąga do polisy majątkowej **NNW i zdrowie** (zawał, udar, nowotwór).
To rozmywa granicę z LIFE i **świadomie zostaje poza zakresem demo majątkowego** — inaczej dwa dema
w tej aplikacji zaczęłyby pokazywać to samo.

---

## 11. Analiza potrzeb klienta (APK) — stan prawny

### 11.1 ⚠️ Korekta założenia z briefu

Brief (§5) opisuje wersję 1 regionu jako „**APK wg Wytycznych KNF z 2014**". **To jest nieścisłe.**

Sprawdzone w oryginale [R3]: Wytyczne z 24 czerwca 2014 r. mają 20 wytycznych podzielonych na
„Zarząd i rada nadzorcza", „Modele dystrybucji", „Relacje z klientami" i „Systemy IT". Słowo
„potrzeb" pada w nich **dwa razy** i ani razu w znaczeniu ustrukturyzowanej analizy potrzeb —
najbliżej jest wytyczna 7.4, o procedurach zapobiegających **misselingowi** produktów
ubezpieczeniowo-inwestycyjnych. **Wytyczne z 2014 r. nie zawierają APK.**

Obowiązek APK jest **ustawowy** i wszedł z implementacją IDD. [R1] art. 8:

> **Art. 8.** 1. Przed zawarciem umowy ubezpieczenia lub umowy gwarancji ubezpieczeniowej
> dystrybutor ubezpieczeń **określa, na podstawie uzyskanych od klienta informacji, jego wymagania
> i potrzeby** oraz podaje w zrozumiałej formie obiektywne informacje o produkcie ubezpieczeniowym,
> w celu umożliwienia klientowi podjęcia świadomej decyzji.
> […]
> 3. **Proponowana umowa ubezpieczenia […] powinna być zgodna z wymaganiami i potrzebami klienta**
> w zakresie ochrony ubezpieczeniowej lub ochrony gwarancyjnej.

To jest **całość** wymogu w stanie „przed 2027". Dwa zdania. Ustawa nie mówi ani ile pytań,
ani o jakiej sumie, ani o wyłączeniach, ani o budżecie, ani o uzasadnieniu.

➜ **Konsekwencja dla demo:** wersja 1 regionu ma być opisana jako **„APK — minimum ustawowe
(art. 8 UDU)"**, a nie „wg Wytycznych 2014". Merytorycznie nic się nie zmienia — wersja 1 nadal
jest krótką ankietą bez budżetu, bez konfrontacji wyłączeń i bez uzasadnienia. Zmienia się
**podpis na ekranie i uzasadnienie w dokumencie**, a to jest różnica między demem, które mówi
prawdę, a demem, które brzmi prawdziwie. Zapisane jako **W-1** w dokumencie projektowym.

### 11.2 Co dokłada uchwała 190/2026

[R2] §2 ust. 1:

> Zakłady ubezpieczeń dostosują swoją działalność do rekomendacji […] nie później niż w terminie
> **do dnia 1 lipca 2027 r.**, z wyjątkiem rekomendacji 7.9, w zakresie części oszczędnościowej
> produktu, do której dostosują swoją działalność w terminie do dnia 1 lipca 2028 r.

§2 ust. 2: „Do czasu dostosowania […] zakłady ubezpieczeń stosują Wytyczne […]".
§4: traci moc uchwała nr 184/2014.

Data **2027-07-01** jest więc twarda i pochodzi z aktu, a nie z interpretacji.

Rekomendacja 8 i 9 w częściach istotnych dla majątkowego — cytaty [R2]:

| Punkt | Cytat (skrót) | Nośnik w Higsonie |
|---|---|---|
| **8.1** | „Projektując APK zakład bierze pod uwagę: a) rodzaj produktu […] b) **zakres informacji niezbędnych do określenia wymagań i potrzeb klienta oraz źródła tych informacji**, c) **kluczowe wyłączenia i ograniczenia odpowiedzialności**, d) kanał dystrybucji" | katalog pytań jako elementy domeny |
| **8.2.b** | „ustalenie, jakie są wymagania i potrzeby klienta […] **na wypadek jakich okoliczności i w jakim okresie** klient chciałby zostać objęty ochroną, a także **jaka suma ubezpieczenia** […] odpowiada wymaganiom i potrzebom" | tabela `need.derive` + funkcja licząca SU |
| **8.2.c** | „ustalenie, **jakie są cechy lub sytuacja klienta**, w celu oceny, **czy wyłączenia lub ograniczenia odpowiedzialności nie stoją w sprzeczności** z wymaganiami i potrzebami" | tabela `exclusion.conflict` — materiał z §7 |
| **8.2.d** | „zaproponowanie umowy odpowiadającej wymaganiom i potrzebom, **z jednoczesnym uwzględnieniem akceptowalnej dla klienta wartości składki**" | pytanie o budżet + flow rekomendacji |
| **8.3** | wolno użyć informacji „uzyskanych od klienta w przeszłości lub pozyskanych z innych źródeł", ale trzeba **poinformować** i umożliwić **weryfikację**, wskazując na żądanie **zakres i źródło** | atrybut pytania: `prefillSource` |
| **8.4** | przy odmowie udzielenia informacji — **poinformować o konsekwencjach** | atrybut pytania: `refusalMessage` |
| **8.7** | dokumentacja APK ma **wykazać, że zaproponowana umowa jest zgodna z wymaganiami i potrzebami** | tabela komunikatów + funkcja składająca uzasadnienie |
| **9.1** | propozycja z produktów, „w odniesieniu do których klient **należy do rynku docelowego**" | tabela dostępności wariantu |
| **9.3** | gdy kilka wariantów odpowiada potrzebom — „**w sposób wyraźny wskazuje na występujące między nimi różnice**" | macierz porównawcza |

**Wniosek R-14.** Osiem z dziesięciu punktów daje się odwzorować **jeden do jednego** na artefakt
Higsona. Dwa (8.8 przechowywanie dokumentacji, 9.2 weryfikacja zmian produktu przy odnowieniu) to
procesy, nie reguły — poza zakresem demo.

### 11.3 Czego APK w majątkowych dziś nie robi

Na podstawie art. 8 [R1] i tego, co widać w dokumentach produktowych: dzisiejsza APK majątkowa to
w praktyce **oświadczenie**, nie analiza. [S4] pkt 135 opisuje mechanizm wprost — „Pytamy Cię o nie
we **wniosku** o zawarcie umowy" — czyli pytania służą **taryfikacji**, a APK jest wobec nich wtórna.

Trzech rzeczy z rekomendacji 8 **nie ma dziś w żadnym z ośmiu dokumentów**:

1. Pytania o **akceptowalną składkę** (8.2.d) — ani jedno wystąpienie.
2. **Konfrontacji sytuacji klienta z wyłączeniami** (8.2.c) — wyłączenia są wymienione w OWU
   i w karcie produktu, ale nigdzie nie ma mechanizmu wiążącego je z odpowiedzią klienta.
   Jedyny wyjątek to [S4] pkt 79.28, gdzie Allianz *odwraca* ciężar: wyłączenie znika, jeśli klient
   **sam** zgłosi niezamieszkiwanie przed zawarciem umowy.
3. **Pisemnego uzasadnienia zgodności propozycji z potrzebami** (8.7) — jest oświadczenie
   o zapoznaniu się z warunkami, nie ma uzasadnienia doboru.

**Wniosek R-15 — to jest teza demo, potwierdzona źródłowo.** Różnica między wersją 1 a wersją 2
nie jest wymyślona na potrzeby prezentacji. Trzy rzeczy, które wersja 2 dokłada, są **dokładnie
tymi trzema, których dziś na rynku nie ma**, i mają twardy termin wdrożenia: 1 lipca 2027 r.

---

## 12. Wnioski dla konfiguracji — lista R

| # | Wniosek | Gdzie użyty |
|---|---|---|
| **R-1** | Podział mury / elementy stałe / ruchomości / OC / assistance jest stały na rynku | drzewo domeny |
| **R-2** | Granica mury/elementy stałe to **trzy niezależne parametry**, nie jedna decyzja | D-1 dokumentu projektowego |
| **R-3** | Wiek i palność przełączają **sposób wyceny szkody**, nie tylko składkę | tabela wartości |
| **R-4** | Zasada proporcji dotyczy murów; ruchomości idą na pierwsze ryzyko | ostrzeżenie o niedoubezpieczeniu |
| **R-5** | Limity mają postać `min(procent SU, kwota)` | wzorzec „reguła bazowa × modyfikująca" |
| **R-6** | Konsumpcja sumy gwarancyjnej OC to **kształt produktu**, nie liczba | atrybut domenowy |
| **R-7** | Franszyza pełni **dwie różne role**: obniża składkę i warunkuje dostępność | rekomendacja wariantu + dostępność |
| **R-8** | Zniżka za zabezpieczenia wiąże się z sankcyjnym udziałem własnym | tabela zniżek |
| **R-9** | 8 z 11 wyłączeń da się rozstrzygnąć jednym pytaniem | `apk.exclusion.conflict` |
| **R-10** | Strefa powodziowa to **przełącznik dostępności** (0/1/2/X), a strefa kradzieżowa to czynnik — dwa różne podziały adresu | dwie tabele stref |
| **R-11** | Metraż i rok budowy wchodzą **przez SU**, nie wprost do stawki | kolejność flow |
| **R-12** | Rynek sprzedaje i drabinką wariantów, i konfiguratorem — demo bierze drabinkę, żeby mieć co porównywać (rek. 9.3) | warianty |
| **R-13** | NNW i zdrowie w polisie majątkowej (ERGO) świadomie poza zakresem — to terytorium LIFE | zakres |
| **R-14** | 8 z 10 punktów rek. 8 i 9 mapuje się 1:1 na artefakt Higsona | §5 briefu potwierdzone |
| **R-15** | Trzy rzeczy, których dziś na rynku nie ma, to dokładnie te trzy, które dokłada wersja 2 | teza demo |

---

## 13. Braki — czego nie ustalono i dlaczego

| # | Czego brak | Dlaczego | Skutek |
|---|---|---|---|
| **B-1** | **Jakichkolwiek wartości taryfowych** — stawek promilowych, mnożników, wysokości zniżek | Taryfy Stawek są dokumentami wewnętrznymi; OWU odsyłają do nich, nie publikują | **Wszystkie liczby w taryfie demo są wymyślone i muszą być tak oznaczone** (odpowiednik W-3 z LIFE) |
| **B-2** | **Stawek zł/m² do wyliczenia SU murów** | Nie ma ich w żadnym OWU; leżą w kalkulatorach i cennikach budowlanych | Stawka zł/m² w demo jest demonstracyjna; realnym źródłem byłby GUS albo cennik Sekocenbud |
| **B-3** | **Sekwencji pytań w kalkulatorach online** | Kalkulatory TU są aplikacjami JS za zgodą na cookies; sesja Playwright była zajęta przez inny proces, a strony marketingowe nie renderują formularza | Kolejność pytań w ankiecie demo wynika z **logiki APK** (rek. 8.2 a→b→c→d), a nie z odwzorowania konkretnego kalkulatora. To jest uczciwsze i tak zostaje zapisane |
| **B-4** | **Bieżącego OWU Link4** | portal `dokumenty.link4.pl` renderowany po stronie klienta | Link4 cytowany wyłącznie z archiwalnego DOM 24 [S8], z jawną adnotacją o dacie |
| **B-5** | **Pełnej listy pakietów Allianz Mój Dom** | W pobranym dokumencie występują nazwy KOMFORT i MAX, ale nie ma miejsca, które wylicza wszystkie pakiety | Drabinka wariantów w demo jest własna, nie odwzorowuje Allianz |
| **B-6** | **Granic stref powodziowych** | Generali podaje, że wyznacza je „w oparciu o statystykę zdarzeń szkodowych", ale nie publikuje mapy | Strefa w demo wyznaczana z kodu pocztowego przez tabelę demonstracyjną |
| **B-7** | **Sumy gwarancyjnej OC u Warty, Generali, InterRisk, Link4** | Wskazywana w polisie, nie w OWU | Katalog sum gwarancyjnych w demo jest wymyślony |
| **B-8** | **Danych o realnym udziale wariantów w sprzedaży** | Nie publikowane | Demo nie twierdzi niczego o popularności wariantów |

---

## 14. Jedno zdanie na koniec

Rynek majątkowy w Polsce ma **stałą strukturę produktu i bardzo zmienne granice** — granicę
mury/elementy stałe, granicę dostępności powodzi, granicę pustostanu, granicę wieku budynku
przełączającą sposób wyceny. Każda z tych granic to jedna liczba albo jedno rozstrzygnięcie
w tabeli, każda zmienia się z OWU na OWU, i każda dziś siedzi w kodzie kalkulatorów. To jest
dobry produkt na demo silnika reguł niezależnie od APK — a APK z terminem 1 lipca 2027 r. dokłada
do tego drugą, regulacyjną oś zmiany, której nikt nie może zignorować.
