package pl.decerto.higson.demo.life.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import pl.decerto.higson.demo.life.domain.LifeQuote;
import pl.decerto.higson.demo.life.example.Life;

/**
 * Trzyma agregat wniosku między żądaniami — tak samo jak QuoteService w profilu motor.
 * Konfiguracja umów, wariantów i świadczeń pobierana jest z domeny raz, przy tworzeniu sesji.
 */
@Service
@SessionScope
public class LifeQuoteService {

	private final Life life;

	private LifeQuote sessionQuote;

	public LifeQuoteService(Life life) {
		this.life = life;
	}

	@PostConstruct
	public void initializeQuote() {
		sessionQuote = life.createQuote();
	}

	public LifeQuote getQuote() {
		return sessionQuote;
	}
}
