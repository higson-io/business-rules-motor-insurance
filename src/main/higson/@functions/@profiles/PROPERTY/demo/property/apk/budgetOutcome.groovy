/*
#Full code: demo.property.apk.budgetOutcome

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "⛔ OCZEKIWANA SKLADKA NIE JEST FILTREM ZAKRESU (rek. 8.2.d). Ta funkcja NIE wybiera pakietu - ona tylko nazywa, czy propozycja miesci sie w oczekiwaniu klienta. Sama robi WYLACZNIE dzielenie; co wynik znaczy i gdzie jest prog, mowi tabela demo.property.apk.budget. Pakiet dostaje ARGUMENTEM, bo w chwili wywolania nie ma go jeszcze w kontekscie."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "indicated"
type = "STRING"
*/

def budget = ctx.getDecimal('quote.needs.budget')
def premium = indicated == null ? null : ctx.getDecimal('quote.premium.' + indicated)
def comparable = budget != null && budget > 0 && premium != null

// Zero, nie null: otwarty koniec zakresu w tabeli dopasowuje null i wygrywa z wierszem gwiazdkowym.
def ratio = comparable ? premium / budget : 0

def ratioCtx = higson.createContext('quote.needs.budgetRatio', ratio)
return higson.getString('demo.property.apk.budget', ratioCtx)