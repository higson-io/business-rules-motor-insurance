/*
#Full code: demo.life.util.entryAge

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Wiek wstepu w pelnych latach na dzien poczatku ochrony. Arytmetyka dat, ktorej tabela nie zrobi."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def birth = ctx.get('quote.insured.birthDate')
def start = ctx.get('quote.policy.startDate')
if (birth == null || start == null) {
	throw new RuntimeException('Brak daty urodzenia lub daty poczatku ochrony')
}
return date.diff(start, birth, 'Y')