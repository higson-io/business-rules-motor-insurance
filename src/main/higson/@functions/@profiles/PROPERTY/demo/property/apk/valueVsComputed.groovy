/*
#Full code: demo.property.apk.valueVsComputed

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "NIZSZE, ZGODNE albo WYZSZE - rek. 8.2.c. Oszacowanie to odpowiedz z ankiety, suma wyliczona jest w kontekscie; funkcja mowi tylko, ktore jest wieksze. Brak odpowiedzi daje null i wtedy zadne zdanie o wartosci nie pada."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def declared = ctx.getDecimal('quote.needs.propertyValue')
def computed = ctx.getDecimal('quote.sums.suggestedBuilding')
if (declared == null || declared.signum() == 0 || computed == null || computed.signum() == 0) {
	return null
}

def direction = declared.compareTo(computed)
if (direction < 0) {
	return 'NIZSZE'
}
return direction > 0 ? 'WYZSZE' : 'ZGODNE'