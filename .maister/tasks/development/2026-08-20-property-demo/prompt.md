# Prompt uruchomieniowy — demo majątkowe z APK w Higsonie

Do wklejenia w sesję Claude uruchomioną z `--dangerously-skip-permissions`, albo do wskazania
zdaniem: „Przeczytaj `.maister/tasks/development/2026-08-20-property-demo/prompt.md` i wykonaj to”.

---

Jesteś na gałęzi `feature/property-demo` w repozytorium business-rules-motor-insurance.
Zadanie: zbudować trzecie demo tej aplikacji — ubezpieczenie domu i mieszkania (detal) — kompletnie,
od reguł w Higsonie po ekrany, wraz z analizą potrzeb klienta (APK) skonfigurowaną w Higsonie.

## Zanim cokolwiek napiszesz

Przeczytaj w tej kolejności:

1. `CLAUDE.md` i `.maister/docs/INDEX.md` — standardy obowiązujące w tym repo
2. `.maister/tasks/development/2026-08-20-property-demo/brief.md` — pełne zamówienie, zwłaszcza
   §5 (motyw przewodni) i §6 (zasady pracy bez nadzoru)
3. `.maister/docs/project/higson-life-profile.md` — wzorzec dokumentu projektowego
4. `.maister/tasks/development/2026-08-13-higson-life-implementation/implementation-plan.md` —
   §9 i §10 to komplet pułapek API Higsona (W-15…W-27). Nie odkrywaj ich drugi raz.
5. Kod demo LIFE jako wzorzec integracji: `src/main/java/pl/decerto/higson/demo/life/` oraz
   `frontend/src/app/{core/life-store.ts, services/life-service.ts, models/life.ts, demos/life/}`

## Co ma powstać

### A. Research rynku

`.maister/tasks/development/2026-08-20-property-demo/analysis/market-research.md`, z przypisami do
źródeł (URL + data dostępu). Minimum pięć OWU polskich towarzystw przeczytanych **w oryginale**, nie
streszczeń z porównywarek: zakresy, definicje granicy mury / elementy stałe, sumy, franszyzy, limity
na gotówkę i przedmioty wartościowe, wyłączenia, czynniki taryfowe. Czego nie da się pobrać — zapisz
jako brak, **nie zmyślaj liczb**.

### B. Dokument projektowy

`.maister/docs/project/higson-property-profile.md`, po polsku, w konwencji dokumentu LIFE: drzewo
domeny, typy domenowe, konteksty wejściowe, tabele decyzyjne z pełnymi macierzami, flow, funkcje
Groovy, wersjonowanie regionem, testy regułowe, decyzje **D-n** z uzasadnieniem, kwestie otwarte
**W-n**, budżet złożoności (funkcja ≤ 80 linii, tabela ≤ 30 wierszy, flow ≤ 15 nodów).

### C. Konfiguracja w Higson Studio

Profil `PROPERTY`, wszystkie kody z prefiksem `demo.property.*`. Kolejność wymuszona zależnościami:
konteksty → tabele → funkcje → flow → typy domenowe → drzewo domeny → region i wersje → publikacja.

**APK ma być konfiguracją, nie kodem.** W Higsonie siedzą: katalog pytań (treść, kolejność, typ,
obowiązkowość) jako elementy domeny z atrybutami, widoczność pytania zależna od wcześniejszych
odpowiedzi, mapowanie odpowiedzi na potrzeby klienta, wyliczenie rekomendowanej sumy ubezpieczenia,
konfrontacja wyłączeń z sytuacją klienta, dobór wariantu do potrzeb i budżetu oraz treść uzasadnienia
propozycji. **Java nie zna ani jednego pytania i ani jednego progu.**

**Region `PROPERTY` ma dwie wersje:**

| Wersja | Od | Zawartość |
|---|---|---|
| 1 | `2026-01-01` | APK w dzisiejszym kształcie, wg Wytycznych KNF z 2014 |
| 2 | `2027-07-01` | APK wg Rekomendacji z uchwały KNF 190/2026 — rekomendacje 8 i 9: okoliczności i okres ochrony, wyliczana suma, pytanie o akceptowalną składkę, ostrzeżenia o wyłączeniach kolidujących z sytuacją klienta, uzasadnienie propozycji, jawne wskazanie różnic między wariantami |

