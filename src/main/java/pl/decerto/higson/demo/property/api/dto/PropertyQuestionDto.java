package pl.decerto.higson.demo.property.api.dto;

import java.util.List;

/** Pytanie ankiety APK w postaci, w jakiej widzi je ekran. Wszystko pochodzi z Higsona. */
public class PropertyQuestionDto {

	private String code;
	private String text;
	private String kind;
	private String valueType;
	private String section;
	private String help;
	private int position;
	private boolean visible;
	private boolean required;
	private List<String> options;
	private List<String> optionLabels;
	private String prefillSource;
	private String refusalMessage;
	private String value;
	private List<String> values;
	private boolean refused;

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
}
