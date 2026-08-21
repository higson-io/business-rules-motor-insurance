package pl.decerto.higson.demo.property.domain;

/** Potrzeba klienta wyprowadzona z odpowiedzi (rekomendacja 8.2.b). Treść pochodzi z tabeli. */
public class Need {

	private String coverCode;
	private String text;
	private int weight;

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
}
