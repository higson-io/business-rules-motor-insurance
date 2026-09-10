package pl.decerto.higson.demo.property.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Wariant ochrony. Niedostępny wariant niesie POWÓD — nie znika po cichu, bo wtedy klient
 * dowiedziałby się o brakującym pokryciu dopiero z polisy.
 */
public class Variant {

	private String code;
	private String name;
	private String description;
	private int position;
	private boolean available;
	private BigDecimal annualPremium;
	private BigDecimal instalment;
	private Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
	private Set<String> covers = new LinkedHashSet<>();

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

	public Set<String> getCovers() {
		return covers;
	}

	public void setCovers(Set<String> covers) {
		this.covers = covers;
	}
}
