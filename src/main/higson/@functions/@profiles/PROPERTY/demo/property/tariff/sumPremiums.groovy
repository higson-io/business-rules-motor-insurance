/*
#Full code: demo.property.tariff.sumPremiums

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = ""

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "breakdown"
type = "STRING"
*/

if (breakdown == null || breakdown.isEmpty()) {
	return 0
}
def total = 0
for (def line in breakdown.split(';')) {
	total = total + new BigDecimal(line.split('=')[1])
}
return math.round(total, 2)