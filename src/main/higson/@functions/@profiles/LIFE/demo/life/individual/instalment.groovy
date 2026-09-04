/*
#Full code: demo.life.individual.instalment

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

// UWAGA: funkcja NIE deklaruje argumentow. Przy zadeklarowanym argumencie ctx przestaje byc
// rozpoznawany jako kontekst i higson.getValue(kod, ctx) traktuje go jako pary klucz-wartosc,
// konczac sie bledem "key-value arguments have to be well paired" - sprawdzone testem.
def premiumMonthly = higson.call('demo.life.individual.monthlyPremium', ctx)

def row = higson.getValue('demo.life.paymentFrequency.factor', ctx)
def factor = row.get(0, 'factor')
def installments = row.get(0, 'installments')

def instalment = math.round(premiumMonthly * 12 * factor / installments, 2)

// Skladka roczna NIE jest liczona osobno - jest wielokrotnoscia zaokraglonej raty. Nie ma
// drugiej liczby, ktora moglaby sie nie zgodzic. Zadna rata nie absorbuje roznicy.
return type.paramValue()
		.withCodes('premiumMonthly', 'instalment', 'installments', 'premiumAnnual')
		.withTypes('number', 'number', 'number', 'number')
		.addValues(premiumMonthly, instalment, installments, instalment * installments)
		.create()