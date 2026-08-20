package pl.decerto.higson.demo.life.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Osoba ubezpieczona. Wiek wstępu i BMI są wyliczane tutaj, bo tabele w Higsonie czytają je
 * z kontekstu jako gotowe wartości — silnik nie wylicza własnych kluczy wejściowych.
 */
public class Insured {

	private LocalDate birthDate;
	private Integer age;
	private boolean smoker;
	private int heightCm;
	private int weightKg;
	private int occupationClass = 1;
	private BigDecimal monthlyIncome;
	private LocalDate priorCoverFrom;
	private LocalDate priorCoverTo;

	/**
	 * Wiek na dzień początku ochrony — parametr, nie stan.
	 *
	 * Data urodzenia ma pierwszeństwo, bo jest dokładniejsza. Wiek podany wprost jest wejściem
	 * demonstracyjnym: ekran pyta o wiek, nie o datę urodzenia, żeby skrócić drogę do wyceny.
	 */
	public Integer getEntryAge(LocalDate policyStart) {
		if (birthDate != null && policyStart != null) {
			return Period.between(birthDate, policyStart).getYears();
		}
		return age;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	/**
	 * Wzrost 0 to dane bezsensowne, nie sytuacja wyjątkowa — zwracamy 0, żeby tabela przedziałowa
	 * dostała wartość zamiast wywalić się na dzieleniu przez zero.
	 */
	public BigDecimal getBmi() {
		if (heightCm <= 0 || weightKg <= 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal metres = BigDecimal.valueOf(heightCm).movePointLeft(2);
		return BigDecimal.valueOf(weightKg)
			.divide(metres.multiply(metres), 1, RoundingMode.HALF_UP);
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public boolean isSmoker() {
		return smoker;
	}

	public void setSmoker(boolean smoker) {
		this.smoker = smoker;
	}

	public int getHeightCm() {
		return heightCm;
	}

	public void setHeightCm(int heightCm) {
		this.heightCm = heightCm;
	}

	public int getWeightKg() {
		return weightKg;
	}

	public void setWeightKg(int weightKg) {
		this.weightKg = weightKg;
	}

	public int getOccupationClass() {
		return occupationClass;
	}

	public void setOccupationClass(int occupationClass) {
		this.occupationClass = occupationClass;
	}

	public BigDecimal getMonthlyIncome() {
		return monthlyIncome;
	}

	public void setMonthlyIncome(BigDecimal monthlyIncome) {
		this.monthlyIncome = monthlyIncome;
	}

	public LocalDate getPriorCoverFrom() {
		return priorCoverFrom;
	}

	public void setPriorCoverFrom(LocalDate priorCoverFrom) {
		this.priorCoverFrom = priorCoverFrom;
	}

	public LocalDate getPriorCoverTo() {
		return priorCoverTo;
	}

	public void setPriorCoverTo(LocalDate priorCoverTo) {
		this.priorCoverTo = priorCoverTo;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("birthDate", birthDate)
			.append("smoker", smoker)
			.append("occupationClass", occupationClass)
			.toString();
	}
}
