/*
#Full code: demo.property.util.buildingAge

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Wiek budynku na dzien poczatku ochrony. Arytmetyka dat, ktorej tabela nie zrobi."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

// UWAGA: ctx.get zwraca date jako String, nie Date - stad substring(0, 4), a nie Calendar.
def year = ctx.getInteger('quote.property.constructionYear')
def start = ctx.get('quote.policy.startDate')
if (year == null || year <= 0 || start == null) {
	return 0
}
def startYear = start.toString().substring(0, 4).toInteger()
def age = startYear - year
return age < 0 ? 0 : age