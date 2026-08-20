package pl.decerto.higson.demo.life.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Świadczenie grupowe.
 *
 * Suma znaczy co innego zależnie od benefitKind: dla PER_PERCENT to kwota za 1% uszczerbku,
 * dla DAILY_AMOUNT kwota za dzień, dla SERVICE_PACKAGE sumy nie ma wcale.
 */
public class Benefit {

	private String code;
	private String name;
	private int position;
	private String benefitKind;

	/** Suma w rozbiciu na warianty — relacja, nie stan pojedynczego świadczenia. */
	private Map<String, BigDecimal> sumByVariant = new LinkedHashMap<>();

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

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public String getBenefitKind() {
		return benefitKind;
	}

	public void setBenefitKind(String benefitKind) {
		this.benefitKind = benefitKind;
	}

	public Map<String, BigDecimal> getSumByVariant() {
		return sumByVariant;
	}

	public void setSumByVariant(Map<String, BigDecimal> sumByVariant) {
		this.sumByVariant = sumByVariant;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("code", code)
			.append("benefitKind", benefitKind)
			.toString();
	}
}
