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

	/**
	 * Krótka nazwa pytania — do listy obok ankiety, nie na ekran pytania.
	 *
	 * ⛔ To NIE jest druga wersja treści pytania i dlatego nie powtarza pułapki, przez którą
	 * zniknął atrybut `TEXT` (D-20). Tamten rozjazd był groźny, bo nazwa i treść udawały TO SAMO
	 * i rozchodziły się niewidocznie. Tu jedno jest pytaniem („Czy chcesz ubezpieczyć swój majątek
	 * na wypadek powodzi?"), a drugie pozycją w spisie („Ochrona przed powodzią") — obie widać na
	 * ekranie naraz, więc rozjazd rzuca się w oczy zamiast się chować.
	 *
	 * Puste — lista pokazuje sam numer.
	 */

	/**
	 * Kod pytania NADRZĘDNEGO, jeśli to pytanie jest jego polem. Puste — pytanie samodzielne.
	 *
	 * ⛔ Pytanie złożone to JEDNO pytanie o kilku polach („Twoje oczekiwania wobec ochrony":
	 * składka i wartość nieruchomości), a nie kilka pytań pokazanych razem. Różnica jest widoczna
	 * w liczniku — „pytanie 6 z 7", nie 6 i 7.
	 *
	 * ⛔ To NIE jest grupowanie po ekranie. §31 odwrócił dokładnie taki zabieg: konfiguracja nie
	 * ma odwzorowywać układu aplikacji. „Oczekiwana składka i wartość nieruchomości to jedno
	 * pytanie" jest zdaniem o przedmiocie rozmowy z klientem, a nie o tym, ile ekranów ma ankieta.
	 *
	 * ⚠️ Model zostaje PŁASKI — zagnieżdżenie robi dopiero konwerter, na drucie. Dzięki temu
	 * odpowiedzi, wartości demonstracyjne i wszystko, co szuka pytania po kodzie, działa bez
	 * zmian: pole jest zwykłym pytaniem, które ma rodzica.
	 */
	private String partOf;
	private String kind;
	private String valueType;



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


	public String getPartOf() {
		return partOf;
	}

	public void setPartOf(String partOf) {
		this.partOf = partOf;
	}

	/** Czy to pytanie jest polem innego. */
	public boolean isField() {
		return partOf != null && !partOf.isEmpty();
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
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
