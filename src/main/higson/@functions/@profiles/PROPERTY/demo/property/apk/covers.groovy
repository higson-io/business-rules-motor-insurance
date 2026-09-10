/*
#Full code: demo.property.apk.covers

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Same kody pokryc wyciagniete z apk.needs - to, czego szuka sie w skladzie pakietu. Istnieje po to, zeby apk.indicated i apk.alternative nie parsowaly listy potrzeb kazda po swojemu."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def needs = higson.call('demo.property.apk.needs', ctx)
def covers = new HashSet()
if (needs.isEmpty()) {
	return covers
}
for (def line in needs.split(';')) {
	def sep = line.indexOf('|')
	if (sep > 0) {
		covers.add(line.substring(0, sep))
	}
}
return covers