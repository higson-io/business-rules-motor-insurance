/*
#Full code: demo.motor.coverage.bi.calculatePremium

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "dodaj 0,5 sekundy sleep  przed return"

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "org.smartparam.engine.core.context.ParamContext"
*/

// tariffication factor
def factor = higson.getNumber('demo.motor.coverage.bi.tariff', ctx)

// coverage pay limits
def limit1 = ctx.getNumber('coverage.limit1')
def limit2 = ctx.getNumber('coverage.limit2')

// use formula
def premium = (100000 + limit1 + limit2) / 1000 * factor

// round to 2 digits
//Thread.sleep(500)
return math.round(premium, 2)