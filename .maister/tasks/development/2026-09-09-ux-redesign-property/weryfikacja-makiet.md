# Weryfikacja z makietami — czy uwzględniliśmy uwagi UX-a

**Data:** 2026-09-09 · **Podstawa:** `makiety-ux.pdf` (strony 5–11 teczki `Demo Higson.pdf`)
**Sprawdzone na:** zbudowanym WAR-ze pod `http://localhost:48080/demo/`, nie na serwerze
deweloperskim.

⛔ Wiążąca jest iteracja ze **stron 6 i 7**. Strona 5 to wcześniejsza wersja tego samego ekranu
(inne podpisy pokryć, zielony tint kolumny, badge „REKOMENDOWANY" zamiast „PROPONOWANY"), strony
3 i 4 są identyczne.

## 1. Chrom wspólny dla wszystkich ekranów

| Element makiety | Stan | Uwaga |
|---|---|---|
| Pasek „← Wszystkie dema" / „Higson Demos" | ✅ | było wcześniej, bez zmian |
| Tło strony `#F3F5F9` | ✅ | zmierzone z makiety |
| Całość w jednej białej karcie z zaokrągleniem | ✅ | |
| Tytuł ciężki, ~30 px | ✅ | 1.875 rem / 700 |
| Karta klienta: awatar w tincie, cztery kolumny, etykiety wersalikami | ✅ | |
| Kontakt w dwóch liniach (e-mail nad telefonem) | ✅ | |
| Zakładki z numeracją, podkreślenie aktywnej, ✓ przy zamkniętych | ✅ | wariant `plain` paska kroków |
| Kolor akcji `#2AA399` | ✅ | D-1, odstępstwo od kontrastu zapisane |

## 2. Krok 1 — analiza potrzeb (str. 2, 3, 4)

| Element makiety | Stan | Uwaga |
|---|---|---|
| Dwie kolumny: spis pytań po lewej, pytanie po prawej | ✅ | |
| „PYTANIA W TYM KROKU" + numerowana lista z podświetleniem bieżącego | ✅ | |
| Krótkie nazwy pytań w spisie | ✅ | z reguł (`shortLabel`), nie z kodu |
| Licznik „Pytanie N z M" | ✅ | |
| Kreska pionowa między kolumnami | ✅ | |
| Treść pytania jako nagłówek, Tak/Nie jako dwa przyciski | ✅ | |
| „← Poprzednie pytanie" / „Następne pytanie →" | ✅ | |
| **Pytanie z kilkoma polami** (str. 3–4) | ✅ | zrealizowane jako pytanie złożone, D-3 |
| Gwiazdka wymagalności | ⚠️ **odstępstwo** | makieta jej nie ma; zostawiona, bo usunięcie oznaczenia pola wymaganego to regres dostępności, nie uproszczenie |
| Etykiety spisu z makiety | ⚠️ **odrzucone** | „Lokalizacja", „Historia szkód", „Data rozpoczęcia ochrony" opisują **przedmiot ubezpieczenia**, nie potrzeby — patrz §3 niżej |

## 3. Krok 3 — rekomendacja i składka (str. 6)

| Element makiety | Stan | Uwaga |
|---|---|---|
| Turkusowy nagłówek „Czym różnią się warianty" | ✅ | |
| Tabela: Zakres / trzy warianty | ✅ | |
| Badge „PROPONOWANY" pod nazwą wariantu | ✅ | |
| Nazwy wariantów w jednym rzędzie mimo dwuwierszowego nagłówka | ✅ | poprawione w przeglądzie — patrz §6 |
| Podpisy limitów pod nazwą pokrycia | ✅ | `capLabel` (D-4) |
| ✓ / — w kolorze akcji | ✅ | |
| Tint na kolumnie proponowanej | ✅ | + obrys na wybranej, gdy różna (D-6) |
| Cena i „Wybieram" w jednej komórce, przycisk wypełniony w kolumnie proponowanej | ✅ | |
| Podsumowanie: wielka suma murów + podpis skąd | ✅ | |
| Kafelek „Wariant X (proponowany)" z opisem | ✅ | |
| **„W cenie" zamiast kwot w wierszach podsumowania** | ⛔ **odrzucone** | D-5 — te liczby wylicza silnik reguł i to jest to, co demo ma pokazywać |
| Blok „Proponujemy wariant X" + „Pokrywa N z M" | ✅ | |
| Alternatywa w oczekiwanej składce, drobniej | ✅ | |
| „SKŁADKA" + wielka kwota po prawej | ✅ | |
| Dwie kolumny: potrzeby (✓) i uzasadnienie (numeracja turkusowa) | ✅ | |
| Stopka nawigacji | ✅ | |

## 4. Krok 4 — oferta (str. 7)

| Element makiety | Stan | Uwaga |
|---|---|---|
| „OFERTA" + numer, daty po prawej | ✅ | etykieta szara, nie turkusowa |
| Sekcja „Ubezpieczający" + siatka danych + „Dane pobrane z:" | ✅ | |
| „Zakres ochrony — wariant X" + kwoty + franszyza | ✅ | |
| „SKŁADKA ZA OKRES OCHRONY" + wielka kwota po prawej | ✅ | |
| Bursztynowy blok „Wybór węższy niż rekomendacja" z ⚠ i ⊗ | ✅ | |
| **Chevron przy załączniku (zwijanie)** | ✅ | dorobione 2026-09-09; `<details open>` — semantyka przeglądarki, domyślnie rozwinięty, bo rek. 8.7 każe załącznik **dołączyć**, nie schować |
| Oświadczenie + „Akceptuję i wystawiam polisę →" | ✅ | |

## 5. Czego makieta nie pokrywała

- **Krok 2 „Przedmiot ubezpieczenia"** — ani jednej strony. Zrobiony przez analogię: trzy kolumny,
  wyrównanie do lewej, bez podsumowania obok (decyzja właściciela 2026-09-09).
- **Stany brzegowe** — wariant niedostępny, pytanie blokujące, błąd ładowania, pusta rekomendacja.
  Sprowadzone do jednej bursztynowej formy.
- **Widok mobilny** — wszystkie artboardy są desktopowe (1920 px).

## 6. Bilans

**Zrealizowane co do joty:** cały chrom, krok 1, krok 3, krok 4 — z jednym wyjątkiem, który
dorobiono podczas tej weryfikacji (chevron).

**Trzy świadome odstępstwa**, każde zapisane z uzasadnieniem:

1. ⛔ **„W cenie" odrzucone** (D-5) — kwoty zostają.
2. ⚠️ **Gwiazdka wymagalności zostaje**, choć makieta jej nie rysuje.
3. ⚠️ **Etykiety spisu napisane od nowa** — te z makiety opisują krok 2, nie krok 1. Jedyna, która
   dała się przenieść tam, gdzie pasuje, to „Lokalizacja" (→ `POSTCODE`); „Rodzaj nieruchomości"
   i „Historia szkód" i tak już tam były.

**Jedno odstępstwo w drugą stronę:** kolor `#2AA399` wzięty z makiety **wbrew** progowi kontrastu
z `accessibility.md` (D-1) — na wyraźną decyzję właściciela, zapisane w standardzie.
