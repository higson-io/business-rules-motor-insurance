/*
#Full code: demo.property.tariff.coverPremiums

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Rozbicie skladki na pokrycia wchodzace w sklad pakietu. Sklad czytany Z DRZEWA jako referencje do pokryc. Wzor na skladke jednego pokrycia stoi w demo.property.cover.premium i tylko tam; ta funkcja jest petla, nie taryfa. Przekazuje dalej WLASNY kontekst, bez budowania nowego - kazde pokrycie ma tabele kluczowana czym innym i kontekst sklejany recznie musialby znac je wszystkie. Linie wychodza w kolejnosci POSITION pokrycia, czyli tej samej, w ktorej pokrycia stoja na ofercie."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def variantCode = ctx.getString('variant.code')
def pack = domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + variantCode + ']')
def members = pack.getAll('COVERS')

// ⛔ Kolejnosc bierze sie z POSITION pokrycia, a nie z kolejnosci, w jakiej drzewo oddaje
// referencje - ta jest nieuporzadkowana i potrafi sie zmienic bez zadnej zmiany w konfiguracji.
// Rozbicie skladki jest czytane przez czlowieka i porownywane przez test regulowy; jedno i drugie
// wymaga, zeby te same dane dawaly ten sam lancuch. POSITION to ten sam atrybut, ktory ustawia
// kolejnosc pokryc na ofercie, wiec rozbicie idzie w kolejnosci, ktora klient widzi na ekranie.
def lines = []
for (def member in members) {
	def coverCode = member.getCode()
	def cover = domain.get('PROPERTY', '/PRODUCTS[HOME]/COVERS[' + coverCode + ']')
	def premium = higson.call('demo.property.cover.premium', ctx, coverCode)
	lines.add([position: cover.getAttrDecimal('POSITION', ctx), text: coverCode + '=' + premium])
}
lines.sort { a, b -> a.position <=> b.position }

def ordered = []
for (def line in lines) {
	ordered.add(line.text)
}
return ordered.join(';')