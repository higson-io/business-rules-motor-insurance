/*
#Full code: demo.property.cover.sum

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Suma ubezpieczenia JEDNEGO pokrycia. Do 7 wrzesnia 2026 ta reguła stała w Javie jako switch po kodzie pokrycia plus trzy przypisania na sztywno (kradziez=ruchomosci, powodz=mury, assistance=0) - czyli ksztalt produktu byl w kodzie, a nie w konfiguracji. Teraz rozstrzyga tabela cover.sumBasis, a to, ile wynosi suma, nadal liczy sum.suggest."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def code = ctx.getString('cover.code')
if (code == null) {
	return null
}

def row = higson.getValue('demo.property.cover.sumBasis', higson.createContext('cover.code', code))
def basis = row.get(0, 'basis')

// Suma wyliczona dla tego pokrycia - mury z metrazu i stawki, elementy stale i ruchomosci
// jako ulamek murow. Wszystkie trzy stoja juz w kontekscie, policzone przez sum.suggest.
if ('COMPUTED' == basis) {
	return ctx.getDecimal('quote.sums.' + code.toLowerCase())
}

// Pokrycie ubezpiecza TO SAMO mienie co inne, wiec idzie na jego sumie. Kradziez na sumie
// ruchomosci, powodz na sumie murow - tak jest w OWU na polskim rynku i tak liczy sie skladka.
if ('DERIVED' == basis) {
	def reference = row.get(0, 'reference')
	if (reference == null || reference.isEmpty()) {
		throw new RuntimeException('Pokrycie ' + code + ' ma sume pochodna, ale nie wskazano od czego')
	}
	return ctx.getDecimal('quote.sums.' + reference.toLowerCase())
}

// Suma gwarancyjna z katalogu wariantu. OC w zyciu prywatnym nie ma nic wspolnego z metrazem
// mieszkania - klient wybiera ja razem z wariantem.
if ('CATALOGUE' == basis) {
	return higson.getDecimal('demo.property.variant.liabilitySum', ctx)
}

// NONE - pokrycie bez sumy, ograniczone samym limitem. Assistance kupuje usluge, nie transfer
// ryzyka, wiec suma nie ma tu sensu i null jest odpowiedzia, a nie brakiem odpowiedzi.
return null