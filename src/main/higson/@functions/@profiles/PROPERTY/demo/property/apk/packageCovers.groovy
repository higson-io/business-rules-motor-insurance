/*
#Full code: demo.property.apk.packageCovers

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Jedna definicja pojecia \\"co pakiet pokrywa\\" - sklad wprost z drzewa. Bez filtrowania po dostepnosci: pakiet, ktory ma choc jedno pokrycie niedostepne, w ogole nie wchodzi do rozwazan, bo odsiewa go wczesniej demo.property.variant.available. Kazdy pakiet, ktory tu trafia, ma wszystkie pokrycia dostepne."

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

def pack = domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + variantCode + ']')
def members = pack.getAll('COVERS')

for (def member in members) {
	covers.add(member.getCode())
}
return covers