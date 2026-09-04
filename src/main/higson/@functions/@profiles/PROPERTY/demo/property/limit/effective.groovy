/*
#Full code: demo.property.limit.effective

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Wzorzec min(ulamek x suma, kwota) z OWU PZU par. 46 ust. 3 - gorna granica to NAJNIZSZY z limitow, do ktorych przedmiot sie kwalifikuje."

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

def code = ctx.getString('cover.code')
def base = ctx.getDecimal('quote.sums.' + code.toLowerCase())
if (base == null) {
	return null
}
def fromRatio = math.round(base * capRatio, 0)
if ('RATIO' == capKind) {
	return fromRatio
}
return fromRatio < capAbsolute ? fromRatio : capAbsolute