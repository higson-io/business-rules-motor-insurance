/*
#Full code: demo.property.apk.budgetOutcome

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "⛔ OCZEKIWANA SKLADKA NIE JEST FILTREM ZAKRESU (rek. 8.2.d). Ta funkcja NIE wybiera pakietu - ona tylko nazywa, czy propozycja miesci sie w oczekiwaniu klienta. Zakres wynika z potrzeb i tak zostaje, a przekroczenie budzetu mowi sie wprost. Do 7 wrzesnia 2026 ta regula byla komentarzem nad linijka w Javie. Zwraca gotowy KOD POWODU, ten sam, ktorym kluczuje sie apk.recommendation.reason - dzieki temu Java nie doklada do niego prefiksu."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

// Rezim rozstrzyga, czy o oczekiwana skladke w ogole pytamy. Gdy nie pyta, nie ma czego porownywac.
def regime = higson.getValue('demo.property.apk.regime', ctx)
if (!Boolean.TRUE.equals(regime.get(0, 'requiresBudget'))) {
	return 'BUDGET_ABSENT'
}

def budget = ctx.getDecimal('quote.needs.budget')
if (budget == null || budget <= 0) {
	return 'BUDGET_ABSENT'
}

def indicated = ctx.getString('quote.apk.indicated')
if (indicated == null) {
	return 'BUDGET_ABSENT'
}

def premium = ctx.getDecimal('quote.premium.' + indicated)
if (premium == null) {
	return 'BUDGET_ABSENT'
}

return premium <= budget ? 'BUDGET_FIT' : 'BUDGET_EXCEEDED'