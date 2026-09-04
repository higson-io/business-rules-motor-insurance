/*
#Full code: demo.life.individual.ridersPremium

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Suma skladek wybranych umow dodatkowych. Zaokraglenie NA LINIE, raz - inaczej total nie zgadza sie z wlasnymi skladnikami. Formula jednej linii siedzi w demo.life.individual.rider.premium, bo ta sama liczba jest tez pokazywana per umowa."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "riskFactor"
type = "BIG_DECIMAL"
*/

def riders = ctx.get('quote.riders')
if (riders == null || riders.isEmpty()) {
	return 0
}

def age = ctx.getInteger('quote.insured.entryAge')
def total = 0

for (def rider in riders) {
	// rider.sum wchodzi do kontekstu, bo formula jednej linii siedzi teraz osobno i czyta ja
	// stamtad - dokladnie tak samo, jak czyta ja Java przy liczeniu pozycji na ekranie.
	def riderCtx = higson.createContext('rider.code', rider.getString('code'),
			'rider.sum', rider.getDecimal('sum'),
			'quote.insured.entryAge', age)

	total = total + higson.call('demo.life.individual.rider.premium', riderCtx, riskFactor)
}

return total