/*
#Full code: demo.property.sum.building

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Suma murow i konstrukcji, policzona wczesniej z metrazu i stawki odtworzeniowej. Wskazuja na nia DWA pokrycia: mury ubezpieczaja sie na niej wprost, a powodz - bo powodz niszczy te sama konstrukcje. Wskaznik na elemencie mowi to sam, bez atrybutu tlumaczacego, skad suma sie bierze."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

return ctx.getDecimal('quote.sums.building')