/*
#Full code: demo.property.tariff.coverPremiums

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Rozbicie skladki na pokrycia wchodzace w sklad pakietu. Sklad czytany Z DRZEWA jako referencje do pokryc. Wzor na skladke jednego pokrycia stoi w demo.property.cover.premium i tylko tam; ta funkcja jest petla, nie taryfa. Przekazuje dalej WLASNY kontekst, bez budowania nowego - kazde pokrycie ma tabele kluczowana czym innym i kontekst sklejany recznie musialby znac je wszystkie."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def variantCode = ctx.getString('variant.code')
def pack = domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + variantCode + ']')
def members = pack.getAll('COVERS')

def lines = []
for (def member in members) {
	def coverCode = member.getCode()
	def premium = higson.call('demo.property.cover.premium', ctx, coverCode)
	lines.add(coverCode + '=' + premium)
}
return lines.join(';')