/*
#Full code: demo.property.limit.effective

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Efektywny limit odpowiedzialnosci pokrycia - nizszy z ulamka sumy i kwoty. Sume bierze z demo.property.cover.sum, czyli z tej samej reguly co reszta systemu; wczesniej siegal wprost do quote.sums.KOD i przez to nie widzial sum pochodnych."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

// UWAGA: selektor "tabela [kolumna]" dziala TYLKO w atrybutach domeny. W API Groovy wielokolumnowa
// tabele czyta sie przez getValue i get(0, 'kolumna').
def row = higson.getValue('demo.property.cover.limitRule', ctx)
def capKind = row.get(0, 'capKind')
if (capKind == null || 'NONE' == capKind) {
	// null, a nie 0 - zero znaczyloby "brak ochrony", a nie "brak limitu".
	return null
}

def capRatio = row.get(0, 'capRatio')
def capAbsolute = row.get(0, 'capAbsolute')
if ('ABSOLUTE' == capKind) {
	return capAbsolute
}

// ⛔ Suma przez cover.sum, a NIE wprost z quote.sums.KOD. Kradziez i powodz nie maja wlasnej
// sumy - ida na sumie ruchomosci i murow - wiec odczyt po kodzie pokrycia zwracal dla nich null
// i limit kradziezy (40% sumy, max 50 000 zl) po cichu znikal z oferty.
def base = higson.call('demo.property.cover.sum', ctx)
if (base == null) {
	return null
}
def fromRatio = math.round(base * capRatio, 0)
if ('RATIO' == capKind) {
	return fromRatio
}
return fromRatio < capAbsolute ? fromRatio : capAbsolute