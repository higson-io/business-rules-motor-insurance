/*
#Full code: demo.life.group.programCost

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Rzeczywisty koszt programu: suma wazona rozkladem wyborow. Bez rozkladu ta liczba nie istnieje i nie wolno jej zastapic zadnym typowym rozkladem - zwraca wtedy null, a nie zero."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"

[[arguments]]
name = "variants"
type = "OBJECT"
*/

// ⛔ ctx PIERWSZY. Poprzednia deklaracja miala variantPremiums na pozycji 0 i ctx na 1, co silnik
// odrzuca ("Invalid argument data type passed on position 0"). Funkcja nie byla wolana przez nikogo,
// wiec nikt tego nie zauwazyl.
def enrolment = ctx.get('quote.group.enrolment')
if (enrolment == null || enrolment.isEmpty()) {
	// Brak rozkladu to brak tej liczby. Zero byloby twierdzeniem, ze program nic nie kosztuje.
	return null
}

// variants to wynik demo.life.group.premiums - te same wyceny, ktore widzi ekran. Skladanie mapy
// tutaj, zamiast przyjmowania jej z zewnatrz, trzyma zrodlo cen w jednym miejscu.
def priceOf = [:]
for (int i = 0; i < variants.size(); i++) {
	priceOf.put(variants.get(i, 'variant'), variants.get(i, 'perEmployee'))
}

def total = 0
for (def entry in enrolment) {
	def code = entry.getString('variantCode')
	def premium = priceOf.get(code)
	// Wariant, ktory zniknal z oferty, a zostal w deklaracjach, to niezgodnosc konfiguracji
	// z rzeczywistoscia. Ciche zero zanizyloby koszt dokladnie o tych pracownikow, ktorzy sa
	// ubezpieczeni.
	if (premium == null) {
		throw new RuntimeException('Wariant wybrany przez pracownikow nie ma wyceny: ' + code)
	}
	total = total + premium * entry.getNumber('count')
}
return math.round(total, 2)