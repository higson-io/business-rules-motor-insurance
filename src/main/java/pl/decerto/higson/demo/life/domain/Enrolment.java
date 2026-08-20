package pl.decerto.higson.demo.life.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Liczba pracowników, którzy wybrali dany wariant. Lista przystąpień nie zawiera osób, tylko
 * liczności — program grupowy nie potrzebuje wiedzieć, kto wybrał, tylko ilu.
 */
public class Enrolment {

	private String variantCode;
	private int count;

	public Enrolment() {
	}

	public Enrolment(String variantCode, int count) {
		this.variantCode = variantCode;
		this.count = count;
	}

	public String getVariantCode() {
		return variantCode;
	}

	public void setVariantCode(String variantCode) {
		this.variantCode = variantCode;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("variantCode", variantCode)
			.append("count", count)
			.toString();
	}
}
