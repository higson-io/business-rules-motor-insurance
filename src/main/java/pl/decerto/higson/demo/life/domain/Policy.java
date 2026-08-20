package pl.decerto.higson.demo.life.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parametry polisy. startDate jest kluczem do wersji taryfy — orkiestrator ustawia go
 * jako effective date silnika, więc region wybiera wersję po tej dacie, a nie po zegarze.
 */
public class Policy {

	private LocalDate startDate = LocalDate.now();
	private BigDecimal mainSum = BigDecimal.valueOf(300_000);
	private String paymentFrequency = "MONTHLY";

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public BigDecimal getMainSum() {
		return mainSum;
	}

	public void setMainSum(BigDecimal mainSum) {
		this.mainSum = mainSum;
	}

	public String getPaymentFrequency() {
		return paymentFrequency;
	}

	public void setPaymentFrequency(String paymentFrequency) {
		this.paymentFrequency = paymentFrequency;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("startDate", startDate)
			.append("mainSum", mainSum)
			.append("paymentFrequency", paymentFrequency)
			.toString();
	}
}
