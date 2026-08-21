package pl.decerto.higson.demo.property.domain;

/**
 * Dwie strefy wyliczone z jednego adresu. Powodziowa steruje DOSTĘPNOŚCIĄ pokrycia, kradzieżowa
 * jest czynnikiem składki — ten sam adres, dwie różne role (decyzja D-3).
 */
public class Zone {

	private String flood;
	private String theft;

	public String getFlood() {
		return flood;
	}

	public void setFlood(String flood) {
		this.flood = flood;
	}

	public String getTheft() {
		return theft;
	}

	public void setTheft(String theft) {
		this.theft = theft;
	}
}
