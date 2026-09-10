/*
#Full code: demo.property.apk.bestCovering

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Drabinka pakietow przejrzana raz: odsiewa te, ktorych nie wolno sprzedac, i wybiera najlepiej dopasowany do potrzeb, a przy remisie tanszy. Argument maxPremium to jedyna roznica miedzy propozycja a alternatywa: ZERO znaczy bez ograniczenia ceny, wartosc dodatnia odrzuca pakiety drozsze od oczekiwanej skladki. Wczesniej ta sama petla stala dwa razy - w apk.indicated i w apk.alternative - i musialy sie zmieniac razem."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "maxPremium"
type = "BIG_DECIMAL"
*/

def required = higson.call('demo.property.apk.covers', ctx)
def floodZone = ctx.getString('quote.zone.flood')
def security = ctx.getString('quote.property.security')
def variants = domain.get('PROPERTY', '/PRODUCTS[HOME]').getAll('VARIANTS')

// ⛔ ZERO, NIE NULL. Wywolanie higson.call(kod, ctx, null) dociera do silnika jako JEDEN argument
// - null wypada po drodze - a walidacja liczby argumentow w runtime konczy sie wtedy bledem
// (Expected: 2, Passed: 1) i wywala cala wycene. Tester Studia tego nie sprawdza, wiec widac to
// dopiero w aplikacji. Zero znaczy: bez ograniczenia ceny.
def capped = maxPremium != null && maxPremium > 0

def best = null
def bestCovered = -1
def bestPosition = null

for (def variant in variants) {
	def code = variant.getCode()
	// ⛔ Waski kontekst musi niesc KAZDE pole, po ktorym kluczuje sie dostepnosc pokrycia:
	// strefe dla powodzi i zabezpieczenia dla kradziezy. Bez ktoregokolwiek tabela pokrycia
	// spada na wiersz gwiazdkowy i pakiet wychodzi dostepny, choc ekran pokaze go inaczej.
	def availabilityCtx = higson.createContext(
			'variant.code', code,
			'quote.zone.flood', floodZone,
			'quote.property.security', security)
	if (!Boolean.TRUE.equals(higson.call('demo.property.variant.available', availabilityCtx))) {
		continue
	}

	if (capped) {
		def premium = ctx.getDecimal('quote.premium.' + code)
		if (premium == null || premium > maxPremium) {
			continue
		}
	}

	def covers = higson.call('demo.property.apk.packageCovers', ctx, code)
	def covered = 0
	for (def cover in required) {
		if (covers.contains(cover)) {
			covered = covered + 1
		}
	}

	def position = variant.getAttrDecimal('POSITION', ctx)
	// Remis w pokryciu: wygrywa tanszy pakiet, czyli nizsza pozycja w drabince.
	if (covered > bestCovered || (covered == bestCovered && position < bestPosition)) {
		best = code
		bestCovered = covered
		bestPosition = position
	}
}
return best