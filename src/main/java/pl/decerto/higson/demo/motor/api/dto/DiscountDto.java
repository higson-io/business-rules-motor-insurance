package pl.decerto.higson.demo.motor.api.dto;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class DiscountDto {
	private String code;
	private String name;
	private BigDecimal value;
	private int position;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("code", code)
				.append("name", name)
				.append("value", value)
				.append("position", position)
				.toString();
	}
}
