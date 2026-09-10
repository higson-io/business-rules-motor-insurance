# Review 31 tabel decyzyjnych PROPERTY (2026-09-09)

Przegląd po refaktoryzacji 19 funkcji, z pełną wiedzą o tym, kto którą kolumnę czyta. Kryteria te
same, co przy funkcjach: **martwe wyjścia**, **wiersze nieosiągalne**, **wymiary, które nie
różnicują**, **treść powtórzona gdzie indziej**.

⛔ Nic nie zostało wykonane — to jest lista do decyzji.

## Wynik w jednym zdaniu

**Trzydzieści z trzydziestu jeden tabel jest szczelnych.** Cała nadmiarowość siedzi w jednej
rodzinie — siedmiu tabelach `cover.*` — plus cztery drobne znaleziska poza nią.

## A. Rodzina `cover.*` — kolumna `basis` nie różnicuje w ŻADNEJ z siedmiu

| Tabela | wierszy | co naprawdę się różni |
|---|---|---|
| `cover.building` | 3 | **tylko `rate`** |
| `cover.contents` | 4 | **tylko `rate`** |
| `cover.fixtures` | 4 | **tylko `rate`** |
| `cover.liability` | 2 | **tylko `rate`** |
| `cover.assistance` | 3 | `rate`, `capAbsolute`, `capLabel` |
| `cover.flood` | 5 | `rate`, `available`, `reason`, `minDeductible` |
| `cover.theft` | 5 | `rate`, `available`, `reason`, `capRatio`, `capAbsolute`, `capLabel` |

Każda z tych tabel ma **osiem kolumn wyjściowych**, a w czterech przypadkach zmienia się jedna.
Najostrzejszy przypadek to `basis`: przyjmuje `SUM` w sześciu tabelach i `FLAT` w siódmej, i **ani
razu nie zmienia się wewnątrz tabeli**. To nie jest wymiar, to stała pokrycia — odpowiada na
pytanie „czy stawka jest promilem sumy, czy kwotą", które nie zależy od klucza.

⚠️ **To jest w napięciu z decyzją z tego samego dnia.** §36 zdjął z `COVER` atrybuty `RATE_BASIS`,
`CAP_RATIO`, `CAP_ABSOLUTE` jako *wskaźniki na kolumny własnej tabeli*. Odesłanie `basis` z powrotem
na element byłoby stałą (`LITERAL`), nie wskaźnikiem — więc formalnie nie odwraca tamtej zasady,
ale trzeba to nazwać, żeby za tydzień nie wyglądało na wahanie.

⛔ **Nie proponuję rozbijania `capRatio`/`capAbsolute`/`capLabel`.** W `theft` te trzy zmieniają się
RAZEM (0,40/50 000/„40% sumy" wobec 0,50/75 000/„50% sumy") — rozdzielenie ich zerwałoby parę, która
jest treścią reguły. To, że w pięciu pozostałych tabelach stoją stałe, jest ceną jednolitego
kształtu: prowadzący otwiera dowolne pokrycie i widzi ten sam układ.

**Do decyzji:** czy `basis` ma zejść na element jako `LITERAL` (7 kolumn mniej, 7 atrybutów więcej),
czy zostaje dla jednolitości kształtu.

## B. `shortLabel` w katalogu pytań — kolumna martwa

Wypełniona w **13 z 28** wierszy — wyłącznie dla pól przedmiotu ubezpieczenia. I **żadna z tych
trzynastu wartości nigdy nie trafia na ekran**:

- spis pytań (`apk__outline`) istnieje **tylko w kroku 1**, a jego pozycjami są **grupy**, które nie
  mają wiersza w katalogu — więc `shortLabel` jest tam zawsze puste i komponent spada na nazwę grupy;
- krok 2 renderuje listę po `question.text`, spisu nie ma wcale.

Kolumna powstała rano dla spisu, a po południu spis przeszedł na nazwy grup, gdy grupy stały się
poziomem drzewa. Klasyczna pozostałość — dokładnie ta sama klasa co `partOf`, usunięty wcześniej.

