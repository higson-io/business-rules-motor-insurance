package pl.decerto.higson.demo.property.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class PropertyVariantDto {

	private String code;
	private String name;
	private String description;
	private int position;
	private boolean available;
	private String unavailableReason;
	private BigDecimal liabilitySum;
	private BigDecimal annualPremium;
	private BigDecimal instalment;
	private Map<String, BigDecimal> breakdown;
	private List<String> covers;

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

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public String getUnavailableReason() {
		return unavailableReason;
	}

	public void setUnavailableReason(String unavailableReason) {
		this.unavailableReason = unavailableReason;
	}

	public BigDecimal getLiabilitySum() {
		return liabilitySum;
	}

	public void setLiabilitySum(BigDecimal liabilitySum) {
		this.liabilitySum = liabilitySum;
	}

	public BigDecimal getAnnualPremium() {
		return annualPremium;
	}

	public void setAnnualPremium(BigDecimal annualPremium) {
		this.annualPremium = annualPremium;
	}

	public BigDecimal getInstalment() {
		return instalment;
	}

	public void setInstalment(BigDecimal instalment) {
		this.instalment = instalment;
	}

	public Map<String, BigDecimal> getBreakdown() {
		return breakdown;
	}

	public void setBreakdown(Map<String, BigDecimal> breakdown) {
		this.breakdown = breakdown;
	}

	public List<String> getCovers() {
		return covers;
	}

	public void setCovers(List<String> covers) {
		this.covers = covers;
	}
}
