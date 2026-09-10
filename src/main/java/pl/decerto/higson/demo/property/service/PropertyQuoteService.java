package pl.decerto.higson.demo.property.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import pl.decerto.higson.demo.property.domain.PropertyQuote;
import pl.decerto.higson.demo.property.example.Property;

/**
 * Trzyma agregat wniosku między żądaniami — tak samo jak QuoteService w profilu motor.
 *
 * ⛔ Świadomie NIE czyta konfiguracji z Higsona przy tworzeniu sesji. Katalog pytań zależy od
 * wersji regionu, a ta od daty polisy, którą klient może jeszcze zmienić. Odczyt tutaj sprawiłby,
 * że przesunięcie daty niczego by nie zmieniło — dokładnie ta pułapka wywróciła demo LIFE.
 */
@Service
@SessionScope
public class PropertyQuoteService {

	private final Property property;

	private PropertyQuote sessionQuote;

	public PropertyQuoteService(Property property) {
		this.property = property;
	}

	@PostConstruct
	public void initializeQuote() {
		sessionQuote = property.createQuote();
	}

	public PropertyQuote getQuote() {
		return sessionQuote;
	}

	/**
	 * Nowy proces — od pustego wniosku.
	 *
	 * ⛔ Buduje wniosek OD NOWA, a nie czyści pola po jednym. Wniosek niesie odpowiedzi, sumy,
	 * wybrany wariant, numer oferty i stan wystawienia; czyszczenie po polu wymagałoby pamiętania
	 * o każdym nowym, a przeoczone zostawałoby na ekranie następnej prezentacji. To jest dokładnie
	 * ten rodzaj resztki, o który poszło zgłoszenie: „na demo może to być mylące, jak nam coś
	 * zostanie".
	 *
	 * ⚠️ Sesja HTTP zostaje ta sama — wymieniamy jej zawartość, nie ją samą. Unieważnienie sesji
	 * wywróciłoby też pozostałe dwa dema, które dzielą tę samą sesję i o niczym nie wiedzą.
	 */
	public void reset() {
		sessionQuote = property.createQuote();
	}
}
