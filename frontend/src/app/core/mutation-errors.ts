import { Injectable, signal } from '@angular/core';

/**
 * Ostatnia nieudana mutacja, dla całej aplikacji.
 *
 * ⛔ Osobna usługa, a nie pole w sklepie. Wcześniej `mutationError` był polem `QuoteStore`, i to
 * miało dwa skutki, oba złe. Po pierwsze, interceptor wołał `inject(QuoteStore)` przy KAŻDYM PUT-cie
 * — także w demo życiowym i majątkowym — więc pierwsza zmiana w tamtych demach powoływała sklep
 * motoryzacyjny i wysyłała jego zapytania. Po drugie, `LifeStore.mutationError` i
 * `PropertyStore.mutationError` istniały, ale nikt do nich nie pisał: nieudany PUT w tych dwóch
 * demach ginął po cichu, czyli dokładnie ta wada, którą DR-06 naprawił dla motoryzacyjnego.
 *
 * ⛔ Celowo wąska (C-7): bez ponowień, bez wycofywania wartości, bez osobnej powierzchni UI.
 * Komunikat kasuje się w chwili startu następnej mutacji, więc baner zawsze opisuje ostatnią próbę,
 * zamiast zostawać nagrobkiem.
 */
@Injectable({ providedIn: 'root' })
export class MutationErrors {
	readonly message = signal<string | null>(null);
}
