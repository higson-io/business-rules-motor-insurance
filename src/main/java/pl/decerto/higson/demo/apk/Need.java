package pl.decerto.higson.demo.apk;

/** Potrzeba klienta wyprowadzona z odpowiedzi (rekomendacja 8.2.b). Treść pochodzi z tabeli. */
public class Need {

	private String coverCode;
	private String text;
	private int weight;

	/**
	 * Czy klient tę potrzebę ZGŁOSIŁ, czy my ją WYWNIOSKOWALIŚMY z jego sytuacji.
	 *
	 * ⛔ Rek. 8.3 KNF pozwala prowadzić analizę na informacjach od klienta „uzyskanych
	 * w przeszłości lub pozyskanych z innych źródeł", ale każe o tym klienta POINFORMOWAĆ.
	 * Zdanie „pilna potrzeba ochrony przed kradzieżą" czyta się jak deklaracja klienta,
	 * a jest naszym wnioskiem z tego, że nie ma zabezpieczeń — i tę różnicę widać na ekranie.
	 */
	private boolean declared = true;

	public Need(String coverCode, String text, int weight) {
		this.coverCode = coverCode;
		this.text = text;
		this.weight = weight;
	}

	public String getCoverCode() {
		return coverCode;
	}

	public String getText() {
		return text;
	}

	public int getWeight() {
		return weight;
	}

	public boolean isDeclared() {
		return declared;
	}

	public void setDeclared(boolean declared) {
		this.declared = declared;
	}
}
