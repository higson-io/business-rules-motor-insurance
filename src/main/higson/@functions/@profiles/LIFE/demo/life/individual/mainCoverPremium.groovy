/*
#Full code: demo.life.individual.mainCoverPremium

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Skladka umowy glownej: suma na 1000 razy stawka wieku razy czynnik ryzyka."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "riskFactor"
type = "BIG_DECIMAL"
*/

def mainSum = ctx.getDecimal('quote.policy.mainSum')
def rate = higson.getDecimal('demo.life.individual.tariff.baseRate', ctx)
return math.round(mainSum / 1000 * rate * riskFactor, 2)