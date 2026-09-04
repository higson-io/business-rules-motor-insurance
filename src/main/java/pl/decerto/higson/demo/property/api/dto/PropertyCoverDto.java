package pl.decerto.higson.demo.property.api.dto;

import java.math.BigDecimal;

public class PropertyCoverDto {

	private String code;
	private String name;
	private String description;
	private int position;
	private String capLabel;
	private BigDecimal effectiveLimit;
	private BigDecimal sum;

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public String getCapLabel() {
		return capLabel;
	}

	public void setCapLabel(String capLabel) {
		this.capLabel = capLabel;
	}

	public BigDecimal getEffectiveLimit() {
		return effectiveLimit;
	}

	public void setEffectiveLimit(BigDecimal effectiveLimit) {
		this.effectiveLimit = effectiveLimit;
	}

	public BigDecimal getSum() {
		return sum;
	}

	public void setSum(BigDecimal sum) {
		this.sum = sum;
	}
}
