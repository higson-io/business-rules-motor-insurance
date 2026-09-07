/*
#Full code: demo.property.apk.indicated

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Serce rekomendacji. Z odpowiedzi wyprowadza wymagane pokrycia (tabela apk.answer), a potem szuka NAJTANSZEGO pakietu, ktory je wszystkie ma - sklad pakietow czyta z drzewa. Gdy zaden nie pokrywa wszystkiego, wygrywa pakiet o najwiekszym pokryciu, a przy remisie tanszy; luki nie zamiatamy, nazywa je uzasadnienie. Funkcja nie zna ANI JEDNEGO kodu pakietu i ani jednego kodu pytania - dolozenie czwartego pakietu to jeden element w drzewie."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def answers = ctx.getString('quote.apk.answers')
if (answers == null || answers.isEmpty()) {
	return null
}

// 1. Czego wymagaja odpowiedzi. Tabela mowi, jakie POKRYCIE zaspokaja dana odpowiedz.
def required = new HashSet()
for (def pair in answers.split(';')) {
	def sep = pair.indexOf('=')
	if (sep < 1) {
		continue
	}
	def row = higson.getValue('demo.property.apk.answer', higson.createContext(
			'question.code', pair.substring(0, sep),
			'answer.value', pair.substring(sep + 1)))
	def cover = row.get(0, 'cover')
	if (cover != null && !cover.toString().isEmpty()) {
		required.add(cover.toString())
	}
}
if (required.isEmpty()) {
	return null
}

// 2. Drabinka pakietow prosto z drzewa, rosnaco po pozycji. Pozycja jest scisla: wyzsza znaczy
// szerszy zakres, wiec pierwszy pakiet pokrywajacy wszystko jest zarazem najtanszym takim.
def zone = ctx.getString('quote.zone.flood')
def ladder = []
for (def variant in domain.get('PROPERTY', '/PRODUCTS[HOME]').getAll('VARIANTS')) {
	def code = variant.getCode()

	// Pakiet niedostepny w tej strefie nie jest propozycja - milczenie tez nia nie jest,
	// dlatego nizej jest fallback na najubozszy dostepny.
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
	            position: higson.getDecimal('demo.property.variant.position',
			            higson.createContext('variant.code', code)),
	            covers: covers])
}
if (ladder.isEmpty()) {
	return null
}
ladder.sort { a, b -> a.position <=> b.position }

// 3. Najtanszy pakiet pokrywajacy WSZYSTKIE potrzeby; gdy takiego nie ma - najwieksze pokrycie.
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