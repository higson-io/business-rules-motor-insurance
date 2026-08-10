/*
#Full code: demo.motor.plan.full.calculatePremium

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "org.smartparam.engine.core.context.ParamContext"
*/


def factor = higson.getNumber('demo.motor.plan.full.tariff', ctx)

def limit1 = ctx.getNumber('coverage.limit1')

def prem = limit1 * factor

if (prem == 0) {
    prem = 10.0
}

prem = prem + 0.11

return math.round(prem, 2)