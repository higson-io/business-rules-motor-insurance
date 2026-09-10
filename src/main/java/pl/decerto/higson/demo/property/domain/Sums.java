package pl.decerto.higson.demo.property.domain;

import java.math.BigDecimal;

/**
 * Sumy ubezpieczenia. Wyliczone przez Higsona, ale klient może je podnieść — dlatego obok każdej
 * wybranej trzymamy wyliczoną, żeby dało się powiedzieć, o ile obniżył (decyzja D-2).
 */
public class Sums {

	private BigDecimal building;
	private BigDecimal fixtures;
	private BigDecimal contents;
	private BigDecimal suggestedBuilding;

	public BigDecimal getBuilding() {
		return building;
	}

	public void setBuilding(BigDecimal building) {
		this.building = building;
	}

	public BigDecimal getFixtures() {
		return fixtures;
	}

	public void setFixtures(BigDecimal fixtures) {
		this.fixtures = fixtures;
	}

	public BigDecimal getContents() {
		return contents;
	}

	public void setContents(BigDecimal contents) {
		this.contents = contents;
	}



	public BigDecimal getSuggestedBuilding() {
		return suggestedBuilding;
	}

	public void setSuggestedBuilding(BigDecimal suggestedBuilding) {
		this.suggestedBuilding = suggestedBuilding;
	}
}
