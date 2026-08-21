package pl.decerto.higson.demo.life.domain;

/**
 * Obowiązująca wersja taryfy — czytana z tabeli, nie wyliczana z daty.
 *
 * ⛔ Nigdzie w Javie ani w SPA nie ma daty progu. Wersję wybiera region po effective date, a jak
 * się nazywa, mówi {@code demo.life.tariff.version}. Kopia progu w kodzie byłaby drugą definicją
 * tej samej reguły — i to ta kopia zdezaktualizowałaby się pierwsza.
 */
public class TariffVersion {

	private String code;
	private String name;
	private String validFrom;

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

	public String getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(String validFrom) {
		this.validFrom = validFrom;
	}
}
