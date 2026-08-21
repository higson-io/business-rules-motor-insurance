package pl.decerto.higson.demo.life.api.dto;

/** Wersja taryfy. Front dostaje gotowy podpis i datę obowiązywania — progu nie zna. */
public class LifeTariffVersionDto {

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
