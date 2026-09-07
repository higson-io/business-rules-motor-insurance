/*
#Full code: demo.property.cover.premium

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Jedyne miejsce, w ktorym stoi wzor na skladke pokrycia. tariff.coverPremiums wola te funkcje w petli po skladzie pakietu, wiec skladka jednego pokrycia i rozbicie calego pakietu nie moga sie rozjechac. Wymaga kontekstu z cover.code, variant.code i sumami wyliczonymi przez sum.suggest."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "riskFactor"
type = "BIG_DECIMAL"
*/

def code = ctx.getString('cover.code')

def rateRow = higson.getValue('demo.property.tariff.baseRate', higson.createContext('cover.code', code))
def rate = rateRow.get(0, 'rate')
def basis = rateRow.get(0, 'basis')

// Assistance kupuje usluge, nie transfer ryzyka - czynniki ryzyka nie maja na co dzialac,
// a stawka JEST cala skladka.
if ('FLAT' == basis) {
	return math.round(rate, 2)
}

def sum = higson.call('demo.property.cover.sum', ctx)
if (sum == null) {
	return 0
}

// Stawka jest w promilach sumy. Zaokraglenie NA POKRYCIE, raz - suma niezaokraglonych linii
// zaokraglona na koncu daje total, ktory nie zgadza sie z wlasnym rozbiciem, a czytelnik
// oferty zawsze to wylapie.
return math.round(sum * rate / 1000 * riskFactor, 2)