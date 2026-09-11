/*
#Full code: demo.property.cover.sum

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Suma pokrycia nazwanego w cover.code - JEDEN ODCZYT ATRYBUTU. O tym, skad suma sie bierze, mowi wskaznik SUM na samym pokryciu: mury i powodz pokazuja sum.building, ruchomosci i kradziez sum.contents, OC sum.liability. Assistance nie ma tego atrybutu wcale, bo nie ma sumy - i dlatego odczyt musi znosic jego brak. Do 11 wrzesnia 2026 stal tu czterodrozny switch po atrybutach SUM_BASIS i SUM_REFERENCE; oba usuniete, bo tlumaczyly to, co wskaznik mowi sam."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def coverCode = ctx.getString('cover.code')
if (coverCode == null) {
	return null
}

def cover = domain.get('PROPERTY', '/PRODUCTS[HOME]/COVERS[' + coverCode + ']')
if (cover.getAttribute('SUM') == null) {
	return null
}
return cover.getAttrDecimal('SUM', ctx)