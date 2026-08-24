package pl.decerto.higson.demo.motor.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import pl.decerto.higson.demo.apk.Regime;

public class Quote {

	private String plan;
	private Driver driver = new Driver();
	private Vehicle vehicle = new Vehicle();
	private List<Option> options = new ArrayList<>();

	/**
	 * Data początku ochrony. Wybiera wersję reżimu APK przez region APK_MOTOR.
	 *
	 * ⚠️ Steruje też regionem MOTOR, do którego podpięte są istniejące tabele taryfowe, bo
	 * effective date jest jedno na silnik. W praktyce jest to nieszkodliwe: próg regionu MOTOR to
	 * 1 listopada 2020 r., a data początku ochrony nie może być wcześniejsza niż dziś — więc
	 * taryfa zawsze rozwiązuje się na tej samej wersji co dotąd. Sprzężenie jest realne i dlatego
	 * jest tu opisane, a nie przemilczane.
	 */
	private LocalDate policyStartDate = LocalDate.now();

	/** Reżim analizy potrzeb, czytany z reguł przy każdej wycenie. */
	private Regime apkRegime = new Regime();

	public Quote() {
	}

	public LocalDate getPolicyStartDate() {
		return policyStartDate;
	}

	public void setPolicyStartDate(LocalDate policyStartDate) {
		this.policyStartDate = policyStartDate;
	}

	public Regime getApkRegime() {
		return apkRegime;
	}

	public void setApkRegime(Regime apkRegime) {
		this.apkRegime = apkRegime;
	}

	public Quote(String plan, Driver driver) {
		this.plan = plan;
		this.driver = driver;
	}

	public Driver getDriver() {
		return driver;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public void addOption(Option o) {
		o.setQuote(this);
		options.add(o);
	}

	public String getPlan() {
		return plan;
	}

	public List<Option> getOptions() {
		return options;
	}

	public String print() {
		StringBuilder sb = new StringBuilder();
		line(sb, "===========  QUOTE  begin ===========");

		for (Option option : options) {
			line(sb, "- option  [" + option.getCode() + "] ");

			for (Coverage cov : option.getCoverages()) {
				line(sb, cov.print());
			}

			for (Discount discount : option.getDiscounts()) {
				line(sb, discount.print());
			}
		}

		line(sb, "===========  QUOTE  end   ===========");
		return sb.toString();
	}


	private static void line(StringBuilder sb, String str) {
		sb.append(str).append(System.getProperty("line.separator"));
	}

}