**Do decyzji:** usunąć kolumnę wraz z polem `shortLabel` w `Question`, DTO i modelu frontu.

## C. `flood.waitingPeriod` — dwa wiersze mówią to samo

```
zone;priorCover;days;reasonCode
*;false;30;FLOOD_WAITING
*;true;0;FLOOD_WAITING_NONE
ZX;false;0;          ← identyczne
ZX;true;0;           ← identyczne
```

Dwa wiersze `ZX` różnią się tylko wartością `priorCover`, której w strefie ZX nikt nie czyta — dają
ten sam wynik. Zwijają się do jednego `ZX;*;0;`.

⚠️ **Wierszy ZX nie wolno usunąć całkiem** — pusty `reasonCode` jest strażnikiem ciszy: bez niego ZX
spadłoby na wiersz `*` i klient w strefie, gdzie powodzi się nie sprzedaje, przeczytałby „ochrona
zaczyna działać po 30 dniach".

## D. `factor.deductible` — pierwsze dwa wiersze identyczne, trzy nieosiągalne

```
*;1.00
0;1.00      ← to samo co gwiazdka
1000;0.84   ← nieosiagalne: front nie ma kontrolki franszyzy
200;0.95    ← nieosiagalne
500;0.90    ← nieosiagalne
```

Opisane w `analiza-tabel.md`: `updateDeductible()` nie ma ani jednego wywołania w `frontend/src`.
Wejście zawsze wynosi zero.

## E. `apk.question.visibility` — jeden z czterech wierszy rządzi martwym pytaniem

Wiersz `RENOVATED;*;* - 50;false` steruje widocznością pytania, którego `quote.property.renovated`
**nie czyta żadna reguła ani Java** — jedyne wystąpienie poza katalogiem to deklaracja typu
w `context.def`.

## F. `offer.choiceKind` — otwarty zakres jako uśpiona pułapka

Pierwszy wiersz to `* - -1`, a `choiceDelta` potrafi zwrócić `null` (gdy nie ma rekomendacji albo
wyboru). Zgodnie z **W-30** otwarty koniec zakresu łapie `null` i bije wiersz gwiazdkowy — czyli
brak wyboru zostałby nazwany „wybór węższy niż rekomendacja".

⚙️ **Dziś nieosiągalne**, sprawdzone na żywo: zaraz po `PUT /reset` serwer podstawia rekomendowany
wariant, `delta` wynosi 0 i wychodzi `MATCHES`. To jest ryzyko, nie usterka — ale domknięcie zakresu
(`-999 - -1`) kosztuje jedną komórkę.

## G. Kolumna `prefill` — żywa w moto i życiu, martwa w majątku

Wypełniona w 4 wierszach katalogu. Podpis „Podpowiedziane: …" renderuje **wspólny** komponent
ankiety (`shared/apk/questionnaire.html`), z którego korzystają moto i życie; ankieta majątkowa ma
własny szablon i podpis z niej zdjęto na decyzję właściciela.

⚠️ To jest rek. 8.3 (informacja, skąd wzięliśmy dane). Zdolność zostaje w konfiguracji, więc
usunięcie kolumny **zabrałoby ją bezpowrotnie** — odnotowuję jako fakt, nie jako propozycję cięcia.

## Czego review NIE znalazło

Dwadzieścia cztery tabele nie mają ani jednej kolumny wyjściowej o jednej wartości, ani wiersza
nieosiągalnego. `apk.question.reveal` (6 wierszy na 5 wejściach) to prawdziwa tablica logiczna;
`apk.answer`, `apk.recommendation.reason`, `sum.ratePerSqm`, `zone.*`, rodzina `factor.*` poza
`deductible` — wszystkie różnicują w każdym wierszu.

Rzadkość katalogu pytań (`text`/`kind`/`contextPath` wypełnione w 13 z 28) **nie jest wadą**: to
cena jednego katalogu dla pytań APK (treść na elemencie drzewa) i pól przedmiotu (treść w tabeli).
Rozbicie na dwie tabele dałoby więcej artefaktów, nie mniej.
