package pl.decerto.higson.demo.life.context;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import io.higson.runtime.core.HigsonContext;
import io.higson.runtime.ext.adapter.Adapter;
import org.apache.commons.lang3.StringUtils;

import pl.decerto.higson.demo.apk.Answer;
import pl.decerto.higson.demo.apk.Question;
import pl.decerto.higson.demo.life.domain.Variant;

import pl.decerto.higson.demo.life.adapter.LifeQuoteAdapter;
import pl.decerto.higson.demo.life.adapter.RiderAdapter;
import pl.decerto.higson.demo.life.domain.LifeQuote;
import pl.decerto.higson.demo.life.domain.Rider;

/**
 * Kontekst profilu LIFE — jedyne miejsce, które wie, że ścieżka "quote.insured.entryAge"
 * rozwiązuje się przez adapter, a "rider.code" przez element iteracji.
 *
 * Elementy iteracji (rider, variant, benefit, claim) są adresowane nazwą typu, dokładnie tak jak
 * coverage i option w profilu motor. Nie występują na liście ścieżek kontekstu i to jest w porządku:
 * lista wystawia wyłącznie statyczne drzewo.
 */
public class LifeContext extends HigsonContext {

	private LifeQuote quote;
	private Rider rider;
	private String variantCode;
	private String benefitCode;
	private String claimCause;

	private String questionCode;
	private String answerValue;
	private String reasonCode;

	/**
	 * Odpowiedzi ankiety APK, zebrane pod ścieżkami, które podał Higson.
	 *
	 * ⛔ Bez adapterów per pole. Ścieżkę każdej odpowiedzi podaje atrybut CONTEXT_PATH pytania —
	 * adapter wyliczający pola jeden po drugim wymagałby zmiany w Javie przy każdym nowym pytaniu.
	 */
	private final Map<String, Object> apkValues = new HashMap<>();

	public LifeContext(LifeQuote quote) {
		this.quote = quote;
		collectApkAnswers();
	}

	public LifeContext withQuestion(String code) {
		this.questionCode = code;
		return this;
	}

	public LifeContext withAnswer(String value) {
		this.answerValue = value;
		return this;
	}

	public LifeContext withReason(String code) {
		this.reasonCode = code;
		return this;
	}

	/**
	 * ⚠️ Konwersja idzie po VALUE_TYPE, a NIE po KIND. Silnik nie konwertuje typów przy dopasowaniu
	 * poziomu wejściowego, więc liczba wpisana jako tekst nie trafi w wiersz i tabela spadnie na
	 * wartość domyślną — po cichu.
	 */
	private void collectApkAnswers() {
		if (quote == null) {
			return;
		}
		for (Question question : quote.getApkQuestions()) {
			String path = question.getContextPath();
			if (path == null || path.isEmpty()) {
				continue;
			}
			Answer answer = quote.getApkAnswers().get(question.getCode());
			if (answer == null || !answer.isAnswered()) {
				continue;
			}
			apkValues.put(path, convertApk(question.getValueType(), answer.getValue()));
		}
	}


	private Object convertApk(String valueType, String raw) {
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

	public LifeContext(LifeQuote quote, Rider rider) {
		this.quote = quote;
		this.rider = rider;
	}

	public LifeContext withVariant(String variantCode) {
		this.variantCode = variantCode;
		return this;
	}

	public LifeContext withBenefit(String benefitCode) {
		this.benefitCode = benefitCode;
		return this;
	}

	/** Przyczyna zdarzenia. Karencja nie obowiązuje przy nieszczęśliwym wypadku — zapis z OWU. */
	public LifeContext withClaimCause(String claimCause) {
		this.claimCause = claimCause;
		return this;
	}

	@Override
	public Object get(String path) {
		// Elementy iteracji APK i odpowiedzi ankiety rozwiązują się przed adapterami.
		if ("question.code".equals(path)) {
			return questionCode;
		}
		if ("answer.value".equals(path)) {
			return answerValue;
		}
		if ("reason.code".equals(path)) {
			return reasonCode;
		}
		if (apkValues.containsKey(path)) {
			return apkValues.get(path);
		}

		String name = StringUtils.substringBefore(path, ".");
		String subpath = StringUtils.substringAfter(path, ".");

		Adapter adapter = null;

		if ("quote".equals(name)) {
			adapter = new LifeQuoteAdapter(quote);
		}

		if ("rider".equals(name)) {
			adapter = new RiderAdapter(rider);
		}

		if ("variant".equals(name) && "code".equals(subpath)) {
			return variantCode;
		}

		if ("benefit".equals(name) && "code".equals(subpath)) {
			return benefitCode;
		}

		if ("claim".equals(name) && "cause".equals(subpath)) {
			return claimCause;
		}

		if (adapter != null) {
			return adapter.get(subpath);
		}

		return null;
	}

	public LifeQuote getQuote() {
		return quote;
	}

	public Rider getRider() {
		return rider;
	}
}
