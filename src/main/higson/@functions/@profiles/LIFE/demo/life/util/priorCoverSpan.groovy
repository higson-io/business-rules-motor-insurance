/*
#Full code: demo.life.util.priorCoverSpan

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

// Brak poprzedniej ochrony to najczestszy przypadek, nie blad. Zwracamy zera, zeby tabela
// zaliczenia trafila w wiersz domyslny i nie przyznala niczego.
def from = ctx.getDate('quote.insured.priorCoverFrom')
def to = ctx.getDate('quote.insured.priorCoverTo')
if (from == null || to == null) {
	return type.paramValue()
			.withCodes('priorMonths', 'gapDays')
			.withTypes('integer', 'integer')
			.addValues(0, 0)
			.create()
}

// UWAGA: date.diff(Date, Date, String) NIE ISTNIEJE - sprawdzone testem, RhinoDate zwraca
// MissingMethodException. Uzywamy natywnego odejmowania Date w Groovym, ktore daje liczbe dni.
// Zero importow, zgodnie ze standardem dla cial regul.
def start = ctx.getDate('quote.policy.startDate')
def coverDays = to - from
def gap = start - to

if (coverDays < 0) {
	throw new RuntimeException('Poprzednia ochrona konczy sie przed swoim poczatkiem')
}
// Ochrona konczaca sie PO starcie nowej polisy to nakladanie sie, nie przerwa. Ujemna przerwa
// wpadlaby w przedzial 0-30 przypadkiem, wiec podnosimy ja do zera swiadomie.
if (gap < 0) {
	gap = 0
}

// Miesiace z dni. Granice przedzialow w tabeli zaliczenia to 1 i 6 miesiecy, czyli daleko od
// bledu zaokraglenia dlugosci miesiaca. Typ integer, bo poziomy wejsciowe tabeli sa integer -
// BigDecimal konczy sie IllegalArgumentException w IntegerType.convert.
def months = (int) (coverDays / 30)

return type.paramValue()
		.withCodes('priorMonths', 'gapDays')
		.withTypes('integer', 'integer')
		.addValues(months, (int) gap)
		.create()