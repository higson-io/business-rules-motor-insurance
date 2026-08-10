/*
#Full code: pl.decerto.demo.telco.package.channels.count

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

def packageCode = ctx.getString('package.code')

if ("COMFORT".equals(packageCode)) {
    return 114
} else if ("COMFORT_WITH_HBO".equals(packageCode)) {
    return 117
} else {
    return 0
}