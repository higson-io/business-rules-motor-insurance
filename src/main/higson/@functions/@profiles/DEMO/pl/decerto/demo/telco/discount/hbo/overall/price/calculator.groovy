/*
#Full code: pl.decerto.demo.telco.discount.hbo.overall.price.calculator

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

for (def extraChannel : ctx.get("package.extraChannels")) {
    if (extraChannel.getBoolean("selected") && "HBO".equals(extraChannel.getString("code"))) {
        return 5;
    }
}

return 0;