/*
#Full code: demo.property.apk.indicated

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Serce rekomendacji: najtanszy pakiet, ktory ma wszystkie wymagane pokrycia - sklad pakietow czyta z drzewa, a miejsce w drabince z ATRYBUTU pakietu. Gdy zaden nie pokrywa wszystkiego, wygrywa pakiet o najwiekszym pokryciu, a przy remisie tanszy; luki nie zamiatamy, nazywa je uzasadnienie. Nie zna ANI JEDNEGO kodu pakietu i ani jednego kodu pytania."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

// Potrzeby liczy apk.needs i tylko ona. Wczesniej ta funkcja przechodzila po odpowiedziach
// sama, wiec ta sama regula stala w dwoch miejscach i mogla sie rozjechac.
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

	// Pakiet niedostepny w tej strefie nie jest propozycja.
	def availability = higson.getValue('demo.property.variant.availability',
			higson.createContext('variant.code', code, 'quote.zone.flood', zone))
	if (!Boolean.TRUE.equals(availability.get(0, 'available'))) {
		continue
	}

	def covers = new HashSet()
	for (def member in variant.getAll('COVERS')) {
		covers.add(member.getCode())
	}
	ladder.add([code: code,
	            position: variant.getAttrDecimal('POSITION', ctx),
	            covers: covers])
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