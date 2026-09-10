/*
#Full code: demo.property.apk.allCovered

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "TAK albo NIE. Czyta potrzeby klienta z kontekstu i sklad proponowanego pakietu z drzewa. Stoi pod kolumna tabeli uzasadnienia - tabela nie porowna dwoch swoich kolumn ze soba, wiec odpowiedz musi przyjsc jako jedna wartosc."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def needed = ctx.getString('quote.apk.covers')
def variantCode = ctx.getString('quote.apk.proposed')
if (needed == null || needed.isEmpty() || variantCode == null) {
	return null
}

def pack = domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + variantCode + ']')
def inPackage = new HashSet()
for (def member in pack.getAll('COVERS')) {
	inPackage.add(member.getCode())
}

for (def cover in needed.split(',')) {
	if (!inPackage.contains(cover)) {
		return 'NIE'
	}
}
return 'TAK'