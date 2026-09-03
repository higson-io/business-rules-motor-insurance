package pl.decerto.higson.demo.apk;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Wynik analizy potrzeb: wariant plus powody. Lista braków jest częścią odpowiedzi, a nie
 * przypisem.
 *
 * ⛔ OCZEKIWANA SKŁADKA NIE OBNIŻA PROPOZYCJI. Do 2 września 2026 obniżała: przy przekroczeniu
 * budżetu rekomendacją zostawał najbogatszy wariant, który się w nim mieścił. To czyniło ze
 * składki filtr zakresu, a rek. 8.2.d wymaga czegoś innego — zakres ma odpowiadać potrzebom,
 * a oczekiwana składka jest wymaganiem klienta, o którego niespełnieniu trzeba go poinformować.
 * Dlatego `variantCode` niesie wariant najlepiej dopasowany do potrzeb ZAWSZE, a tańszy wariant
 * mieszczący się w budżecie jedzie obok, jako `alternative*` — propozycja i alternatywa, nie
 * podmiana jednej na drugą.
 */
public class Recommendation {

	private String variantCode;
	private int coveredNeeds;
	private int totalNeeds;
	private List<String> missingCovers = new ArrayList<>();
	private String budgetOutcome;

	/** Tańszy wariant mieszczący się w oczekiwanej składce. `null`, gdy nie ma takiego. */
	private String alternativeVariantCode;
	private BigDecimal alternativePremium;

	private List<String> justification = new ArrayList<>();

	public String getVariantCode() {
		return variantCode;
	}

	public void setVariantCode(String variantCode) {
		this.variantCode = variantCode;
	}

	public int getCoveredNeeds() {
		return coveredNeeds;
	}

	public void setCoveredNeeds(int coveredNeeds) {
		this.coveredNeeds = coveredNeeds;
	}

	public int getTotalNeeds() {
		return totalNeeds;
	}

	public void setTotalNeeds(int totalNeeds) {
		this.totalNeeds = totalNeeds;
	}

	public List<String> getMissingCovers() {
		return missingCovers;
	}

	public void setMissingCovers(List<String> missingCovers) {
		this.missingCovers = missingCovers;
	}

	public String getBudgetOutcome() {
		return budgetOutcome;
	}

	public void setBudgetOutcome(String budgetOutcome) {
		this.budgetOutcome = budgetOutcome;
	}

	public String getAlternativeVariantCode() {
		return alternativeVariantCode;
	}

	public void setAlternativeVariantCode(String alternativeVariantCode) {
		this.alternativeVariantCode = alternativeVariantCode;
	}

	public BigDecimal getAlternativePremium() {
		return alternativePremium;
	}

	public void setAlternativePremium(BigDecimal alternativePremium) {
		this.alternativePremium = alternativePremium;
	}

	public List<String> getJustification() {
		return justification;
	}

	public void setJustification(List<String> justification) {
		this.justification = justification;
	}
}
