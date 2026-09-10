/*
#Full code: demo.property.apk.covers

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Pokrycia, ktorych klient potrzebuje - to, czego szuka sie w skladzie pakietu. Potrzeby powstaja z ATRYBUTOW PYTAN (zgloszone przez klienta) i z tabeli pol przedmiotu (wywnioskowane), wiec skladane sa poza regulami; tutaj przychodza gotowa lista kodow. Funkcja istnieje po to, zeby apk.indicated i apk.alternative nie parsowaly jej kazda po swojemu."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def covers = new HashSet()
def raw = ctx.getString('quote.apk.covers')
if (raw == null || raw.isEmpty()) {
	return covers
}
for (def code in raw.split(',')) {
	covers.add(code)
}
return covers