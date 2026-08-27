package pl.decerto.higson.demo.apk;

/**
 * Ostrzeżenie o wyłączeniu kolidującym z sytuacją klienta (rekomendacja 8.2.c).
 *
 * severity rozdziela „to wyłącza ochronę, której chcesz" (2) od „o tym trzeba wiedzieć" (1).
 * Bez tego rozróżnienia osiem ostrzeżeń naraz znaczy tyle samo, co zero.
 */
public class Warning {

	private String code;
	private int severity;
	private String text;
	private String remedy;

	public Warning(String code, int severity, String text, String remedy) {
		this.code = code;
		this.severity = severity;
		this.text = text;
		this.remedy = remedy;
	}

	public String getCode() {
		return code;
	}

	public int getSeverity() {
		return severity;
	}

	public String getText() {
		return text;
	}

	public String getRemedy() {
		return remedy;
	}
}
