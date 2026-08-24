package pl.decerto.higson.demo.motor.api.dto;

import java.time.LocalDate;
import java.util.List;

import pl.decerto.higson.demo.apk.api.RegimeDto;

public class QuoteDto {
	private List<OptionDto> options;

	/** Data początku ochrony. Wybiera wersję reżimu APK — patrz `apkRegime`. */
	private LocalDate policyStartDate;

	/** Reżim analizy potrzeb, z reguł. Front nie zna daty progu. */
	private RegimeDto apkRegime;

	public LocalDate getPolicyStartDate() {
		return policyStartDate;
	}

	public void setPolicyStartDate(LocalDate policyStartDate) {
		this.policyStartDate = policyStartDate;
	}

	public RegimeDto getApkRegime() {
		return apkRegime;
	}

	public void setApkRegime(RegimeDto apkRegime) {
		this.apkRegime = apkRegime;
	}

	public List<OptionDto> getOptions() {
		return options;
	}

	public void setOptions(List<OptionDto> options) {
		this.options = options;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("QuoteDto{");
		sb.append("options=").append(options);
		sb.append('}');
		return sb.toString();
	}
}
