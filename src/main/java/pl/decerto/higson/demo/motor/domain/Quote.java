package pl.decerto.higson.demo.motor.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pl.decerto.higson.demo.apk.Answer;
import pl.decerto.higson.demo.apk.Need;
import pl.decerto.higson.demo.apk.Question;
import pl.decerto.higson.demo.apk.Recommendation;
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

	/** Katalog pytań APK. Czytany z domeny przy każdej wycenie — zależy od daty polisy. */
	private List<Question> apkQuestions = new ArrayList<>();

	/** Odpowiedzi, kluczowane kodem pytania. Java nie zna żadnego z tych kodów. */
	private Map<String, Answer> apkAnswers = new LinkedHashMap<>();

	/** Potrzeby wyprowadzone z odpowiedzi (rek. 8.2.b). */
	private List<Need> apkNeeds = new ArrayList<>();

	/** Konfrontacja sytuacji klienta z wyłączeniami OWU (rek. 8.2.c). */

	/** Propozycja wraz z uzasadnieniem (rek. 8.2.d i 8.7). */
	private Recommendation apkRecommendation = new Recommendation();

	public Quote() {
	}

	public LocalDate getPolicyStartDate() {
		return policyStartDate;
	}

	public void setPolicyStartDate(LocalDate policyStartDate) {
		this.policyStartDate = policyStartDate;
	}

	public List<Question> getApkQuestions() {
		return apkQuestions;
	}

	public void setApkQuestions(List<Question> apkQuestions) {
		this.apkQuestions = apkQuestions;
	}

	public Map<String, Answer> getApkAnswers() {
		return apkAnswers;
	}

	public List<Need> getApkNeeds() {
		return apkNeeds;
	}

	public void setApkNeeds(List<Need> apkNeeds) {
		this.apkNeeds = apkNeeds;
	}

	public Recommendation getApkRecommendation() {
		return apkRecommendation;
	}

	public void setApkRecommendation(Recommendation apkRecommendation) {
		this.apkRecommendation = apkRecommendation;
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
