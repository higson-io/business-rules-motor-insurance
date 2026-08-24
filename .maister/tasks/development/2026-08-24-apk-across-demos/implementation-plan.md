# APK na demach komunikacyjnym i życiowym — plan

**Data:** 2026-08-24
**Zlecenie:** „wykorzystaj wiedzę o APK i dostosuj pozostałe dema" → pełna analiza potrzeb na MOTO
i LIFE, w kształcie z profilu PROPERTY. Zatwierdzone: wariant pełny, z dopisywaniem
`demo.moto.apk.*` do profilu `DEMO`.

---

## 1. Teza, która ma z tego wyjść

Nie „trzy dema mają ankietę", tylko: **mechanizm APK jest jeden, a linia produktowa to katalog
pytań.** Jeżeli dodanie ubezpieczenia komunikacyjnego do istniejącej maszynerii oznacza dopisanie
elementów domeny i wierszy w tabelach, a nie napisanie komponentu i klasy — teza demo jest
udowodniona przez samą historię tego commita.

Z tego wynika kolejność: **najpierw uwspólnienie na działającym PROPERTY, potem dwa katalogi.**
Odwrotna kolejność dałaby trzy kopie i uwspólnienie na końcu, czyli refaktor pod presją.

---

## 2. Ryzyko do zdjęcia jako pierwsze

**R-1 — czy profil może mieć dwa regiony naraz i czy silnik rozwiąże oba po tej samej dacie.**

Reżim APK przełącza się **1 lipca 2027** (termin z §2 uchwały 190/2026). Istniejące regiony mają
progi zupełnie gdzie indziej: `MOTOR` w 2020 r., `LIFE` 1 stycznia i 1 lipca 2026 r.

⛔ **Dopisanie wersji do istniejącego regionu jest wykluczone.** Nowa wersja regionu `MOTOR`
oznacza pustą wersję 3 dla **każdego** elementu już do niego podpiętego — czyli demo komunikacyjne
przestałoby działać dla dat po 1 lipca 2027. To dokładnie ta klasa szkody, przed którą chroni
zakaz ruszania reguł MOTO.

Zamiast tego: **osobne regiony `APK_MOTOR` i `APK_LIFE`**, po dwie wersje (`2026-01-01`,
`2027-07-01`), z podpiętymi wyłącznie nowymi tabelami reżimu. Poza bezpieczeństwem daje to lepszą
demonstrację: w profilu `LIFE` istnieją wtedy **dwie niezależne osie czasu** — taryfowa
(1 lipca 2026) i regulacyjna (1 lipca 2027) — a przesunięcie daty polisy może przeciąć jedną bez
drugiej. To ta sama myśl co „wersjonowanie jest selektywne" w PROPERTY, tylko mocniejsza.

**Jeżeli R-1 się nie potwierdzi** (silnik nie rozwiąże dwóch regionów naraz), plan awaryjny:
reżim APK przestaje być wersjonowany datą w MOTO i LIFE, a demonstracja progu 2027 zostaje
wyłącznie w PROPERTY. Zapisać jako W-n, nie udawać, że działa.

---

## 3. Grupy zadań

