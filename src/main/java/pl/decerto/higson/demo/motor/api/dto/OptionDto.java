package pl.decerto.higson.demo.motor.api.dto;

import java.math.BigDecimal;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class OptionDto {
	private String code;
	private List<CoverageDto> coverages;
	private List<DiscountDto> discounts;
	private int order;
	private BigDecimal premium;
	private BigDecimal premiumBeforeDiscounts;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public List<CoverageDto> getCoverages() {
		return coverages;
	}

	public void setCoverages(List<CoverageDto> coverages) {
		this.coverages = coverages;
	}

	public List<DiscountDto> getDiscounts() {
		return discounts;
	}

	public void setDiscounts(List<DiscountDto> discounts) {
		this.discounts = discounts;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public BigDecimal getPremium() {
		return premium;
	}

	public void setPremium(BigDecimal premium) {
		this.premium = premium;
	}

	public BigDecimal getPremiumBeforeDiscounts() {
		return premiumBeforeDiscounts;
	}

	public void setPremiumBeforeDiscounts(BigDecimal premiumBeforeDiscounts) {
		this.premiumBeforeDiscounts = premiumBeforeDiscounts;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("code", code)
				.append("coverages", coverages)
				.append("discounts", discounts)
				.append("order", order)
				.append("premium", premium)
				.append("premiumBeforeDiscounts", premiumBeforeDiscounts)
				.toString();
	}
}
