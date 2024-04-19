package pl.decerto.higson.demo.motor.api.dto;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class CoverageDto {
	private int position;
	private String code;
	private String name;
	private BigDecimal limitLeft;
	private BigDecimal limitRight;
	private BigDecimal premium;
	private String description;

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getLimitLeft() {
		return limitLeft;
	}

	public void setLimitLeft(BigDecimal limitLeft) {
		this.limitLeft = limitLeft;
	}

	public BigDecimal getLimitRight() {
		return limitRight;
	}

	public void setLimitRight(BigDecimal limitRight) {
		this.limitRight = limitRight;
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

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("position", position)
				.append("code", code)
				.append("name", name)
				.append("limitLeft", limitLeft)
				.append("limitRight", limitRight)
				.append("premium", premium)
				.append("description", description)
				.toString();
	}
}
