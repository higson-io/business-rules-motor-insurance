/*
#Full code: demo.property.apk.floodDeductible

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "TAK albo NIE. Czyta warunki pokrycia powodziowego - w strefie Z2 sprzedajemy tylko z narzucona franszyza. Kwote podstawia aplikacja, tutaj chodzi wylacznie o to, czy zdanie ma pasc."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def cover = domain.get('PROPERTY', '/PRODUCTS[HOME]/COVERS[FLOOD]')
def deductible = cover.getAttribute('DEDUCTIBLE').getValue(ctx).getBigDecimal('minDeductible')
return deductible != null && deductible.signum() > 0 ? 'TAK' : 'NIE'