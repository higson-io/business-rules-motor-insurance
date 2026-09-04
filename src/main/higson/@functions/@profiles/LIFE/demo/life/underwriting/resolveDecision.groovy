/*
#Full code: demo.life.underwriting.resolveDecision

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Wybiera najciezsza przeslanke z wielowierszowej tabeli regul i dokleja tresc dla klienta. Bez tego decyzja zalezalaby od kolejnosci wierszy w macierzy."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

// demo.life.underwriting.rules jest wielowierszowa z zalozenia: jedna ocena trafia w kilka
// przeslanek naraz. Wybieramy najciezsza po kolumnie severity. Bez tego decyzja zalezalaby od
// kolejnosci wierszy w macierzy, czyli od czegos, czego nikt nie uznaje za regule biznesowa.
def pv = higson.getValue('demo.life.underwriting.rules', ctx)
if (pv.isEmpty()) {
	throw new RuntimeException('Brak regul underwritingowych dla podanego kontekstu')
}

def best = null
for (def row in pv) {
	def severity = row.getDecimal('severity')
	if (best == null || severity > best.getDecimal('severity')) {
		best = row
	}
}

def decision = best.getString('decision')

// Tresc dla klienta czytamy TUTAJ, a nie we flow. Flow nie potrafi zapisac wyliczonej decyzji do
// kontekstu (istnieje tylko util.ctx.get), wiec tabela kluczowana decyzja musi byc odczytana przez
// createContext. Tresc nadal siedzi w tabeli, wiec reasonInternal nigdy nie trafia do klienta.
def messageRow = higson.getValue('demo.life.underwriting.customerMessage',
		higson.createContext('underwriting.decision', decision))

return type.paramValue()
		.withCodes('decision', 'severity', 'reasonInternal', 'message', 'binding')
		.withTypes('string', 'number', 'string', 'string', 'boolean')
		.addValues(decision,
				best.getDecimal('severity'),
				best.getString('reasonInternal'),
				messageRow.get(0, 'message'),
				messageRow.get(0, 'binding'))
		.create()