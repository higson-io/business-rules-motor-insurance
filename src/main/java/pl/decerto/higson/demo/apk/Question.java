package pl.decerto.higson.demo.apk;

import java.util.ArrayList;
import java.util.List;

/**
 * Pytanie ankiety APK. Każde pole pochodzi z Higsona — treść i typ z atrybutów elementu domeny,
 * widoczność, obowiązkowość i kolejność z tabel decyzyjnych.
 *
 * ⛔ Java nie zna ani jednego kodu pytania. Odpowiedź trafia pod ścieżkę kontekstu, którą podaje
 * atrybut CONTEXT_PATH — dlatego dołożenie pytania w Studio nie wymaga tu żadnej zmiany.
 */
public class Question {

	private String code;
	private String text;
	private String kind;
	private String valueType;

	/**
	 * Kod pytania, którego domknięcie odsłania to pytanie. Puste — widoczne od razu.
	 *
	 * ⛔ Łańcuch jest KONFIGURACJĄ (kolumna `revealedBy` w katalogu pytań), nie kolejnością
	 * w kodzie. Ankieta prowadzona krok po kroku to decyzja o tym, jak rozmawiamy z klientem,
	 * więc przestawienie jej nie może wymagać wdrożenia.
	 */
	private String revealedBy;
	private String section;
	private String help;
	private String contextPath;
	private int position;
	private boolean visible;
	private boolean required;
	private List<String> options = new ArrayList<>();
	private List<String> optionLabels = new ArrayList<>();
	private String prefillSource;
	private String refusalMessage;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getRevealedBy() {
		return revealedBy;
	}

	public void setRevealedBy(String revealedBy) {
		this.revealedBy = revealedBy;
	}

	public String getValueType() {
		return valueType;
	}

	public void setValueType(String valueType) {
		this.valueType = valueType;
	}

	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		this.section = section;
	}

	public String getHelp() {
		return help;
	}

	public void setHelp(String help) {
		this.help = help;
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
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

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public List<String> getOptions() {
		return options;
	}

	public void setOptions(List<String> options) {
		this.options = options;
	}

	public List<String> getOptionLabels() {
		return optionLabels;
	}

	public void setOptionLabels(List<String> optionLabels) {
		this.optionLabels = optionLabels;
	}

	public String getPrefillSource() {
		return prefillSource;
	}

	public void setPrefillSource(String prefillSource) {
		this.prefillSource = prefillSource;
	}

	public String getRefusalMessage() {
		return refusalMessage;
	}

	public void setRefusalMessage(String refusalMessage) {
		this.refusalMessage = refusalMessage;
	}
}
