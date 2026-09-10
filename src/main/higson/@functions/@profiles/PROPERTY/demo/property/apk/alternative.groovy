/*
#Full code: demo.property.apk.alternative

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Gdy propozycja przekracza oczekiwana skladke, tanszy pakiet NIE zastepuje jej, tylko jedzie obok jako alternatywa. Wybieramy najlepiej dopasowany sposrod mieszczacych sie w budzecie, przy remisie tanszy. Sklad pakietu liczy przez demo.property.apk.packageCovers, tak samo jak apk.indicated i jak Java."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def budget = ctx.getDecimal('quote.needs.budget')
if (budget == null || budget <= 0) {
	return null
}

def required = higson.call('demo.property.apk.covers', ctx)
def floodZone = ctx.getString('quote.zone.flood')
def security = ctx.getString('quote.property.security')
def product = domain.get('PROPERTY', '/PRODUCTS[HOME]')
def variants = product.getAll('VARIANTS')

def best = null
def bestCovered = -1
def bestPosition = null
for (def variant in variants) {
	def code = variant.getCode()
	// ⛔ Waski kontekst musi niesc KAZDE pole, po ktorym kluczuje sie dostepnosc pokrycia:
	// strefe dla powodzi i zabezpieczenia dla kradziezy.
	def availabilityCtx = higson.createContext(
			'variant.code', code,
			'quote.zone.flood', floodZone,
			'quote.property.security', security)
	def available = higson.call('demo.property.variant.available', availabilityCtx)
	if (!Boolean.TRUE.equals(available)) {
		continue
	}

	def premium = ctx.getDecimal('quote.premium.' + code)
	if (premium == null || premium > budget) {
		continue
	}

	def covers = higson.call('demo.property.apk.packageCovers', ctx, code)
	def covered = 0
	for (def cover in required) {
		if (covers.contains(cover)) {
			covered = covered + 1
		}
	}

	def position = variant.getAttrDecimal('POSITION', ctx)
	def betterCoverage = covered > bestCovered
	// Remis w pokryciu: wygrywa tanszy pakiet, czyli nizsza pozycja w drabince.
	def sameCoverageButCheaper = covered == bestCovered && position < bestPosition
	if (betterCoverage || sameCoverageButCheaper) {
		best = code
		bestCovered = covered
		bestPosition = position
	}
}
return best