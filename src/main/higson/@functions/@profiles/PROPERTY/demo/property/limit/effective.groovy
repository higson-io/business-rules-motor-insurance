/*
#Full code: demo.property.limit.effective

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Efektywny limit odpowiedzialnosci pokrycia. Rodzaj limitu NIE JEST osobna kolumna - wynika z tego, ktore z dwoch wartosci sa podane: sam ulamek daje limit procentowy, sama kwota kwotowy, obie nizsza z dwoch, zadna brak limitu. Ulamek i kwote czyta z atrybutow pokrycia, czyli z jego wlasnej tabeli."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def code = ctx.getString('cover.code')
def cover = domain.get('PROPERTY', '/PRODUCTS[HOME]/COVERS[' + code + ']')

def ratio = cover.getAttrDecimal('CAP_RATIO', ctx)
def absolute = cover.getAttrDecimal('CAP_ABSOLUTE', ctx)
def hasRatio = ratio != null && ratio > 0
def hasAbsolute = absolute != null && absolute > 0

// null, a nie 0 - zero znaczyloby "brak ochrony", a nie "brak limitu".
if (!hasRatio && !hasAbsolute) {
	return null
}
if (!hasRatio) {
	return absolute
}

// ⛔ Suma przez cover.sum, a NIE wprost z quote.sums.KOD. Kradziez i powodz nie maja wlasnej
// sumy - ida na sumie ruchomosci i murow - wiec odczyt po kodzie pokrycia zwracal dla nich null
// i limit kradziezy po cichu znikal z oferty.
def base = higson.call('demo.property.cover.sum', ctx)
if (base == null) {
	return hasAbsolute ? absolute : null
}

def fromRatio = math.round(base * ratio, 0)
if (!hasAbsolute) {
	return fromRatio
}
return fromRatio < absolute ? fromRatio : absolute