package pl.decerto.higson.demo.property.context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import io.higson.runtime.core.HigsonContext;

import pl.decerto.higson.demo.apk.Answer;
import pl.decerto.higson.demo.property.domain.PropertyQuote;
import pl.decerto.higson.demo.apk.Question;

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
 * Typ wartości bierze się z atrybutu KIND pytania, nie z jego kodu — Java zna rodzaje odpowiedzi,
 * a nie katalog pytań.
 */
public class PropertyContext extends HigsonContext {


	private final PropertyQuote quote;
	private final Map<String, Object> values = new HashMap<>();

	private String questionCode;
	private String coverCode;
	private String variantCode;
	private String choiceRecommended;
	private String choiceSelected;

	public PropertyContext(PropertyQuote quote) {
		this.quote = quote;
		// ⛔ ODPOWIEDZI PRZED WYLICZENIAMI i nie wolno tego zamienić: franszyza leży pod jedną
		// ścieżką i jako odpowiedź klienta, i jako pole wniosku, a wygrać ma odpowiedź —
		// `collectComputed` dokłada ją przez `putIfAbsent` i tylko w tej kolejności to działa.
		collectAnswers();
		collectComputed();
	}

	public PropertyContext withQuestion(String code) {
		this.questionCode = code;
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

	/** Wariant rekomendowany i wybrany — z nich reguła wyprowadza, czy klient poszedł za radą. */
	public PropertyContext withChoiceVariants(String recommended, String selected) {
		this.choiceRecommended = recommended;
		this.choiceSelected = selected;
		return this;
	}

	/**
	 * Wartość policzona na potrzeby JEDNEGO odczytu, pod ścieżką, której nie ma w modelu wniosku.
	 *
	 * Dziś jedna taka jest: stosunek wartości oszacowanej przez klienta do sumy wyliczonej
	 * z metrażu. Nie jest polem wniosku, bo nie jest niczyją odpowiedzią ani niczyim wynikiem —
	 * istnieje wyłącznie po to, żeby próg z tabeli miał na czym się dopasować.
	 */
	public PropertyContext withComputed(String path, Object value) {
		values.put(path, value);
		return this;
	}

	@Override
	public Object get(String path) {
		if ("question.code".equals(path)) {
			return questionCode;
		}
		if ("cover.code".equals(path)) {
			return coverCode;
		}
		if ("variant.code".equals(path)) {
			return variantCode;
		}
		if ("quote.choice.recommended".equals(path)) {
			return choiceRecommended;
		}
		if ("quote.choice.selected".equals(path)) {
			return choiceSelected;
		}
		return values.get(path);
	}

	/**
	 * Odpowiedzi pod ścieżkami, które podał Higson. Pytanie bez ścieżki nie trafia do kontekstu
	 * wcale — dziś każde pytanie tego dema ją ma.
	 */
	private void collectAnswers() {
		for (Question question : quote.getQuestions()) {
			// ⛔ Odpowiedź na pytanie NIEODSŁONIĘTE nie wchodzi do kontekstu. Pytanie warunkowe
			// można odsłonić i odpowiedzieć, a potem cofnąć odpowiedź, która je odsłoniła — wtedy
			// znika z ekranu, ale jego odpowiedź zostaje zapisana (i słusznie: klient wraca, a my
			// jej nie kasujemy). Bez tego filtra dalej karmiła reguły i rekomendacja opierała się
			// na pytaniu, którego klient już nie widzi.
			if (!question.isVisible()) {
				continue;
			}
			String path = question.getContextPath();
			if (path == null || path.isEmpty()) {
				continue;
			}
			Answer answer = quote.getAnswers().get(question.getCode());
			if (answer == null || !answer.isAnswered()) {
				continue;
			}
			values.put(path, convert(question.getKind(), answer.getValue()));
		}
	}

	private void collectComputed() {
		values.put("quote.policy.startDate", quote.getPolicy().getStartDate());
		// ⛔ `putIfAbsent`, nie `put`. Franszyza jest JEDYNĄ ścieżką, pod którą leży i odpowiedź
		// klienta (pole DEDUCTIBLE kroku 2), i wartość z modelu wniosku — a rozstrzygać ma wybór
		// klienta. Nadpisanie zerem z modelu wygaszało zniżkę za franszyzę bez śladu.
		values.putIfAbsent("quote.policy.deductible", quote.getPolicy().getDeductible());


		values.put("quote.zone.flood", quote.getZone().getFlood());

		values.put("quote.sums.building", quote.getSums().getBuilding());
		values.put("quote.sums.fixtures", quote.getSums().getFixtures());
		values.put("quote.sums.contents", quote.getSums().getContents());
		// ⛔ Suma SUGEROWANA zostaje w kontekscie, choc nie czyta jej zadna regula: czyta ja
		// z powrotem Java, przez decimalAt, zeby porownac ja z suma wybrana przez klienta
		// (rodzina zdan MANUAL_SUM i porownanie z oszacowaniem klienta).
		values.put("quote.sums.suggestedBuilding", quote.getSums().getSuggestedBuilding());

		values.put("quote.risk.factor", quote.getRiskFactor());

	}


	/**
	 * Konwersja po RODZAJU ODPOWIEDZI z konfiguracji, nie po kodzie pytania.
	 *
	 * ⚠️ Silnik NIE konwertuje typów przy dopasowaniu poziomu wejściowego — BigDecimal podany pod
	 * poziom integer kończy się „conversion not supported", a łańcuch pod poziom decimal cicho
	 * spada na wiersz domyślny. Dlatego rodzaj musi rozstrzygać jednoznacznie.
	 *
	 * ⛔ JEDEN atrybut, nie dwa. Do 3 września 2026 stały tu obok siebie KIND (jak narysować
	 * kontrolkę) i VALUE_TYPE (czym jest wartość) — para, w której sensowne były dwie kombinacje
	 * na cztery możliwe, a pomyłka nie wybuchała, tylko dawała złą liczbę. Wystarczyło rozbić
	 * `NUMBER` na `INTEGER` i `DECIMAL`, żeby rodzaj wyznaczał typ jednoznacznie i drugi atrybut
	 * stał się zbędny.
	 */
	private Object convert(String kind, String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}
		if ("DECIMAL".equals(kind) || "MONEY".equals(kind)) {
			return new BigDecimal(value);
		}
		if ("INTEGER".equals(kind)) {
			return Integer.valueOf(value);
		}
		if ("BOOLEAN".equals(kind)) {
			return Boolean.valueOf(value);
		}
		if ("DATE".equals(kind)) {
			return LocalDate.parse(value);
		}
		return value;
	}
}
