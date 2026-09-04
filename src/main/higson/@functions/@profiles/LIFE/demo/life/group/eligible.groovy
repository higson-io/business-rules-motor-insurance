/*
#Full code: demo.life.group.eligible

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
*/

// Tabela nie ma wiersza domyslnego i ma matchRequired = false, bo w tabeli o samych kolumnach
// przedzialowych wiersz "* - *" dopasowuje sie ROWNOLEGLE z wlasciwym pasmem i zwraca dwie
// sprzeczne odpowiedzi. Brak dopasowania znaczy "poza zakresem oferty", czyli odmowa.
def row = higson.getValue('demo.life.group.eligibility', ctx)
if (row == null || row.isEmpty()) {
	return false
}
return row.get(0, 'eligible')