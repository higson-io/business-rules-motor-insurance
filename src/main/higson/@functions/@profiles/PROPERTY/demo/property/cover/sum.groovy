/*
#Full code: demo.property.cover.sum

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Suma ubezpieczenia JEDNEGO pokrycia. Bierze kod pokrycia ze sciezki cover.code, bo wisi na pokryciu jako atrybut SUM, a atrybut wola funkcje z samym kontekstem. Skad bierze sie suma, mowi atrybut SUM_BASIS: COMPUTED z metrazu, DERIVED na sumie innego pokrycia (kradziez na ruchomosciach, powodz na murach), CATALOGUE z sumy gwarancyjnej pakietu, NONE zadna."

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

if ('COMPUTED' == basis) {
	def ownSumPath = 'quote.sums.' + coverCode.toLowerCase()
	return ctx.getDecimal(ownSumPath)
}

if ('DERIVED' == basis) {
	def reference = cover.getAttrString('SUM_REFERENCE', ctx)
	if (reference == null || reference.isEmpty()) {
		throw new RuntimeException('Pokrycie ' + coverCode + ' ma sume pochodna, ale nie wskazano od czego')
	}
	def referencedSumPath = 'quote.sums.' + reference.toLowerCase()
	return ctx.getDecimal(referencedSumPath)
}

if ('CATALOGUE' == basis) {
	def variantCode = ctx.getString('variant.code')
	if (variantCode == null) {
		return null
	}
	def variant = domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + variantCode + ']')
	return variant.getAttrDecimal('LIABILITY_SUM', ctx)
}

return null