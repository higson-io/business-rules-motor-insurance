package pl.decerto.higson.demo.property.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Parametry polisy. Data początku ochrony trafia do engine.setEffectiveDate — to ona wybiera
 * wersję regionu, czyli reżim APK i zestaw pytań.
 */
public class Policy {

	private LocalDate startDate = LocalDate.now();
	private BigDecimal deductible = BigDecimal.ZERO;

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}


	public BigDecimal getDeductible() {
		return deductible;
	}

	public void setDeductible(BigDecimal deductible) {
		this.deductible = deductible;
	}
}
