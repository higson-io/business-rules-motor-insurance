/*
#Full code: demo.life.util.ageRatingFactor

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Iloraz stawki bazowej dla wieku ubezpieczonego i dla wieku odniesienia 35 lat. Wymaga dwoch odczytow tej samej tabeli z roznym kontekstem, czego wyrazenie kaskadowe nie zrobi."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def age = ctx.getNumber('quote.insured.entryAge')
def rateNow = higson.getDecimal('demo.life.individual.tariff.baseRate', ctx)
def rateRef = higson.getDecimal('demo.life.individual.tariff.baseRate',
		higson.createContext('quote.insured.entryAge', 35))
// getDecimal, nie getNumber: getNumber zwraca 0.0 przy braku dopasowania, wiec brakujaca stawka
// zamienilaby sie w cicha skladke zerowa zamiast w blad.
if (rateNow == null || rateRef == null || rateRef == 0) {
	throw new RuntimeException('Brak stawki bazowej dla wieku: ' + age)
}
return rateNow / rateRef