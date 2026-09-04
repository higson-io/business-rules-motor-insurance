/*
#Full code: demo.property.risk.factor

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Iloczyn szesciu czynnikow taryfowych. Mnoza sie, a nie zastepuja - kazdy opisuje inne ryzyko."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def factor = higson.getDecimal('demo.property.factor.wallMaterial', ctx) *
		higson.getDecimal('demo.property.factor.occupancy', ctx) *
		higson.getDecimal('demo.property.factor.security', ctx) *
		higson.getDecimal('demo.property.factor.deductible', ctx)

// factor.buildingAge i factor.claims maja same kolumny przedzialowe, wiec NIE maja wiersza
// "* - *" - wiersz calkowicie dziki dopasowalby sie rownolegle z wlasciwym pasmem i silnik
// zwrocilby dwie wartosci. Cena tej decyzji: matchRequired = false i obsluga null tutaj.
def age = higson.getDecimal('demo.property.factor.buildingAge', ctx)
if (age != null) {
	factor = factor * age
}
def claims = higson.getDecimal('demo.property.factor.claims', ctx)
if (claims != null) {
	factor = factor * claims
}

// Strefa kradziezowa jest CZYNNIKIEM, w odroznieniu od powodziowej, ktora jest przelacznikiem
// dostepnosci. Ten sam adres, dwie rozne role.
def theftFactor = higson.getValue('demo.property.zone.theft', ctx).get(0, 'factor')
if (theftFactor != null) {
	factor = factor * theftFactor
}

return math.round(factor, 4)