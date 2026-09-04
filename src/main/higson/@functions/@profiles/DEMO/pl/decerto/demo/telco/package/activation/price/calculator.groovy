/*
#Full code: pl.decerto.demo.telco.package.activation.price.calculator

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
log.info("calculating activation price for:{}", packageCode)

BigDecimal overallActivationPrice = higson.call("pl.decerto.demo.telco.package.activation.price.sum", ctx, packageCode)

//calculate discount
BigDecimal discount = higson.call("pl.decerto.demo.telco.activation.discount.calculator", ctx, packageCode)
BigDecimal overallActivationPriceAfterDiscounts = calculateDiscount(overallActivationPrice, discount)


BigDecimal[] prices = new BigDecimal[2]
prices[0] = overallActivationPrice
prices[1] = overallActivationPriceAfterDiscounts

return prices

def calculateDiscount(value, discountValue) {
   return value.subtract(value.multiply(discountValue))
}