/*
#Full code: demo.util.multiply

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Arytmetyka dla flow: flow potrafi pobrac wartosc do zmiennej i rozgalezic sie warunkiem, ale nie potrafi mnozyc. BRAK WARTOSCI ZNACZY JEDEN, nie zero - czynnik, ktorego tabela nie zwrocila (bo nie ma wiersza dzikiego, jak wiek budynku i szkodowosc), ma nie wplynac na iloczyn, a nie wyzerowac go. Dzieki temu lancuch mnozen nie potrzebuje rozgalezien na null."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "a"
type = "BIG_DECIMAL"

[[arguments]]
name = "b"
type = "BIG_DECIMAL"
*/

// Element neutralny mnozenia, nie zero. Czynnik nieustalony ma zostawic iloczyn w spokoju.
if (a == null && b == null) {
	return null
}
if (a == null) {
	return b
}
if (b == null) {
	return a
}
return a * b