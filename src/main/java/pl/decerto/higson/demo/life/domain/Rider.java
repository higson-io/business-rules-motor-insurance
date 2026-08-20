package pl.decerto.higson.demo.life.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Umowa dodatkowa. Pola konfiguracyjne pochodzą z domeny Higsona, pola wyniku z wyceny —
 * Java niczego z nich nie wylicza.
 */
public class Rider {

	private String code;
	private String name;
	private String description;
	private int position;
	private boolean available;
	private String benefitKind;
	private List<BigDecimal> sums = new ArrayList<>();
	private BigDecimal defaultSum;
	private BigDecimal maxSum;
	private Integer waitingPeriodDays;

	private boolean selected;
	private BigDecimal sum;
	private BigDecimal premium;

	public Rider() {
	}

	public Rider(String code, String name) {
		this.code = code;
		this.name = name;
	}

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

	public String getBenefitKind() {
		return benefitKind;
	}

	public void setBenefitKind(String benefitKind) {
		this.benefitKind = benefitKind;
	}

	public List<BigDecimal> getSums() {
		return sums;
	}

	public void setSums(List<BigDecimal> sums) {
		this.sums = sums;
	}

	public BigDecimal getDefaultSum() {
		return defaultSum;
	}

	public void setDefaultSum(BigDecimal defaultSum) {
		this.defaultSum = defaultSum;
	}

	public BigDecimal getMaxSum() {
		return maxSum;
	}

	public void setMaxSum(BigDecimal maxSum) {
		this.maxSum = maxSum;
	}

	public Integer getWaitingPeriodDays() {
		return waitingPeriodDays;
	}

	public void setWaitingPeriodDays(Integer waitingPeriodDays) {
		this.waitingPeriodDays = waitingPeriodDays;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public BigDecimal getSum() {
		return sum;
	}

	public void setSum(BigDecimal sum) {
		this.sum = sum;
	}

	public BigDecimal getPremium() {
		return premium;
	}

	public void setPremium(BigDecimal premium) {
		this.premium = premium;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("code", code)
			.append("selected", selected)
			.append("sum", sum)
			.append("premium", premium)
			.toString();
	}
}
