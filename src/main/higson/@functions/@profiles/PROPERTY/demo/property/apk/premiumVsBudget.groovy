/*
#Full code: demo.property.apk.premiumVsBudget

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "NIE_PODAL, MIESCI_SIE albo ZA_DROGO. Oczekiwana skladka to odpowiedz z ankiety, cena propozycji przychodzi po wycenie. ⛔ Oczekiwana skladka NIE OBNIZA ZAKRESU - mowi tylko, czy propozycja sie w niej miesci; tansza alternatywa jedzie obok, nie zamiast."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def budget = ctx.getDecimal('quote.needs.budget')
if (budget == null || budget.signum() == 0) {
	return 'NIE_PODAL'
}

def premium = ctx.getDecimal('quote.apk.proposedPremium')
if (premium == null) {
	return null
}
return premium.compareTo(budget) <= 0 ? 'MIESCI_SIE' : 'ZA_DROGO'