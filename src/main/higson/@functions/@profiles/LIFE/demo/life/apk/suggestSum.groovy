/*
#Full code: demo.life.apk.suggestSum

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Suma ubezpieczenia wyprowadzona z sytuacji klienta. Musi byc funkcja, bo tabela nie mnozy - ale ani jeden mnoznik nie siedzi tutaj, wszystkie sa w demo.life.apk.sum.multiplier."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def row = higson.getValue('demo.life.apk.sum.multiplier', ctx)
if (row.isEmpty()) {
	return 0
}

def obligations = ctx.getDecimal('quote.apk.monthlyObligations')
def dependants = ctx.getInteger('quote.apk.dependants')
if (obligations == null) {
	obligations = 0
}
if (dependants == null) {
	dependants = 0
}

def base = row.get(0, 'base')
def months = row.get(0, 'months')
def perDependant = row.get(0, 'perDependant')

def raw = base + obligations * months + dependants * perDependant
if (raw <= 0) {
	return 0
}
// Zaokraglenie W GORE do pelnych 10 000 zl: suma ubezpieczenia jest kwota, ktora klient ma
// zapamietac, a nie wynikiem dzielenia. ⚠️ toBigInteger na koniec, bo inaczej uzasadnienie
// wypisuje "160000.0 zl" - a to jest tekst czytany przez klienta, nie log.
def rounded = math.round((raw / 10000) + 0.49, 0) * 10000
return rounded.toBigInteger()