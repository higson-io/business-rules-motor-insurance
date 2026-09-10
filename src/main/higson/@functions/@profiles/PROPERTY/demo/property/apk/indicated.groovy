/*
#Full code: demo.property.apk.indicated

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Serce rekomendacji: najlepiej dopasowany pakiet sposrod tych, ktore wolno sprzedac, a przy remisie tanszy. Zero jako gorne ograniczenie ceny znaczy BRAK ograniczenia - oczekiwana skladka NIE zawezaja zakresu (rek. 8.2.d), tylko nazywa propozycje jako miesczaca sie albo nie. Przegladanie drabinki robi demo.property.apk.bestCovering, wspolne z alternatywa."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

return higson.call('demo.property.apk.bestCovering', ctx, 0)