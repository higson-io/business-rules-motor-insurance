/*
#Full code: demo.property.tariff.coverPremiums

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Rozbicie skladki na pokrycia wchodzace w sklad wariantu. Sklad czytany Z DRZEWA - pokrycia wisza pod pakietem w kolekcji COVERS. Wzor na skladke JEDNEGO pokrycia stoi w demo.property.cover.premium i tylko tam; ta funkcja jest petla, nie taryfa."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "riskFactor"
type = "BIG_DECIMAL"
*/

def variant = ctx.getString('variant.code')
def lines = []

// ⛔ Kontekst dla pojedynczego pokrycia budujemy JAWNIE. higson.createContext tworzy kontekst
// od zera, wiec funkcja liczaca sume musi dostac wszystko, czego potrzebuje: kod pokrycia,
// kod wariantu (suma gwarancyjna OC bierze sie z katalogu wariantu) i trzy sumy wyliczone
// przez sum.suggest. Trzy wystarcza: kradziez i powodz sa sumami pochodnymi od ruchomosci
// i murow, OC idzie z katalogu, a assistance sumy nie ma wcale.
def building = ctx.getDecimal('quote.sums.building')
def fixtures = ctx.getDecimal('quote.sums.fixtures')
def contents = ctx.getDecimal('quote.sums.contents')

// Sklad pakietu jest POD PAKIETEM w drzewie - petla idzie po tym, co widac po rozwinieciu
// galezi, a nie po tabeli par (pokrycie, wariant).
for (def member in domain.get('PROPERTY', '/PRODUCTS[HOME]/VARIANTS[' + variant + ']').getAll('COVERS')) {
	def code = member.getCode()

	def coverCtx = higson.createContext(
			'cover.code', code,
			'variant.code', variant,
			'quote.sums.building', building,
			'quote.sums.fixtures', fixtures,
			'quote.sums.contents', contents)

	lines.add(code + '=' + higson.call('demo.property.cover.premium', coverCtx, riskFactor))
}

return lines.join(';')