package pl.decerto.higson.demo.property.context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import io.higson.runtime.core.HigsonContext;

import pl.decerto.higson.demo.property.domain.Answer;
import pl.decerto.higson.demo.property.domain.PropertyQuote;
import pl.decerto.higson.demo.property.domain.Question;

/**
 * Kontekst profilu PROPERTY — jedyne miejsce, które wie, jak model wniosku odwzorowuje się na
 * ścieżki kontekstu Higsona.
 *
 * ⛔ Odpowiedzi ankiety NIE mają tu adapterów per pole i to jest decyzja, nie skrót. Ścieżkę
 * kontekstu każdej odpowiedzi podaje atrybut CONTEXT_PATH pytania w Higsonie — adapter
 * wyliczający pola jeden po drugim przywróciłby dokładnie to sprzężenie, które decyzja D-4 usuwa:
 * dołożenie pytania w Studio wymagałoby wtedy zmiany w Javie. Wartości wyliczone (wiek budynku,
 * strefy, sumy, czynnik ryzyka) idą osobno, bo nie pochodzą z żadnej odpowiedzi.
 *
 * Typ wartości bierze się z atrybutu VALUE_TYPE pytania, nie z jego kodu — Java zna rodzaje
 * wartości, a nie katalog pytań.
 */
public class PropertyContext extends HigsonContext {

	private static final String PATH_POSTCODE = "quote.property.postcode";

	private final PropertyQuote quote;
	private final Map<String, Object> values = new HashMap<>();

	private String questionCode;
	private String answerValue;
	private String coverCode;
	private String variantCode;
	private String reasonCode;

	public PropertyContext(PropertyQuote quote) {
		this.quote = quote;
		collectAnswers();
		collectComputed();
	}

	public PropertyContext withQuestion(String code) {
		this.questionCode = code;
		return this;
	}

	public PropertyContext withAnswer(String value) {
		this.answerValue = value;
		return this;
	}

	public PropertyContext withCover(String code) {
		this.coverCode = code;
		return this;
	}

	public PropertyContext withVariant(String code) {
		this.variantCode = code;
		return this;
	}

	public PropertyContext withReason(String code) {
		this.reasonCode = code;
		return this;
	}

	@Override
	public Object get(String path) {
		if ("question.code".equals(path)) {
			return questionCode;
		}
		if ("answer.value".equals(path)) {
			return answerValue;
		}
		if ("cover.code".equals(path)) {
			return coverCode;
		}
		if ("variant.code".equals(path)) {
			return variantCode;
		}
		if ("reason.code".equals(path)) {
			return reasonCode;
		}
		return values.get(path);
	}

	/**
	 * Odpowiedzi pod ścieżkami, które podał Higson. Pytanie bez ścieżki (wielokrotny wybór) nie
	 * trafia do kontekstu wcale — jego wartości iterują się po jednej przez answer.value.
	 */
	private void collectAnswers() {
		for (Question question : quote.getQuestions()) {
			String path = question.getContextPath();
			if (path == null || path.isEmpty()) {
				continue;
			}
			Answer answer = quote.getAnswers().get(question.getCode());
			if (answer == null || !answer.isAnswered()) {
				continue;
			}
			values.put(path, convert(question.getValueType(), answer.getValue()));
		}
	}

	private void collectComputed() {
		values.put("quote.policy.startDate", quote.getPolicy().getStartDate());
		values.put("quote.policy.deductible", quote.getPolicy().getDeductible());

		values.put("quote.property.age", quote.getBuildingAge());
		values.put("quote.property.postcodeArea", postcodeArea());

		values.put("quote.zone.flood", quote.getZone().getFlood());
		values.put("quote.zone.theft", quote.getZone().getTheft());

		values.put("quote.sums.building", quote.getSums().getBuilding());
		values.put("quote.sums.fixtures", quote.getSums().getFixtures());
		values.put("quote.sums.contents", quote.getSums().getContents());
		values.put("quote.sums.theft", quote.getSums().getTheft());
		values.put("quote.sums.flood", quote.getSums().getFlood());
		values.put("quote.sums.liability", quote.getSums().getLiability());
		values.put("quote.sums.assistance", quote.getSums().getAssistance());
		values.put("quote.sums.suggestedBuilding", quote.getSums().getSuggestedBuilding());

		values.put("quote.risk.factor", quote.getRiskFactor());

		values.put("quote.apk.answers", quote.getApkAnswers());
		values.put("quote.apk.premiums", quote.getApkPremiums());
	}

	/** Dwie pierwsze cyfry kodu pocztowego — klucz obu stref. */
	private String postcodeArea() {
		Object postcode = values.get(PATH_POSTCODE);
		if (postcode == null) {
			return null;
		}
		String digits = postcode.toString().replace("-", "");
		return digits.length() < 2 ? null : digits.substring(0, 2);
	}

	/**
	 * Konwersja po TYPIE WARTOŚCI z konfiguracji, nie po kodzie pytania i nie po rodzaju kontrolki.
	 *
	 * ⚠️ Silnik NIE konwertuje typów przy dopasowaniu poziomu wejściowego — BigDecimal podany pod
	 * poziom integer kończy się „conversion not supported". Dlatego VALUE_TYPE jest osobnym
	 * atrybutem pytania: KIND mówi, jak narysować kontrolkę, VALUE_TYPE — czym jest wpisana w nią
	 * wartość. Rozdzielenie tych dwóch rzeczy kosztuje jeden atrybut i usuwa całą klasę cichych
	 * pudeł, w których tabela spada na wiersz domyślny zamiast wybuchnąć.
	 */
	private Object convert(String valueType, String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}
		if ("DECIMAL".equals(valueType)) {
			return new BigDecimal(value);
		}
		if ("INTEGER".equals(valueType)) {
			return Integer.valueOf(value);
		}
		if ("BOOLEAN".equals(valueType)) {
			return Boolean.valueOf(value);
		}
		if ("DATE".equals(valueType)) {
			return LocalDate.parse(value);
		}
		return value;
	}

	public PropertyQuote getQuote() {
		return quote;
	}
}
