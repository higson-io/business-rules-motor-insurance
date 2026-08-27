package pl.decerto.higson.demo.property.domain;

import pl.decerto.higson.demo.apk.Regime;

import pl.decerto.higson.demo.apk.Answer;

import pl.decerto.higson.demo.apk.Question;

import pl.decerto.higson.demo.apk.Recommendation;

import pl.decerto.higson.demo.apk.Warning;

import pl.decerto.higson.demo.apk.Need;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wniosek majątkowy. Agregat trzymany między żądaniami przez PropertyQuoteService.
 *
 * ⛔ Ankieta nie jest polem formularza, tylko listą przeczytaną z domeny Higsona przy KAŻDEJ
 * wycenie — bo zestaw pytań zależy od wersji regionu, a ta od daty polisy. Odpowiedzi są mapowane
 * po ścieżce kontekstu, nie po kodzie pytania, więc dołożenie pytania w Studio nic tu nie zmienia.
 */
public class PropertyQuote {

	private final Policy policy = new Policy();
	private final Sums sums = new Sums();
	private final Zone zone = new Zone();

	private Regime regime = new Regime();
	private List<Question> questions = new ArrayList<>();

	/** Klucz to kod pytania — jedyne miejsce, gdzie kod pytania w ogóle występuje po stronie Javy. */
	private final Map<String, Answer> answers = new LinkedHashMap<>();

	private List<Cover> covers = new ArrayList<>();
	private List<Variant> variants = new ArrayList<>();
	private List<Need> needs = new ArrayList<>();
	private List<Warning> warnings = new ArrayList<>();
	private Recommendation recommendation = new Recommendation();

	private BigDecimal riskFactor;
	private Integer buildingAge;
	private String selectedVariant;

	/**
	 * Czy klient sam ustawił sumę murów.
	 *
	 * ⚠️ Bez tej flagi wyliczona suma zamarzłaby na wartości z pierwszej wyceny — a wtedy zmiana
	 * stawki zł/m² po przesunięciu daty polisy nie byłaby widoczna i reguła ostrzegłaby jeszcze
	 * o „obniżeniu sumy", którego nikt nie zrobił.
	 */
	private boolean buildingSumOverridden;


	public Policy getPolicy() {
		return policy;
	}

	public Sums getSums() {
		return sums;
	}

	public Zone getZone() {
		return zone;
	}

	public Regime getRegime() {
		return regime;
	}

	public void setRegime(Regime regime) {
		this.regime = regime;
	}

	public List<Question> getQuestions() {
		return questions;
	}

	public void setQuestions(List<Question> questions) {
		this.questions = questions;
	}

	public Map<String, Answer> getAnswers() {
		return answers;
	}

	public Answer answerFor(String questionCode) {
		return answers.computeIfAbsent(questionCode, code -> new Answer());
	}

	public List<Cover> getCovers() {
		return covers;
	}

	public void setCovers(List<Cover> covers) {
		this.covers = covers;
	}

	public List<Variant> getVariants() {
		return variants;
	}

	public void setVariants(List<Variant> variants) {
		this.variants = variants;
	}

	public List<Need> getNeeds() {
		return needs;
	}

	public void setNeeds(List<Need> needs) {
		this.needs = needs;
	}

	public List<Warning> getWarnings() {
		return warnings;
	}

	public void setWarnings(List<Warning> warnings) {
		this.warnings = warnings;
	}

	public Recommendation getRecommendation() {
		return recommendation;
	}

	public void setRecommendation(Recommendation recommendation) {
		this.recommendation = recommendation;
	}

	public BigDecimal getRiskFactor() {
		return riskFactor;
	}

	public void setRiskFactor(BigDecimal riskFactor) {
		this.riskFactor = riskFactor;
	}

	public Integer getBuildingAge() {
		return buildingAge;
	}

	public void setBuildingAge(Integer buildingAge) {
		this.buildingAge = buildingAge;
	}

	public String getSelectedVariant() {
		return selectedVariant;
	}

	public void setSelectedVariant(String selectedVariant) {
		this.selectedVariant = selectedVariant;
	}

	public boolean isBuildingSumOverridden() {
		return buildingSumOverridden;
	}

	public void setBuildingSumOverridden(boolean buildingSumOverridden) {
		this.buildingSumOverridden = buildingSumOverridden;
	}
}
