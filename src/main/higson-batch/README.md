# Paczka batch testera — składka pakietu majątkowego

`paczka-skladka-pakietu.xlsx` to plik paczki Higson Batch Testera z dziesięcioma przypadkami
taryfikacji flow `demo.property.calculateQuote`. Wczytuje się w Studio: **Tools → Batch tester →
Load test package**.

⛔ **To NIE jest źródło prawdy.** Same przypadki testowe żyją w snapshocie, w
`src/main/higson/@tests/*.json`, i to je wersjonujemy. Ten plik jest z nich odtwarzalny: w oknie
paczki dodaje się dziesięć przypadków i klika **Download package**. Leży tu dlatego, że batch tester
przyjmuje plik, a nie listę nazw — więc jedno kliknięcie zastępuje dziesięć.

Co pilnuje każdy przypadek, opisuje `.maister/docs/project/higson-property-profile.md` §34.
