/*
#Full code: demo.insurance.calcpremium

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

def basePremiumPerDay = ctx.getNumber('policy.premiumPerDay')

def dateFrom = ctx.getDate('policy.startDate')
def dateTo = ctx.getDate('policy.endDate')

def days = date.getDayDiff(dateFrom, dateTo) + 1

log.debug("days: {}", days)

return basePremiumPerDay * days