/*
#Full code: demo.property.cover.premium

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Jedyne miejsce, w ktorym stoi wzor na skladke pokrycia. Wszystko, co o pokryciu wie, czyta z jego ATRYBUTOW - a te niosą albo wartosc wprost, albo wskazanie wlasnej tabeli pokrycia tam, gdzie stawka czy dostepnosc naprawde sie zmieniaja. Osobny, waski kontekst buduje tylko dla cover.sum, ktora potrzebuje zawsze tych samych trzech sum i kodu wariantu."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "coverCode"
type = "STRING"
*/

def cover = domain.get('PROPERTY', '/PRODUCTS[HOME]/COVERS[' + coverCode + ']')
def available = cover.getAttrBoolean('IS_AVAILABLE', ctx)
// Pokrycie niedostepne nie kosztuje nic - i nie znika po cichu, bo powod odmowy niesie ten sam
// element i trafia do uzasadnienia.
if (!Boolean.TRUE.equals(available)) {
	return 0
}

def rate = cover.getAttrDecimal('RATE', ctx)
if (rate == null) {
	return 0
}

// Assistance kupuje usluge, nie transfer ryzyka - stawka JEST cala skladka.
def rateBasis = cover.getAttrString('RATE_BASIS', ctx)
if ('FLAT' == rateBasis) {
	return math.round(rate, 2)
}

def variantCode = ctx.getString('variant.code')
def buildingSum = ctx.getDecimal('quote.sums.building')
def fixturesSum = ctx.getDecimal('quote.sums.fixtures')
def contentsSum = ctx.getDecimal('quote.sums.contents')
def sumCtx = higson.createContext(
		'cover.code', coverCode,
		'variant.code', variantCode,
		'quote.sums.building', buildingSum,
		'quote.sums.fixtures', fixturesSum,
		'quote.sums.contents', contentsSum)
def sum = higson.call('demo.property.cover.sum', sumCtx)
if (sum == null) {
	return 0
}

def riskFactor = ctx.getDecimal('quote.risk.factor')
if (riskFactor == null) {
	riskFactor = 1
}

// Zaokraglenie NA POKRYCIE, raz - suma niezaokraglonych linii zaokraglona na koncu dalaby total
// niezgodny z wlasnym rozbiciem.
return math.round(sum * rate / 1000 * riskFactor, 2)