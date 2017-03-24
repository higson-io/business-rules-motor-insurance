package pl.decerto.hyperon.demo.motor.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Discount {

	private final String code;
	private final String name;

	// amount value
	private final BigDecimal value;
	private final int position;


	public Discount(String code, String name, BigDecimal value, int position) {
		this.code = code;
		this.name = name;
		this.value = value;
		this.position = position;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getValue() {
		return value;
	}

	public int getPosition() {
		return position;
	}

	public String print() {
		return String.format("    discount [%10s] =  %7s   (%s)", code, value, name);
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
