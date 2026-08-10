/*
#Full code: pl.decerto.demo.telco.invoice.first.entry.price.calculator

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

BigDecimal overallActivationPrice = higson.call("pl.decerto.demo.telco.package.activation.price.sum", ctx, packageCode)
BigDecimal[] overallPrices = (BigDecimal[]) higson.call("pl.decerto.demo.telco.package.overall.price.sum", ctx, packageCode, duration)

log.info("overall activaion price:{}, overall price:{}", overallActivationPrice, overallPrices[0])
return overallActivationPrice.add(overallPrices[0])