/*
#Full code: demo.property.zone.resolve

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Strefa powodziowa i kradziezowa dla adresu. Klucz stref - dwie pierwsze cyfry kodu pocztowego - bierze z gotowej sciezki quote.property.postcodeArea, a nie liczy go po raz drugi z samego kodu; wczesniej te sama regule mialy Java i ta funkcja, i rozjezdzaly sie na brzegu."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def result = type.paramValue()
		.withCodes('flood', 'theft', 'theftFactor')
		.withTypes('string', 'string', 'number')

// ⛔ Klucz stref czytamy GOTOWY. Do 7 wrzesnia 2026 stalo tu wlasne wyliczenie
// postcode.replace('-','').substring(0,2), czyli ta sama regula co w PropertyContext.postcodeArea
// - dwie implementacje, ktore juz sie roznily na brzegu: Java przy krotkim kodzie zwracala null
// (tabele spadaly na wiersz domyslny), a tu wracalo twarde 'Z0','T1',1. Jedno zrodlo, jeden wynik.
def area = ctx.getString('quote.property.postcodeArea')
if (area == null || area.isEmpty()) {
	return result.addValues('Z0', 'T1', 1).create()
}

def areaCtx = higson.createContext('quote.property.postcodeArea', area)
def flood = higson.getString('demo.property.zone.flood', areaCtx)
def theftRow = higson.getValue('demo.property.zone.theft', areaCtx)

// OWU Generali par. 21 ust. 2: mieszkanie powyzej parteru jest uznawane za polozone w strefie 0
// niezaleznie od adresu. To jest regula o INNYM polu niz klucz tabeli, wiec nie da sie jej
// wyrazic wierszem - i dlatego ta funkcja istnieje.
if ('FLAT' == ctx.getString('quote.property.kind')) {
	def floor = ctx.getInteger('quote.property.floor')
	if (floor != null && floor > 0) {
		flood = 'Z0'
	}
}

return result.addValues(flood, theftRow.get(0, 'zone'), theftRow.get(0, 'factor')).create()