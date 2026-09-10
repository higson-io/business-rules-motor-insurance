/*
#Full code: demo.property.apk.indicated

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Serce rekomendacji: najtanszy pakiet, ktory ma wszystkie wymagane pokrycia. Sklad pakietu liczy przez demo.property.apk.packageCovers, czyli TAK SAMO jak Java przy ocenie pakietu. Gdy zaden pakiet nie pokrywa wszystkiego, wygrywa ten o najwiekszym pokryciu, przy remisie tanszy."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def required = higson.call('demo.property.apk.covers', ctx)
if (required.isEmpty()) {
	return null
}

def floodZone = ctx.getString('quote.zone.flood')
def security = ctx.getString('quote.property.security')
def product = domain.get('PROPERTY', '/PRODUCTS[HOME]')
def variants = product.getAll('VARIANTS')

def ladder = []
for (def variant in variants) {
	def code = variant.getCode()
	// ⛔ Waski kontekst musi niesc KAZDE pole, po ktorym kluczuje sie dostepnosc pokrycia:
	// strefe dla powodzi i zabezpieczenia dla kradziezy. Bez ktoregokolwiek tabela pokrycia
	// spada na wiersz gwiazdkowy i pakiet wychodzi dostepny, choc ekran pokaze go inaczej.
	def availabilityCtx = higson.createContext(
			'variant.code', code,
			'quote.zone.flood', floodZone,
			'quote.property.security', security)
	def available = higson.call('demo.property.variant.available', availabilityCtx)
	if (!Boolean.TRUE.equals(available)) {
		continue
	}
	def position = variant.getAttrDecimal('POSITION', ctx)
	def covers = higson.call('demo.property.apk.packageCovers', ctx, code)
	ladder.add([code: code, position: position, covers: covers])
}
if (ladder.isEmpty()) {
	return null
}

// Position rosnie z zakresem pakietu, wiec pierwszy pelny match po posortowaniu jest najtanszy.
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