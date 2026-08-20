package pl.decerto.higson.demo.life.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pl.decerto.higson.demo.life.api.dto.LifeQuoteDto;
import pl.decerto.higson.demo.life.converter.LifeQuoteConverter;
import pl.decerto.higson.demo.life.domain.LifeQuote;
import pl.decerto.higson.demo.life.domain.Rider;
import pl.decerto.higson.demo.life.example.Life;
import pl.decerto.higson.demo.life.service.LifeQuoteService;

/**
 * REST dla demo życiowego. Każda mutacja to PUT jednego pola, tak jak w profilu motor —
 * front nie składa obiektu, tylko zgłasza zmianę i pobiera przeliczoną wycenę.
 */
@RestController
@RequestMapping("/life")
public class LifeApi {

	private final LifeQuoteService quoteService;

	private final LifeQuoteConverter converter;

	private final Life life;

	public LifeApi(LifeQuoteService quoteService, LifeQuoteConverter converter, Life life) {
		this.quoteService = quoteService;
		this.converter = converter;
		this.life = life;
	}

	@GetMapping
	public LifeQuoteDto getQuote() {
		LifeQuote quote = quoteService.getQuote();
		life.calculate(quote);
		return converter.convert(quote);
	}

	@PutMapping("/line")
	public void setLine(@RequestBody String lineCode) {
		quoteService.getQuote().setLineCode(lineCode);
	}

	/**
	 * Data początku ochrony. To ona trafia do engine.setEffectiveDate, więc przesunięcie jej
	 * przez oś czasu regionu zmienia wersję taryfy — cała demonstracja sterowania datą.
	 *
	 * Przyjmujemy java.util.Date, nie String: front wysyła obiekt Date, a ciało tekstowe
	 * kończy się na HTTP 415.
	 */
	@PutMapping("/policy/startDate")
	public void setStartDate(@RequestBody @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate) {
		LocalDate date = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		quoteService.getQuote().getPolicy().setStartDate(date);
	}

	@PutMapping("/policy/mainSum")
	public void setMainSum(@RequestBody BigDecimal mainSum) {
		quoteService.getQuote().getPolicy().setMainSum(mainSum);
	}

	@PutMapping("/policy/paymentFrequency")
	public void setPaymentFrequency(@RequestBody String frequency) {
		quoteService.getQuote().getPolicy().setPaymentFrequency(frequency);
	}

	@PutMapping("/insured/birthDate")
	public void setBirthDate(@RequestBody @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date birthDate) {
		LocalDate date = birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		quoteService.getQuote().getInsured().setBirthDate(date);
	}

	/** Ekran pyta o wiek wprost — data urodzenia zostaje dla wariantu z pełnym wnioskiem. */
	@PutMapping("/insured/age")
	public void setAge(@RequestBody int age) {
		quoteService.getQuote().getInsured().setAge(age);
	}

	@PutMapping("/insured/smoker")
	public void setSmoker(@RequestBody boolean smoker) {
		quoteService.getQuote().getInsured().setSmoker(smoker);
	}

	@PutMapping("/insured/heightCm")
	public void setHeight(@RequestBody int heightCm) {
		quoteService.getQuote().getInsured().setHeightCm(heightCm);
	}

	@PutMapping("/insured/weightKg")
	public void setWeight(@RequestBody int weightKg) {
		quoteService.getQuote().getInsured().setWeightKg(weightKg);
	}

	@PutMapping("/insured/occupationClass")
	public void setOccupationClass(@RequestBody int occupationClass) {
		quoteService.getQuote().getInsured().setOccupationClass(occupationClass);
	}

	@PutMapping("/insured/monthlyIncome")
	public void setMonthlyIncome(@RequestBody BigDecimal monthlyIncome) {
		quoteService.getQuote().getInsured().setMonthlyIncome(monthlyIncome);
	}

	@PutMapping("/rider/{code}/selected")
	public void setRiderSelected(@PathVariable String code, @RequestBody boolean selected) {
		Rider rider = quoteService.getQuote().getRider(code);
		if (rider != null) {
			rider.setSelected(selected);
		}
	}

	@PutMapping("/rider/{code}/sum")
	public void setRiderSum(@PathVariable String code, @RequestBody BigDecimal sum) {
		Rider rider = quoteService.getQuote().getRider(code);
		if (rider != null) {
			rider.setSum(sum);
		}
	}

	@PutMapping("/group/headcount")
	public void setHeadcount(@RequestBody int headcount) {
		quoteService.getQuote().getGroup().setHeadcount(headcount);
	}

	@PutMapping("/group/averageAge")
	public void setAverageAge(@RequestBody int averageAge) {
		quoteService.getQuote().getGroup().setAverageAge(averageAge);
	}

	@PutMapping("/group/industryClass")
	public void setIndustryClass(@RequestBody int industryClass) {
		quoteService.getQuote().getGroup().setIndustryClass(industryClass);
	}

	@PutMapping("/group/participation")
	public void setParticipation(@RequestBody BigDecimal participation) {
		quoteService.getQuote().getGroup().setParticipation(participation);
	}

	@PutMapping("/group/includeSpouse")
	public void setIncludeSpouse(@RequestBody boolean includeSpouse) {
		quoteService.getQuote().getGroup().setIncludeSpouse(includeSpouse);
	}

	@PutMapping("/group/includeAdultChild")
	public void setIncludeAdultChild(@RequestBody boolean includeAdultChild) {
		quoteService.getQuote().getGroup().setIncludeAdultChild(includeAdultChild);
	}
}
