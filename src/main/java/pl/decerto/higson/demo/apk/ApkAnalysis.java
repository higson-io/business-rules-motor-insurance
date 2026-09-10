package pl.decerto.higson.demo.apk;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.higson.runtime.core.HigsonContext;
import io.higson.runtime.core.HigsonEngine;
import io.higson.runtime.engine.core.output.ParamValue;

/**
 * Analiza potrzeb klienta: potrzeby, konfrontacja z wyłączeniami, dobór i uzasadnienie.
 *
 * ⛔ TA KLASA NIE ZNA ŻADNEJ REGUŁY. Nie ma tu ani jednego kodu pytania, kodu pokrycia, progu,
 * mnożnika ani zdania czytanego przez klienta. Wszystkie te rzeczy są w tabelach decyzyjnych —
 * ta klasa tylko chodzi po listach, które zwracają reguły, i pyta tabelę o każdy element z osobna.
 *
 * ⛔ POWSTAŁA Z LIKWIDACJI 852 LINII GROOVY'EGO. Ta sama analiza była wcześniej sześcioma
 * funkcjami na profil, w sumie ponad ośmiuset liniami w trzech profilach — a ich rozbiór pokazał,
 * że około 70% tego kodu to było parsowanie i sklejanie łańcuchów `kod=wartość;kod=wartość`.
 * Te łańcuchy istniały wyłącznie dlatego, że flow Higsona zwraca skalary, a Groovy był jedynym
 * miejscem, w którym da się napisać pętlę. Pętla nie jest regułą biznesową; mapowanie odpowiedzi
 * na potrzebę jest — i ono zostało w tabeli.
 *
 * Co z tego ma demonstracja: w Studio zostaje to, co osoba z biznesu potrafi przeczytać — katalog
 * pytań, mapowanie odpowiedzi na potrzeby, konfrontacja z wyłączeniami, reguła budżetu i treści
 * uzasadnień. Zniknęło to, co wyglądało jak kod przeniesiony w inne miejsce.
 */
public final class ApkAnalysis {

	/** Nazwy poziomów wyjściowych są kontraktem z tabelami — jedyna rzecz, którą Java tu wie. */
	private static final String OUT_NEED = "need";
	private static final String OUT_COVER = "cover";
	private static final String OUT_WEIGHT = "weight";
	private static final String OUT_TEXT = "text";
	private static final String OUT_POSITION = "position";

	private ApkAnalysis() {
	}

	/**
	 * Rek. 8.2.b — z każdej odpowiedzi wynika potrzeba albo nie wynika nic.
	 *
	 * Dwie odpowiedzi wskazujące to samo pokrycie dają JEDNĄ potrzebę o wyższej wadze, a nie dwie
	 * pozycje na liście. To jedyna reguła w tej metodzie i dlatego jest tu opisana.
	 */
	public static List<Need> deriveNeeds(HigsonEngine engine, String needTable,
			List<Question> questions, Map<String, Answer> answers,
			BiFunction<String, String, HigsonContext> contextFor) {

		Map<String, Need> byCover = new LinkedHashMap<>();
		for (AnswerRef ref : flatten(questions, answers)) {
			ParamValue row = engine.get(needTable, contextFor.apply(ref.questionCode, ref.value));
			String need = row.getString(OUT_NEED);
			if (need == null || need.isEmpty()) {
				continue;
			}
			String cover = row.getString(OUT_COVER);
			int weight = intOrZero(row.getInteger(OUT_WEIGHT));
			Need current = byCover.get(cover);
			if (current == null || current.getWeight() < weight) {
				byCover.put(cover, new Need(cover, need, weight));
			}
		}
		return new ArrayList<>(byCover.values());
	}


	/**
	 * Ile z potrzeb pokrywa dany zestaw — i czego w nim brakuje.
	 *
	 * `availability` odpowiada na jedno pytanie: czy to pokrycie jest w tym wariancie. Zadaje je
	 * wywołujący, bo każda linia produktowa pyta o to inną tabelą.
	 */
	public static Score score(List<Need> needs, Function<String, Boolean> availability) {
		int covered = 0;
		List<String> missing = new ArrayList<>();
		for (Need need : needs) {
			if (Boolean.TRUE.equals(availability.apply(need.getCoverCode()))) {
				covered++;
			} else {
				missing.add(need.getCoverCode());
			}
		}
		return new Score(covered, needs.size(), missing);
	}

