/*
#Full code: demo.life.util.bmi

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Wskaznik masy ciala. Tabela przedzialowa dostaje BMI jako wejscie, wiec ktos musi je wyliczyc."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def cm = ctx.getDecimal('quote.insured.heightCm')
def kg = ctx.getDecimal('quote.insured.weightKg')
// Wzrost 0 to dane bezsensowne, nie sytuacja wyjatkowa. Zwracamy 0, zeby tabela przedzialowa
// trafila w wiersz domyslny zamiast wywalic sie na dzieleniu przez zero.
if (cm == null || kg == null || cm <= 0) {
	return 0
}
def m = cm / 100
return math.round(kg / (m * m), 1)