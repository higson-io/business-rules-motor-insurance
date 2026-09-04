/*
#Full code: pl.decerto.demo.telco.invoice.standard.entry.price.calculator

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

def packageCode = ctx.getString("package.code")
def duration = ctx.getInteger("package.duration")
log.info("calculating price for:{}, duration:{}", packageCode, duration)

//first element - overall price, second - overall price after cross sell discounts
BigDecimal[] overallPrices = (BigDecimal[]) higson.call("pl.decerto.demo.telco.package.overall.price.sum", ctx, packageCode, duration)

//return price without any cross sell discount
return overallPrices[0]