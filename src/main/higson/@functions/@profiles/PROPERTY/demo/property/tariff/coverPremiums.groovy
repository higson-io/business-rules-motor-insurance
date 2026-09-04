/*
#Full code: demo.property.tariff.coverPremiums

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Skladki pokryc wchodzacych w sklad wariantu. Sklad czytany Z DRZEWA - pokrycia wisza pod pakietem w kolekcji COVERS - a nie z tabeli par (pokrycie, wariant)."

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

// Sklad pakietu jest POD PAKIETEM w drzewie. Do 4 wrzesnia 2026 przechodzilo sie tu po
// wszystkich pokryciach produktu i odpytywalo tabele demo.property.cover.availability, czy dane
// pokrycie jest w tym wariancie. Teraz iteruje sie wprost po skladzie: petla jest krotsza o jedno
// pytanie, a odpowiedz na nie widac w drzewie bez czytania tabeli.
for (def member in domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + variant + ']').getAll('COVERS')) {
	def code = member.getCode()

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