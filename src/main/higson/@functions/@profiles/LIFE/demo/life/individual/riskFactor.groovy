/*
#Full code: demo.life.individual.riskFactor

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Iloczyn trzech czynnikow ryzyka. Mnoza sie, a nie zastepuja - kazdy opisuje inne ryzyko."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

return higson.getDecimal('demo.life.individual.factor.smoker', ctx) *
		higson.getDecimal('demo.life.individual.factor.occupation', ctx) *
		higson.getDecimal('demo.life.individual.factor.bmi', ctx)