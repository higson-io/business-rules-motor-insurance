/*
#Full code: demo.property.apk.alternative

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Gdy propozycja przekracza oczekiwana skladke, tanszy pakiet NIE zastepuje jej, tylko jedzie obok jako alternatywa - razem z lista tego, czego w nim brakuje. Wybieramy najlepiej dopasowany sposrod mieszczacych sie w budzecie, przy remisie tanszy; moze nie byc zadnego i to tez jest odpowiedz. Pokrycia bierze z apk.covers, tak samo jak apk.indicated."

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
def zone = ctx.getString('quote.zone.flood')

def best = null
def bestCovered = -1
def bestPosition = null

for (def variant in domain.get('PROPERTY', '/PRODUCTS[HOME]').getAll('VARIANTS')) {
	def code = variant.getCode()

	def availability = higson.getValue('demo.property.variant.availability',
			higson.createContext('variant.code', code, 'quote.zone.flood', zone))
	if (!Boolean.TRUE.equals(availability.get(0, 'available'))) {
		continue
	}

	def premium = ctx.getDecimal('quote.premium.' + code)
	if (premium == null || premium > budget) {
		continue
	}

	def covered = 0
	for (def member in variant.getAll('COVERS')) {
		if (required.contains(member.getCode())) {
			covered = covered + 1
		}
	}
	def position = higson.getDecimal('demo.property.variant.position',
			higson.createContext('variant.code', code))

	// Przy remisie w pokryciu wygrywa TANSZY szczebel - alternatywa ma byc ustepstwem cenowym,
	// wiec z dwoch rownie dopasowanych bierzemy nizszy.
	if (covered > bestCovered || (covered == bestCovered && position < bestPosition)) {
		best = code
		bestCovered = covered
		bestPosition = position
	}
}
return best