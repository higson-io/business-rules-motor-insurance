/*
#Full code: demo.life.individual.maxSumAssured

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

// UWAGA: selektor kolumny "tabela [kolumna]" dziala TYLKO w atrybutach domeny ($p kod [kol]).
// W API Groovy konczy sie bledem "Parameter not found in user perspective" - sprawdzone testem.
// Wielokolumnowa tabele czyta sie przez getValue i get(0, 'kolumna').

// Pulap produktowy ma trzy rodzaje, bo benefitKind decyduje, czym jest liczba w polu suma:
// RATIO skaluje sie suma glowna, ABSOLUTE jest kwota, NONE nie ogranicza niczego.
def productRow = higson.getValue('demo.life.individual.limit.againstMainSum', ctx)
def capKind = productRow.get(0, 'capKind')
def capValue = productRow.get(0, 'capValue')
def mainSum = ctx.getDecimal('quote.policy.mainSum')

def productCap = null
if (capKind == 'RATIO') {
	productCap = capValue * mainSum
} else if (capKind == 'ABSOLUTE') {
	productCap = capValue
}

// Pulap dochodowy dotyczy tylko swiadczen zastepujacych dochod. Pole jest opcjonalne, wiec jego
// brak nie moze blokowac wyceny umow, ktorych nie dotyczy.
def incomeRow = higson.getValue('demo.life.individual.limit.againstIncome', ctx)
def basis = incomeRow.get(0, 'basis')
def ratio = incomeRow.get(0, 'ratio')
def income = ctx.getDecimal('quote.insured.monthlyIncome')

def incomeCap = null
if (income != null && income > 0) {
	if (basis == 'DAILY_INCOME') {
		incomeCap = ratio * income / 30
	} else if (basis == 'ANNUAL_INCOME') {
		incomeCap = ratio * income * 12
	}
}

// min z niepustych, a nie "ten wlasciwszy" - wybor jednego zostawilby druga furtke otwarta.
if (productCap == null) {
	return incomeCap
}
if (incomeCap == null) {
	return productCap
}
return productCap < incomeCap ? productCap : incomeCap