/*
#Full code: demo.life.individual.monthlyPremium

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Skladka miesieczna. Zaokraglenie NA LINIE, raz - suma zaokraglonych linii, a nie zaokraglona suma, bo inaczej total nie zgadza sie z wlasnymi skladnikami."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def riskFactor = higson.getDecimal('demo.life.individual.factor.smoker', ctx) *
		higson.getDecimal('demo.life.individual.factor.occupation', ctx) *
		higson.getDecimal('demo.life.individual.factor.bmi', ctx)

def mainSum = ctx.getDecimal('quote.policy.mainSum')
def baseRate = higson.getDecimal('demo.life.individual.tariff.baseRate', ctx)
def total = math.round(mainSum / 1000 * baseRate * riskFactor, 2)

def riders = ctx.get('quote.riders')
if (riders == null) {
	return total
}

def agedFactor = higson.call('demo.life.util.ageRatingFactor', ctx)

for (def rider in riders) {
	def code = rider.getString('code')
	def riderCtx = higson.createContext('rider.code', code,
			'quote.insured.entryAge', ctx.getInteger('quote.insured.entryAge'))

	if (!higson.getBoolean('demo.life.individual.rider.availability', riderCtx)) {
		continue
	}

	def kind = higson.getString('demo.life.individual.rider.benefitKind', riderCtx)
	def rateRow = higson.getValue('demo.life.individual.rider.rate', riderCtx)
	def rate = rateRow.get(0, 'rate')
	def aged = rateRow.get(0, 'ageRated') ? agedFactor : 1
	def sum = rider.getDecimal('sum')

	def premium
	if (kind == 'SERVICE_PACKAGE') {
		// Assistance kupuje uslugi, nie transfer ryzyka - wiek i palenie nie maja na co dzialac.
		premium = rate
	} else if (kind == 'DAILY_AMOUNT') {
		premium = sum * rate * riskFactor * aged
	} else {
		premium = sum / 1000 * rate * riskFactor * aged
	}

	total = total + math.round(premium, 2)
}

return total