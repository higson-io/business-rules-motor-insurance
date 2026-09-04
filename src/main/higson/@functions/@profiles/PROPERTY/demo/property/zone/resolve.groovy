/*
#Full code: demo.property.zone.resolve

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Dwie strefy z jednego adresu. Musi byc funkcja, bo regula \\"mieszkanie powyzej parteru to strefa 0\\" czyta INNE pole niz klucz tabeli."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def result = type.paramValue()
		.withCodes('flood', 'theft', 'theftFactor')
		.withTypes('string', 'string', 'number')

def postcode = ctx.getString('quote.property.postcode')
if (postcode == null || postcode.replace('-', '').length() < 2) {
	return result.addValues('Z0', 'T1', 1).create()
}

def area = postcode.replace('-', '').substring(0, 2)
def areaCtx = higson.createContext('quote.property.postcodeArea', area)
def flood = higson.getString('demo.property.zone.flood', areaCtx)
def theftRow = higson.getValue('demo.property.zone.theft', areaCtx)

// OWU Generali par. 21 ust. 2: mieszkanie powyzej parteru jest uznawane za polozone w strefie 0
// niezaleznie od adresu. To jest regula o INNYM polu niz klucz tabeli, wiec nie da sie jej
// wyrazic wierszem - i dlatego ta funkcja istnieje.
def kind = ctx.getString('quote.property.kind')
def floor = ctx.getInteger('quote.property.floor')
if ('FLAT' == kind && floor != null && floor >= 1) {
	flood = 'Z0'
}

return result.addValues(flood, theftRow.get(0, 'zone'), theftRow.get(0, 'factor')).create()