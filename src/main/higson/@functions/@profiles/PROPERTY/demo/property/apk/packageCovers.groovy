/*
#Full code: demo.property.apk.packageCovers

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Jedna definicja pojecia \\"co pakiet pokrywa\\". Sklad bierze z drzewa, ale ODRZUCA pokrycia niedostepne w tej sytuacji - kradziez bez zabezpieczen, powodz w strefie najwyzszego zagrozenia. Do 8 wrzesnia 2026 filtr mial tylko Java przy ocenie pakietu, a reguly wybierajace pakiet go nie mialy: rekomendacja wybierala wariant liczac kradziez jako pokryta, po czym ta sama kradziez raportowala sie jako brakujaca. Wybor i ocena szly po dwoch roznych definicjach tego samego."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "variantCode"
type = "STRING"
*/

def covers = new HashSet()
if (variantCode == null) {
	return covers
}

for (def member in domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + variantCode + ']').getAll('COVERS')) {
	def code = member.getCode()
	def cover = domain.get('PROPERTY', '/PRODUCTS[HOME]/COVERS[' + code + ']')
	if (Boolean.TRUE.equals(cover.getAttrBoolean('IS_AVAILABLE', ctx))) {
		covers.add(code)
	}
}
return covers