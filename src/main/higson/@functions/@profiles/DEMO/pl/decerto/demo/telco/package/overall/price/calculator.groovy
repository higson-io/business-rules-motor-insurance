/*
#Full code: pl.decerto.demo.telco.package.overall.price.calculator

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
def overallPrice = overallPrices[0]
def overallPriceAfterCrossSellPromotion = overallPrices[1]
//calculate general discounts
BigDecimal discounts = higson.call("pl.decerto.demo.telco.overall.discount.calculator", ctx, packageCode)
BigDecimal overallPackagePriceAfterDiscounts = calculateDiscount(overallPriceAfterCrossSellPromotion, discounts)


BigDecimal[] prices = new BigDecimal[2]
prices[0] = overallPrice
prices[1] = overallPackagePriceAfterDiscounts

return prices

def calculateDiscount(value, discountValue) {
   return value.subtract(value.multiply(discountValue))
}