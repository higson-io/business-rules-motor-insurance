/*
#Full code: demo.property.apk.sumVsComputed

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "NIZSZA, ROWNA albo WYZSZA. Obie sumy sa w kontekscie - wybrana przez klienta i wyliczona z metrazu i stawki - a funkcja mowi tylko, ktora jest wieksza. Tabela nie porowna dwoch swoich kolumn ze soba, wiec porownanie musi przyjsc jako jedna wartosc."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def chosen = ctx.getDecimal('quote.sums.building')
def computed = ctx.getDecimal('quote.sums.suggestedBuilding')
if (chosen == null || computed == null || computed.signum() == 0) {
	return null
}

def direction = chosen.compareTo(computed)
if (direction < 0) {
	return 'NIZSZA'
}
return direction > 0 ? 'WYZSZA' : 'ROWNA'