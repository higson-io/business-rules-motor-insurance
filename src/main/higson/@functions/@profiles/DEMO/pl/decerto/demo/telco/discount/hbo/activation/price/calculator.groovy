/*
#Full code: pl.decerto.demo.telco.discount.hbo.activation.price.calculator

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

for (def hardware : ctx.get("package.hardware")) {
    if (hardware.getBoolean("selected") && "ULTRABOX".equals(hardware.getString("code"))) {
        return 5
    }
}

return 0