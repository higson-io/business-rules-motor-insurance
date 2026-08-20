package pl.decerto.higson.demo.life.api.dto;

import java.math.BigDecimal;
import java.util.Map;

public class LifeBenefitDto {

	private String code;
	private String name;
	private int position;
	private String benefitKind;
	private Map<String, BigDecimal> sumByVariant;

	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public int getPosition() { return position; }
	public void setPosition(int position) { this.position = position; }
	public String getBenefitKind() { return benefitKind; }
	public void setBenefitKind(String benefitKind) { this.benefitKind = benefitKind; }
	public Map<String, BigDecimal> getSumByVariant() { return sumByVariant; }
	public void setSumByVariant(Map<String, BigDecimal> sumByVariant) { this.sumByVariant = sumByVariant; }
}
