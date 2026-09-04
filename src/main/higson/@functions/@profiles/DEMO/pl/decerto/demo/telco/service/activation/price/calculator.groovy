/*
#Full code: pl.decerto.demo.telco.service.activation.price.calculator

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = ""

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "org.smartparam.engine.core.context.ParamContext"

[[arguments]]
name = "packageCode"
type = "STRING"
*/

def packageRoot = domain.get("TELCO").get("/Packages[" + packageCode + "]")

BigDecimal activationPrice = BigDecimal.ZERO

for (def service : ctx.get("package.services")) {
    if (service.getBoolean("selected")) {
        String code = service.get("code")
        def serviceElement = packageRoot.get("SERVICES", code)
        
        def serviceContext = higson.createContext(ctx, "service.code", code, "service.selected", true)
        BigDecimal serviceActivationPrice = serviceElement.getAttrDecimal("ActivationPrice", serviceContext)

        log.info("adding selected service:{} with activation price:{}", code, serviceActivationPrice)
        
        activationPrice = activationPrice.add(serviceActivationPrice)
    }
}
log.info("calculated service activation price:{}", activationPrice)

return activationPrice
