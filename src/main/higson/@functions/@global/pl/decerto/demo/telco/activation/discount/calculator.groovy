/*
#Full code: pl.decerto.demo.telco.activation.discount.calculator

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
*/

def packageRoot = domain.get("TELCO").get("/Packages[" + packageCode + "]")

BigDecimal activationDiscountsPrice = BigDecimal.ZERO

for (def discount : packageRoot.getAll("DISCOUNTS")) {
    //todo discounts
    if (discount.getAttrBoolean("Available", ctx)) {
        BigDecimal activationValue = discount.getAttrDecimal("ActivationValue", ctx)
        
        log.info("adding discount:{} with activation price:{}", discount.getCode(), activationValue)
        
        activationDiscountsPrice = activationDiscountsPrice.add(activationValue)
    }
}

activationDiscountsPrice = calculatePercentage(activationDiscountsPrice)

log.info("calculated activation discount:{}", activationDiscountsPrice)

return activationDiscountsPrice

def calculatePercentage(BigDecimal value) {
    return value.divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP)
}