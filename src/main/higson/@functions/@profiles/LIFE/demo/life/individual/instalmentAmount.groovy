/*
#Full code: demo.life.individual.instalmentAmount

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Rata przy wybranej czestotliwosci. Rata jest zrodlem prawdy - skladka roczna powstaje z niej, nie odwrotnie."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "premiumMonthly"
type = "BIG_DECIMAL"
*/

def row = higson.getValue('demo.life.paymentFrequency.factor', ctx)
return math.round(premiumMonthly * 12 * row.get(0, 'factor') / row.get(0, 'installments'), 2)