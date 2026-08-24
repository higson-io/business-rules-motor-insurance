package pl.decerto.higson.demo.apk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import io.higson.runtime.core.HigsonContext;
import io.higson.runtime.model.DomainObject;

/**
 * Czyta katalog pytań APK z domeny — wspólnie dla wszystkich linii produktowych.
 *
 * ⛔ Ta klasa nie zna ani jednego kodu pytania, ani jednej linii produktowej. Dostaje element
 * nadrzędny, nazwę kolekcji i sposób budowania kontekstu — a wszystko, co opisuje pytanie, czyta
 * z jego atrybutów. Dołożenie pytania w Studio nie wymaga tu żadnej zmiany; dołożenie CAŁEJ LINII
 * produktowej wymaga wyłącznie podania innego elementu nadrzędnego.
 *
 * ⛔ Wołana przy KAŻDEJ wycenie, po `engine.setEffectiveDate` — nigdy raz przy tworzeniu sesji.
 * Część pytań istnieje wyłącznie w drugiej wersji regionu, więc to, o co w ogóle wolno zapytać,
 * zależy od daty polisy. Odczyt w konstruktorze wniosku sprawiłby, że wersja 2 nigdy by się nie
 * pokazała — i wyglądałoby to na błąd konfiguracji, a nie kodu.
 */
public final class QuestionCatalogue {

	private QuestionCatalogue() {
	}

	/**
	 * @param parent     element domeny, pod którym wisi kolekcja pytań
	 * @param collection nazwa kolekcji
	 * @param contextFor buduje kontekst dla pytania o danym kodzie — ⚠️ W-22: nawigacja po domenie
	 *                   NIE wstrzykuje kodu elementu do kontekstu, więc atrybut kluczowany
	 *                   `question.code` nie miałby się z czego rozwiązać
	 */
	public static List<Question> read(DomainObject parent, String collection,
			Function<String, HigsonContext> contextFor) {

		List<Question> questions = new ArrayList<>();
		if (parent == null) {
			return questions;
		}

		for (DomainObject definition : parent.getChildren(collection)) {
			HigsonContext ctx = contextFor.apply(definition.getCode());

			Question question = new Question();
			question.setCode(definition.getCode());
			question.setText(attr(definition, "TEXT", ctx));
			question.setKind(attr(definition, "KIND", ctx));
			question.setValueType(attr(definition, "VALUE_TYPE", ctx));
			question.setSection(attr(definition, "SECTION", ctx));
			question.setHelp(attr(definition, "HELP", ctx));
			question.setContextPath(attr(definition, "CONTEXT_PATH", ctx));
			question.setPosition(intOrZero(definition.getAttrInteger("POSITION", ctx)));
			question.setVisible(Boolean.TRUE.equals(definition.getAttrBoolean("IS_VISIBLE", ctx)));
			question.setRequired(Boolean.TRUE.equals(definition.getAttrBoolean("IS_REQUIRED", ctx)));
			question.setOptions(split(attr(definition, "OPTIONS", ctx)));
			question.setOptionLabels(split(attr(definition, "OPTION_LABELS", ctx)));
			question.setPrefillSource(attr(definition, "PREFILL_SOURCE", ctx));
			question.setRefusalMessage(attr(definition, "REFUSAL_MESSAGE", ctx));
			questions.add(question);
		}

		questions.sort(Comparator.comparingInt(Question::getPosition));
		return questions;
	}

	/**
	 * ⚠️ Null-safe z konieczności. Pusty atrybut LITERAL po prostu NIE ISTNIEJE w modelu, a
	 * `getAttrString` na nieistniejącym atrybucie rzuca NPE zamiast zwrócić null.
	 */
	private static String attr(DomainObject definition, String code, HigsonContext ctx) {
		return definition.getAttr(code) == null ? null : definition.getAttrString(code, ctx);
	}

	private static int intOrZero(Integer value) {
		return value == null ? 0 : value;
	}

	private static List<String> split(String csv) {
		if (csv == null || csv.isEmpty()) {
			return new ArrayList<>();
		}
		return new ArrayList<>(Arrays.asList(csv.split("\\s*,\\s*")));
	}
}
