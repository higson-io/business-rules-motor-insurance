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

def variant = ctx.getString('variant.code')
def lines = []

// Sklad pakietu jest POD PAKIETEM w drzewie - petla idzie po tym, co widac po rozwinieciu
// galezi, a nie po tabeli par (pokrycie, wariant).
for (def member in domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + variant + ']').getAll('COVERS')) {
	def code = member.getCode()
	lines.add(code + '=' + higson.call('demo.property.cover.premium', ctx, code))
}

return lines.join(';')