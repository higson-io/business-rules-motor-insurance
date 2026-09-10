/*
#Full code: demo.property.variant.available

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Pakiet wolno sprzedac dokladnie wtedy, gdy WSZYSTKIE jego pokrycia sa dostepne w tej sytuacji. Nie ma dla dostepnosci pakietu osobnej konfiguracji - wynika ona ze skladu pakietu w drzewie i z regul dostepnosci poszczegolnych pokryc. Powodu niedostepnosci demo nie pokazuje, wiec niedostepny pakiet wraca bez zdania wyjasniajacego."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def variantCode = ctx.getString('variant.code')
if (variantCode == null) {
	return true
}

def pack = domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + variantCode + ']')
def members = pack.getAll('COVERS')

for (def member in members) {
	def cover = domain.get('PROPERTY', '/PRODUCTS[HOME]/COVERS[' + member.getCode() + ']')
	def available = cover.getAttrBoolean('IS_AVAILABLE', ctx)
	if (!Boolean.TRUE.equals(available)) {
		return false
	}
}
return true