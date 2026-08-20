package pl.decerto.higson.demo.life.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class LifeRiderDto {

	private String code;
	private String name;
	private String description;
	private int position;
	private String benefitKind;
	private List<BigDecimal> sums;
	private BigDecimal defaultSum;
	private BigDecimal maxSum;
	private Integer waitingPeriodDays;
	private boolean selected;
	private BigDecimal sum;
	private BigDecimal premium;

	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public int getPosition() { return position; }
	public void setPosition(int position) { this.position = position; }
	public String getBenefitKind() { return benefitKind; }
	public void setBenefitKind(String benefitKind) { this.benefitKind = benefitKind; }
	public List<BigDecimal> getSums() { return sums; }
	public void setSums(List<BigDecimal> sums) { this.sums = sums; }
	public BigDecimal getDefaultSum() { return defaultSum; }
	public void setDefaultSum(BigDecimal defaultSum) { this.defaultSum = defaultSum; }
	public BigDecimal getMaxSum() { return maxSum; }
	public void setMaxSum(BigDecimal maxSum) { this.maxSum = maxSum; }
	public Integer getWaitingPeriodDays() { return waitingPeriodDays; }
	public void setWaitingPeriodDays(Integer waitingPeriodDays) { this.waitingPeriodDays = waitingPeriodDays; }
	public boolean isSelected() { return selected; }
	public void setSelected(boolean selected) { this.selected = selected; }
	public BigDecimal getSum() { return sum; }
	public void setSum(BigDecimal sum) { this.sum = sum; }
	public BigDecimal getPremium() { return premium; }
	public void setPremium(BigDecimal premium) { this.premium = premium; }
}
