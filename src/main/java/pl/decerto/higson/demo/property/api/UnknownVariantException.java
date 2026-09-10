package pl.decerto.higson.demo.property.api;

/**
 * Wybrano pakiet, którego nie ma w konfiguracji.
 *
 * ⛔ Bez tego `PUT /property/variant` przyjmował dowolny napis z HTTP 200, a padał dopiero
 * NASTĘPNY `GET`: `cover.sum` szuka wtedy `/PRODUCTS[HOME]/VARIANTS[<kod>]`, żeby wziąć sumę
 * gwarancyjną OC z katalogu wariantu, i nie znajduje elementu. Wniosek zostawał w tym stanie
 * na stałe — każde kolejne żądanie wracało 500 aż do zresetowania procesu.
 */
public class UnknownVariantException extends RuntimeException {

	public UnknownVariantException(String code) {
		super("Nie ma pakietu o kodzie: " + code);
	}
}
