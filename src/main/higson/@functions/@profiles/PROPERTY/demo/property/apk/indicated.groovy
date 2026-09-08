/*
#Full code: demo.property.apk.indicated

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Serce rekomendacji: najtanszy pakiet, ktory ma wszystkie wymagane pokrycia. Sklad pakietu liczy przez demo.property.apk.packageCovers, czyli TAK SAMO jak Java przy ocenie pakietu - pokrycie niedostepne w tej sytuacji nie liczy sie jako zawarte. Gdy zaden pakiet nie pokrywa wszystkiego, wygrywa ten o najwiekszym pokryciu, przy remisie tanszy."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def required = higson.call('demo.property.apk.covers', ctx)
if (required.isEmpty()) {
	return null
}

// Drabinka pakietow prosto z drzewa, rosnaco po pozycji. Pozycja jest scisla: wyzsza znaczy
// szerszy zakres, wiec pierwszy pakiet pokrywajacy wszystko jest zarazem najtanszym takim.
def zone = ctx.getString('quote.zone.flood')
def ladder = []
for (def variant in domain.get('PROPERTY', '/PRODUCTS[HOME]').getAll('VARIANTS')) {
	def code = variant.getCode()

	def availability = higson.getValue('demo.property.variant.availability',
			higson.createContext('variant.code', code, 'quote.zone.flood', zone))
	if (!Boolean.TRUE.equals(availability.get(0, 'available'))) {
		continue
	}

	ladder.add([code: code,
	            position: variant.getAttrDecimal('POSITION', ctx),
	            covers: higson.call('demo.property.apk.packageCovers', ctx, code)])
}
if (ladder.isEmpty()) {
	return null
}
ladder.sort { a, b -> a.position <=> b.position }

def best = null
def bestCovered = -1
for (def candidate in ladder) {
	def covered = 0
	for (def cover in required) {
		if (candidate.covers.contains(cover)) {
			covered = covered + 1
		}
	}
	if (covered > bestCovered) {
		best = candidate.code
		bestCovered = covered
		if (covered == required.size()) {
			return best
		}
	}
}
return best