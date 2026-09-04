/*
#Full code: demo.property.tariff.coverPremiums

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = ""

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "riskFactor"
type = "BIG_DECIMAL"
*/

def variant = ctx.getString('variant.code')
def lines = []

for (def cover in domain.get('PROPERTY', '/PRODUCTS[HOME]').getAll('COVERS')) {
	def code = cover.getCode()
	def cellCtx = higson.createContext('cover.code', code, 'variant.code', variant)
	if (!higson.getBoolean('demo.property.cover.availability', cellCtx)) {
		continue
	}

	def rateRow = higson.getValue('demo.property.tariff.baseRate',
			higson.createContext('cover.code', code))
	def rate = rateRow.get(0, 'rate')
	def basis = rateRow.get(0, 'basis')

	def premium
	if ('FLAT' == basis) {
		// Assistance kupuje uslugi, nie transfer ryzyka - czynniki ryzyka nie maja na co dzialac.
		premium = rate
	} else {
		def sum = ctx.getDecimal('quote.sums.' + code.toLowerCase())
		premium = sum == null ? 0 : sum * rate / 1000 * riskFactor
	}

	// Zaokraglenie NA LINIE, raz. Suma niezaokraglonych linii zaokraglona na koncu daje total,
	// ktory nie zgadza sie z wlasnym rozbiciem, a czytelnik oferty zawsze to wylapie.
	lines.add(code + '=' + math.round(premium, 2))
}

return lines.join(';')