| # | Grupa | Zawartość | Dowód ukończenia |
|---|---|---|---|
| **G0** | Zdjęcie R-1 | Region `APK_MOTOR` z dwiema wersjami, jedna tabela próbna, odczyt przez runtime przy dwóch datach | Dwie różne wartości przy dwóch datach polisy, przy nietkniętym regionie `MOTOR` |
| **G1** | Wspólny pakiet Java | `pl.decerto.higson.demo.apk`: `Question`, `Answer`, `Need`, `Recommendation`, `Regime`, `Warning`, oraz `ApkCatalogue` czytający katalog, wysiewający domyślne odpowiedzi i reżim. PROPERTY przestawione na niego | Demo majątkowe zachowuje się identycznie: 9 pytań przed progiem, 16 po, suma 474 000 → 528 000 |
| **G2** | Wspólne komponenty Angulara | `models/apk.ts`, `shared/apk/questionnaire`, `shared/apk/recommendation` — sterowane wejściami i zdarzeniami, bez wstrzykiwania sklepu. PROPERTY przestawione | Suita zielona, ekran majątkowy bez zmian wizualnych |
| **G3** | Reguły MOTO | 12 pytań jako elementy domeny, 8 tabel `demo.moto.apk.*`, funkcje, flow `demo.moto.apk.recommend`, region `APK_MOTOR` | Ankieta czytana z reguł, wyłączenia z §2 researchu |
| **G4** | MOTO backend + ekran | `MotorApkContext`, odczyt w `Motor`, DTO, sekcja APK nad tabelą porównawczą | Data polisy przesunięta przez 1 lipca 2027 zmienia zestaw pytań |
| **G5** | Reguły LIFE | 14 pytań, tabele `demo.life.apk.*`, region `APK_LIFE` | j.w. |
| **G6** | LIFE backend + ekran | j.w. dla obu ścieżek | j.w., przy nietkniętej osi taryfowej |
| **G7** | Dokumentacja | §14 w dokumencie profilu LIFE, nowy dokument profilu MOTO, INDEX, run-log | — |

---

## 4. Pułapki przeniesione z poprzednich dwóch budów

| # | Pułapka | Dotyczy tu |
|---|---|---|
| **W-22** | Nawigacja po domenie **nie wstrzykuje** kodu elementu do kontekstu — atrybut kluczowany `question.code` nie ma się z czego rozwiązać. Kontekst buduje się samemu | G3, G5 — każdy odczyt atrybutu pytania |
| **W-27** | Katalog pytań czytany przy **każdej** wycenie, po `setEffectiveDate`, nigdy raz przy tworzeniu sesji | G4, G6 |
| **W-30** | Importer XLSX odrzuca pliki z openpyxl — generator musi kopiować statyczne wpisy zipa z eksportu Higsona | G3, G5, jeśli pójdę importem |
| **W-31** | `docker cp` kładzie plik z prawami 640, Studio go nie odczyta — `chmod 644` po każdym wgraniu | j.w. |
| **W-34** | `import_domain_configuration` **po cichu gubi** przypięcie do regionu — po każdym imporcie ponowić `attach_to_region` i sprawdzić SQL-em | G3, G5 |
| **NOWA** | Pytanie bez `CONTEXT_PATH` i `VALUE_TYPE` nie dojdzie do silnika. `VALUE_TYPE` jest osobny od `KIND` — konwersja idzie po nim | G3, G5 |
| **NOWA** | `ctx` musi być **pierwszym** zadeklarowanym argumentem funkcji, inaczej „Invalid argument data type passed on position 0" | G3, G5 |
| **NOWA** | `@SessionScope` nie jest bezpieczny wątkowo — seria mutacji idzie po kolei, nie przez `Promise.all` | G4, G6 |

---

## 5. Granice zakresu

- **Nie ruszam istniejących reguł MOTO ani LIFE.** Wyłącznie dopisywanie pod prefiksem
  `demo.moto.apk.*` / `demo.life.apk.*`, zero `delete_*` poza własnym prefiksem, zero zmian
  w regionach `MOTOR` i `LIFE`.
- **Nie tłumaczę MOTO na polski** — ND-2 i goldeny parity, decyzja z 2026-08-21. Sekcja APK jest
  nowa, więc jej treść jest polska; istniejący ekran zostaje po angielsku. ⚠️ To da ekran mieszany
  językowo i **trzeba to zgłosić jako otwartą kwestię**, a nie przemilczeć.
- **Nie dotykam taryf** — APK odpowiada na „czego klient potrzebuje", nie na „ile to kosztuje".
- **Nie buduję APK dla ubezpieczeń z UFK** (B-4 researchu).
