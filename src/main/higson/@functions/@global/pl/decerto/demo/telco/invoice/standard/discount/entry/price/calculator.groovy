/*
#Full code: pl.decerto.demo.telco.invoice.standard.discount.entry.price.calculator

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

BigDecimal[] overallPrices = (BigDecimal[]) higson.call("pl.decerto.demo.telco.package.overall.price.sum", ctx, packageCode, duration)

//calculate general discounts
BigDecimal discounts = higson.call("pl.decerto.demo.telco.overall.discount.calculator", ctx, packageCode)
BigDecimal overallPackagePriceAfterDiscounts = calculateDiscount(overallPrices[0], discounts)

log.info("overall price:{}, after discounts:{}", overallPrices[0], overallPackagePriceAfterDiscounts)
return overallPackagePriceAfterDiscounts

def calculateDiscount(value, discountValue) {
   return value.subtract(value.multiply(discountValue))
}