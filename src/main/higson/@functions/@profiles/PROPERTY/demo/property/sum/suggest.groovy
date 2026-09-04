/*
#Full code: demo.property.sum.suggest

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Suma murow jako iloczyn metrazu i stawki odtworzeniowej, sumy pochodne jako jej ulamek. Mechanizm rynkowy - OWU Generali par. 27."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def area = ctx.getDecimal('quote.property.area')
if (area == null || area <= 0) {
	throw new RuntimeException('Brak powierzchni uzytkowej - nie da sie wyliczyc sumy ubezpieczenia')
}

// getDecimal, nie getNumber: brak stawki ma byc bledem, a nie cicha suma zero.
def rate = higson.getDecimal('demo.property.sum.ratePerSqm', ctx)
if (rate == null) {
	throw new RuntimeException('Brak stawki odtworzeniowej dla tej lokalizacji i standardu')
}

def building = math.round(area * rate, 0)
def standard = ctx.getString('quote.property.finishStandard')

def fixturesRatio = higson.getDecimal('demo.property.sum.derivedRatio',
		higson.createContext('cover.code', 'FIXTURES', 'quote.property.finishStandard', standard))
def contentsRatio = higson.getDecimal('demo.property.sum.derivedRatio',
		higson.createContext('cover.code', 'CONTENTS', 'quote.property.finishStandard', standard))

return type.paramValue()
		.withCodes('building', 'fixtures', 'contents', 'rate')
		.withTypes('number', 'number', 'number', 'number')
		.addValues(building,
				math.round(building * fixturesRatio, 0),
				math.round(building * contentsRatio, 0),
				rate)
		.create()