	/**
	 * Rek. 8.7 — treść uzasadnienia pochodzi z tabeli, a Java podstawia liczby.
	 *
	 * Wołający przekazuje listę powodów (kod plus parametry) i sposób, w jaki dla kodu wyjmuje się
	 * wiersz. Czego NIE ma tutaj: ani jednego zdania i ANI JEDNEGO ADRESU REGUŁY. Gdzie stoją
	 * zdania, wie profil — bo każdy z trzech sięga po nie inaczej.
	 */
	public static List<String> justify(List<Reason> reasons, Function<String, ParamValue> rowFor) {

		List<String[]> ordered = new ArrayList<>();
		for (Reason reason : reasons) {
			ParamValue row = rowFor.apply(reason.code);
			String text = row.getString(OUT_TEXT);
			if (text == null || text.isEmpty()) {
				continue;
			}
			for (int i = 0; i < reason.arguments.size(); i++) {
				text = text.replace("{" + i + "}", String.valueOf(reason.arguments.get(i)));
			}
			ordered.add(new String[] { String.format("%04d", intOrZero(row.getInteger(OUT_POSITION))), text });
		}
		ordered.sort(Comparator.comparing(entry -> entry[0]));

		List<String> lines = new ArrayList<>();
		for (String[] entry : ordered) {
			lines.add(entry[1]);
		}
		return lines;
	}

	/**
	 * Udzielone odpowiedzi, po jednej pozycji na wartość.
	 *
	 * Pytanie wielokrotnego wyboru daje kilka pozycji, bo każda wartość osobno trafia w tabelę —
	 * inaczej „mam alarm i GPS" nie dopasowałoby się do żadnego wiersza.
	 */
	/**
	 * Odpowiedzi jako jeden łańcuch `KOD=wartość;KOD=wartość` — postać, w której reguła Higsona
	 * może po nich przejść.
	 *
	 * ⛔ Istnieje po to, żeby reguła NIE MUSIAŁA znać listy pytań. Gdyby zamiast tego czytała
	 * odpowiedzi ze ścieżek kontekstu, musiałaby te ścieżki wymienić — a wtedy dołożenie pytania
	 * wymagałoby zmiany reguły, czyli dokładnie tego, czego katalog pytań w tabeli ma unikać.
	 * Pytanie wielokrotnego wyboru daje tyle par, ile zaznaczono odpowiedzi.
	 */
	public static String serialize(List<Question> questions, Map<String, Answer> answers) {
		StringBuilder out = new StringBuilder();
		for (AnswerRef ref : flatten(questions, answers)) {
			if (ref.value == null || ref.value.isEmpty()) {
				continue;
			}
			if (out.length() > 0) {
				out.append(';');
			}
			out.append(ref.questionCode).append('=').append(ref.value);
		}
		return out.toString();
	}

	private static List<AnswerRef> flatten(List<Question> questions, Map<String, Answer> answers) {
		List<AnswerRef> refs = new ArrayList<>();
		for (Question question : questions) {
			Answer answer = answers.get(question.getCode());
			if (answer == null || !answer.isAnswered()) {
				continue;
			}
			if (!answer.getValues().isEmpty()) {
				for (String value : answer.getValues()) {
					refs.add(new AnswerRef(question.getCode(), value));
				}
			} else {
				refs.add(new AnswerRef(question.getCode(), answer.getValue()));
			}
		}
		return refs;
	}

	private static int intOrZero(Integer value) {
		return value == null ? 0 : value;
	}

	private static final class AnswerRef {
		private final String questionCode;
		private final String value;

		private AnswerRef(String questionCode, String value) {
			this.questionCode = questionCode;
			this.value = value;
		}
	}

	/** Ile potrzeb pokrytych, ile wszystkich, czego brakuje. */
	public static final class Score {
		private final int covered;
		private final int total;
		private final List<String> missing;

		Score(int covered, int total, List<String> missing) {
			this.covered = covered;
			this.total = total;
			this.missing = missing;
		}

		public int getCovered() {
			return covered;
		}

		public int getTotal() {
			return total;
		}

		public List<String> getMissing() {
			return missing;
		}
	}

	/** Kod powodu i jego parametry — treść dokłada tabela. */
	public static final class Reason {
		private final String code;
		private final List<Object> arguments = new ArrayList<>();

		public Reason(String code, Object... arguments) {
			this.code = code;
			for (Object argument : arguments) {
				this.arguments.add(argument instanceof BigDecimal
					? ((BigDecimal) argument).stripTrailingZeros().toPlainString()
					: argument);
			}
		}
	}
}
