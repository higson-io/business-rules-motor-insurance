/*
#Full code: pl.decerto.demo.telco.invoice.overall.price.sum.calculator

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
name = "includeCrossSell"
type = "BOOLEAN"

[[arguments]]
name = "packageCode"
type = "STRING"

[[arguments]]
name = "duration"
type = "INTEGER"
*/

def overallPackagePrice = getMonthlyPrice(ctx)


//calculate extra channels price
BigDecimal overallChannelsPrice = higson.call("pl.decerto.demo.telco.extra.channel.price.calculator", ctx, packageCode, duration)
overallPackagePrice = overallPackagePrice.add(overallChannelsPrice)

//calculate hardware price
BigDecimal hardwarePrice = higson.call("pl.decerto.demo.telco.hardware.overall.price.calculator", ctx, packageCode, duration)
overallPackagePrice = overallPackagePrice.add(hardwarePrice)

//calculate service price, first element - overall price, second - price after cross-sell discount
BigDecimal[] servicePrices = (BigDecimal[]) higson.call("pl.decerto.demo.telco.service.overall.price.calculator", ctx, packageCode, duration)

if (includeCrossSell) {
    return overallPackagePrice.add(servicePrices[1])
} else {
    return overallPackagePrice.add(servicePrices[0])
}

def getMonthlyPrice(ctx) {
    def packageCode = ctx.getString("package.code")
    BigDecimal monthlyPrice = domain.get("TELCO").get("/Packages[" + packageCode + "]").getAttrDecimal("StartingPricePromos", ctx)

    if (monthlyPrice == null) {
       monthlyPrice = domain.get("TELCO").get("/Packages[" + packageCode + "]").getAttrDecimal("MonthlyPrice")
    }
    
    return monthlyPrice
}
