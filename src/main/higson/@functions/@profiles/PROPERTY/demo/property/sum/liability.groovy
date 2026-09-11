/*
#Full code: demo.property.sum.liability

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Suma gwarancyjna OC - jedyna suma, ktora nie bierze sie z nieruchomosci, tylko z KATALOGU PAKIETU: kazdy pakiet niesie wlasna (atrybut LIABILITY_SUM wariantu). Pakiet bez OC nie ma jej wcale i wtedy funkcja oddaje nic."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def variantCode = ctx.getString('variant.code')
if (variantCode == null) {
	return null
}
return domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + variantCode + ']')
		.getAttrDecimal('LIABILITY_SUM', ctx)