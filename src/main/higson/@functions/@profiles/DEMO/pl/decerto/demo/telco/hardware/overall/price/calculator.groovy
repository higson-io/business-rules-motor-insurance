/*
#Full code: pl.decerto.demo.telco.hardware.overall.price.calculator

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

BigDecimal hardwarePrice = BigDecimal.ZERO

for (def hardware : ctx.get("package.hardware")) {
    if (hardware.getBoolean("selected")) {
        String code = hardware.get("code")
        
        def hardwareContext = higson.createContext(ctx, "hardware.code", code)
        BigDecimal monthlyPrice = packageRoot.get("HARDWARE", code).getAttrDecimal("MonthlyPrice", hardwareContext)
        log.info("adding selected hardware:{} with monthly price:{}", code, monthlyPrice)
        
        hardwarePrice = hardwarePrice.add(monthlyPrice)
    }
}
log.info("calculated hardware price:{}", hardwarePrice,)

return hardwarePrice
