package pl.decerto.higson.demo.motor.api;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.decerto.higson.demo.apk.Answer;
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

	/**
	 * Data początku ochrony.
	 *
	 * ⛔ Przyjmuje OBIEKT daty, nie tekst — tak samo jak `PUT life/startDate` i
	 * `PUT property/startDate`. String przyszedłby jako `text/plain` i Jackson odpowiada 415.
	 *
	 * ⚠️ Podłoga na dniu dzisiejszym jest celowa. Ta data trafia do `engine.setEffectiveDate`,
	 * więc steruje także regionem MOTOR, którego próg leży 1 listopada 2020 r. Data z przeszłości
	 * przełączyłaby taryfę na wersję 1 i zmieniła ceny — a polisa i tak nie może zacząć się wczoraj.
	 */
	/**
	 * Odpowiedź na jedno pytanie ankiety.
	 *
	 * ⛔ Kod pytania jest w ŚCIEŻCE, a nie w zestawie osobnych endpointów per pole. Endpoint per
	 * pole wymagałby dokładania metody przy każdym nowym pytaniu — czyli dokładnie tego wdrożenia,
	 * którego cała ta konstrukcja unika.
	 */
	@PutMapping("/apk/{code}")
	public void setApkAnswer(@PathVariable String code, @RequestBody String value) {
		Answer answer = new Answer(value);
		quoteService.getQuote().getApkAnswers().put(code, answer);
	}

	/** „Nie chcę podać" — trzeci stan odpowiedzi. Nie blokuje wyceny, pokazuje konsekwencję. */
	@PutMapping("/apk/{code}/refused")
	public void setApkRefused(@PathVariable String code, @RequestBody boolean refused) {
		Answer answer = quoteService.getQuote().getApkAnswers()
			.computeIfAbsent(code, key -> new Answer());
		answer.setRefused(refused);
	}

	@PutMapping("/startDate")
	public void setStartDate(@RequestBody Date startDate) {
		LocalDate value = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate today = LocalDate.now();
		quoteService.getQuote().setPolicyStartDate(value.isBefore(today) ? today : value);
	}
}
