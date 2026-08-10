/*
#Full code: pl.decerto.demo.telco.package.overall.price.sum

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "org.smartparam.engine.core.context.ParamContext"

[[arguments]]
name = "packageCode"
type = "STRING"

[[arguments]]
name = "duration"
type = "INTEGER"
*/

BigDecimal monthlyPrice = getMonthlyPrice(packageCode, ctx)
log.info("starting monthly price:{}", monthlyPrice)

BigDecimal overallPackagePrice = monthlyPrice

//calculate extra channels price
BigDecimal overallChannelsPrice = higson.call("pl.decerto.demo.telco.extra.channel.price.calculator", ctx, packageCode, duration)
overallPackagePrice = overallPackagePrice.add(overallChannelsPrice)

//calculate hardware price
BigDecimal hardwarePrice = higson.call("pl.decerto.demo.telco.hardware.overall.price.calculator", ctx, packageCode, duration)
overallPackagePrice = overallPackagePrice.add(hardwarePrice)

//calculate service price, first element - overall price, second - price after cross-sell discount
BigDecimal[] servicePrices = (BigDecimal[]) higson.call("pl.decerto.demo.telco.service.overall.price.calculator", ctx, packageCode, duration)
def overallServicePrice = servicePrices[0]
def overallServicePriceAfterCrossSellPromotion = servicePrices[1]

BigDecimal overallPriceWithCrossSellDiscounts = overallPackagePrice.add(overallServicePriceAfterCrossSellPromotion)
overallPackagePrice = overallPackagePrice.add(overallServicePrice)


BigDecimal[] prices = new BigDecimal[2]
prices[0] = overallPackagePrice
prices[1] = overallPriceWithCrossSellDiscounts

return prices

def getMonthlyPrice(packageCode, ctx) {
    BigDecimal monthlyPrice = domain.get("TELCO").get("/Packages[" + packageCode + "]").getAttrDecimal("StartingPricePromos", ctx)

    if (monthlyPrice == null) {
       monthlyPrice = domain.get("TELCO").get("/Packages[" + packageCode + "]").getAttrDecimal("MonthlyPrice")
    }
    
    return monthlyPrice
}
