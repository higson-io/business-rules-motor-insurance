/*
#Full code: demo.life.individual.rider.premium

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Skladka JEDNEJ umowy dodatkowej. Wyjeta z petli demo.life.individual.ridersPremium, zeby ta sama formula liczyla sume i pozycje - ekran pokazywal wczesniej kazda wybrana umowe za 0,00 zl, bo skladki per umowa nie liczyl nikt. Zaokraglenie NA LINIE, tak jak przedtem w petli."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "riskFactor"
type = "BIG_DECIMAL"
*/

if (!higson.getBoolean('demo.life.individual.rider.availability', ctx)) {
	return 0
}

def kind = higson.getString('demo.life.individual.rider.benefitKind', ctx)
def rateRow = higson.getValue('demo.life.individual.rider.rate', ctx)
def rate = rateRow.get(0, 'rate')
def aged = rateRow.get(0, 'ageRated') ? higson.call('demo.life.util.ageRatingFactor', ctx) : 1
def sum = ctx.getDecimal('rider.sum')

def premium
if (kind == 'SERVICE_PACKAGE') {
	// Assistance kupuje uslugi, nie transfer ryzyka - wiek i palenie nie maja na co dzialac.
	premium = rate
} else if (kind == 'DAILY_AMOUNT') {
	premium = sum * rate * riskFactor * aged
} else {
	premium = sum / 1000 * rate * riskFactor * aged
}

return math.round(premium, 2)