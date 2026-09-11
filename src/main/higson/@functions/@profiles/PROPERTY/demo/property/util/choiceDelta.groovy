/*
#Full code: demo.property.util.choiceDelta

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Util do zrodla poziomu tabeli demo.property.offer.choiceKind. Zwraca ROZNICE LICZBY POKRYC miedzy pakietem wybranym a rekomendowanym: ujemna gdy klient wzial wezszy zakres, zero gdy ten sam, dodatnia gdy szerszy. Do 11 wrzesnia liczyla roznice POZYCJI w drabince - a pozycja to umowa o kolejnosci na ekranie, nie miara zakresu: przestawienie jej zmienialo notatke w ofercie, nie zmieniajac ochrony. Null, gdy ktorejkolwiek strony jeszcze nie ma. Samo odejmowanie - co ta roznica ZNACZY, mowi tabela."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def recommended = ctx.getString('quote.choice.recommended')
def chosen = ctx.getString('quote.choice.selected')
if (recommended == null || chosen == null) {
	return null
}

// ⛔ Liczba pokryc PAKIETU, a nie liczba pokryc spelniajacych potrzebe. Rekomendacja jest
// z definicji najtanszym pakietem pokrywajacym WSZYSTKIE potrzeby, wiec pakiet szerszy spelnia
// ich dokladnie tyle samo - po tej drugiej mierze roznica zawsze wychodzilaby zerowa i przypadek
// RICHER nie moglby nigdy paść, choc to wlasnie on opisuje zakres wykraczajacy poza analize.
def coverCount = { code ->
	domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + code + ']').getAll('COVERS').size()
}

return coverCount(chosen) - coverCount(recommended)