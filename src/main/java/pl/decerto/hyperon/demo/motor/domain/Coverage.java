package pl.decerto.hyperon.demo.motor.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Coverage {

	private final String code;
	private String name;

	private int position;
	private BigDecimal limit1;
	private BigDecimal limit2;
	private BigDecimal premium;
	private String description;

	private Option option;

	public Coverage(String code) {
		this.code = code;
	}

	public Coverage(String code, String name) {
		this.code = code;
		this.name = name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getLimit1() {
		return limit1;
	}

	public void setLimit1(BigDecimal limit1) {
		this.limit1 = limit1;
	}

	public BigDecimal getLimit2() {
		return limit2;
	}

	public void setLimit2(BigDecimal limit2) {
		this.limit2 = limit2;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public BigDecimal getPremium() {
		return premium;
	}

	public void setPremium(BigDecimal premium) {
		this.premium = premium;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setOption(Option option) {
		this.option = option;
	}

	public Option getOption() {
		return option;
	}

	public Quote getQuote() {
		if (option != null) {
			return option.getQuote();
		}

		return null;
	}

	String print() {
		String limit1str = limit1 != null ? limit1.toPlainString() : "-";
		String limit2str = limit2 != null ? limit2.toPlainString() : "-";

		return String.format("    coverage [%5s]  ::  limits = %6s /%6s    premium = %7s",
			code, limit1str, limit2str, premium
		);

	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("code", code)
				.append("name", name)
				.append("position", position)
				.append("limit1", limit1)
				.append("limit2", limit2)
				.append("premium", premium)
				.append("description", description)
				.append("option", option)
				.toString();
	}
}
