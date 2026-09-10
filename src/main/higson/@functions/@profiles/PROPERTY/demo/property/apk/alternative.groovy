/*
#Full code: demo.property.apk.alternative

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Gdy propozycja przekracza oczekiwana skladke, tanszy pakiet NIE zastepuje jej, tylko jedzie obok jako alternatywa. To ta sama drabinka co przy propozycji, przejrzana z gornym ograniczeniem ceny - stad jedyna roznica to argument maxPremium. Bez podanej oczekiwanej skladki nie ma czego szukac."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def budget = ctx.getDecimal('quote.needs.budget')
if (budget == null || budget <= 0) {
	return null
}
return higson.call('demo.property.apk.bestCovering', ctx, budget)