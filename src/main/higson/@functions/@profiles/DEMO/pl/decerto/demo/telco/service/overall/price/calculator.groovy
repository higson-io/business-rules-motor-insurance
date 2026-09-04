/*
#Full code: pl.decerto.demo.telco.service.overall.price.calculator

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

def packageRoot = domain.get("TELCO").get("/Packages[" + packageCode + "]")

BigDecimal servicePrice = BigDecimal.ZERO
BigDecimal servicePriceAfterDiscounts = BigDecimal.ZERO

for (def service : ctx.get("package.services")) {
    if (service.getBoolean("selected")) {
        String code = service.get("code")
        def serviceElement = packageRoot.get("SERVICES", code)
        
        def serviceContext = higson.createContext(ctx, "service.code", code, "service.selected", true)
        BigDecimal monthlyPrice = serviceElement.getAttrDecimal("MonthlyPrice", serviceContext)
        BigDecimal monthlyPriceAfterDiscounts = serviceElement.getAttrDecimal("MonthlyPriceAfterDiscounts", serviceContext)

        log.info("adding selected service:{} with monthly price:{}, after discount:{}", 
            code, monthlyPrice, monthlyPriceAfterDiscounts)
        
        servicePrice = servicePrice.add(monthlyPrice)
        servicePriceAfterDiscounts = servicePriceAfterDiscounts.add(monthlyPriceAfterDiscounts)
    }
}
log.info("calculated service price:{}, after discount:{}", servicePrice, servicePriceAfterDiscounts)

BigDecimal[] prices = new BigDecimal[2]
prices[0] = servicePrice
prices[1] = servicePriceAfterDiscounts

return prices