package pl.decerto.higson.demo.property.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Sekcja ankiety APK — gałąź drzewa domeny, a nie wartość atrybutu na pytaniu.
 *
 * ⛔ Ta różnica jest celem całej zmiany. Sekcja jako atrybut była etykietą: nie mogła mieć
 * własnych ustawień, jej tytuł musiał żyć w kodzie ekranu, a wersjonowanie regionem dotyczyło
 * pojedynczych pytań, nie grupy. Sekcja jako element domeny ma tytuł, kolejność i własną regułę
 * widoczności — i może NIE ISTNIEĆ w danej wersji regionu razem ze wszystkimi swoimi pytaniami.
 *
 * ⚠️ `visible` (reguła) i nieobecność w wersji regionu to dwie różne rzeczy. Sekcja niewidoczna
 * istnieje i może się pokazać po zmianie odpowiedzi; sekcji spoza wersji nie ma w drzewie wcale.
 */
public class Section {

	private String code;
	private String title;
	private int position;
	private boolean visible;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("code", code).toString();
	}
}
