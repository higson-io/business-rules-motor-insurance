package pl.decerto.higson.demo.motor.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.decerto.higson.demo.motor.converter.QuoteConverter;
import pl.decerto.higson.demo.motor.api.dto.QuoteDto;
import pl.decerto.higson.demo.motor.domain.Quote;
import pl.decerto.higson.demo.motor.example.Motor;
import pl.decerto.higson.demo.motor.service.QuoteService;

@RestController
@RequestMapping("/quote")
public class QuoteApi {

	private final QuoteService quoteService;

	private final QuoteConverter quoteConverter;

	private final Motor motor;

	@Autowired
	public QuoteApi(QuoteService quoteService, QuoteConverter quoteConverter, Motor motor) {
		this.quoteService = quoteService;
		this.quoteConverter = quoteConverter;
		this.motor = motor;
	}

	@GetMapping
	public QuoteDto getQuote() {
		Quote quote = quoteService.getQuote();
		motor.calculate(quote);
		return quoteConverter.convert(quote);
	}
}
