/*
#Full code: demo.property.tariff.annualPremium

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = ""

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "base"
type = "BIG_DECIMAL"
*/

def row = higson.getValue('demo.property.factor.period', ctx)
def factor = row.get(0, 'factor')
if (factor == null) {
	factor = 1
}
return math.round(base * factor, 2)