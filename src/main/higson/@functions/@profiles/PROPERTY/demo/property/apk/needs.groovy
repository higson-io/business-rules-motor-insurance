/*
#Full code: demo.property.apk.needs

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Jedyne miejsce, w ktorym z odpowiedzi klienta powstaje lista potrzeb. Tabela apk.answer mowi, co znaczy pojedyncza odpowiedz; ta funkcja niesie regule ROZSTRZYGANIA KOLIZJI: gdy dwie odpowiedzi wskazuja to samo pokrycie, wygrywa ta o wyzszej wadze, bo tresc potrzeby ma byc jedna i ma byc ta mocniejsza. Zwraca POKRYCIE|tresc|waga rozdzielone srednikiem, wiec tresc potrzeby nie moze zawierac znaku | ani srednika."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def answers = ctx.getString('quote.apk.answers')
if (answers == null || answers.isEmpty()) {
	return ''
}

def strongestByCover = new LinkedHashMap()

for (def pair in answers.split(';')) {
	def sep = pair.indexOf('=')
	if (sep < 1) {
		continue
	}

	def questionCode = pair.substring(0, sep)
	def answerValue = pair.substring(sep + 1)
	def answerCtx = higson.createContext('question.code', questionCode, 'answer.value', answerValue)
	def row = higson.getValue('demo.property.apk.answer', answerCtx)

	def need = row.get(0, 'need')
	def cover = row.get(0, 'cover')
	if (need == null || need.toString().isEmpty() || cover == null || cover.toString().isEmpty()) {
		continue
	}

	def weight = row.get(0, 'weight')
	if (weight == null) {
		weight = 0
	}

	def coverCode = cover.toString()
	def current = strongestByCover.get(coverCode)
	if (current == null || current.weight < weight) {
		strongestByCover.put(coverCode, [need: need.toString(), weight: weight])
	}
}

def lines = []
for (def entry in strongestByCover.entrySet()) {
	lines.add(entry.key + '|' + entry.value.need + '|' + entry.value.weight)
}
return lines.join(';')