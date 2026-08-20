package pl.decerto.higson.demo.life.domain;

import java.math.BigDecimal;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Wariant programu grupowego.
 *
 * monthlyTotalIfAll, nie monthlyTotal — przy wyborze wariantu przez pracownika ta liczba
 * odpowiada na pytanie "ile kosztowałby program, gdyby WSZYSCY wybrali ten wariant", czyli jest
 * hipotezą. Rzeczywisty koszt niesie programCost i istnieje tylko przy znanym rozkładzie.
 */
public class Variant {

	private String code;
	private String name;
	private int position;
	private boolean available;
	private BigDecimal perEmployee;
	private BigDecimal monthlyTotalIfAll;

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

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public BigDecimal getPerEmployee() {
		return perEmployee;
	}

	public void setPerEmployee(BigDecimal perEmployee) {
		this.perEmployee = perEmployee;
	}

	public BigDecimal getMonthlyTotalIfAll() {
		return monthlyTotalIfAll;
	}

	public void setMonthlyTotalIfAll(BigDecimal monthlyTotalIfAll) {
		this.monthlyTotalIfAll = monthlyTotalIfAll;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("code", code)
			.append("perEmployee", perEmployee)
			.toString();
	}
}
