package pl.decerto.higson.demo.apk.api;

import java.util.ArrayList;
import java.util.List;

import pl.decerto.higson.demo.apk.Answer;
import pl.decerto.higson.demo.apk.Question;

/**
 * Pytanie APK na drucie — wspólne dla wszystkich trzech linii.
 *
 * Niesie pytanie RAZEM z odpowiedzią, bo ekran renderuje jedno i drugie w tej samej kontrolce,
 * a rozdzielenie ich na dwie listy zmusiłoby front do sklejania po kodzie.
 */
public class QuestionDto {

	private String code;
	private String text;
	private String kind;
	private String valueType;
	private String section;
	private String help;
	private int position;
	private boolean visible;
	private boolean required;
	private List<String> options = new ArrayList<>();
	private List<String> optionLabels = new ArrayList<>();
	private String prefillSource;
	private String refusalMessage;

	private String value;
	private List<String> values = new ArrayList<>();
	private boolean refused;

	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public String getText() { return text; }
	public void setText(String text) { this.text = text; }
	public String getKind() { return kind; }
	public void setKind(String kind) { this.kind = kind; }
	public String getValueType() { return valueType; }
	public void setValueType(String valueType) { this.valueType = valueType; }
	public String getSection() { return section; }
	public void setSection(String section) { this.section = section; }
	public String getHelp() { return help; }
	public void setHelp(String help) { this.help = help; }
	public int getPosition() { return position; }
	public void setPosition(int position) { this.position = position; }
	public boolean isVisible() { return visible; }
	public void setVisible(boolean visible) { this.visible = visible; }
	public boolean isRequired() { return required; }
	public void setRequired(boolean required) { this.required = required; }
	public List<String> getOptions() { return options; }
	public void setOptions(List<String> options) { this.options = options; }
	public List<String> getOptionLabels() { return optionLabels; }
	public void setOptionLabels(List<String> optionLabels) { this.optionLabels = optionLabels; }
	public String getPrefillSource() { return prefillSource; }
	public void setPrefillSource(String prefillSource) { this.prefillSource = prefillSource; }
	public String getRefusalMessage() { return refusalMessage; }
	public void setRefusalMessage(String refusalMessage) { this.refusalMessage = refusalMessage; }
	public String getValue() { return value; }
	public void setValue(String value) { this.value = value; }
	public List<String> getValues() { return values; }
	public void setValues(List<String> values) { this.values = values; }
	public boolean isRefused() { return refused; }
	public void setRefused(boolean refused) { this.refused = refused; }

	/** Jedno miejsce, w którym pytanie i odpowiedź stają się jednym obiektem na drucie. */
	public static QuestionDto of(Question question, Answer answer) {
		QuestionDto dto = new QuestionDto();
		dto.setCode(question.getCode());
		dto.setText(question.getText());
		dto.setKind(question.getKind());
		dto.setValueType(question.getValueType());
		dto.setSection(question.getSection());
		dto.setHelp(question.getHelp());
		dto.setPosition(question.getPosition());
		dto.setVisible(question.isVisible());
		dto.setRequired(question.isRequired());
		dto.setOptions(question.getOptions());
		dto.setOptionLabels(question.getOptionLabels());
		dto.setPrefillSource(question.getPrefillSource());
		dto.setRefusalMessage(question.getRefusalMessage());
		if (answer != null) {
			dto.setValue(answer.getValue());
			dto.setValues(answer.getValues());
			dto.setRefused(answer.isRefused());
		}
		return dto;
	}
}
