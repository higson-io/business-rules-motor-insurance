/*
#Full code: demo.property.cover.sum

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Suma ubezpieczenia JEDNEGO pokrycia. Bierze kod pokrycia ze sciezki cover.code, bo wisi na pokryciu jako atrybut SUM, a atrybut wola funkcje z samym kontekstem. Skad bierze sie suma, mowi atrybut SUM_BASIS: COMPUTED z metrazu, DERIVED na sumie innego pokrycia, CATALOGUE z pakietu, NONE zadna."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def coverCode = ctx.getString('cover.code')
if (coverCode == null) {
	return null
}
def cover = domain.get('PROPERTY', '/PRODUCTS[HOME]/COVERS[' + coverCode + ']')
def basis = cover.getAttrString('SUM_BASIS', ctx)

// Suma wyliczona dla tego pokrycia - mury z metrazu i stawki, elementy stale i ruchomosci
// jako ulamek murow. Wszystkie trzy stoja juz w kontekscie, policzone przez sum.suggest.
if ('COMPUTED' == basis) {
	return ctx.getDecimal('quote.sums.' + coverCode.toLowerCase())
}

// Pokrycie ubezpiecza TO SAMO mienie co inne, wiec idzie na jego sumie. Kradziez na sumie
// ruchomosci, powodz na sumie murow - tak jest w OWU na polskim rynku.
if ('DERIVED' == basis) {
	def reference = cover.getAttrString('SUM_REFERENCE', ctx)
	if (reference == null || reference.isEmpty()) {
		throw new RuntimeException('Pokrycie ' + coverCode + ' ma sume pochodna, ale nie wskazano od czego')
	}
	return ctx.getDecimal('quote.sums.' + reference.toLowerCase())
}

// Suma gwarancyjna z katalogu wariantu. OC nie ma nic wspolnego z metrazem mieszkania -
// klient wybiera je razem z pakietem, wiec suma jest WARTOSCIA na pakiecie.
if ('CATALOGUE' == basis) {
	def variant = ctx.getString('variant.code')
	if (variant == null) {
		return null
	}
	return domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + variant + ']')
			.getAttrDecimal('LIABILITY_SUM', ctx)
}

// NONE - pokrycie bez sumy, ograniczone samym limitem.
return null