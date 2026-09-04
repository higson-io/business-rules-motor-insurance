/*
#Full code: demo.life.underwriting.decision

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Cienka nakladka zwracajaca sama decyzje. Flow rozgalezia sie po zmiennych skalarnych, a bez EXPRESSION nie rozpakuje wielowartosciowego wyniku evaluate."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

return higson.call('demo.life.underwriting.evaluate', ctx).get(0, 'decision')