/*
#Full code: demo.property.cover.sum

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Suma ubezpieczenia JEDNEGO pokrycia. Rozstrzyga tabela cover.sumBasis: suma wyliczona, pochodna od innego pokrycia, z katalogu wariantu albo zadna. Sume gwarancyjna OC czyta z ATRYBUTU pakietu, bo to wartosc nalezaca do pakietu, a nie odpowiedz tabeli."

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
// mieszkania - klient wybiera ja razem z pakietem, wiec jest WARTOSCIA na pakiecie.
if ('CATALOGUE' == basis) {
	def variant = ctx.getString('variant.code')
	if (variant == null) {
		return null
	}
	return domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + variant + ']')
			.getAttrDecimal('LIABILITY_SUM', ctx)
}

// NONE - pokrycie bez sumy, ograniczone samym limitem. Assistance kupuje usluge, nie transfer
// ryzyka, wiec suma nie ma tu sensu i null jest odpowiedzia, a nie brakiem odpowiedzi.
return null