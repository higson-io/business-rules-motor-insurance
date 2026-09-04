/*
#Full code: demo.life.group.insured

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = "Liczba ubezpieczonych obowiazujaca w tej fazie. Rozstrzyga, ktore z dwoch zrodel liczby jest wlasciwe - nie liczy jej sam."

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

// Ta funkcja NIE ma wlasnego wzoru. Obie liczby maja swoje miejsca - enrolmentStats liczy je
// z rzeczywistych przystapien, util.insuredCount z deklaracji pracodawcy - a tutaj zapada wylacznie
// rozstrzygniecie, ktora z faz obowiazuje. Dokladnie ten sam rozdzial robi demo.life.group.premiums
// u siebie, bo potrzebuje przy okazji participation i topVariantShare.
def enrolment = ctx.get('quote.group.enrolment')

if (enrolment == null || enrolment.isEmpty()) {
	// Faza oferty: pracodawca deklaruje stan i partycypacje, wiec liczba jest ich iloczynem.
	return higson.call('demo.life.util.insuredCount', ctx)
}

// Faza programu: liczy sie to, ile osob faktycznie przystapilo.
return higson.call('demo.life.group.enrolmentStats', ctx).get(0, 'insuredCount')