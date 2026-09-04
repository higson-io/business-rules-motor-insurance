/*
#Full code: demo.property.tariff.instalment

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
name = "annual"
type = "BIG_DECIMAL"
*/

def row = higson.getValue('demo.property.factor.period', ctx)
def installments = row.get(0, 'installments')
if (installments == null || installments < 1) {
	installments = 1
}
return math.round(annual / installments, 2)