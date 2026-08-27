package pl.decerto.higson.demo.property.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pl.decerto.higson.demo.property.api.dto.PropertyQuoteDto;
import pl.decerto.higson.demo.property.converter.PropertyQuoteConverter;
import pl.decerto.higson.demo.apk.Answer;
import pl.decerto.higson.demo.property.domain.PropertyQuote;
import pl.decerto.higson.demo.property.example.Property;
import pl.decerto.higson.demo.property.service.PropertyQuoteService;

/**
 * REST demo majątkowego. Każda mutacja to PUT jednego pola — front nie składa obiektu, tylko
 * zgłasza zmianę i pobiera przeliczoną wycenę.
 *
 * ⛔ Nie ma tu endpointu na pojedyncze pole ankiety. Jest JEDEN endpoint przyjmujący odpowiedź
 * pod kodem pytania, bo kody pytań pochodzą z Higsona — endpoint per pole wymagałby wdrożenia
 * za każdym razem, gdy regulator dołoży pytanie, czyli dokładnie tego, czemu to demo przeczy.
 */
@RestController
@RequestMapping("/property")
public class PropertyApi {

	private final PropertyQuoteService quoteService;

	private final PropertyQuoteConverter converter;

	private final Property property;

	public PropertyApi(PropertyQuoteService quoteService, PropertyQuoteConverter converter, Property property) {
		this.quoteService = quoteService;
		this.converter = converter;
		this.property = property;
	}

	@GetMapping
	public PropertyQuoteDto getQuote() {
		PropertyQuote quote = quoteService.getQuote();
		property.calculate(quote);
		return converter.convert(quote);
	}

	/**
	 * Data początku ochrony. Trafia do engine.setEffectiveDate, więc przesunięcie jej przez
	 * 1 lipca 2027 zmienia wersję regionu — a z nią zestaw pytań i reżim APK.
	 *
	 * Przyjmujemy java.util.Date, nie String: front wysyła obiekt Date, a ciało tekstowe kończy
	 * się na HTTP 415.
	 */
	@PutMapping("/policy/startDate")
	public void setStartDate(@RequestBody @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate) {
		LocalDate date = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		quoteService.getQuote().getPolicy().setStartDate(date);
	}

	@PutMapping("/policy/deductible")
	public void setDeductible(@RequestBody BigDecimal deductible) {
		quoteService.getQuote().getPolicy().setDeductible(deductible);
	}

	/** Jedna odpowiedź ankiety. Kod pytania jest danymi, nie nazwą endpointu. */
	@PutMapping("/answer/{code}")
	public void setAnswer(@PathVariable String code, @RequestBody String value) {
		Answer answer = quoteService.getQuote().answerFor(code);
		answer.setValue(value);
		answer.getValues().clear();
		answer.setRefused(false);
	}

	/** Odpowiedź wielokrotnego wyboru — wartości rozdzielone przecinkiem. */
	@PutMapping("/answer/{code}/multi")
	public void setMultiAnswer(@PathVariable String code, @RequestBody String values) {
		Answer answer = quoteService.getQuote().answerFor(code);
		answer.setValue(null);
		answer.getValues().clear();
		if (values != null && !values.isEmpty()) {
			answer.getValues().addAll(Arrays.asList(values.split(",")));
		}
		answer.setRefused(false);
	}

	/**
	 * Odmowa udzielenia odpowiedzi (decyzja D-6). Nie blokuje wyceny — rekomendacja 8.4 każe
	 * poinformować o konsekwencjach, a nie przerwać rozmowę.
	 */
	@PutMapping("/answer/{code}/refused")
	public void setRefused(@PathVariable String code, @RequestBody boolean refused) {
		Answer answer = quoteService.getQuote().answerFor(code);
		answer.setRefused(refused);
		if (refused) {
			answer.setValue(null);
			answer.getValues().clear();
		}
	}

	/**
	 * Suma murów wybrana przez klienta. Od tego momentu przestaje podążać za wyliczoną — bo to
	 * jest właśnie decyzja, o której mówi uzasadnienie („podniosłeś" albo „obniżyłeś").
	 */
	@PutMapping("/sum/building")
	public void setSumBuilding(@RequestBody BigDecimal sum) {
		quoteService.getQuote().getSums().setBuilding(sum);
		quoteService.getQuote().setBuildingSumOverridden(true);
	}

	@PutMapping("/variant")
	public void setVariant(@RequestBody String variantCode) {
		quoteService.getQuote().setSelectedVariant(variantCode);
	}
}
