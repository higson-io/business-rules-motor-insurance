/*
#Full code: demo.property.cover.premium

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Jedyne miejsce, w ktorym stoi wzor na skladke pokrycia. Kod pokrycia dostaje ARGUMENTEM, wiec atrybuty pokrycia czyta na PELNYM kontekscie wyceny - a to konieczne, bo kazde pokrycie ma wlasna tabele kluczowana czym innym: powodz strefa, kradziez zabezpieczeniami, ruchomosci standardem wykonczenia. Osobny, waski kontekst buduje tylko dla cover.sum, ktora potrzebuje zawsze tych samych trzech sum i kodu wariantu - i ta lista nie rosnie, gdy pokrycie dostaje nowy wymiar."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "coverCode"
type = "STRING"
*/

def cover = domain.get('PROPERTY', '/PRODUCTS[HOME]/COVERS[' + coverCode + ']')

// Pokrycie niedostepne w tej sytuacji nie kosztuje nic - i nie znika po cichu, bo powod
// odmowy niesie ta sama tabela i trafia do uzasadnienia.
if (!Boolean.TRUE.equals(cover.getAttrBoolean('IS_AVAILABLE', ctx))) {
	return 0
}

def rate = cover.getAttrDecimal('RATE', ctx)
if (rate == null) {
	return 0
}

// Assistance kupuje usluge, nie transfer ryzyka - czynniki ryzyka nie maja na co dzialac,
// a stawka JEST cala skladka.
if ('FLAT' == cover.getAttrString('RATE_BASIS', ctx)) {
	return math.round(rate, 2)
}

def sumCtx = higson.createContext(
		'cover.code', coverCode,
		'variant.code', ctx.getString('variant.code'),
		'quote.sums.building', ctx.getDecimal('quote.sums.building'),
		'quote.sums.fixtures', ctx.getDecimal('quote.sums.fixtures'),
		'quote.sums.contents', ctx.getDecimal('quote.sums.contents'))
def sum = higson.call('demo.property.cover.sum', sumCtx)
if (sum == null) {
	return 0
}

def riskFactor = ctx.getDecimal('quote.risk.factor')
if (riskFactor == null) {
	riskFactor = 1
}

// Stawka jest w promilach sumy. Zaokraglenie NA POKRYCIE, raz - suma niezaokraglonych linii
// zaokraglona na koncu daje total, ktory nie zgadza sie z wlasnym rozbiciem.
return math.round(sum * rate / 1000 * riskFactor, 2)