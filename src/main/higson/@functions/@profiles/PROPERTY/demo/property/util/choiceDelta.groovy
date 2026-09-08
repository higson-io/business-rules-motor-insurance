/*
#Full code: demo.property.util.choiceDelta

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Util do zrodla poziomu tabeli demo.property.offer.choiceKind. Zwraca ROZNICE pozycji w drabince pakietow: ujemna gdy klient wybral wezszy pakiet niz rekomendowany, zero gdy ten sam, dodatnia gdy szerszy. Null, gdy ktorejkolwiek strony jeszcze nie ma. Sama odejmowanie - co ta roznica ZNACZY, mowi tabela."

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

def positionOf = { code ->
	domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + code + ']').getAttrDecimal('POSITION', ctx)
}
def recommendedPosition = positionOf(recommended)
def chosenPosition = positionOf(chosen)
if (recommendedPosition == null || chosenPosition == null) {
	return null
}
return chosenPosition - recommendedPosition