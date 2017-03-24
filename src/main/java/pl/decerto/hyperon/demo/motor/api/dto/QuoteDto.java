package pl.decerto.hyperon.demo.motor.api.dto;

import java.util.List;

public class QuoteDto {
	private List<OptionDto> options;

	public List<OptionDto> getOptions() {
		return options;
	}

	public void setOptions(List<OptionDto> options) {
		this.options = options;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("QuoteDto{");
		sb.append("options=").append(options);
		sb.append('}');
		return sb.toString();
	}
}
