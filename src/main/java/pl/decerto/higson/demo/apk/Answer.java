package pl.decerto.higson.demo.apk;

import java.util.ArrayList;
import java.util.List;

/**
 * Odpowiedź na pytanie APK. Ma TRZY stany, nie dwa: udzielona, nieudzielona jeszcze i odmówiona
 * (decyzja D-6). Odmowa jest wartością, bo rekomendacja 8.4 każe poinformować o konsekwencjach
 * nieudzielenia informacji — a nie zablokować ankietę.
 */
public class Answer {

	private String value;
	private List<String> values = new ArrayList<>();
	private boolean refused;

	public Answer() {
	}

	public Answer(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}

	public boolean isRefused() {
		return refused;
	}

	public void setRefused(boolean refused) {
		this.refused = refused;
	}

	public boolean isAnswered() {
		return !refused && (value != null && !value.isEmpty() || !values.isEmpty());
	}

	/**
	 * Czy klient się tym pytaniem ZAJĄŁ — odpowiedział albo świadomie odmówił.
	 *
	 * ⛔ To NIE jest `isAnswered()` i różnica jest nośna. Ankieta zależna odsłania kolejne pytanie
	 * po domknięciu poprzedniego; gdyby liczyła się sama odpowiedź, pytanie nieobowiązkowe, które
	 * klient pomija, zatrzymywałoby całą resztę ankiety na zawsze. Odmowa jest wartością (rek. 8.4),
	 * więc domyka pytanie tak samo jak odpowiedź.
	 */
	public boolean isSettled() {
		return refused || isAnswered();
	}
}
