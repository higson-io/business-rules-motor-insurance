/*
#Full code: demo.life.util.insuredCount

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Liczba ubezpieczonych w fazie oferty, gdy nie znamy jeszcze rzeczywistych przystapien."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def head = ctx.getNumber('quote.group.headcount')
def part = ctx.getNumber('quote.group.participation')
def count = math.round(head * part / 100, 0)
return count < 1 ? 1 : count