/*
#Full code: pl.decerto.demo.telco.invoice.first.entry.discount.price.calculator

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

BigDecimal[] overallPrices = (BigDecimal[]) higson.call("pl.decerto.demo.telco.package.overall.price.calculator", ctx)
BigDecimal[] activationPrices = (BigDecimal[]) higson.call("pl.decerto.demo.telco.package.activation.price.calculator", ctx)

return overallPrices[1].add(activationPrices[1])