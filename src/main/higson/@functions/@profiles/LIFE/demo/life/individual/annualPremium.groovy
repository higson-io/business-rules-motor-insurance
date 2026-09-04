/*
#Full code: demo.life.individual.annualPremium

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Skladka roczna jako wielokrotnosc zaokraglonej raty. Nie ma drugiej liczby, ktora moglaby sie nie zgodzic."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "instalment"
type = "BIG_DECIMAL"
*/

def installments = higson.getValue('demo.life.paymentFrequency.factor', ctx).get(0, 'installments')
return instalment * installments