/*
#Full code: demo.property.zone.resolve

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Strefa powodziowa i kradziezowa dla adresu. Klucz obu tabel - obszar kodu pocztowego - wyprowadzaja one same przez demo.property.util.postcodeArea, wiec ta funkcja nie buduje juz zadnego kontekstu. Mieszkanie nad parterem liczy sie jako strefa powodziowa Z0 niezaleznie od adresu - OWU Generali par. 21 ust. 2."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def floodZone = higson.getString('demo.property.zone.flood', ctx)
def theftRow = higson.getValue('demo.property.zone.theft', ctx)
def theftZone = theftRow.get(0, 'zone')

def propertyKind = ctx.getString('quote.property.kind')
def floor = ctx.getInteger('quote.property.floor')
def flatAboveGroundFloor = 'FLAT' == propertyKind && floor != null && floor > 0
def effectiveFloodZone = flatAboveGroundFloor ? 'Z0' : floodZone

return [flood: effectiveFloodZone, theft: theftZone]