/*
#Full code: demo.life.individual.effectiveWaitingPeriod

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Karencja po zaliczeniu poprzedniej ochrony. Musi byc funkcja, nie flow: tabela zaliczenia jest kluczowana wyliczonymi priorCoverMonths i gapDays, a flow nie zapisuje kontekstu."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

// Karencja bazowa i zaliczenie zostaja DWIEMA tabelami - to dwie niezalezne decyzje produktowe.
// Ta funkcja tylko je sklada. Nie moze byc flow, bo tabela zaliczenia potrzebuje wyliczonych
// priorCoverMonths i gapDays, a flow nie ma jak wstawic ich do kontekstu (brak ctx.set).
def span = higson.call('demo.life.util.priorCoverSpan', ctx)
def riderCode = ctx.getString('rider.code')
def cause = ctx.getString('claim.cause')

def baseDays = higson.getDecimal('demo.life.individual.waitingPeriod.base',
		higson.createContext('rider.code', riderCode, 'claim.cause', cause))
if (baseDays == null || baseDays == 0) {
	return 0
}

def creditRatio = higson.getDecimal('demo.life.individual.waitingPeriod.credit',
		higson.createContext('rider.code', riderCode,
				'quote.insured.priorCoverMonths', span.get(0, 'priorMonths'),
				'quote.insured.priorCoverGapDays', span.get(0, 'gapDays')))
if (creditRatio == null) {
	creditRatio = 0
}

return math.round(baseDays * (1 - creditRatio), 0)