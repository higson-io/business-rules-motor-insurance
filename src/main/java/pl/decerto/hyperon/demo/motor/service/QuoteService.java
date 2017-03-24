package pl.decerto.hyperon.demo.motor.service;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;
import pl.decerto.hyperon.demo.motor.domain.Quote;
import pl.decerto.hyperon.demo.motor.example.Motor;

@Service
@SessionScope
public class QuoteService {

	private final Motor motor;

	private Quote sessionQuote;

	@Autowired
	public QuoteService(Motor motor) {
		this.motor = motor;
	}

	@PostConstruct
	public void initializeQuote() {
		sessionQuote = motor.createQuote();
	}

	public Quote getQuote() {
		return sessionQuote;
	}
}
