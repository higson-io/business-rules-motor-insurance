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
}
