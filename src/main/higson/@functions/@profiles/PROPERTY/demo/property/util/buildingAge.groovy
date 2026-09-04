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

// UWAGA: ctx.get zwraca daty jako String, nie java.util.Date - dlatego rok bierzemy z pierwszych
// czterech znakow, a nie przez Calendar. Ta sama pulapka co przy LIFE.
def year = ctx.getInteger('quote.property.constructionYear')
def start = ctx.get('quote.policy.startDate')
if (year == null || year <= 0 || start == null) {
	return 0
}
def startYear = start.toString().substring(0, 4).toInteger()
def age = startYear - year
return age < 0 ? 0 : age