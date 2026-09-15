/*
#Full code: demo.property.util.budgetRatio

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Zrodlo poziomu tabeli demo.property.apk.budget ($f). Wylacznie dzielenie: skladka proponowanego pakietu przez skladke oczekiwana przez klienta. Co ten stosunek znaczy i gdzie stoi prog, mowi tabela. Pakietu nie dostaje argumentem: czyta quote.apk.proposed, ktore wstawia do kontekstu argument wezla tabeli we flow demo.property.apk.recommend (w chwili wywolania pakiet wybral dopiero poprzedni wezel)."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def budget = ctx.getDecimal('quote.needs.budget')
def proposed = ctx.getString('quote.apk.proposed')
def premium = proposed == null ? null : ctx.getDecimal('quote.premium.' + proposed)
if (budget == null || budget <= 0 || premium == null) {
	// ⛔ Zero, nie null. Otwarty koniec zakresu w tabeli dopasowuje NULL i wygrywa z wierszem
	// gwiazdkowym (W-30), wiec brak oczekiwanej skladki wychodzilby jako BUDGET_FIT. Zero laduje
	// w wierszu "* - 0" i znaczy "nie ma czego porownywac".
	return 0
}
return premium / budget