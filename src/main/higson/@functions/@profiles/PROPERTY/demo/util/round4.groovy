/*
#Full code: demo.util.round4

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Arytmetyka dla flow. Cztery miejsca to skala czynnikow taryfowych: iloczyn szesciu czynnikow po dwa miejsca daje ogon, ktory bez zaokraglenia wedruje do skladki i sprawia, ze ta sama wycena policzona dwa razy roznymi droga daje inny grosz."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "value"
type = "BIG_DECIMAL"
*/

if (value == null) {
	return null
}
return math.round(value, 4)