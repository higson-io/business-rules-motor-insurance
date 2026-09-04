/*
#Full code: pl.decerto.demo.telco.overall.discount.calculator

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
name = "codesToInclude"
type = "OBJECT"
*/

def packageRoot = domain.get("TELCO").get("/Packages[" + packageCode + "]")

BigDecimal overallDiscountsPrice = BigDecimal.ZERO

for (def discount : packageRoot.getAll("DISCOUNTS")) {
    if (discount.getAttrBoolean("Available", ctx) && shouldBeIncluded(discount.getCode())) {
        BigDecimal overallValue = discount.getAttrDecimal("OverallValue", ctx)
        
        log.info("adding discount:{} with overall price:{}", discount.getCode(), overallValue)
        
        overallDiscountsPrice = overallDiscountsPrice.add(overallValue)
    }
}

overallDiscountsPrice = calculatePercentage(overallDiscountsPrice)

log.info("calculated overall discount:{}", overallDiscountsPrice)

return overallDiscountsPrice

def shouldBeIncluded(discountCode) {
    if (codesToInclude == null) {
        return true
    } else {
        return ((Set<String>) codesToInclude).contains(discountCode)     
    }
}
def calculatePercentage(BigDecimal value) {
    return value.divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP)
}