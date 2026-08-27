package pl.decerto.higson.demo.motor.context;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import io.higson.runtime.core.HigsonContext;
import org.apache.commons.lang3.StringUtils;

import pl.decerto.higson.demo.apk.Answer;
import pl.decerto.higson.demo.apk.Question;

import pl.decerto.higson.demo.motor.adapter.CoverageAdapter;
import pl.decerto.higson.demo.motor.adapter.OptionAdapter;
import pl.decerto.higson.demo.motor.adapter.QuoteAdapter;
import pl.decerto.higson.demo.motor.domain.Driver;
import pl.decerto.higson.demo.motor.domain.Option;
import pl.decerto.higson.demo.motor.domain.OptionPricing;
import pl.decerto.higson.demo.motor.domain.Quote;
import pl.decerto.higson.demo.motor.domain.Coverage;
import io.higson.runtime.ext.adapter.Adapter;

public class MotorContext extends HigsonContext {

	private Quote quote;
	private Coverage coverage;
	private Driver driver;
	private Option option;

	/**
	 * Odpowiedzi ankiety APK, zebrane pod ścieżkami, które podał Higson.
	 *
	 * ⛔ Bez adapterów per pole i to jest decyzja, nie skrót. Ścieżkę każdej odpowiedzi podaje
	 * atrybut CONTEXT_PATH pytania — adapter wyliczający pola jeden po drugim przywróciłby
	 * dokładnie to sprzężenie, którego cała ta konstrukcja się pozbywa: dołożenie pytania w Studio
	 * wymagałoby wtedy zmiany w Javie i wdrożenia.
	 */
	private final Map<String, Object> apkValues = new HashMap<>();

	private String questionCode;
	private String answerValue;
	private String reasonCode;
	private String coverageCode;
	private String optionCode;


	public MotorContext(Object... args) {
		super(args);
	}

	public MotorContext(Coverage coverage) {
		this.coverage = coverage;
	}

	public MotorContext(Quote quote) {
		this.quote = quote;
		collectAnswers();
	}

	public MotorContext withQuestion(String code) {
		this.questionCode = code;
		return this;
	}

	public MotorContext withAnswer(String value) {
		this.answerValue = value;
		return this;
	}

	public MotorContext withReason(String code) {
		this.reasonCode = code;
		return this;
	}

	/**
	 * Pokrycie i wariant jako elementy iteracji — potrzebne, żeby zapytać tabelę dostępności
	 * o KONKRETNĄ parę bez powoływania obiektu Coverage.
	 *
	 * ⚠️ Ustawiają `coverageCode`/`optionCode`, a nie adaptery: adapter `coverage.*` rozwiązuje
	 * całe pokrycie razem ze składką, a tutaj chodzi wyłącznie o kod.
	 */
	public MotorContext withCoverage(String code) {
		this.coverageCode = code;
		return this;
	}

	public MotorContext withOption(String code) {
		this.optionCode = code;
		return this;
	}

	/**
	 * Przepisuje odpowiedzi pod ścieżki podane przez pytania.
	 *
	 * ⚠️ Konwersja idzie po VALUE_TYPE, a NIE po KIND. Silnik nie konwertuje typów przy dopasowaniu
	 * poziomu wejściowego, więc liczba wpisana jako tekst po prostu nie trafi w wiersz i tabela
	 * spadnie na wartość domyślną — cicho.
	 */
	private void collectAnswers() {
		if (quote == null) {
			return;
		}
		apkValues.put("quote.apk.vehicleAge", vehicleAge());

		for (Question question : quote.getApkQuestions()) {
			String path = question.getContextPath();
			if (path == null || path.isEmpty()) {
				continue;
			}
			Answer answer = quote.getApkAnswers().get(question.getCode());
			if (answer == null || !answer.isAnswered()) {
				continue;
			}
			apkValues.put(path, convert(question.getValueType(), answer.getValue()));
		}
	}


	/** Wiek pojazdu jest wyliczony, nie zapytany — rocznik już go zawiera. */
	private Integer vehicleAge() {
		if (quote.getVehicle() == null || quote.getVehicle().getProductionYear() <= 0) {
			return null;
		}
		return quote.getPolicyStartDate().getYear() - quote.getVehicle().getProductionYear();
	}

	private Object convert(String valueType, String raw) {
		if (raw == null || raw.isEmpty()) {
			return null;
		}
		if ("INTEGER".equals(valueType)) {
			return Integer.valueOf(raw);
		}
		if ("DECIMAL".equals(valueType)) {
			return new BigDecimal(raw);
		}
		if ("BOOLEAN".equals(valueType)) {
			return Boolean.valueOf(raw);
		}
		return raw;
	}

	public MotorContext(Option option) {
		this.option = option;
	}

	/*
	 * PART 1 :: dynamic context
	 */

	public Coverage getCoverage() {
		return coverage;
	}

	public Option getOption() {

		if (option == null && getCoverage() != null) {
			option = getCoverage().getOption();
		}

		return option;
	}

	public Quote getQuote() {

		if (quote == null && getCoverage() != null) {
			quote = getCoverage().getQuote();
		}

		if (quote == null && getOption() != null) {
			quote = getOption().getQuote();
		}

		return quote;
	}

	public Driver getDriver() {
		if (driver == null && getQuote() != null) {
			driver = getQuote().getDriver();
		}
		return driver;
	}


	/*
	 * PART 2 :: canonical model adapter
	 */

	/**
	 * get value from path,
	 * where path is valid path in canonical model,
	 * for example:
	 * - quote.insured.professionCode
	 * - quote.insured.age
	 * - quote.productCode
	 * - cover.code
	 */
	@Override
	public Object get(String path) {
		// Elementy iteracji i odpowiedzi ankiety rozwiązują się przed adapterami — nie mają
		// i nie powinny mieć adapterów.
		if ("question.code".equals(path)) {
			return questionCode;
		}
		if ("answer.value".equals(path)) {
			return answerValue;
		}
		if ("reason.code".equals(path)) {
			return reasonCode;
		}
		if (coverageCode != null && "coverage.code".equals(path)) {
			return coverageCode;
		}
		if (optionCode != null && "option.code".equals(path)) {
			return optionCode;
		}
		if (apkValues.containsKey(path)) {
			return apkValues.get(path);
		}

		String name = getFirstToken(path);
		String subpath = skipFirstToken(path);

		Adapter adapter = null;
		if (name.equals("quote")) {
			adapter = new QuoteAdapter(getQuote());
		}

		if (name.equals("coverage")) {
			adapter = new CoverageAdapter(getCoverage());
		}

		if (name.equals("option")) {
			adapter = new OptionAdapter(getOption());
		}

		if (adapter != null) {
			return adapter.get(subpath);
		}

		return null;
	}

	public void setQuote(Quote quote) {
		this.quote = quote;
	}

	public void setDriver(Driver driver) {
		this.driver = driver;
	}

	public void setCoverage(Coverage coverage) {
		this.coverage = coverage;
	}

	private String getFirstToken(String path) {
		return StringUtils.substringBefore(path, ".");
	}

	private String skipFirstToken(String path) {
		return StringUtils.substringAfter(path, ".");
	}

}
