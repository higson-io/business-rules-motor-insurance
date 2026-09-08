/*
#Full code: demo.property.offer.choiceKind

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Rek. 9.3 - w ofercie ma stac, czy klient poszedl za rekomendacja, czy od niej odszedl i w ktora strone. Miejsce w drabince czyta z ATRYBUTU pakietu, a nie z tabeli kluczowanej kodem pakietu - taka tabela byla indirekcja, bo wartosc nalezy do elementu."

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
if (recommended == chosen) {
	return 'MATCHES'
}

// Pozycja w drabince jest WARTOSCIA na pakiecie. Drabinka jest scisla: wyzsza pozycja znaczy
// szerszy zakres, wiec porownanie pozycji odpowiada na pytanie "szerzej czy wezej".
def recommendedPosition = domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + recommended + ']')
		.getAttrDecimal('POSITION', ctx)
def chosenPosition = domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + chosen + ']')
		.getAttrDecimal('POSITION', ctx)

if (recommendedPosition == null || chosenPosition == null) {
	return null
}
return chosenPosition > recommendedPosition ? 'RICHER' : 'POORER'