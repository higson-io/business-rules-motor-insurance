/*
#Full code: demo.property.apk.floodWaiting

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "KARENCJA albo BEZ_KARENCJI. Czyta tabele karencji pokrycia powodziowego. Ciaglosc ochrony z poprzedniej polisy znosi karencje - i to jest INNE ZDANIE dla klienta, a nie to samo z zerem dni."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def cover = domain.get('PROPERTY', '/PRODUCTS[HOME]/COVERS[FLOOD]')
def days = cover.getAttribute('WAITING_PERIOD').getValue(ctx).getInteger('days')
return days != null && days > 0 ? 'KARENCJA' : 'BEZ_KARENCJI'