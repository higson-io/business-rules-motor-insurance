/*
#Full code: demo.life.underwriting.evaluate

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Pelna ocena ryzyka. DECLINE nie jest obnizane do REFER przez limit sumy, a prog badan liczy sie zawsze - obie galezie schodza sie przed nim."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def resolved = higson.call('demo.life.underwriting.resolveDecision', ctx)
def decision = resolved.get(0, 'decision')
def severity = resolved.get(0, 'severity')
def reasonInternal = resolved.get(0, 'reasonInternal')
def message = resolved.get(0, 'message')
def binding = resolved.get(0, 'binding')

def mainSum = ctx.getDecimal('quote.policy.mainSum')
def maxSum = higson.getDecimal('demo.life.underwriting.sumLimit', ctx)

// Suma powyzej limitu zawodowego podnosi do REFER, ale NIGDY nie obniza DECLINE.
if (maxSum != null && mainSum > maxSum && severity < 2) {
	decision = 'REFER'
	severity = 1
	reasonInternal = 'Suma powyzej limitu dla klasy zawodu'
	def refer = higson.getValue('demo.life.underwriting.customerMessage',
			higson.createContext('underwriting.decision', 'REFER'))
	message = refer.get(0, 'message')
	binding = refer.get(0, 'binding')
}

// Prog badan liczy sie ZAWSZE, takze dla wnioskow podniesionych do REFER powyzej.
// W pierwszej wersji flow ta galaz omijala prog i zwracala medicalRequired = false.
def maxNoExam = higson.getDecimal('demo.life.underwriting.medicalExamThreshold', ctx)
def medicalRequired = maxNoExam != null && mainSum > maxNoExam

return type.paramValue()
		.withCodes('decision', 'severity', 'reasonInternal', 'message', 'binding', 'medicalRequired')
		.withTypes('string', 'number', 'string', 'string', 'boolean', 'boolean')
		.addValues(decision, severity, reasonInternal, message, binding, medicalRequired)
		.create()