/*
#Full code: demo.life.group.enrolmentStats

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

// Faza programu: rozklad wyborow pracownikow jest zrodlem prawdy. Partycypacja przestaje byc
// deklaracja pracodawcy i staje sie ilorazem policzonym z rzeczywistych przystapien.
def enrolment = ctx.get('quote.group.enrolment')
def head = ctx.getNumber('quote.group.headcount')
if (enrolment == null || enrolment.isEmpty()) {
	throw new RuntimeException('Faza programu wymaga niepustej listy przystapien')
}

def insured = 0
def topPosition = 0
def topCount = 0

for (def entry in enrolment) {
	def count = entry.getNumber('count')
	insured = insured + count

	// Najbogatszy wariant to ten o najwyzszej POSITION, nie ten o konkretnej nazwie.
	// UWAGA: czytamy parametr wprost z jawnym kontekstem, a nie przez domain.getAttrDecimal.
	// Nawigacja po domenie NIE wstrzykuje kodu elementu do kontekstu, wiec atrybut kluczowany
	// variant.code nie mialby sie z czego rozwiazac - sprawdzone testem.
	def position = higson.getDecimal('demo.life.group.variant.position',
			higson.createContext('variant.code', entry.getString('variantCode')))

	if (position > topPosition) {
		topPosition = position
		topCount = count
	} else if (position == topPosition) {
		topCount = topCount + count
	}
}

if (insured <= 0) {
	throw new RuntimeException('Lista przystapien sumuje sie do zera')
}

return type.paramValue()
		.withCodes('insuredCount', 'participation', 'topVariantShare')
		.withTypes('number', 'number', 'number')
		.addValues(insured,
				math.round(insured * 100 / head, 2),
				math.round(topCount * 100 / insured, 2))
		.create()