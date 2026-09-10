/*
#Full code: demo.property.util.budgetRatio

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Wylacznie dzielenie: skladka proponowanego pakietu przez skladke oczekiwana przez klienta. Co ten stosunek znaczy i gdzie stoi prog, mowi tabela demo.property.apk.budget, ktora we flow stoi obok jako osobny wezel. Pakiet dostaje ARGUMENTEM, bo w chwili wywolania nie ma go jeszcze w kontekscie."

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
if (budget == null || budget <= 0 || premium == null) {
	// ⛔ Zero, nie null. Otwarty koniec zakresu w tabeli dopasowuje NULL i wygrywa z wierszem
	// gwiazdkowym (W-30), wiec brak oczekiwanej skladki wychodzilby jako BUDGET_FIT. Zero laduje
	// w wierszu "* - 0" i znaczy "nie ma czego porownywac".
	return 0
}
return premium / budget