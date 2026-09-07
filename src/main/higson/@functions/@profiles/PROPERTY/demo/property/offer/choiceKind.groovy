/*
#Full code: demo.property.offer.choiceKind

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Rek. 9.3 - w ofercie ma stac, czy klient poszedl za rekomendacja, czy od niej odszedl i w ktora strone. Do 7 wrzesnia 2026 rozstrzygala to Java, porownujac pozycje wariantow. Wynik kluczuje tabele demo.property.offer.choice, ktora niesie tresc notatki."

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

// Pozycja w drabince, nie kolejnosc alfabetyczna kodow. Drabinka jest scisla: wyzsza pozycja
// znaczy szerszy zakres, wiec porownanie pozycji odpowiada na pytanie "szerzej czy wezej".
def recommendedPosition = higson.getDecimal('demo.property.variant.position',
		higson.createContext('variant.code', recommended))
def chosenPosition = higson.getDecimal('demo.property.variant.position',
		higson.createContext('variant.code', chosen))

if (recommendedPosition == null || chosenPosition == null) {
	return null
}
return chosenPosition > recommendedPosition ? 'RICHER' : 'POORER'