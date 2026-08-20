package pl.decerto.higson.demo.life.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Program grupowy. Brak tu jakichkolwiek danych osobowych — polisa grupowa nie prowadzi
 * indywidualnej oceny ryzyka, więc gałąź grupowa nie ma dostępu do quote.insured.*
 */
public class GroupScheme {

	private int headcount = 100;
	private int averageAge = 35;
	private int industryClass = 1;
	private BigDecimal participation = BigDecimal.valueOf(60);
	private boolean includeSpouse;
	private boolean includeAdultChild;
	private List<Enrolment> enrolment = new ArrayList<>();

	public int getHeadcount() {
		return headcount;
	}

	public void setHeadcount(int headcount) {
		this.headcount = headcount;
	}

	public int getAverageAge() {
		return averageAge;
	}

	public void setAverageAge(int averageAge) {
		this.averageAge = averageAge;
	}

	public int getIndustryClass() {
		return industryClass;
	}

	public void setIndustryClass(int industryClass) {
		this.industryClass = industryClass;
	}

	public BigDecimal getParticipation() {
		return participation;
	}

	public void setParticipation(BigDecimal participation) {
		this.participation = participation;
	}

	public boolean isIncludeSpouse() {
		return includeSpouse;
	}

	public void setIncludeSpouse(boolean includeSpouse) {
		this.includeSpouse = includeSpouse;
	}

	public boolean isIncludeAdultChild() {
		return includeAdultChild;
	}

	public void setIncludeAdultChild(boolean includeAdultChild) {
		this.includeAdultChild = includeAdultChild;
	}

	public List<Enrolment> getEnrolment() {
		return enrolment;
	}

	public void setEnrolment(List<Enrolment> enrolment) {
		this.enrolment = enrolment;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("headcount", headcount)
			.append("participation", participation)
			.append("enrolment", enrolment.size())
			.toString();
	}
}
