/*
#Full code: demo.life.group.premiums

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

// UWAGA: higson.createContext NIE konwertuje typow. Wartosc musi miec dokladnie ten typ, co
// poziom wejsciowy tabeli - Double pod poziom integer konczy sie IllegalArgumentException.
def head = ctx.getInteger('quote.group.headcount')
def enrolment = ctx.get('quote.group.enrolment')
def hasEnrolment = enrolment != null && !enrolment.isEmpty()

def insured
def participation
def mixFactor
if (hasEnrolment) {
	def stats = higson.call('demo.life.group.enrolmentStats', ctx)
	insured = stats.get(0, 'insuredCount')
	participation = stats.get(0, 'participation')
	mixFactor = higson.getDecimal('demo.life.group.factor.variantMix',
			higson.createContext('quote.group.topVariantShare', stats.get(0, 'topVariantShare')))
} else {
	// Faza oferty: bez rozkladu wyborow druga os antyselekcji nie ma czego mierzyc, wiec jest
	// neutralna. Regula siedzi TUTAJ, a nie w wierszu domyslnym tabeli - wiersz "* - *" pasowalby
	// takze do realnych wartosci i dokladal drugie, bledne trafienie.
	insured = higson.call('demo.life.util.insuredCount', ctx)
	participation = ctx.getDecimal('quote.group.participation')
	mixFactor = 1
}

def groupFactor = higson.getDecimal('demo.life.group.factor.averageAge', ctx) *
		higson.getDecimal('demo.life.group.factor.industry', ctx) *
		higson.getDecimal('demo.life.group.factor.headcount', ctx) *
		higson.getDecimal('demo.life.group.factor.familyExtension', ctx) *
		higson.getDecimal('demo.life.group.factor.participation',
				higson.createContext('quote.group.participation', participation)) *
		mixFactor

def result = type.paramValue()
		.withCodes('variant', 'perEmployee', 'monthlyTotalIfAll')
		.withTypes('string', 'number', 'number')

def variants = domain.get('LIFE', '/PRODUCT_LINES[GROUP]').getAll('VARIANTS')

for (def variant in variants) {
	def code = variant.getCode()
	def variantCtx = higson.createContext('variant.code', code,
			'quote.group.headcount', head)

	if (!higson.getBoolean('demo.life.group.variant.availability', variantCtx)) {
		continue
	}

	def base = higson.getDecimal('demo.life.group.tariff.basePremium', variantCtx)
	def perEmployee = math.round(base * groupFactor, 2)
	// monthlyTotalIfAll, nie monthlyTotal: przy wyborze per pracownik to hipoteza
	// "ile, gdyby wszyscy wybrali ten wariant", a nie koszt programu.
	result.addValues(code, perEmployee, math.round(perEmployee * insured, 2))
}

return result.create()