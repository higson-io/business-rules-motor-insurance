package pl.decerto.higson.demo.life.api.dto;

import java.math.BigDecimal;

public class LifeVariantDto {

	private String code;
	private String name;
	private int position;
	private boolean available;
	private BigDecimal perEmployee;

	/** Hipoteza "gdyby wszyscy wybrali ten wariant", nie koszt programu. */
	private BigDecimal monthlyTotalIfAll;

	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public int getPosition() { return position; }
	public void setPosition(int position) { this.position = position; }
	public boolean isAvailable() { return available; }
	public void setAvailable(boolean available) { this.available = available; }
	public BigDecimal getPerEmployee() { return perEmployee; }
	public void setPerEmployee(BigDecimal perEmployee) { this.perEmployee = perEmployee; }
	public BigDecimal getMonthlyTotalIfAll() { return monthlyTotalIfAll; }
	public void setMonthlyTotalIfAll(BigDecimal monthlyTotalIfAll) { this.monthlyTotalIfAll = monthlyTotalIfAll; }
}