Data `2027-07-01` to termin dostosowania z §2 tej uchwały — pełny tekst leży obok briefu
w `knf-rekomendacje-190-2026.txt`.

### D. Backend

Pakiet `pl.decerto.higson.demo.property` w konwencji `demo.life` — `api` / `dto` / `converter` /
`service` / `domain` / `adapter` / `context` plus orkiestrator. Data polisy idzie do
`engine.setEffectiveDate`, bo to ona wybiera wersję APK. **Katalog pytań czytaj przy KAŻDEJ wycenie,
po `setEffectiveDate`, nigdy raz przy tworzeniu sesji** — na tym przewróciło się demo LIFE (W-27).
Dopisz pakiet do `@ComponentScan` w `ApplicationConfig`.

### E. Front

Wpis w `frontend/src/app/demos/demo-catalog.ts`, gałąź w `@switch` w `app.html`, komponenty
w `frontend/src/app/demos/property/`, store wzorowany na `LifeStore` (`resource()` + `reload()`,
**nigdy** przeładowanie przez zmianę `params`, bo wtedy ekran miga). Przepływ ekranów: ankieta APK →
rekomendacja z uzasadnieniem → porównanie wariantów → podsumowanie. Podsumowanie w przyklejonej
kolumnie po prawej, jak w demo LIFE. Data początku ochrony jako kontrolka nad ankietą, bo to ona
przełącza wersję APK.

## Weryfikacja — bez tego zadanie nie jest skończone

Zbuduj i uruchom aplikację, wejdź w nią przeglądarką przez Playwright MCP i sprawdź, że:

- ankieta APK renderuje się z pytań pochodzących z Higsona,
- **przesunięcie daty polisy przez `2027-07-01` widocznie zmienia zestaw pytań i treść rekomendacji**,
- rekomendacja pokazuje uzasadnienie i różnice między wariantami,
- ostrzeżenie o wyłączeniu pojawia się dla sytuacji klienta, która je wywołuje.

Uruchomienie:

```bash
PATH=/opt/homebrew/opt/node@22/bin:$PATH mvn -o package -Pall_in_one -DskipTests
HIGSON_DATABASE_URL='jdbc:h2:tcp://localhost:9192/higson.demo;NON_KEYWORDS=VALUE' \
HIGSON_DATABASE_USERNAME=sa HIGSON_DATABASE_PASSWORD=sa \
java -jar target/motor-insurance.war        # http://localhost:48080/demo
```

Przy zmianach wyłącznie w Javie dodaj `-Dexec.skip=true`, żeby pominąć przebudowę frontu.
Na koniec obie suity mają być zielone: `ng test --watch=false` w `frontend/` oraz `mvn -o test`.

## Zasady

Pracujesz bez nadzoru — nie zadawaj pytań, przy wątpliwości przyjmij założenie, oznacz je jako
**W-n** i pracuj dalej.

⛔ Nie dotykaj reguł MOTO ani DEMO: `assign_to_profile` wyłącznie z kodami `demo.property.*`
i **nigdy** z parametrem `currentProfileCode`, żadnych `delete_*` poza własnym prefiksem.
Przed publikacją w Studio zrób backup bazy (`backup to '/h2-data/pre-property.zip'` + `docker cp`)
i sprawdź, że sesja robocza zawiera wyłącznie elementy `demo.property.*`. **Nie pushuj.**

Commituj po każdym ukończonym dokumencie i po każdej grupie zadań, opisami mówiącymi **dlaczego**,
nie tylko co. Nie zostawiaj martwego kodu ani plików tymczasowych w `src/`.

Na koniec zapisz `.maister/tasks/development/2026-08-20-property-demo/run-log.md`: co zrobione,
jakie założenia przyjęte, co wymaga mojej decyzji, czego nie udało się ustalić i dlaczego.
