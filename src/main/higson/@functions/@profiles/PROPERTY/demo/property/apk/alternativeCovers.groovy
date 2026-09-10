/*
#Full code: demo.property.apk.alternativeCovers

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "BRAK, PELNA albo Z_LUKAMI - a gdy propozycja miesci sie w skladce, null, bo alternatywy wtedy nie szukamy. PELNA znaczy, ze tanszy pakiet pokrywa dokladnie te same potrzeby co proponowany, i to jest INNE ZDANIE dla klienta niz alternatywa z lukami."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

if (higson.call('demo.property.apk.premiumVsBudget', ctx) != 'ZA_DROGO') {
	return null
}

def variantCode = ctx.getString('quote.apk.alternative')
if (variantCode == null) {
	return 'BRAK'
}

def pack = domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + variantCode + ']')
def inPackage = new HashSet()
for (def member in pack.getAll('COVERS')) {
	inPackage.add(member.getCode())
}

for (def cover in ctx.getString('quote.apk.covers').split(',')) {
	if (!inPackage.contains(cover)) {
		return 'Z_LUKAMI'
	}
}
return 'PELNA'