/*
#Full code: pl.decerto.demo.telco.invoice.discount.price.calculator

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

def pv = higson.getParamValue("pl.decerto.demo.telco.package.invoice.discount", ctx)

def discountCodes = pv.row().getStringArray("discounts")
def includeCrossSellPromotion = pv.row().getBoolean("cross-sell promotion")

log.info("include discounts:{}, cross sell promotion:{}",discountCodes, includeCrossSellPromotion)

def packageCode = ctx.getString("package.code")
def duration = ctx.getString("package.duration")

def overallPrice = higson.call("pl.decerto.demo.telco.invoice.overall.price.sum.calculator",
    ctx, includeCrossSellPromotion, packageCode, duration)
log.info("calculated overall price before discount:{}", overallPrice)

def discountPercentageValue = calculateDiscountPercentage(discountCodes, packageCode)
log.info("calculated discount percentage:{}", discountPercentageValue)


return calculateDiscount(overallPrice, discountPercentageValue)






def calculateDiscountPercentage(discountCodes, packageCode) {
    def codesSet = createDiscountCodeSet(discountCodes)
    return higson.call("pl.decerto.demo.telco.overall.discount.calculator", ctx, packageCode, codesSet)
}

def createDiscountCodeSet(discountCodes) {
     def set = util.set()
    discountCodes.each { code ->
        set.add(code)
    }
}

def calculateDiscount(value, discountValue) {
   return value.subtract(value.multiply(discountValue))
}