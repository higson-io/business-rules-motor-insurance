package pl.decerto.higson.demo.apk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import io.higson.runtime.core.HigsonContext;
import io.higson.runtime.core.HigsonEngine;

/**
 * Uruchamia flow rekomendacji APK i przepisuje wynik do modelu — wspólnie dla wszystkich linii.
 *
 * ⛔ Wynik przychodzi jako tekst z separatorami, a nie jako obiekty, i to nie jest niechlujstwo.
 * Flow Higsona zwraca skalary; lista potrzeb i lista ostrzeżeń mają zmienną długość, więc albo
 * jedzie tekstem, albo trzeba by je czytać osobnymi wywołaniami po jednym elemencie. Format jest
 * opisany przy każdej metodzie parsującej i jest jedynym miejscem, w którym Java umawia się
 * z regułami co do kształtu.
 *
 * ⛔ Wołane PO `engine.setEffectiveDate`. Reżim rozstrzyga, czy w ogóle sprawdzać wyłączenia
 * i czy pisać uzasadnienie, a reżim jest wersjonowany datą polisy.
 */
public final class RecommendationReader {

	private RecommendationReader() {
	}

	public static Result read(HigsonEngine engine, String flowCode, HigsonContext ctx) {
		Map<String, Object> out = engine.flow(flowCode, ctx).getResult();
		Result result = new Result();
		result.needs = parseNeeds(asString(out.get("needs")));
		result.warnings = parseWarnings(asString(out.get("warnings")));
		result.recommendation = parseRecommendation(asString(out.get("recommendation")),
			asString(out.get("justification")));
		return result;
	}

	/** Odpowiedzi jako `kod=wartość;kod=wartość` — tak, jak czyta je `apk.deriveNeeds`. */
	public static String answersAsString(List<Question> questions, Map<String, Answer> answers) {
		List<String> parts = new ArrayList<>();
		for (Question question : questions) {
			Answer answer = answers.get(question.getCode());
			if (answer == null || !answer.isAnswered()) {
				continue;
			}
			if (!answer.getValues().isEmpty()) {
				for (String value : answer.getValues()) {
					parts.add(question.getCode() + "=" + value);
				}
			} else {
				parts.add(question.getCode() + "=" + answer.getValue());
			}
		}
		return String.join(";", parts);
	}

	/** `pokrycie|treść|waga` rozdzielone średnikiem. */
	private static List<Need> parseNeeds(String raw) {
		List<Need> needs = new ArrayList<>();
		if (raw == null || raw.isEmpty()) {
			return needs;
		}
		for (String entry : raw.split(";")) {
			String[] parts = entry.split("\\|", -1);
			if (parts.length >= 3) {
				needs.add(new Need(parts[0], parts[1], parseInt(parts[2])));
			}
		}
		return needs;
	}

	/**
	 * `kod|waga|ostrzeżenie|zaradzenie` rozdzielone PODWÓJNYM średnikiem, bo same teksty
	 * ostrzeżeń cytują OWU i zawierają pojedyncze średniki.
	 */
	private static List<Warning> parseWarnings(String raw) {
		List<Warning> warnings = new ArrayList<>();
		if (raw == null || raw.isEmpty()) {
			return warnings;
		}
		for (String entry : raw.split(";;")) {
			String[] parts = entry.split("\\|", -1);
			if (parts.length >= 4) {
				warnings.add(new Warning(parts[0], parseInt(parts[1]), parts[2], parts[3]));
			}
		}
		// Najcięższe wyłączenie na górze — kolejność wierszy w tabeli nie jest kolejnością wagi.
		warnings.sort(Comparator.comparingInt(Warning::getSeverity).reversed());
		return warnings;
	}

	/** `wariant|pokrytych|wszystkich|brakujące pokrycia|wynik budżetu`. */
	private static Recommendation parseRecommendation(String raw, String justification) {
		Recommendation recommendation = new Recommendation();
		recommendation.setJustification(parseJustification(justification));
		if (raw == null || raw.isEmpty()) {
			return recommendation;
		}
		String[] parts = raw.split("\\|", -1);
		recommendation.setVariantCode(emptyToNull(parts[0]));
		if (parts.length > 2) {
			recommendation.setCoveredNeeds(parseInt(parts[1]));
			recommendation.setTotalNeeds(parseInt(parts[2]));
		}
		if (parts.length > 3) {
			recommendation.setMissingCovers(split(parts[3]));
		}
		if (parts.length > 4) {
			recommendation.setBudgetOutcome(emptyToNull(parts[4]));
		}
		return recommendation;
	}

	/** `pozycja|treść`, podwójny średnik między zdaniami. Pozycja porządkuje, potem znika. */
	private static List<String> parseJustification(String raw) {
		List<String> lines = new ArrayList<>();
		if (raw == null || raw.isEmpty()) {
			return lines;
		}
		for (String entry : raw.split(";;")) {
			int separator = entry.indexOf('|');
			lines.add(separator < 0 ? entry : entry.substring(separator + 1));
		}
		return lines;
	}

	private static List<String> split(String csv) {
		if (csv == null || csv.isEmpty()) {
			return new ArrayList<>();
		}
		return new ArrayList<>(Arrays.asList(csv.split("\\s*,\\s*")));
	}

	private static int parseInt(String raw) {
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String emptyToNull(String raw) {
		return raw == null || raw.isEmpty() ? null : raw;
	}

	private static String asString(Object value) {
		return value == null ? null : value.toString();
	}

	/** Trzy listy, które flow zwraca razem i które razem opisują jedną analizę. */
	public static final class Result {

		private List<Need> needs = new ArrayList<>();
		private List<Warning> warnings = new ArrayList<>();
		private Recommendation recommendation = new Recommendation();

		public List<Need> getNeeds() {
			return needs;
		}

		public List<Warning> getWarnings() {
			return warnings;
		}

		public Recommendation getRecommendation() {
			return recommendation;
		}
	}
}
