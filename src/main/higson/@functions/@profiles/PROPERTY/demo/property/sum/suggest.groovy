/*
#Full code: demo.property.sum.suggest

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Suma murow jako iloczyn metrazu i stawki odtworzeniowej, sumy pochodne jako jej ulamek. Mechanizm rynkowy - OWU Generali par. 27. Trzy sumy naraz, bo licza sie na poziomie calej wyceny, zanim wiadomo cokolwiek o pokryciach: pokrycie bierze potem swoja z kontekstu przez SUM_BASIS."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def area = ctx.getDecimal('quote.property.area')
if (area == null || area <= 0) {
	throw new RuntimeException('Brak powierzchni uzytkowej - nie da sie wyliczyc sumy ubezpieczenia')
}

def rate = higson.getDecimal('demo.property.sum.ratePerSqm', ctx)
def building = math.round(area * rate, 0)

def ratios = higson.getValue('demo.property.sum.derivedRatio', ctx)
def fixturesRatio = ratios.get(0, 'fixtures')
def contentsRatio = ratios.get(0, 'contents')

def fixtures = math.round(building * fixturesRatio, 0)
def contents = math.round(building * contentsRatio, 0)

return [building: building, fixtures: fixtures, contents: contents]