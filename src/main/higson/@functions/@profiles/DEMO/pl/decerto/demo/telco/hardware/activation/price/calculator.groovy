/*
#Full code: pl.decerto.demo.telco.hardware.activation.price.calculator

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

BigDecimal activationPrice = BigDecimal.ZERO

for (def hardware : ctx.get("package.hardware")) {
    if (hardware.getBoolean("selected")) {
        String code = hardware.get("code")
        
        def hardwareContext = higson.createContext(ctx, "hardware.code", code)
        BigDecimal hardwareActivationPrice = packageRoot.get("HARDWARE", code).getAttrDecimal("ActivationPrice", hardwareContext)
        log.info("adding selected hardware:{} with activation price:{}", code, hardwareActivationPrice)
        
        activationPrice = activationPrice.add(hardwareActivationPrice)
    }
}
log.info("calculated activation price:{}", activationPrice)

return activationPrice