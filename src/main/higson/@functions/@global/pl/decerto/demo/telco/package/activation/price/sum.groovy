/*
#Full code: pl.decerto.demo.telco.package.activation.price.sum

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

BigDecimal overallActivationPrice = BigDecimal.ZERO

//calculate hardware price
BigDecimal hardwareActivationPrice = higson.call("pl.decerto.demo.telco.hardware.activation.price.calculator", ctx, packageCode)
overallActivationPrice = overallActivationPrice.add(hardwareActivationPrice)

//calculate service price
BigDecimal serviceActivationPrice = higson.call("pl.decerto.demo.telco.service.activation.price.calculator", ctx, packageCode)
overallActivationPrice = overallActivationPrice.add(serviceActivationPrice)

return overallActivationPrice